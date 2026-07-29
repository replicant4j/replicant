package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Notification when a Connector receives a response to a Command.
 */
public final class CommandCompletedEvent implements SerializableEvent {
    private final int _systemSchemaId;

    @NonNull
    private final String _systemSchemaName;

    @NonNull
    private final String _commandName;

    private final int _requestId;

    public CommandCompletedEvent(
            final int systemSchemaId,
            @NonNull final String systemSchemaName,
            @NonNull final String commandName,
            final int requestId) {
        _systemSchemaId = systemSchemaId;
        _systemSchemaName = Objects.requireNonNull(systemSchemaName);
        _commandName = Objects.requireNonNull(commandName);
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
    public String getCommandName() {
        return _commandName;
    }

    public int getRequestId() {
        return _requestId;
    }

    @Override
    public void toMap(@NonNull final Map<String, Object> map) {
        map.put("type", "Connector.CommandCompleted");
        map.put("systemSchema.id", getSystemSchemaId());
        map.put("systemSchema.name", getSystemSchemaName());
        map.put("command.name", getCommandName());
        map.put("requestId", getRequestId());
    }
}
