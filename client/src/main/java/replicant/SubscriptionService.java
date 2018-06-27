package replicant;

import arez.Arez;
import arez.ArezContext;
import arez.Component;
import arez.Disposable;
import arez.Observer;
import arez.Priority;
import arez.annotations.ArezComponent;
import arez.annotations.ComponentNameRef;
import arez.annotations.ComponentRef;
import arez.annotations.ContextRef;
import arez.annotations.Observable;
import arez.annotations.ObservableRef;
import arez.annotations.PreDispose;
import arez.component.CollectionsUtil;
import arez.component.ComponentObservable;
import arez.component.Identifiable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.spy.SubscriptionCreatedEvent;
import static org.realityforge.braincheck.Guards.*;

/**
 * A class that records the subscriptions within the system.
 */
@ArezComponent
abstract class SubscriptionService
  extends ReplicantService
{
  //SystemId -> ChannelId => Id => Entry
  private final Map<Integer, Map<Integer, Map<Integer, Subscription>>> _instanceSubscriptions = new HashMap<>();
  //SystemId -> ChannelId => Entry
  private final Map<Integer, Map<Integer, SubscriptionEntry>> _typeSubscriptions = new HashMap<>();

  static SubscriptionService create( @Nullable final ReplicantContext context )
  {
    return new Arez_SubscriptionService( context );
  }

  SubscriptionService( @Nullable final ReplicantContext context )
  {
    super( context );
  }

  /**
   * Return the context associated with the service.
   *
   * @return the context associated with the service.
   */
  @ContextRef
  @Nonnull
  abstract ArezContext getContext();

  /**
   * Return the name associated with the service.
   *
   * @return the name associated with the service.
   */
  @ComponentNameRef
  @Nonnull
  abstract String getComponentName();

  /**
   * Return the component associated with service if native components enabled.
   *
   * @return the component associated with service if native components enabled.
   */
  @ComponentRef
  @Nonnull
  abstract Component component();

  /**
   * Return the collection of type subscriptions.
   *
   * @return the collection of type subscriptions.
   */
  @Nonnull
  @Observable( expectSetter = false )
  List<Subscription> getTypeSubscriptions()
  {
    return _typeSubscriptions
      .values()
      .stream()
      .flatMap( s -> s.values().stream() )
      .filter( Disposable::isNotDisposed )
      .map( SubscriptionEntry::getSubscription )
      .collect( Collectors.toList() );
  }

  @ObservableRef
  protected abstract arez.Observable getTypeSubscriptionsObservable();

  /**
   * Return the collection of instance subscriptions.
   *
   * @return the collection of instance subscriptions.
   */
  @Nonnull
  @Observable( expectSetter = false )
  Collection<Subscription> getInstanceSubscriptions()
  {
    return _instanceSubscriptions
      .values()
      .stream()
      .flatMap( s -> s.values().stream() )
      .flatMap( s -> s.values().stream() )
      .filter( Disposable::isNotDisposed )
      .collect( Collectors.toList() );
  }

  @ObservableRef
  protected abstract arez.Observable getInstanceSubscriptionsObservable();

  /**
   * Return the collection of instance subscriptions for channel.
   *
   * @param systemId  the system id.
   * @param channelId the channel id.
   * @return the set of ids for all instance subscriptions with specified channel type.
   */
  @Nonnull
  Set<Object> getInstanceSubscriptionIds( final int systemId, final int channelId )
  {
    getInstanceSubscriptionsObservable().reportObserved();
    final Map<Integer, Map<Integer, Subscription>> channelMaps = _instanceSubscriptions.get( systemId );
    final Map<Integer, Subscription> map = null == channelMaps ? null : channelMaps.get( channelId );
    if ( null == map )
    {
      return Collections.emptySet();
    }
    else
    {
      final Set<Object> results =
        map
          .entrySet()
          .stream()
          .filter( e -> Disposable.isNotDisposed( e.getValue() ) )
          .map( Map.Entry::getKey )
          .collect( Collectors.toSet() );
      return CollectionsUtil.wrap( results );
    }
  }

  /**
   * Create a subscription.
   * This method should not be invoked if a subscription with the existing name already exists.
   *
   * @param address              the channel address.
   * @param filter               the filter if subscription is filterable.
   * @param explicitSubscription if subscription was explicitly requested by the client.
   * @return the subscription.
   */
  @Nonnull
  final Subscription createSubscription( @Nonnull final ChannelAddress address,
                                         @Nullable final Object filter,
                                         final boolean explicitSubscription )
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> null == findSubscription( address ),
                    () -> "Replicant-0064: createSubscription invoked with address " + address +
                          " but a subscription with that address already exists." );
    }
    final Integer id = address.getId();
    if ( null == id )
    {
      getTypeSubscriptionsObservable().preReportChanged();
    }
    else
    {
      getInstanceSubscriptionsObservable().preReportChanged();
    }
    final Subscription subscription =
      Subscription.create( Replicant.areZonesEnabled() ? getReplicantContext() : null,
                           address,
                           filter,
                           explicitSubscription );
    final SubscriptionEntry entry = createSubscriptionEntry( subscription );
    if ( null == id )
    {
      _typeSubscriptions
        .computeIfAbsent( address.getSystemId(), HashMap::new )
        .put( address.getChannelId(), entry );
      getTypeSubscriptionsObservable().reportChanged();
    }
    else
    {
      _instanceSubscriptions
        .computeIfAbsent( address.getSystemId(), HashMap::new )
        .computeIfAbsent( address.getChannelId(), HashMap::new )
        .put( id, subscription );
      getInstanceSubscriptionsObservable().reportChanged();
    }
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new SubscriptionCreatedEvent( subscription ) );
    }
    return subscription;
  }

  @Nonnull
  private SubscriptionEntry createSubscriptionEntry( @Nonnull final Subscription subscription )
  {
    final SubscriptionEntry entry = new SubscriptionEntry( subscription );
    final Object arezId = Identifiable.getArezId( subscription );
    final Observer monitor =
      getContext().when( Arez.areNativeComponentsEnabled() ? component() : null,
                         Arez.areNamesEnabled() ? getComponentName() + ".SubscriptionWatcher." + arezId : null,
                         true,
                         () -> !ComponentObservable.observe( subscription ),
                         () -> destroy( subscription ),
                         Priority.HIGH,
                         true );
    entry.setMonitor( monitor );
    return entry;
  }

  private void destroy( @Nonnull final Subscription subscription )
  {
    subscription.delinkSubscriptionFromAllEntities();
    unlinkSubscription( subscription.getAddress() );
  }

  /**
   * Return the subscription for the specified address.
   * This method will observe the <code>typeSubscriptions</code> or <code>instanceSubscriptions</code>
   * property if not found and the result {@link Subscription} if found. This ensures that if an observer
   * invokes this method then the observer will be rescheduled when the result changes.
   *
   * @param address the channel address.
   * @return the subscription if it exists, null otherwise.
   */
  @Nullable
  final Subscription findSubscription( @Nonnull final ChannelAddress address )
  {
    final int systemId = address.getSystemId();
    final int channelId = address.getChannelId();
    final Integer id = address.getId();
    return null == id ?
           findTypeSubscription( systemId, channelId ) :
           findInstanceSubscription( systemId, channelId, id );
  }

  /**
   * Return the type subscription for the specified channelType.
   * This method will observe the <code>typeSubscriptions</code> property if not
   * found and the result {@link Subscription} if found. This ensures that if an observer
   * invokes this method then the observer will be rescheduled when the result changes.
   *
   * @param systemId  the system id.
   * @param channelId the channel id.
   * @return the subscription if any matches.
   */
  @Nullable
  private Subscription findTypeSubscription( final int systemId, final int channelId )
  {
    final Map<Integer, SubscriptionEntry> channelMap = _typeSubscriptions.get( systemId );
    final SubscriptionEntry entry = null == channelMap ? null : channelMap.get( channelId );
    if ( null == entry || Disposable.isDisposed( entry.getSubscription() ) )
    {
      getTypeSubscriptionsObservable().reportObserved();
      return null;
    }
    else
    {
      final Subscription subscription = entry.getSubscription();
      ComponentObservable.observe( subscription );
      return subscription;
    }
  }

  /**
   * Return the instance subscription for the specified channelType and id.
   * This method will observe the <code>instanceSubscriptions</code> property if not
   * found and the result {@link Subscription} if found. This ensures that if an observer
   * invokes this method then the observer will be rescheduled when the result changes.
   *
   * @param systemId  the system id.
   * @param channelId the channel id.
   * @param id        the channel id.
   * @return the subscription if any matches.
   */
  @Nullable
  private Subscription findInstanceSubscription( final int systemId, final int channelId, final int id )
  {
    final Map<Integer, Map<Integer, Subscription>> channelMap = _instanceSubscriptions.get( systemId );
    final Map<Integer, Subscription> instanceMap = null == channelMap ? null : channelMap.get( channelId );
    final Subscription subscription = null == instanceMap ? null : instanceMap.get( id );
    if ( null == subscription || Disposable.isDisposed( subscription ) )
    {
      getInstanceSubscriptionsObservable().reportObserved();
      return null;
    }
    else
    {
      ComponentObservable.observe( subscription );
      return subscription;
    }
  }

  /**
   * Remove subscription on channel specified by address.
   * This method should only be invoked if a subscription exists
   *
   * @param address the channel address.
   * @return the subscription.
   */
  @Nonnull
  final Subscription unlinkSubscription( @Nonnull final ChannelAddress address )
  {
    final int systemId = address.getSystemId();
    final int channelId = address.getChannelId();
    final Integer id = address.getId();
    if ( null == id )
    {
      getTypeSubscriptionsObservable().preReportChanged();
      final Map<Integer, SubscriptionEntry> entryMap = _typeSubscriptions.get( systemId );
      final SubscriptionEntry entry = null == entryMap ? null : entryMap.remove( channelId );
      if ( null != entry && entryMap.isEmpty() )
      {
        _typeSubscriptions.remove( systemId );
      }
      if ( Replicant.shouldCheckInvariants() )
      {
        invariant( () -> null != entry,
                   () -> "Replicant-0062: unlinkSubscription invoked with address " + address +
                         " but no subscription with that address exists." );
        assert null != entry;
        invariant( () -> Disposable.isDisposed( entry ),
                   () -> "Replicant-0063: unlinkSubscription invoked with address " + address +
                         " but subscription has not already been disposed." );
      }
      assert null != entry;
      getTypeSubscriptionsObservable().reportChanged();
      return entry.getSubscription();
    }
    else
    {
      getInstanceSubscriptionsObservable().preReportChanged();
      final Map<Integer, Map<Integer, Subscription>> channelMap = _instanceSubscriptions.get( systemId );
      final Map<Integer, Subscription> instanceMap = null == channelMap ? null : channelMap.get( channelId );
      final Subscription subscription = null == instanceMap ? null : instanceMap.remove( id );
      if ( null != subscription && instanceMap.isEmpty() )
      {
        channelMap.remove( channelId );
        if ( channelMap.isEmpty() )
        {
          _instanceSubscriptions.remove( systemId );
        }
      }
      if ( Replicant.shouldCheckInvariants() )
      {
        invariant( () -> null != subscription,
                   () -> "Replicant-0060: unlinkSubscription invoked with address " + address +
                         " but no subscription with that address exists." );
        assert null != subscription;
        invariant( () -> Disposable.isDisposed( subscription ),
                   () -> "Replicant-0061: unlinkSubscription invoked with address " + address +
                         " but subscription has not already been disposed." );
      }
      assert null != subscription;
      getInstanceSubscriptionsObservable().reportChanged();
      return subscription;
    }
  }

  @PreDispose
  final void preDispose()
  {
    _typeSubscriptions
      .values()
      .stream()
      .flatMap( s -> s.values().stream() )
      .forEach( s -> Disposable.dispose( s ) );
    _instanceSubscriptions
      .values()
      .stream()
      .flatMap( t -> t.values().stream() )
      .flatMap( t -> t.values().stream() )
      .forEach( Disposable::dispose );
  }
}
