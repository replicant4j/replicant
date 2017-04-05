package org.realityforge.replicant.client.gwt;

import com.google.gwt.core.client.Scheduler;
import com.google.web.bindery.event.shared.EventBus;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.ChangeSet;
import org.realityforge.replicant.client.transport.ClientSession;
import org.realityforge.replicant.client.transport.SessionContext;
import org.realityforge.replicant.client.transport.WebPollerDataLoaderService;

public abstract class GwtDataLoaderService
  extends WebPollerDataLoaderService
{
  private static final String REQUEST_DEBUG = "imitRequestDebug";
  private static final String SUBSCRIPTION_DEBUG = "imitSubscriptionDebug";
  private static final String REPOSITORY_DEBUG = "imitRepositoryDebug";
  private final ReplicantConfig _replicantConfig;
  private final SessionContext _sessionContext;

  protected GwtDataLoaderService( @Nonnull final SessionContext sessionContext,
                                  @Nonnull final EventBus eventBus,
                                  @Nonnull final ReplicantConfig replicantConfig )
  {
    setListener( new GwtDataLoaderListener( eventBus ) );
    createWebPoller();
    _sessionContext = sessionContext;
    _replicantConfig = replicantConfig;

    if ( _replicantConfig.repositoryDebugOutputEnabled() )
    {
      final String message =
        getKey() + ".RepositoryDebugOutput module is enabled. Run the javascript " +
        "'window." + REPOSITORY_DEBUG + " = true' to enable debug output when change messages arrive. To limit " +
        "the debug output to just this data loader run the javascript '" +
        toSessionSpecificJavascript( REPOSITORY_DEBUG ) + "'";
      LOG.info( message );
    }

    if ( _replicantConfig.subscriptionsDebugOutputEnabled() )
    {
      final String message =
        getKey() + ".SubscriptionDebugOutput module is enabled. Run the javascript " +
        "'window." + SUBSCRIPTION_DEBUG + " = true' to enable debug output when change messages arrive. To limit " +
        "the debug output to just this data loader run the javascript '" +
        toSessionSpecificJavascript( SUBSCRIPTION_DEBUG ) + "'";
      LOG.info( message );
    }

    if ( _replicantConfig.requestDebugOutputEnabled() )
    {
      final String message =
        getKey() + ".RequestDebugOutput module is enabled. Run the javascript " +
        "'window." + REQUEST_DEBUG + " = true' to enable debug output when change messages arrive. To limit " +
        "the debug output to just this data loader run the javascript '" +
        toSessionSpecificJavascript( REQUEST_DEBUG ) + "'";
      LOG.info( message );
    }
  }

  @Nonnull
  @Override
  protected SessionContext getSessionContext()
  {
    return _sessionContext;
  }

  private String toSessionSpecificJavascript( final String variable )
  {
    final String key = getKey();
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
           RepositoryDebugEnabledChecker.isEnabled( getKey(), REQUEST_DEBUG );
  }

  @Override
  protected boolean subscriptionsDebugOutputEnabled()
  {
    return _replicantConfig.subscriptionsDebugOutputEnabled() &&
           RepositoryDebugEnabledChecker.isEnabled( getKey(), SUBSCRIPTION_DEBUG );
  }

  @Override
  protected boolean repositoryDebugOutputEnabled()
  {
    return _replicantConfig.repositoryDebugOutputEnabled() &&
           RepositoryDebugEnabledChecker.isEnabled( getKey(), REPOSITORY_DEBUG );
  }

  @Nonnull
  @Override
  protected ChangeSet parseChangeSet( @Nonnull final String rawJsonData )
  {
    return JsoChangeSet.asChangeSet( rawJsonData );
  }

  protected void doScheduleDataLoad()
  {
    Scheduler.get().scheduleIncremental( this::stepDataLoad );
  }
}
