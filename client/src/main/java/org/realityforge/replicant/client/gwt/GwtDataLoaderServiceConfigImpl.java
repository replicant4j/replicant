package org.realityforge.replicant.client.gwt;

import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.transport.DataLoaderServiceConfig;

public class GwtDataLoaderServiceConfigImpl
  implements DataLoaderServiceConfig
{
  private static final Logger LOG = Logger.getLogger( GwtDataLoaderServiceConfigImpl.class.getName() );

  private static final String REQUEST_DEBUG = "imitRequestDebug";
  private static final String SUBSCRIPTION_DEBUG = "imitSubscriptionDebug";

  private final String _key;

  GwtDataLoaderServiceConfigImpl( @Nonnull final String key )
  {
    _key = key;

    if ( ReplicantConfig.canSubscriptionsDebugOutputBeEnabled() )
    {
      final String message =
        _key + ".SubscriptionDebugOutput module is enabled. Run the javascript " +
        "'window." + SUBSCRIPTION_DEBUG + " = true' to enable debug output when change messages arrive. To limit " +
        "the debug output to just this data loader run the javascript '" +
        toSessionSpecificJavascript( SUBSCRIPTION_DEBUG ) + "'";
      LOG.info( message );
    }

    if ( ReplicantConfig.canRequestDebugOutputBeEnabled() )
    {
      final String message =
        _key + ".RequestDebugOutput module is enabled. Run the javascript " +
        "'window." + REQUEST_DEBUG + " = true' to enable debug output when change messages arrive. To limit " +
        "the debug output to just this data loader run the javascript '" +
        toSessionSpecificJavascript( REQUEST_DEBUG ) + "'";
      LOG.info( message );
    }
  }

  @Override
  public boolean shouldRecordRequestKey()
  {
    return ReplicantConfig.shouldRecordRequestKey();
  }

  @Override
  public boolean shouldValidateRepositoryOnLoad()
  {
    return ReplicantConfig.shouldValidateRepositoryOnLoad();
  }

  @Override
  public boolean requestDebugOutputEnabled()
  {
    return ReplicantConfig.canRequestDebugOutputBeEnabled() && isEnabled( _key, REQUEST_DEBUG );
  }

  @Override
  public boolean subscriptionsDebugOutputEnabled()
  {
    return ReplicantConfig.canSubscriptionsDebugOutputBeEnabled() && isEnabled( _key, SUBSCRIPTION_DEBUG );
  }

  @Nonnull
  private String toSessionSpecificJavascript( @Nonnull final String variable )
  {
    final String key = _key;
    return "( window." + key + " ? window." + key + " : window." + key + " = {} )." + variable + " = true";
  }

  private static native boolean isEnabled( String sessionKey, String feature ) /*-{
    return $wnd[feature] == true || ($wnd[sessionKey] && $wnd[sessionKey][feature] == true);
  }-*/;
}
