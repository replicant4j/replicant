package org.realityforge.replicant.client.gwt;

import dagger.Module;
import dagger.Provides;
import javax.annotation.Nonnull;
import javax.inject.Singleton;
import replicant.spi.CacheService;
import replicant.spi.WebStorageCacheService;

@Module
public class ReplicantDaggerModule
{
  @Nonnull
  @Provides
  @Singleton
  public static CacheService provideCacheService( @Nonnull final WebStorageCacheService service )
  {
    return service;
  }
}
