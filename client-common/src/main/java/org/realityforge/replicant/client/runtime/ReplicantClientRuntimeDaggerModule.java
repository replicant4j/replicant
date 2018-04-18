package org.realityforge.replicant.client.runtime;

import dagger.Module;
import dagger.Provides;
import javax.annotation.Nonnull;
import javax.inject.Singleton;

@Module( includes = { AreaOfInterestServiceDaggerModule.class } )
public interface ReplicantClientRuntimeDaggerModule
{
  @Nonnull
  @Provides
  @Singleton
  static ReplicantConnection provideReplicantConnection( @Nonnull final ReplicantConnectionImpl service )
  {
    return service;
  }
}
