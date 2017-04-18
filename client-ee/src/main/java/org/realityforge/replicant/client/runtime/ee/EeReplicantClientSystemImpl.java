package org.realityforge.replicant.client.runtime.ee;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.transaction.Transactional;
import org.realityforge.replicant.client.runtime.DataLoaderEntry;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;
import org.realityforge.replicant.client.runtime.ReplicantClientSystemImpl;

@Singleton
@Transactional( Transactional.TxType.NOT_SUPPORTED )
@Typed( ReplicantClientSystem.class )
public class EeReplicantClientSystemImpl
  extends ReplicantClientSystemImpl
{
  @Inject
  private BeanManager _beanManager;
  @Inject
  private DataLoaderEntry[] _dataLoaderEntries;
  @Replicant
  @Inject
  private ManagedScheduledExecutorService _scheduledExecutorService;

  private ScheduledFuture<?> _future;

  @PostConstruct
  protected void postConstruct()
  {
    setDataLoaders( _dataLoaderEntries );
    addReplicantSystemListener( new EeReplicantSystemListenerImpl( _beanManager ) );
    _future = _scheduledExecutorService.
      scheduleAtFixedRate( this::converge, 0, CONVERGE_DELAY_IN_MS, TimeUnit.MILLISECONDS );
  }

  @PreDestroy
  protected void preDestroy()
  {
    if ( null != _future )
    {
      _future.cancel( true );
      _future = null;
    }
    release();
  }
}
