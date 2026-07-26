package replicant;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Interface used during Subscription updates to re-evaluate Replica membership after the Filter Parameter changes.
 */
public interface FilterParameterUpdateReplicaMatcher<T> {
    /**
     * Return true if the specified Replica is matched by the Dataset Filter and Filter Parameter.
     * This interface is invoked when the server updates a Subscription and a client is responsible
     * for removing local Replicas from that Subscription that no longer match the Filter Parameter.
     *
     * @param filterParameter the Filter Parameter.
     * @param replicaEntry the Replica Entry to match.
     */
    boolean doesReplicaMatchFilterParameter(@Nullable T filterParameter, @NonNull ReplicaEntry replicaEntry);
}
