package replicant;

import arez.component.Linkable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.messages.ChannelChange;
import replicant.messages.EntityChange;
import replicant.messages.ServerToClientMessage;
import replicant.messages.UpdateMessage;
import replicant.spy.DataLoadStatus;
import static org.realityforge.braincheck.Guards.*;

/**
 * A simple class encapsulating the process of loading data from a json change set.
 */
@SuppressFBWarnings( value = { "EQ_COMPARETO_USE_OBJECT_EQUALS" }, justification = "Equals is not used and implementing it would add code that GWT2.x could not optimize away" )
final class MessageResponse
{
  private final int _schemaId;
  /**
   * The message to process.
   */
  @Nonnull
  private final ServerToClientMessage _message;
  @Nullable
  private final RequestEntry _request;
  /**
   * The current index into changes.
   */
  private int _entityChangeIndex;
  private LinkedList<Linkable> _entitiesToLink = new LinkedList<>();
  /**
   * The list of entities that have been changed during processing.
   * Used to provide a hook to perform local internal filtered subscription management.
   */
  @Nonnull
  private final LinkedList<Object> _entitiesChanged = new LinkedList<>();
  private List<ChannelChangeDescriptor> _parsedChannelChanges;
  private boolean _worldValidated;
  private boolean _channelActionsProcessed;
  private boolean _orphanSubscriptionRemoved;
  private int _channelAddCount;
  private int _channelUpdateCount;
  private int _channelRemoveCount;
  private int _entityUpdateCount;
  private int _entityRemoveCount;
  private int _entityLinkCount;

  MessageResponse( final int schemaId,
                   @Nonnull final ServerToClientMessage message,
                   @Nullable final RequestEntry request )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> null == request || ( (Integer) request.getRequestId() ).equals( message.getRequestId() ),
                 () -> "Replicant-0011: Response message specified requestId '" + message.getRequestId() +
                       "' but request specified requestId '" + Objects.requireNonNull( request ).getRequestId() +
                       "'." );
    }
    _schemaId = schemaId;
    _message = Objects.requireNonNull( message );
    _request = request;
    _entityChangeIndex = 0;
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

  @Nullable
  RequestEntry getRequest()
  {
    return _request;
  }

  boolean areEntityChangesPending()
  {
    if ( UpdateMessage.TYPE.equals( _message.getType() ) )
    {
      final UpdateMessage message = (UpdateMessage) _message;
      return message.hasEntityChanges() && _entityChangeIndex < message.getEntityChanges().length;
    }
    else
    {
      return false;
    }
  }

  boolean needsChannelChangesProcessed()
  {
    if ( UpdateMessage.TYPE.equals( _message.getType() ) )
    {
      final UpdateMessage message = (UpdateMessage) _message;
      return !_channelActionsProcessed && (
        message.hasChannels() && 0 != message.getChannels().length ||
        message.hasFilteredChannels() && 0 != message.getFilteredChannels().length
      );
    }
    else
    {
      return false;
    }
  }

  void setParsedChannelChanges( @Nonnull final List<ChannelChangeDescriptor> parsedChannelChanges )
  {
    _parsedChannelChanges = Objects.requireNonNull( parsedChannelChanges );
  }

  @Nonnull
  List<ChannelChangeDescriptor> getChannelChanges()
  {
    assert UpdateMessage.TYPE.equals( _message.getType() );
    final UpdateMessage changeSet = (UpdateMessage) _message;
    assert changeSet.hasChannels() || changeSet.hasFilteredChannels();
    if ( null == _parsedChannelChanges )
    {
      _parsedChannelChanges = toChannelChanges( changeSet );
    }
    return _parsedChannelChanges;
  }

  void markChannelActionsProcessed()
  {
    _channelActionsProcessed = true;
  }

  @Nullable
  EntityChange nextEntityChange()
  {
    if ( areEntityChangesPending() )
    {
      final EntityChange change = ( (UpdateMessage) _message ).getEntityChanges()[ _entityChangeIndex ];
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
      _entitiesToLink.add( (Linkable) entity );
    }
    _entitiesChanged.add( entity );
  }

  boolean areEntityLinksPending()
  {
    return null != _entitiesToLink && !_entitiesToLink.isEmpty();
  }

  @Nullable
  Linkable nextEntityToLink()
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

  boolean areEntityUpdateActionsPending()
  {
    return !_entitiesChanged.isEmpty();
  }

  @Nullable
  Object nextEntityToPostAction()
  {
    if ( areEntityUpdateActionsPending() )
    {
      return _entitiesChanged.remove();
    }
    else
    {
      return null;
    }
  }

  void completePostActions()
  {
    _entitiesChanged.clear();
  }

  @Nonnull
  ServerToClientMessage getMessage()
  {
    return _message;
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
    return new DataLoadStatus( _message.getRequestId(),
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
      return "MessageResponse[" +
             "Type=" + _message.getType() +
             ",RequestId=" + _message.getRequestId() +
             ",ChangeIndex=" + _entityChangeIndex +
             ",EntitiesToLink.size=" + ( null == _entitiesToLink ? 0 : _entitiesToLink.size() ) +
             "]";
    }
    else
    {
      return super.toString();
    }
  }

  @Nonnull
  private List<ChannelChangeDescriptor> toChannelChanges( @Nonnull final UpdateMessage changeSet )
  {
    final List<ChannelChangeDescriptor> changes = new ArrayList<>();

    if ( changeSet.hasChannels() )
    {
      for ( final String channelChange : changeSet.getChannels() )
      {
        changes.add( ChannelChangeDescriptor.from( _schemaId, channelChange ) );
      }
    }
    if ( changeSet.hasFilteredChannels() )
    {
      for ( final ChannelChange channelChange : changeSet.getFilteredChannels() )
      {
        changes.add( ChannelChangeDescriptor.from( _schemaId, channelChange ) );
      }
    }
    return changes;
  }

  boolean areOrphanSubscriptionsRemoved()
  {
    return _orphanSubscriptionRemoved;
  }

  void markOrphanSubscriptionsRemoved()
  {
    _orphanSubscriptionRemoved = true;
  }
}
