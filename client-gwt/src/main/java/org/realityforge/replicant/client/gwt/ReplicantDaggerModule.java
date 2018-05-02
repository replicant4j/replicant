package org.realityforge.replicant.client.gwt;

import dagger.Module;
import dagger.Provides;
import javax.annotation.Nonnull;
import javax.inject.Singleton;
import org.realityforge.replicant.client.subscription.EntityServiceDaggerModule;
import org.realityforge.replicant.client.subscription.SubscriptionServiceDaggerModule;
import org.realityforge.replicant.client.transport.CacheService;

@Module( includes = { EntityServiceDaggerModule.class, SubscriptionServiceDaggerModule.class } )
public class ReplicantDaggerModule
{
  @Nonnull
  @Provides
  @Singleton
  public static CacheService provideCacheService( @Nonnull final LocalCacheService service )
  {
    return service;
  }
}
