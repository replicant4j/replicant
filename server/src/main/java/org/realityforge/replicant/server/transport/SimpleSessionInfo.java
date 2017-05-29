package org.realityforge.replicant.server.transport;

import java.io.Serializable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SimpleSessionInfo
  implements SessionInfo, Serializable
{
  private final String _userID;
  private final String _sessionID;
  private long _createdAt;
  private long _lastAccessedAt;

  public SimpleSessionInfo( @Nullable final String userID, @Nonnull final String sessionID )
  {
    _userID = userID;
    _sessionID = sessionID;
    _createdAt = _lastAccessedAt = System.currentTimeMillis();
  }

  /**
   * {@inheritDoc}
   */
  @Nullable
  @Override
  public String getUserID()
  {
    return _userID;
  }

  /**
   * {@inheritDoc}
   */
  @Nonnull
  @Override
  public String getSessionID()
  {
    return _sessionID;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getCreatedAt()
  {
    return _createdAt;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getLastAccessedAt()
  {
    return _lastAccessedAt;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateAccessTime()
  {
    _lastAccessedAt = System.currentTimeMillis();
  }
}
