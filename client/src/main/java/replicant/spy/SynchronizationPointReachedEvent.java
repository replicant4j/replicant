package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import org.jspecify.annotations.NonNull;

/**
 * Notification when a Connector reaches a Synchronization Point.
 */
public final class SynchronizationPointReachedEvent implements SerializableEvent {
    private final int _systemSchemaId;

    public SynchronizationPointReachedEvent(final int systemSchemaId) {
        _systemSchemaId = systemSchemaId;
    }

    public int getSystemSchemaId() {
        return _systemSchemaId;
    }

    @Override
    public void toMap(@NonNull final Map<String, Object> map) {
        map.put("type", "Connector.SynchronizationPointReached");
        map.put("systemSchema.id", getSystemSchemaId());
    }
}
