package replicant.server.transport;

import java.util.List;
import javax.json.JsonObject;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.server.ChangeSet;
import replicant.server.DatasetAddress;
import replicant.server.EntityMessage;

public interface ReplicantSessionContext {
    @NonNull
    SchemaMetaData getSchemaMetaData();

    boolean isAuthorized(@NonNull ReplicantSession session);

    void preSubscribe(
            @NonNull ReplicantSession session, @NonNull DatasetAddress datasetAddress, @Nullable JsonObject filter);

    /**
     * Hook invoked before sending a change message to the given session.
     * Used to optimise expansion of change messages prior to performing the normal expand cycle.
     *
     * @param session the session in which the change message is being sent. Must not be null.
     * @param packet  the packet representing the change message to be sent. Must not be null.
     */
    void preSendChangeMessage(@NonNull ReplicantSession session, @NonNull Packet packet);

    /**
     * Derive a filter for the target Dataset Address based on the source Dataset Address and filter.
     *
     * @param entityMessage the Entity Message in the context of which the Dataset Link is being evaluated.
     * @param sourceDatasetAddress the source Dataset Address.
     * @param sourceFilter         the filter for the source Dataset Address.
     * @param targetDatasetAddress the target Dataset Address.
     * @return the filter for the target Dataset Address.
     */
    @NonNull
    JsonObject deriveTargetFilter(
            @NonNull EntityMessage entityMessage,
            @NonNull DatasetAddress sourceDatasetAddress,
            @Nullable JsonObject sourceFilter,
            @NonNull DatasetAddress targetDatasetAddress);

    /**
     * Derive the target Dataset Key for a partially specified target Dataset Address.
     *
     * @param entityMessage the Entity Message in the context of which the Dataset Link is being evaluated.
     * @param sourceDatasetAddress the concrete source Dataset Address.
     * @param sourceFilter         the filter for the source Dataset Address.
     * @param targetDatasetAddress the target Dataset Address template with a missing Dataset Key.
     * @param targetFilter         the target filter if already known, null otherwise.
     * @return the Dataset Key for the target Dataset Address.
     */
    @NonNull
    String deriveTargetDatasetKey(
            @NonNull EntityMessage entityMessage,
            @NonNull DatasetAddress sourceDatasetAddress,
            @Nullable JsonObject sourceFilter,
            @NonNull DatasetAddress targetDatasetAddress,
            @Nullable JsonObject targetFilter);

    /**
     * Flush the EntityManager that contains replicated entities.
     *
     * @return true if the EntityManager was open and flushed, false if was not open or could not be flushed.
     */
    boolean flushOpenEntityManager();

    void execCommand(
            @NonNull ReplicantSession session, @NonNull String command, int requestId, @Nullable JsonObject payload);

    /**
     * Add changes to the ChangeSet as a result of subscribing at one or more Dataset Addresses.
     * If the session is not null, then the implementation is expected to update the actual Subscription state.
     *
     * @param session             the session. May be null if data is being collected for caching.
     * @param datasetAddresses the Dataset Addresses to collect data for; every address must have the same Dataset ID
     * @param filter           the Filter Parameter. May be null if the Dataset has no Filter Parameter.
     * @param changeSet           the changeSet to add the collected data to.
     * @param isExplicitSubscribe true if the subscribe action is explicit, false if it is implicit, ignored unless session is non-null.
     */
    void collectSubscriptionData(
            @Nullable ReplicantSession session,
            @NonNull List<DatasetAddress> datasetAddresses,
            @Nullable JsonObject filter,
            @NonNull ChangeSet changeSet,
            boolean isExplicitSubscribe);

    /**
     * Add changes to the ChangeSet as a result of changing a Subscription Filter Parameter.
     * It is expected that the hook does everything including updating SubscriptionEntry with new
     * filter and reconciling Subscription Dependencies.
     *
     * @param session        the session.
     * @param datasetAddresses the Dataset Addresses to collect data for; every address must have the same Dataset ID
     * @param originalFilter the old Filter Parameter.
     * @param newFilter      the new Filter Parameter.
     * @param changeSet      the changeSet to add the collected data to.
     */
    void collectSubscriptionDataForFilterChange(
            @NonNull ReplicantSession session,
            @NonNull List<DatasetAddress> datasetAddresses,
            @NonNull JsonObject originalFilter,
            @NonNull JsonObject newFilter,
            @NonNull ChangeSet changeSet);

    @Nullable
    EntityMessage filterEntityMessage(
            @NonNull ReplicantSession session, @NonNull DatasetAddress datasetAddress, @NonNull EntityMessage message);

    boolean shouldFollowDatasetLink(
            @NonNull DatasetAddress sourceDatasetAddress,
            @Nullable JsonObject sourceFilter,
            @NonNull DatasetAddress targetDatasetAddress,
            @Nullable JsonObject targetFilter);
}
