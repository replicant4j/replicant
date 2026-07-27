package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import replicant.DatasetAddress;

/**
 * Notification when a Connector completes a subscribe operation.
 */
public final class SubscribeCompletedEvent implements SerializableEvent {
    private final int _systemSchemaId;

    @NonNull
    private final String _systemSchemaName;

    @NonNull
    private final DatasetAddress _datasetAddress;

    public SubscribeCompletedEvent(
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
        map.put("type", "Connector.SubscribeCompleted");
        map.put("systemSchema.id", getSystemSchemaId());
        map.put("systemSchema.name", getSystemSchemaName());
        final DatasetAddress datasetAddress = getDatasetAddress();
        map.put("datasetAddress.systemSchemaId", datasetAddress.systemSchemaId());
        map.put("datasetAddress.datasetId", datasetAddress.datasetId());
        map.put("datasetAddress.datasetRootId", datasetAddress.datasetRootId());
    }
}
