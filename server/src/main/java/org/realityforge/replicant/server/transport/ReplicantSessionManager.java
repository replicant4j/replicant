package org.realityforge.replicant.server.transport;

import java.util.Collection;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.websocket.Session;
import org.realityforge.replicant.server.ChannelAddress;

public interface ReplicantSessionManager
{
  /**
   * Status returned when attempting to subscribe.
   */
  enum CacheStatus
  {
    /**
     * The client supplied cacheKey is still valid and cached data should be reused.
     */
    USE,
    /**
     * The client did not supply cacheKey or it is out of date. Client cache should be refreshed from supplied data.
     */
    REFRESH,
    /**
     * The client did not supply cacheKey or it is out of date and the response is not cacheable. This may occur
     * if multiple subscriptions occur in a single subscribe call or attempting to subscribe to channels that are
     * already on the client.
     *
     * One day this may not be needed if the client can generate the cache from the in-memory representation rather
     * than the representation as it passes over the network.
     * TODO: Fix this.
     */
    IGNORE
  }

  /**
   * Return the session for specified ID.
   * Session ID's are effectively opaque.
   *
   * @param sessionId the session id.
   * @return the associated session or null if no such session.
   */
  @Nullable
  ReplicantSession getSession( @Nonnull String sessionId );

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
   * @param sessionId the session id.
   * @return true if a session was invalidated, false otherwise.
   */
  boolean invalidateSession( @Nonnull String sessionId );

  /**
   * Create replicant session.
   * It is assumed the username has already been authenticated and this is just tracking the session.
   *
   * @return the new session.
   */
  @Nonnull
  default ReplicantSession createSession()
  {
    return createSession( null );
  }

  /**
   * Create replicant session for specified WebSocket session.
   * It is assumed the username has already been authenticated and this is just tracking the session.
   *
   * @return the new session.
   */
  @Nonnull
  ReplicantSession createSession( @Nullable Session webSocketSession );

  /**
   * @return the metadata for replicant system.
   */
  @Nonnull
  SystemMetaData getSystemMetaData();

  @Nonnull
  CacheStatus subscribe( @Nonnull ReplicantSession session,
                         @Nonnull ChannelAddress address,
                         @Nullable Object filter );

  void bulkSubscribe( @Nonnull ReplicantSession session,
                      int channelId,
                      @Nonnull Collection<Integer> subChannelIds,
                      @Nullable Object filter );

  void unsubscribe( @Nonnull ReplicantSession session, @Nonnull ChannelAddress address );

  void bulkUnsubscribe( @Nonnull ReplicantSession session, int channelId, @Nonnull Collection<Integer> subChannelIds );
}
