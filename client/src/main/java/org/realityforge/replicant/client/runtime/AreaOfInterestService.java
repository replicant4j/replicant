package org.realityforge.replicant.client.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.ChannelAddress;

/**
 * The AreaOfInterestService is responsible for managing the expected subscriptions
 * that the client is interested in. The subscriptions may cross different replicant
 * systems, and may exist before the data sources have been connected. The AreaOfInterestService
 * intends to represent the desired state that the DataSources converge towards.
 */
public interface AreaOfInterestService
{
  boolean addAreaOfInterestListener( @Nonnull AreaOfInterestListener listener );

  boolean removeAreaOfInterestListener( @Nonnull AreaOfInterestListener listener );

  @Nonnull
  Map<String, Scope> getScopeMap();

  @Nonnull
  default Collection<String> getScopeNames()
  {
    return getScopeMap().keySet();
  }

  @Nonnull
  default Collection<Scope> getScopes()
  {
    return getScopeMap().values();
  }

  @Nullable
  Scope findScope( @Nonnull String name );

  @Nonnull
  ScopeReference createScopeReference( @Nonnull String name );

  void destroyScope( @Nonnull Scope scope );

  @Nonnull
  Map<ChannelAddress, Subscription> getSubscriptionsMap();

  @Nonnull
  default Collection<ChannelAddress> getSubscriptionsChannels()
  {
    return getSubscriptionsMap().keySet();
  }

  @Nullable
  Subscription findSubscription( @Nonnull ChannelAddress channel );

  @Nonnull
  SubscriptionReference createSubscriptionReference( @Nonnull Scope scope, @Nonnull ChannelAddress channel );

  @Nonnull
  Subscription createSubscription( @Nonnull ChannelAddress descriptor, @Nullable Object filter )
    throws SubscriptionExistsException;

  void updateSubscription( @Nonnull Subscription subscription, @Nullable Object filter );

  void destroySubscription( @Nonnull Subscription subscription );

  /**
   * Find or create a scope with specified name.
   * Note that if a scope is needs to be created then a scope reference will also be
   * created but will not be retained. In this scenario it is necessary for the application
   * to use other mechanisms to manage liveness of scopes.
   */
  @Nonnull
  default Scope findOrCreateScope( @Nonnull final String scopeName )
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
  default void releaseScopesExcept( @Nonnull final String... scopeNames )
  {
    new ArrayList<>( getScopes() ).
      stream().filter( s -> !isExcepted( s, scopeNames ) ).forEach( Scope::release );
  }

  /**
   * TODO: Mark this as private in Java9
   */
  default boolean isExcepted( @Nonnull final Scope scope, @Nonnull final String[] scopeNames )
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

}