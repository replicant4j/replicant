package org.realityforge.replicant.client.runtime.gwt;

import com.google.gwt.inject.client.AbstractGinModule;
import org.realityforge.replicant.client.runtime.AreaOfInterestService;
import org.realityforge.replicant.client.runtime.AreaOfInterestServiceImpl;
import org.realityforge.replicant.client.runtime.ContextConverger;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;

public class ReplicantNetworkModule
  extends AbstractGinModule
{
  @Override
  protected void configure()
  {
    bindAreaOfInterestService();
    bindReplicantClientSystem();
    bindContextConverger();
  }

  private void bindContextConverger()
  {
    bind( ContextConverger.class ).to( GwtContextConvergerImpl.class ).asEagerSingleton();
  }

  protected void bindReplicantClientSystem()
  {
    bind( ReplicantClientSystem.class ).to( GwtReplicantClientSystemImpl.class ).asEagerSingleton();
  }

  protected void bindAreaOfInterestService()
  {
    bind( AreaOfInterestService.class ).to( AreaOfInterestServiceImpl.class ).asEagerSingleton();
  }
}
