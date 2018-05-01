package org.realityforge.replicant.client.subscription;

import arez.Arez;
import arez.Disposable;
import arez.annotations.ArezComponent;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import org.realityforge.replicant.client.Channel;
import org.realityforge.replicant.client.ChannelAddress;

/**
 * A class that records the subscriptions to channels and entities.
 */
@Singleton
@ArezComponent( allowEmpty = true )
public abstract class EntitySubscriptionManager
{
  //ChannelType => InstanceID
  private final HashMap<Enum, Map<Object, ChannelSubscriptionEntry>> _instanceChannelSubscriptions = new HashMap<>();

  //ChannelType => Type
  private final HashMap<Enum, ChannelSubscriptionEntry> _typeChannelSubscriptions = new HashMap<>();

  // Entity map: Type => ID
  private final HashMap<Class<?>, EntityTypeRepository> _entityMapping = new HashMap<>();

  @Nonnull
  public static EntitySubscriptionManager create()
  {
    return new Arez_EntitySubscriptionManager();
  }

  EntitySubscriptionManager()
  {
  }

  /**
   * Return the collection of current type subscriptions.
   * These keys can be directly used to unsubscribe from the channel.
   */
  @Nonnull
  public Collection<ChannelSubscriptionEntry> getTypeChannelSubscriptions()
  {
    final Collection<ChannelSubscriptionEntry> values = _typeChannelSubscriptions.values();
    return Arez.areRepositoryResultsModifiable() ? values : Collections.unmodifiableCollection( values );
  }

  /**
   * Return the collection of enums that represent instance subscriptions.
   * These can be used to further interrogate the EntitySubscriptionManager
   * to retrieve the set of instance subscriptions.
   */
  @Nonnull
  public Set<Enum> getInstanceChannelSubscriptionKeys()
  {
    return Collections.unmodifiableSet( _instanceChannelSubscriptions.keySet() );
  }

  /**
   * Return the collection of instance subscriptions for channel.
   */
  @Nonnull
  public Set<Object> getInstanceChannelSubscriptions( @Nonnull final Enum channel )
  {
    final Map<Object, ChannelSubscriptionEntry> map = _instanceChannelSubscriptions.get( channel );
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
   * Record a subscription for specified channel.
   *
   * @param address              the channel address.
   * @param filter               the filter if subscription is filterable.
   * @param explicitSubscription if subscription was explicitly requested by the client.
   * @return the subscription entry.
   * @throws IllegalStateException if channel already subscribed to.
   */
  @Nonnull
  public final ChannelSubscriptionEntry recordChannelSubscription( @Nonnull final ChannelAddress address,
                                                                   @Nullable final Object filter,
                                                                   final boolean explicitSubscription )
    throws IllegalStateException
  {
    final ChannelSubscriptionEntry existing = findChannelSubscription( address );
    if ( null == existing )
    {
      final ChannelSubscriptionEntry entry =
        ChannelSubscriptionEntry.create( Channel.create( address, filter ), explicitSubscription );
      final Object id = address.getId();
      if ( null == id )
      {
        _typeChannelSubscriptions.put( address.getChannelType(), entry );
      }
      else
      {
        _instanceChannelSubscriptions
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
   * Update subscription details for the specified channel.
   *
   * @param channel the channel.
   * @param filter  the filter being updated.
   * @return the subscription entry.
   * @throws IllegalStateException if channel already subscribed to.
   */
  @Nonnull
  public ChannelSubscriptionEntry updateChannelSubscription( @Nonnull final ChannelAddress channel,
                                                             @Nullable final Object filter )
    throws IllegalStateException
  {
    final ChannelSubscriptionEntry subscription = getChannelSubscription( channel );
    subscription.getChannel().setFilter( filter );
    return subscription;
  }

  /**
   * Return the subscription details for the specified channel if a subscription is recorded.
   *
   * @param channel the channel.
   * @return the subscription entry if it exists, null otherwise.
   */
  @Nullable
  public final ChannelSubscriptionEntry findChannelSubscription( @Nonnull final ChannelAddress channel )
  {
    final Object id = channel.getId();
    if ( null == id )
    {
      return _typeChannelSubscriptions.get( channel.getChannelType() );
    }
    else
    {
      Map<Object, ChannelSubscriptionEntry> instanceMap = _instanceChannelSubscriptions.get( channel.getChannelType() );
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
   * Return the subscription details for the specified channel.
   *
   * @param channel the channel.
   * @return the subscription entry.
   * @throws IllegalArgumentException if no such subscription
   */
  @Nonnull
  public ChannelSubscriptionEntry getChannelSubscription( @Nonnull final ChannelAddress channel )
    throws IllegalArgumentException
  {
    final ChannelSubscriptionEntry subscription = findChannelSubscription( channel );
    if ( null == subscription )
    {
      throw new IllegalStateException( "Channel not subscribed: " + channel );
    }
    return subscription;
  }

  /**
   * Remove subscription details for specified channel.
   *
   * @param channel the channel.
   * @return the subscription entry.
   * @throws IllegalStateException if channel not subscribed to.
   */
  @Nonnull
  public final ChannelSubscriptionEntry removeChannelSubscription( @Nonnull final ChannelAddress channel )
    throws IllegalStateException
  {
    final Object id = channel.getId();
    if ( null == id )
    {
      final ChannelSubscriptionEntry entry = _typeChannelSubscriptions.remove( channel.getChannelType() );
      if ( null == entry )
      {
        throw new IllegalStateException( "Channel not subscribed: " + channel );
      }
      Disposable.dispose( entry );
      return entry;
    }
    else
    {
      final Map<Object, ChannelSubscriptionEntry> instanceMap =
        _instanceChannelSubscriptions.get( channel.getChannelType() );
      if ( null == instanceMap )
      {
        throw new IllegalStateException( "Channel not subscribed: " + channel );
      }
      final ChannelSubscriptionEntry entry = instanceMap.remove( id );
      if ( null == entry )
      {
        throw new IllegalStateException( "Channel not subscribed: " + channel );
      }
      Disposable.dispose( entry );
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
  public Entity getEntity( @Nonnull final Class<?> type, @Nonnull final Object id )
  {
    final Entity entityEntry = findEntity( type, id );
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
  public Entity findEntity( @Nonnull final Class<?> type, @Nonnull final Object id )
  {
    return getEntityTypeMap( type ).findEntityById( id );
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
      entity.addChannelSubscription( getChannelSubscription( channel ) );
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
  public Entity removeEntityFromChannel( @Nonnull final Class<?> type,
                                         @Nonnull final Object id,
                                         @Nonnull final ChannelAddress channel )
    throws IllegalStateException
  {
    final Entity entity = findOrCreateEntity( type, id );
    final ChannelSubscriptionEntry channelSubscription = getChannelSubscription( channel );
    entity.removeChannelSubscription( channelSubscription );
    return entity;
  }

  /**
   * Remove entity and all associated subscriptions.
   *
   * @param type the type of the entity.
   * @param id   the id of the entity.
   */
  public void removeEntity( @Nonnull final Class<?> type, @Nonnull final Object id )
  {
    final EntityTypeRepository entityTypeRepository = _entityMapping.get( type );
    if ( null != entityTypeRepository )
    {
      final Entity entity = entityTypeRepository.findEntityById( id );
      if ( null != entity )
      {
        entityTypeRepository.destroy( entity );
      }
    }
  }

  @Nonnull
  private Entity findOrCreateEntity( @Nonnull final Class<?> type, @Nonnull final Object id )
  {
    return getEntityTypeMap( type ).findOrCreateEntity( id );
  }

  @Nonnull
  public List<Entity> findEntitiesByType( @Nonnull final Class<?> type )
  {
    return getEntityTypeMap( type ).getEntities();
  }

  @Nonnull
  private EntityTypeRepository getEntityTypeMap( @Nonnull final Class<?> type )
  {
    return _entityMapping.computeIfAbsent( type, EntityTypeRepository::create );
  }
}
