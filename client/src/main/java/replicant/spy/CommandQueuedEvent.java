package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Notification when a Connector queues a Command.
 */
public final class CommandQueuedEvent implements SerializableEvent {
    private final int _systemSchemaId;

    @NonNull
    private final String _systemSchemaName;

    @NonNull
    private final String _commandName;

    public CommandQueuedEvent(
            final int systemSchemaId, @NonNull final String systemSchemaName, @NonNull final String commandName) {
        _systemSchemaId = systemSchemaId;
        _systemSchemaName = Objects.requireNonNull(systemSchemaName);
        _commandName = Objects.requireNonNull(commandName);
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

    @Override
    public void toMap(@NonNull final Map<String, Object> map) {
        map.put("type", "Connector.CommandQueued");
        map.put("systemSchema.id", getSystemSchemaId());
        map.put("systemSchema.name", getSystemSchemaName());
        map.put("command.name", getCommandName());
    }
}
