package org.realityforge.replicant.client.test;

import org.realityforge.guiceyloops.shared.AbstractModule;
import org.realityforge.replicant.client.EntityChangeBroker;
import org.realityforge.replicant.client.EntityChangeBrokerImpl;
import org.realityforge.replicant.client.EntityRepository;
import org.realityforge.replicant.client.EntityRepositoryImpl;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.client.EntitySubscriptionManagerImpl;

/**
 * Module containing all the common client services.
 */
public final class ReplicantClientTestModule
  extends AbstractModule
{
  @Override
  protected void configure()
  {
    bindSingleton( EntityRepository.class, EntityRepositoryImpl.class );
    bindSingleton( EntityChangeBroker.class, EntityChangeBrokerImpl.class );
    bindSingleton( EntitySubscriptionManager.class, EntitySubscriptionManagerImpl.class );
  }
}
