package replicant;

import arez.component.Linkable;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.messages.ChangeSetMessage;
import replicant.messages.EntityChange;
import replicant.spy.DataLoadStatus;
import static org.realityforge.braincheck.Guards.*;

/**
 * A simple class encapsulating the process of loading data from a json change set.
 */
@SuppressFBWarnings( value = { "EQ_COMPARETO_USE_OBJECT_EQUALS" }, justification = "Equals is not used and implementing it would add code that GWT2.x could not optimize away" )
final class MessageResponse
{
  /**
   * The raw data string data prior to parsing. Null-ed after parsing.
   */
  @Nullable
  private String _rawJsonData;
  /**
   * The array of changes after parsing. Null prior to parsing.
   */
  @Nullable
  private ChangeSetMessage _changeSet;
  /**
   * The current index into changes.
   */
  private int _entityChangeIndex;
  private LinkedList<Linkable> _updatedEntities = new LinkedList<>();
  private List<ChannelChangeDescriptor> _parsedChannelChanges;
  private boolean _worldValidated;
  private boolean _channelActionsProcessed;
  private RequestEntry _request;
  private int _channelAddCount;
  private int _channelUpdateCount;
  private int _channelRemoveCount;
  private int _entityUpdateCount;
  private int _entityRemoveCount;
  private int _entityLinkCount;

  MessageResponse( @Nonnull final String rawJsonData )
  {
    _rawJsonData = Objects.requireNonNull( rawJsonData );
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

  void incChannelAddCount()
  {
    if ( Replicant.areSpiesEnabled() )
    {
      _channelAddCount++;
    }
  }

  void incChannelUpdateCount()
  {
    if ( Replicant.areSpiesEnabled() )
    {
      _channelUpdateCount++;
    }
  }

  void incChannelRemoveCount()
  {
    if ( Replicant.areSpiesEnabled() )
    {
      _channelRemoveCount++;
    }
  }

  void incEntityUpdateCount()
  {
    if ( Replicant.areSpiesEnabled() )
    {
      _entityUpdateCount++;
    }
  }

  void incEntityRemoveCount()
  {
    if ( Replicant.areSpiesEnabled() )
    {
      _entityRemoveCount++;
    }
  }

  void incEntityLinkCount()
  {
    if ( Replicant.areSpiesEnabled() )
    {
      _entityLinkCount++;
    }
  }

  boolean needsParsing()
  {
    return null != _rawJsonData;
  }

  @Nullable
  String getRawJsonData()
  {
    return _rawJsonData;
  }

  void recordChangeSet( @Nonnull final ChangeSetMessage changeSet, @Nullable final RequestEntry request )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> null == request || ( (Integer) request.getRequestId() ).equals( changeSet.getRequestId() ),
                 () -> "Replicant-0011: ChangeSet specified requestId '" + changeSet.getRequestId() +
                       "' but request with requestId '" + Objects.requireNonNull( request ).getRequestId() +
                       "' has been passed to recordChangeSet." );
    }
    _changeSet = Objects.requireNonNull( changeSet );
    _request = request;
    _rawJsonData = null;
    _entityChangeIndex = 0;
  }

  RequestEntry getRequest()
  {
    return _request;
  }

  boolean areEntityChangesPending()
  {
    return null != _changeSet &&
           _changeSet.hasEntityChanges() &&
           _entityChangeIndex < _changeSet.getEntityChanges().length;
  }

  boolean needsChannelChangesProcessed()
  {
    return null != _changeSet &&
           (
             ( _changeSet.hasChannels() && 0 != _changeSet.getChannels().length ) ||
             ( _changeSet.hasFilteredChannels() && 0 != _changeSet.getFilteredChannels().length )
           ) &&
           !_channelActionsProcessed;
  }

  void setParsedChannelChanges( @Nonnull final List<ChannelChangeDescriptor> parsedChannelChanges )
  {
    _parsedChannelChanges = Objects.requireNonNull( parsedChannelChanges );
  }

  List<ChannelChangeDescriptor> getChannelChanges()
  {
    return _parsedChannelChanges;
  }

  void markChannelActionsProcessed()
  {
    _channelActionsProcessed = true;
  }

  EntityChange nextEntityChange()
  {
    if ( areEntityChangesPending() )
    {
      assert null != _changeSet;
      final EntityChange change = _changeSet.getEntityChanges()[ _entityChangeIndex ];
      _entityChangeIndex++;
      return change;
    }
    else
    {
      return null;
    }
  }

  void changeProcessed( @Nonnull final Object entity )
  {
    if ( entity instanceof Linkable )
    {
      _updatedEntities.add( (Linkable) entity );
    }
  }

  boolean areEntityLinksPending()
  {
    return null != _updatedEntities && !_updatedEntities.isEmpty();
  }

  Linkable nextEntityToLink()
  {
    if ( areEntityLinksPending() )
    {
      assert null != _updatedEntities;
      return _updatedEntities.remove();
    }
    else
    {
      _updatedEntities = null;
      return null;
    }
  }

  @Nonnull
  ChangeSetMessage getChangeSet()
  {
    assert null != _changeSet;
    return _changeSet;
  }

  @Nullable
  SafeProcedure getCompletionAction()
  {
    if ( null == _request || !_request.hasCompleted() )
    {
      return null;
    }
    else
    {
      return _request.getCompletionAction();
    }
  }

  void markWorldAsValidated()
  {
    if ( Replicant.shouldValidateEntitiesOnLoad() )
    {
      _worldValidated = true;
    }
  }

  boolean hasWorldBeenValidated()
  {
    return !Replicant.shouldValidateEntitiesOnLoad() || _worldValidated;
  }

  @Nonnull
  DataLoadStatus toStatus()
  {
    assert Replicant.areSpiesEnabled();
    final ChangeSetMessage changeSet = getChangeSet();
    return new DataLoadStatus(
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
             ",ChangeIndex=" + _entityChangeIndex +
             ",CompletionAction.null?=" + ( null == getCompletionAction() ) +
             ",UpdatedEntities.size=" + ( null == _updatedEntities ? 0 : _updatedEntities.size() ) +
             "]";
    }
    else
    {
      return super.toString();
    }
  }

  LinkedList<Linkable> getUpdatedEntities()
  {
    return _updatedEntities;
  }
}
