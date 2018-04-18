package org.realityforge.replicant.client.runtime;

import arez.annotations.ArezComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import org.realityforge.replicant.client.ChannelDescriptor;

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
  private final HashMap<String, Scope> _scopes = new HashMap<>();
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

  @Nonnull
  public Map<String, Scope> getScopeMap()
  {
    return Collections.unmodifiableMap( _scopes );
  }

  @Nonnull
  public Collection<String> getScopeNames()
  {
    return getScopeMap().keySet();
  }

  @Nonnull
  public Collection<Scope> getScopes()
  {
    return getScopeMap().values();
  }

  @Nullable
  public Scope findScope( @Nonnull final String name )
  {
    return _scopes.get( name );
  }

  @Nonnull
  public ScopeReference createScopeReference( @Nonnull final String name )
  {
    return _findOrCreateScope( name ).createReference();
  }

  public void destroyScope( @Nonnull final Scope scope )
  {
    if ( scope.isActive() && null != _scopes.remove( scope.getName() ) )
    {
      scope.delete();
      _listeners.scopeDeleted( scope );
    }
  }

  /**
   * Find or create a scope with specified name.
   * Note that if a scope is needs to be created then a scope reference will also be
   * created but will not be retained. In this scenario it is necessary for the application
   * to use other mechanisms to manage liveness of scopes.
   */
  @Nonnull
  public Scope findOrCreateScope( @Nonnull final String scopeName )
  {
    final Scope scope = findScope( scopeName );
    if ( null != scope )
    {
      return scope;
    }
    else
    {
      return createScopeReference( scopeName ).getScope();
    }
  }

  /**
   * Release all scopes except for those named.
   * This is typically used when the caller has not kept references to the ScopeReferences and needs to align
   * the state of the world with new structure.
   */
  public void releaseScopesExcept( @Nonnull final String... scopeNames )
  {
    new ArrayList<>( getScopes() ).
      stream().filter( s -> !isExcepted( s, scopeNames ) ).forEach( Scope::release );
  }

  private boolean isExcepted( @Nonnull final Scope scope, @Nonnull final String[] scopeNames )
  {
    for ( final String scopeName : scopeNames )
    {
      if ( null != scopeName && scopeName.equals( scope.getName() ) )
      {
        return true;
      }
    }
    return false;
  }

  @Nonnull
  public Map<ChannelDescriptor, Subscription> getSubscriptionsMap()
  {
    return Collections.unmodifiableMap( _subscriptions );
  }

  @Nonnull
  public Collection<ChannelDescriptor> getSubscriptionsChannels()
  {
    return getSubscriptionsMap().keySet();
  }

  @Nullable
  public Subscription findSubscription( @Nonnull final ChannelDescriptor channel )
  {
    return _subscriptions.get( channel );
  }

  @Nonnull
  public SubscriptionReference createSubscriptionReference( @Nonnull final Scope scope,
                                                            @Nonnull final ChannelDescriptor channel )
  {
    assert scope.isActive();
    return scope.requireSubscription( findOrCreateSubscription( channel ) );
  }

  public void updateSubscription( @Nonnull final Subscription subscription, @Nullable final Object filter )
  {
    assert subscription.isActive();
    subscription.setFilter( filter );
    _listeners.subscriptionUpdated( subscription );
  }

  public void destroySubscription( @Nonnull final Subscription subscription )
  {
    if ( subscription.isActive() && null != _subscriptions.remove( subscription.getDescriptor() ) )
    {
      subscription.delete();
      _listeners.subscriptionDeleted( subscription );
    }
  }

  @Nonnull
  protected Scope createScope( @Nonnull final String name )
  {
    assert !_scopes.containsKey( name );
    final Scope scope = new Scope( this, name );
    _scopes.put( scope.getName(), scope );
    _listeners.scopeCreated( scope );
    return scope;
  }

  /**
   * Find or create a scope without creating a reference.
   */
  @Nonnull
  protected Scope _findOrCreateScope( @Nonnull final String name )
  {
    final Scope scope = findScope( name );
    return null != scope ? scope : createScope( name );
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
  protected Subscription findOrCreateSubscription( @Nonnull final ChannelDescriptor descriptor )
  {
    final Subscription subscription = findSubscription( descriptor );
    return null != subscription ? subscription : createSubscription( descriptor, null );
  }
}
