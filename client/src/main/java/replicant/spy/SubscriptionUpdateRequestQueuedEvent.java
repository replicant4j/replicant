package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.ChannelAddress;

/**
 * Notification when a Subscription update is requested.
 */
public final class SubscriptionUpdateRequestQueuedEvent implements SerializableEvent {
    @NonNull
    private final ChannelAddress _address;

    @Nullable
    private final Object _filter;

    public SubscriptionUpdateRequestQueuedEvent(@NonNull final ChannelAddress address, @Nullable final Object filter) {
        _address = Objects.requireNonNull(address);
        _filter = filter;
    }

    @NonNull
    public ChannelAddress getAddress() {
        return _address;
    }

    @Nullable
    public Object getFilter() {
        return _filter;
    }

    @Override
    public void toMap(@NonNull final Map<String, Object> map) {
        map.put("type", "Connector.SubscriptionUpdateRequestQueued");
        final ChannelAddress address = getAddress();
        map.put("channel.schemaId", address.schemaId());
        map.put("channel.channelId", address.channelId());
        map.put("channel.rootId", address.rootId());
        map.put("channel.filter", getFilter());
    }
}
