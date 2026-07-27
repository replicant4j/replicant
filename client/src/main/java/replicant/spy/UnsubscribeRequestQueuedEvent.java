package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import replicant.DatasetAddress;

/**
 * Notification when a Subscription removal is requested.
 */
public final class UnsubscribeRequestQueuedEvent implements SerializableEvent {
    @NonNull
    private final DatasetAddress _datasetAddress;

    public UnsubscribeRequestQueuedEvent(@NonNull final DatasetAddress datasetAddress) {
        _datasetAddress = Objects.requireNonNull(datasetAddress);
    }

    @NonNull
    public DatasetAddress getDatasetAddress() {
        return _datasetAddress;
    }

    @Override
    public void toMap(@NonNull final Map<String, Object> map) {
        map.put("type", "Connector.UnsubscribeRequestQueued");
        final DatasetAddress datasetAddress = getDatasetAddress();
        map.put("datasetAddress.systemSchemaId", datasetAddress.systemSchemaId());
        map.put("datasetAddress.datasetId", datasetAddress.datasetId());
        map.put("datasetAddress.datasetRootId", datasetAddress.datasetRootId());
    }
}
