package org.realityforge.replicant.client.runtime;

import java.util.Collection;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.ChannelDescriptor;

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
  Map<ChannelDescriptor, Subscription> getSubscriptionsMap();

  @Nonnull
  default Collection<ChannelDescriptor> getSubscriptionsChannels()
  {
    return getSubscriptionsMap().keySet();
  }

  @Nullable
  Subscription findSubscription( @Nonnull ChannelDescriptor channel );

  @Nonnull
  SubscriptionReference createSubscriptionReference( @Nonnull Scope scope, @Nonnull ChannelDescriptor channel );

  @Nonnull
  Subscription createSubscription( @Nonnull ChannelDescriptor descriptor, @Nullable Object filter )
    throws SubscriptionExistsException;

  void updateSubscription( @Nonnull Subscription subscription, @Nullable Object filter );

  void destroySubscription( @Nonnull Subscription subscription );
}
