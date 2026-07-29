package replicant.server.transport;

import java.util.List;
import javax.json.JsonObject;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.server.ChangeSet;
import replicant.server.DatasetAddress;
import replicant.server.DatasetAddressCandidate;
import replicant.server.DatasetAddressTemplate;
import replicant.server.EntityChangeCandidate;

/**
 * Application-supplied adapter that connects the Replicant server runtime to the application System Schema,
 * authorization, commands, persistence, and Dataset selection logic.
 */
public interface ReplicantServerAdapter {
    @NonNull
    SystemSchema getSystemSchema();

    boolean isAuthorized(@NonNull ReplicantSession session);

    void preSubscribe(
            @NonNull ReplicantSession session,
            @NonNull DatasetAddress datasetAddress,
            @Nullable JsonObject filterParameter);

    /**
     * Hook invoked before sending a Change Set to the given session.
     * Used to optimise expansion of the Change Set prior to performing the normal expand cycle.
     *
     * @param session the session to which the Change Set is being sent. Must not be null.
     * @param packet  the packet containing the Change Set to be sent. Must not be null.
     */
    void preSendChangeSet(@NonNull ReplicantSession session, @NonNull Packet packet);

    /**
     * Derive a Filter Parameter for the target Dataset Address based on the source Dataset Address and Filter
     * Parameter.
     *
     * @param entityChangeCandidate the Entity Change Candidate in the context of which the Dataset Link is being
     *                              evaluated.
     * @param sourceDatasetAddress the source Dataset Address.
     * @param sourceFilterParameter the Filter Parameter for the source Dataset Address.
     * @param targetDatasetAddressCandidate the target Dataset Address Candidate.
     * @return the Filter Parameter for the target Dataset Address.
     */
    @NonNull
    JsonObject deriveTargetFilterParameter(
            @NonNull EntityChangeCandidate entityChangeCandidate,
            @NonNull DatasetAddress sourceDatasetAddress,
            @Nullable JsonObject sourceFilterParameter,
            @NonNull DatasetAddressCandidate targetDatasetAddressCandidate);

    /**
     * Derive the target Dataset Key for a target Dataset Address Template.
     *
     * @param entityChangeCandidate the Entity Change Candidate in the context of which the Dataset Link is being
     *                              evaluated.
     * @param sourceDatasetAddress the concrete source Dataset Address.
     * @param sourceFilterParameter the Filter Parameter for the source Dataset Address.
     * @param targetDatasetAddressTemplate the target Dataset Address Template with a missing Dataset Key.
     * @param targetFilterParameter the target Filter Parameter if already known, null otherwise.
     * @return the Dataset Key for the target Dataset Address.
     */
    @NonNull
    String deriveTargetDatasetKey(
            @NonNull EntityChangeCandidate entityChangeCandidate,
            @NonNull DatasetAddress sourceDatasetAddress,
            @Nullable JsonObject sourceFilterParameter,
            @NonNull DatasetAddressTemplate targetDatasetAddressTemplate,
            @Nullable JsonObject targetFilterParameter);

    /**
     * Flush the EntityManager that contains replicated entities.
     *
     * @return true if the EntityManager was open and flushed, false if was not open or could not be flushed.
     */
    boolean flushOpenEntityManager();

    void executeCommand(
            @NonNull ReplicantSession session,
            @NonNull String commandName,
            int requestId,
            @Nullable JsonObject payload);

    /**
     * Collect the current contents of a Dataset selection into the Change Set when establishing one or more
     * Subscriptions.
     * If the session is not null, then the implementation is expected to update the actual Subscription state.
     *
     * @param session             the session. May be null while collecting a Dataset Cache Entry Change Set.
     * @param datasetAddresses the Dataset Addresses to collect data for; every address must have the same Dataset ID
     * @param filterParameter  the Filter Parameter. May be null if the Dataset has no Filter Parameter.
     * @param changeSet           the changeSet to add the collected data to.
     * @param mode the Subscription Mode, ignored unless session is non-null.
     */
    void collectSubscriptionData(
            @Nullable ReplicantSession session,
            @NonNull List<DatasetAddress> datasetAddresses,
            @Nullable JsonObject filterParameter,
            @NonNull ChangeSet changeSet,
            @NonNull SubscriptionMode mode);

    /**
     * Collect the current contents of a Dataset selection into the Change Set when changing a Subscription Filter
     * Parameter.
     * It is expected that the hook does everything including updating Subscription with new
     * Filter Parameter and reconciling Subscription Dependencies.
     *
     * @param session        the session.
     * @param datasetAddresses the Dataset Addresses to collect data for; every address must have the same Dataset ID
     * @param originalFilterParameter the old Filter Parameter.
     * @param newFilterParameter      the new Filter Parameter.
     * @param changeSet      the changeSet to add the collected data to.
     */
    void collectSubscriptionDataForFilterParameterChange(
            @NonNull ReplicantSession session,
            @NonNull List<DatasetAddress> datasetAddresses,
            @NonNull JsonObject originalFilterParameter,
            @NonNull JsonObject newFilterParameter,
            @NonNull ChangeSet changeSet);

    @Nullable
    EntityChangeCandidate filterEntityChangeCandidate(
            @NonNull ReplicantSession session,
            @NonNull DatasetAddress datasetAddress,
            @NonNull EntityChangeCandidate entityChangeCandidate);

    boolean shouldFollowDatasetLink(
            @NonNull DatasetAddress sourceDatasetAddress,
            @Nullable JsonObject sourceFilterParameter,
            @NonNull DatasetAddress targetDatasetAddress,
            @Nullable JsonObject targetFilterParameter);
}
