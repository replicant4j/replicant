package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import org.jspecify.annotations.NonNull;

/**
 * Notification when a Connector requested synchronized but is out of synchronization with the backend.
 */
public final class OutOfSyncEvent implements SerializableEvent {
    private final int _schemaId;

    public OutOfSyncEvent(final int schemaId) {
        _schemaId = schemaId;
    }

    public int getSchemaId() {
        return _schemaId;
    }

    @Override
    public void toMap(@NonNull final Map<String, Object> map) {
        map.put("type", "Connector.OutOfSync");
        map.put("schema.id", getSchemaId());
    }
}
