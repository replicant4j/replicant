package replicant;

import arez.Disposable;
import arez.component.CollectionsUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.messages.ServerToClientMessage;
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
  private String _connectionId;
  /**
   * A map containing the rpc requests that are in progress.
   */
  private final Map<Integer, RequestEntry> _requests = new HashMap<>();
  /**
   * The id of the last rpc request transmitted to the server.
   */
  private int _lastTxRequestId;
  /**
   * The id of the last sync request responded to by the server.
   */
  private int _lastRxSyncRequestId;
  /**
   * Pending actions that will change the area of interest.
   */
  private final LinkedList<AreaOfInterestRequest> _pendingAreaOfInterestRequests = new LinkedList<>();
  /**
   * This list contains the messages from the server.
   */
  private final LinkedList<MessageResponse> _pendingResponses = new LinkedList<>();
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

  Connection( @Nonnull final Connector connector )
  {
    _connector = Objects.requireNonNull( connector );
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

  void setConnectionId( @Nonnull final String connectionId )
  {
    _connectionId = Objects.requireNonNull( connectionId );
  }

  @Nonnull
  Connector getConnector()
  {
    return _connector;
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

  void enqueueResponse( @Nonnull final ServerToClientMessage message )
  {
    _pendingResponses.add( new MessageResponse( _connector.getSchema().getId(), message, _requests.get( message.getRequestId() ) ) );
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
    final int requestId = ++_lastTxRequestId;
    final RequestEntry request = new RequestEntry( requestId, name );
    _requests.put( requestId, request );
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
        removeRequest( request.getRequestId(), false );
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

  //TODO: Pings should pass in correct isSyncRequest
  void removeRequest( final int requestId, final boolean isSyncRequest )
  {
    final boolean removed = null != _requests.remove( requestId );
    if ( isSyncRequest )
    {
      _lastRxSyncRequestId = requestId;
    }
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> removed,
                 () -> "Replicant-0067: Attempted to remove request with id " + requestId +
                       " from connection with id '" + getConnectionId() + "' but no such request exists." );
    }
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
   * Return true if there are no pending requests and the last request was a "SYNC" request.
   */
  boolean syncComplete()
  {
    return null != _connectionId && _lastTxRequestId == _lastRxSyncRequestId;
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

    //Step: Retrieve the action from the parsed queue
    if ( !_pendingResponses.isEmpty() )
    {
      _currentMessageResponse = _pendingResponses.remove();
      return true;
    }
    else
    {
      return false;
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

  List<MessageResponse> getPendingResponses()
  {
    return CollectionsUtil.wrap( _pendingResponses );
  }

  List<AreaOfInterestRequest> getPendingAreaOfInterestRequests()
  {
    return CollectionsUtil.wrap( _pendingAreaOfInterestRequests );
  }
}
