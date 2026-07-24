package replicant.server.transport;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
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
  ReplicantSession getSession( @NonNull String sessionId );

  <T> T runRequest( @NonNull String invocationKey,
                    @Nullable ReplicantSession session,
                    @Nullable Integer requestId,
                    @NonNull Callable<T> action )
    throws Exception;

  boolean isAuthorized( @NonNull ReplicantSession session );

  void execCommand( @NonNull ReplicantSession session,
                    @NonNull String command,
                    int requestId,
                    @Nullable JsonObject payload );

  /**
   * Invalidate specified session.
   *
   * @param session the session.
   */
  void invalidateSession( @NonNull ReplicantSession session );

  /**
   * Create replicant session for specified WebSocket session.
   * It is assumed the username has already been authenticated and this is just tracking the session.
   *
   * @return the new session.
   */
  @NonNull
  ReplicantSession createSession( @NonNull Session webSocketSession,
                                   @NonNull ReplicantSessionAuthorization authorization );

  /**
   * @return the metadata for replicant system.
   */
  @NonNull
  SchemaMetaData getSchemaMetaData();

  void setETags( @NonNull ReplicantSession session, @NonNull final Map<ChannelAddress, String> eTags );

  void subscribe( @NonNull ReplicantSession session,
                  int requestId,
                  @NonNull List<ChannelAddress> addresses,
                  @Nullable JsonObject filter );

  void unsubscribe( @NonNull ReplicantSession session,
                    int requestId,
                    @NonNull List<ChannelAddress> addresses );

  /**
   * Send the "Change" message to the client.
   * This change is (most likely) the result of a request.
   * If the session that initiated the request is the specified session,
   * then the requestId and response parameters will be present.
   *
   * @param session the session
   * @param packet  the packet  associated with the change.
   * @return true if the message was sent, false if the session is closed or packet did not need to be sent.
   */
  boolean sendChangeMessage( @NonNull ReplicantSession session, @NonNull Packet packet );

  /**
   * Clears any cached data associated with the system.
   * This operation is typically called to free up resources or reset internal state.
   * Implementations should ensure that any temporary or intermediate data stored in memory
   * is purged, without impacting the operational integrity of established sessions or ongoing tasks.
   */
  void clearCache();
}
