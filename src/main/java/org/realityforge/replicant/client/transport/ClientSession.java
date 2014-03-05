package org.realityforge.replicant.client.transport;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Client-side representation of session.
 * Includes a database of outstanding requests, a subscription database and a
 * collection of outstanding data actions to apply.
 *
 * @param <T> The enum type representing the different graphs.
 */
public abstract class ClientSession<T extends Enum>
{
  private final String _sessionID;
  private final Map<String, RequestEntry> _requests = new HashMap<String, RequestEntry>();
  private final Map<String, RequestEntry> _roRequests = Collections.unmodifiableMap( _requests );
  private int _requestID;

  //Graph => InstanceID
  private final HashMap<T, Map<Object, SubscriptionEntry<T>>> _instanceSubscriptions =
    new HashMap<T, Map<Object, SubscriptionEntry<T>>>();
  private final Map<T, Map<Object, SubscriptionEntry<T>>> _roInstanceSubscriptions =
    Collections.unmodifiableMap( _instanceSubscriptions );

  //Graph => Type
  private final HashMap<T, SubscriptionEntry<T>> _typeSubscriptions = new HashMap<T, SubscriptionEntry<T>>();
  private final Map<T, SubscriptionEntry<T>> _roTypeSubscriptions = Collections.unmodifiableMap( _typeSubscriptions );

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

  public ClientSession( @Nonnull final String sessionID )
  {
    _sessionID = sessionID;
  }

  @Nonnull
  public String getSessionID()
  {
    return _sessionID;
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
  public final RequestEntry newRequestRegistration( @Nonnull final String requestKey, @Nullable final String cacheKey, final boolean bulkLoad )
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
  protected final Map<T, Map<Object, SubscriptionEntry<T>>> getInstanceSubscriptions()
  {
    return _roInstanceSubscriptions;
  }

  /**
   * @return read-only map of type subscriptions.
   */
  protected final Map<T, SubscriptionEntry<T>> getTypeSubscriptions()
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
  protected final SubscriptionEntry<T> subscribeToTypeGraph( @Nonnull final T graph )
  {
    SubscriptionEntry<T> typeMap = findTypeGraphSubscription( graph );
    if ( null == typeMap )
    {
      final SubscriptionEntry<T> entry = new SubscriptionEntry<T>( graph, null );
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
  protected final SubscriptionEntry<T> findTypeGraphSubscription( @Nonnull final T graph )
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
  protected final SubscriptionEntry<T> subscribeToInstanceGraph( @Nonnull final T graph, @Nonnull final Object id )
  {
    Map<Object, SubscriptionEntry<T>> instanceMap = _instanceSubscriptions.get( graph );
    if ( null == instanceMap )
    {
      instanceMap = new HashMap<Object, SubscriptionEntry<T>>();
      _instanceSubscriptions.put( graph, instanceMap );
    }
    if ( !instanceMap.containsKey( id ) )
    {
      final SubscriptionEntry<T> entry = new SubscriptionEntry<T>( graph, id );
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
  protected final SubscriptionEntry<T> findInstanceGraphSubscription( @Nonnull final T graph, @Nonnull final Object id )
  {
    Map<Object, SubscriptionEntry<T>> instanceMap = _instanceSubscriptions.get( graph );
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
  protected final SubscriptionEntry<T> unsubscribeFromTypeGraph( @Nonnull final T graph )
  {
    final SubscriptionEntry<T> entry = _typeSubscriptions.remove( graph );
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
  protected final SubscriptionEntry<T> unsubscribeFromInstanceGraph( @Nonnull final T graph, @Nonnull final Object id )
  {
    final Map<Object, SubscriptionEntry<T>> instanceMap = _instanceSubscriptions.get( graph );
    if ( null == instanceMap )
    {
      return null;
    }
    final SubscriptionEntry<T> entry = instanceMap.remove( id );
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
