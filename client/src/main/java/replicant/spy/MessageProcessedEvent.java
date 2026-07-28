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
    private final MessageProcessingSummary _messageProcessingSummary;

    public MessageProcessedEvent(
            final int systemSchemaId,
            @NonNull final String systemSchemaName,
            @NonNull final MessageProcessingSummary messageProcessingSummary) {
        _systemSchemaId = systemSchemaId;
        _systemSchemaName = Objects.requireNonNull(systemSchemaName);
        _messageProcessingSummary = Objects.requireNonNull(messageProcessingSummary);
    }

    public int getSystemSchemaId() {
        return _systemSchemaId;
    }

    @NonNull
    public String getSystemSchemaName() {
        return _systemSchemaName;
    }

    @NonNull
    public MessageProcessingSummary getMessageProcessingSummary() {
        return _messageProcessingSummary;
    }

    @Override
    public void toMap(@NonNull final Map<String, Object> map) {
        map.put("type", "Connector.MessageProcess");
        map.put("systemSchema.id", getSystemSchemaId());
        map.put("systemSchema.name", getSystemSchemaName());
        final MessageProcessingSummary summary = getMessageProcessingSummary();
        map.put("requestId", summary.getRequestId());
        map.put("subscriptionSubscribeCount", summary.getSubscriptionSubscribeCount());
        map.put("subscriptionUnsubscribeCount", summary.getSubscriptionUnsubscribeCount());
        map.put("subscriptionUpdateCount", summary.getSubscriptionUpdateCount());
        map.put("entityUpdateCount", summary.getEntityUpdateCount());
        map.put("entityRemoveCount", summary.getEntityRemoveCount());
        map.put("entityLinkCount", summary.getEntityLinkCount());
    }
}
