package org.realityforge.replicant.client;

import javax.annotation.Nonnull;

/**
 * The service that exposes the EntityRepository and the EntitySubscriptionManager.
 */
public interface EntitySystem
{
  @Nonnull
  EntityRepository getRepository();

  @Nonnull
  EntitySubscriptionManager getSubscriptionManager();
}
