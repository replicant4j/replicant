package replicant;

import static org.realityforge.braincheck.Guards.*;

import arez.annotations.ArezComponent;
import arez.annotations.ComponentDependency;
import arez.annotations.Feature;
import arez.annotations.Observable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.messages.ServerToClientMessage;
import replicant.spy.RequestStartedEvent;

/**
 * Connection state used by the connector to manage connection to backend system.
 * This includes a list of pending requests, pending messages that needs to be applied
 * to the local state etc.
 */
@ArezComponent(requireId = Feature.DISABLE)
abstract class Connection {
    /**
     * The containing Connector.
     */
    @NonNull
    @ComponentDependency
    final Connector _connector;
    /**
     * A map containing the rpc requests that are in progress.
     */
    private final Map<Integer, RequestEntry> _requests = new HashMap<>();
    /**
     * Pending actions that will change the area of interest.
     */
    private final LinkedList<SubscriptionOperation> _pendingSubscriptionOperations = new LinkedList<>();
    /**
     * Pending Commands.
     */
    private final LinkedList<Command> _pendingCommands = new LinkedList<>();
    /**
     * Pending Message Processing state for messages received from the server.
     */
    private final LinkedList<MessageProcessing> _pendingMessageProcessingQueue = new LinkedList<>();
    /**
     * The current message being processed.
     */
    @Nullable
    private MessageProcessing _currentMessageProcessing;
    /**
     * The current requests being processed. This list can contain multiple requests if they
     * are candidates for bulk actions.
     */
    @NonNull
    private final List<SubscriptionOperation> _currentSubscriptionOperations = new ArrayList<>();
    /**
     * Commands that have been sent to the server and are awaiting a Command Result.
     */
    @NonNull
    private final Map<Integer, Command> _activeCommands = new HashMap<>();

    @NonNull
    static Connection create(@NonNull final Connector connector) {
        return new Arez_Connection(connector);
    }

    Connection(@NonNull final Connector connector) {
        _connector = Objects.requireNonNull(connector);
    }

    @NonNull
    String ensureReplicantSessionId() {
        final String replicantSessionId = getReplicantSessionId();
        return Objects.requireNonNull(replicantSessionId);
    }

    /**
     * Return the server-issued Replicant Session ID.
     */
    @Observable(readOutsideTransaction = Feature.ENABLE, writeOutsideTransaction = Feature.ENABLE)
    @Nullable
    abstract String getReplicantSessionId();

    abstract void setReplicantSessionId(@NonNull String replicantSessionId);

    @NonNull
    Connector getConnector() {
        return _connector;
    }

    void requestCommand(
            @NonNull final String commandName,
            @Nullable final Object payload,
            @Nullable final CommandResultHandler commandResultHandler) {
        _pendingCommands.add(new Command(commandName, payload, commandResultHandler));
    }

    void requestSubscribe(@NonNull final DatasetAddress datasetAddress, @Nullable final Object filterParameter) {
        enqueueSubscriptionOperation(datasetAddress, SubscriptionOperation.Type.SUBSCRIBE, filterParameter);
    }

    void requestSubscriptionUpdate(
            @NonNull final DatasetAddress datasetAddress, @Nullable final Object filterParameter) {
        enqueueSubscriptionOperation(datasetAddress, SubscriptionOperation.Type.UPDATE, filterParameter);
    }

    void requestUnsubscribe(@NonNull final DatasetAddress datasetAddress) {
        enqueueSubscriptionOperation(datasetAddress, SubscriptionOperation.Type.UNSUBSCRIBE, null);
    }

    private void enqueueSubscriptionOperation(
            @NonNull final DatasetAddress datasetAddress,
            final SubscriptionOperation.@NonNull Type type,
            @Nullable final Object filterParameter) {
        _pendingSubscriptionOperations.add(new SubscriptionOperation(datasetAddress, type, filterParameter));
    }

    void enqueueMessageForProcessing(
            @NonNull final ServerToClientMessage message, @Nullable final RequestEntry request) {
        _pendingMessageProcessingQueue.add(
                new MessageProcessing(_connector.getSystemSchema().getId(), message, request));
    }

    /**
     * Return true if a matching Subscription Operation is pending or being processed.
     * The Filter Parameter is ignored for an Unsubscribe Operation.
     */
    boolean isSubscriptionOperationPending(
            final SubscriptionOperation.@NonNull Type type,
            @NonNull final DatasetAddress datasetAddress,
            @Nullable final Object filterParameter) {
        if (Replicant.shouldCheckInvariants()) {
            invariant(
                    () -> type != SubscriptionOperation.Type.UNSUBSCRIBE || null == filterParameter,
                    () -> "Replicant-0025: Connection.isSubscriptionOperationPending passed an UNSUBSCRIBE "
                            + "operation for Dataset Address '" + datasetAddress
                            + "' with a non-null Filter Parameter '" + filterParameter
                            + "'.");
        }
        return _currentSubscriptionOperations.stream().anyMatch(a -> a.match(type, datasetAddress, filterParameter))
                || _pendingSubscriptionOperations.stream()
                        .anyMatch(a -> a.match(type, datasetAddress, filterParameter));
    }

    /**
     * Return the index of the last matching Type in the pending Subscription Operation list.
     */
    int lastIndexOfPendingSubscriptionOperation(
            final SubscriptionOperation.@NonNull Type type,
            @NonNull final DatasetAddress datasetAddress,
            @Nullable final Object filterParameter) {
        if (Replicant.shouldCheckInvariants()) {
            invariant(
                    () -> type != SubscriptionOperation.Type.UNSUBSCRIBE || null == filterParameter,
                    () -> "Replicant-0024: Connection.lastIndexOfPendingSubscriptionOperation passed an UNSUBSCRIBE "
                            + "operation for Dataset Address '" + datasetAddress
                            + "' with a non-null Filter Parameter '" + filterParameter
                            + "'.");
        }
        int index = _pendingSubscriptionOperations.size();

        final Iterator<SubscriptionOperation> iterator = _pendingSubscriptionOperations.descendingIterator();
        while (iterator.hasNext()) {
            final SubscriptionOperation operation = iterator.next();
            if (operation.match(type, datasetAddress, filterParameter)) {
                return index;
            }
            index -= 1;
        }
        if (_currentSubscriptionOperations.stream().anyMatch(a -> a.match(type, datasetAddress, filterParameter))) {
            return 0;
        } else {
            return -1;
        }
    }

    @NonNull
    RequestEntry newRequest(
            @Nullable final String name,
            final boolean synchronizationPointRequest,
            @Nullable final CommandResultHandler commandResultHandler) {
        final int requestId = getLastTxRequestId() + 1;
        setLastTxRequestId(requestId);
        final RequestEntry request =
                new RequestEntry(requestId, name, synchronizationPointRequest, commandResultHandler);
        _requests.put(requestId, request);
        if (Replicant.areSpiesEnabled()
                && _connector.getReplicantContext().getSpy().willPropagateSpyEvents()) {
            _connector
                    .getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new RequestStartedEvent(
                            _connector.getSystemSchema().getId(),
                            _connector.getSystemSchema().getName(),
                            request.getRequestId(),
                            request.getName()));
        }
        return request;
    }

    @NonNull
    RequestEntry getRequest(final int requestId) {
        final RequestEntry entry = _requests.get(requestId);
        if (Replicant.shouldCheckApiInvariants()) {
            apiInvariant(
                    () -> null != entry,
                    () -> "Replicant-0066: Unable to locate request with id '" + requestId + "' specified "
                            + "by message. Existing Requests: " + getRequests());
        }
        return Objects.requireNonNull(entry);
    }

    Map<Integer, RequestEntry> getRequests() {
        return _requests;
    }

    void removeRequest(final int requestId) {
        final RequestEntry entry = _requests.remove(requestId);
        if (null != entry && entry.isSynchronizationPointRequest()) {
            setLastReachedSynchronizationPointRequestId(requestId);
        }
        if (Replicant.shouldCheckInvariants()) {
            invariant(
                    () -> null != entry,
                    () -> "Replicant-0067: Attempted to remove request with id " + requestId
                            + " from Replicant Session '" + getReplicantSessionId()
                            + "' but no such request exists.");
        }
    }

    @Nullable
    MessageProcessing getCurrentMessageProcessing() {
        return _currentMessageProcessing;
    }

    @NonNull
    MessageProcessing ensureCurrentMessageProcessing() {
        return Objects.requireNonNull(_currentMessageProcessing);
    }

    /**
     * Return the id of the last request transmitted to the server.
     */
    @Observable(readOutsideTransaction = Feature.ENABLE, writeOutsideTransaction = Feature.ENABLE)
    abstract int getLastTxRequestId();

    abstract void setLastTxRequestId(int lastTxRequestId);

    /**
     * Return the id of the last request that established a Synchronization Point.
     */
    @Observable(readOutsideTransaction = Feature.ENABLE, writeOutsideTransaction = Feature.ENABLE)
    abstract int getLastReachedSynchronizationPointRequestId();

    abstract void setLastReachedSynchronizationPointRequestId(int requestId);

    /**
     * Return true if the latest request has established a Synchronization Point.
     */
    boolean isSynchronizationPointReached() {
        return null != getReplicantSessionId() && getLastTxRequestId() == getLastReachedSynchronizationPointRequestId();
    }

    /**
     * Select the next pending Message Processing state when no message is currently being processed.
     *
     * @return true if a message was selected, false otherwise.
     */
    boolean selectNextMessageProcessing() {
        assert null == _currentMessageProcessing;

        if (!_pendingMessageProcessingQueue.isEmpty()) {
            _currentMessageProcessing = _pendingMessageProcessingQueue.remove();
            return true;
        } else {
            return false;
        }
    }

    void setCurrentMessageProcessing(@Nullable final MessageProcessing currentMessageProcessing) {
        _currentMessageProcessing = currentMessageProcessing;
    }

    @NonNull
    List<SubscriptionOperation> getActiveSubscriptionOperations() {
        return CollectionsUtil.wrap(_currentSubscriptionOperations);
    }

    /**
     * Return the Subscription Operations currently being processed. If there are none and operations are pending,
     * derive the next batch and set them as current.
     */
    @NonNull
    List<SubscriptionOperation> getCurrentSubscriptionOperations() {
        if (_currentSubscriptionOperations.isEmpty() && !_pendingSubscriptionOperations.isEmpty()) {
            final SubscriptionOperation first = _pendingSubscriptionOperations.removeFirst();
            _currentSubscriptionOperations.add(first);
            while (!_pendingSubscriptionOperations.isEmpty()
                    && canGroupSubscriptionOperations(first, _pendingSubscriptionOperations.get(0))) {
                _currentSubscriptionOperations.add(_pendingSubscriptionOperations.removeFirst());
            }
        }
        return CollectionsUtil.wrap(_currentSubscriptionOperations);
    }

    @Nullable
    Command nextCommand() {
        return _pendingCommands.isEmpty() ? null : _pendingCommands.removeFirst();
    }

    @NonNull
    List<Command> getPendingCommands() {
        return _pendingCommands;
    }

    void recordActiveCommand(@NonNull final Command command) {
        assert command.isInProgress();
        _activeCommands.put(command.getRequestId(), command);
    }

    @NonNull
    Map<Integer, Command> getActiveCommands() {
        return _activeCommands;
    }

    @Nullable
    Command getActiveCommand(final int requestId) {
        return _activeCommands.get(requestId);
    }

    void markCommandAsComplete(final int requestId) {
        final Command request = _activeCommands.remove(requestId);
        assert null != request;
        request.markAsComplete();
    }

    /**
     * Return true if the matching operation can be grouped with the template operation in one network message.
     */
    boolean canGroupSubscriptionOperations(
            @NonNull final SubscriptionOperation template, @NonNull final SubscriptionOperation match) {
        final DatasetCacheService datasetCacheService =
                _connector.getReplicantContext().getDatasetCacheService();
        return null != template.getDatasetAddress().datasetRootId()
                && null != match.getDatasetAddress().datasetRootId()
                && (null == datasetCacheService
                        || null == datasetCacheService.lookupDatasetCacheEntry(template.getDatasetAddress()))
                && (null == datasetCacheService
                        || null == datasetCacheService.lookupDatasetCacheEntry(match.getDatasetAddress()))
                && template.getType().equals(match.getType())
                && template.getDatasetAddress().datasetId()
                        == match.getDatasetAddress().datasetId()
                && (SubscriptionOperation.Type.UNSUBSCRIBE == match.getType()
                        || FilterParameterUtil.filterParametersEqual(
                                match.getFilterParameter(), template.getFilterParameter()));
    }

    /**
     * Mark all current Subscription Operations as complete and clear the current list.
     */
    void completeSubscriptionOperation() {
        if (Replicant.shouldCheckInvariants()) {
            invariant(
                    () -> !_currentSubscriptionOperations.isEmpty(),
                    () -> "Replicant-0023: Connection.completeSubscriptionOperation() invoked when there are no"
                            + " current Subscription Operations.");
        }
        _currentSubscriptionOperations.forEach(SubscriptionOperation::markAsComplete);
        _currentSubscriptionOperations.clear();
    }

    void injectCurrentSubscriptionOperation(@NonNull final SubscriptionOperation operation) {
        _currentSubscriptionOperations.add(operation);
    }

    List<MessageProcessing> getPendingMessageProcessingQueue() {
        return CollectionsUtil.wrap(_pendingMessageProcessingQueue);
    }

    List<SubscriptionOperation> getPendingSubscriptionOperations() {
        return CollectionsUtil.wrap(_pendingSubscriptionOperations);
    }
}
