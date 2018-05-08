package org.realityforge.replicant.client.gwt;

import dagger.Module;
import dagger.Provides;
import javax.annotation.Nonnull;
import javax.inject.Singleton;
import org.realityforge.replicant.client.transport.ConvergerDaggerModule;
import org.realityforge.replicant.client.transport.ReplicantClientSystem;
import replicant.AreaOfInterestServiceDaggerModule;

@Module( includes = { GwtReplicantClientSystemDaggerModule.class, ConvergerDaggerModule.class, AreaOfInterestServiceDaggerModule.class } )
public interface ReplicantNetworkDaggerModule
{
  @Nonnull
  @Provides
  @Singleton
  static ReplicantClientSystem provideReplicantClientSystem( @Nonnull final GwtReplicantClientSystem service )
  {
    return service;
  }
}
