package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import org.jspecify.annotations.NonNull;

/**
 * Notification when a Connector requested synchronized but is out of synchronization with the backend.
 */
public final class OutOfSyncEvent implements SerializableEvent {
    private final int _systemSchemaId;

    public OutOfSyncEvent(final int systemSchemaId) {
        _systemSchemaId = systemSchemaId;
    }

    public int getSystemSchemaId() {
        return _systemSchemaId;
    }

    @Override
    public void toMap(@NonNull final Map<String, Object> map) {
        map.put("type", "Connector.OutOfSync");
        map.put("systemSchema.id", getSystemSchemaId());
    }
}
