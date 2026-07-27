package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Notification when a Request completes.
 */
public final class RequestCompletedEvent implements SerializableEvent {
    private final int _systemSchemaId;

    @NonNull
    private final String _systemSchemaName;

    private final int _requestId;

    @NonNull
    private final String _name;

    private final boolean _expectingResults;
    private final boolean _resultsArrived;

    public RequestCompletedEvent(
            final int systemSchemaId,
            @NonNull final String systemSchemaName,
            final int requestId,
            @NonNull final String name,
            final boolean expectingResults,
            final boolean resultsArrived) {
        _systemSchemaId = systemSchemaId;
        _systemSchemaName = Objects.requireNonNull(systemSchemaName);
        _requestId = requestId;
        _name = Objects.requireNonNull(name);
        _expectingResults = expectingResults;
        _resultsArrived = resultsArrived;
    }

    public int getSystemSchemaId() {
        return _systemSchemaId;
    }

    @NonNull
    public String getSystemSchemaName() {
        return _systemSchemaName;
    }

    public int getRequestId() {
        return _requestId;
    }

    @NonNull
    public String getName() {
        return _name;
    }

    public boolean isExpectingResults() {
        return _expectingResults;
    }

    public boolean haveResultsArrived() {
        return _resultsArrived;
    }

    @Override
    public void toMap(@NonNull final Map<String, Object> map) {
        map.put("type", "Connector.RequestCompleted");
        map.put("systemSchema.id", getSystemSchemaId());
        map.put("systemSchema.name", getSystemSchemaName());
        map.put("requestId", getRequestId());
        map.put("name", getName());
        map.put("expectingResults", isExpectingResults());
        map.put("resultsArrived", haveResultsArrived());
    }
}
