package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;

/**
 * Abstract representation of client session.
 * Simply tracks the session identifier and job sequencing.
 */
public abstract class ClientSession
{
  private final String _sessionID;
  private final RequestManager _requestManager;

  private int _lastRxSequence;

  public ClientSession( @Nonnull final String sessionID )
  {
    _sessionID = sessionID;
    _requestManager = newRequestManager();
  }

  @Nonnull
  public String getSessionID()
  {
    return _sessionID;
  }

  @Nonnull
  public RequestManager getRequestManager()
  {
    return _requestManager;
  }

  protected RequestManager newRequestManager()
  {
    return new RequestManager();
  }

  public int getLastRxSequence()
  {
    return _lastRxSequence;
  }

  public void setLastRxSequence( final int lastRxSequence )
  {
    _lastRxSequence = lastRxSequence;
  }
}
