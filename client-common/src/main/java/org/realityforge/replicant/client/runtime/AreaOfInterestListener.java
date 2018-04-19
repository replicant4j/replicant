package org.realityforge.replicant.client.runtime;

import javax.annotation.Nonnull;

public interface AreaOfInterestListener
{
  void subscriptionCreated( @Nonnull Subscription subscription );

  void subscriptionUpdated( @Nonnull Subscription subscription );

  void subscriptionDeleted( @Nonnull Subscription subscription );
}
