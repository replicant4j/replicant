package org.realityforge.replicant.client.test;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.client.runtime.AreaOfInterestService;
import org.realityforge.replicant.client.runtime.ContextConvergerImpl;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;

public class TestContextConvergerImpl
  extends ContextConvergerImpl
{
  private final EntitySubscriptionManager _subscriptionManager;
  private final AreaOfInterestService _areaOfInterestService;
  private final ReplicantClientSystem _replicantClientSystem;
  private boolean _active;

  @Inject
  public TestContextConvergerImpl( @Nonnull final EntitySubscriptionManager subscriptionManager,
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

  @Override
  @Nullable
  protected String filterToString( @Nullable final Object filter )
  {
    return Objects.toString( filter );
  }
}
