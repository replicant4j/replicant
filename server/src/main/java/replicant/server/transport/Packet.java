package replicant.server.transport;

import java.util.Collection;
import java.util.Collections;
import javax.json.JsonValue;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.server.ChangeSet;
import replicant.server.DatasetAddress;
import replicant.server.EntityChangeCandidate;

/**
 * Packet contains the data generated from the transaction that needs to be sent to a specific client.
 * This packet has not been fully resolved and is just used to pass the data to another thread that will perform
 * Subscription Dependency expansion operation.
 *
 * @param fromSubscriptionRequest true if the packet resulted from a Subscription request.
 * @param requestId               the request that resulted in this change when the packet is sent to the initiator.
 * @param response                the response when the packet is sent to the request initiator.
 * @param datasetCacheVersion     the opaque Dataset Cache Version for a complete Cacheable Dataset Change Set, if any.
 * @param entityChangeCandidates  the Entity Change Candidates collected during the transaction.
 * @param changeSet               the complete Change Set carried by this packet.
 * @param datasetCacheEntryAddress the Dataset Address of the client Dataset Cache Entry to reuse, or null for a
 *                                 Change Set packet.
 */
public record Packet(
        boolean fromSubscriptionRequest,
        @Nullable Integer requestId,
        @Nullable JsonValue response,
        @Nullable String datasetCacheVersion,
        @NonNull Collection<EntityChangeCandidate> entityChangeCandidates,
        @NonNull ChangeSet changeSet,
        @Nullable DatasetAddress datasetCacheEntryAddress) {
    public Packet(
            final boolean fromSubscriptionRequest,
            @Nullable final Integer requestId,
            @Nullable final JsonValue response,
            @Nullable final String datasetCacheVersion,
            @NonNull final Collection<EntityChangeCandidate> entityChangeCandidates,
            @NonNull final ChangeSet changeSet) {
        this(
                fromSubscriptionRequest,
                requestId,
                response,
                datasetCacheVersion,
                entityChangeCandidates,
                changeSet,
                null);
    }

    @NonNull
    static Packet datasetCacheEntryReference(
            @Nullable final Integer requestId,
            @NonNull final DatasetAddress datasetAddress,
            @NonNull final String datasetCacheVersion) {
        return new Packet(
                true, requestId, null, datasetCacheVersion, Collections.emptyList(), new ChangeSet(), datasetAddress);
    }
}
