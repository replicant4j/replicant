package org.realityforge.replicant.client.transport;

import javax.annotation.Nullable;

public final class SessionContext
{
  private ClientSession _session;
  private RequestEntry _request;

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
