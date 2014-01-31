package org.realityforge.replicant.client.transport;

import javax.annotation.Nullable;

public final class SessionContext
{
  private SessionContext()
  {
  }

  private static ClientSession c_session;
  private static RequestEntry c_request;

  @Nullable
  public static ClientSession getSession()
  {
    return c_session;
  }

  public static void setSession( @Nullable final ClientSession session )
  {
    c_session = session;
  }

  @Nullable
  public static RequestEntry getRequest()
  {
    return c_request;
  }

  public static void setRequest( @Nullable final RequestEntry request )
  {
    c_request = request;
  }
}
