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
  private static final String REPOSITORY_DEBUG = "imitRepositoryDebug";

  private final String _key;

  GwtDataLoaderServiceConfigImpl( @Nonnull final String key )
  {
    _key = key;

    if ( canRepositoryDebugOutputBeEnabled() )
    {
      final String message =
        _key + ".RepositoryDebugOutput module is enabled. Run the javascript " +
        "'window." + REPOSITORY_DEBUG + " = true' to enable debug output when change messages arrive. To limit " +
        "the debug output to just this data loader run the javascript '" +
        toSessionSpecificJavascript( REPOSITORY_DEBUG ) + "'";
      LOG.info( message );
    }

    if ( canSubscriptionsDebugOutputBeEnabled() )
    {
      final String message =
        _key + ".SubscriptionDebugOutput module is enabled. Run the javascript " +
        "'window." + SUBSCRIPTION_DEBUG + " = true' to enable debug output when change messages arrive. To limit " +
        "the debug output to just this data loader run the javascript '" +
        toSessionSpecificJavascript( SUBSCRIPTION_DEBUG ) + "'";
      LOG.info( message );
    }

    if ( canRequestDebugOutputBeEnabled() )
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
  public boolean shouldValidateRepositoryOnLoad()
  {
    return System.getProperty( "replicant.shouldValidateRepositoryOnLoad", "false" ).equals( "true" );
  }

  @Override
  public boolean requestDebugOutputEnabled()
  {
    return canRequestDebugOutputBeEnabled() && isEnabled( _key, REQUEST_DEBUG );
  }

  @Override
  public boolean subscriptionsDebugOutputEnabled()
  {
    return canSubscriptionsDebugOutputBeEnabled() && isEnabled( _key, SUBSCRIPTION_DEBUG );
  }

  @Override
  public boolean repositoryDebugOutputEnabled()
  {
    return canRepositoryDebugOutputBeEnabled() && isEnabled( _key, REPOSITORY_DEBUG );
  }

  @Nonnull
  private String toSessionSpecificJavascript( @Nonnull final String variable )
  {
    final String key = _key;
    return "( window." + key + " ? window." + key + " : window." + key + " = {} )." + variable + " = true";
  }

  private boolean canRepositoryDebugOutputBeEnabled()
  {
    return System.getProperty( "replicant.repositoryDebugOutputEnabled", "false" ).equals( "true" );
  }

  private boolean canRequestDebugOutputBeEnabled()
  {
    return System.getProperty( "replicant.requestDebugOutputEnabled", "false" ).equals( "true" );
  }

  private boolean canSubscriptionsDebugOutputBeEnabled()
  {
    return System.getProperty( "replicant.subscriptionsDebugOutputEnabled", "false" ).equals( "true" );
  }

  private static native boolean isEnabled( String sessionKey, String feature ) /*-{
    return $wnd[feature] == true || ($wnd[sessionKey] && $wnd[sessionKey][feature] == true);
  }-*/;
}
