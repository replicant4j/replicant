package org.realityforge.replicant.server.transport;

import java.io.Serializable;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A basic interface defining a session.
 */
public interface SessionInfo
{
  /**
   * @return an opaque ID representing user that created session.
   */
  @Nullable
  String getUserID();

  /**
   * @return an opaque ID representing session.
   */
  @Nonnull
  String getSessionID();

  /**
   * @return the attribute keys for session.
   */
  @Nonnull
  Set<String> getAttributeKeys();

  /**
   * @param key the attribute key to return.
   * @return the attribute for specified key.
   */
  @Nullable
  Serializable getAttribute( @Nonnull String key );

  /**
   * @param key the attribute key to set.
   * @param value the value to set attribute to.
   */
  void setAttribute( @Nonnull String key, @Nonnull Serializable value );

  /**
   * @param key the attribute key to remove.
   */
  void removeAttribute( @Nonnull String key );

  /**
   * @return the time at which session was created.
   */
  long getCreatedAt();

  /**
   * @return the time at which session was last accessed.
   */
  long getLastAccessedAt();

  /**
   * Update the access time to now.
   */
  void updateAccessTime();
}
