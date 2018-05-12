package replicant;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.spy.DataLoadStatus;

/**
 * A simple class encapsulating the process of loading data from a json change set.
 */
public final class DataLoadAction
  implements Comparable<DataLoadAction>
{
  //TODO: Make this package access after all classes migrated to replicant package
  /**
   * The raw data string data prior to parsing. Null-ed after parsing.
   */
  @Nullable
  private String _rawJsonData;

  /**
   * Is the message out-of-band ?
   */
  private final boolean _oob;

  /**
   * The array of changes after parsing. Null prior to parsing.
   */
  @Nullable
  private ChangeSet _changeSet;

  /**
   * The runnable action that may have been explicitly set for oob message.
   */
  @Nullable
  private Runnable _runnable;

  /**
   * The current index into changes.
   */
  private int _changeIndex;

  private LinkedList<Linkable> _updatedEntities = new LinkedList<>();
  private HashSet<Linkable> _removedEntities = new HashSet<>();
  private LinkedList<Linkable> _entitiesToLink;
  private boolean _entityLinksCalculated;
  private boolean _worldNotified;
  private boolean _channelActionsProcessed;
  private RequestEntry _request;

  private int _channelAddCount;
  private int _channelUpdateCount;
  private int _channelRemoveCount;
  private int _entityUpdateCount;
  private int _entityRemoveCount;
  private int _entityLinkCount;

  public DataLoadAction( @Nonnull final String rawJsonData, final boolean oob )
  {
    _rawJsonData = Objects.requireNonNull( rawJsonData );
    _oob = oob;
  }

  public int getChannelAddCount()
  {
    return _channelAddCount;
  }

  public int getChannelUpdateCount()
  {
    return _channelUpdateCount;
  }

  public int getChannelRemoveCount()
  {
    return _channelRemoveCount;
  }

  public int getEntityUpdateCount()
  {
    return _entityUpdateCount;
  }

  public int getEntityRemoveCount()
  {
    return _entityRemoveCount;
  }

  public int getEntityLinkCount()
  {
    return _entityLinkCount;
  }

  public void incChannelAddCount()
  {
    if ( Replicant.areSpiesEnabled() )
    {
      _channelAddCount++;
    }
  }

  public void incChannelUpdateCount()
  {
    if ( Replicant.areSpiesEnabled() )
    {
      _channelUpdateCount++;
    }
  }

  public void incChannelRemoveCount()
  {
    if ( Replicant.areSpiesEnabled() )
    {
      _channelRemoveCount++;
    }
  }

  public void incEntityUpdateCount()
  {
    if ( Replicant.areSpiesEnabled() )
    {
      _entityUpdateCount++;
    }
  }

  public void incEntityRemoveCount()
  {
    if ( Replicant.areSpiesEnabled() )
    {
      _entityRemoveCount++;
    }
  }

  public void incEntityLinkCount()
  {
    if ( Replicant.areSpiesEnabled() )
    {
      _entityLinkCount++;
    }
  }

  public boolean isOob()
  {
    return _oob;
  }

  @Nullable
  public String getRawJsonData()
  {
    return _rawJsonData;
  }

  public void setChangeSet( @Nullable final ChangeSet changeSet, @Nullable final RequestEntry request )
  {
    assert !isOob() || null == request;
    _request = request;
    _changeSet = changeSet;
    _rawJsonData = null;
    _changeIndex = 0;
  }

  public RequestEntry getRequest()
  {
    return _request;
  }

  public boolean areChangesPending()
  {
    return null != _changeSet && _changeIndex < _changeSet.getEntityChanges().length;
  }

  public boolean needsChannelActionsProcessed()
  {
    return null != _changeSet && 0 != _changeSet.getChannelChanges().length && !_channelActionsProcessed;
  }

  public void markChannelActionsProcessed()
  {
    _channelActionsProcessed = true;
  }

  public EntityChange nextChange()
  {
    if ( areChangesPending() )
    {
      assert null != _changeSet;
      final EntityChange change = _changeSet.getEntityChanges()[ _changeIndex ];
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
    _entitiesToLink = new LinkedList<>();
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

  @Nonnull
  public ChangeSet getChangeSet()
  {
    assert null != _changeSet;
    return _changeSet;
  }

  public void setRunnable( @Nullable final Runnable runnable )
  {
    assert isOob();
    _runnable = runnable;
  }

  @Nullable
  public Runnable getRunnable()
  {
    if ( null != _runnable )
    {
      return _runnable;
    }
    else if ( null == _request || !_request.isCompletionDataPresent() )
    {
      return null;
    }
    else
    {
      return _request.getCompletionAction();
    }
  }

  public void markWorldAsNotified()
  {
    _worldNotified = true;
  }

  public boolean hasWorldBeenNotified()
  {
    return _worldNotified;
  }

  @Nonnull
  public DataLoadStatus toStatus()
  {
    final ChangeSet changeSet = getChangeSet();
    return new DataLoadStatus( changeSet.getSequence(),
                               changeSet.getRequestID(),
                               getChannelAddCount(),
                               getChannelUpdateCount(),
                               getChannelRemoveCount(),
                               getEntityUpdateCount(),
                               getEntityRemoveCount(),
                               getEntityLinkCount() );
  }

  @Override
  public String toString()
  {
    return "DataLoad[" +
           ",RawJson.null?=" + ( null == _rawJsonData ) +
           ",ChangeSet.null?=" + ( null == _changeSet ) +
           ",ChangeIndex=" + _changeIndex +
           ",Runnable.null?=" + ( null == getRunnable() ) +
           ",UpdatedEntities.size=" + ( null == _updatedEntities ? null : _updatedEntities.size() ) +
           ",RemovedEntities.size=" + ( null == _removedEntities ? null : _removedEntities.size() ) +
           ",EntitiesToLink.size=" + ( null == _entitiesToLink ? null : _entitiesToLink.size() ) +
           ",EntityLinksCalculated=" + _entityLinksCalculated +
           "]";
  }

  @Override
  public int compareTo( @Nonnull final DataLoadAction other )
  {
    if ( isOob() && other.isOob() )
    {
      return 0;
    }
    else if ( isOob() )
    {
      return -1;
    }
    else if ( other.isOob() )
    {
      return 1;
    }
    else
    {
      final ChangeSet changeSet1 = getChangeSet();
      final ChangeSet changeSet2 = other.getChangeSet();
      return changeSet1.getSequence() - changeSet2.getSequence();
    }
  }
}
