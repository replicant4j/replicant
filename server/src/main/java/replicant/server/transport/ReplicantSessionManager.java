package replicant.server.transport;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.json.JsonObject;
import javax.websocket.Session;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.server.DatasetAddress;

public interface ReplicantSessionManager {
    /**
     * Return the Replicant Session for the specified Replicant Session ID.
     * Replicant Session IDs are effectively opaque.
     *
     * @param replicantSessionId the Replicant Session ID.
     * @return the associated Replicant Session or null if no such Replicant Session.
     */
    @Nullable
    ReplicantSession getSession(@NonNull String replicantSessionId);

    /**
     * Run an action as a Replication Invocation.
     */
    <T> T runReplicationInvocation(
            @NonNull String invocationKey,
            @Nullable ReplicantSession session,
            @Nullable Integer requestId,
            @NonNull Callable<T> action)
            throws Exception;

    boolean isAuthorized(@NonNull ReplicantSession session);

    void executeCommand(
            @NonNull ReplicantSession session,
            @NonNull String commandName,
            int requestId,
            @Nullable JsonObject payload);

    /**
     * Invalidate specified session.
     *
     * @param session the session.
     */
    void invalidateSession(@NonNull ReplicantSession session);

    /**
     * Create replicant session for specified WebSocket session.
     * It is assumed the username has already been authenticated and this is just tracking the session.
     *
     * @return the new session.
     */
    @NonNull
    ReplicantSession createSession(
            @NonNull Session webSocketSession, @NonNull ReplicantSessionAuthorization authorization);

    /**
     * @return the System Schema for the replicated system.
     */
    @NonNull
    SystemSchema getSystemSchema();

    void setDatasetCacheVersions(
            @NonNull ReplicantSession session, @NonNull final Map<DatasetAddress, String> datasetCacheVersions);

    void subscribe(
            @NonNull ReplicantSession session,
            int requestId,
            @NonNull List<DatasetAddress> datasetAddresses,
            @Nullable JsonObject filterParameter);

    void unsubscribe(@NonNull ReplicantSession session, int requestId, @NonNull List<DatasetAddress> datasetAddresses);

    /**
     * Send the Change Set in the packet to the client.
     * The Change Set is most likely the result of a request.
     * If the session that initiated the request is the specified session,
     * then the Request ID and Command Result will be present when applicable.
     *
     * @param session the session
     * @param packet  the packet containing the Change Set.
     * @return true if the Change Set was sent, false if the session is closed or packet did not need to be sent.
     */
    boolean sendChangeSet(@NonNull ReplicantSession session, @NonNull Packet packet);

    /**
     * Clear every Dataset Cache Entry associated with the System Schema.
     */
    void clearDatasetCacheEntries();
}
