package org.realityforge.replicant.client.transport;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Client-side database to record the subscriptions present.
 *
 * @param <T> The enum type representing the different graphs.
 */
public abstract class AbstractSubscriptionManager<T extends Enum>
{
  //Graph => InstanceID
  private final HashMap<T, Map<Object, SubscriptionEntry<T>>> _instanceSubscriptions = new HashMap<>();
  private final Map<T, Map<Object, SubscriptionEntry<T>>> _roInstanceSubscriptions =
    Collections.unmodifiableMap( _instanceSubscriptions );

  //Graph => Type
  private final HashMap<T, SubscriptionEntry<T>> _typeSubscriptions = new HashMap<>();
  private final Map<T, SubscriptionEntry<T>> _roTypeSubscriptions = Collections.unmodifiableMap( _typeSubscriptions );

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
    SubscriptionEntry<T> typeMap = _typeSubscriptions.get( graph );
    if ( null == typeMap )
    {
      final SubscriptionEntry<T> entry = new SubscriptionEntry<>( graph, null );
      _typeSubscriptions.put( graph, entry );
      return entry;
    }
    else
    {
      return null;
    }
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
      instanceMap = new HashMap<>();
      _instanceSubscriptions.put( graph, instanceMap );
    }
    if ( !instanceMap.containsKey( id ) )
    {
      final SubscriptionEntry<T> entry = new SubscriptionEntry<>( graph, id );
      instanceMap.put( id, entry );
      return entry;
    }
    else
    {
      return null;
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
