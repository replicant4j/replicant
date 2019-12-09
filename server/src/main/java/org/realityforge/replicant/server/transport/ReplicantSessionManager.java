package org.realityforge.replicant.server.transport;

import java.util.Collection;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.server.ChannelAddress;

public interface ReplicantSessionManager
{
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
   * Invalidate specified session.
   *
   * @param session the session.
   * @return true if a session was invalidated, false otherwise.
   */
  boolean invalidateSession( @Nonnull ReplicantSession session );

  /**
   * Create session for specified username.
   * It is assumed the username has already been authenticated and this is just tracking the session.
   *
   * @return the new session.
   */
  @Nonnull
  ReplicantSession createSession();

  /**
   * @return the metadata for replicant system.
   */
  @Nonnull
  SystemMetaData getSystemMetaData();

  void subscribe( @Nonnull ReplicantSession session, @Nonnull ChannelAddress address, @Nullable Object filter );

  void bulkSubscribe( @Nonnull ReplicantSession session,
                      int channelId,
                      @Nonnull Collection<Integer> subChannelIds,
                      @Nullable Object filter );

  void unsubscribe( @Nonnull ReplicantSession session, @Nonnull ChannelAddress address );

  void bulkUnsubscribe( @Nonnull ReplicantSession session, int channelId, @Nonnull Collection<Integer> subChannelIds );
}
