package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SessionContext
{
  @Nonnull
  private final String _key;
  @Nullable
  private String _authenticationToken;
  private String _baseURL;
  @Nullable
  private ClientSession _session;
  @Nullable
  private RequestEntry _request;

  public SessionContext( @Nonnull final String key )
  {
    _key = key;
  }

  /**
   * Symbolic key identifying session. Usually the name of the
   * replicant session such as "Planner" or "Tyrell".
   */
  @Nonnull
  public String getKey()
  {
    return _key;
  }

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
  public ClientSession getSession()
  {
    return _session;
  }

  public void setSession( @Nullable final ClientSession session )
  {
    _session = session;
  }

  @Nullable
  public RequestEntry getRequest()
  {
    return _request;
  }

  public void setRequest( @Nullable final RequestEntry request )
  {
    _request = request;
  }
}
