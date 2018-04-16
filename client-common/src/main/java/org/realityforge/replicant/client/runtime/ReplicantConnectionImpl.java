package org.realityforge.replicant.client.runtime;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.realityforge.replicant.client.EntityLocator;
import org.realityforge.replicant.client.EntitySubscriptionManager;

@Singleton
public class ReplicantConnectionImpl
  implements ReplicantConnection
{
  private final EntityLocator _entityLocator;
  private final EntitySubscriptionManager _subscriptionManager;
  private final ReplicantClientSystem _replicantClientSystem;
  private final AreaOfInterestService _areaOfInterestService;
  private final ContextConverger _converger;

  @Inject
  public ReplicantConnectionImpl( @Nonnull final ContextConverger converger,
                                  @Nonnull final EntityLocator entityLocator,
                                  @Nonnull final EntitySubscriptionManager subscriptionManager,
                                  @Nonnull final ReplicantClientSystem replicantClientSystem,
                                  @Nonnull final AreaOfInterestService areaOfInterestService )
  {
    _areaOfInterestService = Objects.requireNonNull( areaOfInterestService );
    _entityLocator = Objects.requireNonNull( entityLocator );
    _subscriptionManager = Objects.requireNonNull( subscriptionManager );
    _replicantClientSystem = Objects.requireNonNull( replicantClientSystem );
    _converger = Objects.requireNonNull( converger );
  }

  @Override
  public void disconnect()
  {
    _converger.deactivate();
    _replicantClientSystem.deactivate();
  }

  @Override
  public void connect()
  {
    _replicantClientSystem.activate();
    _converger.activate();
  }

  @Nonnull
  @Override
  public AreaOfInterestService getAreaOfInterestService()
  {
    return _areaOfInterestService;
  }

  @Nonnull
  @Override
  public ContextConverger getContextConverger()
  {
    return _converger;
  }

  @Nonnull
  @Override
  public EntityLocator getEntityLocator()
  {
    return _entityLocator;
  }

  @Nonnull
  @Override
  public EntitySubscriptionManager getSubscriptionManager()
  {
    return _subscriptionManager;
  }

  @Nonnull
  @Override
  public ReplicantClientSystem getReplicantClientSystem()
  {
    return _replicantClientSystem;
  }
}
