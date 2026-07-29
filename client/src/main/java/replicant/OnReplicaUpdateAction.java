package replicant;

import org.jspecify.annotations.NonNull;

/**
 * Interface invoked during Message Processing after a Replica is updated.
 */
public interface OnReplicaUpdateAction {
    void onReplicaUpdate(@NonNull ReplicantContext context, @NonNull Object replica);
}
