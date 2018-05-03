package org.realityforge.replicant.client.converger;

import javax.annotation.Nonnull;
import replicant.AreaOfInterestService;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;
import replicant.SubscriptionService;

final class TestContextConverger
  extends ContextConverger
{
  TestContextConverger( @Nonnull final SubscriptionService subscriptionService,
                        @Nonnull final AreaOfInterestService areaOfInterestService,
                        @Nonnull final ReplicantClientSystem replicantClientSystem )
  {
    super( subscriptionService, areaOfInterestService, replicantClientSystem );
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
