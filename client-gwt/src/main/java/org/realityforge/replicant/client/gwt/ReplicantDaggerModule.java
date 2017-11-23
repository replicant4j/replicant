package org.realityforge.replicant.client.gwt;

import dagger.Module;
import dagger.Provides;
import javax.annotation.Nonnull;
import javax.inject.Singleton;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.client.EntitySubscriptionManagerImpl;
import org.realityforge.replicant.client.transport.CacheService;

/**
 * A simple Dagger module that defines the repository and change broker services.
 */

@Module
public class ReplicantDaggerModule
{
  @Nonnull
  @Provides
  @Singleton
  public static EntitySubscriptionManager provideEntitySubscriptionManager( @Nonnull final EntitySubscriptionManagerImpl service )
  {
    return service;
  }

  @Nonnull
  @Provides
  @Singleton
  public static CacheService provideCacheService( @Nonnull final LocalCacheService service )
  {
    return service;
  }
}
