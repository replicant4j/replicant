package replicant;

import org.jspecify.annotations.NonNull;

/**
 * Interface invoked after processing a MessageResponse.
 */
public interface OnReplicaUpdateAction {
    void onReplicaUpdate(@NonNull ReplicantContext context, @NonNull Object replica);
}
