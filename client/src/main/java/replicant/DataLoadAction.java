package replicant;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.spy.DataLoadStatus;
import static org.realityforge.braincheck.Guards.*;

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
   * The array of changes after parsing. Null prior to parsing.
   */
  @Nullable
  private ChangeSet _changeSet;
  /**
   * The action executed at completion of out-of-band message.
   */
  @Nullable
  private final SafeProcedure _oobCompletionAction;

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

  public DataLoadAction( @Nonnull final String rawJsonData )
  {
    this( rawJsonData, null );
  }

  public DataLoadAction( @Nonnull final String rawJsonData, @Nullable final SafeProcedure oobCompletionAction )
  {
    _rawJsonData = Objects.requireNonNull( rawJsonData );
    _oobCompletionAction = oobCompletionAction;
  }

  int getChannelAddCount()
  {
    return _channelAddCount;
  }

  int getChannelUpdateCount()
  {
    return _channelUpdateCount;
  }

  int getChannelRemoveCount()
  {
    return _channelRemoveCount;
  }

  int getEntityUpdateCount()
  {
    return _entityUpdateCount;
  }

  int getEntityRemoveCount()
  {
    return _entityRemoveCount;
  }

  int getEntityLinkCount()
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

  /**
   * Return true if the message is an out-of-band message, false otherwise.
   *
   * @return true if the message is an out-of-band message, false otherwise.
   */
  public boolean isOob()
  {
    return null != _oobCompletionAction;
  }

  @Nullable
  public String getRawJsonData()
  {
    return _rawJsonData;
  }

  public void recordChangeSet( @Nonnull final ChangeSet changeSet, @Nullable final RequestEntry request )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> !isOob() || null == request,
                 () -> "Replicant-0010: Incorrectly associating a request named '" +
                       Objects.requireNonNull( request ).getRequestKey() + "' with requestId '" +
                       Objects.requireNonNull( request ).getRequestId() + "' with an out-of-band message." );
      invariant( () -> null == request || request.getRequestId().equals( changeSet.getRequestId() ),
                 () -> "Replicant-0011: ChangeSet specified requestId '" + changeSet.getRequestId() +
                       "' but request with requestId '" + Objects.requireNonNull( request ).getRequestId() +
                       "' has been passed to recordChangeSet." );
    }
    _changeSet = Objects.requireNonNull( changeSet );
    _request = request;
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
        // TODO: This should not be needed, seems like a bug in server code if it occurs
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
      // TODO: This should not be needed, seems like a bug in server code if it occurs
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

  @Nullable
  public SafeProcedure getCompletionAction()
  {
    if ( null != _oobCompletionAction )
    {
      return _oobCompletionAction;
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
    assert Replicant.areSpiesEnabled();
    final ChangeSet changeSet = getChangeSet();
    return new DataLoadStatus( changeSet.getSequence(),
                               changeSet.getRequestId(),
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
    if ( Replicant.areNamesEnabled() )
    {
      return "DataLoad[" +
             ",RawJson.null?=" + ( null == _rawJsonData ) +
             ",ChangeSet.null?=" + ( null == _changeSet ) +
             ",ChangeIndex=" + _changeIndex +
             ",Runnable.null?=" + ( null == getCompletionAction() ) +
             ",UpdatedEntities.size=" + ( null == _updatedEntities ? null : _updatedEntities.size() ) +
             ",RemovedEntities.size=" + ( null == _removedEntities ? null : _removedEntities.size() ) +
             ",EntitiesToLink.size=" + ( null == _entitiesToLink ? null : _entitiesToLink.size() ) +
             ",EntityLinksCalculated=" + _entityLinksCalculated +
             "]";
    }
    else
    {
      return super.toString();
    }
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
