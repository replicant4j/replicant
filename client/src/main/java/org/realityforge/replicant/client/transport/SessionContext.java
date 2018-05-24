package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.Connection;
import replicant.Connector;
import replicant.RequestEntry;

public final class SessionContext
{
  @Nullable
  private String _authenticationToken;
  private String _baseURL;
  @Nullable
  private Connector _connector;
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
    return null == _connector ? null : _connector.getConnection();
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
    final Connection connection = getConnection();
    if ( null != connection )
    {
      _request = connection.newRequest( key, cacheKey );
    }
    action.invokeRequest( connection, _request );
    _request = null;
  }

  public void setConnector( @Nonnull final Connector connector )
  {
    _connector = connector;
  }
}
