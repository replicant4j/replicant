package org.realityforge.replicant.client.gwt;

import dagger.Module;
import dagger.Provides;
import javax.annotation.Nonnull;
import javax.inject.Singleton;
import org.realityforge.replicant.client.aoi.AreaOfInterestServiceDaggerModule;
import org.realityforge.replicant.client.converger.ContextConverger;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;

@Module( includes = { GwtContextConvergerDaggerModule.class, AreaOfInterestServiceDaggerModule.class } )
public interface ReplicantNetworkDaggerModule
{
  @Nonnull
  @Provides
  @Singleton
  static ContextConverger provideContextConverger( @Nonnull final GwtContextConverger component )
  {
    return component;
  }

  @Nonnull
  @Provides
  @Singleton
  static ReplicantClientSystem provideReplicantClientSystem( @Nonnull final GwtReplicantClientSystem service )
  {
    return service;
  }
}
