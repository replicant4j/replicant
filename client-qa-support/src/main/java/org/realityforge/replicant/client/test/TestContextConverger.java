package org.realityforge.replicant.client.test;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.realityforge.replicant.client.aoi.AreaOfInterestService;
import org.realityforge.replicant.client.converger.ContextConverger;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;
import org.realityforge.replicant.client.subscription.SubscriptionService;

public class TestContextConverger
  extends ContextConverger
{
  private boolean _active;

  @Inject
  public TestContextConverger( @Nonnull final SubscriptionService subscriptionManager,
                               @Nonnull final AreaOfInterestService areaOfInterestService,
                               @Nonnull final ReplicantClientSystem replicantClientSystem )
  {
    super( subscriptionManager, areaOfInterestService, replicantClientSystem );
  }

  @Override
  public boolean isActive()
  {
    return _active;
  }

  @Override
  public void activate()
  {
    deactivate();
    _active = true;
  }

  @Override
  public void deactivate()
  {
    unpause();
    _active = false;
  }
}
