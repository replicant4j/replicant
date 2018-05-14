package replicant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.spy.RequestCompletedEvent;

/**
 * Connection state used by the connector to manage connection to backend system.
 * This includes a list of pending requests, pending messages that needs to be applied
 * to the local state etc.
 */
public final class Connection
{
  //TODO: Make this package access after all classes migrated to replicant package

  private static final Logger LOG = Logger.getLogger( Connection.class.getName() );
  private static final Level LOG_LEVEL = Level.INFO;

  /**
   * The containing Connector.
   */
  @Nonnull
  private final Connector _connector;
  /**
   * A unique identifier for the connection, typically supplied by the backend.
   */
  private final String _connectionId;
  /**
   * A map containing the rpc requests that are in progress.
   */
  private final Map<String, RequestEntry> _requests = new HashMap<>();
  /**
   * The id of the last rpc request sent to the server.
   */
  private int _requestId;

  /**
   * Pending actions that will change the area of interest.
   */
  private final LinkedList<AreaOfInterestRequest> _pendingAreaOfInterestRequests = new LinkedList<>();
  /**
   * This contains the messages that are not yet parsed. These need to be parsed
   * and placed in the {@link #_pendingResponses} list in order before they will
   * progress.
   */
  private final LinkedList<MessageResponse> _unparsedResponses = new LinkedList<>();
  /**
   * This list contains the messages that have been parsed and are ordered according to their sequence
   * in ascending order.
   */
  private final LinkedList<MessageResponse> _pendingResponses = new LinkedList<>();
  /**
   * Sometimes a "Message" occurs that is not initiated by the server. These do not
   * typically need to be sequenced and are prioritized above other actions. This is only
   * really used in responding to requests with cached value.
   */
  private final LinkedList<MessageResponse> _outOfBandResponses = new LinkedList<>();

  private int _lastRxSequence;

  /**
   * The current message being processed.
   */
  @Nullable
  private MessageResponse _currentMessageResponse;
  /**
   * The current requests being processed. This list can contain multiple requests if they
   * are candidates for bulk actions.
   */
  @Nonnull
  private List<AreaOfInterestRequest> _currentAreaOfInterestRequests = new ArrayList<>();

  public Connection( @Nonnull final Connector connector, @Nonnull final String connectionId )
  {
    _connector = Objects.requireNonNull( connector );
    _connectionId = Objects.requireNonNull( connectionId );
  }

  @Nonnull
  public String getConnectionId()
  {
    return _connectionId;
  }

  public final void requestSubscribe( @Nonnull final ChannelAddress address, @Nullable final Object filterParameter )
  {
    enqueueAreaOfInterestRequest( address, AreaOfInterestAction.ADD, filterParameter );
  }

  public void requestSubscriptionUpdate( @Nonnull final ChannelAddress address,
                                         @Nullable final Object filterParameter )
  {
    enqueueAreaOfInterestRequest( address, AreaOfInterestAction.UPDATE, filterParameter );
  }

  public void requestUnsubscribe( @Nonnull final ChannelAddress address )
  {
    enqueueAreaOfInterestRequest( address, AreaOfInterestAction.REMOVE, null );
  }

  private void enqueueAreaOfInterestRequest( @Nonnull final ChannelAddress descriptor,
                                             @Nonnull final AreaOfInterestAction action,
                                             @Nullable final Object filterParameter )
  {
    getPendingAreaOfInterestRequests().add( new AreaOfInterestRequest( descriptor, action, filterParameter ) );
  }

  public void enqueueResponse( @Nonnull final String rawJsonData )
  {
    getUnparsedResponses().add( new MessageResponse( rawJsonData ) );
  }

  public void enqueueOutOfBandResponse( @Nonnull final String rawJsonData,
                                        @Nonnull final SafeProcedure oobCompletionAction )
  {
    getOutOfBandResponses().add( new MessageResponse( rawJsonData, oobCompletionAction ) );
  }

  public LinkedList<AreaOfInterestRequest> getPendingAreaOfInterestRequests()
  {
    return _pendingAreaOfInterestRequests;
  }

  public LinkedList<MessageResponse> getUnparsedResponses()
  {
    return _unparsedResponses;
  }

  public LinkedList<MessageResponse> getParsedResponses()
  {
    return _pendingResponses;
  }

  public LinkedList<MessageResponse> getOutOfBandResponses()
  {
    return _outOfBandResponses;
  }

  public int getLastRxSequence()
  {
    return _lastRxSequence;
  }

  public void setLastRxSequence( final int lastRxSequence )
  {
    _lastRxSequence = lastRxSequence;
  }

  @Nonnull
  public final RequestEntry newRequest( @Nullable final String name, @Nullable final String cacheKey )
  {
    final RequestEntry entry = new RequestEntry( nextRequestId(), name, cacheKey );
    _requests.put( entry.getRequestId(), entry );
    if ( LOG.isLoggable( LOG_LEVEL ) )
    {
      LOG.log( LOG_LEVEL, "Created request " + entry );
    }
    return entry;
  }

  public final void completeRequest( @Nonnull final RequestEntry request,
                                     @Nonnull final SafeProcedure completionAction )
  {
    if ( request.isExpectingResults() && !request.haveResultsArrived() )
    {
      request.setCompletionAction( completionAction );
    }
    else
    {
      completionAction.call();
      removeRequest( request.getRequestId() );
    }
    if ( Replicant.areSpiesEnabled() && _connector.getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      _connector
        .getReplicantContext()
        .getSpy()
        .reportSpyEvent( new RequestCompletedEvent( _connector.getSystemType(),
                                                    request.getRequestId(),
                                                    request.getName(),
                                                    request.isNormalCompletion(),
                                                    request.isExpectingResults(),
                                                    request.haveResultsArrived() ) );
    }
  }

  @Nullable
  public RequestEntry getRequest( @Nonnull final String requestID )
  {
    return _requests.get( requestID );
  }

  public Map<String, RequestEntry> getRequests()
  {
    return _requests;
  }

  public boolean removeRequest( @Nonnull final String requestID )
  {
    return null != _requests.remove( requestID );
  }

  private String nextRequestId()
  {
    return String.valueOf( ++_requestId );
  }

  @Nullable
  public MessageResponse getCurrentMessageResponse()
  {
    return _currentMessageResponse;
  }

  public void setCurrentMessageResponse( @Nullable final MessageResponse currentMessageResponse )
  {
    _currentMessageResponse = currentMessageResponse;
  }

  @Nonnull
  public List<AreaOfInterestRequest> getCurrentAreaOfInterestRequests()
  {
    return _currentAreaOfInterestRequests;
  }
}
