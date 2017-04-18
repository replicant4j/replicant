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
  private final ReplicantConfig _replicantConfig;

  protected GwtDataLoaderServiceConfigImpl( @Nonnull final String key,
                                            @Nonnull final ReplicantConfig replicantConfig )
  {
    _key = key;
    _replicantConfig = replicantConfig;

    if ( _replicantConfig.repositoryDebugOutputEnabled() )
    {
      final String message =
        _key + ".RepositoryDebugOutput module is enabled. Run the javascript " +
        "'window." + REPOSITORY_DEBUG + " = true' to enable debug output when change messages arrive. To limit " +
        "the debug output to just this data loader run the javascript '" +
        toSessionSpecificJavascript( REPOSITORY_DEBUG ) + "'";
      LOG.info( message );
    }

    if ( _replicantConfig.subscriptionsDebugOutputEnabled() )
    {
      final String message =
        _key + ".SubscriptionDebugOutput module is enabled. Run the javascript " +
        "'window." + SUBSCRIPTION_DEBUG + " = true' to enable debug output when change messages arrive. To limit " +
        "the debug output to just this data loader run the javascript '" +
        toSessionSpecificJavascript( SUBSCRIPTION_DEBUG ) + "'";
      LOG.info( message );
    }

    if ( _replicantConfig.requestDebugOutputEnabled() )
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
    return _replicantConfig.shouldValidateRepositoryOnLoad();
  }

  @Override
  public boolean requestDebugOutputEnabled()
  {
    return _replicantConfig.requestDebugOutputEnabled() &&
           RepositoryDebugEnabledChecker.isEnabled( _key, REQUEST_DEBUG );
  }

  @Override
  public boolean subscriptionsDebugOutputEnabled()
  {
    return _replicantConfig.subscriptionsDebugOutputEnabled() &&
           RepositoryDebugEnabledChecker.isEnabled( _key, SUBSCRIPTION_DEBUG );
  }

  @Override
  public boolean repositoryDebugOutputEnabled()
  {
    return _replicantConfig.repositoryDebugOutputEnabled() &&
           RepositoryDebugEnabledChecker.isEnabled( _key, REPOSITORY_DEBUG );
  }

  private String toSessionSpecificJavascript( final String variable )
  {
    final String key = _key;
    return "( window." + key + " ? window." + key + " : window." + key + " = {} )." + variable + " = true";
  }

  //Static class to help check whether debug is enabled at the current time
  private static class RepositoryDebugEnabledChecker
  {
    public static native boolean isEnabled( String sessionKey, String feature ) /*-{
      return $wnd[feature] == true || ($wnd[sessionKey] && $wnd[sessionKey][feature] == true);
    }-*/;
  }
}
