package org.realityforge.replicant.client.runtime;

import arez.Arez;
import arez.Disposable;
import arez.annotations.ArezComponent;
import arez.annotations.Observable;
import arez.annotations.ObservableRef;
import arez.annotations.PreDispose;
import arez.component.ComponentObservable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import org.realityforge.braincheck.Guards;
import org.realityforge.replicant.client.ChannelDescriptor;
import org.realityforge.replicant.client.FilterUtil;

/**
 * The AreaOfInterestService is responsible for managing the expected subscriptions
 * that the client is interested in. The subscriptions may cross different replicant
 * systems, and may exist before the data sources have been connected. The AreaOfInterestService
 * intends to represent the desired state that the DataSources converge towards.
 */
@Singleton
@ArezComponent( allowEmpty = true )
public abstract class AreaOfInterestService
{
  private final HashMap<ChannelDescriptor, Subscription> _subscriptions = new HashMap<>();
  private final AreaOfInterestListenerSupport _listeners = new AreaOfInterestListenerSupport();

  public AreaOfInterestService()
  {
  }

  public boolean addAreaOfInterestListener( @Nonnull final AreaOfInterestListener listener )
  {
    return _listeners.addListener( Objects.requireNonNull( listener ) );
  }

  public boolean removeAreaOfInterestListener( @Nonnull final AreaOfInterestListener listener )
  {
    return _listeners.removeListener( Objects.requireNonNull( listener ) );
  }

  @ObservableRef
  @Nonnull
  abstract arez.Observable getSubscriptionsObservable();

  @Observable( name = "subscriptions", expectSetter = false )
  @Nonnull
  public Map<ChannelDescriptor, Subscription> getSubscriptionsMap()
  {
    return Arez.areRepositoryResultsModifiable() ? _subscriptions : Collections.unmodifiableMap( _subscriptions );
  }

  @Nonnull
  public Collection<Subscription> getSubscriptions()
  {
    return getSubscriptionsMap().values();
  }

  @Nonnull
  public Collection<ChannelDescriptor> getSubscriptionsChannels()
  {
    return getSubscriptionsMap().keySet();
  }

  @Nullable
  public Subscription findSubscription( @Nonnull final ChannelDescriptor channel )
  {
    final Subscription subscription = _subscriptions.get( channel );
    if ( null != subscription && !Disposable.isDisposed( subscription ) )
    {
      ComponentObservable.observe( subscription );
      return subscription;
    }
    else
    {
      getSubscriptionsObservable().reportObserved();
      return null;
    }
  }

  public void updateSubscription( @Nonnull final Subscription subscription, @Nullable final Object filter )
  {
    assert subscription.isActive();
    subscription.setFilter( filter );
    _listeners.subscriptionUpdated( subscription );
  }

  public void destroySubscription( @Nonnull final Subscription subscription )
  {
    if ( null != _subscriptions.remove( subscription.getDescriptor() ) )
    {
      getSubscriptionsObservable().preReportChanged();
      if ( subscription.isActive() )
      {
        subscription.delete();
        _listeners.subscriptionDeleted( subscription );
        Disposable.dispose( subscription );
      }
      getSubscriptionsObservable().reportChanged();
    }
    else
    {
      Guards.fail( () -> "Called AreaOfInterestService.destroySubscription() passing a subscription that was " +
                         "not in the repository. Subscription: " + subscription );
    }
  }

  @Nonnull
  public Subscription createSubscription( @Nonnull final ChannelDescriptor descriptor,
                                          @Nullable final Object filter )
  {
    if ( _subscriptions.containsKey( descriptor ) )
    {
      throw new SubscriptionExistsException( _subscriptions.get( descriptor ) );
    }
    else
    {
      final Subscription subscription = new Subscription( this, descriptor );
      subscription.setFilter( filter );
      _subscriptions.put( subscription.getDescriptor(), subscription );
      _listeners.subscriptionCreated( subscription );
      return subscription;
    }
  }

  @Nonnull
  public Subscription findOrCreateSubscription( @Nonnull final ChannelDescriptor channel,
                                                @Nullable final Object filter )
  {
    final Subscription subscription = findSubscription( channel );
    if ( null != subscription )
    {
      if ( !FilterUtil.filtersEqual( subscription.getFilter(), filter ) )
      {
        updateSubscription( subscription, filter );
      }
      return subscription;
    }
    else
    {
      return createSubscription( channel, filter );
    }
  }

  @PreDispose
  protected void preDispose()
  {
    getSubscriptionsObservable().preReportChanged();
    _subscriptions.values().forEach( Disposable::dispose );
    _subscriptions.clear();
    getSubscriptionsObservable().reportChanged();
  }
}
