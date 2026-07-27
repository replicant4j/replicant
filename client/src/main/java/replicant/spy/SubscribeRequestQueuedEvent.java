package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.DatasetAddress;

/**
 * Notification when a Subscription is requested.
 */
public final class SubscribeRequestQueuedEvent implements SerializableEvent {
    @NonNull
    private final DatasetAddress _datasetAddress;

    @Nullable
    private final Object _filterParameter;

    public SubscribeRequestQueuedEvent(
            @NonNull final DatasetAddress datasetAddress, @Nullable final Object filterParameter) {
        _datasetAddress = Objects.requireNonNull(datasetAddress);
        _filterParameter = filterParameter;
    }

    @NonNull
    public DatasetAddress getDatasetAddress() {
        return _datasetAddress;
    }

    @Nullable
    public Object getFilterParameter() {
        return _filterParameter;
    }

    @Override
    public void toMap(@NonNull final Map<String, Object> map) {
        map.put("type", "Connector.SubscribeRequestQueued");
        final DatasetAddress datasetAddress = getDatasetAddress();
        map.put("datasetAddress.systemSchemaId", datasetAddress.systemSchemaId());
        map.put("datasetAddress.datasetId", datasetAddress.datasetId());
        map.put("datasetAddress.datasetRootId", datasetAddress.datasetRootId());
        map.put("subscription.filterParameter", getFilterParameter());
    }
}
