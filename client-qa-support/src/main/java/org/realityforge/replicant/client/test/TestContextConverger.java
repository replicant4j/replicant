package org.realityforge.replicant.client.test;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.client.aoi.AreaOfInterestService;
import org.realityforge.replicant.client.runtime.ContextConverger;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;

public class TestContextConverger
  extends ContextConverger
{
  private final EntitySubscriptionManager _subscriptionManager;
  private final AreaOfInterestService _areaOfInterestService;
  private final ReplicantClientSystem _replicantClientSystem;
  private boolean _active;

  @Inject
  public TestContextConverger( @Nonnull final EntitySubscriptionManager subscriptionManager,
                               @Nonnull final AreaOfInterestService areaOfInterestService,
                               @Nonnull final ReplicantClientSystem replicantClientSystem )
  {
    _subscriptionManager = Objects.requireNonNull( subscriptionManager );
    _areaOfInterestService = Objects.requireNonNull( areaOfInterestService );
    _replicantClientSystem = Objects.requireNonNull( replicantClientSystem );
    addListeners();
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

  @Override
  @Nonnull
  protected EntitySubscriptionManager getSubscriptionManager()
  {
    return _subscriptionManager;
  }

  @Override
  @Nonnull
  protected AreaOfInterestService getAreaOfInterestService()
  {
    return _areaOfInterestService;
  }

  @Override
  @Nonnull
  protected ReplicantClientSystem getReplicantClientSystem()
  {
    return _replicantClientSystem;
  }
}
