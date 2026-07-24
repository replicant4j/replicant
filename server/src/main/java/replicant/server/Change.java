package replicant.server;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.NonNull;

public class Change {
    @NonNull
    private final String _key;

    @NonNull
    private final EntityMessage _entityMessage;

    @NonNull
    private final Set<ChannelAddress> _channels = new LinkedHashSet<>();

    public Change(@NonNull final EntityMessage entityMessage) {
        _key = entityMessage.getTypeId() + "#" + entityMessage.getId();
        _entityMessage = Objects.requireNonNull(entityMessage);
    }

    public Change(@NonNull final EntityMessage entityMessage, @NonNull final ChannelAddress address) {
        this(entityMessage);
        _channels.add(Objects.requireNonNull(address));
    }

    @NonNull
    public String getKey() {
        return _key;
    }

    @NonNull
    public EntityMessage getEntityMessage() {
        return _entityMessage;
    }

    @NonNull
    public Set<ChannelAddress> getChannels() {
        return _channels;
    }

    public void merge(@NonNull final Change other) {
        getEntityMessage().merge(other.getEntityMessage());
        getChannels().addAll(other.getChannels());
    }

    @NonNull
    public Change duplicate() {
        final var change = new Change(getEntityMessage().duplicate());
        change.getChannels().addAll(getChannels());
        return change;
    }
}
