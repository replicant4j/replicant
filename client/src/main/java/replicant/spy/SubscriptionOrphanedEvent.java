package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import replicant.DatasetAddress;
import replicant.Subscription;

/**
 * Notification when an Subscription is "orphaned".
 * An "orphaned" subscription no longer has an explicit subscription to it. This may result in it being
 * unsubscribed if there is no implicit subscription within the system. Subscription Reconciliation is responsible
 * for identifying orphaned subscriptions while comparing desired AreaOfInterest state with actual Subscription state.
 */
public final class SubscriptionOrphanedEvent implements SerializableEvent {
    @NonNull
    private final Subscription _subscription;

    public SubscriptionOrphanedEvent(@NonNull final Subscription subscription) {
        _subscription = Objects.requireNonNull(subscription);
    }

    @NonNull
    public Subscription getSubscription() {
        return _subscription;
    }

    @Override
    public void toMap(@NonNull final Map<String, Object> map) {
        map.put("type", "Subscription.Orphaned");
        final DatasetAddress datasetAddress = getSubscription().datasetAddress();
        map.put("datasetAddress.schemaId", datasetAddress.schemaId());
        map.put("datasetAddress.datasetId", datasetAddress.datasetId());
        map.put("datasetAddress.datasetRootId", datasetAddress.datasetRootId());
        map.put("channel.filter", getSubscription().getFilter());
    }
}
