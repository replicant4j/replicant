package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import replicant.DatasetAddress;

/**
 * Notification when a Connector completes an update of a Subscription's Filter Parameter.
 */
public final class SubscriptionUpdateCompletedEvent implements SerializableEvent {
    private final int _systemSchemaId;

    @NonNull
    private final String _systemSchemaName;

    @NonNull
    private final DatasetAddress _datasetAddress;

    public SubscriptionUpdateCompletedEvent(
            final int systemSchemaId,
            @NonNull final String systemSchemaName,
            @NonNull final DatasetAddress datasetAddress) {
        _systemSchemaId = systemSchemaId;
        _systemSchemaName = Objects.requireNonNull(systemSchemaName);
        _datasetAddress = Objects.requireNonNull(datasetAddress);
    }

    public int getSystemSchemaId() {
        return _systemSchemaId;
    }

    @NonNull
    public String getSystemSchemaName() {
        return _systemSchemaName;
    }

    @NonNull
    public DatasetAddress getDatasetAddress() {
        return _datasetAddress;
    }

    @Override
    public void toMap(@NonNull final Map<String, Object> map) {
        map.put("type", "Connector.SubscriptionUpdateCompleted");
        map.put("systemSchema.id", getSystemSchemaId());
        map.put("systemSchema.name", getSystemSchemaName());
        final DatasetAddress datasetAddress = getDatasetAddress();
        map.put("datasetAddress.systemSchemaId", datasetAddress.systemSchemaId());
        map.put("datasetAddress.datasetId", datasetAddress.datasetId());
        map.put("datasetAddress.datasetRootId", datasetAddress.datasetRootId());
    }
}
