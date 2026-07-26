package replicant;

import static org.realityforge.braincheck.Guards.*;

import arez.ArezContext;
import arez.Disposable;
import arez.ObservableValue;
import arez.annotations.ArezComponent;
import arez.annotations.ContextRef;
import arez.annotations.Feature;
import arez.annotations.Observable;
import arez.annotations.ObservableValueRef;
import arez.annotations.PreDispose;
import arez.component.ComponentObservable;
import arez.component.DisposeNotifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.spy.SubscriptionCreatedEvent;

/**
 * A class that records the subscriptions within the system.
 */
@ArezComponent(disposeNotifier = Feature.DISABLE, requireId = Feature.DISABLE)
abstract class SubscriptionService extends ReplicantService {
    // SystemId -> DatasetId -> DatasetRootId -> DatasetKey => Entry
    @NonNull
    private final Map<Integer, Map<Integer, Map<Integer, Map<String, Subscription>>>> _instanceSubscriptions =
            new HashMap<>();
    // SystemId -> DatasetId -> DatasetKey => Entry
    @NonNull
    private final Map<Integer, Map<Integer, Map<String, Subscription>>> _typeSubscriptions = new HashMap<>();

    @NonNull
    static SubscriptionService create(@Nullable final ReplicantContext context) {
        return new Arez_SubscriptionService(context);
    }

    SubscriptionService(@Nullable final ReplicantContext context) {
        super(context);
    }

    /**
     * Return the collection of type subscriptions.
     *
     * @return the collection of type subscriptions.
     */
    @NonNull
    @Observable(expectSetter = false)
    List<Subscription> getTypeSubscriptions() {
        return _typeSubscriptions.values().stream()
                .flatMap(s -> s.values().stream())
                .flatMap(s -> s.values().stream())
                .collect(Collectors.toList());
    }

    @ObservableValueRef
    abstract ObservableValue<?> getTypeSubscriptionsObservableValue();

    /**
     * Return the collection of instance subscriptions.
     *
     * @return the collection of instance subscriptions.
     */
    @NonNull
    @Observable(expectSetter = false)
    Collection<Subscription> getInstanceSubscriptions() {
        return _instanceSubscriptions.values().stream()
                .flatMap(s -> s.values().stream())
                .flatMap(s -> s.values().stream())
                .flatMap(s -> s.values().stream())
                .collect(Collectors.toList());
    }

    @ObservableValueRef
    abstract ObservableValue<?> getInstanceSubscriptionsObservableValue();

    /**
     * Return the collection of instance subscriptions for a Dataset.
     *
     * @param schemaId  the schema id.
     * @param datasetId the Dataset ID.
     * @return the set of Dataset Root IDs for all instance subscriptions with the specified Dataset ID.
     */
    @NonNull
    Set<Integer> getInstanceSubscriptionIds(final int schemaId, final int datasetId) {
        getInstanceSubscriptionsObservableValue().reportObserved();
        final Map<Integer, Map<Integer, Map<String, Subscription>>> datasetMaps = _instanceSubscriptions.get(schemaId);
        final Map<Integer, Map<String, Subscription>> datasetRootMap =
                null == datasetMaps ? null : datasetMaps.get(datasetId);
        if (null == datasetRootMap) {
            return Collections.emptySet();
        } else {
            return CollectionsUtil.wrap(new HashSet<>(datasetRootMap.keySet()));
        }
    }

    /**
     * Create a subscription.
     * This method should not be invoked if a subscription with the existing name already exists.
     *
     * @param datasetAddress the Dataset Address
     * @param filter the filter if subscription is filterable.
     * @param explicitSubscription if subscription was explicitly requested by the client.
     * @return the subscription.
     */
    @NonNull
    Subscription createSubscription(
            @NonNull final DatasetAddress datasetAddress,
            @Nullable final Object filter,
            final boolean explicitSubscription) {
        if (Replicant.shouldCheckApiInvariants()) {
            apiInvariant(
                    () -> null == findSubscription(datasetAddress),
                    () -> "Replicant-0064: createSubscription invoked with Dataset Address " + datasetAddress
                            + " but a subscription with that Dataset Address already exists.");
        }
        final Integer datasetRootId = datasetAddress.datasetRootId();
        final String datasetKey = datasetAddress.datasetKey();
        if (null == datasetRootId) {
            getTypeSubscriptionsObservableValue().preReportChanged();
        } else {
            getInstanceSubscriptionsObservableValue().preReportChanged();
        }
        final Subscription subscription = Subscription.create(
                Replicant.areZonesEnabled() ? getReplicantContext() : null,
                datasetAddress,
                filter,
                explicitSubscription);
        DisposeNotifier.asDisposeNotifier(subscription).addOnDisposeListener(this, () -> destroy(subscription), true);
        if (null == datasetRootId) {
            _typeSubscriptions
                    .computeIfAbsent(datasetAddress.schemaId(), key -> new HashMap<>())
                    .computeIfAbsent(datasetAddress.datasetId(), key -> new HashMap<>())
                    .put(datasetKey, subscription);
            getTypeSubscriptionsObservableValue().reportChanged();
        } else {
            _instanceSubscriptions
                    .computeIfAbsent(datasetAddress.schemaId(), key -> new HashMap<>())
                    .computeIfAbsent(datasetAddress.datasetId(), key -> new HashMap<>())
                    .computeIfAbsent(datasetRootId, key -> new HashMap<>())
                    .put(datasetKey, subscription);
            getInstanceSubscriptionsObservableValue().reportChanged();
        }
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext().getSpy().reportSpyEvent(new SubscriptionCreatedEvent(subscription));
        }
        return subscription;
    }

    private void destroy(@NonNull final Subscription subscription) {
        detachSubscription(subscription);
        unlinkSubscription(subscription.datasetAddress());
    }

    private void detachSubscription(@NonNull final Subscription subscription) {
        DisposeNotifier.asDisposeNotifier(subscription).removeOnDisposeListener(this, true);
    }

    /**
     * Return the subscription for the specified Dataset Address.
     * This method will observe the <code>typeSubscriptions</code> or <code>instanceSubscriptions</code>
     * property if not found and the result {@link Subscription} if found. This ensures that if an observer
     * invokes this method then the observer will be rescheduled when the result changes.
     *
     * @param datasetAddress the Dataset Address
     * @return the subscription if it exists, null otherwise.
     */
    @Nullable
    Subscription findSubscription(@NonNull final DatasetAddress datasetAddress) {
        final int schemaId = datasetAddress.schemaId();
        final int datasetId = datasetAddress.datasetId();
        final Integer datasetRootId = datasetAddress.datasetRootId();
        final String datasetKey = datasetAddress.datasetKey();
        return null == datasetRootId
                ? findTypeSubscription(schemaId, datasetId, datasetKey)
                : findInstanceSubscription(schemaId, datasetId, datasetRootId, datasetKey);
    }

    /**
     * Return the type subscription for the specified Dataset.
     * This method will observe the <code>typeSubscriptions</code> property if not
     * found and the result {@link Subscription} if found. This ensures that if an observer
     * invokes this method then the observer will be rescheduled when the result changes.
     *
     * @param schemaId  the schema id.
     * @param datasetId the Dataset ID.
     * @return the subscription if any matches.
     */
    @Nullable
    private Subscription findTypeSubscription(
            final int schemaId, final int datasetId, @Nullable final String datasetKey) {
        final Map<Integer, Map<String, Subscription>> datasetMap = _typeSubscriptions.get(schemaId);
        final Map<String, Subscription> datasetKeyMap = null == datasetMap ? null : datasetMap.get(datasetId);
        final Subscription subscription = null == datasetKeyMap ? null : datasetKeyMap.get(datasetKey);
        if (null == subscription) {
            getTypeSubscriptionsObservableValue().reportObservedIfTrackingTransactionActive();
            return null;
        } else {
            if (context().isTrackingTransactionActive()) {
                ComponentObservable.observe(subscription);
            }
            return subscription;
        }
    }

    /**
     * Return the instance subscription for the specified Dataset and Dataset Root ID.
     * This method will observe the <code>instanceSubscriptions</code> property if not
     * found and the result {@link Subscription} if found. This ensures that if an observer
     * invokes this method then the observer will be rescheduled when the result changes.
     *
     * @param schemaId  the schema id.
     * @param datasetId     the Dataset ID.
     * @param datasetRootId the Dataset Root ID.
     * @return the subscription if any matches.
     */
    @Nullable
    private Subscription findInstanceSubscription(
            final int schemaId, final int datasetId, final int datasetRootId, @Nullable final String datasetKey) {
        final Map<Integer, Map<Integer, Map<String, Subscription>>> datasetMap = _instanceSubscriptions.get(schemaId);
        final Map<Integer, Map<String, Subscription>> datasetRootMap =
                null == datasetMap ? null : datasetMap.get(datasetId);
        final Map<String, Subscription> datasetKeyMap =
                null == datasetRootMap ? null : datasetRootMap.get(datasetRootId);
        final Subscription subscription = null == datasetKeyMap ? null : datasetKeyMap.get(datasetKey);
        if (null == subscription || Disposable.isDisposed(subscription)) {
            getInstanceSubscriptionsObservableValue().reportObservedIfTrackingTransactionActive();
            return null;
        } else {
            if (context().isTrackingTransactionActive()) {
                ComponentObservable.observe(subscription);
            }
            return subscription;
        }
    }

    /**
     * Remove the subscription at the specified Dataset Address.
     * This method should only be invoked if a subscription exists
     *
     * @param datasetAddress the Dataset Address
     * @return the subscription.
     */
    @NonNull
    Subscription unlinkSubscription(@NonNull final DatasetAddress datasetAddress) {
        final int schemaId = datasetAddress.schemaId();
        final int datasetId = datasetAddress.datasetId();
        final Integer datasetRootId = datasetAddress.datasetRootId();
        final String datasetKey = datasetAddress.datasetKey();
        if (null == datasetRootId) {
            getTypeSubscriptionsObservableValue().preReportChanged();
            final Map<Integer, Map<String, Subscription>> datasetMap = _typeSubscriptions.get(schemaId);
            final Map<String, Subscription> datasetKeyMap = null == datasetMap ? null : datasetMap.get(datasetId);
            final Subscription subscription = null == datasetKeyMap ? null : datasetKeyMap.remove(datasetKey);
            if (null != datasetKeyMap && datasetKeyMap.isEmpty()) {
                Objects.requireNonNull(datasetMap).remove(datasetId);
            }
            if (null != subscription && Objects.requireNonNull(datasetMap).isEmpty()) {
                _typeSubscriptions.remove(schemaId);
            }
            if (Replicant.shouldCheckInvariants()) {
                invariant(
                        () -> null != subscription,
                        () -> "Replicant-0062: unlinkSubscription invoked with Dataset Address " + datasetAddress
                                + " but no subscription with that Dataset Address exists.");
                assert null != subscription;
                invariant(
                        () -> Disposable.isDisposed(subscription),
                        () -> "Replicant-0063: unlinkSubscription invoked with Dataset Address " + datasetAddress
                                + " but subscription has not already been disposed.");
            }
            getTypeSubscriptionsObservableValue().reportChanged();
            return Objects.requireNonNull(subscription);
        } else {
            getInstanceSubscriptionsObservableValue().preReportChanged();
            final Map<Integer, Map<Integer, Map<String, Subscription>>> datasetMap =
                    _instanceSubscriptions.get(schemaId);
            final Map<Integer, Map<String, Subscription>> datasetRootMap =
                    null == datasetMap ? null : datasetMap.get(datasetId);
            final Map<String, Subscription> datasetKeyMap =
                    null == datasetRootMap ? null : datasetRootMap.get(datasetRootId);
            final Subscription subscription = null == datasetKeyMap ? null : datasetKeyMap.remove(datasetKey);
            if (null != datasetKeyMap && datasetKeyMap.isEmpty()) {
                Objects.requireNonNull(datasetRootMap).remove(datasetRootId);
            }
            if (null != subscription && Objects.requireNonNull(datasetRootMap).isEmpty()) {
                Objects.requireNonNull(datasetMap).remove(datasetId);
                if (datasetMap.isEmpty()) {
                    _instanceSubscriptions.remove(schemaId);
                }
            }
            if (Replicant.shouldCheckInvariants()) {
                invariant(
                        () -> null != subscription,
                        () -> "Replicant-0060: unlinkSubscription invoked with Dataset Address " + datasetAddress
                                + " but no subscription with that Dataset Address exists.");
                assert null != subscription;
                invariant(
                        () -> Disposable.isDisposed(subscription),
                        () -> "Replicant-0061: unlinkSubscription invoked with Dataset Address " + datasetAddress
                                + " but subscription has not already been disposed.");
            }
            getInstanceSubscriptionsObservableValue().reportChanged();
            return Objects.requireNonNull(subscription);
        }
    }

    @PreDispose
    void preDispose() {
        _typeSubscriptions.values().stream()
                .flatMap(s -> s.values().stream())
                .flatMap(s -> s.values().stream())
                .peek(this::detachSubscription)
                .forEach(Disposable::dispose);
        _instanceSubscriptions.values().stream()
                .flatMap(t -> t.values().stream())
                .flatMap(t -> t.values().stream())
                .flatMap(t -> t.values().stream())
                .peek(this::detachSubscription)
                .forEach(Disposable::dispose);
    }

    @ContextRef
    abstract ArezContext context();
}
