package org.realityforge.replicant.server.transport;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SimpleSessionInfo
  implements SessionInfo, Serializable
{
  private final String _userID;
  private final String _sessionID;
  private long _createdAt;
  private long _lastAccessedAt;
  private Map<String, Serializable> _attributes = new HashMap<>();

  public SimpleSessionInfo( @Nullable final String userID, @Nonnull final String sessionID )
  {
    _userID = userID;
    _sessionID = sessionID;
    _createdAt = _lastAccessedAt = System.currentTimeMillis();
  }

  @Nonnull
  @Override
  public Set<String> getAttributeKeys()
  {
    return _attributes.keySet();
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
  @Nullable
  @Override
  public Serializable getAttribute( @Nonnull final String key )
  {
    return _attributes.get( key );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setAttribute( @Nonnull final String key, @Nonnull final Serializable value )
  {
    _attributes.put( key, value );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeAttribute( @Nonnull final String key )
  {
    _attributes.remove( key );
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
