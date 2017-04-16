package org.realityforge.replicant.client.runtime;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.EntitySubscriptionManager;

final class TestContextConvergerImpl
  extends ContextConvergerImpl
{
  private final EntitySubscriptionManager _subscriptionManager;
  private final AreaOfInterestService _areaOfInterestService;
  private final ReplicantClientSystem _replicantClientSystem;

  TestContextConvergerImpl( final EntitySubscriptionManager subscriptionManager,
                            final AreaOfInterestService areaOfInterestService,
                            final ReplicantClientSystem replicantClientSystem )
  {
    _subscriptionManager = subscriptionManager;
    _areaOfInterestService = areaOfInterestService;
    _replicantClientSystem = replicantClientSystem;
  }

  @Nullable
  @Override
  protected String filterToString( @Nullable final Object filter )
  {
    return Objects.toString( filter );
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
