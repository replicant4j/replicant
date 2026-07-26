package replicant;

import static org.realityforge.braincheck.Guards.*;

import arez.Disposable;
import arez.ObservableValue;
import arez.annotations.ArezComponent;
import arez.annotations.Feature;
import arez.annotations.Observable;
import arez.annotations.ObservableValueRef;
import arez.annotations.PreDispose;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The internal tracking entry for a client-side Replica.
 */
@ArezComponent(observable = Feature.ENABLE, requireId = Feature.DISABLE)
public abstract class ReplicaEntry extends ReplicantService {
    @NonNull
    private final Map<DatasetAddress, Subscription> _subscriptions = new HashMap<>();
    /**
     * A human consumable name for the Replica Entry. It should be non-null if {@link Replicant#areNamesEnabled()} returns
     * true and <tt>null</tt> otherwise.
     */
    @Nullable
    private final String _name;

    @NonNull
    private final Class<?> _type;

    private final int _id;

    @Nullable
    private Object _replica;

    static ReplicaEntry create(
            @Nullable final ReplicantContext context,
            @Nullable final String name,
            @NonNull final Class<?> type,
            final int id) {
        return new Arez_ReplicaEntry(context, name, type, id);
    }

    ReplicaEntry(
            @Nullable final ReplicantContext context,
            @Nullable final String name,
            @NonNull final Class<?> type,
            final int id) {
        super(context);
        if (Replicant.shouldCheckApiInvariants()) {
            apiInvariant(
                    () -> Replicant.areNamesEnabled() || null == name,
                    () -> "Replicant-0032: ReplicaEntry passed a name '" + name
                            + "' but Replicant.areNamesEnabled() is false");
        }
        _name = Replicant.areNamesEnabled() ? Objects.requireNonNull(name) : null;
        _type = Objects.requireNonNull(type);
        _id = id;
    }

    /**
     * Return the name of the ReplicaEntry.
     * This method should NOT be invoked unless {@link Replicant#areNamesEnabled()} returns true and will throw an
     * exception if invariant checking is enabled.
     *
     * @return the name of the ReplicaEntry.
     */
    @NonNull
    public String getName() {
        if (Replicant.shouldCheckApiInvariants()) {
            apiInvariant(
                    Replicant::areNamesEnabled,
                    () -> "Replicant-0009: ReplicaEntry.getName() invoked when Replicant.areNamesEnabled() is false");
        }
        return Objects.requireNonNull(_name);
    }

    @NonNull
    public Class<?> getType() {
        return _type;
    }

    public int getId() {
        return _id;
    }

    @NonNull
    public Object getReplica() {
        final Object replica = maybeReplica();
        if (Replicant.shouldCheckApiInvariants()) {
            apiInvariant(
                    () -> null != replica,
                    () -> "Replicant-0071: ReplicaEntry.getReplica() invoked when no replica present");
        }
        return Objects.requireNonNull(replica);
    }

    @Observable(name = "replica")
    @Nullable
    public Object maybeReplica() {
        return _replica;
    }

    void setReplica(@Nullable final Object replica) {
        _replica = replica;
    }

    /**
     * Return the collection of subscriptions for the Replica Entry.
     *
     * @return the subscriptions.
     */
    @NonNull
    @Observable(expectSetter = false)
    public Collection<Subscription> getSubscriptions() {
        // This return result is already immutable as it is part of map so no need to convert to immutable
        return _subscriptions.values();
    }

    @ObservableValueRef
    abstract ObservableValue<?> getSubscriptionsObservableValue();

    /**
     * Link to subscription if not already subscribed, ignore otherwise.
     */
    void tryLinkToSubscription(@NonNull final Subscription subscription) {
        if (!_subscriptions.containsKey(subscription.datasetAddress())) {
            linkToSubscription(subscription);
        }
    }

    /**
     * Link to subscription if it does not exist.
     *
     * @param subscription the subscription.
     */
    void linkToSubscription(@NonNull final Subscription subscription) {
        if (Replicant.shouldCheckInvariants()) {
            invariant(
                    () -> null == subscription.findReplicaEntryByTypeAndId(getType(), getId()),
                    () -> "Replicant-0080: ReplicaEntry.linkToSubscription invoked on Replica Entry " + this
                            + " passing subscription "
                            + subscription.datasetAddress() + " but Replica Entry is already linked to subscription.");
        }
        linkReplicaEntryToSubscription(subscription);
        subscription.linkSubscriptionToReplicaEntry(this);
    }

    private void linkReplicaEntryToSubscription(@NonNull final Subscription subscription) {
        getSubscriptionsObservableValue().preReportChanged();
        final DatasetAddress datasetAddress = subscription.datasetAddress();
        if (!_subscriptions.containsKey(datasetAddress)) {
            _subscriptions.put(datasetAddress, subscription);
            getSubscriptionsObservableValue().reportChanged();
        }
    }

    /**
     * Remove the specified subscription.
     * This is invoked on the client-side by user code when data changes and is now filtered out from the Dataset.
     * This is only intended to be invoked when changes in data can change a Dataset's Subscription. This occurs when
     * the Dataset has a mutable Routing Key or a Filter Type of INTERNAL with rules that are
     * data dependent. This means that the client has to be responsible for removing subscriptions on the client.
     *
     * @param subscription the subscription.
     */
    public void delinkFromFilteringSubscription(@NonNull final Subscription subscription) {
        if (Replicant.shouldCheckInvariants()) {
            invariant(
                    () -> Dataset.FilterType.NONE != subscription.getDataset().getFilterType(),
                    () -> "Replicant-0018: ReplicaEntry.delinkFromFilteringSubscription invoked on Replica Entry "
                            + this
                            + " passing subscription "
                            + subscription.datasetAddress() + " but subscription is " + "not filtered.");
        }
        delinkFromSubscription(subscription);
    }

    /**
     * Remove the specified subscription.
     *
     * @param subscription the subscription.
     */
    void delinkFromSubscription(@NonNull final Subscription subscription) {
        if (Replicant.shouldCheckInvariants()) {
            invariant(
                    () -> null != subscription.findReplicaEntryByTypeAndId(getType(), getId()),
                    () -> "Replicant-0081: ReplicaEntry.delinkFromSubscription invoked on Replica Entry " + this
                            + " passing subscription "
                            + subscription.datasetAddress() + " but Replica Entry is not linked to subscription.");
        }
        delinkSubscriptionFromReplicaEntry(subscription, false);
        subscription.delinkReplicaEntryFromSubscription(this, false);
        disposeReplicaEntryIfNoSubscriptions();
    }

    /**
     * Delink the specified subscription from this Replica Entry.
     * This method does not delink the Replica Entry from the subscription and it is assumed this is achieved through
     * other means such as {@link Subscription#delinkReplicaEntryFromSubscription(ReplicaEntry, boolean)}.
     *
     * @param subscription the subscription.
     */
    void delinkSubscriptionFromReplicaEntry(@NonNull final Subscription subscription) {
        delinkSubscriptionFromReplicaEntry(subscription, true);
    }

    private void delinkSubscriptionFromReplicaEntry(
            @NonNull final Subscription subscription, final boolean disposeIfNoSubscriptions) {
        getSubscriptionsObservableValue().preReportChanged();
        final DatasetAddress datasetAddress = subscription.datasetAddress();
        final Subscription candidate = _subscriptions.remove(datasetAddress);
        getSubscriptionsObservableValue().reportChanged();
        if (Replicant.shouldCheckApiInvariants()) {
            apiInvariant(
                    () -> null != candidate,
                    () -> "Unable to locate subscription for Dataset Address " + datasetAddress + " on Replica Entry "
                            + this);
        }
        if (disposeIfNoSubscriptions) {
            disposeReplicaEntryIfNoSubscriptions();
        }
    }

    void disposeReplicaEntryIfNoSubscriptions() {
        if (_subscriptions.isEmpty()) {
            Disposable.dispose(this);
        }
    }

    @PreDispose
    void preDispose() {
        if (null != _replica) {
            for (final Subscription subscription : new ArrayList<>(_subscriptions.values())) {
                subscription.delinkReplicaEntryFromSubscription(this, false);

                final Dataset dataset = subscription.getDataset();
                if (dataset.isInstanceDataset()
                        && (dataset.getDatasetRootEntityType() == getType())
                        && (Objects.equals(subscription.datasetAddress().datasetRootId(), getId()))) {
                    // If there is any subscription that this Replica is the Dataset Root of, then explicitly dispose
                    // it.
                    // Historically we used to leave this to removeOrphanedSubscriptions process to clean them up but
                    // now
                    // downstream code is directly subscribing to subscription objects which may be briefly invalid.
                    // when
                    // the Dataset Root has been removed but the subscription has not been cleaned up then the
                    // Subscription
                    // is in a zombie state and accessing methods like getDatasetRoot will cause crashes at best and
                    // assertions
                    // in development mode. Explicitly unsubscribing from subscriptions will cause any result in almost
                    // all that
                    // code being skipped as Subscription is no longer observable.
                    Disposable.dispose(subscription);
                }
            }
            Disposable.dispose(_replica);
        }
        if (Replicant.shouldCheckInvariants()) {
            // This is not needed but we do it to make it easier to understand behaviour during debugging
            _subscriptions.clear();
        }
    }

    @Override
    public String toString() {
        if (Replicant.areNamesEnabled()) {
            return getName();
        } else {
            return super.toString();
        }
    }

    Map<DatasetAddress, Subscription> subscriptions() {
        return _subscriptions;
    }
}
