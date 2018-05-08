package org.realityforge.replicant.client.gwt;

import dagger.Module;
import dagger.Provides;
import javax.annotation.Nonnull;
import javax.inject.Singleton;
import org.realityforge.replicant.client.transport.CacheService;
import org.realityforge.replicant.client.transport.ConvergerDaggerModule;
import org.realityforge.replicant.client.transport.ReplicantClientSystem;

@Module( includes = { GwtReplicantClientSystemDaggerModule.class, ConvergerDaggerModule.class } )
public class ReplicantDaggerModule
{
  @Nonnull
  @Provides
  @Singleton
  public static CacheService provideCacheService( @Nonnull final LocalCacheService service )
  {
    return service;
  }

  @Nonnull
  @Provides
  @Singleton
  static ReplicantClientSystem provideReplicantClientSystem( @Nonnull final GwtReplicantClientSystem service )
  {
    return service;
  }
}
