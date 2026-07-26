package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import replicant.DatasetAddress;

/**
 * Notification when a Connector completes an unsubscribe operation.
 */
public final class UnsubscribeCompletedEvent implements SerializableEvent {
    private final int _schemaId;

    @NonNull
    private final String _schemaName;

    @NonNull
    private final DatasetAddress _datasetAddress;

    public UnsubscribeCompletedEvent(
            final int schemaId, @NonNull final String schemaName, @NonNull final DatasetAddress datasetAddress) {
        _schemaId = schemaId;
        _schemaName = Objects.requireNonNull(schemaName);
        _datasetAddress = Objects.requireNonNull(datasetAddress);
    }

    public int getSchemaId() {
        return _schemaId;
    }

    @NonNull
    public String getSchemaName() {
        return _schemaName;
    }

    @NonNull
    public DatasetAddress getDatasetAddress() {
        return _datasetAddress;
    }

    @Override
    public void toMap(@NonNull final Map<String, Object> map) {
        map.put("type", "Connector.UnsubscribeCompleted");
        map.put("schema.id", getSchemaId());
        map.put("schema.name", getSchemaName());
        final DatasetAddress datasetAddress = getDatasetAddress();
        map.put("datasetAddress.schemaId", datasetAddress.schemaId());
        map.put("datasetAddress.datasetId", datasetAddress.datasetId());
        map.put("datasetAddress.datasetRootId", datasetAddress.datasetRootId());
    }
}
