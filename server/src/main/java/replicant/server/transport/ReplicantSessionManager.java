package replicant.server.transport;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.JsonObject;
import javax.websocket.Session;
import replicant.server.ChannelAddress;

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

  <T> T runRequest( @Nonnull String invocationKey,
                    @Nullable ReplicantSession session,
                    @Nullable Integer requestId,
                    @Nonnull Callable<T> action )
    throws Exception;

  boolean isAuthorized( @Nonnull ReplicantSession session );

  void execCommand( @Nonnull ReplicantSession session,
                    @Nonnull String command,
                    int requestId,
                    @Nullable JsonObject payload );

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
  SchemaMetaData getSchemaMetaData();

  void setETags( @Nonnull ReplicantSession session, @Nonnull final Map<ChannelAddress, String> eTags );

  void bulkSubscribe( @Nonnull ReplicantSession session,
                      int requestId,
                      @Nonnull List<ChannelAddress> addresses,
                      @Nullable Object filter );

  void bulkUnsubscribe( @Nonnull ReplicantSession session,
                        int requestId,
                        @Nonnull List<ChannelAddress> addresses );

  /**
   * Send the "Change" message to the client.
   * This change is (most likely) the result of a request.
   * If the session that initiated the request is the specified session,
   * then the requestId and response parameters will be present.
   *
   * @param session   the session
   * @param packet  the packet  associated with the change.
   */
  void sendChangeMessage( @Nonnull ReplicantSession session, @Nonnull Packet packet  );

  /**
   * Exposed so that bulk changes can reset Cache.
   */
  void deleteAllCacheEntries();
}
