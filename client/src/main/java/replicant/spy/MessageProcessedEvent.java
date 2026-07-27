package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Notification when a Connector processed a message from a DataSource.
 */
public final class MessageProcessedEvent implements SerializableEvent {
    private final int _systemSchemaId;

    @NonNull
    private final String _systemSchemaName;

    @NonNull
    private final DataLoadStatus _dataLoadStatus;

    public MessageProcessedEvent(
            final int systemSchemaId,
            @NonNull final String systemSchemaName,
            @NonNull final DataLoadStatus dataLoadStatus) {
        _systemSchemaId = systemSchemaId;
        _systemSchemaName = Objects.requireNonNull(systemSchemaName);
        _dataLoadStatus = Objects.requireNonNull(dataLoadStatus);
    }

    public int getSystemSchemaId() {
        return _systemSchemaId;
    }

    @NonNull
    public String getSystemSchemaName() {
        return _systemSchemaName;
    }

    @NonNull
    public DataLoadStatus getDataLoadStatus() {
        return _dataLoadStatus;
    }

    @Override
    public void toMap(@NonNull final Map<String, Object> map) {
        map.put("type", "Connector.MessageProcess");
        map.put("systemSchema.id", getSystemSchemaId());
        map.put("systemSchema.name", getSystemSchemaName());
        final DataLoadStatus status = getDataLoadStatus();
        map.put("requestId", status.getRequestId());
        map.put("subscriptionSubscribeCount", status.getSubscriptionSubscribeCount());
        map.put("subscriptionUnsubscribeCount", status.getSubscriptionUnsubscribeCount());
        map.put("subscriptionUpdateCount", status.getSubscriptionUpdateCount());
        map.put("entityUpdateCount", status.getEntityUpdateCount());
        map.put("entityRemoveCount", status.getEntityRemoveCount());
        map.put("entityLinkCount", status.getEntityLinkCount());
    }
}
