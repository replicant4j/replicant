package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import org.jspecify.annotations.NonNull;

/**
 * Notification when a Connector is not at a Synchronization Point.
 */
public final class SynchronizationPointPendingEvent implements SerializableEvent {
    private final int _systemSchemaId;

    public SynchronizationPointPendingEvent(final int systemSchemaId) {
        _systemSchemaId = systemSchemaId;
    }

    public int getSystemSchemaId() {
        return _systemSchemaId;
    }

    @Override
    public void toMap(@NonNull final Map<String, Object> map) {
        map.put("type", "Connector.SynchronizationPointPending");
        map.put("systemSchema.id", getSystemSchemaId());
    }
}
