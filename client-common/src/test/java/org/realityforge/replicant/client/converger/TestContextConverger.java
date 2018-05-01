package org.realityforge.replicant.client.converger;

import javax.annotation.Nonnull;
import org.realityforge.replicant.client.aoi.AreaOfInterestService;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;
import org.realityforge.replicant.client.subscription.EntitySubscriptionManager;

final class TestContextConverger
  extends ContextConverger
{
  TestContextConverger( @Nonnull final EntitySubscriptionManager subscriptionManager,
                        @Nonnull final AreaOfInterestService areaOfInterestService,
                        @Nonnull final ReplicantClientSystem replicantClientSystem )
  {
    super( subscriptionManager, areaOfInterestService, replicantClientSystem );
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
