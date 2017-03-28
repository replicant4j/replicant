package org.realityforge.replicant.client.ee;

import java.lang.annotation.Annotation;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.naming.InitialContext;
import org.realityforge.gwt.webpoller.client.WebPoller;
import org.realityforge.replicant.client.ChangeSet;
import org.realityforge.replicant.client.transport.CacheService;
import org.realityforge.replicant.client.transport.ClientSession;
import org.realityforge.replicant.client.transport.DataLoadStatus;
import org.realityforge.replicant.client.transport.DataLoaderService;
import org.realityforge.replicant.client.transport.WebPollerDataLoaderService;

public abstract class EeDataLoaderService<T extends ClientSession<T, G>, G extends Enum<G>>
  extends WebPollerDataLoaderService<T, G>
{
  private static final int DEFAULT_EE_CHANGES_TO_PROCESS_PER_TICK = 10000;
  private static final int DEFAULT_EE_LINKS_TO_PROCESS_PER_TICK = 10000;

  @Inject
  private BeanManager _beanManager;
  @Nullable
  private ScheduledFuture _future;
  private final InMemoryCacheService _cacheService = new InMemoryCacheService();
  private final ReentrantReadWriteLock _lock = new ReentrantReadWriteLock( true );

  protected EeDataLoaderService()
  {
    setChangesToProcessPerTick( DEFAULT_EE_CHANGES_TO_PROCESS_PER_TICK );
    setLinksToProcessPerTick( DEFAULT_EE_LINKS_TO_PROCESS_PER_TICK );
  }

  @Nonnull
  protected ReentrantReadWriteLock getLock()
  {
    return _lock;
  }

  @Nonnull
  @Override
  protected CacheService getCacheService()
  {
    return _cacheService;
  }

  @Override
  protected boolean shouldValidateOnLoad()
  {
    return isFlagTrue( "shouldValidateRepositoryOnLoad" );
  }

  @Override
  protected boolean requestDebugOutputEnabled()
  {
    return isFlagTrue( "requestDebugOutputEnabled" );
  }

  @Override
  protected boolean subscriptionsDebugOutputEnabled()
  {
    return isFlagTrue( "subscriptionsDebugOutputEnabled" );
  }

  @Override
  protected boolean repositoryDebugOutputEnabled()
  {
    return isFlagTrue( "repositoryDebugOutputEnabled" );
  }

  private boolean isFlagTrue( @Nonnull final String flag )
  {
    try
    {
      return (Boolean) new InitialContext().lookup( getJndiPrefix() + "/" + flag );
    }
    catch ( final Exception e )
    {
      return false;
    }
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

  /**
   * Invoked to fire an event when data load has completed.
   */
  @Override
  protected void fireDataLoadCompleteEvent( @Nonnull final DataLoadStatus status )
  {
    fireEvent( new DataLoadCompleteEvent( status ) );
  }

  protected void handleSystemFailure( @Nonnull final Throwable caught, @Nonnull final String message )
  {
    super.handleSystemFailure( caught, message );
    fireEvent( new SystemErrorEvent( getSessionContext().getKey(), message, caught ) );
  }

  @Override
  protected void fireConnectEvent()
  {
    fireEvent( new ConnectEvent( getSessionContext().getKey() ) );
  }

  @Override
  protected void fireInvalidConnectEvent( @Nonnull final Throwable exception )
  {
    fireEvent( new InvalidConnectEvent( getSessionContext().getKey(), exception ) );
  }

  @Override
  protected void fireDisconnectEvent()
  {
    fireEvent( new DisconnectEvent( getSessionContext().getKey() ) );
  }

  @Override
  protected void fireInvalidDisconnectEvent( @Nonnull final Throwable exception )
  {
    fireEvent( new InvalidDisconnectEvent( getSessionContext().getKey(), exception ) );
  }

  @Override
  protected void firePollFailure( @Nonnull final Throwable exception )
  {
    fireEvent( new PollErrorEvent( getSessionContext().getKey(), exception ) );
  }

  @Override
  protected void fireDataLoadFailure( @Nonnull final Exception e )
  {
    fireEvent( new DataLoadFailureEvent( getSessionContext().getKey(), e ) );
  }

  protected void fireEvent( @Nonnull final Object event )
  {
    _beanManager.fireEvent( event, getEventQualifiers() );
  }

  @Nonnull
  protected Annotation[] getEventQualifiers()
  {
    return new Annotation[ 0 ];
  }

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
  protected void setSession( @Nullable final T session, @Nullable final Runnable postAction )
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
}
