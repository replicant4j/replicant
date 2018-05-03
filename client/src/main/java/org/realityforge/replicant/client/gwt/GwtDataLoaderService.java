package org.realityforge.replicant.client.gwt;

import com.google.gwt.core.client.Scheduler;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.transport.CacheService;
import org.realityforge.replicant.client.transport.ChangeSet;
import org.realityforge.replicant.client.transport.SessionContext;
import org.realityforge.replicant.client.transport.WebPollerDataLoaderService;
import replicant.Replicant;

public abstract class GwtDataLoaderService
  extends WebPollerDataLoaderService
{
  private static final String REQUEST_DEBUG = "imitRequestDebug";
  private static final String SUBSCRIPTION_DEBUG = "imitSubscriptionDebug";

  private final SessionContext _sessionContext;

  protected GwtDataLoaderService( @Nonnull final CacheService cacheService,
                                  @Nonnull final SessionContext sessionContext )
  {
    super( cacheService );
    _sessionContext = sessionContext;
    if ( Replicant.canSubscriptionsDebugOutputBeEnabled() )
    {
      final String message =
        getKey() + ".SubscriptionDebugOutput module is enabled. Run the javascript " +
        "'window." + SUBSCRIPTION_DEBUG + " = true' to enable debug output when change messages arrive. To limit " +
        "the debug output to just this data loader run the javascript '" +
        toSessionSpecificJavascript( SUBSCRIPTION_DEBUG ) + "'";
      LOG.info( message );
    }

    if ( Replicant.canRequestsDebugOutputBeEnabled() )
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

  @Override
  protected final boolean requestDebugOutputEnabled()
  {
    return Replicant.canRequestsDebugOutputBeEnabled() && isEnabled( getKey(), REQUEST_DEBUG );
  }

  @Override
  protected final boolean subscriptionsDebugOutputEnabled()
  {
    return Replicant.canSubscriptionsDebugOutputBeEnabled() && isEnabled( getKey(), SUBSCRIPTION_DEBUG );
  }

  @Nonnull
  private String toSessionSpecificJavascript( @Nonnull final String variable )
  {
    final String key = getKey();
    return "( window." + key + " ? window." + key + " : window." + key + " = {} )." + variable + " = true";
  }

  private static native boolean isEnabled( String sessionKey, String feature ) /*-{
    return $wnd[feature] == true || ($wnd[sessionKey] && $wnd[sessionKey][feature] == true);
  }-*/;
}
