package replicant;

import arez.Disposable;
import arez.ObservableValue;
import arez.annotations.ArezComponent;
import arez.annotations.Feature;
import arez.annotations.Observable;
import arez.annotations.ObservableValueRef;
import arez.annotations.PreDispose;
import arez.component.ComponentObservable;
import arez.component.DisposeNotifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.spy.SubscriptionDisposedEvent;
import static org.realityforge.braincheck.Guards.*;

/**
 * Representation of a subscription to a channel.
 */
@ArezComponent( observable = Feature.ENABLE, requireId = Feature.ENABLE )
public abstract class Subscription
  extends ReplicantService
  implements Comparable<Subscription>
{
  @NonNull
  private final Map<Class<?>, NavigableMap<Integer, EntitySubscriptionEntry>> _entities = new HashMap<>();
  @NonNull
  private final ChannelAddress _address;

  @NonNull
  static Subscription create( @Nullable final ReplicantContext context,
                              @NonNull final ChannelAddress address,
                              @Nullable final Object filter,
                              final boolean explicitSubscription )
  {
    return new Arez_Subscription( context, address, filter, explicitSubscription );
  }

  Subscription( @Nullable final ReplicantContext context, @NonNull final ChannelAddress address )
  {
    super( context );
    _address = Objects.requireNonNull( address );
  }

  @NonNull
  public ChannelAddress address()
  {
    return _address;
  }

  @Observable( initializer = Feature.ENABLE )
  @Nullable
  public abstract Object getFilter();

  abstract void setFilter( @Nullable Object filter );

  @Observable( initializer = Feature.ENABLE )
  public abstract boolean isExplicitSubscription();

  public abstract void setExplicitSubscription( boolean explicitSubscription );

  @NonNull
  @Observable( expectSetter = false )
  Map<Class<?>, NavigableMap<Integer, EntitySubscriptionEntry>> getEntities()
  {
    return _entities;
  }

  @NonNull
  public Collection<Class<?>> findAllEntityTypes()
  {
    return getEntities().keySet();
  }

  @NonNull
  public List<Entity> findAllEntitiesByType( @NonNull final Class<?> type )
  {
    final Map<Integer, EntitySubscriptionEntry> typeMap = getEntities().get( type );
    return null == typeMap ?
           Collections.emptyList() :
           CollectionsUtil.asList( typeMap.values().stream().map( EntitySubscriptionEntry::getEntity ) );
  }

  @Nullable
  public Entity findEntityByTypeAndId( @NonNull final Class<?> type, final int id )
  {
    final Map<Integer, EntitySubscriptionEntry> typeMap = _entities.get( type );
    if ( null == typeMap )
    {
      getEntitiesObservableValue().reportObserved();
      return null;
    }
    else
    {
      final EntitySubscriptionEntry entry = typeMap.get( id );
      if ( null == entry )
      {
        getEntitiesObservableValue().reportObserved();
        return null;
      }
      else
      {
        final Entity entity = entry.getEntity();
        ComponentObservable.observe( entity );
        return entity;
      }
    }
  }

  /**
   * Return the instance root for this subscription.
   * This method should NOT be invoked on subscriptions for type graphs
   *
   * @return the instance root.
   */
  @NonNull
  public Object getInstanceRoot()
  {
    final ChannelSchema channel = getChannelSchema();
    final Integer rootId = _address.rootId();
    if ( Replicant.shouldCheckApiInvariants() )
    {
      invariant( channel::isInstanceChannel,
                 () -> "Replicant-0029: Subscription.getInstanceRoot() invoked on subscription for channel " +
                       _address + " but channel is not instance based." );
      invariant( () -> null != rootId,
                 () -> "Replicant-0087: Subscription.getInstanceRoot() invoked on subscription for channel " +
                       _address + " but channel has not supplied expected id." );
    }
    final Entity entity =
      findEntityByTypeAndId( Objects.requireNonNull( channel.getInstanceType() ), Objects.requireNonNull( rootId ) );
    if ( Replicant.shouldCheckApiInvariants() )
    {
      invariant( () -> null != entity,
                 () -> "Replicant-0088: Subscription.getInstanceRoot() invoked on subscription for channel " +
                       _address + " but entity is not present." );
    }
    return Objects.requireNonNull( entity ).getUserObject();
  }

  /**
   * Return the channel schema for subscription.
   *
   * @return the channel schema for subscription.
   */
  @NonNull
  public ChannelSchema getChannelSchema()
  {
    return getReplicantContext()
      .getSchemaService()
      .getById( _address.schemaId() )
      .getChannel( _address.channelId() );
  }

  @ObservableValueRef
  abstract ObservableValue<?> getEntitiesObservableValue();

  @Override
  public int compareTo( @NonNull final Subscription o )
  {
    return address().compareTo( o.address() );
  }

  void linkSubscriptionToEntity( @NonNull final Entity entity )
  {
    getEntitiesObservableValue().preReportChanged();
    final Class<?> type = entity.getType();
    final int id = entity.getId();
    final NavigableMap<Integer, EntitySubscriptionEntry> typeMap = _entities.computeIfAbsent( type,
                                                                                              t -> new TreeMap<>() );
    if ( !typeMap.containsKey( id ) )
    {
      createSubscriptionEntry( typeMap, entity );
    }
  }

  private void createSubscriptionEntry( @NonNull final Map<Integer, EntitySubscriptionEntry> typeMap,
                                        @NonNull final Entity entity )
  {
    typeMap.put( entity.getId(), EntitySubscriptionEntry.create( entity ) );
    DisposeNotifier
      .asDisposeNotifier( entity )
      .addOnDisposeListener( this, () -> detachEntity( entity, false ), true );
    getEntitiesObservableValue().reportChanged();
  }

  void delinkEntityFromSubscription( @NonNull final Entity entity, final boolean disposeEntityIfNoSubscriptions )
  {
    getEntitiesObservableValue().preReportChanged();
    detachEntity( entity, disposeEntityIfNoSubscriptions );
    getEntitiesObservableValue().reportChanged();
  }

  private void detachEntity( @NonNull final Entity entity, final boolean disposeEntityIfNoSubscriptions )
  {
    final Class<?> entityType = entity.getType();
    final Map<Integer, EntitySubscriptionEntry> typeMap = _entities.get( entityType );
    final ChannelAddress address = address();
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> null != typeMap,
                 () -> "Entity type " + entityType.getSimpleName() + " not present in subscription " +
                       "to channel " + address );
    }
    final EntitySubscriptionEntry removed = Objects.requireNonNull( typeMap ).remove( entity.getId() );
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> null != removed,
                 () -> "Entity instance " + entity + " not present in subscription to channel " + address );
    }
    DisposeNotifier.asDisposeNotifier( entity ).removeOnDisposeListener( this, true );
    Disposable.dispose( removed );
    if ( disposeEntityIfNoSubscriptions )
    {
      entity.disposeIfNoSubscriptions();
    }
    if ( typeMap.isEmpty() )
    {
      _entities.remove( entityType );
    }
  }

  @PreDispose
  void preDispose()
  {
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new SubscriptionDisposedEvent( this ) );
    }
    delinkSubscriptionFromAllEntities();
  }

  private void delinkSubscriptionFromAllEntities()
  {
    new ArrayList<>( _entities.values() )
      .stream()
      .flatMap( entitySet -> new ArrayList<>( entitySet.values() ).stream() )
      .forEachOrdered( entity -> entity.getEntity().delinkSubscriptionFromEntity( this ) );
  }
}
