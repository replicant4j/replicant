package org.realityforge.replicant.client.runtime.gwt;

import dagger.Module;
import dagger.Provides;
import javax.annotation.Nonnull;
import javax.inject.Singleton;
import org.realityforge.replicant.client.runtime.ContextConverger;
import org.realityforge.replicant.client.runtime.ReplicantClientRuntimeDaggerModule;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;

@Module( includes = { GwtContextConvergerImplDaggerModule.class, ReplicantClientRuntimeDaggerModule.class } )
public interface ReplicantNetworkDaggerModule
{
  @Nonnull
  @Provides
  @Singleton
  static ContextConverger provideContextConverger( @Nonnull final GwtContextConvergerImpl component )
  {
    return component;
  }

  @Nonnull
  @Provides
  @Singleton
  static ReplicantClientSystem provideReplicantClientSystem( @Nonnull final GwtReplicantClientSystemImpl service )
  {
    return service;
  }
}
