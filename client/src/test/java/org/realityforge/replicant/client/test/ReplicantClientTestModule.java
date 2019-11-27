package org.realityforge.replicant.client.test;

import com.google.inject.Provides;
import javax.inject.Singleton;
import org.realityforge.guiceyloops.shared.AbstractModule;
import org.realityforge.replicant.client.EntityChangeBroker;
import org.realityforge.replicant.client.EntityRepository;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.client.EntitySystem;
import org.realityforge.replicant.client.EntitySystemImpl;
import org.realityforge.replicant.client.runtime.AreaOfInterestService;
import org.realityforge.replicant.client.runtime.AreaOfInterestServiceImpl;
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
    bindEntitySystem();
    bindContextConverger();
    bindReplicantClientSystem();
    bindAreaOfInterestService();
  }

  protected void bindEntitySystem()
  {
    bind( EntitySystem.class ).to( EntitySystemImpl.class ).asEagerSingleton();
  }

  protected void bindContextConverger()
  {
    bind( ContextConverger.class ).to( TestContextConvergerImpl.class ).asEagerSingleton();
  }

  protected void bindReplicantClientSystem()
  {
    bindMock( ReplicantClientSystem.class );
  }

  protected void bindAreaOfInterestService()
  {
    bind( AreaOfInterestService.class ).to( AreaOfInterestServiceImpl.class ).asEagerSingleton();
  }

  @Provides
  @Singleton
  public final EntityRepository getEntityRepository( final EntitySystem system )
  {
    return system.getRepository();
  }

  @Provides
  @Singleton
  public final EntityChangeBroker getEntityChangeBroker( final EntitySystem system )
  {
    return system.getChangeBroker();
  }

  @Provides
  @Singleton
  public final EntitySubscriptionManager getEntitySubscriptionManager( final EntitySystem system )
  {
    return system.getSubscriptionManager();
  }
}
