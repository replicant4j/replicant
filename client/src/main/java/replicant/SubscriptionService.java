package replicant;

import arez.Arez;
import arez.ArezContext;
import arez.Component;
import arez.Disposable;
import arez.Observer;
import arez.annotations.ArezComponent;
import arez.annotations.ComponentNameRef;
import arez.annotations.ComponentRef;
import arez.annotations.ContextRef;
import arez.annotations.Observable;
import arez.annotations.ObservableRef;
import arez.annotations.PreDispose;
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
import javax.inject.Singleton;
import static org.realityforge.braincheck.Guards.*;

/**
 * A class that records the subscriptions within the system.
 */
@Singleton
@ArezComponent
public abstract class SubscriptionService
{
  //ChannelType => InstanceID
  private final HashMap<Enum, Map<Object, Subscription>> _instanceSubscriptions = new HashMap<>();

  //ChannelType => Type
  private final HashMap<Enum, SubscriptionEntry> _typeSubscriptions = new HashMap<>();

  @Nonnull
  public static SubscriptionService create()
  {
    return new Arez_SubscriptionService();
  }

  SubscriptionService()
  {
  }

  /**
   * Return the context associated with the service.
   *
   * @return the context associated with the service.
   */
  @ContextRef
  @Nonnull
  protected abstract ArezContext getContext();

  /**
   * Return the name associated with the service.
   *
   * @return the name associated with the service.
   */
  @ComponentNameRef
  @Nonnull
  protected abstract String getComponentName();

  /**
   * Return the component associated with service if native components enabled.
   *
   * @return the component associated with service if native components enabled.
   */
  @ComponentRef
  @Nonnull
  protected abstract Component component();

  /**
   * Return the collection of type subscriptions.
   *
   * @return the collection of type subscriptions.
   */
  @Nonnull
  @Observable( expectSetter = false )
  public List<Subscription> getTypeSubscriptions()
  {
    return _typeSubscriptions
      .values()
      .stream()
      .filter( s -> !Disposable.isDisposed( s ) )
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
  public Collection<Subscription> getInstanceSubscriptions()
  {
    return _instanceSubscriptions
      .values()
      .stream()
      .flatMap( s -> s.values().stream() )
      .filter( s -> !Disposable.isDisposed( s ) )
      .collect( Collectors.toList() );
  }

  @ObservableRef
  protected abstract arez.Observable getInstanceSubscriptionsObservable();

  /**
   * Return the collection of instance subscriptions for channel.
   */
  @Nonnull
  public Set<Object> getInstanceSubscriptionIds( @Nonnull final Enum channelType )
  {
    getInstanceSubscriptionsObservable().reportObserved();
    final Map<Object, Subscription> map = _instanceSubscriptions.get( channelType );
    return null == map ? Collections.emptySet() : map.keySet();
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
  public final Subscription createSubscription( @Nonnull final ChannelAddress address,
                                                @Nullable final Object filter,
                                                final boolean explicitSubscription )
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> null == findSubscription( address ),
                    () -> "createSubscription invoked with address " + address +
                          " but a subscription with that address already exists." );
    }
    final Object id = address.getId();
    if ( null == id )
    {
      getTypeSubscriptionsObservable().preReportChanged();
    }
    else
    {
      getInstanceSubscriptionsObservable().preReportChanged();
    }
    final Subscription subscription = Subscription.create( Channel.create( address, filter ), explicitSubscription );
    final SubscriptionEntry entry = createSubscriptionEntry( subscription );
    if ( null == id )
    {
      _typeSubscriptions.put( address.getChannelType(), entry );
      getTypeSubscriptionsObservable().reportChanged();
    }
    else
    {
      _instanceSubscriptions
        .computeIfAbsent( address.getChannelType(), k -> new HashMap<>() )
        .put( id, subscription );
      getInstanceSubscriptionsObservable().reportChanged();
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
                         () -> unlinkSubscription( subscription.getChannel().getAddress() ),
                         true,
                         true );
    entry.setMonitor( monitor );
    return entry;
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
  public final Subscription findSubscription( @Nonnull final ChannelAddress address )
  {
    final Enum channelType = address.getChannelType();
    final Object id = address.getId();
    return null == id ? findTypeSubscription( channelType ) : findInstanceSubscription( channelType, id );
  }

  /**
   * Return the type subscription for the specified channelType.
   * This method will observe the <code>typeSubscriptions</code> property if not
   * found and the result {@link Subscription} if found. This ensures that if an observer
   * invokes this method then the observer will be rescheduled when the result changes.
   *
   * @param channelType the channel type.
   * @return the subscription if any matches.
   */
  @Nullable
  private Subscription findTypeSubscription( @Nonnull final Enum channelType )
  {
    final SubscriptionEntry entry = _typeSubscriptions.get( channelType );
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
   * @param channelType the channel type.
   * @param id          the channel id.
   * @return the subscription if any matches.
   */
  @Nullable
  private Subscription findInstanceSubscription( @Nonnull final Enum channelType, @Nonnull final Object id )
  {
    final Map<Object, Subscription> instanceMap = _instanceSubscriptions.get( channelType );
    if ( null == instanceMap )
    {
      getInstanceSubscriptionsObservable().reportObserved();
      return null;
    }
    else
    {
      final Subscription subscription = instanceMap.get( id );
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
    final Object id = address.getId();
    if ( null == id )
    {
      getTypeSubscriptionsObservable().preReportChanged();
      final SubscriptionEntry entry = _typeSubscriptions.remove( address.getChannelType() );
      if ( Replicant.shouldCheckInvariants() )
      {
        invariant( () -> null != entry,
                   () -> "unlinkSubscription invoked with address " + address +
                         " but no subscription with that address exists." );
        invariant( () -> Disposable.isDisposed( entry ),
                   () -> "unlinkSubscription invoked with address " + address +
                         " but subscription has not already been disposed." );
      }
      getTypeSubscriptionsObservable().reportChanged();
      return entry.getSubscription();
    }
    else
    {
      getInstanceSubscriptionsObservable().preReportChanged();
      final Map<Object, Subscription> instanceMap = _instanceSubscriptions.get( address.getChannelType() );
      if ( Replicant.shouldCheckInvariants() )
      {
        invariant( () -> null != instanceMap,
                   () -> "unlinkSubscription invoked with address " + address +
                         " but no subscription with that address exists." );
      }
      final Subscription subscription = instanceMap.remove( id );
      if ( Replicant.shouldCheckInvariants() )
      {
        invariant( () -> null != subscription,
                   () -> "unlinkSubscription invoked with address " + address +
                         " but no subscription with that address exists." );
        invariant( () -> Disposable.isDisposed( subscription ),
                   () -> "unlinkSubscription invoked with address " + address +
                         " but subscription has not already been disposed." );
      }
      getInstanceSubscriptionsObservable().reportChanged();
      return subscription;
    }
  }

  @PreDispose
  final void preDispose()
  {
    _typeSubscriptions.values().forEach( s -> Disposable.dispose( s ) );
    _instanceSubscriptions.values().stream().flatMap( t -> t.values().stream() ).forEach( Disposable::dispose );
  }
}
