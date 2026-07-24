package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import org.jspecify.annotations.NonNull;

/**
 * Notification when a Connector is synchronized with the backend.
 */
public final class InSyncEvent implements SerializableEvent {
    private final int _schemaId;

    public InSyncEvent(final int schemaId) {
        _schemaId = schemaId;
    }

    public int getSchemaId() {
        return _schemaId;
    }

    @Override
    public void toMap(@NonNull final Map<String, Object> map) {
        map.put("type", "Connector.InSync");
        map.put("schema.id", getSchemaId());
    }
}
