package org.realityforge.replicant.server.transport;

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
