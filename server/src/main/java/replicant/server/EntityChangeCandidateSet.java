package replicant.server;

import java.util.Collection;
import java.util.LinkedHashMap;
import org.jspecify.annotations.NonNull;

public final class EntityChangeCandidateSet {
    @NonNull
    private final LinkedHashMap<String, EntityChangeCandidate> _candidates = new LinkedHashMap<>();

    public boolean containsEntityChangeCandidate(final int entityTypeId, final int entityId) {
        return _candidates.containsKey(toKey(entityTypeId, entityId));
    }

    public void mergeAll(@NonNull final Collection<EntityChangeCandidate> candidates) {
        mergeAll(candidates, false);
    }

    public void mergeAll(@NonNull final Collection<EntityChangeCandidate> candidates, final boolean copyOnMerge) {
        for (final var candidate : candidates) {
            merge(candidate, copyOnMerge);
        }
    }

    public void merge(@NonNull final EntityChangeCandidate candidate) {
        merge(candidate, false);
    }

    public void merge(@NonNull final EntityChangeCandidate candidate, final boolean copyOnMerge) {
        final var key = toKey(candidate.getEntityTypeId(), candidate.getEntityId());
        final var existing = _candidates.get(key);
        if (null != existing) {
            existing.merge(candidate);
        } else {
            _candidates.put(key, copyOnMerge ? candidate.duplicate() : candidate);
        }
    }

    @NonNull
    public Collection<EntityChangeCandidate> getEntityChangeCandidates() {
        return _candidates.values();
    }

    @NonNull
    private String toKey(final int entityTypeId, final int entityId) {
        return entityTypeId + "#" + entityId;
    }
}
