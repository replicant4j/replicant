package org.realityforge.replicant.client.ee;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.concurrent.ContextService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import org.realityforge.gwt.webpoller.client.WebPoller;
import org.realityforge.replicant.client.ChangeSet;
import org.realityforge.replicant.client.transport.CacheService;
import org.realityforge.replicant.client.transport.ClientSession;
import org.realityforge.replicant.client.transport.DataLoaderListener;
import org.realityforge.replicant.client.transport.DataLoaderServiceConfig;
import org.realityforge.replicant.client.transport.WebPollerDataLoaderService;

public abstract class EeDataLoaderService
  extends WebPollerDataLoaderService
{
  private static final int DEFAULT_EE_CHANGES_TO_PROCESS_PER_TICK = 10000;
  private static final int DEFAULT_EE_LINKS_TO_PROCESS_PER_TICK = 10000;

  @Nullable
  private ScheduledFuture _future;
  private final InMemoryCacheService _cacheService = new InMemoryCacheService();
  private final ReentrantReadWriteLock _lock = new ReentrantReadWriteLock( true );
  private DataLoaderServiceConfig _config;

  protected EeDataLoaderService()
  {
    setChangesToProcessPerTick( DEFAULT_EE_CHANGES_TO_PROCESS_PER_TICK );
    setLinksToProcessPerTick( DEFAULT_EE_LINKS_TO_PROCESS_PER_TICK );
  }

  protected void postConstruct()
  {
    _config = getContextService().
      createContextualProxy( new EeDataLoaderServiceConfigImpl( getJndiPrefix() ), DataLoaderServiceConfig.class );
  }

  @Nonnull
  @Override
  protected DataLoaderServiceConfig config()
  {
    return _config;
  }

  @Nonnull
  protected ReentrantReadWriteLock getLock()
  {
    return _lock;
  }

  @Nonnull
  protected abstract ContextService getContextService();

  @Nonnull
  @Override
  protected CacheService getCacheService()
  {
    return _cacheService;
  }

  @Nonnull
  protected abstract ManagedScheduledExecutorService getManagedScheduledExecutorService();

  @Override
  @Nonnull
  protected ChangeSet parseChangeSet( @Nonnull final String rawJsonData )
  {
    return ChangeSetDTO.asChangeSet( rawJsonData );
  }

  @Override
  protected void doScheduleDataLoad()
  {
    _future = getManagedScheduledExecutorService().
      scheduleAtFixedRate( this::scheduleTick, 0, 1, TimeUnit.MILLISECONDS );
  }

  private void scheduleTick()
  {
    if ( !stepDataLoad() && null != _future )
    {
      final ScheduledFuture future = _future;
      _future = null;
      future.cancel( false );
    }
  }

  /**
   * Return the JNDI prefix used to derive optional configuration.
   * Typically it returns a string such as: "myapp/replicant/client"
   */
  @Nonnull
  protected abstract String getJndiPrefix();

  @Override
  protected void startPolling()
  {
    withLock( getLock().writeLock(), () -> super.startPolling() );
  }

  @Override
  protected void stopPolling()
  {
    withLock( getLock().writeLock(), () -> super.stopPolling() );
  }

  @Nonnull
  @Override
  public State getState()
  {
    return withLock( getLock().readLock(), super::getState );
  }

  @Override
  protected void resumeWebPoller()
  {
    withLock( getLock().writeLock(), () -> super.resumeWebPoller() );
  }

  @Override
  protected void pauseWebPoller()
  {
    withLock( getLock().writeLock(), () -> super.pauseWebPoller() );
  }

  @Nonnull
  @Override
  protected WebPoller getWebPoller()
  {
    return withLock( getLock().readLock(), () -> super.getWebPoller() );
  }

  @Override
  protected boolean stepDataLoad()
  {
    return withLock( getLock().writeLock(), () -> super.stepDataLoad() );
  }

  @Override
  protected void setSession( @Nullable final ClientSession session, @Nullable final Runnable postAction )
  {
    withLock( getLock().writeLock(), () -> super.setSession( session, postAction ) );
  }

  @Override
  public void scheduleDataLoad()
  {
    withLock( getLock().writeLock(), super::scheduleDataLoad );
  }

  private <R> R withLock( final Lock lock, final Supplier<R> action )
  {
    lock.lock();
    try
    {
      return action.get();
    }
    finally
    {
      lock.unlock();
    }
  }

  private void withLock( final Lock lock, final Runnable action )
  {
    lock.lock();
    try
    {
      action.run();
    }
    finally
    {
      lock.unlock();
    }
  }

  @Override
  public boolean addDataLoaderListener( @Nonnull final DataLoaderListener listener )
  {
    return super.addDataLoaderListener( wrap( listener ) );
  }

  @Override
  public boolean removeDataLoaderListener( @Nonnull final DataLoaderListener listener )
  {
    return super.removeDataLoaderListener( wrap( listener ) );
  }

  @Nonnull
  private DataLoaderListener wrap( @Nonnull final DataLoaderListener listener )
  {
    return getContextService().createContextualProxy( listener, DataLoaderListener.class );
  }
}
