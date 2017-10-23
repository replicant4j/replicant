package org.realityforge.replicant.client.gwt;

import com.google.gwt.inject.client.AbstractGinModule;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.client.EntitySubscriptionManagerImpl;
import org.realityforge.replicant.client.transport.CacheService;

/**
 * A simple GIN module that defines the repository and change broker services.
 */
public class ReplicantGinModule
  extends AbstractGinModule
{
  @Override
  protected void configure()
  {
    bindEntitySubscriptionManager();
    bindCacheService();
  }

  protected void bindEntitySubscriptionManager()
  {
    bind( EntitySubscriptionManager.class ).to( EntitySubscriptionManagerImpl.class ).asEagerSingleton();
  }

  protected void bindCacheService()
  {
    bind( CacheService.class ).to( LocalCacheService.class ).asEagerSingleton();
  }
}
