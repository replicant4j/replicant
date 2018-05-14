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

  @Nonnull
  private final Connector _connector;
  private final String _connectionId;
  private final Map<String, RequestEntry> _requests = new HashMap<>();
  private int _requestID;

  /**
   * Pending actions that will change the area of interest.
   */
  private final LinkedList<AreaOfInterestRequest> _pendingAreaOfInterestRequests = new LinkedList<>();
  /**
   * The set of data load actions that still need to have the json parsed.
   */
  private final LinkedList<MessageResponse> _unparsedResponses = new LinkedList<>();
  /**
   * The set of data load actions that have their json parsed. They are inserted into
   * this list according to their sequence.
   */
  private final LinkedList<MessageResponse> _pendingResponses = new LinkedList<>();
  /**
   * Sometimes a data load action occurs that is not initiated by the server. These do not
   * typically need to be sequenced and are prioritized above other actions.
   */
  private final LinkedList<MessageResponse> _outOfBandResponses = new LinkedList<>();

  private int _lastRxSequence;

  /**
   * The current message action being processed.
   */
  @Nullable
  private MessageResponse _currentMessageResponse;
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
    return String.valueOf( ++_requestID );
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
