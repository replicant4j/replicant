package org.realityforge.replicant.client.ee;

import javax.annotation.Nonnull;
import javax.naming.InitialContext;
import org.realityforge.replicant.client.transport.DataLoaderServiceConfig;

public class EeDataLoaderServiceConfigImpl
  implements DataLoaderServiceConfig
{
  private final String _jndiPrefix;

  public EeDataLoaderServiceConfigImpl( @Nonnull final String jndiPrefix )
  {
    _jndiPrefix = jndiPrefix;
  }

  @Override
  public boolean shouldValidateRepositoryOnLoad()
  {
    return isFlagTrue( "shouldValidateRepositoryOnLoad" );
  }

  @Override
  public boolean requestDebugOutputEnabled()
  {
    return isFlagTrue( "requestDebugOutputEnabled" );
  }

  @Override
  public boolean subscriptionsDebugOutputEnabled()
  {
    return isFlagTrue( "subscriptionsDebugOutputEnabled" );
  }

  @Override
  public boolean repositoryDebugOutputEnabled()
  {
    return isFlagTrue( "repositoryDebugOutputEnabled" );
  }

  private boolean isFlagTrue( @Nonnull final String flag )
  {
    try
    {
      return (Boolean) new InitialContext().lookup( _jndiPrefix + "/" + flag );
    }
    catch ( final Exception e )
    {
      return false;
    }
  }
}
