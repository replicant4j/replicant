package replicant;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class PendingRequest {
    @Nullable
    private final String _name;

    @NonNull
    private final SafeProcedure _callback;

    @Nullable
    private final CommandResultHandler _commandResultHandler;

    PendingRequest(
            @Nullable final String name,
            @NonNull final SafeProcedure callback,
            @Nullable final CommandResultHandler commandResultHandler) {
        _name = name;
        _callback = Objects.requireNonNull(callback);
        _commandResultHandler = commandResultHandler;
    }

    @Nullable
    String getName() {
        return _name;
    }

    @NonNull
    SafeProcedure getCallback() {
        return _callback;
    }

    @Nullable
    CommandResultHandler getCommandResultHandler() {
        return _commandResultHandler;
    }
}
