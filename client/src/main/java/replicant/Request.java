package replicant;

import java.util.Objects;
import org.jspecify.annotations.NonNull;

public final class Request {
    @NonNull
    private final Connection _connection;

    @NonNull
    private final RequestEntry _entry;

    Request(@NonNull final Connection connection, @NonNull final RequestEntry entry) {
        _connection = Objects.requireNonNull(connection);
        _entry = Objects.requireNonNull(entry);
    }

    @NonNull
    public String getConnectionId() {
        return _connection.ensureConnectionId();
    }

    public int getRequestId() {
        return _entry.getRequestId();
    }
}
