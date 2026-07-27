package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Notification when a Connector generated an error processing a message from a DataSource.
 */
public final class MessageProcessFailureEvent implements SerializableEvent {
    private final int _systemSchemaId;

    @NonNull
    private final String _systemSchemaName;

    @NonNull
    private final Throwable _error;

    public MessageProcessFailureEvent(
            final int systemSchemaId, @NonNull final String systemSchemaName, @NonNull final Throwable error) {
        _systemSchemaId = systemSchemaId;
        _systemSchemaName = Objects.requireNonNull(systemSchemaName);
        _error = Objects.requireNonNull(error);
    }

    public int getSystemSchemaId() {
        return _systemSchemaId;
    }

    @NonNull
    public String getSystemSchemaName() {
        return _systemSchemaName;
    }

    @NonNull
    public Throwable getError() {
        return _error;
    }

    @Override
    public void toMap(@NonNull final Map<String, Object> map) {
        map.put("type", "Connector.MessageProcessFailure");
        map.put("systemSchema.id", getSystemSchemaId());
        map.put("systemSchema.name", getSystemSchemaName());
        final Throwable throwable = getError();
        map.put("message", null == throwable.getMessage() ? throwable.toString() : throwable.getMessage());
    }
}
