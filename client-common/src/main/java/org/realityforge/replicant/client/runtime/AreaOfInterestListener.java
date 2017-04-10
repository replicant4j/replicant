package org.realityforge.replicant.client.runtime;

import javax.annotation.Nonnull;

public interface AreaOfInterestListener
{
  void scopeCreated( @Nonnull Scope scope );

  void scopeDeleted( @Nonnull Scope scope );

  void subscriptionCreated( @Nonnull Subscription subscription );

  void subscriptionUpdated( @Nonnull Subscription subscription );

  void subscriptionDeleted( @Nonnull Subscription subscription );
}
