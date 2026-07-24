package replicant.server;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import javax.json.JsonObject;

public final class ChangeSet
{
  @NonNull
  private final List<ChannelAction> _channelActions = new LinkedList<>();
  @NonNull
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

  @Nullable
  public String getETag()
  {
    return _eTag;
  }

  public void setETag( @NonNull final String eTag )
  {
    assert null == _eTag;
    _eTag = Objects.requireNonNull( eTag );
  }

  public void mergeActions( @NonNull final Collection<ChannelAction> actions )
  {
    for ( final var action : actions )
    {
      mergeAction( action );
    }
  }

  public void mergeAction( @NonNull final ChannelAddress address, final ChannelAction.@NonNull Action action )
  {
    mergeAction( address, action, null );
  }

  public void mergeAction( @NonNull final ChannelAddress address,
                           final ChannelAction.@NonNull Action action,
                           @Nullable final JsonObject filter )
  {
    //noinspection ConstantValue
    assert ChannelAction.Action.DELETE != action || ChannelAction.Action.REMOVE != action || null == filter;
    mergeAction( ChannelAction.of( address, action, filter ) );
  }

  public void mergeAction( @NonNull final ChannelAction action )
  {
    final var actionType = action.action();
    /*
     * If we have a matching inverse action in actions list then we can remove
     * that action and avoid adding this action. This avoids scenario where there
     * are multiple actions for the same address and filter in ChangeSet.
     */
    if ( ChannelAction.Action.ADD == actionType )
    {
      final var removedRemove = _channelActions.removeIf( a -> ChannelAction.Action.REMOVE == a.action() &&
                                                               a.address().equals( action.address() ) &&
                                                               null == a.filter() );
      _channelActions.removeIf( a -> a.address().equals( action.address() ) );
      if ( removedRemove )
      {
        return;
      }
    }
    else if ( ChannelAction.Action.UPDATE == actionType )
    {
      // We have got an update for one we are adding so ignore the update and maybe update the existing
      final var newFilter = action.filter();
      var flags = new boolean[ 1 ];
      _channelActions.replaceAll( a -> {
        final var address = a.address();
        if ( ChannelAction.Action.ADD == a.action() && address.equals( action.address() ) )
        {
          flags[ 0 ] = true;
          if ( FilterUtil.filtersEqual( a.filter(), newFilter ) )
          {
            return a;
          }
          else
          {
            return ChannelAction.of( address, ChannelAction.Action.ADD, newFilter );
          }
        }
        else
        {
          return a;
        }
      } );
      //noinspection ConstantValue
      if ( flags[ 0 ] )
      {
        return;
      }
    }
    else if ( ChannelAction.Action.REMOVE == actionType || ChannelAction.Action.DELETE == actionType )
    {
      final var removedAdd =
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

  @NonNull
  public List<ChannelAction> getChannelActions()
  {
    return _channelActions;
  }

  public void merge( @NonNull final Collection<Change> changes )
  {
    merge( changes, false );
  }

  private void merge( @NonNull final Collection<Change> changes, final boolean copyOnMerge )
  {
    for ( final var change : changes )
    {
      merge( change, copyOnMerge );
    }
  }

  public void merge( @NonNull final Change change )
  {
    merge( change, false );
  }

  void merge( @NonNull final Change change, final boolean copyOnMerge )
  {
    final var existing = _changes.get( change.getKey() );
    if ( null != existing )
    {
      existing.merge( change );
    }
    else
    {
      _changes.put( change.getKey(), copyOnMerge ? change.duplicate() : change );
    }
  }

  public void merge( @NonNull final ChangeSet changeSet )
  {
    _eTag = changeSet.getETag();
    merge( changeSet.getChanges(), true );
    mergeActions( changeSet.getChannelActions() );
  }

  @NonNull
  public Collection<Change> getChanges()
  {
    return _changes.values();
  }
}
