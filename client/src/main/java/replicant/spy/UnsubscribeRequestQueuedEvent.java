package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import replicant.ChannelAddress;

/**
 * Notification when a Subscription removal is requested.
 */
public final class UnsubscribeRequestQueuedEvent implements SerializableEvent {
    @NonNull
    private final ChannelAddress _address;

    public UnsubscribeRequestQueuedEvent(@NonNull final ChannelAddress address) {
        _address = Objects.requireNonNull(address);
    }

    @NonNull
    public ChannelAddress getAddress() {
        return _address;
    }

    @Override
    public void toMap(@NonNull final Map<String, Object> map) {
        map.put("type", "Connector.UnsubscribeRequestQueued");
        final ChannelAddress address = getAddress();
        map.put("channel.schemaId", address.schemaId());
        map.put("channel.channelId", address.channelId());
        map.put("channel.rootId", address.rootId());
    }
}
