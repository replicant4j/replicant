package replicant;

import arez.Disposable;
import arez.annotations.ArezComponent;
import arez.annotations.Feature;
import arez.annotations.Observable;
import arez.annotations.ObservableRef;
import arez.annotations.PreDispose;
import arez.component.ComponentObservable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import replicant.spy.SubscriptionDisposedEvent;
import static org.realityforge.braincheck.Guards.*;

/**
 * Representation of a subscription to a channel.
 */
@ArezComponent
public abstract class Subscription
  implements Comparable<Subscription>
{
  /**
   * Reference to the container that created Subscription.
   * In the future this reference should be eliminated when there is a way to get to the singleton
   * SubscriptionService. (Similar to the way we have Arez.context().X we should have Replicant.context().X)
   * This will save memory resources on the client.
   */
  @Nonnull
  private final SubscriptionService _subscriptionService;

  private final Map<Class<?>, Map<Integer, EntityEntry>> _entities = new HashMap<>();
  @Nonnull
  private final Channel _channel;

  static Subscription create( @Nonnull final SubscriptionService subscriptionService,
                              @Nonnull final Channel channel,
                              final boolean explicitSubscription )
  {
    return new Arez_Subscription( subscriptionService, channel, explicitSubscription );
  }

  Subscription( @Nonnull final SubscriptionService subscriptionService, @Nonnull final Channel channel )
  {
    _subscriptionService = Objects.requireNonNull( subscriptionService );
    _channel = Objects.requireNonNull( channel );
  }

  @Nonnull
  public Channel getChannel()
  {
    return _channel;
  }

  @Observable( initializer = Feature.ENABLE )
  public abstract boolean isExplicitSubscription();

  public abstract void setExplicitSubscription( boolean explicitSubscription );

  @Observable( expectSetter = false )
  Map<Class<?>, Map<Integer, EntityEntry>> getEntities()
  {
    return _entities;
  }

  @Nonnull
  public Collection<Class<?>> findAllEntityTypes()
  {
    return getEntities().keySet();
  }

  @Nonnull
  public List<Entity> findAllEntitiesByType( @Nonnull final Class<?> type )
  {
    final Map<Integer, EntityEntry> typeMap = getEntities().get( type );
    return null == typeMap ?
           Collections.emptyList() :
           typeMap.values().stream().map( EntityEntry::getEntity ).collect( Collectors.toList() );
  }

  @Nullable
  public Entity findEntityByTypeAndId( @Nonnull final Class<?> type, final int id )
  {
    final Map<Integer, EntityEntry> typeMap = _entities.get( type );
    if ( null == typeMap )
    {
      getEntitiesObservable().reportObserved();
      return null;
    }
    else
    {
      final EntityEntry entry = typeMap.get( id );
      if ( null == entry )
      {
        getEntitiesObservable().reportObserved();
        return null;
      }
      else
      {
        ComponentObservable.observe( entry );
        return entry.getEntity();
      }
    }
  }

  @ObservableRef
  protected abstract arez.Observable getEntitiesObservable();

  @SuppressWarnings( "unchecked" )
  @Override
  public int compareTo( @NotNull final Subscription o )
  {
    return getChannel().getAddress().getChannelType().compareTo( o.getChannel().getAddress().getChannelType() );
  }

  final void linkSubscriptionToEntity( @Nonnull final Entity entity )
  {
    getEntitiesObservable().preReportChanged();
    final Class<?> type = entity.getType();
    final int id = entity.getId();
    Map<Integer, EntityEntry> typeMap = _entities.get( type );
    if ( null == typeMap )
    {
      typeMap = new HashMap<>();
      typeMap.put( id, EntityEntry.create( entity ) );
      _entities.put( type, typeMap );
      getEntitiesObservable().reportChanged();
    }
    else
    {
      if ( !typeMap.containsKey( id ) )
      {
        typeMap.put( id, EntityEntry.create( entity ) );
        getEntitiesObservable().reportChanged();
      }
    }
  }

  /**
   * Unlink the specified entity from this subscription.
   * This method does not delink channel from entity and it is assumed this is achieved through
   * other means such as {@link Entity#delinkSubscriptionFromEntity(Subscription)}.
   *
   * @param entity the entity.
   */
  final void delinkEntityFromSubscription( @Nonnull final Entity entity )
  {
    getEntitiesObservable().preReportChanged();
    final Class<?> entityType = entity.getType();
    final Map<Integer, EntityEntry> typeMap = _entities.get( entityType );
    final ChannelAddress address = getChannel().getAddress();
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> null != typeMap,
                 () -> "Entity type " + entityType.getSimpleName() + " not present in subscription " +
                       "to channel " + address );
    }
    assert null != typeMap;
    final EntityEntry removed = typeMap.remove( entity.getId() );
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> null != removed,
                 () -> "Entity instance " + entity + " not present in subscription to channel " + address );
    }
    Disposable.dispose( removed );
    if ( typeMap.isEmpty() )
    {
      _entities.remove( entityType );
    }
    getEntitiesObservable().reportChanged();
  }

  @PreDispose
  final void preDispose()
  {
    delinkSubscriptionFromAllEntities();
    if ( Replicant.areSpiesEnabled() && _subscriptionService.getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      _subscriptionService.getReplicantContext().getSpy().reportSpyEvent( new SubscriptionDisposedEvent( this ) );
    }
  }

  private void delinkSubscriptionFromAllEntities()
  {
    _entities.values()
      .stream()
      .flatMap( entitySet -> entitySet.values().stream() )
      .forEachOrdered( entity -> entity.getEntity().delinkSubscriptionFromEntity( this ) );
  }
}
