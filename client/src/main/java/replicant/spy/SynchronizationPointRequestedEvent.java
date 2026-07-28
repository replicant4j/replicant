package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import org.jspecify.annotations.NonNull;

/**
 * Notification when a Connector requests a Synchronization Point.
 */
public final class SynchronizationPointRequestedEvent implements SerializableEvent {
    private final int _systemSchemaId;

    public SynchronizationPointRequestedEvent(final int systemSchemaId) {
        _systemSchemaId = systemSchemaId;
    }

    public int getSystemSchemaId() {
        return _systemSchemaId;
    }

    @Override
    public void toMap(@NonNull final Map<String, Object> map) {
        map.put("type", "Connector.SynchronizationPointRequested");
        map.put("systemSchema.id", getSystemSchemaId());
    }
}
