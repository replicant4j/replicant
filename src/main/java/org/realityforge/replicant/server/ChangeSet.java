package org.realityforge.replicant.server;

import java.util.Collection;
import java.util.LinkedHashMap;

public final class ChangeSet
{
  private final LinkedHashMap<String, Change> _entities = new LinkedHashMap<String, Change>();

  public void mergeAll( final Collection<Change> changes )
  {
    mergeAll( changes, false );
  }

  public void mergeAll( final Collection<Change> changes, final boolean copyOnMerge )
  {
    for ( final Change change : changes )
    {
      merge( change, copyOnMerge );
    }
  }

  public void merge( final Change change )
  {
    merge( change, false );
  }

  public void merge( final Change change, final boolean copyOnMerge )
  {
    final Change existing = _entities.get( change.getID() );
    if ( null != existing )
    {
      existing.merge( change );
    }
    else
    {
      _entities.put( change.getID(), copyOnMerge ? change.duplicate() : change );
    }
  }

  public Collection<Change> getChanges()
  {
    return _entities.values();
  }
}
