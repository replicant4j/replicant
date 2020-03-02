package org.realityforge.replicant.server.transport;

import java.util.Collection;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.websocket.Session;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.ChannelAddress;
import org.realityforge.replicant.server.EntityMessage;

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
  boolean invalidateSession( @Nonnull ReplicantSession session )
    throws InterruptedException;

  /**
   * Create replicant session for specified WebSocket session.
   * It is assumed the username has already been authenticated and this is just tracking the session.
   *
   * @return the new session.
   */
  @Nonnull
  ReplicantSession createSession( @Nonnull Session webSocketSession );

  /**
   * @return the metadata for replicant system.
   */
  @Nonnull
  SystemMetaData getSystemMetaData();

  void subscribe( @Nonnull ReplicantSession session, @Nonnull ChannelAddress address, @Nullable Object filter )
    throws InterruptedException;

  void bulkSubscribe( @Nonnull ReplicantSession session,
                      int channelId,
                      @Nonnull Collection<Integer> subChannelIds,
                      @Nullable Object filter )
    throws InterruptedException;

  void unsubscribe( @Nonnull ReplicantSession session, @Nonnull ChannelAddress address )
    throws InterruptedException;

  void bulkUnsubscribe( @Nonnull ReplicantSession session, int channelId, @Nonnull Collection<Integer> subChannelIds )
    throws InterruptedException;

  void sendChangeMessage( @Nonnull ReplicantSession session,
                          @Nullable Integer requestId,
                          @Nullable String etag,
                          @Nonnull Collection<EntityMessage> messages,
                          @Nonnull ChangeSet changeSet );
}
