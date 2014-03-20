package org.realityforge.replicant.client.json.gwt;

import com.google.gwt.inject.client.AbstractGinModule;
import org.realityforge.replicant.client.EntityChangeBroker;
import org.realityforge.replicant.client.EntityChangeBrokerImpl;
import org.realityforge.replicant.client.EntityRepository;
import org.realityforge.replicant.client.EntityRepositoryImpl;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.client.EntitySubscriptionManagerImpl;

/**
 * A simple GIN module that defines the repository and change broker services.
 */
public class ReplicantGinModule
  extends AbstractGinModule
{
  @Override
  protected void configure()
  {
    bind( EntityRepository.class ).to( EntityRepositoryImpl.class ).asEagerSingleton();
    bind( EntityChangeBroker.class ).to( EntityChangeBrokerImpl.class ).asEagerSingleton();
    bind( EntitySubscriptionManager.class ).to( EntitySubscriptionManagerImpl.class ).asEagerSingleton();
  }
}
