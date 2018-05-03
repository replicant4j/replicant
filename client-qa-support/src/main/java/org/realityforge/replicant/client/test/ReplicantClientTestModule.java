package org.realityforge.replicant.client.test;

import org.realityforge.guiceyloops.shared.AbstractModule;
import replicant.AreaOfInterestService;
import org.realityforge.replicant.client.converger.ContextConverger;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;
import replicant.EntityService;

/**
 * Module containing all the common client services.
 */
public class ReplicantClientTestModule
  extends AbstractModule
{
  @Override
  protected void configure()
  {
    bindEntitySubscriptionManager();
    bindContextConverger();
    bindReplicantClientSystem();
    bindAreaOfInterestService();
  }

  protected void bindEntitySubscriptionManager()
  {
    bind( EntityService.class ).toInstance( EntityService.create() );
  }

  protected void bindContextConverger()
  {
    bind( ContextConverger.class ).to( TestContextConverger.class ).asEagerSingleton();
  }

  protected void bindReplicantClientSystem()
  {
    bindMock( ReplicantClientSystem.class );
  }

  protected void bindAreaOfInterestService()
  {
    bind( AreaOfInterestService.class ).toInstance( AreaOfInterestService.create() );
  }
}
