package org.realityforge.replicant.server.transport;

import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A simple interface for managing session data.
 */
public interface SessionManager<T extends SessionInfo>
{
  /**
   * Return the key used to access session information.
   * Typically this is used as part of the cookie or header name in the dependent system.
   *
   * @return the key used to access session information.
   */
  @Nonnull
  String getSessionKey();

  /**
   * Return the session for specified ID.
   * Session ID's are effectively opaque.
   *
   * @param sessionID the session id.
   * @return the associated session or null if no such session.
   */
  @Nullable
  T getSession( @Nonnull String sessionID );

  /**
   * Return the st of valid session ids.
   *
   * @return the set of valid session ids.
   */
  @Nonnull
  Set<String> getSessionIDs();

  /**
   * Invalidate session with specified session ID.
   * Ignore if no session with specified id.
   *
   * @param sessionID the session id.
   * @return true if a session was invalidated, false otherwise.
   */
  boolean invalidateSession( @Nonnull String sessionID );

  /**
   * Create session for specified username.
   * It is assumed the username has already been authenticated and this is just tracking the session.
   *
   * @return the new session.
   */
  @Nonnull
  T createSession();
}
