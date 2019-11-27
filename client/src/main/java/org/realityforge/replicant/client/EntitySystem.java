package org.realityforge.replicant.client;

import javax.annotation.Nonnull;

/**
 * The service that exposes the EntityRepository, the EntityChangeBroker and the EntitySubscriptionManager.
 */
public interface EntitySystem
{
  @Nonnull
  EntityRepository getRepository();

  @Nonnull
  EntityChangeBroker getChangeBroker();

  @Nonnull
  EntitySubscriptionManager getSubscriptionManager();
}
