package replicant.server.transport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.json.JsonObject;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.server.DatasetAddress;

/**
 * An object defining the state of the Subscription at a particular Dataset Address and
 * all its Subscription Dependencies.
 */
final class SubscriptionEntry implements Comparable<SubscriptionEntry> {
    @NonNull
    private final ReplicantSession _session;

    @NonNull
    private final DatasetAddress _datasetAddress;
    /**
     * The target Dataset Addresses of this Subscription's outward Subscription Dependencies.
     */
    @NonNull
    private final Set<DatasetAddress> _outwardSubscriptionDependencies = new HashSet<>();

    @NonNull
    private final Set<DatasetAddress> _roOutwardSubscriptionDependencies =
            Collections.unmodifiableSet(_outwardSubscriptionDependencies);

    @NonNull
    private final Map<SubscriptionDependencyOwner, Set<DatasetAddress>> _ownedOutwardSubscriptionDependencies =
            new HashMap<>();

    @NonNull
    private final Map<DatasetAddress, Integer> _outwardSubscriptionDependencyReferenceCounts = new HashMap<>();
    /**
     * The source Dataset Addresses of this Subscription's inward Subscription Dependencies.
     */
    @NonNull
    private final Set<DatasetAddress> _inwardSubscriptionDependencies = new HashSet<>();

    @NonNull
    private final Set<DatasetAddress> _roInwardSubscriptionDependencies =
            Collections.unmodifiableSet(_inwardSubscriptionDependencies);

    private boolean _explicitlySubscribed;

    @Nullable
    private JsonObject _filterParameter;

    SubscriptionEntry(@NonNull final ReplicantSession session, @NonNull final DatasetAddress datasetAddress) {
        _session = Objects.requireNonNull(session);
        _datasetAddress = Objects.requireNonNull(datasetAddress);
    }

    @NonNull
    DatasetAddress datasetAddress() {
        return _datasetAddress;
    }

    /**
     * Return true if this Subscription can be automatically unsubscribed. This means it has not
     * been explicitly subscribed and has no inward Subscription Dependencies.
     */
    boolean canUnsubscribe() {
        return !isExplicitlySubscribed() && _inwardSubscriptionDependencies.isEmpty();
    }

    /**
     * Return true if this Subscription was explicitly requested by the client.
     */
    boolean isExplicitlySubscribed() {
        return _explicitlySubscribed;
    }

    void setExplicitlySubscribed(final boolean explicitlySubscribed) {
        _session.ensureLockedByCurrentThread();
        _explicitlySubscribed = explicitlySubscribed;
    }

    /**
     * Return the Filter Parameter applied to this Subscription, if any.
     */
    @Nullable
    JsonObject getFilterParameter() {
        return _filterParameter;
    }

    /**
     * Set the Filter Parameter.
     * User code should not invoke this unless they are implementing bulk loading and are propagating
     * Filter Parameters between multiple Datasets loaded in a single sweep.
     *
     * @param filterParameter the Filter Parameter.
     */
    void setFilterParameter(@Nullable final JsonObject filterParameter) {
        _session.ensureLockedByCurrentThread();
        _filterParameter = filterParameter;
    }

    /**
     * Return the target Dataset Addresses of this Subscription's outward Subscription Dependencies.
     */
    @NonNull
    Set<DatasetAddress> getOutwardSubscriptionDependencies() {
        return _roOutwardSubscriptionDependencies;
    }

    @NonNull
    Set<DatasetAddress> getOwnedOutwardSubscriptionDependencies(@NonNull final SubscriptionDependencyOwner owner) {
        assert null != owner;
        _session.ensureLockedByCurrentThread();
        final var datasetAddresses = _ownedOutwardSubscriptionDependencies.get(owner);
        return null == datasetAddresses ? Collections.emptySet() : Set.copyOf(datasetAddresses);
    }

    /**
     * Register outward Subscription Dependencies and return the target Dataset Addresses that were newly retained.
     */
    @NonNull
    DatasetAddress[] registerOutwardSubscriptionDependencies(
            @NonNull final SubscriptionDependencyOwner owner, @NonNull final DatasetAddress... datasetAddresses) {
        assert null != owner;
        _session.ensureLockedByCurrentThread();
        final var results = new ArrayList<DatasetAddress>(datasetAddresses.length);
        final var owned = _ownedOutwardSubscriptionDependencies.computeIfAbsent(owner, k -> new HashSet<>());
        for (final var datasetAddress : datasetAddresses) {
            if (owned.add(datasetAddress)) {
                final var referenceCount =
                        _outwardSubscriptionDependencyReferenceCounts.merge(datasetAddress, 1, Integer::sum);
                if (1 == referenceCount) {
                    _outwardSubscriptionDependencies.add(datasetAddress);
                    results.add(datasetAddress);
                }
            }
        }
        return results.toArray(new DatasetAddress[0]);
    }

    /**
     * Deregister outward Subscription Dependencies and return the target Dataset Addresses no longer retained.
     */
    @NonNull
    DatasetAddress[] deregisterOutwardSubscriptionDependencies(
            @NonNull final SubscriptionDependencyOwner owner, @NonNull final DatasetAddress... datasetAddresses) {
        assert null != owner;
        _session.ensureLockedByCurrentThread();
        final var owned = _ownedOutwardSubscriptionDependencies.get(owner);
        if (null == owned) {
            return new DatasetAddress[0];
        } else {
            final var results = new ArrayList<DatasetAddress>(datasetAddresses.length);
            for (final var datasetAddress : datasetAddresses) {
                if (owned.remove(datasetAddress)) {
                    final var existing =
                            Objects.requireNonNull(_outwardSubscriptionDependencyReferenceCounts.get(datasetAddress));
                    assert existing > 0;
                    if (1 == existing) {
                        _outwardSubscriptionDependencyReferenceCounts.remove(datasetAddress);
                        _outwardSubscriptionDependencies.remove(datasetAddress);
                        results.add(datasetAddress);
                    } else {
                        _outwardSubscriptionDependencyReferenceCounts.put(datasetAddress, existing - 1);
                    }
                }
            }
            if (owned.isEmpty()) {
                _ownedOutwardSubscriptionDependencies.remove(owner);
            }
            return results.toArray(new DatasetAddress[0]);
        }
    }

    /**
     * Deregister the target Dataset Addresses from all Subscription Dependency owners and return those no longer
     * retained.
     */
    @NonNull
    DatasetAddress[] deregisterAllOutwardSubscriptionDependencies(@NonNull final DatasetAddress... datasetAddresses) {
        _session.ensureLockedByCurrentThread();
        final var results = new ArrayList<DatasetAddress>(datasetAddresses.length);
        for (final var datasetAddress : datasetAddresses) {
            if (_outwardSubscriptionDependencies.remove(datasetAddress)) {
                _outwardSubscriptionDependencyReferenceCounts.remove(datasetAddress);
                _ownedOutwardSubscriptionDependencies.entrySet().removeIf(e -> {
                    e.getValue().remove(datasetAddress);
                    return e.getValue().isEmpty();
                });
                results.add(datasetAddress);
            }
        }
        return results.toArray(new DatasetAddress[0]);
    }

    /**
     * Return the source Dataset Addresses of this Subscription's inward Subscription Dependencies.
     */
    @NonNull
    Set<DatasetAddress> getInwardSubscriptionDependencies() {
        return _roInwardSubscriptionDependencies;
    }

    /**
     * Register inward Subscription Dependencies and return the source Dataset Addresses that were newly recorded.
     */
    @NonNull
    DatasetAddress[] registerInwardSubscriptionDependencies(@NonNull final DatasetAddress... datasetAddresses) {
        _session.ensureLockedByCurrentThread();
        final var results = new ArrayList<DatasetAddress>(datasetAddresses.length);
        for (final var datasetAddress : datasetAddresses) {
            if (!_inwardSubscriptionDependencies.contains(datasetAddress)) {
                _inwardSubscriptionDependencies.add(datasetAddress);
                results.add(datasetAddress);
            }
        }
        return results.toArray(new DatasetAddress[0]);
    }

    /**
     * Deregister inward Subscription Dependencies and return the source Dataset Addresses that were removed.
     */
    @NonNull
    DatasetAddress[] deregisterInwardSubscriptionDependencies(@NonNull final DatasetAddress... datasetAddresses) {
        _session.ensureLockedByCurrentThread();
        final var results = new ArrayList<DatasetAddress>(datasetAddresses.length);
        for (final var datasetAddress : datasetAddresses) {
            if (_inwardSubscriptionDependencies.contains(datasetAddress)) {
                _inwardSubscriptionDependencies.remove(datasetAddress);
                results.add(datasetAddress);
            }
        }
        return results.toArray(new DatasetAddress[0]);
    }

    @Override
    public int compareTo(@NonNull final SubscriptionEntry o) {
        return datasetAddress().compareTo(o.datasetAddress());
    }
}
