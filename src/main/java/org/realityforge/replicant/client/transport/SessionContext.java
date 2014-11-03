package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SessionContext
{
  private String _baseURL;
  private ClientSession _session;
  private RequestEntry _request;

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
