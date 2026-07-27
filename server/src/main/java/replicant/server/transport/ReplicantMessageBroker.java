package replicant.server.transport;

import java.util.Collection;
import javax.json.JsonValue;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.server.ChangeSet;
import replicant.server.EntityChangeCandidate;

public interface ReplicantMessageBroker {
    @NonNull
    Packet queueChangeMessage(
            @NonNull ReplicantSession session,
            boolean fromSubscriptionRequest,
            @Nullable Integer requestId,
            @Nullable JsonValue response,
            @Nullable String etag,
            @NonNull Collection<EntityChangeCandidate> messages,
            @NonNull ChangeSet changeSet);
}
