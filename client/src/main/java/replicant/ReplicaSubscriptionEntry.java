package replicant;

import arez.annotations.ArezComponent;
import arez.annotations.ComponentDependency;
import arez.annotations.Feature;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * An observable structure containing a Replica Entry reference used within a Subscription.
 *
 * <p>This is used so we can observe it in finder and thus finder will be rescheduled once the entry
 * is removed from the Subscription, even if the Replica Entry is not removed altogether.</p>
 */
@ArezComponent(requireId = Feature.DISABLE)
abstract class ReplicaSubscriptionEntry {
    /**
     * The underlying Replica Entry.
     */
    @NonNull
    @ComponentDependency
    final ReplicaEntry _replicaEntry;

    @NonNull
    static ReplicaSubscriptionEntry create(@NonNull final ReplicaEntry replicaEntry) {
        return new Arez_ReplicaSubscriptionEntry(replicaEntry);
    }

    /**
     * Create a subscription entry for a Replica Entry.
     *
     * @param replicaEntry the Replica Entry.
     */
    ReplicaSubscriptionEntry(@NonNull final ReplicaEntry replicaEntry) {
        _replicaEntry = Objects.requireNonNull(replicaEntry);
    }

    /**
     * Return the Replica Entry represented by this subscription entry.
     *
     * @return the Replica Entry.
     */
    @NonNull
    ReplicaEntry getReplicaEntry() {
        return _replicaEntry;
    }
}
