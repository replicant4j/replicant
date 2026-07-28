package replicant.server;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.NonNull;

/**
 * A client-visible Entity Change derived from one Entity Change Candidate after routing and filtering.
 *
 * <p>The Dataset Addresses identify the Subscriptions affected by the change. The source candidate supplies Entity
 * identity and serialized attribute values; it remains distinct from this routed Entity Change.</p>
 */
public class EntityChange {
    @NonNull
    private final String _key;

    @NonNull
    private final EntityChangeCandidate _entityChangeCandidate;

    @NonNull
    private final Set<DatasetAddress> _datasetAddresses = new LinkedHashSet<>();

    /**
     * Create an Entity Change with no target Subscription Dataset Addresses yet.
     *
     * @param entityChangeCandidate the pre-routing source candidate.
     */
    public EntityChange(@NonNull final EntityChangeCandidate entityChangeCandidate) {
        _key = entityChangeCandidate.getTypeId() + "#" + entityChangeCandidate.getId();
        _entityChangeCandidate = Objects.requireNonNull(entityChangeCandidate);
    }

    /**
     * Create an Entity Change for one target Subscription Dataset Address.
     *
     * @param entityChangeCandidate the pre-routing source candidate.
     * @param datasetAddress        the target Subscription Dataset Address.
     */
    public EntityChange(
            @NonNull final EntityChangeCandidate entityChangeCandidate, @NonNull final DatasetAddress datasetAddress) {
        this(entityChangeCandidate);
        _datasetAddresses.add(Objects.requireNonNull(datasetAddress));
    }

    /**
     * Return the internal key combining Entity Type and Entity identity for Change Set coalescing.
     *
     * @return the internal Change Set coalescing key.
     */
    @NonNull
    public String getKey() {
        return _key;
    }

    /**
     * Return the pre-routing candidate from which this Entity Change was derived.
     *
     * @return the source Entity Change Candidate.
     */
    @NonNull
    public EntityChangeCandidate getEntityChangeCandidate() {
        return _entityChangeCandidate;
    }

    /**
     * Return the Dataset Addresses of Subscriptions affected by this Entity Change.
     *
     * @return the target Subscription Dataset Addresses.
     */
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
