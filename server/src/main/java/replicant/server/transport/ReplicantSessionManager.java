package replicant.server.transport;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.websocket.Session;
import replicant.server.ChangeSet;
import replicant.server.ChannelAddress;
import replicant.server.EntityMessage;

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

  void subscribe( @Nonnull ReplicantSession session,
                  int requestId,
                  @Nonnull ChannelAddress address,
                  @Nullable Object filter );

  void bulkSubscribe( @Nonnull ReplicantSession session,
                      int requestId,
                      int channelId,
                      @Nullable Collection<Integer> rootIds,
                      @Nullable Object filter );

  void unsubscribe( @Nonnull ReplicantSession session, int requestId, @Nonnull ChannelAddress address );

  void bulkUnsubscribe( @Nonnull ReplicantSession session,
                        int requestId,
                        int channelId,
                        @Nonnull Collection<Integer> rootIds );

  /**
   * Send the "Change" message to the client.
   * This change is (most likely) the result of a request.
   * If the session that initiated the request is the specified session,
   * then the requestId and response parameters will be present.
   *
   * @param session   the session
   * @param requestId the requestId if the change is in response to a request and the request was initiated by the session.
   * @param response  the response message if a response exists,
   *                  and the change is in response to a request and the request was initiated by the session.
   * @param etag      the etag associated with the data. Unique identifier used during caching.
   * @param messages  the changes to send to the session.
   * @param changeSet the changeSet associated with the session
   */
  void sendChangeMessage( @Nonnull ReplicantSession session,
                          @Nullable Integer requestId,
                          @Nullable JsonValue response,
                          @Nullable String etag,
                          @Nonnull Collection<EntityMessage> messages,
                          @Nonnull ChangeSet changeSet );

  /**
   * Exposed so that bulk changes can reset Cache.
   */
  void deleteAllCacheEntries();
}
