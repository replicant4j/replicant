package org.realityforge.replicant.client;

import arez.annotations.ArezComponent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;

/**
 * A class that records the subscriptions to graphs and entities.
 */
@Singleton
@ArezComponent( allowEmpty = true )
public abstract class EntitySubscriptionManager
{
  //Graph => InstanceID
  private final HashMap<Enum, Map<Object, ChannelSubscriptionEntry>> _instanceSubscriptions = new HashMap<>();

  //Graph => Type
  private final HashMap<Enum, ChannelSubscriptionEntry> _typeSubscriptions = new HashMap<>();

  // Entity map: Type => ID
  private final HashMap<Class<?>, Map<Object, EntitySubscriptionEntry>> _entityMapping = new HashMap<>();

  /**
   * Return the collection of current type subscriptions.
   * These keys can be directly used to unsubscribe from the graph.
   */
  @Nonnull
  public Set<Enum> getTypeSubscriptions()
  {
    return Collections.unmodifiableSet( _typeSubscriptions.keySet() );
  }

  /**
   * Return the collection of enums that represent instance subscriptions.
   * These can be used to further interrogate the EntitySubscriptionManager
   * to retrieve the set of instance subscriptions.
   */
  @Nonnull
  public Set<Enum> getInstanceSubscriptionKeys()
  {
    return Collections.unmodifiableSet( _instanceSubscriptions.keySet() );
  }

  /**
   * Return the collection of instance subscriptions for graph.
   */
  @Nonnull
  public Set<Object> getInstanceSubscriptions( @Nonnull final Enum graph )
  {
    final Map<Object, ChannelSubscriptionEntry> map = _instanceSubscriptions.get( graph );
    if ( null == map )
    {
      return Collections.emptySet();
    }
    else
    {
      return Collections.unmodifiableSet( map.keySet() );
    }
  }

  /**
   * Record a subscription for specified graph.
   *
   * @param address              the graph.
   * @param filter               the filter if subscription is filterable.
   * @param explicitSubscription if subscription was explicitly requested by the client.
   * @return the subscription entry.
   * @throws IllegalStateException if graph already subscribed to.
   */
  @Nonnull
  public final ChannelSubscriptionEntry recordSubscription( @Nonnull final ChannelAddress address,
                                                            @Nullable final Object filter,
                                                            final boolean explicitSubscription )
    throws IllegalStateException
  {
    final ChannelSubscriptionEntry existing = findSubscription( address );
    if ( null == existing )
    {
      final ChannelSubscriptionEntry entry =
        ChannelSubscriptionEntry.create( Channel.create( address, filter ), explicitSubscription );
      final Object id = address.getId();
      if ( null == id )
      {
        _typeSubscriptions.put( address.getGraph(), entry );
      }
      else
      {
        _instanceSubscriptions.computeIfAbsent( address.getGraph(), k -> new HashMap<>() ).put( id, entry );
      }
      return entry;
    }
    else
    {
      throw new IllegalStateException( "Graph already subscribed: " + address );
    }
  }

  /**
   * Update subscription details for the specified graph.
   *
   * @param graph  the graph.
   * @param filter the filter being updated.
   * @return the subscription entry.
   * @throws IllegalStateException if graph already subscribed to.
   */
  @Nonnull
  public ChannelSubscriptionEntry updateSubscription( @Nonnull final ChannelAddress graph,
                                                      @Nullable final Object filter )
    throws IllegalStateException
  {
    final ChannelSubscriptionEntry subscription = getSubscription( graph );
    subscription.getChannel().setFilter( filter );
    return subscription;
  }

  /**
   * Return the subscription details for the specified graph if a subscription is recorded.
   *
   * @param graph the graph.
   * @return the subscription entry if it exists, null otherwise.
   */
  @Nullable
  public final ChannelSubscriptionEntry findSubscription( @Nonnull final ChannelAddress graph )
  {
    final Object id = graph.getId();
    if ( null == id )
    {
      return _typeSubscriptions.get( graph.getGraph() );
    }
    else
    {
      Map<Object, ChannelSubscriptionEntry> instanceMap = _instanceSubscriptions.get( graph.getGraph() );
      if ( null == instanceMap )
      {
        return null;
      }
      else
      {
        return instanceMap.get( id );
      }
    }
  }

  /**
   * Return the subscription details for the specified graph.
   *
   * @param graph the graph.
   * @return the subscription entry.
   * @throws IllegalArgumentException if no such subscription
   */
  @Nonnull
  public ChannelSubscriptionEntry getSubscription( @Nonnull final ChannelAddress graph )
    throws IllegalArgumentException
  {
    final ChannelSubscriptionEntry subscription = findSubscription( graph );
    if ( null == subscription )
    {
      throw new IllegalStateException( "Graph not subscribed: " + graph );
    }
    return subscription;
  }

  /**
   * Remove subscription details for specified graph.
   *
   * @param graph the graph.
   * @return the subscription entry.
   * @throws IllegalStateException if graph not subscribed to.
   */
  @Nonnull
  public final ChannelSubscriptionEntry removeSubscription( @Nonnull final ChannelAddress graph )
    throws IllegalStateException
  {
    final Object id = graph.getId();
    if ( null == id )
    {
      final ChannelSubscriptionEntry entry = _typeSubscriptions.remove( graph.getGraph() );
      if ( null == entry )
      {
        throw new IllegalStateException( "Graph not subscribed: " + graph );
      }
      return entry;
    }
    else
    {
      final Map<Object, ChannelSubscriptionEntry> instanceMap = _instanceSubscriptions.get( graph.getGraph() );
      if ( null == instanceMap )
      {
        throw new IllegalStateException( "Graph not subscribed: " + graph );
      }
      final ChannelSubscriptionEntry entry = instanceMap.remove( id );
      if ( null == entry )
      {
        throw new IllegalStateException( "Graph not subscribed: " + graph );
      }
      return entry;

    }
  }

  /**
   * Return the subscription details for entity.
   *
   * @param type the type of the entity.
   * @param id   the id of the entity.
   * @return the subscription entry.
   * @throws IllegalArgumentException if no such subscription
   */
  @Nonnull
  public EntitySubscriptionEntry getSubscription( @Nonnull final Class<?> type, @Nonnull final Object id )
  {
    final EntitySubscriptionEntry entityEntry = findSubscription( type, id );
    if ( null == entityEntry )
    {
      throw new IllegalStateException( "Entity not subscribed: " + type.getSimpleName() + "/" + id );
    }
    return entityEntry;
  }

  /**
   * Find the subscription details for entity.
   *
   * @param type the type of the entity.
   * @param id   the id of the entity.
   * @return the subscription entry if it exists, null otherwise.
   */
  @Nullable
  public EntitySubscriptionEntry findSubscription( @Nonnull final Class<?> type, @Nonnull final Object id )
  {
    return getEntityTypeMap( type ).get( id );
  }

  /**
   * Register specified entity as being part of specified graphs.
   *
   * Note: It is assumed that if an entity is part of a graph, they are always part of the graph.
   * This may not be true with filters but we can assume it for all other scenarios.
   *
   * @param type   the type of the entity.
   * @param id     the id of the entity.
   * @param graphs the graphs that the entity is part of.
   */
  public void updateEntity( @Nonnull final Class<?> type,
                            @Nonnull final Object id,
                            @Nonnull final ChannelAddress[] graphs )
  {
    final EntitySubscriptionEntry entityEntry = getEntitySubscriptions( type, id );
    for ( final ChannelAddress graph : graphs )
    {
      entityEntry
        .getRwGraphSubscriptions()
        .computeIfAbsent( graph, this::getSubscription )
        .getRwEntities()
        .computeIfAbsent( type, k -> new HashMap<>() )
        .putIfAbsent( id, entityEntry );
    }
  }

  /**
   * Disassociate entity from specified graph.
   *
   * Note: It is assumed that the caller will remove the entity from the subscription manager and
   * repository if there are no more subscriptions.
   *
   * @param type  the type of the entity.
   * @param id    the id of the entity.
   * @param graph the graph that the entity is to be disassociated from.
   * @return the entry representing entities subscription state.
   * @throws IllegalStateException if no such entity or the entity is not associated with the graph.
   */
  @Nonnull
  public EntitySubscriptionEntry removeEntityFromGraph( @Nonnull final Class<?> type,
                                                        @Nonnull final Object id,
                                                        @Nonnull final ChannelAddress graph )
    throws IllegalStateException
  {
    final EntitySubscriptionEntry entry = getEntitySubscriptions( type, id );
    final Map<ChannelAddress, ChannelSubscriptionEntry> subscriptions = entry.getRwGraphSubscriptions();
    final ChannelSubscriptionEntry graphEntry = subscriptions.remove( graph );
    if ( null == graphEntry )
    {
      final String message = "Unable to locate graph " + graph + " for entity " + type.getSimpleName() + "/" + id;
      throw new IllegalStateException( message );
    }
    removeEntityFromGraph( type, id, graphEntry );
    return entry;
  }

  /**
   * Remove entity and all associated subscriptions.
   *
   * @param type the type of the entity.
   * @param id   the id of the entity.
   */
  public void removeEntity( @Nonnull final Class<?> type, @Nonnull final Object id )
  {
    final Map<Object, EntitySubscriptionEntry> typeMap = _entityMapping.get( type );
    if ( null == typeMap )
    {
      return;
    }
    final EntitySubscriptionEntry entityEntry = typeMap.remove( id );
    if ( null == entityEntry )
    {
      return;
    }
    for ( final ChannelSubscriptionEntry entry : entityEntry.getRwGraphSubscriptions().values() )
    {
      removeEntityFromGraph( type, id, entry );
    }
  }

  private void removeEntityFromGraph( final Class<?> type, final Object id, final ChannelSubscriptionEntry entry )
  {
    final Map<Class<?>, Map<Object, EntitySubscriptionEntry>> map = entry.getRwEntities();
    final Map<Object, EntitySubscriptionEntry> typeMap = map.get( type );
    final EntitySubscriptionEntry removed = null != typeMap ? typeMap.remove( id ) : null;
    if ( null == removed )
    {
      final String message =
        "Unable to remove entity " + type.getSimpleName() + "/" + id + " from " + entry.getChannel().getAddress();
      throw new IllegalStateException( message );
    }
    if ( typeMap.isEmpty() )
    {
      map.remove( type );
    }
  }

  private EntitySubscriptionEntry getEntitySubscriptions( final Class<?> type, final Object id )
  {
    return getEntityTypeMap( type ).computeIfAbsent( id, k -> new EntitySubscriptionEntry( type, id ) );
  }

  @Nonnull
  private Map<Object, EntitySubscriptionEntry> getEntityTypeMap( @Nonnull final Class<?> type )
  {
    return _entityMapping.computeIfAbsent( type, k -> new HashMap<>() );
  }
}
