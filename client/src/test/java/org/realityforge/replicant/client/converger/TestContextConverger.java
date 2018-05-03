package org.realityforge.replicant.client.converger;

import javax.annotation.Nonnull;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;
import replicant.SubscriptionService;

final class TestContextConverger
  extends ContextConverger
{
  TestContextConverger( @Nonnull final SubscriptionService subscriptionService,
                        @Nonnull final ReplicantClientSystem replicantClientSystem )
  {
    super( subscriptionService, replicantClientSystem );
  }

  @Override
  public void activate()
  {
  }

  @Override
  public void deactivate()
  {
  }

  @Override
  public boolean isActive()
  {
    return true;
  }
}
