package replicant;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class Command {
    @NonNull
    private final String _name;

    @Nullable
    private final Object _payload;

    @Nullable
    private final CommandResultHandler _commandResultHandler;

    private int _requestId;

    Command(
            @NonNull final String name,
            @Nullable final Object payload,
            @Nullable final CommandResultHandler commandResultHandler) {
        _name = Objects.requireNonNull(name);
        _payload = payload;
        _commandResultHandler = commandResultHandler;
        _requestId = -1;
    }

    @NonNull
    String getName() {
        return _name;
    }

    @Nullable
    Object getPayload() {
        return _payload;
    }

    @Nullable
    CommandResultHandler getCommandResultHandler() {
        return _commandResultHandler;
    }

    boolean isInProgress() {
        return -1 != _requestId;
    }

    int getRequestId() {
        return _requestId;
    }

    void markAsInProgress(final int requestId) {
        _requestId = requestId;
    }

    void markAsComplete() {
        _requestId = -1;
    }

    @Override
    public String toString() {
        if (Replicant.areNamesEnabled()) {
            return "Command[" + "Name="
                    + _name + (null == _payload ? "" : " Payload=" + String.valueOf(_payload))
                    + "]" + (-1 != _requestId ? "(InProgress)" : "");
        } else {
            return super.toString();
        }
    }
}
