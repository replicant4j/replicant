package org.realityforge.replicant.client;

import dagger.Module;
import dagger.Provides;
import javax.annotation.Nonnull;
import javax.inject.Singleton;

@Module
public interface ReplicantClientDaggerModule
{
  @Nonnull
  @Provides
  @Singleton
  static EntitySubscriptionManager provideEntitySubscriptionManager( @Nonnull final EntitySubscriptionManagerImpl service )
  {
    return service;
  }
}
