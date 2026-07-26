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
    private final Set<DatasetAddress> _datasetAddresses = new LinkedHashSet<>();

    public Change(@NonNull final EntityMessage entityMessage) {
        _key = entityMessage.getTypeId() + "#" + entityMessage.getId();
        _entityMessage = Objects.requireNonNull(entityMessage);
    }

    public Change(@NonNull final EntityMessage entityMessage, @NonNull final DatasetAddress datasetAddress) {
        this(entityMessage);
        _datasetAddresses.add(Objects.requireNonNull(datasetAddress));
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
    public Set<DatasetAddress> getDatasetAddresses() {
        return _datasetAddresses;
    }

    public void merge(@NonNull final Change other) {
        getEntityMessage().merge(other.getEntityMessage());
        getDatasetAddresses().addAll(other.getDatasetAddresses());
    }

    @NonNull
    public Change duplicate() {
        final var change = new Change(getEntityMessage().duplicate());
        change.getDatasetAddresses().addAll(getDatasetAddresses());
        return change;
    }
}
