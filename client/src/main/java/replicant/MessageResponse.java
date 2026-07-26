package replicant;

import static org.realityforge.braincheck.Guards.*;

import arez.component.Linkable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.messages.EntityChange;
import replicant.messages.ServerToClientMessage;
import replicant.messages.SubscriptionChangeMessage;
import replicant.messages.UpdateMessage;
import replicant.spy.DataLoadStatus;

/**
 * A simple class encapsulating the process of loading data from a json change set.
 */
final class MessageResponse {
    private final int _schemaId;
    /**
     * The message to process.
     */
    @NonNull
    private final ServerToClientMessage _message;

    @Nullable
    private final RequestEntry _request;
    /**
     * The current index into changes.
     */
    private int _entityChangeIndex;

    @Nullable
    private LinkedList<Linkable> _replicasToLink = new LinkedList<>();
    /**
     * The list of Replicas that have been changed during processing.
     * Used to invoke the schema hook after replicated data changes.
     */
    @NonNull
    private final LinkedList<Object> _replicasChanged = new LinkedList<>();

    @Nullable
    private List<SubscriptionChange> _parsedSubscriptionChanges;

    private boolean _worldValidated;
    private boolean _subscriptionActionsProcessed;
    private boolean _orphanSubscriptionRemoved;
    private int _subscriptionSubscribeCount;
    private int _subscriptionUpdateCount;
    private int _subscriptionUnsubscribeCount;
    private int _entityUpdateCount;
    private int _entityRemoveCount;
    private int _entityLinkCount;

    MessageResponse(
            final int schemaId, @NonNull final ServerToClientMessage message, @Nullable final RequestEntry request) {
        if (Replicant.shouldCheckInvariants()) {
            invariant(
                    () -> null == request || ((Integer) request.getRequestId()).equals(message.getRequestId()),
                    () -> "Replicant-0011: Response message specified requestId '" + message.getRequestId()
                            + "' but request specified requestId '"
                            + Objects.requireNonNull(request).getRequestId() + "'.");
        }
        _schemaId = schemaId;
        _message = Objects.requireNonNull(message);
        _request = request;
        _entityChangeIndex = 0;
    }

    int getSubscriptionSubscribeCount() {
        return _subscriptionSubscribeCount;
    }

    int getSubscriptionUpdateCount() {
        return _subscriptionUpdateCount;
    }

    int getSubscriptionUnsubscribeCount() {
        return _subscriptionUnsubscribeCount;
    }

    int getEntityUpdateCount() {
        return _entityUpdateCount;
    }

    int getEntityRemoveCount() {
        return _entityRemoveCount;
    }

    int getEntityLinkCount() {
        return _entityLinkCount;
    }

    void incSubscriptionSubscribeCount() {
        if (Replicant.areSpiesEnabled()) {
            _subscriptionSubscribeCount++;
        }
    }

    void incSubscriptionUpdateCount() {
        if (Replicant.areSpiesEnabled()) {
            _subscriptionUpdateCount++;
        }
    }

    void incSubscriptionUnsubscribeCount() {
        if (Replicant.areSpiesEnabled()) {
            _subscriptionUnsubscribeCount++;
        }
    }

    void incEntityUpdateCount() {
        if (Replicant.areSpiesEnabled()) {
            _entityUpdateCount++;
        }
    }

    void incEntityRemoveCount() {
        if (Replicant.areSpiesEnabled()) {
            _entityRemoveCount++;
        }
    }

    void incEntityLinkCount() {
        if (Replicant.areSpiesEnabled()) {
            _entityLinkCount++;
        }
    }

    @Nullable
    RequestEntry getRequest() {
        return _request;
    }

    boolean areEntityChangesPending() {
        if (UpdateMessage.TYPE.equals(_message.getType())) {
            final UpdateMessage message = (UpdateMessage) _message;
            return message.hasEntityChanges() && _entityChangeIndex < message.getEntityChanges().length;
        } else {
            return false;
        }
    }

    boolean needsSubscriptionChangesProcessed() {
        if (UpdateMessage.TYPE.equals(_message.getType())) {
            final UpdateMessage message = (UpdateMessage) _message;
            return !_subscriptionActionsProcessed
                    && (message.hasSubscriptionChanges() && 0 != message.getSubscriptionChanges().length
                            || message.hasFilterParameterSubscriptionChanges()
                                    && 0 != message.getFilterParameterSubscriptionChanges().length);
        } else {
            return false;
        }
    }

    void setParsedSubscriptionChanges(@NonNull final List<SubscriptionChange> parsedSubscriptionChanges) {
        _parsedSubscriptionChanges = Objects.requireNonNull(parsedSubscriptionChanges);
    }

    @NonNull
    List<SubscriptionChange> getSubscriptionChanges() {
        assert UpdateMessage.TYPE.equals(_message.getType());
        final UpdateMessage changeSet = (UpdateMessage) _message;
        assert changeSet.hasSubscriptionChanges() || changeSet.hasFilterParameterSubscriptionChanges();
        if (null == _parsedSubscriptionChanges) {
            _parsedSubscriptionChanges = toSubscriptionChanges(changeSet);
        }
        return Objects.requireNonNull(_parsedSubscriptionChanges);
    }

    void markSubscriptionActionsProcessed() {
        _subscriptionActionsProcessed = true;
    }

    @Nullable
    EntityChange nextEntityChange() {
        if (areEntityChangesPending()) {
            final EntityChange change = ((UpdateMessage) _message).getEntityChanges()[_entityChangeIndex];
            _entityChangeIndex++;
            return change;
        } else {
            return null;
        }
    }

    void replicaProcessed(@NonNull final Object replica) {
        if (replica instanceof Linkable) {
            Objects.requireNonNull(_replicasToLink).add((Linkable) replica);
        }
        _replicasChanged.add(replica);
    }

    boolean areReplicaLinksPending() {
        return null != _replicasToLink && !_replicasToLink.isEmpty();
    }

    @Nullable
    Linkable nextReplicaToLink() {
        if (areReplicaLinksPending()) {
            return Objects.requireNonNull(_replicasToLink).remove();
        } else {
            _replicasToLink = null;
            return null;
        }
    }

    boolean areReplicaUpdateActionsPending() {
        return !_replicasChanged.isEmpty();
    }

    @Nullable
    Object nextReplicaToPostAction() {
        if (areReplicaUpdateActionsPending()) {
            return _replicasChanged.remove();
        } else {
            return null;
        }
    }

    void completePostActions() {
        _replicasChanged.clear();
    }

    @NonNull
    ServerToClientMessage getMessage() {
        return _message;
    }

    void markWorldAsValidated() {
        if (Replicant.shouldValidateReplicasOnLoad()) {
            _worldValidated = true;
        }
    }

    boolean hasWorldBeenValidated() {
        return !Replicant.shouldValidateReplicasOnLoad() || _worldValidated;
    }

    @NonNull
    DataLoadStatus toStatus() {
        assert Replicant.areSpiesEnabled();
        return new DataLoadStatus(
                _message.getRequestId(),
                getSubscriptionSubscribeCount(),
                getSubscriptionUpdateCount(),
                getSubscriptionUnsubscribeCount(),
                getEntityUpdateCount(),
                getEntityRemoveCount(),
                getEntityLinkCount());
    }

    @Override
    public String toString() {
        if (Replicant.areNamesEnabled()) {
            return "MessageResponse[" + "Type="
                    + _message.getType() + ",RequestId="
                    + _message.getRequestId() + ",ChangeIndex="
                    + _entityChangeIndex + ",ReplicasToLink.size="
                    + (null == _replicasToLink ? 0 : _replicasToLink.size()) + "]";
        } else {
            return super.toString();
        }
    }

    @NonNull
    private List<SubscriptionChange> toSubscriptionChanges(@NonNull final UpdateMessage changeSet) {
        final List<SubscriptionChange> changes = new ArrayList<>();

        if (changeSet.hasSubscriptionChanges()) {
            for (final String subscriptionChange : changeSet.getSubscriptionChanges()) {
                changes.add(SubscriptionChange.from(_schemaId, subscriptionChange));
            }
        }
        if (changeSet.hasFilterParameterSubscriptionChanges()) {
            for (final SubscriptionChangeMessage subscriptionChange :
                    changeSet.getFilterParameterSubscriptionChanges()) {
                changes.add(SubscriptionChange.from(_schemaId, subscriptionChange));
            }
        }
        return changes;
    }

    boolean areOrphanSubscriptionsRemoved() {
        return _orphanSubscriptionRemoved;
    }

    void markOrphanSubscriptionsRemoved() {
        _orphanSubscriptionRemoved = true;
    }
}
