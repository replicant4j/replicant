package org.realityforge.replicant.client.gwt;

import com.google.gwt.core.client.Scheduler;
import elemental2.core.Global;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.base.Js;
import org.realityforge.replicant.client.transport.CacheService;
import replicant.ChangeSet;
import org.realityforge.replicant.client.transport.SessionContext;
import org.realityforge.replicant.client.transport.WebPollerDataLoaderService;
import replicant.Replicant;
import replicant.ReplicantContext;

public abstract class GwtDataLoaderService
  extends WebPollerDataLoaderService
{
  private static final String REQUEST_DEBUG = "imitRequestDebug";

  private final SessionContext _sessionContext;

  protected GwtDataLoaderService( @Nullable final ReplicantContext context,
                                  @Nonnull final Class<?> systemType,
                                  @Nonnull final CacheService cacheService,
                                  @Nonnull final SessionContext sessionContext )
  {
    super( context, systemType, cacheService );
    _sessionContext = sessionContext;

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
    return Js.cast( Global.JSON.parse( rawJsonData ));
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
