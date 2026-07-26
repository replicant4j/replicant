package replicant.server;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.json.JsonObject;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class ChangeSet {
    @NonNull
    private final List<SubscriptionAction> _subscriptionActions = new LinkedList<>();

    @NonNull
    private final Map<String, Change> _changes = new LinkedHashMap<>();

    private boolean _required;

    @Nullable
    private String _eTag;

    public boolean hasContent() {
        return _required || !_subscriptionActions.isEmpty() || !_changes.isEmpty();
    }

    public boolean isRequired() {
        return _required;
    }

    public void setRequired(final boolean required) {
        _required = required;
    }

    @Nullable
    public String getETag() {
        return _eTag;
    }

    public void setETag(@NonNull final String eTag) {
        assert null == _eTag;
        _eTag = Objects.requireNonNull(eTag);
    }

    public void mergeSubscriptionActions(@NonNull final Collection<SubscriptionAction> actions) {
        for (final var action : actions) {
            mergeSubscriptionAction(action);
        }
    }

    public void mergeSubscriptionAction(
            @NonNull final DatasetAddress datasetAddress, final SubscriptionAction.@NonNull Action action) {
        mergeSubscriptionAction(datasetAddress, action, null);
    }

    public void mergeSubscriptionAction(
            @NonNull final DatasetAddress datasetAddress,
            final SubscriptionAction.@NonNull Action action,
            @Nullable final JsonObject filterParameter) {
        //noinspection ConstantValue
        assert SubscriptionAction.Action.DELETE != action
                || SubscriptionAction.Action.UNSUBSCRIBE != action
                || null == filterParameter;
        mergeSubscriptionAction(SubscriptionAction.of(datasetAddress, action, filterParameter));
    }

    public void mergeSubscriptionAction(@NonNull final SubscriptionAction action) {
        final var actionType = action.action();
        /*
         * If we have a matching inverse action in actions list then we can remove
         * that action and avoid adding this action. This avoids scenario where there
         * are multiple actions for the same Dataset Address and Filter Parameter in ChangeSet.
         * A parameterized SUBSCRIBE after an UNSUBSCRIBE is retained as a single SUBSCRIBE so a
         * Fixed Filter Parameter replacement reaches the client.
         */
        if (SubscriptionAction.Action.SUBSCRIBE == actionType) {
            final var removedUnsubscribe =
                    _subscriptionActions.removeIf(a -> SubscriptionAction.Action.UNSUBSCRIBE == a.action()
                            && a.datasetAddress().equals(action.datasetAddress())
                            && null == a.filterParameter());
            _subscriptionActions.removeIf(a -> a.datasetAddress().equals(action.datasetAddress()));
            if (removedUnsubscribe && null == action.filterParameter()) {
                return;
            }
        } else if (SubscriptionAction.Action.UPDATE == actionType) {
            // We have got an update for one we are subscribing to so ignore the update and maybe update the existing
            // action
            final var newFilterParameter = action.filterParameter();
            var flags = new boolean[1];
            _subscriptionActions.replaceAll(a -> {
                final var datasetAddress = a.datasetAddress();
                if (SubscriptionAction.Action.SUBSCRIBE == a.action()
                        && datasetAddress.equals(action.datasetAddress())) {
                    flags[0] = true;
                    if (FilterParameterUtil.filterParametersEqual(a.filterParameter(), newFilterParameter)) {
                        return a;
                    } else {
                        return SubscriptionAction.of(
                                datasetAddress, SubscriptionAction.Action.SUBSCRIBE, newFilterParameter);
                    }
                } else {
                    return a;
                }
            });
            //noinspection ConstantValue
            if (flags[0]) {
                return;
            }
        } else if (SubscriptionAction.Action.UNSUBSCRIBE == actionType
                || SubscriptionAction.Action.DELETE == actionType) {
            final var removedSubscribe =
                    _subscriptionActions.removeIf(a -> SubscriptionAction.Action.SUBSCRIBE == a.action()
                            && a.datasetAddress().equals(action.datasetAddress()));
            _subscriptionActions.removeIf(a -> a.datasetAddress().equals(action.datasetAddress()));
            if (removedSubscribe) {
                return;
            }
        }

        _subscriptionActions.add(action);
    }

    @NonNull
    public List<SubscriptionAction> getSubscriptionActions() {
        return _subscriptionActions;
    }

    public void merge(@NonNull final Collection<Change> changes) {
        merge(changes, false);
    }

    private void merge(@NonNull final Collection<Change> changes, final boolean copyOnMerge) {
        for (final var change : changes) {
            merge(change, copyOnMerge);
        }
    }

    public void merge(@NonNull final Change change) {
        merge(change, false);
    }

    void merge(@NonNull final Change change, final boolean copyOnMerge) {
        final var existing = _changes.get(change.getKey());
        if (null != existing) {
            existing.merge(change);
        } else {
            _changes.put(change.getKey(), copyOnMerge ? change.duplicate() : change);
        }
    }

    public void merge(@NonNull final ChangeSet changeSet) {
        _eTag = changeSet.getETag();
        merge(changeSet.getChanges(), true);
        mergeSubscriptionActions(changeSet.getSubscriptionActions());
    }

    @NonNull
    public Collection<Change> getChanges() {
        return _changes.values();
    }
}
