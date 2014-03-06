package org.realityforge.replicant.client.transport;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.transport.AreaOfInterestAction.Action;

/**
 * Client-side representation of session.
 * Includes a database of outstanding requests, a subscription database and a
 * collection of outstanding data actions to apply.
 *
 * @param <G> The enum type representing the different graphs.
 */
public abstract class ClientSession<T extends ClientSession<T,G>, G extends Enum>
{
  @Nonnull
  private final AbstractDataLoaderService<T, G> _dataLoaderService;
  private final String _sessionID;
  private final Map<String, RequestEntry> _requests = new HashMap<String, RequestEntry>();
  private final Map<String, RequestEntry> _roRequests = Collections.unmodifiableMap( _requests );
  private int _requestID;

  //Graph => InstanceID
  private final HashMap<G, Map<Object, SubscriptionEntry<G>>> _instanceSubscriptions =
    new HashMap<G, Map<Object, SubscriptionEntry<G>>>();
  private final Map<G, Map<Object, SubscriptionEntry<G>>> _roInstanceSubscriptions =
    Collections.unmodifiableMap( _instanceSubscriptions );

  //Graph => Type
  private final HashMap<G, SubscriptionEntry<G>> _typeSubscriptions = new HashMap<G, SubscriptionEntry<G>>();
  private final Map<G, SubscriptionEntry<G>> _roTypeSubscriptions = Collections.unmodifiableMap( _typeSubscriptions );

  /**
   * Pending actions that will change the area of interest.
   */
  private final LinkedList<AreaOfInterestAction<G>> _pendingAreaOfInterestActions =
    new LinkedList<AreaOfInterestAction<G>>();
  /**
   * The set of data load actions that still need to have the json parsed.
   */
  private final LinkedList<DataLoadAction> _pendingActions = new LinkedList<DataLoadAction>();
  /**
   * The set of data load actions that have their json parsed. They are inserted into
   * this list according to their sequence.
   */
  private final LinkedList<DataLoadAction> _parsedActions = new LinkedList<DataLoadAction>();
  /**
   * Sometimes a data load action occurs that is not initiated by the server. These do not
   * typically need to be sequenced and are prioritized above other actions.
   */
  private final LinkedList<DataLoadAction> _oobActions = new LinkedList<DataLoadAction>();

  private int _lastRxSequence;

  protected ClientSession( @Nonnull final AbstractDataLoaderService<T, G> dataLoaderService,
                           @Nonnull final String sessionID )
  {
    _dataLoaderService = dataLoaderService;
    _sessionID = sessionID;
  }

  @Nonnull
  public String getSessionID()
  {
    return _sessionID;
  }

  protected void subscribe( @Nonnull final G graph,
                            @Nullable final Object id,
                            @Nonnull final String cacheKey,
                            @Nullable final Object filterParameter,
                            @Nullable final Runnable userAction )
  {
    enqueueAoiAction( graph, Action.ADD, cacheKey, id, filterParameter, userAction );
  }

  protected void updateSubscription( @Nonnull final G graph,
                                     @Nullable final Object id,
                                     @Nullable final Object filterParameter,
                                     @Nullable final Runnable userAction )
  {
    enqueueAoiAction( graph, Action.UPDATE, null, id, filterParameter, userAction );
  }

  protected void unsubscribe( @Nonnull final G graph,
                              @Nullable final Object id,
                              @Nullable final Runnable userAction )
  {
    enqueueAoiAction( graph, Action.REMOVE, null, id, null, userAction );
  }

  private void enqueueAoiAction( @Nonnull final G graph,
                                 @Nonnull final Action action,
                                 @Nullable final String cacheKey,
                                 @Nullable final Object id,
                                 @Nullable final Object filterParameter,
                                 @Nullable final Runnable userAction )
  {
    _pendingAreaOfInterestActions.
      add( new AreaOfInterestAction<G>( graph, action, cacheKey, id, filterParameter, userAction ) );
    _dataLoaderService.scheduleDataLoad();
  }

  @SuppressWarnings( "ConstantConditions" )
  public final void enqueueDataLoad( @Nonnull final String rawJsonData )
  {
    if ( null == rawJsonData )
    {
      throw new IllegalStateException( "null == rawJsonData" );
    }
    getPendingActions().add( new DataLoadAction( rawJsonData, false ) );
    _dataLoaderService.scheduleDataLoad();
  }

  @SuppressWarnings( "ConstantConditions" )
  protected final void enqueueOOB( @Nonnull final String rawJsonData,
                                   @Nullable final Runnable runnable,
                                   final boolean bulkLoad )
  {
    if ( null == rawJsonData )
    {
      throw new IllegalStateException( "null == rawJsonData" );
    }
    final DataLoadAction action = new DataLoadAction( rawJsonData, true );
    action.setRunnable( runnable );
    action.setBulkLoad( bulkLoad );
    getOobActions().add( action );
    _dataLoaderService.scheduleDataLoad();
  }

  final LinkedList<AreaOfInterestAction<G>> getPendingAreaOfInterestActions()
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


  /**
   * @return read-only map of instance subscriptions.
   */
  protected final Map<G, Map<Object, SubscriptionEntry<G>>> getInstanceSubscriptions()
  {
    return _roInstanceSubscriptions;
  }

  /**
   * @return read-only map of type subscriptions.
   */
  protected final Map<G, SubscriptionEntry<G>> getTypeSubscriptions()
  {
    return _roTypeSubscriptions;
  }

  /**
   * Subscribe to graph containing types.
   *
   * @param graph the graph to subscribe to.
   * @return the subscription entry if this was the first subscription for graph, null otherwise.
   */
  @Nullable
  final SubscriptionEntry<G> newTypeGraphSubscription( @Nonnull final G graph )
  {
    SubscriptionEntry<G> typeMap = findTypeGraphSubscription( graph );
    if ( null == typeMap )
    {
      final SubscriptionEntry<G> entry = new SubscriptionEntry<G>( graph, null );
      _typeSubscriptions.put( graph, entry );
      return entry;
    }
    else
    {
      return null;
    }
  }

  /**
   * Find subscription for type graph if it exists.
   *
   * @param graph the graph.
   * @return the subscription entry if it exists, null otherwise.
   */
  @Nullable
  protected final SubscriptionEntry<G> findTypeGraphSubscription( @Nonnull final G graph )
  {
    return _typeSubscriptions.get( graph );
  }

  /**
   * Subscribe to graph rooted at an instance.
   *
   * @param graph the graph to subscribe to.
   * @param id    the id of the root object.
   * @return the subscription entry if this was the first subscription for graph, null otherwise.
   */
  @Nullable
  final SubscriptionEntry<G> newInstanceGraphSubscription( @Nonnull final G graph, @Nonnull final Object id )
  {
    Map<Object, SubscriptionEntry<G>> instanceMap = _instanceSubscriptions.get( graph );
    if ( null == instanceMap )
    {
      instanceMap = new HashMap<Object, SubscriptionEntry<G>>();
      _instanceSubscriptions.put( graph, instanceMap );
    }
    if ( !instanceMap.containsKey( id ) )
    {
      final SubscriptionEntry<G> entry = new SubscriptionEntry<G>( graph, id );
      instanceMap.put( id, entry );
      return entry;
    }
    else
    {
      return null;
    }
  }

  /**
   * Find a graph rooted at an instance.
   *
   * @param graph the graph to look for.
   * @param id    the id of the root object.
   * @return the subscription entry if it exists, null otherwise.
   */
  @Nullable
  protected final SubscriptionEntry<G> findInstanceGraphSubscription( @Nonnull final G graph,
                                                                      @Nonnull final Object id )
  {
    Map<Object, SubscriptionEntry<G>> instanceMap = _instanceSubscriptions.get( graph );
    if ( null == instanceMap )
    {
      return null;
    }
    else
    {
      return instanceMap.get( id );
    }
  }

  /**
   * Unsubscribe from graph containing types.
   *
   * @param graph the graph to unsubscribe from.
   * @return the subscription entry if subscribed, null otherwise.
   */
  @Nullable
  final SubscriptionEntry<G> unsubscribeFromTypeGraph( @Nonnull final G graph )
  {
    final SubscriptionEntry<G> entry = _typeSubscriptions.remove( graph );
    if ( null != entry )
    {
      entry.markDeregisterInProgress();
      return entry;
    }
    else
    {
      return null;
    }
  }

  /**
   * Unsubscribe from graph rooted at an instance.
   *
   * @param graph the graph to unsubscribe from.
   * @param id    the id of the root object.
   * @return the subscription entry if subscribed, null otherwise.
   */
  final SubscriptionEntry<G> unsubscribeFromInstanceGraph( @Nonnull final G graph, @Nonnull final Object id )
  {
    final Map<Object, SubscriptionEntry<G>> instanceMap = _instanceSubscriptions.get( graph );
    if ( null == instanceMap )
    {
      return null;
    }
    final SubscriptionEntry<G> entry = instanceMap.remove( id );
    if ( null != entry )
    {
      entry.markDeregisterInProgress();
      return entry;
    }
    else
    {
      return null;
    }
  }
}
