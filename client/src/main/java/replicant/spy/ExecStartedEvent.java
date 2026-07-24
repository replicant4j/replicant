package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Notification when a Connector sends an Exec message.
 */
public final class ExecStartedEvent implements SerializableEvent {
    private final int _schemaId;

    @NonNull
    private final String _schemaName;

    @NonNull
    private final String _command;

    private final int _requestId;

    public ExecStartedEvent(
            final int schemaId, @NonNull final String schemaName, @NonNull final String command, final int requestId) {
        _schemaId = schemaId;
        _schemaName = Objects.requireNonNull(schemaName);
        _command = Objects.requireNonNull(command);
        _requestId = requestId;
    }

    public int getSchemaId() {
        return _schemaId;
    }

    @NonNull
    public String getSchemaName() {
        return _schemaName;
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
        map.put("schema.id", getSchemaId());
        map.put("schema.name", getSchemaName());
        map.put("command", getCommand());
        map.put("requestId", getRequestId());
    }
}
