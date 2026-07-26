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
    // SystemId -> ChannelId -> RootId -> DatasetKey => Entry
    @NonNull
    private final Map<Integer, Map<Integer, Map<Integer, Map<String, Subscription>>>> _instanceSubscriptions =
            new HashMap<>();
    // SystemId -> ChannelId -> DatasetKey => Entry
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
     * Return the collection of instance subscriptions for channel.
     *
     * @param schemaId  the schema id.
     * @param channelId the channel id.
     * @return the set of ids for all instance subscriptions with specified channel type.
     */
    @NonNull
    Set<Integer> getInstanceSubscriptionIds(final int schemaId, final int channelId) {
        getInstanceSubscriptionsObservableValue().reportObserved();
        final Map<Integer, Map<Integer, Map<String, Subscription>>> channelMaps = _instanceSubscriptions.get(schemaId);
        final Map<Integer, Map<String, Subscription>> map = null == channelMaps ? null : channelMaps.get(channelId);
        if (null == map) {
            return Collections.emptySet();
        } else {
            return CollectionsUtil.wrap(new HashSet<>(map.keySet()));
        }
    }

    /**
     * Create a subscription.
     * This method should not be invoked if a subscription with the existing name already exists.
     *
     * @param address              the channel address.
     * @param filter               the filter if subscription is filterable.
     * @param explicitSubscription if subscription was explicitly requested by the client.
     * @return the subscription.
     */
    @NonNull
    Subscription createSubscription(
            @NonNull final ChannelAddress address, @Nullable final Object filter, final boolean explicitSubscription) {
        if (Replicant.shouldCheckApiInvariants()) {
            apiInvariant(
                    () -> null == findSubscription(address),
                    () -> "Replicant-0064: createSubscription invoked with address " + address
                            + " but a subscription with that address already exists.");
        }
        final Integer rootId = address.rootId();
        final String datasetKey = address.datasetKey();
        if (null == rootId) {
            getTypeSubscriptionsObservableValue().preReportChanged();
        } else {
            getInstanceSubscriptionsObservableValue().preReportChanged();
        }
        final Subscription subscription = Subscription.create(
                Replicant.areZonesEnabled() ? getReplicantContext() : null, address, filter, explicitSubscription);
        DisposeNotifier.asDisposeNotifier(subscription).addOnDisposeListener(this, () -> destroy(subscription), true);
        if (null == rootId) {
            _typeSubscriptions
                    .computeIfAbsent(address.schemaId(), key -> new HashMap<>())
                    .computeIfAbsent(address.channelId(), key -> new HashMap<>())
                    .put(datasetKey, subscription);
            getTypeSubscriptionsObservableValue().reportChanged();
        } else {
            _instanceSubscriptions
                    .computeIfAbsent(address.schemaId(), key -> new HashMap<>())
                    .computeIfAbsent(address.channelId(), key -> new HashMap<>())
                    .computeIfAbsent(rootId, key -> new HashMap<>())
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
        unlinkSubscription(subscription.address());
    }

    private void detachSubscription(@NonNull final Subscription subscription) {
        DisposeNotifier.asDisposeNotifier(subscription).removeOnDisposeListener(this, true);
    }

    /**
     * Return the subscription for the specified address.
     * This method will observe the <code>typeSubscriptions</code> or <code>instanceSubscriptions</code>
     * property if not found and the result {@link Subscription} if found. This ensures that if an observer
     * invokes this method then the observer will be rescheduled when the result changes.
     *
     * @param address the channel address.
     * @return the subscription if it exists, null otherwise.
     */
    @Nullable
    Subscription findSubscription(@NonNull final ChannelAddress address) {
        final int schemaId = address.schemaId();
        final int channelId = address.channelId();
        final Integer rootId = address.rootId();
        final String datasetKey = address.datasetKey();
        return null == rootId
                ? findTypeSubscription(schemaId, channelId, datasetKey)
                : findInstanceSubscription(schemaId, channelId, rootId, datasetKey);
    }

    /**
     * Return the type subscription for the specified channelType.
     * This method will observe the <code>typeSubscriptions</code> property if not
     * found and the result {@link Subscription} if found. This ensures that if an observer
     * invokes this method then the observer will be rescheduled when the result changes.
     *
     * @param schemaId  the schema id.
     * @param channelId the channel id.
     * @return the subscription if any matches.
     */
    @Nullable
    private Subscription findTypeSubscription(
            final int schemaId, final int channelId, @Nullable final String datasetKey) {
        final Map<Integer, Map<String, Subscription>> channelMap = _typeSubscriptions.get(schemaId);
        final Map<String, Subscription> datasetKeyMap = null == channelMap ? null : channelMap.get(channelId);
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
     * Return the instance subscription for the specified channelType and id.
     * This method will observe the <code>instanceSubscriptions</code> property if not
     * found and the result {@link Subscription} if found. This ensures that if an observer
     * invokes this method then the observer will be rescheduled when the result changes.
     *
     * @param schemaId  the schema id.
     * @param channelId the channel id.
     * @param id        the channel id.
     * @return the subscription if any matches.
     */
    @Nullable
    private Subscription findInstanceSubscription(
            final int schemaId, final int channelId, final int id, @Nullable final String datasetKey) {
        final Map<Integer, Map<Integer, Map<String, Subscription>>> channelMap = _instanceSubscriptions.get(schemaId);
        final Map<Integer, Map<String, Subscription>> rootMap = null == channelMap ? null : channelMap.get(channelId);
        final Map<String, Subscription> datasetKeyMap = null == rootMap ? null : rootMap.get(id);
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
     * Remove subscription on channel specified by address.
     * This method should only be invoked if a subscription exists
     *
     * @param address the channel address.
     * @return the subscription.
     */
    @NonNull
    Subscription unlinkSubscription(@NonNull final ChannelAddress address) {
        final int schemaId = address.schemaId();
        final int channelId = address.channelId();
        final Integer rootId = address.rootId();
        final String datasetKey = address.datasetKey();
        if (null == rootId) {
            getTypeSubscriptionsObservableValue().preReportChanged();
            final Map<Integer, Map<String, Subscription>> map = _typeSubscriptions.get(schemaId);
            final Map<String, Subscription> datasetKeyMap = null == map ? null : map.get(channelId);
            final Subscription subscription = null == datasetKeyMap ? null : datasetKeyMap.remove(datasetKey);
            if (null != datasetKeyMap && datasetKeyMap.isEmpty()) {
                Objects.requireNonNull(map).remove(channelId);
            }
            if (null != subscription && Objects.requireNonNull(map).isEmpty()) {
                _typeSubscriptions.remove(schemaId);
            }
            if (Replicant.shouldCheckInvariants()) {
                invariant(
                        () -> null != subscription,
                        () -> "Replicant-0062: unlinkSubscription invoked with address " + address
                                + " but no subscription with that address exists.");
                assert null != subscription;
                invariant(
                        () -> Disposable.isDisposed(subscription),
                        () -> "Replicant-0063: unlinkSubscription invoked with address " + address
                                + " but subscription has not already been disposed.");
            }
            getTypeSubscriptionsObservableValue().reportChanged();
            return Objects.requireNonNull(subscription);
        } else {
            getInstanceSubscriptionsObservableValue().preReportChanged();
            final Map<Integer, Map<Integer, Map<String, Subscription>>> channelMap =
                    _instanceSubscriptions.get(schemaId);
            final Map<Integer, Map<String, Subscription>> rootMap =
                    null == channelMap ? null : channelMap.get(channelId);
            final Map<String, Subscription> datasetKeyMap = null == rootMap ? null : rootMap.get(rootId);
            final Subscription subscription = null == datasetKeyMap ? null : datasetKeyMap.remove(datasetKey);
            if (null != datasetKeyMap && datasetKeyMap.isEmpty()) {
                Objects.requireNonNull(rootMap).remove(rootId);
            }
            if (null != subscription && Objects.requireNonNull(rootMap).isEmpty()) {
                Objects.requireNonNull(channelMap).remove(channelId);
                if (channelMap.isEmpty()) {
                    _instanceSubscriptions.remove(schemaId);
                }
            }
            if (Replicant.shouldCheckInvariants()) {
                invariant(
                        () -> null != subscription,
                        () -> "Replicant-0060: unlinkSubscription invoked with address " + address
                                + " but no subscription with that address exists.");
                assert null != subscription;
                invariant(
                        () -> Disposable.isDisposed(subscription),
                        () -> "Replicant-0061: unlinkSubscription invoked with address " + address
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
