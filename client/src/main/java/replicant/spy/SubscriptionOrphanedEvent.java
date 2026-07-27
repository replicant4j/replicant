package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import replicant.DatasetAddress;
import replicant.Subscription;

/**
 * Notification when a Subscription is orphaned from its Area of Interest.
 * Subscription Reconciliation identifies orphaned Subscriptions while comparing desired Area of Interest state with
 * actual Subscription state. The server either transitions the Subscription to Implicit Subscription Mode when a
 * Subscription Dependency retains it or removes it.
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
        map.put("datasetAddress.systemSchemaId", datasetAddress.systemSchemaId());
        map.put("datasetAddress.datasetId", datasetAddress.datasetId());
        map.put("datasetAddress.datasetRootId", datasetAddress.datasetRootId());
        map.put("subscription.filterParameter", getSubscription().getFilterParameter());
    }
}
