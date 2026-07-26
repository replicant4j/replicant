package replicant;

import static org.realityforge.braincheck.Guards.*;

import arez.Disposable;
import arez.ObservableValue;
import arez.annotations.ArezComponent;
import arez.annotations.Feature;
import arez.annotations.Observable;
import arez.annotations.ObservableValueRef;
import arez.annotations.PreDispose;
import arez.component.ComponentObservable;
import arez.component.DisposeNotifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.spy.SubscriptionDisposedEvent;

/**
 * Representation of a Subscription to a Dataset at a Dataset Address.
 */
@ArezComponent(observable = Feature.ENABLE, requireId = Feature.ENABLE)
public abstract class Subscription extends ReplicantService implements Comparable<Subscription> {
    @NonNull
    private final Map<Class<?>, NavigableMap<Integer, ReplicaSubscriptionEntry>> _replicaEntries = new HashMap<>();

    @NonNull
    private final DatasetAddress _datasetAddress;

    @NonNull
    static Subscription create(
            @Nullable final ReplicantContext context,
            @NonNull final DatasetAddress datasetAddress,
            @Nullable final Object filter,
            final boolean explicitSubscription) {
        return new Arez_Subscription(context, datasetAddress, filter, explicitSubscription);
    }

    Subscription(@Nullable final ReplicantContext context, @NonNull final DatasetAddress datasetAddress) {
        super(context);
        _datasetAddress = Objects.requireNonNull(datasetAddress);
    }

    @NonNull
    public DatasetAddress datasetAddress() {
        return _datasetAddress;
    }

    @Observable(initializer = Feature.ENABLE)
    @Nullable
    public abstract Object getFilter();

    abstract void setFilter(@Nullable Object filter);

    @Observable(initializer = Feature.ENABLE)
    public abstract boolean isExplicitSubscription();

    public abstract void setExplicitSubscription(boolean explicitSubscription);

    @NonNull
    @Observable(expectSetter = false)
    Map<Class<?>, NavigableMap<Integer, ReplicaSubscriptionEntry>> getReplicaEntries() {
        return _replicaEntries;
    }

    /**
     * Return the Replica types present in this Subscription.
     *
     * @return the Replica types.
     */
    @NonNull
    public Collection<Class<?>> findAllReplicaTypes() {
        return getReplicaEntries().keySet();
    }

    @NonNull
    public List<ReplicaEntry> findAllReplicaEntriesByType(@NonNull final Class<?> type) {
        final Map<Integer, ReplicaSubscriptionEntry> typeMap =
                getReplicaEntries().get(type);
        return null == typeMap
                ? Collections.emptyList()
                : CollectionsUtil.asList(typeMap.values().stream().map(ReplicaSubscriptionEntry::getReplicaEntry));
    }

    @Nullable
    public ReplicaEntry findReplicaEntryByTypeAndId(@NonNull final Class<?> type, final int id) {
        final Map<Integer, ReplicaSubscriptionEntry> typeMap = _replicaEntries.get(type);
        if (null == typeMap) {
            getReplicaEntriesObservableValue().reportObserved();
            return null;
        } else {
            final ReplicaSubscriptionEntry entry = typeMap.get(id);
            if (null == entry) {
                getReplicaEntriesObservableValue().reportObserved();
                return null;
            } else {
                final ReplicaEntry replicaEntry = entry.getReplicaEntry();
                ComponentObservable.observe(replicaEntry);
                return replicaEntry;
            }
        }
    }

    /**
     * Return the instance root for this subscription.
     * This method should NOT be invoked on subscriptions for type graphs
     *
     * @return the instance root.
     */
    @NonNull
    public Object getInstanceRoot() {
        final Dataset dataset = getDataset();
        final Integer datasetRootId = _datasetAddress.datasetRootId();
        if (Replicant.shouldCheckApiInvariants()) {
            invariant(
                    dataset::isInstanceGraph,
                    () -> "Replicant-0029: Subscription.getInstanceRoot() invoked for Dataset Address "
                            + _datasetAddress + " but the Dataset is not instance based.");
            invariant(
                    () -> null != datasetRootId,
                    () -> "Replicant-0087: Subscription.getInstanceRoot() invoked for Dataset Address "
                            + _datasetAddress + " but the Dataset Address has no Dataset Root ID.");
        }
        final ReplicaEntry replicaEntry = findReplicaEntryByTypeAndId(
                Objects.requireNonNull(dataset.getInstanceType()), Objects.requireNonNull(datasetRootId));
        if (Replicant.shouldCheckApiInvariants()) {
            invariant(
                    () -> null != replicaEntry,
                    () -> "Replicant-0088: Subscription.getInstanceRoot() invoked for Dataset Address "
                            + _datasetAddress + " but Replica is not present.");
        }
        return Objects.requireNonNull(replicaEntry).getReplica();
    }

    /**
     * Return the Dataset for this Subscription.
     *
     * @return the Dataset for this Subscription.
     */
    @NonNull
    public Dataset getDataset() {
        return getReplicantContext()
                .getSchemaService()
                .getById(_datasetAddress.schemaId())
                .getDataset(_datasetAddress.datasetId());
    }

    @ObservableValueRef
    abstract ObservableValue<?> getReplicaEntriesObservableValue();

    @Override
    public int compareTo(@NonNull final Subscription o) {
        return datasetAddress().compareTo(o.datasetAddress());
    }

    void linkSubscriptionToReplicaEntry(@NonNull final ReplicaEntry replicaEntry) {
        getReplicaEntriesObservableValue().preReportChanged();
        final Class<?> type = replicaEntry.getType();
        final int id = replicaEntry.getId();
        final NavigableMap<Integer, ReplicaSubscriptionEntry> typeMap =
                _replicaEntries.computeIfAbsent(type, t -> new TreeMap<>());
        if (!typeMap.containsKey(id)) {
            createSubscriptionEntry(typeMap, replicaEntry);
        }
    }

    private void createSubscriptionEntry(
            @NonNull final Map<Integer, ReplicaSubscriptionEntry> typeMap, @NonNull final ReplicaEntry replicaEntry) {
        typeMap.put(replicaEntry.getId(), ReplicaSubscriptionEntry.create(replicaEntry));
        DisposeNotifier.asDisposeNotifier(replicaEntry)
                .addOnDisposeListener(this, () -> detachReplicaEntry(replicaEntry, false), true);
        getReplicaEntriesObservableValue().reportChanged();
    }

    void delinkReplicaEntryFromSubscription(
            @NonNull final ReplicaEntry replicaEntry, final boolean disposeIfNoSubscriptions) {
        getReplicaEntriesObservableValue().preReportChanged();
        detachReplicaEntry(replicaEntry, disposeIfNoSubscriptions);
        getReplicaEntriesObservableValue().reportChanged();
    }

    private void detachReplicaEntry(@NonNull final ReplicaEntry replicaEntry, final boolean disposeIfNoSubscriptions) {
        final Class<?> replicaType = replicaEntry.getType();
        final Map<Integer, ReplicaSubscriptionEntry> typeMap = _replicaEntries.get(replicaType);
        final DatasetAddress datasetAddress = datasetAddress();
        if (Replicant.shouldCheckInvariants()) {
            invariant(
                    () -> null != typeMap,
                    () -> "Replica type " + replicaType.getSimpleName() + " not present in Subscription at "
                            + datasetAddress);
        }
        final ReplicaSubscriptionEntry removed = Objects.requireNonNull(typeMap).remove(replicaEntry.getId());
        if (Replicant.shouldCheckInvariants()) {
            invariant(
                    () -> null != removed,
                    () -> "Replica Entry " + replicaEntry + " not present in Subscription at " + datasetAddress);
        }
        DisposeNotifier.asDisposeNotifier(replicaEntry).removeOnDisposeListener(this, true);
        Disposable.dispose(removed);
        if (disposeIfNoSubscriptions) {
            replicaEntry.disposeReplicaEntryIfNoSubscriptions();
        }
        if (typeMap.isEmpty()) {
            _replicaEntries.remove(replicaType);
        }
    }

    @PreDispose
    void preDispose() {
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext().getSpy().reportSpyEvent(new SubscriptionDisposedEvent(this));
        }
        delinkSubscriptionFromAllReplicaEntries();
    }

    private void delinkSubscriptionFromAllReplicaEntries() {
        new ArrayList<>(_replicaEntries.values())
                .stream()
                        .flatMap(replicaEntrySet -> new ArrayList<>(replicaEntrySet.values()).stream())
                        .forEachOrdered(replicaEntry ->
                                replicaEntry.getReplicaEntry().delinkSubscriptionFromReplicaEntry(this));
    }
}
