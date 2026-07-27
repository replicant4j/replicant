package replicant.server;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.NonNull;

public class EntityChange {
    @NonNull
    private final String _key;

    @NonNull
    private final EntityChangeCandidate _entityChangeCandidate;

    @NonNull
    private final Set<DatasetAddress> _datasetAddresses = new LinkedHashSet<>();

    public EntityChange(@NonNull final EntityChangeCandidate entityChangeCandidate) {
        _key = entityChangeCandidate.getTypeId() + "#" + entityChangeCandidate.getId();
        _entityChangeCandidate = Objects.requireNonNull(entityChangeCandidate);
    }

    public EntityChange(
            @NonNull final EntityChangeCandidate entityChangeCandidate, @NonNull final DatasetAddress datasetAddress) {
        this(entityChangeCandidate);
        _datasetAddresses.add(Objects.requireNonNull(datasetAddress));
    }

    @NonNull
    public String getKey() {
        return _key;
    }

    @NonNull
    public EntityChangeCandidate getEntityChangeCandidate() {
        return _entityChangeCandidate;
    }

    @NonNull
    public Set<DatasetAddress> getDatasetAddresses() {
        return _datasetAddresses;
    }

    public void merge(@NonNull final EntityChange other) {
        getEntityChangeCandidate().merge(other.getEntityChangeCandidate());
        getDatasetAddresses().addAll(other.getDatasetAddresses());
    }

    @NonNull
    public EntityChange duplicate() {
        final var change = new EntityChange(getEntityChangeCandidate().duplicate());
        change.getDatasetAddresses().addAll(getDatasetAddresses());
        return change;
    }
}
