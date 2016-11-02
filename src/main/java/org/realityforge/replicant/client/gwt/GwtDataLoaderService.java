package org.realityforge.replicant.client.gwt;

import com.google.gwt.core.client.Scheduler;
import com.google.web.bindery.event.shared.EventBus;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.ChangeSet;
import org.realityforge.replicant.client.EntityChangeBroker;
import org.realityforge.replicant.client.EntityRepository;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.client.transport.CacheService;
import org.realityforge.replicant.client.transport.ClientSession;
import org.realityforge.replicant.client.transport.DataLoadStatus;
import org.realityforge.replicant.client.transport.SessionContext;
import org.realityforge.replicant.client.transport.WebPollerDataLoaderService;

public abstract class GwtDataLoaderService<T extends ClientSession<T, G>, G extends Enum>
  extends WebPollerDataLoaderService<T, G>
{
  private static final String REQUEST_DEBUG = "imitRequestDebug";
  private static final String SUBSCRIPTION_DEBUG = "imitSubscriptionDebug";
  private static final String REPOSITORY_DEBUG = "imitRepositoryDebug";
  private final ReplicantConfig _replicantConfig;
  private final EventBus _eventBus;

  protected GwtDataLoaderService( @Nonnull final SessionContext sessionContext,
                                  @Nonnull final EntityChangeBroker changeBroker,
                                  @Nonnull final EntityRepository repository,
                                  @Nonnull final CacheService cacheService,
                                  @Nonnull final EntitySubscriptionManager subscriptionManager,
                                  @Nonnull final EventBus eventBus,
                                  @Nonnull final ReplicantConfig replicantConfig )
  {
    super( sessionContext, changeBroker, repository, cacheService, subscriptionManager );
    setupWebPoller();
    _eventBus = eventBus;
    _replicantConfig = replicantConfig;

    if ( _replicantConfig.repositoryDebugOutputEnabled() )
    {
      final String message =
        getSessionContext().getKey() + ".RepositoryDebugOutput module is enabled. Run the javascript " +
        "'window." + REPOSITORY_DEBUG + " = true' to enable debug output when change messages arrive. To limit " +
        "the debug output to just this data loader run the javascript '" +
        toSessionSpecificJavascript( REPOSITORY_DEBUG ) + "'";
      LOG.info( message );
    }

    if ( _replicantConfig.subscriptionsDebugOutputEnabled() )
    {
      final String message =
        getSessionContext().getKey() + ".SubscriptionDebugOutput module is enabled. Run the javascript " +
        "'window." + SUBSCRIPTION_DEBUG + " = true' to enable debug output when change messages arrive. To limit " +
        "the debug output to just this data loader run the javascript '" +
        toSessionSpecificJavascript( SUBSCRIPTION_DEBUG ) + "'";
      LOG.info( message );
    }

    if ( _replicantConfig.requestDebugOutputEnabled() )
    {
      final String message =
        getSessionContext().getKey() + ".RequestDebugOutput module is enabled. Run the javascript " +
        "'window." + REQUEST_DEBUG + " = true' to enable debug output when change messages arrive. To limit " +
        "the debug output to just this data loader run the javascript '" +
        toSessionSpecificJavascript( REQUEST_DEBUG ) + "'";
      LOG.info( message );
    }
  }

  private String toSessionSpecificJavascript( final String variable )
  {
    final String key = getSessionContext().getKey();
    return "( window." + key + " ? window." + key + " : window." + key + " = {} )." + variable + " = true";
  }

  @Override
  protected boolean shouldValidateOnLoad()
  {
    return _replicantConfig.shouldValidateRepositoryOnLoad();
  }

  //Static class to help check whether debug is enabled at the current time
  private static class RepositoryDebugEnabledChecker
  {
    public static native boolean isEnabled( String sessionKey, String feature ) /*-{
      return $wnd[feature] == true || ($wnd[sessionKey] && $wnd[sessionKey][feature] == true);
    }-*/;
  }

  @Override
  protected boolean requestDebugOutputEnabled()
  {
    return _replicantConfig.requestDebugOutputEnabled() &&
           RepositoryDebugEnabledChecker.isEnabled( getSessionContext().getKey(), REQUEST_DEBUG );
  }

  @Override
  protected boolean subscriptionsDebugOutputEnabled()
  {
    return _replicantConfig.subscriptionsDebugOutputEnabled() &&
           RepositoryDebugEnabledChecker.isEnabled( getSessionContext().getKey(), SUBSCRIPTION_DEBUG );
  }

  @Override
  protected boolean repositoryDebugOutputEnabled()
  {
    return _replicantConfig.repositoryDebugOutputEnabled() &&
           RepositoryDebugEnabledChecker.isEnabled( getSessionContext().getKey(), REPOSITORY_DEBUG );
  }

  @Nonnull
  @Override
  protected ChangeSet parseChangeSet( @Nonnull final String rawJsonData )
  {
    return JsoChangeSet.asChangeSet( rawJsonData );
  }

  protected void doScheduleDataLoad()
  {
    Scheduler.get().scheduleIncremental( new Scheduler.RepeatingCommand()
    {
      @Override
      public boolean execute()
      {
        return stepDataLoad();
      }
    } );
  }

  /**
   * Invoked to fire an event when data load has completed.
   */
  @Override
  protected void fireDataLoadCompleteEvent( @Nonnull final DataLoadStatus status )
  {
    getEventBus().fireEvent( new DataLoadCompleteEvent( status ) );
  }

  /**
   * Return the event bus associated with the service.
   */
  @Nonnull
  protected final EventBus getEventBus()
  {
    return _eventBus;
  }
}
