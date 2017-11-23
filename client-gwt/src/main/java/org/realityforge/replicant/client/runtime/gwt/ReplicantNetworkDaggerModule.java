package org.realityforge.replicant.client.runtime.gwt;

import dagger.Module;
import dagger.Provides;
import javax.annotation.Nonnull;
import javax.inject.Singleton;
import org.realityforge.replicant.client.runtime.AreaOfInterestService;
import org.realityforge.replicant.client.runtime.AreaOfInterestServiceImpl;
import org.realityforge.replicant.client.runtime.ContextConverger;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;

@Module
public class ReplicantNetworkDaggerModule
{
  @Nonnull
  @Provides
  @Singleton
  public static ContextConverger provideContextConverger( @Nonnull final GwtContextConvergerImpl service )
  {
    return service;
  }

  @Nonnull
  @Provides
  @Singleton
  public static ReplicantClientSystem provideReplicantClientSystem( @Nonnull final GwtReplicantClientSystemImpl service )
  {
    return service;
  }

  @Nonnull
  @Provides
  @Singleton
  public static AreaOfInterestService provideAreaOfInterestService( @Nonnull final AreaOfInterestServiceImpl service )
  {
    return service;
  }
}
