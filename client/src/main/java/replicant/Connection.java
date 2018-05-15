package replicant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.spy.RequestCompletedEvent;
import replicant.spy.RequestStartedEvent;
import static org.realityforge.braincheck.Guards.*;

/**
 * Connection state used by the connector to manage connection to backend system.
 * This includes a list of pending requests, pending messages that needs to be applied
 * to the local state etc.
 */
public final class Connection
{
  //TODO: Make this package access after all classes migrated to replicant package
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
  private int _lastRequestId;

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

  public final void requestSubscribe( @Nonnull final ChannelAddress address, @Nullable final Object filter )
  {
    enqueueAreaOfInterestRequest( address, AreaOfInterestAction.ADD, filter );
  }

  public void requestSubscriptionUpdate( @Nonnull final ChannelAddress address, @Nullable final Object filter )
  {
    enqueueAreaOfInterestRequest( address, AreaOfInterestAction.UPDATE, filter );
  }

  public void requestUnsubscribe( @Nonnull final ChannelAddress address )
  {
    enqueueAreaOfInterestRequest( address, AreaOfInterestAction.REMOVE, null );
  }

  private void enqueueAreaOfInterestRequest( @Nonnull final ChannelAddress descriptor,
                                             @Nonnull final AreaOfInterestAction action,
                                             @Nullable final Object filter )
  {
    getPendingAreaOfInterestRequests().add( new AreaOfInterestRequest( descriptor, action, filter ) );
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

  public LinkedList<MessageResponse> getPendingResponses()
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

  /**
   * Return true if an area of interest request with specified parameters is pending or being processed.
   * When the action parameter is DELETE the filter parameter is ignored.
   */
  public boolean isAreaOfInterestRequestPending( @Nonnull final AreaOfInterestAction action,
                                                 @Nonnull final ChannelAddress address,
                                                 @Nullable final Object filter )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> action != AreaOfInterestAction.REMOVE || null == filter,
                 () -> "Replicant-0025: Connection.isAreaOfInterestRequestPending passed a REMOVE " +
                       "request for address '" + address + "' with a non-null filter '" + filter + "'." );
    }
    return
      _currentAreaOfInterestRequests.stream().anyMatch( a -> a.match( action, address, filter ) ) ||
      _pendingAreaOfInterestRequests.stream().anyMatch( a -> a.match( action, address, filter ) );
  }

  /**
   * Return the index of last matching AreaOfInterestAction in pending aoi request list.
   */
  public int lastIndexOfPendingAreaOfInterestRequest( @Nonnull final AreaOfInterestAction action,
                                                      @Nonnull final ChannelAddress address,
                                                      @Nullable final Object filter )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> action != AreaOfInterestAction.REMOVE || null == filter,
                 () -> "Replicant-0024: Connection.lastIndexOfPendingAreaOfInterestRequest passed a REMOVE " +
                       "request for address '" + address + "' with a non-null filter '" + filter + "'." );
    }
    int index = _pendingAreaOfInterestRequests.size();

    final Iterator<AreaOfInterestRequest> iterator = _pendingAreaOfInterestRequests.descendingIterator();
    while ( iterator.hasNext() )
    {
      final AreaOfInterestRequest request = iterator.next();
      if ( request.match( action, address, filter ) )
      {
        return index;
      }
      index -= 1;
    }
    if ( _currentAreaOfInterestRequests.stream().anyMatch( a -> a.match( action, address, filter ) ) )
    {
      return 0;
    }

    return -1;
  }

  @Nonnull
  public final RequestEntry newRequest( @Nullable final String name, @Nullable final String cacheKey )
  {
    final RequestEntry request = new RequestEntry( nextRequestId(), name, cacheKey );
    _requests.put( request.getRequestId(), request );
    if ( Replicant.areSpiesEnabled() && _connector.getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      _connector
        .getReplicantContext()
        .getSpy()
        .reportSpyEvent( new RequestStartedEvent( _connector.getSystemType(),
                                                  request.getRequestId(),
                                                  request.getName() ) );
    }
    return request;
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
    return String.valueOf( ++_lastRequestId );
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

  /**
   * Mark all the current AreaOfInterest requests as complete and clear out the current requests list
   */
  public void completeAreaOfInterestRequest()
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> !_currentAreaOfInterestRequests.isEmpty(),
                 () -> "Replicant-0023: Connection.completeAreaOfInterestRequest() invoked when there is no current AreaOfInterest requests." );
    }
    _currentAreaOfInterestRequests.forEach( AreaOfInterestRequest::markAsComplete );
    _currentAreaOfInterestRequests.clear();
  }
}
