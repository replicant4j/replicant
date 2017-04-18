package org.realityforge.replicant.client.runtime.ee;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.transaction.Transactional;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.client.EntitySystem;
import org.realityforge.replicant.client.runtime.AreaOfInterestService;
import org.realityforge.replicant.client.runtime.ContextConverger;
import org.realityforge.replicant.client.runtime.ContextConvergerImpl;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;

@Singleton
@Transactional( Transactional.TxType.NOT_SUPPORTED )
@Typed( ContextConverger.class )
public class EeContextConvergerImpl
  extends ContextConvergerImpl
{
  @Inject
  private EntitySystem _entitySystem;
  @Inject
  private AreaOfInterestService _areaOfInterestService;
  @Inject
  private ReplicantClientSystem _replicantClientSystem;
  @Replicant
  @Inject
  private ManagedScheduledExecutorService _scheduledExecutorService;

  private ScheduledFuture<?> _future;

  @PostConstruct
  protected void postConstruct()
  {
    addListeners();
  }

  @PreDestroy
  protected void preDestroy()
  {
    deactivate();
    release();
  }

  @Override
  public void activate()
  {
    deactivate();
    _future = _scheduledExecutorService.
      scheduleAtFixedRate( this::converge, 0, CONVERGE_DELAY_IN_MS, TimeUnit.MILLISECONDS );

  }

  @Override
  public void deactivate()
  {
    if ( null != _future )
    {
      _future.cancel( true );
      _future = null;
    }
  }

  @Override
  public boolean isActive()
  {
    return null != _future;
  }

  @Override
  @Nullable
  protected String filterToString( @Nullable final Object filter )
  {
    return JsonUtil.toJsonString( filter );
  }

  @Override
  @Nonnull
  protected EntitySubscriptionManager getSubscriptionManager()
  {
    return _entitySystem.getSubscriptionManager();
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
