package org.realityforge.replicant.client.test;

import org.realityforge.guiceyloops.shared.AbstractModule;
import org.realityforge.replicant.client.Arez_EntitySubscriptionManager;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.client.runtime.AreaOfInterestService;
import org.realityforge.replicant.client.runtime.Arez_AreaOfInterestService;
import org.realityforge.replicant.client.runtime.ContextConverger;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;

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
    bind( EntitySubscriptionManager.class ).to( Arez_EntitySubscriptionManager.class ).asEagerSingleton();
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
    bind( AreaOfInterestService.class ).to( Arez_AreaOfInterestService.class ).asEagerSingleton();
  }
}
