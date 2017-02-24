package org.realityforge.replicant.client.ee;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.naming.InitialContext;
import org.realityforge.replicant.client.ChangeSet;
import org.realityforge.replicant.client.transport.CacheService;
import org.realityforge.replicant.client.transport.ClientSession;
import org.realityforge.replicant.client.transport.DataLoadStatus;
import org.realityforge.replicant.client.transport.WebPollerDataLoaderService;

public abstract class EeDataLoaderService<T extends ClientSession<T, G>, G extends Enum>
  extends WebPollerDataLoaderService<T, G>
{
  private static final int DEFAULT_EE_CHANGES_TO_PROCESS_PER_TICK = 10000;
  private static final int DEFAULT_EE_LINKS_TO_PROCESS_PER_TICK = 10000;

  @Inject
  private Event<DataLoadCompleteEvent> _dataLoadCompleteEvent;
  @Inject
  private Event<SystemErrorEvent> _systemErrorEvent;
  @Inject
  private Event<ConnectEvent> _connectEvent;
  @Inject
  private Event<InvalidConnectEvent> _invalidConnectEvent;
  @Inject
  private Event<DisconnectEvent> _disconnectEvent;
  @Inject
  private Event<InvalidDisconnectEvent> _invalidDisconnectEvent;

  @Nullable
  private ScheduledFuture _future;
  private final InMemoryCacheService _cacheService = new InMemoryCacheService();

  protected EeDataLoaderService()
  {
    setChangesToProcessPerTick( DEFAULT_EE_CHANGES_TO_PROCESS_PER_TICK );
    setLinksToProcessPerTick( DEFAULT_EE_LINKS_TO_PROCESS_PER_TICK );
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
    _dataLoadCompleteEvent.fire( new DataLoadCompleteEvent( status ) );
  }

  protected void handleSystemFailure( @Nullable final Throwable caught, @Nonnull final String message )
  {
    super.handleSystemFailure( caught, message );
    _systemErrorEvent.fire( new SystemErrorEvent( getSystemKey(), message, caught ) );
  }

  @Override
  protected void fireConnectEvent()
  {
    _connectEvent.fire( new ConnectEvent( getSystemKey() ) );
  }

  @Override
  protected void fireInvalidConnectEvent( @Nonnull final Throwable exception )
  {
    _invalidConnectEvent.fire( new InvalidConnectEvent( getSystemKey(), exception ) );
  }

  @Override
  protected void fireDisconnectEvent()
  {
    _disconnectEvent.fire( new DisconnectEvent( getSystemKey() ) );
  }

  @Override
  protected void fireInvalidDisconnectEvent( @Nonnull final Throwable exception )
  {
    _invalidDisconnectEvent.fire( new InvalidDisconnectEvent( getSystemKey(), exception ) );
  }
}
