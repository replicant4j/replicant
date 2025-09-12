package replicant;

import arez.ArezContext;
import arez.Disposable;
import arez.ObservableValue;
import arez.annotations.ArezComponent;
import arez.annotations.ContextRef;
import arez.annotations.Feature;
import arez.annotations.Observable;
import arez.annotations.ObservableValueRef;
import arez.annotations.PreDispose;
import arez.component.ComponentObservable;
import arez.component.DisposeNotifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
@ArezComponent( disposeNotifier = Feature.DISABLE, requireId = Feature.DISABLE )
abstract class SubscriptionService
  extends ReplicantService
{
  //SystemId -> ChannelId => Id => Entry
  @Nonnull
  private final Map<Integer, Map<Integer, Map<Integer, Subscription>>> _instanceSubscriptions = new HashMap<>();
  //SystemId -> ChannelId => Entry
  @Nonnull
  private final Map<Integer, Map<Integer, Subscription>> _typeSubscriptions = new HashMap<>();

  @Nonnull
  static SubscriptionService create( @Nullable final ReplicantContext context )
  {
    return new Arez_SubscriptionService( context );
  }

  SubscriptionService( @Nullable final ReplicantContext context )
  {
    super( context );
  }

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
      .collect( Collectors.toList() );
  }

  @ObservableValueRef
  abstract ObservableValue<?> getTypeSubscriptionsObservableValue();

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
      .collect( Collectors.toList() );
  }

  @ObservableValueRef
  abstract ObservableValue<?> getInstanceSubscriptionsObservableValue();

  /**
   * Return the collection of instance subscriptions for channel.
   *
   * @param schemaId  the schema id.
   * @param channelId the channel id.
   * @return the set of ids for all instance subscriptions with specified channel type.
   */
  @Nonnull
  Set<Integer> getInstanceSubscriptionIds( final int schemaId, final int channelId )
  {
    getInstanceSubscriptionsObservableValue().reportObserved();
    final Map<Integer, Map<Integer, Subscription>> channelMaps = _instanceSubscriptions.get( schemaId );
    final Map<Integer, Subscription> map = null == channelMaps ? null : channelMaps.get( channelId );
    if ( null == map )
    {
      return Collections.emptySet();
    }
    else
    {
      return CollectionsUtil.wrap( new HashSet<>( map.keySet() ) );
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
  Subscription createSubscription( @Nonnull final ChannelAddress address,
                                   @Nullable final Object filter,
                                   final boolean explicitSubscription )
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> null == findSubscription( address ),
                    () -> "Replicant-0064: createSubscription invoked with address " + address +
                          " but a subscription with that address already exists." );
    }
    final Integer rootId = address.rootId();
    if ( null == rootId )
    {
      getTypeSubscriptionsObservableValue().preReportChanged();
    }
    else
    {
      getInstanceSubscriptionsObservableValue().preReportChanged();
    }
    final Subscription subscription =
      Subscription.create( Replicant.areZonesEnabled() ? getReplicantContext() : null,
                           address,
                           filter,
                           explicitSubscription );
    DisposeNotifier
      .asDisposeNotifier( subscription )
      .addOnDisposeListener( this, () -> destroy( subscription ), true );
    if ( null == rootId )
    {
      _typeSubscriptions
        .computeIfAbsent( address.schemaId(), HashMap::new )
        .put( address.channelId(), subscription );
      getTypeSubscriptionsObservableValue().reportChanged();
    }
    else
    {
      _instanceSubscriptions
        .computeIfAbsent( address.schemaId(), HashMap::new )
        .computeIfAbsent( address.channelId(), HashMap::new )
        .put( rootId, subscription );
      getInstanceSubscriptionsObservableValue().reportChanged();
    }
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new SubscriptionCreatedEvent( subscription ) );
    }
    return subscription;
  }

  private void destroy( @Nonnull final Subscription subscription )
  {
    detachSubscription( subscription );
    unlinkSubscription( subscription.getAddress() );
  }

  private void detachSubscription( @Nonnull final Subscription subscription )
  {
    DisposeNotifier.asDisposeNotifier( subscription ).removeOnDisposeListener( this, true );
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
  Subscription findSubscription( @Nonnull final ChannelAddress address )
  {
    final int schemaId = address.schemaId();
    final int channelId = address.channelId();
    final Integer rootId = address.rootId();
    return null == rootId ?
           findTypeSubscription( schemaId, channelId ) :
           findInstanceSubscription( schemaId, channelId, rootId );
  }

  /**
   * Return the type subscription for the specified channelType.
   * This method will observe the <code>typeSubscriptions</code> property if not
   * found and the result {@link Subscription} if found. This ensures that if an observer
   * invokes this method then the observer will be rescheduled when the result changes.
   *
   * @param schemaId  the schema id.
   * @param channelId the channel id.
   * @return the subscription if any matches.
   */
  @Nullable
  private Subscription findTypeSubscription( final int schemaId, final int channelId )
  {
    final Map<Integer, Subscription> channelMap = _typeSubscriptions.get( schemaId );
    final Subscription subscription = null == channelMap ? null : channelMap.get( channelId );
    if ( null == subscription )
    {
      getTypeSubscriptionsObservableValue().reportObservedIfTrackingTransactionActive();
      return null;
    }
    else
    {
      if ( context().isTrackingTransactionActive() )
      {
        ComponentObservable.observe( subscription );
      }
      return subscription;
    }
  }

  /**
   * Return the instance subscription for the specified channelType and id.
   * This method will observe the <code>instanceSubscriptions</code> property if not
   * found and the result {@link Subscription} if found. This ensures that if an observer
   * invokes this method then the observer will be rescheduled when the result changes.
   *
   * @param schemaId  the schema id.
   * @param channelId the channel id.
   * @param id        the channel id.
   * @return the subscription if any matches.
   */
  @Nullable
  private Subscription findInstanceSubscription( final int schemaId, final int channelId, final int id )
  {
    final Map<Integer, Map<Integer, Subscription>> channelMap = _instanceSubscriptions.get( schemaId );
    final Map<Integer, Subscription> instanceMap = null == channelMap ? null : channelMap.get( channelId );
    final Subscription subscription = null == instanceMap ? null : instanceMap.get( id );
    if ( null == subscription || Disposable.isDisposed( subscription ) )
    {
      getInstanceSubscriptionsObservableValue().reportObservedIfTrackingTransactionActive();
      return null;
    }
    else
    {
      if ( context().isTrackingTransactionActive() )
      {
        ComponentObservable.observe( subscription );
      }
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
  Subscription unlinkSubscription( @Nonnull final ChannelAddress address )
  {
    final int schemaId = address.schemaId();
    final int channelId = address.channelId();
    final Integer rootId = address.rootId();
    if ( null == rootId )
    {
      getTypeSubscriptionsObservableValue().preReportChanged();
      final Map<Integer, Subscription> map = _typeSubscriptions.get( schemaId );
      final Subscription subscription = null == map ? null : map.remove( channelId );
      if ( null != subscription && map.isEmpty() )
      {
        _typeSubscriptions.remove( schemaId );
      }
      if ( Replicant.shouldCheckInvariants() )
      {
        invariant( () -> null != subscription,
                   () -> "Replicant-0062: unlinkSubscription invoked with address " + address +
                         " but no subscription with that address exists." );
        assert null != subscription;
        invariant( () -> Disposable.isDisposed( subscription ),
                   () -> "Replicant-0063: unlinkSubscription invoked with address " + address +
                         " but subscription has not already been disposed." );
      }
      assert null != subscription;
      getTypeSubscriptionsObservableValue().reportChanged();
      return subscription;
    }
    else
    {
      getInstanceSubscriptionsObservableValue().preReportChanged();
      final Map<Integer, Map<Integer, Subscription>> channelMap = _instanceSubscriptions.get( schemaId );
      final Map<Integer, Subscription> instanceMap = null == channelMap ? null : channelMap.get( channelId );
      final Subscription subscription = null == instanceMap ? null : instanceMap.remove( rootId );
      if ( null != subscription && instanceMap.isEmpty() )
      {
        channelMap.remove( channelId );
        if ( channelMap.isEmpty() )
        {
          _instanceSubscriptions.remove( schemaId );
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
      getInstanceSubscriptionsObservableValue().reportChanged();
      return subscription;
    }
  }

  @PreDispose
  void preDispose()
  {
    _typeSubscriptions
      .values()
      .stream()
      .flatMap( s -> s.values().stream() )
      .peek( this::detachSubscription )
      .forEach( Disposable::dispose );
    _instanceSubscriptions
      .values()
      .stream()
      .flatMap( t -> t.values().stream() )
      .flatMap( t -> t.values().stream() )
      .peek( this::detachSubscription )
      .forEach( Disposable::dispose );
  }

  @ContextRef
  abstract ArezContext context();
}
