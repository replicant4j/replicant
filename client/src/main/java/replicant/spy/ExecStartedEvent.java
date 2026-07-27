package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Notification when a Connector sends an Exec message.
 */
public final class ExecStartedEvent implements SerializableEvent {
    private final int _systemSchemaId;

    @NonNull
    private final String _systemSchemaName;

    @NonNull
    private final String _command;

    private final int _requestId;

    public ExecStartedEvent(
            final int systemSchemaId,
            @NonNull final String systemSchemaName,
            @NonNull final String command,
            final int requestId) {
        _systemSchemaId = systemSchemaId;
        _systemSchemaName = Objects.requireNonNull(systemSchemaName);
        _command = Objects.requireNonNull(command);
        _requestId = requestId;
    }

    public int getSystemSchemaId() {
        return _systemSchemaId;
    }

    @NonNull
    public String getSystemSchemaName() {
        return _systemSchemaName;
    }

    @NonNull
    public String getCommand() {
        return _command;
    }

    public int getRequestId() {
        return _requestId;
    }

    @Override
    public void toMap(@NonNull final Map<String, Object> map) {
        map.put("type", "Connector.ExecStarted");
        map.put("systemSchema.id", getSystemSchemaId());
        map.put("systemSchema.name", getSystemSchemaName());
        map.put("command", getCommand());
        map.put("requestId", getRequestId());
    }
}
