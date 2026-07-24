package replicant;

import org.jspecify.annotations.NonNull;

/**
 * Interface invoked after processing a MessageResponse.
 */
public interface OnEntityUpdateAction {
    void onEntityUpdate(@NonNull ReplicantContext context, @NonNull Object entity);
}
