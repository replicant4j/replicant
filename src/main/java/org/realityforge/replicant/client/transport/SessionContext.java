package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SessionContext
{
  @Nonnull
  private final String _key;
  private String _baseURL;
  private ClientSession _session;
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
