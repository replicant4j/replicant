package replicant;

import arez.component.RepositoryUtil;
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
import org.realityforge.anodoc.TestOnly;
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

  private void enqueueAreaOfInterestRequest( @Nonnull final ChannelAddress descriptor,
                                             @Nonnull final AreaOfInterestRequest.Type action,
                                             @Nullable final Object filter )
  {
    _pendingAreaOfInterestRequests.add( new AreaOfInterestRequest( descriptor, action, filter ) );
  }

  public void enqueueResponse( @Nonnull final String rawJsonData )
  {
    _unparsedResponses.add( new MessageResponse( rawJsonData ) );
  }

  public void enqueueOutOfBandResponse( @Nonnull final String rawJsonData,
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

  public int getLastRxSequence()
  {
    return _lastRxSequence;
  }

  void setLastRxSequence( final int lastRxSequence )
  {
    _lastRxSequence = lastRxSequence;
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
  public final RequestEntry newRequest( @Nullable final String name, @Nullable final String cacheKey )
  {
    final RequestEntry request = new RequestEntry( nextRequestId(), name, cacheKey );
    _requests.put( request.getRequestId(), request );
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
  public RequestEntry getRequest( @Nonnull final String requestID )
  {
    return _requests.get( requestID );
  }

  Map<String, RequestEntry> getRequests()
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
  public boolean selectNextMessageResponse()
  {
    assert null == _currentMessageResponse;
    // Step: Retrieve any out of band actions
    if ( !_outOfBandResponses.isEmpty() )
    {
      _currentMessageResponse = _outOfBandResponses.removeFirst();
      return true;
    }

    //Step: Retrieve the action from the parsed queue if it is the next in the sequence
    if ( !_pendingResponses.isEmpty() )
    {
      final MessageResponse response = _pendingResponses.get( 0 );
      if ( response.isOob() || _lastRxSequence + 1 == response.getChangeSet().getSequence() )
      {
        _currentMessageResponse = _pendingResponses.remove();
        return true;
      }
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
  public List<AreaOfInterestRequest> getCurrentAreaOfInterestRequests()
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
    return RepositoryUtil.toResults( _currentAreaOfInterestRequests );
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
           ( null == cacheService || null == cacheService.lookup( template.getCacheKey() ) ) &&
           ( null == cacheService || null == cacheService.lookup( match.getCacheKey() ) ) &&
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

  @TestOnly
  void injectCurrentAreaOfInterestRequest( @Nonnull final AreaOfInterestRequest request )
  {
    _currentAreaOfInterestRequests.add( request );
  }

  @TestOnly
  void injectPendingResponses( @Nonnull final MessageResponse response )
  {
    _pendingResponses.add( response );
  }

  @TestOnly
  List<MessageResponse> getPendingResponses()
  {
    return Collections.unmodifiableList( _pendingResponses );
  }

  @TestOnly
  List<MessageResponse> getUnparsedResponses()
  {
    return Collections.unmodifiableList( _unparsedResponses );
  }

  @TestOnly
  List<MessageResponse> getOutOfBandResponses()
  {
    return Collections.unmodifiableList( _outOfBandResponses );
  }

  @TestOnly
  List<AreaOfInterestRequest> getPendingAreaOfInterestRequests()
  {
    return Collections.unmodifiableList( _pendingAreaOfInterestRequests );
  }
}
