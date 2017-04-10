package org.realityforge.replicant.client.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.ChannelDescriptor;

public class AreaOfInterestServiceImpl
  implements AreaOfInterestService
{
  private final HashMap<String, Scope> _scopes = new HashMap<>();
  private final HashMap<ChannelDescriptor, Subscription> _subscriptions = new HashMap<>();
  private final AreaOfInterestListenerSupport _listeners = new AreaOfInterestListenerSupport();

  @Override
  public boolean addAreaOfInterestListener( @Nonnull final AreaOfInterestListener listener )
  {
    return _listeners.addListener( Objects.requireNonNull( listener ) );
  }

  @Override
  public boolean removeAreaOfInterestListener( @Nonnull final AreaOfInterestListener listener )
  {
    return _listeners.removeListener( Objects.requireNonNull( listener ) );
  }

  @Nonnull
  @Override
  public Map<String, Scope> getScopeMap()
  {
    return Collections.unmodifiableMap( _scopes );
  }

  @Nullable
  @Override
  public Scope findScope( @Nonnull final String name )
  {
    return _scopes.get( name );
  }

  @Nonnull
  @Override
  public ScopeReference createScopeReference( @Nonnull final String name )
  {
    return findOrCreateScope( name ).createReference();
  }

  @Override
  public void destroyScope( @Nonnull final Scope scope )
  {
    if ( scope.isActive() && null != _scopes.remove( scope.getName() ) )
    {
      scope.delete();
      _listeners.scopeDeleted( scope );
    }
  }

  @Nonnull
  @Override
  public Map<ChannelDescriptor, Subscription> getSubscriptionsMap()
  {
    return Collections.unmodifiableMap( _subscriptions );
  }

  @Nullable
  @Override
  public Subscription findSubscription( @Nonnull final ChannelDescriptor channel )
  {
    return _subscriptions.get( channel );
  }

  @Nonnull
  @Override
  public SubscriptionReference createSubscriptionReference( @Nonnull final Scope scope,
                                                            @Nonnull final ChannelDescriptor channel )
  {
    assert scope.isActive();
    return findOrCreateSubscription( channel ).createReference();
  }

  @Override
  public void updateSubscription( @Nonnull final Subscription subscription, @Nullable final Object filter )
  {
    assert subscription.isActive();
    subscription.setFilter( filter );
    _listeners.subscriptionUpdated( subscription );
  }

  @Override
  public void destroySubscription( @Nonnull final Subscription subscription )
  {
    if ( subscription.isActive() && null != _subscriptions.remove( subscription.getDescriptor() ) )
    {
      subscription.delete();
      _listeners.subscriptionDeleted( subscription );
    }
  }

  @Nonnull
  protected Scope newScope( @Nonnull final String name )
  {
    assert !_scopes.containsKey( name );
    final Scope scope = new Scope( this, name );
    _scopes.put( scope.getName(), scope );
    _listeners.scopeCreated( scope );
    return scope;
  }

  @Nonnull
  protected Scope findOrCreateScope( @Nonnull final String name )
  {
    final Scope scope = findScope( name );
    return null != scope ? scope : newScope( name );
  }

  @Nonnull
  protected Subscription newSubscription( @Nonnull final ChannelDescriptor descriptor )
  {
    assert !_subscriptions.containsKey( descriptor );
    final Subscription subscription = new Subscription( this, descriptor );
    _subscriptions.put( subscription.getDescriptor(), subscription );
    _listeners.subscriptionCreated( subscription );
    return subscription;
  }

  @Nonnull
  protected Subscription findOrCreateSubscription( @Nonnull final ChannelDescriptor descriptor )
  {
    final Subscription subscription = findSubscription( descriptor );
    return null != subscription ? subscription : newSubscription( descriptor );
  }
}
