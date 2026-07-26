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
 * all the dependency relationships to other graphs.
 */
final class SubscriptionEntry implements Comparable<SubscriptionEntry> {
    @NonNull
    private final ReplicantSession _session;

    @NonNull
    private final DatasetAddress _datasetAddress;
    /**
     * This is the set of Dataset Addresses subscribed to through this Subscription.
     */
    @NonNull
    private final Set<DatasetAddress> _outwardSubscriptions = new HashSet<>();

    @NonNull
    private final Set<DatasetAddress> _roOutwardSubscriptions = Collections.unmodifiableSet(_outwardSubscriptions);

    @NonNull
    private final Map<LinkOwner, Set<DatasetAddress>> _ownedOutwardSubscriptions = new HashMap<>();

    @NonNull
    private final Map<DatasetAddress, Integer> _outwardSubscriptionReferenceCounts = new HashMap<>();
    /**
     * This is the set of Dataset Addresses whose Subscriptions depend on this Subscription.
     */
    @NonNull
    private final Set<DatasetAddress> _inwardSubscriptions = new HashSet<>();

    @NonNull
    private final Set<DatasetAddress> _roInwardSubscriptions = Collections.unmodifiableSet(_inwardSubscriptions);

    private boolean _explicitlySubscribed;

    @Nullable
    private JsonObject _filter;

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
     * been explicitly subscribed and has no incoming subscriptions.
     */
    boolean canUnsubscribe() {
        return !isExplicitlySubscribed() && _inwardSubscriptions.isEmpty();
    }

    /**
     * Return true if this Dataset Address has been explicitly subscribed to from the client,
     * false the subscription occurred due to a graph link.
     */
    boolean isExplicitlySubscribed() {
        return _explicitlySubscribed;
    }

    void setExplicitlySubscribed(final boolean explicitlySubscribed) {
        _session.ensureLockedByCurrentThread();
        _explicitlySubscribed = explicitlySubscribed;
    }

    /**
     * Return the filter that was applied to this Subscription. A particular Dataset Address
     * may or may not have a filter.
     */
    @Nullable
    JsonObject getFilter() {
        return _filter;
    }

    /**
     * Set the filter.
     * User code should not invoke this unless they are implementing bulk loading and are propagating
     * filters between multiple graphs loaded in a single sweep.
     *
     * @param filter the filter.
     */
    void setFilter(@Nullable final JsonObject filter) {
        _session.ensureLockedByCurrentThread();
        _filter = filter;
    }

    /**
     * Return the Dataset Addresses that were subscribed as a result of subscribing to this Dataset Address.
     */
    @NonNull
    Set<DatasetAddress> getOutwardSubscriptions() {
        return _roOutwardSubscriptions;
    }

    @NonNull
    Set<DatasetAddress> getOwnedOutwardSubscriptions(@NonNull final LinkOwner owner) {
        assert null != owner;
        _session.ensureLockedByCurrentThread();
        final var datasetAddresses = _ownedOutwardSubscriptions.get(owner);
        return null == datasetAddresses ? Collections.emptySet() : Set.copyOf(datasetAddresses);
    }

    /**
     * Register the specified Dataset Address as outward links. Returns the set of links that were actually added.
     */
    @NonNull
    DatasetAddress[] registerOutwardSubscriptions(
            @NonNull final LinkOwner owner, @NonNull final DatasetAddress... datasetAddresses) {
        assert null != owner;
        _session.ensureLockedByCurrentThread();
        final var results = new ArrayList<DatasetAddress>(datasetAddresses.length);
        final var owned = _ownedOutwardSubscriptions.computeIfAbsent(owner, k -> new HashSet<>());
        for (final var datasetAddress : datasetAddresses) {
            if (owned.add(datasetAddress)) {
                final var referenceCount = _outwardSubscriptionReferenceCounts.merge(datasetAddress, 1, Integer::sum);
                if (1 == referenceCount) {
                    _outwardSubscriptions.add(datasetAddress);
                    results.add(datasetAddress);
                }
            }
        }
        return results.toArray(new DatasetAddress[0]);
    }

    /**
     * Deregister the specified Dataset Addresses as outward links. Returns the set of links that were actually deregistered.
     */
    @NonNull
    DatasetAddress[] deregisterOutwardSubscriptions(
            @NonNull final LinkOwner owner, @NonNull final DatasetAddress... datasetAddresses) {
        assert null != owner;
        _session.ensureLockedByCurrentThread();
        final var owned = _ownedOutwardSubscriptions.get(owner);
        if (null == owned) {
            return new DatasetAddress[0];
        } else {
            final var results = new ArrayList<DatasetAddress>(datasetAddresses.length);
            for (final var datasetAddress : datasetAddresses) {
                if (owned.remove(datasetAddress)) {
                    final var existing =
                            Objects.requireNonNull(_outwardSubscriptionReferenceCounts.get(datasetAddress));
                    assert existing > 0;
                    if (1 == existing) {
                        _outwardSubscriptionReferenceCounts.remove(datasetAddress);
                        _outwardSubscriptions.remove(datasetAddress);
                        results.add(datasetAddress);
                    } else {
                        _outwardSubscriptionReferenceCounts.put(datasetAddress, existing - 1);
                    }
                }
            }
            if (owned.isEmpty()) {
                _ownedOutwardSubscriptions.remove(owner);
            }
            return results.toArray(new DatasetAddress[0]);
        }
    }

    /**
     * Deregister the specified Dataset Addresses from all graph-link owners. Returns the set of links that were actually deregistered.
     */
    @NonNull
    DatasetAddress[] deregisterAllOutwardSubscriptions(@NonNull final DatasetAddress... datasetAddresses) {
        _session.ensureLockedByCurrentThread();
        final var results = new ArrayList<DatasetAddress>(datasetAddresses.length);
        for (final var datasetAddress : datasetAddresses) {
            if (_outwardSubscriptions.remove(datasetAddress)) {
                _outwardSubscriptionReferenceCounts.remove(datasetAddress);
                _ownedOutwardSubscriptions.entrySet().removeIf(e -> {
                    e.getValue().remove(datasetAddress);
                    return e.getValue().isEmpty();
                });
                results.add(datasetAddress);
            }
        }
        return results.toArray(new DatasetAddress[0]);
    }

    /**
     * Return the Dataset Addresses that were auto-subscribed to the current Dataset Address.
     */
    @NonNull
    Set<DatasetAddress> getInwardSubscriptions() {
        return _roInwardSubscriptions;
    }

    /**
     * Register the specified Dataset Address as inward links. Returns the set of links that were actually added.
     */
    @NonNull
    DatasetAddress[] registerInwardSubscriptions(@NonNull final DatasetAddress... datasetAddresses) {
        _session.ensureLockedByCurrentThread();
        final var results = new ArrayList<DatasetAddress>(datasetAddresses.length);
        for (final var datasetAddress : datasetAddresses) {
            if (!_inwardSubscriptions.contains(datasetAddress)) {
                _inwardSubscriptions.add(datasetAddress);
                results.add(datasetAddress);
            }
        }
        return results.toArray(new DatasetAddress[0]);
    }

    /**
     * Deregister the specified Dataset Addresses as outward links. Returns the set of links that were actually deregistered.
     */
    @NonNull
    DatasetAddress[] deregisterInwardSubscriptions(@NonNull final DatasetAddress... datasetAddresses) {
        _session.ensureLockedByCurrentThread();
        final var results = new ArrayList<DatasetAddress>(datasetAddresses.length);
        for (final var datasetAddress : datasetAddresses) {
            if (_inwardSubscriptions.contains(datasetAddress)) {
                _inwardSubscriptions.remove(datasetAddress);
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
