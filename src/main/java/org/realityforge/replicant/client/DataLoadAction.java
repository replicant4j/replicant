package org.realityforge.replicant.client;

import java.util.HashSet;
import java.util.LinkedList;
import javax.annotation.Nullable;

/**
 * A simple class encapsulating the process of loading data from a json change set.
 */
final class DataLoadAction
{
  private final boolean _bulkLoad;
  /**
   * The raw data string data prior to parsing. Null-ed after parsing.
   */
  @Nullable
  private String _rawJsonData;

  /**
   * The array of changes after parsing. Null prior to parsing.
   */
  @Nullable
  private ChangeSet _changeSet;

  /**
   * The current index into changes.
   */
  private int _changeIndex;

  /**
   * The code to execute at the end of the load action if any.
   */
  @Nullable
  private final Runnable _runnable;

  private LinkedList<Linkable> _updatedEntities = new LinkedList<Linkable>();
  private HashSet<Linkable> _removedEntities = new HashSet<Linkable>();
  private LinkedList<Linkable> _entitiesToLink;
  private boolean _entityLinksCalculated;
  private boolean _worldNotified;

  public DataLoadAction( final boolean bulkLoad, final String rawJsonData, @Nullable final Runnable runnable )
  {
    _bulkLoad = bulkLoad;
    _rawJsonData = rawJsonData;
    _runnable = runnable;
  }

  public boolean isBulkLoad()
  {
    return _bulkLoad;
  }

  @Nullable
  public String getRawJsonData()
  {
    return _rawJsonData;
  }

  public void setChangeSet( @Nullable final ChangeSet changeSet )
  {
    _changeSet = changeSet;
    _rawJsonData = null;
    _changeIndex = 0;
  }

  public boolean areChangesPending()
  {
    return null != _changeSet && _changeIndex < _changeSet.getChangeCount();
  }

  public Change nextChange()
  {
    if ( areChangesPending() )
    {
      assert null != _changeSet;
      final Change change = _changeSet.getChange( _changeIndex );
      _changeIndex++;
      return change;
    }
    else
    {
      return null;
    }
  }

  public void changeProcessed( final boolean isUpdate, final Object entity )
  {
    if ( entity instanceof Linkable )
    {
      if ( isUpdate )
      {
        _updatedEntities.add( (Linkable) entity );
      }
      else
      {
        _removedEntities.add( (Linkable) entity );
      }
    }
  }

  public boolean areEntityLinksCalculated()
  {
    return _entityLinksCalculated;
  }

  public void calculateEntitiesToLink()
  {
    _entityLinksCalculated = true;
    _entitiesToLink = new LinkedList<Linkable>();
    for ( final Linkable entity : _updatedEntities )
    {
      // In some circumstances a create and remove can appear in same change set so guard against this
      if ( !_removedEntities.contains( entity ) )
      {
        _entitiesToLink.add( entity );
      }
    }
    _updatedEntities = null;
    _removedEntities = null;
  }

  public boolean areEntityLinksPending()
  {
    return null != _entitiesToLink && !_entitiesToLink.isEmpty();
  }

  public Linkable nextEntityToLink()
  {
    if ( areEntityLinksPending() )
    {
      assert null != _entitiesToLink;
      return _entitiesToLink.remove();
    }
    else
    {
      _entitiesToLink = null;
      return null;
    }
  }

  @Nullable
  public ChangeSet getChangeSet()
  {
    return _changeSet;
  }

  @Nullable
  public Runnable getRunnable()
  {
    return _runnable;
  }

  public void markWorldAsNotified()
  {
    _worldNotified = true;
  }

  public boolean hasWorldBeenNotified()
  {
    return _worldNotified;
  }

  @Override
  public String toString()
  {
    return "DataLoad[" +
           "IsBulk=" + _bulkLoad +
           ",RawJson.null?=" + ( _rawJsonData == null ) +
           ",ChangeSet.null?=" + ( _changeSet == null ) +
           ",ChangeIndex=" + _changeIndex +
           ",Runnable.null?=" + ( _runnable == null ) +
           ",UpdatedEntities.size=" + ( _updatedEntities != null ? _updatedEntities.size() : null) +
           ",RemovedEntities.size=" + ( _removedEntities != null ? _removedEntities.size() : null) +
           ",EntitiesToLink.size=" + ( _entitiesToLink != null ? _entitiesToLink.size() : null) +
           ",EntityLinksCalculated=" + _entityLinksCalculated +
           "]";
  }
}
