package org.realityforge.replicant.client.subscription;

import arez.Disposable;
import arez.annotations.ArezComponent;
import arez.annotations.Observable;
import arez.annotations.ObservableRef;
import arez.component.ComponentObservable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import org.realityforge.braincheck.BrainCheckConfig;
import org.realityforge.replicant.client.Channel;
import org.realityforge.replicant.client.ChannelAddress;
import static org.realityforge.braincheck.Guards.*;

/**
 * A class that records the subscriptions to channels and entities.
 */
@Singleton
@ArezComponent
public abstract class EntitySubscriptionManager
{
  //ChannelType => InstanceID
  private final HashMap<Enum, Map<Object, Subscription>> _instanceSubscriptions = new HashMap<>();

  //ChannelType => Type
  private final HashMap<Enum, Subscription> _typeSubscriptions = new HashMap<>();

  // Entity map: Type => ID
  private final Map<Class<?>, Map<Object, Entity>> _entities = new HashMap<>();

  @Nonnull
  public static EntitySubscriptionManager create()
  {
    return new Arez_EntitySubscriptionManager();
  }

  EntitySubscriptionManager()
  {
  }

  /**
   * Return the collection of type subscriptions.
   *
   * @return the collection of type subscriptions.
   */
  @Nonnull
  public Collection<Subscription> getTypeSubscriptions()
  {
    return _typeSubscriptions.values();
  }

  /**
   * Return the collection of instance subscriptions.
   *
   * @return the collection of instance subscriptions.
   */
  @Nonnull
  public Collection<Subscription> getInstanceSubscriptions()
  {
    return _instanceSubscriptions
      .values()
      .stream()
      .flatMap( s -> s.values().stream() )
      .collect( Collectors.toList() );
  }

  /**
   * Return the collection of instance subscriptions for channel.
   */
  @Nonnull
  public Set<Object> getInstanceSubscriptionIds( @Nonnull final Enum channelType )
  {
    final Map<Object, Subscription> map = _instanceSubscriptions.get( channelType );
    return null == map ? Collections.emptySet() : map.keySet();
  }

  /**
   * Record a subscription for specified channel.
   *
   * @param address              the channel address.
   * @param filter               the filter if subscription is filterable.
   * @param explicitSubscription if subscription was explicitly requested by the client.
   * @return the subscription entry.
   * @throws IllegalStateException if channel already subscribed to.
   */
  @Nonnull
  public final Subscription recordSubscription( @Nonnull final ChannelAddress address,
                                                @Nullable final Object filter,
                                                final boolean explicitSubscription )
    throws IllegalStateException
  {
    final Subscription existing = findSubscription( address );
    if ( null == existing )
    {
      final Subscription entry = Subscription.create( Channel.create( address, filter ), explicitSubscription );
      final Object id = address.getId();
      if ( null == id )
      {
        _typeSubscriptions.put( address.getChannelType(), entry );
      }
      else
      {
        _instanceSubscriptions
          .computeIfAbsent( address.getChannelType(), k -> new HashMap<>() )
          .put( id, entry );
      }
      return entry;
    }
    else
    {
      throw new IllegalStateException( "Channel already subscribed: " + address );
    }
  }

  /**
   * Return the subscription details for the specified channel if a subscription is recorded.
   *
   * @param channel the channel.
   * @return the subscription entry if it exists, null otherwise.
   */
  @Nullable
  public final Subscription findSubscription( @Nonnull final ChannelAddress channel )
  {
    final Enum channelType = channel.getChannelType();
    final Object id = channel.getId();
    if ( null == id )
    {
      return _typeSubscriptions.get( channelType );
    }
    else
    {
      final Map<Object, Subscription> instanceMap = _instanceSubscriptions.get( channelType );
      return null == instanceMap ? null : instanceMap.get( id );
    }
  }

  /**
   * Return the subscription details for the specified channel.
   *
   * @param channel the channel.
   * @return the subscription entry.
   * @throws IllegalArgumentException if no such subscription
   */
  @Nonnull
  public Subscription getSubscription( @Nonnull final ChannelAddress channel )
    throws IllegalArgumentException
  {
    final Subscription subscription = findSubscription( channel );
    if ( null == subscription )
    {
      throw new IllegalStateException( "Channel not subscribed: " + channel );
    }
    return subscription;
  }

  /**
   * Remove subscription details for specified channel.
   *
   * @param address the channel.
   * @return the subscription.
   * @throws IllegalStateException if channel not subscribed to.
   */
  @Nonnull
  public final Subscription removeSubscription( @Nonnull final ChannelAddress address )
    throws IllegalStateException
  {
    final Object id = address.getId();
    if ( null == id )
    {
      final Subscription entry = _typeSubscriptions.remove( address.getChannelType() );
      if ( null == entry )
      {
        throw new IllegalStateException( "Channel not subscribed: " + address );
      }
      Disposable.dispose( entry );
      return entry;
    }
    else
    {
      final Map<Object, Subscription> instanceMap = _instanceSubscriptions.get( address.getChannelType() );
      if ( null == instanceMap )
      {
        throw new IllegalStateException( "Channel not subscribed: " + address );
      }
      final Subscription entry = instanceMap.remove( id );
      if ( null == entry )
      {
        throw new IllegalStateException( "Channel not subscribed: " + address );
      }
      Disposable.dispose( entry );
      return entry;
    }
  }

  @ObservableRef
  protected abstract arez.Observable getEntitiesObservable();

  @Observable( expectSetter = false )
  Map<Class<?>, Map<Object, Entity>> getEntities()
  {
    return _entities;
  }

  @Nonnull
  public Collection<Class<?>> findAllEntityTypes()
  {
    return getEntities().keySet();
  }

  /**
   * Find the subscription details for entity.
   *
   * @param type the type of the entity.
   * @param id   the id of the entity.
   * @return the subscription entry if it exists, null otherwise.
   */
  @Nullable
  public Entity findEntityByTypeAndId( @Nonnull final Class<?> type, @Nonnull final Object id )
  {
    final Map<Object, Entity> typeMap = _entities.get( type );
    if ( null == typeMap )
    {
      getEntitiesObservable().reportObserved();
      return null;
    }
    else
    {
      final Entity entity = typeMap.get( id );
      if ( null == entity )
      {
        getEntitiesObservable().reportObserved();
        return null;
      }
      else
      {
        ComponentObservable.observe( entity );
        return entity;
      }
    }
  }

  @Nonnull
  public List<Entity> findAllEntitiesByType( @Nonnull final Class<?> type )
  {
    final Map<Object, Entity> typeMap = getEntities().get( type );
    return null == typeMap ? Collections.emptyList() : new ArrayList<>( typeMap.values() );
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
  public Entity getEntity( @Nonnull final Class<?> type, @Nonnull final Object id )
  {
    final Entity entityEntry = findEntityByTypeAndId( type, id );
    if ( null == entityEntry )
    {
      throw new IllegalStateException( "Entity not subscribed: " + type.getSimpleName() + "/" + id );
    }
    return entityEntry;
  }

  /**
   * Register specified entity as being part of specified channels.
   *
   * Note: It is assumed that if an entity is part of a channel, they are always part of the channel.
   * This may not be true with filters but we can assume it for all other scenarios.
   *
   * @param <T>      the type of the entity.
   * @param type     the type of the entity.
   * @param id       the id of the entity.
   * @param channels the channels that the entity is part of.
   */
  public <T> void updateEntity( @Nonnull final Class<T> type,
                                @Nonnull final Object id,
                                @Nonnull final ChannelAddress[] channels,
                                @Nonnull final T userObject )
  {
    final Entity entity = findOrCreateEntity( type, id );
    entity.setUserObject( userObject );
    for ( final ChannelAddress channel : channels )
    {
      entity.linkToSubscription( getSubscription( channel ) );
    }
  }

  /**
   * Disassociate entity from specified channel.
   *
   * Note: It is assumed that the caller will remove the entity from the subscription manager and
   * repository if there are no more subscriptions.
   *
   * @param type    the type of the entity.
   * @param id      the id of the entity.
   * @param channel the channel that the entity is to be disassociated from.
   * @return the entry representing entities subscription state.
   * @throws IllegalStateException if no such entity or the entity is not associated with the channel.
   */
  @Nonnull
  public Entity removeEntityFromSubscription( @Nonnull final Class<?> type,
                                              @Nonnull final Object id,
                                              @Nonnull final ChannelAddress channel )
    throws IllegalStateException
  {
    final Entity entity = getEntity( type, id );
    final Subscription channelSubscription = getSubscription( channel );
    entity.delinkFromSubscription( channelSubscription );
    return entity;
  }

  /**
   * Remove entity and all associated subscriptions.
   *
   * @param entityType the type of the entity.
   * @param id         the id of the entity.
   */
  public void removeEntity( @Nonnull final Class<?> entityType, @Nonnull final Object id )
  {
    getEntitiesObservable().preReportChanged();
    final Map<Object, Entity> typeMap = _entities.get( entityType );
    if ( BrainCheckConfig.checkInvariants() )
    {
      invariant( () -> null != typeMap,
                 () -> "Entity type " + entityType.getSimpleName() + " not present in EntitySubscriptionManager" );
    }
    assert null != typeMap;
    final Entity removed = typeMap.remove( id );
    if ( BrainCheckConfig.checkInvariants() )
    {
      invariant( () -> null != removed,
                 () -> "Entity instance " + descEntity( entityType, id ) + " not present " +
                       "in EntitySubscriptionManager" );
    }
    Disposable.dispose( removed );
    if ( typeMap.isEmpty() )
    {
      _entities.remove( entityType );
    }
    getEntitiesObservable().reportChanged();
  }

  @Nonnull
  Entity findOrCreateEntity( @Nonnull final Class<?> type, @Nonnull final Object id )
  {
    final Map<Object, Entity> typeMap = _entities.get( type );
    if ( null == typeMap )
    {
      final HashMap<Object, Entity> newTypeMap = new HashMap<>();
      _entities.put( type, newTypeMap );
      return createEntity( newTypeMap, type, id );
    }
    else
    {
      final Entity entity = typeMap.get( id );
      if ( null == entity )
      {
        return createEntity( typeMap, type, id );
      }
      else
      {
        ComponentObservable.observe( entity );
        return entity;
      }
    }
  }

  @Nonnull
  private Entity createEntity( @Nonnull final Map<Object, Entity> typeMap,
                               @Nonnull final Class<?> type,
                               @Nonnull final Object id )
  {
    getEntitiesObservable().preReportChanged();
    final Entity entity = Entity.create( type, id );
    typeMap.put( id, entity );
    getEntitiesObservable().reportChanged();
    return entity;
  }

  @Nonnull
  private String descEntity( @Nonnull final Class<?> type, @Nonnull final Object id )
  {
    return type.getSimpleName() + "/" + id;
  }
}
