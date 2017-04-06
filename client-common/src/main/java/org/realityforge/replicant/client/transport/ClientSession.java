package org.realityforge.replicant.client.transport;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Client-side representation of session.
 * Includes a database of outstanding requests, a subscription database and a
 * collection of outstanding data actions to apply.
 */
public final class ClientSession
{
  @Nonnull
  private final DataLoaderService _dataLoaderService;
  private final String _sessionID;
  private final Map<String, RequestEntry> _requests = new HashMap<>();
  private final Map<String, RequestEntry> _roRequests = Collections.unmodifiableMap( _requests );
  private int _requestID;

  /**
   * Pending actions that will change the area of interest.
   */
  private final LinkedList<AreaOfInterestEntry> _pendingAreaOfInterestActions = new LinkedList<>();
  /**
   * The set of data load actions that still need to have the json parsed.
   */
  private final LinkedList<DataLoadAction> _pendingActions = new LinkedList<>();
  /**
   * The set of data load actions that have their json parsed. They are inserted into
   * this list according to their sequence.
   */
  private final LinkedList<DataLoadAction> _parsedActions = new LinkedList<>();
  /**
   * Sometimes a data load action occurs that is not initiated by the server. These do not
   * typically need to be sequenced and are prioritized above other actions.
   */
  private final LinkedList<DataLoadAction> _oobActions = new LinkedList<>();

  private int _lastRxSequence;

  public ClientSession( @Nonnull final DataLoaderService dataLoaderService, @Nonnull final String sessionID )
  {
    _dataLoaderService = Objects.requireNonNull( dataLoaderService );
    _sessionID = Objects.requireNonNull( sessionID );
  }

  @Nonnull
  public String getSessionID()
  {
    return _sessionID;
  }

  public void requestSubscribe( @Nonnull final Enum graph,
                                @Nullable final Object id,
                                @Nullable final String cacheKey,
                                @Nullable final Object filterParameter,
                                @Nullable final Runnable userAction )
  {
    enqueueAoiAction( graph, AreaOfInterestAction.ADD, cacheKey, id, filterParameter, userAction );
  }

  public void requestSubscriptionUpdate( @Nonnull final Enum graph,
                                         @Nullable final Object id,
                                         @Nullable final Object filterParameter,
                                         @Nullable final Runnable userAction )
  {
    enqueueAoiAction( graph, AreaOfInterestAction.UPDATE, null, id, filterParameter, userAction );
  }

  public void requestUnsubscribe( @Nonnull final Enum graph,
                                  @Nullable final Object id,
                                  @Nullable final Runnable userAction )
  {
    enqueueAoiAction( graph, AreaOfInterestAction.REMOVE, null, id, null, userAction );
  }

  private void enqueueAoiAction( @Nonnull final Enum graph,
                                 @Nonnull final AreaOfInterestAction action,
                                 @Nullable final String cacheKey,
                                 @Nullable final Object id,
                                 @Nullable final Object filterParameter,
                                 @Nullable final Runnable userAction )
  {
    _pendingAreaOfInterestActions.
      add( new AreaOfInterestEntry( graph, action, cacheKey, id, filterParameter, userAction ) );
    _dataLoaderService.scheduleDataLoad();
  }

  public final void enqueueDataLoad( @Nonnull final String rawJsonData )
  {
    getPendingActions().add( new DataLoadAction( Objects.requireNonNull( rawJsonData ), false ) );
    _dataLoaderService.scheduleDataLoad();
  }

  protected final void enqueueOOB( @Nonnull final String rawJsonData,
                                   @Nullable final Runnable runnable,
                                   final boolean bulkLoad )
  {
    final DataLoadAction action = new DataLoadAction( Objects.requireNonNull( rawJsonData ), true );
    action.setRunnable( runnable );
    action.setBulkLoad( bulkLoad );
    getOobActions().add( action );
    _dataLoaderService.scheduleDataLoad();
  }

  final LinkedList<AreaOfInterestEntry> getPendingAreaOfInterestActions()
  {
    return _pendingAreaOfInterestActions;
  }

  final LinkedList<DataLoadAction> getPendingActions()
  {
    return _pendingActions;
  }

  final LinkedList<DataLoadAction> getParsedActions()
  {
    return _parsedActions;
  }

  final LinkedList<DataLoadAction> getOobActions()
  {
    return _oobActions;
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
  public final RequestEntry newRequestRegistration( @Nonnull final String requestKey,
                                                    @Nullable final String cacheKey,
                                                    final boolean bulkLoad )
  {
    final RequestEntry entry = new RequestEntry( newRequestID(), requestKey, cacheKey, bulkLoad );
    _requests.put( entry.getRequestID(), entry );
    return entry;
  }

  @Nullable
  public final RequestEntry getRequest( @Nonnull final String requestID )
  {
    return _requests.get( requestID );
  }

  public Map<String, RequestEntry> getRequests()
  {
    return _roRequests;
  }

  public final boolean removeRequest( @Nonnull final String requestID )
  {
    return null != _requests.remove( requestID );
  }

  protected String newRequestID()
  {
    return String.valueOf( ++_requestID );
  }
}
