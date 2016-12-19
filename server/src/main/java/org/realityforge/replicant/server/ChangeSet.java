package org.realityforge.replicant.server;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import javax.annotation.Nonnull;

public final class ChangeSet
{
  private final LinkedList<ChannelAction> _channelActions = new LinkedList<>();
  private final LinkedHashMap<String, Change> _changes = new LinkedHashMap<>();

  public void addActions( @Nonnull final Collection<ChannelAction> actions )
  {
    for ( final ChannelAction action : actions )
    {
      addAction( action );
    }
  }

  public void addAction( @Nonnull final ChannelAction action )
  {
    _channelActions.add( action );
  }

  @Nonnull
  public LinkedList<ChannelAction> getChannelActions()
  {
    return _channelActions;
  }

  public void mergeAll( @Nonnull final Collection<Change> changes )
  {
    mergeAll( changes, false );
  }

  public void mergeAll( @Nonnull final Collection<Change> changes, final boolean copyOnMerge )
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
    final Change existing = _changes.get( change.getID() );
    if ( null != existing )
    {
      existing.merge( change );
    }
    else
    {
      _changes.put( change.getID(), copyOnMerge ? change.duplicate() : change );
    }
  }

  public void merge( @Nonnull final ChangeSet changeSet )
  {
    merge( changeSet, false );
  }

  public void merge( @Nonnull final ChangeSet changeSet, final boolean copyOnMerge )
  {
    mergeAll( changeSet.getChanges(), copyOnMerge );
    addActions( changeSet.getChannelActions() );
  }

  @Nonnull
  public Collection<Change> getChanges()
  {
    return _changes.values();
  }
}
