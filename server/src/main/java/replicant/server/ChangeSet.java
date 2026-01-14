package replicant.server;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.JsonObject;
import replicant.server.ee.JsonUtil;

public final class ChangeSet
{
  @Nonnull
  private final List<ChannelAction> _channelActions = new LinkedList<>();
  @Nonnull
  private final Map<String, Change> _changes = new LinkedHashMap<>();
  private boolean _required;
  @Nullable
  private String _eTag;

  public boolean hasContent()
  {
    return _required || !_channelActions.isEmpty() || !_changes.isEmpty();
  }

  public boolean isRequired()
  {
    return _required;
  }

  public void setRequired( final boolean required )
  {
    _required = required;
  }

  public boolean isCacheResponse()
  {
    return null != _eTag;
  }

  @Nullable
  public String getETag()
  {
    return _eTag;
  }

  public void setETag( @Nonnull final String eTag )
  {
    assert null == _eTag;
    _eTag = Objects.requireNonNull( eTag );
  }

  public void mergeActions( @Nonnull final Collection<ChannelAction> actions )
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
    if ( ChannelAction.Action.ADD == action.action() )
    {
      if ( _channelActions.removeIf( a -> ChannelAction.Action.REMOVE == a.action() &&
                                          a.address().equals( action.address() ) &&
                                          null == action.filter() ) )
      {
        return;
      }
    }
    else if ( ChannelAction.Action.UPDATE == action.action() )
    {
      // We have got an update for one we are adding so ignore the update
      if ( _channelActions.stream().anyMatch( a -> ChannelAction.Action.ADD == a.action() &&
                                                   a.address().equals( action.address() ) &&
                                                   null == action.filter() ) )
      {
        return;
      }
    }
    else if ( ChannelAction.Action.REMOVE == action.action() )
    {
      if ( _channelActions.removeIf( a -> ChannelAction.Action.ADD == a.action() &&
                                          a.address().equals( action.address() ) &&
                                          null == a.filter() ) )
      {
        return;
      }
    }
    else if ( ChannelAction.Action.DELETE == action.action() )
    {
      final boolean removedAdd =
        _channelActions.removeIf( a -> ChannelAction.Action.ADD == a.action() &&
                                       a.address().equals( action.address() ) );
      _channelActions.removeIf( a -> a.address().equals( action.address() ) );
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
  public List<ChannelAction> getChannelActions()
  {
    return _channelActions;
  }

  public void merge( @Nonnull final Collection<Change> changes )
  {
    merge( changes, false );
  }

  private void merge( @Nonnull final Collection<Change> changes, final boolean copyOnMerge )
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
    _eTag = changeSet.getETag();
    merge( changeSet.getChanges(), copyOnMerge );
    mergeActions( changeSet.getChannelActions() );
  }

  public void merge( @Nonnull final ChannelAddress address, @Nonnull final EntityMessageSet messages )
  {
    merge( ChangeUtil.toChanges( messages.getEntityMessages(), address ) );
  }

  @Nonnull
  public Collection<Change> getChanges()
  {
    return _changes.values();
  }
}
