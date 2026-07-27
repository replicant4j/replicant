package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import org.jspecify.annotations.NonNull;

/**
 * Notification when a Connector is synchronized with the backend.
 */
public final class InSyncEvent implements SerializableEvent {
    private final int _systemSchemaId;

    public InSyncEvent(final int systemSchemaId) {
        _systemSchemaId = systemSchemaId;
    }

    public int getSystemSchemaId() {
        return _systemSchemaId;
    }

    @Override
    public void toMap(@NonNull final Map<String, Object> map) {
        map.put("type", "Connector.InSync");
        map.put("systemSchema.id", getSystemSchemaId());
    }
}
