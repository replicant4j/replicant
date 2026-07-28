package replicant.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * Provides ordering of Entity Change Candidates so as to ensure their correct processing when retrieved from the database
 * Order changes as they should be processed, so a to make subsequent retrieval and application easier.
 * Ordering is:
 * - Deletions, then updates
 * - Within deletions, order by type, desc, so as to delete in the reverse order from the architecture.rb.
 * - Within updates, order by type, so as to create in the order from the architecture.rb.
 * - Within identical types, order by change time, desc on delete, asc on update.
 */
public final class EntityChangeCandidateSorter implements Comparator<EntityChangeCandidate> {
    private EntityChangeCandidateSorter() {}

    @NonNull
    public static List<EntityChangeCandidate> sort(@NonNull final Collection<EntityChangeCandidate> candidates) {
        final var sortedCandidates = new ArrayList<>(candidates);
        sortedCandidates.sort(COMPARATOR);
        return sortedCandidates;
    }

    public static final EntityChangeCandidateSorter COMPARATOR = new EntityChangeCandidateSorter();

    @Override
    public int compare(@NonNull final EntityChangeCandidate o1, @NonNull final EntityChangeCandidate o2) {
        if (o1.isDelete()) {
            if (o2.isUpdate()) {
                return -1;
            } else {
                final var typeComparison = o2.getEntityTypeId() - o1.getEntityTypeId();
                return 0 != typeComparison ? typeComparison : Long.compare(o2.getTimestamp(), o1.getTimestamp());
            }
        } else if (o2.isDelete()) {
            return 1;
        } else {
            final var typeComparison = o1.getEntityTypeId() - o2.getEntityTypeId();
            return 0 != typeComparison ? typeComparison : Long.compare(o1.getTimestamp(), o2.getTimestamp());
        }
    }
}
