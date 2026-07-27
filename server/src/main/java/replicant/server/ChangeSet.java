package replicant.server;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.json.JsonObject;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class ChangeSet {
    @NonNull
    private final List<SubscriptionChange> _subscriptionChanges = new LinkedList<>();

    @NonNull
    private final Map<String, EntityChange> _entityChanges = new LinkedHashMap<>();

    private boolean _required;

    public boolean hasContent() {
        return _required || !_subscriptionChanges.isEmpty() || !_entityChanges.isEmpty();
    }

    public boolean isRequired() {
        return _required;
    }

    public void setRequired(final boolean required) {
        _required = required;
    }

    public void mergeSubscriptionChanges(@NonNull final Collection<SubscriptionChange> changes) {
        for (final var change : changes) {
            mergeSubscriptionChange(change);
        }
    }

    public void mergeSubscriptionChange(
            @NonNull final DatasetAddress datasetAddress, final SubscriptionChange.@NonNull Type type) {
        mergeSubscriptionChange(datasetAddress, type, null);
    }

    public void mergeSubscriptionChange(
            @NonNull final DatasetAddress datasetAddress,
            final SubscriptionChange.@NonNull Type type,
            @Nullable final JsonObject filterParameter) {
        //noinspection ConstantValue
        assert SubscriptionChange.Type.INVALIDATE_DATASET_ADDRESS != type
                || SubscriptionChange.Type.UNSUBSCRIBE != type
                || null == filterParameter;
        mergeSubscriptionChange(SubscriptionChange.of(datasetAddress, type, filterParameter));
    }

    public void mergeSubscriptionChange(@NonNull final SubscriptionChange change) {
        final var changeType = change.type();
        /*
         * If we have a matching inverse action in actions list then we can remove
         * that action and avoid adding this action. This avoids scenario where there
         * are multiple actions for the same Dataset Address and Filter Parameter in ChangeSet.
         * A parameterized SUBSCRIBE after an UNSUBSCRIBE is retained as a single SUBSCRIBE so a
         * Fixed Filter Parameter replacement reaches the client.
         */
        if (SubscriptionChange.Type.SUBSCRIBE == changeType) {
            final var removedUnsubscribe =
                    _subscriptionChanges.removeIf(a -> SubscriptionChange.Type.UNSUBSCRIBE == a.type()
                            && a.datasetAddress().equals(change.datasetAddress())
                            && null == a.filterParameter());
            _subscriptionChanges.removeIf(a -> a.datasetAddress().equals(change.datasetAddress()));
            if (removedUnsubscribe && null == change.filterParameter()) {
                return;
            }
        } else if (SubscriptionChange.Type.UPDATE == changeType) {
            // We have got an update for one we are subscribing to so ignore the update and maybe update the existing
            // action
            final var newFilterParameter = change.filterParameter();
            var flags = new boolean[1];
            _subscriptionChanges.replaceAll(a -> {
                final var datasetAddress = a.datasetAddress();
                if (SubscriptionChange.Type.SUBSCRIBE == a.type() && datasetAddress.equals(change.datasetAddress())) {
                    flags[0] = true;
                    if (FilterParameterUtil.filterParametersEqual(a.filterParameter(), newFilterParameter)) {
                        return a;
                    } else {
                        return SubscriptionChange.of(
                                datasetAddress, SubscriptionChange.Type.SUBSCRIBE, newFilterParameter);
                    }
                } else {
                    return a;
                }
            });
            //noinspection ConstantValue
            if (flags[0]) {
                return;
            }
        } else if (SubscriptionChange.Type.UNSUBSCRIBE == changeType
                || SubscriptionChange.Type.INVALIDATE_DATASET_ADDRESS == changeType) {
            final var removedSubscribe =
                    _subscriptionChanges.removeIf(a -> SubscriptionChange.Type.SUBSCRIBE == a.type()
                            && a.datasetAddress().equals(change.datasetAddress()));
            _subscriptionChanges.removeIf(a -> a.datasetAddress().equals(change.datasetAddress()));
            if (removedSubscribe) {
                return;
            }
        }

        _subscriptionChanges.add(change);
    }

    @NonNull
    public List<SubscriptionChange> getSubscriptionChanges() {
        return _subscriptionChanges;
    }

    public void merge(@NonNull final Collection<EntityChange> changes) {
        merge(changes, false);
    }

    private void merge(@NonNull final Collection<EntityChange> changes, final boolean copyOnMerge) {
        for (final var change : changes) {
            merge(change, copyOnMerge);
        }
    }

    public void merge(@NonNull final EntityChange change) {
        merge(change, false);
    }

    void merge(@NonNull final EntityChange change, final boolean copyOnMerge) {
        final var existing = _entityChanges.get(change.getKey());
        if (null != existing) {
            existing.merge(change);
        } else {
            _entityChanges.put(change.getKey(), copyOnMerge ? change.duplicate() : change);
        }
    }

    public void merge(@NonNull final ChangeSet changeSet) {
        merge(changeSet.getEntityChanges(), true);
        mergeSubscriptionChanges(changeSet.getSubscriptionChanges());
    }

    @NonNull
    public Collection<EntityChange> getEntityChanges() {
        return _entityChanges.values();
    }
}
