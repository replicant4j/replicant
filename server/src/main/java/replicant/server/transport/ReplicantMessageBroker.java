package replicant.server.transport;

import java.util.Collection;
import javax.json.JsonValue;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.server.ChangeSet;
import replicant.server.DatasetAddress;
import replicant.server.EntityChangeCandidate;

public interface ReplicantMessageBroker {
    @NonNull
    Packet queueDatasetCacheEntryReference(
            @NonNull ReplicantSession session,
            @Nullable Integer requestId,
            @NonNull DatasetAddress datasetAddress,
            @NonNull String datasetCacheVersion);

    @NonNull
    Packet queueChangeSet(
            @NonNull ReplicantSession session,
            boolean fromSubscriptionRequest,
            @Nullable Integer requestId,
            @Nullable JsonValue commandResult,
            @Nullable String datasetCacheVersion,
            @NonNull Collection<EntityChangeCandidate> entityChangeCandidates,
            @NonNull ChangeSet changeSet);
}
