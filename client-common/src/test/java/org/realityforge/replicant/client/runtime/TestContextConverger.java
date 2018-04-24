package org.realityforge.replicant.client.runtime;

import javax.annotation.Nonnull;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.client.aoi.AreaOfInterestService;

final class TestContextConverger
  extends ContextConverger
{
  private final EntitySubscriptionManager _subscriptionManager;
  private final AreaOfInterestService _areaOfInterestService;
  private final ReplicantClientSystem _replicantClientSystem;

  TestContextConverger( final EntitySubscriptionManager subscriptionManager,
                        final AreaOfInterestService areaOfInterestService,
                        final ReplicantClientSystem replicantClientSystem )
  {
    _subscriptionManager = subscriptionManager;
    _areaOfInterestService = areaOfInterestService;
    _replicantClientSystem = replicantClientSystem;
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

  @Nonnull
  @Override
  protected EntitySubscriptionManager getSubscriptionManager()
  {
    return _subscriptionManager;
  }

  @Nonnull
  @Override
  protected AreaOfInterestService getAreaOfInterestService()
  {
    return _areaOfInterestService;
  }

  @Nonnull
  @Override
  protected ReplicantClientSystem getReplicantClientSystem()
  {
    return _replicantClientSystem;
  }
}
