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
import org.realityforge.replicant.client.EntityChangeBrokerImpl;
import org.realityforge.replicant.client.EntityRepositoryImpl;
import org.realityforge.replicant.client.EntitySubscriptionManagerImpl;
import org.realityforge.replicant.client.transport.ClientSession;
import org.realityforge.replicant.client.transport.DataLoadStatus;
import org.realityforge.replicant.client.transport.SessionContext;
import org.realityforge.replicant.client.transport.WebPollerDataLoaderService;

public abstract class EeDataLoaderService<T extends ClientSession<T, G>, G extends Enum>
  extends WebPollerDataLoaderService<T, G>
{
  private static final int DEFAULT_EE_CHANGES_TO_PROCESS_PER_TICK = 10000;
  private static final int DEFAULT_EE_LINKS_TO_PROCESS_PER_TICK = 10000;

  @Inject
  private Event<DataLoadCompleteEvent> _dataLoadCompleteEvent;

  @Nullable
  private ScheduledFuture _future;

  protected EeDataLoaderService( @Nonnull final SessionContext sessionContext )
  {
    super( sessionContext,
           new EntityChangeBrokerImpl(),
           new EntityRepositoryImpl(),
           new InMemoryCacheService(),
           new EntitySubscriptionManagerImpl() );
    setChangesToProcessPerTick( DEFAULT_EE_CHANGES_TO_PROCESS_PER_TICK );
    setLinksToProcessPerTick( DEFAULT_EE_LINKS_TO_PROCESS_PER_TICK );
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
    _future = getManagedScheduledExecutorService().scheduleAtFixedRate( new Runnable()
    {
      @Override
      public void run()
      {
        if ( !stepDataLoad() && null != _future )
        {
          final ScheduledFuture future = _future;
          _future = null;
          future.cancel( false );
        }
      }
    }, 0, 1, TimeUnit.MILLISECONDS );
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
}
