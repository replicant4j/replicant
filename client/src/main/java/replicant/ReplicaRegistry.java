package replicant;

import static org.realityforge.braincheck.Guards.*;

import arez.Disposable;
import arez.ObservableValue;
import arez.annotations.ArezComponent;
import arez.annotations.Feature;
import arez.annotations.Observable;
import arez.annotations.ObservableValueRef;
import arez.component.ComponentObservable;
import arez.component.DisposeNotifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The registry of Replica Entry instances within the Replicant system.
 */
@ArezComponent(disposeNotifier = Feature.DISABLE, requireId = Feature.DISABLE)
abstract class ReplicaRegistry extends ReplicantService {
    // Replica Entry map: Type => ID
    private final Map<Class<?>, Map<Integer, ReplicaEntry>> _replicaEntries = new HashMap<>();

    @NonNull
    static ReplicaRegistry create(@Nullable final ReplicantContext context) {
        return new Arez_ReplicaRegistry(context);
    }

    ReplicaRegistry(@Nullable final ReplicantContext context) {
        super(context);
    }

    @ObservableValueRef
    abstract ObservableValue<?> getReplicaEntriesObservableValue();

    @Observable(expectSetter = false)
    Map<Class<?>, Map<Integer, ReplicaEntry>> getReplicaEntries() {
        return _replicaEntries;
    }

    /**
     * Return the collection of Replica types that exist in the system.
     * Only Replica types that have at least one instance will be returned from this method unless
     * a Replica Entry has been disposed and the scheduler is yet to invoke code to remove the type from the set.
     * This is unlikely to be exposed to normal user code.
     *
     * @return the collection of entity types.
     */
    @NonNull
    Collection<Class<?>> findAllReplicaTypes() {
        return getReplicaEntries().keySet();
    }

    /**
     * Find the ReplicaEntry by type and id.
     *
     * @param type the type of the Replica.
     * @param id   the Entity identifier.
     * @return the ReplicaEntry if it exists, null otherwise.
     */
    @Nullable
    ReplicaEntry findReplicaEntryByTypeAndId(@NonNull final Class<?> type, final int id) {
        final Map<Integer, ReplicaEntry> typeMap = _replicaEntries.get(type);
        if (null == typeMap) {
            getReplicaEntriesObservableValue().reportObserved();
            return null;
        } else {
            final ReplicaEntry replicaEntry = typeMap.get(id);
            if (null == replicaEntry) {
                getReplicaEntriesObservableValue().reportObserved();
                return null;
            } else {
                ComponentObservable.observe(replicaEntry);
                return replicaEntry;
            }
        }
    }

    @NonNull
    List<ReplicaEntry> findAllReplicaEntriesByType(@NonNull final Class<?> type) {
        final Map<Integer, ReplicaEntry> typeMap = getReplicaEntries().get(type);
        return null == typeMap ? Collections.emptyList() : CollectionsUtil.asList(typeMap.values().stream());
    }

    /**
     * Remove the Replica Entry and unlink it from associated subscriptions.
     *
     * @param replicaEntry the Replica Entry.
     */
    void unlinkReplicaEntry(@NonNull final ReplicaEntry replicaEntry) {
        getReplicaEntriesObservableValue().preReportChanged();

        final Class<?> replicaType = replicaEntry.getType();
        final int id = replicaEntry.getId();
        final Map<Integer, ReplicaEntry> typeMap = _replicaEntries.get(replicaType);
        if (Replicant.shouldCheckInvariants()) {
            invariant(
                    () -> null != typeMap,
                    () -> "Replica type " + replicaType.getSimpleName() + " not present in ReplicaRegistry");
        }
        final ReplicaEntry removed = Objects.requireNonNull(typeMap).remove(id);
        if (Replicant.shouldCheckInvariants()) {
            invariant(() -> null != removed, () -> "Replica Entry " + replicaEntry + " not present in ReplicaRegistry");
        }
        detachReplicaEntry(replicaEntry);
        Disposable.dispose(removed);
        if (typeMap.isEmpty()) {
            _replicaEntries.remove(replicaType);
        }
        getReplicaEntriesObservableValue().reportChanged();
    }

    /**
     * Return the Replica Entry specified by type and id, creating one if it does not already exist.
     *
     * @param name the name of the Replica Entry if any. Must be null unless {@link Replicant#areNamesEnabled()} returns
     *             true.
     * @param type the type of the Replica.
     * @param id   the Entity identifier.
     * @return the existing Replica Entry if it exists, otherwise the newly created entry.
     */
    @NonNull
    ReplicaEntry findOrCreateReplicaEntry(@Nullable final String name, @NonNull final Class<?> type, final int id) {
        final Map<Integer, ReplicaEntry> typeMap = _replicaEntries.get(type);
        if (null == typeMap) {
            final HashMap<Integer, ReplicaEntry> newTypeMap = new HashMap<>();
            _replicaEntries.put(type, newTypeMap);
            return createReplicaEntry(newTypeMap, name, type, id);
        } else {
            final ReplicaEntry replicaEntry = typeMap.get(id);
            if (null == replicaEntry) {
                return createReplicaEntry(typeMap, name, type, id);
            } else {
                ComponentObservable.observe(replicaEntry);
                return replicaEntry;
            }
        }
    }

    @NonNull
    private ReplicaEntry createReplicaEntry(
            @NonNull final Map<Integer, ReplicaEntry> typeMap,
            @Nullable final String name,
            @NonNull final Class<?> type,
            final int id) {
        getReplicaEntriesObservableValue().preReportChanged();
        final ReplicaEntry replicaEntry =
                ReplicaEntry.create(Replicant.areZonesEnabled() ? getReplicantContext() : null, name, type, id);
        DisposeNotifier.asDisposeNotifier(replicaEntry).addOnDisposeListener(this, () -> destroy(replicaEntry), true);
        typeMap.put(id, replicaEntry);
        getReplicaEntriesObservableValue().reportChanged();
        ComponentObservable.observe(replicaEntry);
        return replicaEntry;
    }

    private void destroy(@NonNull final ReplicaEntry replicaEntry) {
        unlinkReplicaEntry(replicaEntry);
    }

    private void detachReplicaEntry(@NonNull final ReplicaEntry replicaEntry) {
        DisposeNotifier.asDisposeNotifier(replicaEntry).removeOnDisposeListener(this, true);
    }
}
