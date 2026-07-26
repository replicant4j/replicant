package replicant;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Interface used during subscription updates to remove Replicas from subscriptions
 * as a result of a filter change. Each channel with a {@link replicant.ChannelSchema.FilterType#DYNAMIC}
 * filter type must be associated with a filter of this type.
 */
public interface SubscriptionUpdateReplicaFilter<T> {
    /**
     * Return true if the specified Replica is matched by the channel-designated filter.
     * This interfaces is invoked when the server updates a subscription and a client is responsible
     * for removing local Replicas from that subscription that no longer match the filter.
     *
     * @param filter the filter.
     * @param replicaEntry the Replica Entry to match.
     */
    boolean doesReplicaMatchFilter(@Nullable T filter, @NonNull ReplicaEntry replicaEntry);
}
