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
    private final Object _filter;

    public SubscribeRequestQueuedEvent(@NonNull final DatasetAddress datasetAddress, @Nullable final Object filter) {
        _datasetAddress = Objects.requireNonNull(datasetAddress);
        _filter = filter;
    }

    @NonNull
    public DatasetAddress getDatasetAddress() {
        return _datasetAddress;
    }

    @Nullable
    public Object getFilter() {
        return _filter;
    }

    @Override
    public void toMap(@NonNull final Map<String, Object> map) {
        map.put("type", "Connector.SubscribeRequestQueued");
        final DatasetAddress datasetAddress = getDatasetAddress();
        map.put("datasetAddress.schemaId", datasetAddress.schemaId());
        map.put("datasetAddress.datasetId", datasetAddress.datasetId());
        map.put("datasetAddress.datasetRootId", datasetAddress.datasetRootId());
        map.put("channel.filter", getFilter());
    }
}
