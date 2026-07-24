package replicant;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class TestLogger implements ReplicantLogger.Logger {
    static final class LogEntry {
        @NonNull
        private final String _message;

        @Nullable
        private final Throwable _throwable;

        LogEntry(@NonNull final String message, @Nullable final Throwable throwable) {
            _message = message;
            _throwable = throwable;
        }

        @NonNull
        String getMessage() {
            return _message;
        }

        @Nullable
        Throwable getThrowable() {
            return _throwable;
        }
    }

    @NonNull
    private final List<LogEntry> _entries = new ArrayList<>();

    @Override
    public void log(@NonNull final String message, @Nullable final Throwable throwable) {
        _entries.add(new LogEntry(message, throwable));
    }

    @NonNull
    List<LogEntry> getEntries() {
        return _entries;
    }
}
