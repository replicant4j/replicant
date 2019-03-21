package replicant;

import arez.Disposable;
import arez.component.CollectionsUtil;
import java.util.ArrayList;
import java.util.Collections;
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
final class Connection
  implements Disposable
{
  /**
   * The containing Connector.
   */
  @Nonnull
  private final Connector _connector;
  /**
   * The interface between Transport and Connector infrastructure.
   */
  @Nonnull
  private final TransportContextImpl _transportContext;
  /**
   * A unique identifier for the connection, typically supplied by the backend.
   */
  private final String _connectionId;
  /**
   * A map containing the rpc requests that are in progress.
   */
  private final Map<Integer, RequestEntry> _requests = new HashMap<>();
  /**
   * The id of the last rpc request transmitted to the server.
   */
  private int _lastTxRequestId;
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

  Connection( @Nonnull final Connector connector, @Nonnull final String connectionId )
  {
    _connector = Objects.requireNonNull( connector );
    _connectionId = Objects.requireNonNull( connectionId );
    _transportContext = new TransportContextImpl( connector );
  }

  @Override
  public void dispose()
  {
    _transportContext.dispose();
  }

  @Override
  public boolean isDisposed()
  {
    return _transportContext.isDisposed();
  }

  @Nonnull
  String getConnectionId()
  {
    return _connectionId;
  }

  @Nonnull
  Transport.Context getTransportContext()
  {
    return _transportContext;
  }

  void requestSubscribe( @Nonnull final ChannelAddress address, @Nullable final Object filter )
  {
    enqueueAreaOfInterestRequest( address, AreaOfInterestRequest.Type.ADD, filter );
  }

  void requestSubscriptionUpdate( @Nonnull final ChannelAddress address, @Nullable final Object filter )
  {
    enqueueAreaOfInterestRequest( address, AreaOfInterestRequest.Type.UPDATE, filter );
  }

  void requestUnsubscribe( @Nonnull final ChannelAddress address )
  {
    enqueueAreaOfInterestRequest( address, AreaOfInterestRequest.Type.REMOVE, null );
  }

  private void enqueueAreaOfInterestRequest( @Nonnull final ChannelAddress address,
                                             @Nonnull final AreaOfInterestRequest.Type action,
                                             @Nullable final Object filter )
  {
    _pendingAreaOfInterestRequests.add( new AreaOfInterestRequest( address, action, filter ) );
  }

  void enqueueResponse( @Nonnull final String rawJsonData )
  {
    _unparsedResponses.add( new MessageResponse( rawJsonData ) );
  }

  void enqueueOutOfBandResponse( @Nonnull final String rawJsonData,
                                 @Nonnull final SafeProcedure oobCompletionAction )
  {
    _outOfBandResponses.add( new MessageResponse( rawJsonData, oobCompletionAction ) );
  }

  /**
   * Take the current message and queue it in the pending messages lists.
   * This is invoked when the message has been parsed and now has enough
   * information to sequence message.
   */
  void queueCurrentResponse()
  {
    assert null != _currentMessageResponse;
    _pendingResponses.add( _currentMessageResponse );
    Collections.sort( _pendingResponses );
    _currentMessageResponse = null;
  }

  /**
   * Return true if an area of interest request with specified parameters is pending or being processed.
   * When the action parameter is DELETE the filter parameter is ignored.
   */
  boolean isAreaOfInterestRequestPending( @Nonnull final AreaOfInterestRequest.Type action,
                                          @Nonnull final ChannelAddress address,
                                          @Nullable final Object filter )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> action != AreaOfInterestRequest.Type.REMOVE || null == filter,
                 () -> "Replicant-0025: Connection.isAreaOfInterestRequestPending passed a REMOVE " +
                       "request for address '" + address + "' with a non-null filter '" + filter + "'." );
    }
    return
      _currentAreaOfInterestRequests.stream().anyMatch( a -> a.match( action, address, filter ) ) ||
      _pendingAreaOfInterestRequests.stream().anyMatch( a -> a.match( action, address, filter ) );
  }

  /**
   * Return the index of last matching Type in pending aoi request list.
   */
  int lastIndexOfPendingAreaOfInterestRequest( @Nonnull final AreaOfInterestRequest.Type action,
                                               @Nonnull final ChannelAddress address,
                                               @Nullable final Object filter )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> action != AreaOfInterestRequest.Type.REMOVE || null == filter,
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
    else
    {
      return -1;
    }
  }

  @Nonnull
  final Request newRequest( @Nullable final String name )
  {
    final int requestId = nextRequestId();
    final RequestEntry request = new RequestEntry( requestId, name );
    _requests.put( requestId, request );
    _connector.recordLastTxRequestId( requestId );
    if ( Replicant.areSpiesEnabled() && _connector.getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      _connector
        .getReplicantContext()
        .getSpy()
        .reportSpyEvent( new RequestStartedEvent( _connector.getSchema().getId(),
                                                  _connector.getSchema().getName(),
                                                  request.getRequestId(),
                                                  request.getName() ) );
    }
    return new Request( this, request );
  }

  final void completeRequest( @Nonnull final RequestEntry request, @Nonnull final SafeProcedure completionAction )
  {
    if ( request.isExpectingResults() && !request.haveResultsArrived() )
    {
      request.setCompletionAction( completionAction );
    }
    else
    {
      if ( !request.isExpectingResults() )
      {
        removeRequest( request.getRequestId() );
      }
      completionAction.call();
    }
    if ( Replicant.areSpiesEnabled() && _connector.getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      _connector
        .getReplicantContext()
        .getSpy()
        .reportSpyEvent( new RequestCompletedEvent( _connector.getSchema().getId(),
                                                    _connector.getSchema().getName(),
                                                    request.getRequestId(),
                                                    request.getName(),
                                                    request.isNormalCompletion(),
                                                    request.isExpectingResults(),
                                                    request.haveResultsArrived() ) );
    }
  }

  @Nullable
  RequestEntry getRequest( final int requestId )
  {
    return _requests.get( requestId );
  }

  Map<Integer, RequestEntry> getRequests()
  {
    return _requests;
  }

  void removeRequest( final int requestId )
  {
    final boolean removed = null != _requests.remove( requestId );
    _connector.recordLastRxRequestId( requestId );
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> removed,
                 () -> "Replicant-0067: Attempted to remove request with id " + requestId +
                       " from connection with id '" + getConnectionId() + "' but no such request exists." );
    }
  }

  int getLastTxRequestId()
  {
    return _lastTxRequestId;
  }

  private int nextRequestId()
  {
    return ++_lastTxRequestId;
  }

  @Nullable
  MessageResponse getCurrentMessageResponse()
  {
    return _currentMessageResponse;
  }

  @Nonnull
  MessageResponse ensureCurrentMessageResponse()
  {
    assert null != _currentMessageResponse;
    return _currentMessageResponse;
  }

  /**
   * This method is invoked when there is no current MessageResponse to process
   * and we need to select a candidate message to be processed next step. It will
   * return true if there is another message to process, false otherwise.
   *
   * @return true if a message was selected, false otherwise.
   */
  boolean selectNextMessageResponse()
  {
    assert null == _currentMessageResponse;
    // Step: Retrieve any out of band actions
    if ( !_outOfBandResponses.isEmpty() )
    {
      _currentMessageResponse = _outOfBandResponses.removeFirst();
      return true;
    }

    //Step: Retrieve the action from the parsed queue
    if ( !_pendingResponses.isEmpty() )
    {
      _currentMessageResponse = _pendingResponses.remove();
      return true;
    }

    // Abort if there is no pending data load actions to take
    if ( _unparsedResponses.isEmpty() )
    {
      return false;
    }
    else
    {
      //Step: Retrieve the action from the un-parsed queue
      _currentMessageResponse = _unparsedResponses.remove();
      return true;
    }
  }

  void setCurrentMessageResponse( @Nullable final MessageResponse currentMessageResponse )
  {
    _currentMessageResponse = currentMessageResponse;
  }

  /**
   * Return the list of AreaOfInterest requests currently being processed. If there is none
   * currently being processed and there are pending requests then derive the next batch of
   * requests and set them as current.
   */
  @Nonnull
  List<AreaOfInterestRequest> getCurrentAreaOfInterestRequests()
  {
    if ( _currentAreaOfInterestRequests.isEmpty() && !_pendingAreaOfInterestRequests.isEmpty() )
    {
      final AreaOfInterestRequest first = _pendingAreaOfInterestRequests.removeFirst();
      _currentAreaOfInterestRequests.add( first );
      while ( _pendingAreaOfInterestRequests.size() > 0 &&
              canGroupRequests( first, _pendingAreaOfInterestRequests.get( 0 ) ) )
      {
        _currentAreaOfInterestRequests.add( _pendingAreaOfInterestRequests.removeFirst() );
      }
    }
    return CollectionsUtil.wrap( _currentAreaOfInterestRequests );
  }

  /**
   * Return true if the match request can be grouped with the template request and sent to the backend using a
   * single network message.
   */
  final boolean canGroupRequests( @Nonnull final AreaOfInterestRequest template,
                                  @Nonnull final AreaOfInterestRequest match )
  {
    final CacheService cacheService = _connector.getReplicantContext().getCacheService();
    return null != template.getAddress().getId() &&
           null != match.getAddress().getId() &&
           ( null == cacheService || null == cacheService.lookup( template.getAddress().getCacheKey() ) ) &&
           ( null == cacheService || null == cacheService.lookup( match.getAddress().getCacheKey() ) ) &&
           template.getType().equals( match.getType() ) &&
           template.getAddress().getChannelId() == match.getAddress().getChannelId() &&
           ( AreaOfInterestRequest.Type.REMOVE == match.getType() ||
             FilterUtil.filtersEqual( match.getFilter(), template.getFilter() ) );
  }

  /**
   * Mark all the current AreaOfInterest requests as complete and clear out the current requests list
   */
  void completeAreaOfInterestRequest()
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> !_currentAreaOfInterestRequests.isEmpty(),
                 () -> "Replicant-0023: Connection.completeAreaOfInterestRequest() invoked when there is no current AreaOfInterest requests." );
    }
    _currentAreaOfInterestRequests.forEach( AreaOfInterestRequest::markAsComplete );
    _currentAreaOfInterestRequests.clear();
  }

  void injectCurrentAreaOfInterestRequest( @Nonnull final AreaOfInterestRequest request )
  {
    _currentAreaOfInterestRequests.add( request );
  }

  void injectPendingResponses( @Nonnull final MessageResponse response )
  {
    _pendingResponses.add( response );
  }

  List<MessageResponse> getPendingResponses()
  {
    return CollectionsUtil.wrap( _pendingResponses );
  }

  List<MessageResponse> getUnparsedResponses()
  {
    return CollectionsUtil.wrap( _unparsedResponses );
  }

  List<MessageResponse> getOutOfBandResponses()
  {
    return CollectionsUtil.wrap( _outOfBandResponses );
  }

  List<AreaOfInterestRequest> getPendingAreaOfInterestRequests()
  {
    return CollectionsUtil.wrap( _pendingAreaOfInterestRequests );
  }
}
