package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.Connection;
import replicant.RequestEntry;

public final class SessionContext
{
  @Nullable
  private String _authenticationToken;
  private String _baseURL;
  @Nullable
  private Connection _connection;
  @Nullable
  private RequestEntry _request;

  /**
   * Return the authentication token.
   * Typically passed as bearer authorization token when using http based transport.
   */
  @Nullable
  public String getAuthenticationToken()
  {
    return _authenticationToken;
  }

  /**
   * Set the authentication token.
   */
  public void setAuthenticationToken( @Nullable final String authenticationToken )
  {
    _authenticationToken = authenticationToken;
  }

  @Nonnull
  public String getBaseURL()
  {
    return _baseURL;
  }

  public void setBaseURL( @Nonnull final String baseURL )
  {
    _baseURL = baseURL;
  }

  @Nullable
  public Connection getConnection()
  {
    return _connection;
  }

  public void setConnection( @Nullable final Connection connection )
  {
    _connection = connection;
  }

  @Nullable
  public RequestEntry getRequest()
  {
    return _request;
  }

  public void request( @Nullable final String key,
                       @Nullable final String cacheKey,
                       @Nonnull final RequestAction action )
  {
    if ( null != _connection )
    {
      _request = _connection.newRequest( key, cacheKey );
    }
    action.invokeRequest( _connection, _request );
    _request = null;
  }
}
