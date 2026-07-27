package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import replicant.DatasetAddress;
import replicant.Subscription;

/**
 * Notification when an Subscription is disposed.
 */
public final class SubscriptionDisposedEvent implements SerializableEvent {
    @NonNull
    private final Subscription _subscription;

    public SubscriptionDisposedEvent(@NonNull final Subscription subscription) {
        _subscription = Objects.requireNonNull(subscription);
    }

    @NonNull
    public Subscription getSubscription() {
        return _subscription;
    }

    @Override
    public void toMap(@NonNull final Map<String, Object> map) {
        map.put("type", "Subscription.Disposed");
        final DatasetAddress datasetAddress = getSubscription().datasetAddress();
        map.put("datasetAddress.systemSchemaId", datasetAddress.systemSchemaId());
        map.put("datasetAddress.datasetId", datasetAddress.datasetId());
        map.put("datasetAddress.datasetRootId", datasetAddress.datasetRootId());
        map.put("subscription.filterParameter", getSubscription().getFilterParameter());
        map.put("subscription.mode", getSubscription().getMode().name());
    }
}
