package org.realityforge.replicant.client.gwt;

import dagger.Module;
import dagger.Provides;
import javax.annotation.Nonnull;
import javax.inject.Singleton;
import org.realityforge.replicant.client.transport.CacheService;
import org.realityforge.replicant.client.transport.ConvergerDaggerModule;

@Module( includes = { ConvergerDaggerModule.class } )
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
