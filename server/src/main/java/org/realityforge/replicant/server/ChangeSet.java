package org.realityforge.replicant.server;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.JsonObject;
import org.realityforge.replicant.server.ee.JsonUtil;

public final class ChangeSet
{
  private final LinkedList<ChannelAction> _channelActions = new LinkedList<>();
  private final LinkedHashMap<String, Change> _changes = new LinkedHashMap<>();
  private boolean _required;

  public boolean isRequired()
  {
    return _required;
  }

  public void setRequired( final boolean required )
  {
    _required = required;
  }

  private void mergeActions( @Nonnull final Collection<ChannelAction> actions )
  {
    for ( final ChannelAction action : actions )
    {
      mergeAction( action );
    }
  }

  public void mergeAction( @Nonnull final ChannelAddress address,
                           @Nonnull final ChannelAction.Action action,
                           @Nullable final Object filter )
  {
    mergeAction( new ChannelAction( address, action, filterToJsonObject( filter ) ) );
  }

  public void mergeAction( @Nonnull final ChannelAction action )
  {
    /*
     * If we have an unfiltered inverse action in actions list then we can remove
     * that action and avoid adding this action. This avoids scenario where there
     * are multiple actions for the same address in ChangeSet.
     */
    if ( ChannelAction.Action.ADD == action.getAction() )
    {
      if ( _channelActions.removeIf( a -> ChannelAction.Action.REMOVE == a.getAction() &&
                                          a.getAddress().equals( action.getAddress() ) &&
                                          null == action.getFilter() ) )
      {
        return;
      }
    }
    else if ( ChannelAction.Action.REMOVE == action.getAction() )
    {
      if ( _channelActions.removeIf( a -> ChannelAction.Action.ADD == a.getAction() &&
                                          a.getAddress().equals( action.getAddress() ) &&
                                          null == a.getFilter() ) )
      {
        return;
      }
    }
    else if ( ChannelAction.Action.DELETE == action.getAction() )
    {
      final boolean removedAdd =
        _channelActions.removeIf( a -> ChannelAction.Action.ADD == a.getAction() &&
                                       a.getAddress().equals( action.getAddress() ) );
      _channelActions.removeIf( a -> a.getAddress().equals( action.getAddress() ) );
      if ( removedAdd )
      {
        return;
      }
    }

    _channelActions.add( action );
  }

  private JsonObject filterToJsonObject( @Nullable final Object filter )
  {
    return null == filter ? null : JsonUtil.toJsonObject( filter );
  }

  @Nonnull
  public LinkedList<ChannelAction> getChannelActions()
  {
    return _channelActions;
  }

  void mergeAll( @Nonnull final Collection<Change> changes )
  {
    mergeAll( changes, false );
  }

  private void mergeAll( @Nonnull final Collection<Change> changes, final boolean copyOnMerge )
  {
    for ( final Change change : changes )
    {
      merge( change, copyOnMerge );
    }
  }

  public void merge( @Nonnull final Change change )
  {
    merge( change, false );
  }

  public void merge( @Nonnull final Change change, final boolean copyOnMerge )
  {
    final Change existing = _changes.get( change.getKey() );
    if ( null != existing )
    {
      existing.merge( change );
    }
    else
    {
      _changes.put( change.getKey(), copyOnMerge ? change.duplicate() : change );
    }
  }

  public void merge( @Nonnull final ChangeSet changeSet, final boolean copyOnMerge )
  {
    mergeAll( changeSet.getChanges(), copyOnMerge );
    mergeActions( changeSet.getChannelActions() );
  }

  public void merge( @Nonnull final ChannelAddress address, @Nonnull final EntityMessageSet messages )
  {
    mergeAll( ChangeUtil.toChanges( messages.getEntityMessages(),
                                    address.getChannelId(),
                                    address.getSubChannelId() ) );
  }

  @Nonnull
  public Collection<Change> getChanges()
  {
    return _changes.values();
  }
}
