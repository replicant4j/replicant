package replicant;

import static org.realityforge.braincheck.Guards.*;

import akasha.core.JSON;
import arez.Arez;
import arez.ArezContext;
import arez.Disposable;
import arez.annotations.Action;
import arez.annotations.ArezComponent;
import arez.annotations.CascadeDispose;
import arez.annotations.ContextRef;
import arez.annotations.Feature;
import arez.annotations.Memoize;
import arez.annotations.Observable;
import arez.annotations.PostConstruct;
import arez.annotations.PreDispose;
import arez.component.Linkable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.messages.EntityChange;
import replicant.messages.EntityChangeData;
import replicant.messages.ErrorMessage;
import replicant.messages.OkMessage;
import replicant.messages.ServerToClientMessage;
import replicant.messages.UpdateMessage;
import replicant.messages.UseCacheMessage;
import replicant.spy.ConnectFailureEvent;
import replicant.spy.ConnectedEvent;
import replicant.spy.DisconnectFailureEvent;
import replicant.spy.DisconnectedEvent;
import replicant.spy.ExecCompletedEvent;
import replicant.spy.ExecRequestQueuedEvent;
import replicant.spy.ExecStartedEvent;
import replicant.spy.InSyncEvent;
import replicant.spy.MessageProcessFailureEvent;
import replicant.spy.MessageProcessedEvent;
import replicant.spy.MessageReadFailureEvent;
import replicant.spy.OutOfSyncEvent;
import replicant.spy.RestartEvent;
import replicant.spy.SubscribeCompletedEvent;
import replicant.spy.SubscribeRequestQueuedEvent;
import replicant.spy.SubscribeStartedEvent;
import replicant.spy.SubscriptionUpdateCompletedEvent;
import replicant.spy.SubscriptionUpdateRequestQueuedEvent;
import replicant.spy.SubscriptionUpdateStartedEvent;
import replicant.spy.SyncRequestEvent;
import replicant.spy.UnsubscribeCompletedEvent;
import replicant.spy.UnsubscribeRequestQueuedEvent;
import replicant.spy.UnsubscribeStartedEvent;

/**
 * The Connector is responsible for managing a Connection to a backend datasource.
 */
@ArezComponent(observable = Feature.ENABLE, requireId = Feature.DISABLE)
abstract class Connector extends ReplicantService {
    private static final int DEFAULT_LINKS_TO_PROCESS_PER_TICK = 20;
    private static final int DEFAULT_CHANGES_TO_PROCESS_PER_TICK = 20;
    /**
     * The schema that defines data-API used to interact with datasource.
     */
    @NonNull
    private final SystemSchema _schema;
    /**
     * The transport that connects to the backend system.
     */
    @NonNull
    private final Transport _transport;

    @NonNull
    private ConnectorState _state = ConnectorState.DISCONNECTED;
    /**
     * The current connection managed by the connector, if any.
     */
    @Nullable
    @CascadeDispose
    Connection _connection;
    /**
     * Flag indicating that the Connectors internal scheduler is actively progressing
     * requests and responses. A scheduler should only be active if there is a connection present.
     */
    private boolean _schedulerActive;
    /**
     * Flag when the scheduler has been explicitly paused.
     * When this is true, the {@link #progressMessages()} will terminate the next time
     * it is invoked and the scheduler will not be activated. This is
     */
    private boolean _schedulerPaused;
    /**
     * This lock is acquired by the Connector when it begins processing messages from the network.
     * Once the processor is idle the lock should be released to allow Arez to reflect all the changes.
     */
    @Nullable
    private Disposable _schedulerLock;
    /**
     * Maximum number of Replica links to attempt in a single tick of the scheduler. After this many links have
     * been processed then return and any remaining links can occur in a later tick.
     */
    private int _linksToProcessPerTick = DEFAULT_LINKS_TO_PROCESS_PER_TICK;
    /**
     * Maximum number of EntityChange messages processed in a single tick of the scheduler. After this many changes have
     * been processed then return and any remaining change can be processed in a later tick.
     */
    private int _changesToProcessPerTick = DEFAULT_CHANGES_TO_PROCESS_PER_TICK;
    /**
     * Action invoked after current MessageResponse is processed. This is typically used to update or alter
     * change Connection on message processing complete.
     */
    @Nullable
    private SafeProcedure _postMessageResponseAction;

    @Nullable
    private TransportContextImpl _context;

    @NonNull
    static Connector create(
            @Nullable final ReplicantContext context,
            @NonNull final SystemSchema schema,
            @NonNull final Transport transport) {
        return new Arez_Connector(context, schema, transport);
    }

    Connector(
            @Nullable final ReplicantContext context,
            @NonNull final SystemSchema schema,
            @NonNull final Transport transport) {
        super(context);
        _schema = Objects.requireNonNull(schema);
        _transport = Objects.requireNonNull(transport);
    }

    @PostConstruct
    void postConstruct() {
        getReplicantRuntime().registerConnector(this);
        getReplicantContext().getSchemaService().registerSchema(_schema);
    }

    @PreDispose
    void preDispose() {
        _schedulerPaused = true;
        _schedulerActive = false;
        releaseSchedulerLock();
        getReplicantContext().getSchemaService().deregisterSchema(_schema);
    }

    /**
     * Connect to the underlying data source.
     */
    void connect() {
        final ConnectorState state = getState();
        if (ConnectorState.CONNECTING != state && ConnectorState.CONNECTED != state) {
            ConnectorState newState = ConnectorState.ERROR;
            try {
                Disposable.dispose(_context);
                _context = new TransportContextImpl(this);
                _transport.requestConnect(_context);
                newState = ConnectorState.CONNECTING;
            } finally {
                setState(newState);
            }
        }
    }

    /**
     * Disconnect from underlying data source.
     */
    void disconnect() {
        final ConnectorState state = getState();
        if (ConnectorState.DISCONNECTING != state && ConnectorState.DISCONNECTED != state) {
            ConnectorState newState = ConnectorState.ERROR;
            try {
                _transport.requestDisconnect();
                newState = ConnectorState.DISCONNECTING;
            } finally {
                setState(newState);
            }
        }
    }

    /**
     * Return the schema associated with the connector.
     *
     * @return the schema associated with the connector.
     */
    @NonNull
    SystemSchema getSchema() {
        return _schema;
    }

    void onConnection(@NonNull final String connectionId) {
        final Connection connection = Connection.create(this);
        connection.setConnectionId(connectionId);
        doSetConnection(connection);
        triggerMessageScheduler();
    }

    void onDisconnection() {
        if (null == _connection) {
            onDisconnected();
        } else {
            doSetConnection(null);
        }
    }

    private void doSetConnection(@Nullable final Connection connection) {
        if (!Objects.equals(connection, _connection)) {
            if (null == _connection || null == _connection.getCurrentMessageResponse()) {
                setConnection(connection);
            } else {
                setPostMessageResponseAction(() -> setConnection(connection));
            }
        }
    }

    void setConnection(@Nullable final Connection connection) {
        _connection = connection;
        // Lock arez otherwise purgeSubscriptions will trigger subscription reconciliation when
        // _connection is null but State may be CONNECTED
        final Disposable schedulerLock = Arez.context().pauseScheduler();
        purgeSubscriptions();
        // Avoid emitting an event if disconnect resulted in an error
        if (ConnectorState.ERROR != getState() && ConnectorState.FATAL_ERROR != getState()) {
            if (null != _connection) {
                sendEtagsIfAny();
                onConnected();
            } else {
                onDisconnected();
            }
        }
        schedulerLock.dispose();
    }

    private void sendEtagsIfAny() {
        final CacheService cacheService = getReplicantContext().getCacheService();
        if (null != cacheService) {
            final HashMap<String, String> etags = new HashMap<>();
            final Set<DatasetAddress> datasetAddresses =
                    cacheService.keySet(getSchema().getId());
            for (final DatasetAddress datasetAddress : datasetAddresses) {
                final String eTag = cacheService.lookupEtag(datasetAddress);
                assert null != eTag;
                etags.put(datasetAddress.asDatasetAddressDescriptor(), eTag);
            }
            if (!etags.isEmpty()) {
                _transport.updateEtagsSync(etags);
            }
        }
    }

    @NonNull
    Connection ensureConnection() {
        if (Replicant.shouldCheckInvariants()) {
            invariant(
                    () -> null != _connection,
                    () -> "Replicant-0031: Connector.ensureConnection() when no connection is present.");
        }
        return Objects.requireNonNull(_connection);
    }

    @NonNull
    private MessageResponse ensureCurrentMessageResponse() {
        return ensureConnection().ensureCurrentMessageResponse();
    }

    @Action
    void purgeSubscriptions() {
        final SubscriptionService subscriptionService = getReplicantContext().getSubscriptionService();
        Stream.concat(
                        subscriptionService.getTypeDatasetSubscriptions().stream(),
                        subscriptionService.getInstanceDatasetSubscriptions().stream())
                // Only purge subscriptions for current system
                .filter(s -> s.datasetAddress().schemaId() == getSchema().getId())
                // Purge in reverse order. First Instance Dataset subscriptions then Type Dataset subscriptions
                .sorted(Comparator.reverseOrder())
                .forEachOrdered(Disposable::dispose);

        // Purge AreaOfInterest for current system
        getReplicantContext().getAreaOfInterestService().getAreasOfInterest().stream()
                .filter(s -> s.getDatasetAddress().schemaId() == getSchema().getId())
                .forEachOrdered(aoi -> updateAreaOfInterest(aoi.getDatasetAddress(), AreaOfInterest.Status.NOT_ASKED));
    }

    void setLinksToProcessPerTick(final int linksToProcessPerTick) {
        _linksToProcessPerTick = linksToProcessPerTick;
    }

    void setChangesToProcessPerTick(final int changesToProcessPerTick) {
        _changesToProcessPerTick = changesToProcessPerTick;
    }

    /**
     * Return true if a Subscription Operation with the specified parameters is pending or being processed.
     * For an UNSUBSCRIBE operation, the Filter Parameter is ignored.
     */
    boolean isSubscriptionOperationPending(
            final SubscriptionOperation.@NonNull Type type,
            @NonNull final DatasetAddress datasetAddress,
            @Nullable final Object filterParameter) {
        return null != _connection && _connection.isSubscriptionOperationPending(type, datasetAddress, filterParameter);
    }

    /**
     * Return the index of the last matching Type in the pending Subscription Operation list.
     */
    int lastIndexOfPendingSubscriptionOperation(
            final SubscriptionOperation.@NonNull Type type,
            @NonNull final DatasetAddress datasetAddress,
            @Nullable final Object filterParameter) {
        return null == _connection
                ? -1
                : _connection.lastIndexOfPendingSubscriptionOperation(type, datasetAddress, filterParameter);
    }

    void requestSync() {
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new SyncRequestEvent(getSchema().getId()));
        }
        _transport.requestSync();
        tryTriggerMessageScheduler();
    }

    void requestExec(
            @NonNull final String command,
            @Nullable final Object payload,
            @Nullable final ResponseHandler responseHandler) {
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new ExecRequestQueuedEvent(
                            getSchema().getId(), getSchema().getName(), command));
        }
        ensureConnection().requestExec(command, payload, responseHandler);
        tryTriggerMessageScheduler();
    }

    void requestSubscribe(@NonNull final DatasetAddress datasetAddress, @Nullable final Object filterParameter) {
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new SubscribeRequestQueuedEvent(datasetAddress, filterParameter));
        }
        validateDatasetKey(datasetAddress);
        ensureConnection().requestSubscribe(datasetAddress, filterParameter);
        tryTriggerMessageScheduler();
    }

    void requestSubscriptionUpdate(
            @NonNull final DatasetAddress datasetAddress, @Nullable final Object filterParameter) {
        if (Replicant.shouldCheckInvariants()) {
            invariant(
                    () -> {
                        return getSchema()
                                .getDataset(datasetAddress.datasetId())
                                .hasUpdatableFilterParameter();
                    },
                    () -> "Replicant-0082: Connector.requestSubscriptionUpdate invoked for Dataset Address "
                            + datasetAddress + " but the Dataset does not have an updatable Filter Parameter.");
        }
        validateDatasetKey(datasetAddress);
        ensureConnection().requestSubscriptionUpdate(datasetAddress, filterParameter);
        tryTriggerMessageScheduler();
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new SubscriptionUpdateRequestQueuedEvent(datasetAddress, filterParameter));
        }
    }

    void requestUnsubscribe(@NonNull final DatasetAddress datasetAddress) {
        validateDatasetKey(datasetAddress);
        ensureConnection().requestUnsubscribe(datasetAddress);
        tryTriggerMessageScheduler();
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext().getSpy().reportSpyEvent(new UnsubscribeRequestQueuedEvent(datasetAddress));
        }
    }

    boolean isSchedulerActive() {
        return _schedulerActive;
    }

    private void tryTriggerMessageScheduler() {
        context().task(this::triggerMessageScheduler);
    }

    private void validateDatasetKey(@NonNull final DatasetAddress datasetAddress) {
        if (Replicant.shouldCheckInvariants()) {
            final SystemSchema schema = getSchema();
            if (schema.hasDataset(datasetAddress.datasetId())) {
                final Dataset dataset = schema.getDataset(datasetAddress.datasetId());
                if (dataset.isKeyed()) {
                    invariant(
                            () -> null != datasetAddress.datasetKey(),
                            () -> "Replicant-0098: Dataset Address " + datasetAddress
                                    + " requires a Dataset Key but none was supplied.");
                } else {
                    invariant(
                            () -> null == datasetAddress.datasetKey(),
                            () -> "Replicant-0099: Dataset Address " + datasetAddress
                                    + " does not support Dataset Keys but one was supplied.");
                }
            }
        }
    }

    /**
     * Schedule request and response processing.
     * This method should be invoked when requests are queued or responses are received.
     */
    private void triggerMessageScheduler() {
        if (!_schedulerActive) {
            _schedulerActive = true;

            if (!_schedulerPaused) {
                activateMessageScheduler();
            }
        }
    }

    boolean isSchedulerPaused() {
        return _schedulerPaused;
    }

    void pauseMessageScheduler() {
        _schedulerPaused = true;
    }

    void resumeMessageScheduler() {
        if (_schedulerPaused) {
            _schedulerPaused = false;
            if (_schedulerActive) {
                activateMessageScheduler();
            }
        }
    }

    /**
     * Perform a single step progressing requests and responses.
     * This is invoked from the scheduler and will continue to be
     * invoked until it returns false.
     *
     * @return true if more work is to be done.
     */
    boolean progressMessages() {
        if (_schedulerPaused) {
            return false;
        }
        if (null == _schedulerLock) {
            _schedulerLock = context().pauseScheduler();
        }
        try {
            if (null != _connection && ConnectorState.DISCONNECTING != _state) {
                final boolean step1 = progressSubscriptionOperationProcessing();
                final boolean step2 = progressExecRequestProcessing();
                final boolean step3 = progressResponseProcessing();
                _schedulerActive = step1 || step2 || step3;
            } else {
                /*
                 * This can happen when a connection has been disconnected before the timer triggers
                 * that invokes progressMessages() - this can happen in a few scenarios but most of
                 * them are the result of errors occurring and connection being removed on error
                 */
                _schedulerActive = false;
                callPostMessageResponseActionIfPresent();
            }
        } catch (final Throwable e) {
            onMessageProcessFailure(e);
            _schedulerActive = false;
            releaseSchedulerLock();
            return false;
        } finally {
            if (!_schedulerActive) {
                releaseSchedulerLock();
            }
        }
        return _schedulerActive;
    }

    private void releaseSchedulerLock() {
        if (null != _schedulerLock) {
            _schedulerLock.dispose();
            _schedulerLock = null;
        }
    }

    /**
     * Activate the scheduler.
     * This involves creating a scheduler that will invoke {@link #progressMessages()} until
     * that method returns false.
     */
    private void activateMessageScheduler() {
        Scheduler.schedule(this::progressMessages);
    }

    /**
     * Perform a single step processing messages received from the server.
     *
     * @return true if more work is to be done.
     */
    boolean progressResponseProcessing() {
        final Connection connection = ensureConnection();
        final MessageResponse response = connection.getCurrentMessageResponse();
        if (null == response) {
            // Select the MessageResponse if there is none active
            return connection.selectNextMessageResponse();
        } else if (response.needsSubscriptionChangesProcessed()) {
            processSubscriptionChanges();
            return true;
        } else if (response.areEntityChangesPending()) {
            // Process a chunk of entity changes
            processEntityChanges();
            return true;
        } else if (response.areReplicaLinksPending()) {
            // Process a chunk of Replica links
            processReplicaLinks();
            return true;
        } else if (response.areReplicaUpdateActionsPending()) {
            // Process all Replica update actions. The presumption is that they do not do much
            processReplicaUpdateActions();
            return true;
        } else if (response.areOrphanSubscriptionsRemoved()) {
            // Remove all subscriptions that have been orphaned ... just in case we have some logic that triggers on
            // incoming change and queries the repository and accesses orphaned and potentially invalid Replicas.
            // This MUST be done prior to validateWorld()
            getReplicantContext().getSubscriptionReconciler().removeOrphanSubscriptions();
            response.markOrphanSubscriptionsRemoved();
            return true;
        } else if (!response.hasWorldBeenValidated()) {
            releaseSchedulerLock();
            // Validate the world after the change set has been applied (if feature is enabled)
            validateWorld();
            return true;
        } else {
            // We have to also release scheduler lock here in scenario where system not configured to validate world
            releaseSchedulerLock();
            completeMessageResponse();
            return true;
        }
    }

    @Memoize
    boolean isSynchronized() {
        return areRequestResponseQueuesEmpty() && ensureConnection().syncComplete();
    }

    boolean shouldRequestSync() {
        return areRequestResponseQueuesEmpty() && !ensureConnection().syncComplete();
    }

    private boolean areRequestResponseQueuesEmpty() {
        if (ConnectorState.CONNECTED != getState()) {
            return false;
        } else {
            final Connection connection = ensureConnection();
            return connection.getRequests().isEmpty()
                    && connection.getPendingResponses().isEmpty();
        }
    }

    @NonNull
    @Observable(readOutsideTransaction = Feature.ENABLE)
    ConnectorState getState() {
        return _state;
    }

    void setState(@NonNull final ConnectorState state) {
        _state = Objects.requireNonNull(state);
        if (ConnectorState.ERROR == _state
                || ConnectorState.FATAL_ERROR == _state
                || ConnectorState.DISCONNECTED == _state) {
            _transport.unbind();
        }
    }

    @Action
    void processSubscriptionChanges() {
        final MessageResponse response = ensureCurrentMessageResponse();

        for (final SubscriptionChange subscriptionChange : response.getSubscriptionChanges()) {
            final DatasetAddress datasetAddress = subscriptionChange.getDatasetAddress();
            final Object filterParameter = subscriptionChange.getFilterParameter();
            final SubscriptionChange.Type changeType = subscriptionChange.getType();

            if (SubscriptionChange.Type.SUBSCRIBE == changeType) {
                response.incSubscriptionSubscribeCount();
                final Subscription existingSubscription = getReplicantContext().findSubscription(datasetAddress);
                if (null != existingSubscription) {
                    final Dataset dataset = getSchema().getDataset(datasetAddress.datasetId());
                    if (dataset.hasFixedFilterParameter()
                            && !FilterParameterUtil.filterParametersEqual(
                                    filterParameter, existingSubscription.getFilterParameter())) {
                        Disposable.dispose(existingSubscription);
                    }
                }
                final SubscriptionMode mode = getReplicantContext().getAreasOfInterest().stream()
                                .anyMatch(a -> a.getDatasetAddress().equals(datasetAddress))
                        ? SubscriptionMode.EXPLICIT
                        : SubscriptionMode.IMPLICIT;
                getReplicantContext()
                        .getSubscriptionService()
                        .createSubscription(datasetAddress, filterParameter, mode);
            } else if (SubscriptionChange.Type.UNSUBSCRIBE == changeType
                    || SubscriptionChange.Type.INVALIDATE_DATASET_ADDRESS == changeType) {
                final Subscription subscription = getReplicantContext().findSubscription(datasetAddress);
                /*
                 * A Subscription may already be absent when an unsubscribe or Dataset Address Invalidation arrives.
                 * This can occur when an application deletes an Instance Dataset's Dataset Root and removes its Area
                 * of Interest concurrently.
                 */
                if (null != subscription) {
                    Disposable.dispose(subscription);
                }

                final AreaOfInterest areaOfInterest =
                        getReplicantContext().findAreaOfInterestByDatasetAddress(datasetAddress);
                if (null != areaOfInterest) {
                    if (SubscriptionChange.Type.INVALIDATE_DATASET_ADDRESS == changeType) {
                        areaOfInterest.updateAreaOfInterest(AreaOfInterest.Status.DATASET_ADDRESS_INVALIDATED, null);
                    } else {
                        // This means the Subscription was removed on the server side
                        // We dispose it locally and assume that whatever component create AreaOfInterest can respond
                        // appropriately
                        Disposable.dispose(areaOfInterest);
                    }
                }
                response.incSubscriptionUnsubscribeCount();
            } else {
                assert SubscriptionChange.Type.UPDATE == changeType;
                final Subscription subscription = getReplicantContext().findSubscription(datasetAddress);
                if (Replicant.shouldCheckInvariants()) {
                    invariant(
                            () -> null != subscription,
                            () -> "Replicant-0033: Received SubscriptionChange of type UPDATE for Dataset Address "
                                    + datasetAddress + " but no such subscription exists.");
                    assert null != subscription;
                    if (Replicant.shouldCheckInvariants()) {
                        invariant(
                                () -> {
                                    return getSchema()
                                            .getDataset(datasetAddress.datasetId())
                                            .hasUpdatableFilterParameter();
                                },
                                () -> "Replicant-0078: Received SubscriptionChange of type UPDATE for Dataset Address "
                                        + datasetAddress
                                        + " but the Dataset does not have an updatable Filter Parameter.");
                    }
                }
                final Subscription existingSubscription = Objects.requireNonNull(subscription);
                existingSubscription.setFilterParameter(filterParameter);
                reevaluateReplicaMembershipAfterFilterParameterUpdate(existingSubscription);
                response.incSubscriptionUpdateCount();
            }
        }
        response.markSubscriptionChangesProcessed();
    }

    @Action(verifyRequired = false)
    void processReplicaLinks() {
        final MessageResponse response = ensureCurrentMessageResponse();
        Linkable linkable;
        for (int i = 0; i < _linksToProcessPerTick && null != (linkable = response.nextReplicaToLink()); i++) {
            linkable.link();
            response.incEntityLinkCount();
        }
    }

    @Action(verifyRequired = false)
    void processReplicaUpdateActions() {
        final MessageResponse response = ensureCurrentMessageResponse();
        final OnReplicaUpdateAction action = getSchema().getOnReplicaUpdateAction();
        if (null != action) {
            Object replica;
            while (null != (replica = response.nextReplicaToPostAction())) {
                action.onReplicaUpdate(getReplicantContext(), replica);
            }
        } else {
            response.completePostActions();
        }
    }

    /**
     * Re-evaluate Replica membership after a Filter Parameter update and delink any Replicas that no longer match.
     *
     * @param subscription the subscription that was updated.
     */
    @SuppressWarnings("unchecked")
    void reevaluateReplicaMembershipAfterFilterParameterUpdate(@NonNull final Subscription subscription) {
        final DatasetAddress datasetAddress = subscription.datasetAddress();
        final Dataset dataset = getSchema().getDataset(datasetAddress.datasetId());
        if (Replicant.shouldCheckInvariants()) {
            invariant(
                    dataset::hasUpdatableFilterParameter,
                    () -> "Replicant-0079: Connector.reevaluateReplicaMembershipAfterFilterParameterUpdate invoked"
                            + " for Dataset Address "
                            + subscription.datasetAddress()
                            + " but the Dataset does not have an updatable Filter Parameter.");
        }

        final List<ReplicaEntry> replicaEntriesToDelink = new ArrayList<>();
        for (final Class<?> replicaType : new ArrayList<>(subscription.findAllReplicaTypes())) {
            final List<ReplicaEntry> replicaEntries = subscription.findAllReplicaEntriesByType(replicaType);
            if (!replicaEntries.isEmpty()) {
                @SuppressWarnings("rawtypes")
                final FilterParameterUpdateReplicaMatcher matcher =
                        Objects.requireNonNull(dataset.getFilterParameterUpdateReplicaMatcher());
                final Object filterParameter = subscription.getFilterParameter();
                for (final ReplicaEntry replicaEntry : replicaEntries) {
                    if (!matcher.doesReplicaMatchFilterParameter(filterParameter, replicaEntry)) {
                        // We need to collect all the Replica Entries into a separate list and delink later.
                        // If we delink immediately and the Arez Replica is disposed and a subsequent Replica
                        // calls `doesReplicaMatchFilterParameter` and tries to traverse across the already disposed
                        // Replica,
                        // then we get a crash or unexpected behaviour.
                        // i.e. Moving days in planner will match the day first and remove it before attempting
                        // to match RosterEntry but RosterEntry involves walking from RosterEntry->Day->Shift
                        // which will crash
                        replicaEntriesToDelink.add(replicaEntry);
                    }
                }
            }
        }

        for (final ReplicaEntry replicaEntry : replicaEntriesToDelink) {
            replicaEntry.delinkFromSubscription(subscription);
        }
    }

    void setPostMessageResponseAction(@Nullable final SafeProcedure postMessageResponseAction) {
        _postMessageResponseAction = postMessageResponseAction;
    }

    void completeMessageResponse() {
        final Connection connection = ensureConnection();
        final MessageResponse response = connection.ensureCurrentMessageResponse();

        // Step: Run the post actions
        final RequestEntry request = response.getRequest();
        final ServerToClientMessage message = response.getMessage();
        final Integer requestId = message.getRequestId();

        final ExecRequest execRequest = null != requestId ? ensureConnection().getActiveExecRequest(requestId) : null;
        if (null != execRequest && null != request && message instanceof UpdateMessage) {
            @SuppressWarnings("PatternVariableCanBeUsed")
            final UpdateMessage updateMessage = (UpdateMessage) message;
            final ResponseHandler responseHandler = request.getResponseHandler();
            if (null != responseHandler) {
                responseHandler.onResponse(Objects.requireNonNull(updateMessage.getResponse()));
            }
        }

        // We can remove the request because this side ran second and the RPC channel has already returned.
        if (null != requestId) {
            connection.removeRequest(requestId);
        }
        connection.setCurrentMessageResponse(null);
        if (null != execRequest) {
            final int completedRequestId = Objects.requireNonNull(requestId);
            connection.markExecRequestAsComplete(completedRequestId);
            onExecCompleted(execRequest.getCommand(), completedRequestId);
        }
        onMessageProcessed(response);
        callPostMessageResponseActionIfPresent();

        if (null != request) {
            final List<SubscriptionOperation> requests = connection.getActiveSubscriptionOperations();
            if (!requests.isEmpty()) {
                if (requests.get(0).getRequestId() == request.getRequestId()) {
                    completeSubscriptionOperations(requests);
                }
            }
        }
        //noinspection IfCanBeSwitch
        if (OkMessage.TYPE.equals(message.getType())) {
            if (null != requestId && connection.getLastRxSyncRequestId() == requestId) {
                if (connection.syncComplete()) {
                    onInSync();
                    getReplicantContext().getSubscriptionReconciler().removeOrphanSubscriptions();
                } else {
                    onOutOfSync();
                }
                triggerMessageScheduler();
            }
        } else if (UpdateMessage.TYPE.equals(message.getType())) {
            // If message is not a ping response then try to perform sync
            maybeRequestSync();
            final UpdateMessage updateMessage = (UpdateMessage) message;
            if (null != updateMessage.getETag()) {
                cacheMessageIfPossible(response, updateMessage);
            }
        } else if (ErrorMessage.TYPE.equals(message.getType())) {
            final ErrorMessage errorMessage = (ErrorMessage) message;
            final String m = errorMessage.getMessage();
            final String text = null == m ? "" : m;
            if (text.startsWith("java.lang.SecurityException:")) {
                fatalError();
            }
        }
    }

    @Action
    void fatalError() {
        setState(ConnectorState.FATAL_ERROR);
    }

    // This is in an action so that completeSubscriptionOperation() is called observers can react to status changes in
    // AreaOfInterest
    @Action(reportParameters = false)
    void completeSubscriptionOperations(@NonNull final List<SubscriptionOperation> subscriptionOperations) {
        subscriptionOperations.forEach(subscriptionOperation -> {
            final DatasetAddress datasetAddress = subscriptionOperation.getDatasetAddress();
            final SubscriptionOperation.Type type = subscriptionOperation.getType();
            if (SubscriptionOperation.Type.SUBSCRIBE == type) {
                onSubscribeCompleted(datasetAddress);
            } else if (SubscriptionOperation.Type.UNSUBSCRIBE == type) {
                transitionSubscriptionsToImplicitMode(Collections.singletonList(subscriptionOperation));
                onUnsubscribeCompleted(datasetAddress);
            } else {
                assert SubscriptionOperation.Type.UPDATE == type;
                onSubscriptionUpdateCompleted(datasetAddress);
            }
        });
        completeSubscriptionOperation();
    }

    void maybeRequestSync() {
        if (shouldRequestSync()) {
            requestSync();
        }
    }

    private void callPostMessageResponseActionIfPresent() {
        if (null != _postMessageResponseAction) {
            _postMessageResponseAction.call();
            _postMessageResponseAction = null;
        }
    }

    @Action
    void transitionSubscriptionsToImplicitMode(@NonNull final List<SubscriptionOperation> subscriptionOperations) {
        subscriptionOperations.forEach(subscriptionOperation -> {
            if (Replicant.shouldCheckInvariants()) {
                invariant(
                        () -> SubscriptionOperation.Type.UNSUBSCRIBE == subscriptionOperation.getType(),
                        () -> "Replicant-0034: Connector.transitionSubscriptionsToImplicitMode() invoked with "
                                + "Subscription Operation with type that is not UNSUBSCRIBE. Operation: "
                                + subscriptionOperation);
            }
            final Subscription subscription =
                    getReplicantContext().findSubscription(subscriptionOperation.getDatasetAddress());
            if (null != subscription) {
                subscription.setMode(SubscriptionMode.IMPLICIT);
            }
        });
    }

    @Action(verifyRequired = false)
    void removeUnneededUpdateRequests(@NonNull final List<SubscriptionOperation> requests) {
        requests.removeIf(a -> {
            final DatasetAddress datasetAddress = a.getDatasetAddress();
            final Subscription subscription = getReplicantContext().findSubscription(datasetAddress);
            if (Replicant.shouldCheckInvariants()) {
                invariant(
                        () -> null != subscription,
                        () -> "Replicant-0048: Request to update Subscription at Dataset Address " + datasetAddress
                                + " but no Subscription exists.");
            }
            // The following code can probably be removed but it was present in the previous system
            // and it is unclear if there is any scenarios where it can still happen. The code has
            // been left in until we can verify it is no longer an issue. The above invariants will trigger
            // in development mode to help us track down these scenarios
            if (null == subscription) {
                a.markAsComplete();
                return true;
            } else {
                return false;
            }
        });
    }

    @Action(verifyRequired = false)
    void removeUnneededRemoveRequests(@NonNull final List<SubscriptionOperation> requests) {
        requests.removeIf(request -> {
            final DatasetAddress datasetAddress = request.getDatasetAddress();
            final Subscription subscription = getReplicantContext().findSubscription(datasetAddress);
            if (Replicant.shouldCheckInvariants()) {
                invariant(
                        () -> null != subscription,
                        () -> "Replicant-0046: Request to unsubscribe at Dataset Address " + datasetAddress
                                + " but no Subscription exists.");
                invariant(
                        () -> null == subscription || SubscriptionMode.EXPLICIT == subscription.getMode(),
                        () -> "Replicant-0047: Request to unsubscribe at Dataset Address " + datasetAddress
                                + " but Subscription is not in Explicit Subscription Mode.");
            }
            // The following code can probably be removed but it was present in the previous system
            // and it is unclear if there is any scenarios where it can still happen. The code has
            // been left in until we can verify it is no longer an issue. The above invariants will trigger
            // in development mode to help us track down these scenarios
            if (null == subscription || SubscriptionMode.EXPLICIT != subscription.getMode()) {
                // We were getting here if a Dataset Address Invalidation was reported after removal of its Dataset
                // Root was delivered to the client. That delivery explicitly unsubscribes, which gets sent back a
                // successful unsubscribe even though the Subscription had already been orphaned or invalidated.
                request.markAsComplete();
                return true;
            } else {
                return false;
            }
        });
    }

    private void cacheMessageIfPossible(
            @NonNull final MessageResponse response, @NonNull final UpdateMessage changeSet) {
        final String eTag = changeSet.getETag();
        final CacheService cacheService = getReplicantContext().getCacheService();

        boolean candidate = false;
        if (null != cacheService
                && null != eTag
                && (changeSet.hasSubscriptionChanges() || changeSet.hasFilterParameterSubscriptionChanges())) {
            final List<SubscriptionChange> subscriptionChanges = response.getSubscriptionChanges();

            if (1 == subscriptionChanges.size()
                    && SubscriptionChange.Type.SUBSCRIBE
                            == subscriptionChanges.get(0).getType()
                    && getSchema()
                            .getDataset(subscriptionChanges
                                    .get(0)
                                    .getDatasetAddress()
                                    .datasetId())
                            .isCacheable()) {
                final DatasetAddress datasetAddress = subscriptionChanges.get(0).getDatasetAddress();
                cacheService.store(datasetAddress, eTag, changeSet);
                candidate = true;
            }
        }
        if (Replicant.shouldCheckApiInvariants()) {
            final boolean c = candidate;
            apiInvariant(
                    () -> null == eTag || null == cacheService || c,
                    () -> "Replicant-0072: eTag in reply for ChangeSet but ChangeSet is not a candidate for caching.");
        }
    }

    @SuppressWarnings("unchecked")
    @Action
    void processEntityChanges() {
        final MessageResponse response = ensureCurrentMessageResponse();
        EntityChange change;
        for (int i = 0; i < _changesToProcessPerTick && null != (change = response.nextEntityChange()); i++) {
            final String id = change.getId();
            final int idSeparator = id.indexOf(".");
            if (idSeparator <= 0 || idSeparator >= id.length() - 1) {
                onMessageProcessFailure(new IllegalArgumentException("Invalid entity id format: '" + id + "'"));
                return;
            }
            final int typeId;
            final int entityId;
            try {
                typeId = Integer.parseInt(id.substring(0, idSeparator));
                entityId = Integer.parseInt(id.substring(idSeparator + 1));
            } catch (final Throwable t) {
                onMessageProcessFailure(t);
                return;
            }
            final EntityType entityType = getSchema().getEntityType(typeId);
            final Class<?> type = entityType.getType();
            ReplicaEntry replicaEntry =
                    getReplicantContext().getReplicaRegistry().findReplicaEntryByTypeAndId(type, entityId);
            if (change.isRemove()) {
                /*
                 * Sometimes a remove can occur for an entity that is no longer present on the client. The most
                 * common cause of this is initiating an action that deletes an entity and then un-subscribing
                 * from the Subscription that contains the Entity. This can result in an Entity that has been removed
                 * locally but has a remove message in the queue. Other interleaved async operations can also
                 * trigger this scenario.
                 */
                if (null != replicaEntry) {
                    Disposable.dispose(replicaEntry);
                    response.incEntityRemoveCount();
                }
            } else {
                final EntityChangeData data = change.getData();
                if (null == replicaEntry) {
                    final String name = Replicant.areNamesEnabled() ? entityType.getName() + "/" + entityId : null;
                    replicaEntry =
                            getReplicantContext().getReplicaRegistry().findOrCreateReplicaEntry(name, type, entityId);
                    final Object replica = entityType.getCreator().createReplica(entityId, data);
                    replicaEntry.setReplica(replica);
                } else {
                    @SuppressWarnings("rawtypes")
                    final EntityType.Updater updater = entityType.getUpdater();
                    if (null != updater) {
                        updater.updateReplica(replicaEntry.getReplica(), data);
                    }
                }

                final String[] datasetAddressDescriptors = change.getDatasetAddresses();
                final int schemaId = getSchema().getId();
                for (final String datasetAddressDescriptor : datasetAddressDescriptors) {
                    try {
                        final DatasetAddress datasetAddress = DatasetAddress.parse(schemaId, datasetAddressDescriptor);
                        final Subscription subscription = getReplicantContext().findSubscription(datasetAddress);
                        if (Replicant.shouldCheckInvariants()) {
                            invariant(
                                    () -> null != subscription,
                                    () -> "Replicant-0069: UpdateMessage contained an EntityChange message"
                                            + " referencing Dataset Address "
                                            + datasetAddress + " but no such subscription exists locally.");
                        }
                        if (null != subscription) {
                            replicaEntry.tryLinkToSubscription(subscription);
                        } else {
                            onOutOfSync();
                            return;
                        }
                    } catch (final Throwable t) {
                        if (t instanceof IllegalStateException) {
                            throw (IllegalStateException) t;
                        }
                        onMessageProcessFailure(t);
                        return;
                    }
                }
                /*
                We could get the existing subscriptions for a Replica Entry, and any that are not present
                in the Entity change could be removed here. However we assume the code generated in
                subscription change will handle subscription changes and remove subscriptions no longer
                relevant.
                */

                response.incEntityUpdateCount();
                response.replicaProcessed(replicaEntry.getReplica());
            }
        }
    }

    void validateWorld() {
        ensureCurrentMessageResponse().markWorldAsValidated();
        if (Replicant.shouldValidateReplicasOnLoad()) {
            getReplicantContext().getValidator().validateReplicas();
        }
    }

    /**
     * Perform a single step in sending one (or a batch) or requests to the server.
     */
    boolean progressSubscriptionOperationProcessing() {
        final List<SubscriptionOperation> requests =
                new ArrayList<>(ensureConnection().getCurrentSubscriptionOperations());
        if (requests.isEmpty()) {
            return false;
        } else if (requests.get(0).isInProgress()) {
            return false;
        } else {
            final SubscriptionOperation.Type type = requests.get(0).getType();
            if (SubscriptionOperation.Type.SUBSCRIBE == type) {
                progressAreaOfInterestAddRequests(requests);
            } else if (SubscriptionOperation.Type.UNSUBSCRIBE == type) {
                progressAreaOfInterestRemoveRequests(requests);
            } else {
                progressAreaOfInterestUpdateRequests(requests);
            }
            return true;
        }
    }

    boolean progressExecRequestProcessing() {
        final ExecRequest request = ensureConnection().nextExecRequest();
        if (null == request) {
            return false;
        } else {
            final String command = request.getCommand();

            _transport.requestExec(command, request.getPayload(), request.getResponseHandler());
            request.markAsInProgress(ensureConnection().getLastTxRequestId());
            ensureConnection().recordActiveExecRequest(request);

            onExecStarted(command, request.getRequestId());
            return true;
        }
    }

    void progressAreaOfInterestAddRequests(@NonNull final List<SubscriptionOperation> requests) {
        // We very deliberately do not strip out requests even if there is a local subscription.
        // If the local subscription matched exactly the request would not make it to here and
        // If an Area of Interest is moving a Subscription from Implicit to Explicit Subscription Mode, let the
        // to let it flow through to backend so that the backend knows that the subscription has
        // server observe the mode transition.
        if (requests.isEmpty()) {
            completeSubscriptionOperation();
        } else if (1 == requests.size()) {
            progressAreaOfInterestAddRequest(requests.get(0));
        } else {
            progressBulkAreaOfInterestAddRequests(requests);
        }
    }

    void progressAreaOfInterestAddRequest(@NonNull final SubscriptionOperation request) {
        final DatasetAddress datasetAddress = request.getDatasetAddress();
        onSubscribeStarted(datasetAddress);

        _transport.requestSubscribe(request.getDatasetAddress(), request.getFilterParameter());
        request.markAsInProgress(ensureConnection().getLastTxRequestId());
    }

    void progressBulkAreaOfInterestAddRequests(@NonNull final List<SubscriptionOperation> requests) {
        final List<DatasetAddress> datasetAddresses =
                requests.stream().map(SubscriptionOperation::getDatasetAddress).collect(Collectors.toList());
        datasetAddresses.forEach(this::onSubscribeStarted);

        _transport.requestBulkSubscribe(datasetAddresses, requests.get(0).getFilterParameter());
        final int requestId = ensureConnection().getLastTxRequestId();
        requests.forEach(r -> r.markAsInProgress(requestId));
    }

    void progressAreaOfInterestUpdateRequests(@NonNull final List<SubscriptionOperation> requests) {
        removeUnneededUpdateRequests(requests);

        if (requests.isEmpty()) {
            completeSubscriptionOperation();
        } else if (requests.size() > 1) {
            progressBulkAreaOfInterestUpdateRequests(requests);
        } else {
            progressAreaOfInterestUpdateRequest(requests.get(0));
        }
    }

    void progressAreaOfInterestUpdateRequest(@NonNull final SubscriptionOperation request) {
        final DatasetAddress datasetAddress = request.getDatasetAddress();
        onSubscriptionUpdateStarted(datasetAddress);

        final Object filterParameter = request.getFilterParameter();
        assert null != filterParameter;
        _transport.requestSubscribe(datasetAddress, filterParameter);
        final int requestId = ensureConnection().getLastTxRequestId();
        request.markAsInProgress(requestId);
    }

    void progressBulkAreaOfInterestUpdateRequests(@NonNull final List<SubscriptionOperation> requests) {
        final List<DatasetAddress> datasetAddresses =
                requests.stream().map(SubscriptionOperation::getDatasetAddress).collect(Collectors.toList());
        datasetAddresses.forEach(this::onSubscriptionUpdateStarted);

        // All Filter Parameters will be the same if they are grouped
        final Object filterParameter = requests.get(0).getFilterParameter();
        assert null != filterParameter;
        _transport.requestBulkSubscribe(datasetAddresses, filterParameter);
        final int requestId = ensureConnection().getLastTxRequestId();
        requests.forEach(r -> r.markAsInProgress(requestId));
    }

    void progressAreaOfInterestRemoveRequests(@NonNull final List<SubscriptionOperation> requests) {
        removeUnneededRemoveRequests(requests);

        if (requests.isEmpty()) {
            completeSubscriptionOperation();
        } else if (requests.size() > 1) {
            progressBulkAreaOfInterestRemoveRequests(requests);
        } else {
            progressAreaOfInterestRemoveRequest(requests.get(0));
        }
    }

    void progressAreaOfInterestRemoveRequest(@NonNull final SubscriptionOperation request) {
        final DatasetAddress datasetAddress = request.getDatasetAddress();
        onUnsubscribeStarted(datasetAddress);

        _transport.requestUnsubscribe(datasetAddress);
        request.markAsInProgress(ensureConnection().getLastTxRequestId());
    }

    void progressBulkAreaOfInterestRemoveRequests(@NonNull final List<SubscriptionOperation> requests) {
        final List<DatasetAddress> datasetAddresses =
                requests.stream().map(SubscriptionOperation::getDatasetAddress).collect(Collectors.toList());
        datasetAddresses.forEach(this::onUnsubscribeStarted);

        _transport.requestBulkUnsubscribe(datasetAddresses);
        final int requestId = ensureConnection().getLastTxRequestId();
        requests.forEach(r -> r.markAsInProgress(requestId));
    }

    /**
     * The SubscriptionOperation currently being processed can be completed and
     * trigger scheduler to start next step.
     */
    void completeSubscriptionOperation() {
        /*
         * Sometimes an SubscriptionOperation completes during a disconnection or network failure.
         * i.e. This could be called in response to an error as a result of network failure or it could
         * overlap a disconnect request.
         */
        if (null != _connection) {
            _connection.completeSubscriptionOperation();
        }
        triggerMessageScheduler();
    }

    /**
     * Invoked to fire an event when disconnect has completed.
     */
    @Action
    void onConnected() {
        setState(ConnectorState.CONNECTED);
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(
                            new ConnectedEvent(getSchema().getId(), getSchema().getName()));
        }
    }

    /**
     * Invoked to fire an event when failed to connect.
     */
    @Action
    void onConnectFailure() {
        setState(ConnectorState.ERROR);
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new ConnectFailureEvent(
                            getSchema().getId(), getSchema().getName()));
        }
    }

    /**
     * Invoked to fire an event when disconnect has completed.
     */
    @Action
    void onDisconnected() {
        setState(ConnectorState.DISCONNECTED);
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new DisconnectedEvent(
                            getSchema().getId(), getSchema().getName()));
        }
    }

    /**
     * Invoked to fire an event when failed to connect.
     */
    @Action
    void onDisconnectFailure() {
        setState(ConnectorState.ERROR);
        doSetConnection(null);
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new DisconnectFailureEvent(
                            getSchema().getId(), getSchema().getName()));
        }
    }

    /**
     * Invoked when a transport received a message.
     *
     * @param message the message.
     */
    void onMessageReceived(@NonNull final ServerToClientMessage message) {
        final Connection connection = ensureConnection();

        final Integer requestId = message.getRequestId();
        final RequestEntry request = null != requestId ? connection.getRequest(requestId) : null;

        final ServerToClientMessage messageToQueue;
        if (UseCacheMessage.TYPE.equals(message.getType())) {
            final UseCacheMessage useCacheMessage = (UseCacheMessage) message;
            final String datasetAddressDescriptor = useCacheMessage.getDatasetAddress();
            final DatasetAddress datasetAddress;
            try {
                datasetAddress = DatasetAddress.parse(getSchema().getId(), datasetAddressDescriptor);
            } catch (final Throwable t) {
                onMessageProcessFailure(t);
                return;
            }
            final String etag = useCacheMessage.getEtag();

            final CacheService cacheService = getReplicantContext().getCacheService();
            if (null == cacheService) {
                ReplicantLogger.log(
                        "Received a use-cache message for Dataset Address " + datasetAddress
                                + " but no cache service configured.",
                        null);
                onMessageReadFailure();
                return;
            }

            final CacheEntry entry = cacheService.lookup(datasetAddress);
            if (null == entry) {
                ReplicantLogger.log(
                        "Received a use-cache message for Dataset Address " + datasetAddressDescriptor
                                + " but no cache entry is present.",
                        null);
                onMessageReadFailure();
                return;
            }
            if (!Objects.equals(entry.getETag(), etag)) {
                ReplicantLogger.log(
                        "Received a use-cache message for Dataset Address " + datasetAddressDescriptor + " with etag '"
                                + etag
                                + "' but cache entry has etag '" + entry.getETag() + "'.",
                        null);
                onMessageReadFailure();
                return;
            }
            try {
                messageToQueue =
                        Objects.requireNonNull(JSON.parse(entry.getContent())).cast();
            } catch (final Throwable t) {
                onMessageProcessFailure(t);
                return;
            }
            messageToQueue.setRequestId(requestId);
        } else {
            messageToQueue = message;
        }

        connection.enqueueResponse(messageToQueue, request);
        triggerMessageScheduler();
    }

    /**
     * Invoked when a change set has been completely processed.
     *
     * @param response the message response.
     */
    void onMessageProcessed(@NonNull final MessageResponse response) {
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new MessageProcessedEvent(
                            getSchema().getId(), getSchema().getName(), response.toStatus()));
        }
    }

    /**
     * Invoked when an exec has been sent to the server.
     *
     * @param command the exec request command.
     */
    void onExecStarted(@NonNull final String command, final int requestId) {
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new ExecStartedEvent(
                            getSchema().getId(), getSchema().getName(), command, requestId));
        }
    }

    /**
     * Invoked when an exec has been sent to the server.
     *
     * @param command the exec request command.
     */
    void onExecCompleted(@NonNull final String command, final int requestId) {
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new ExecCompletedEvent(
                            getSchema().getId(), getSchema().getName(), command, requestId));
        }
    }

    /**
     * Called when a data load has resulted in a failure.
     */
    @Action(verifyRequired = false)
    void onMessageProcessFailure(@NonNull final Throwable error) {
        final String message = ReplicantUtil.safeGetString(() -> "Exception processing replicant message.");
        ReplicantLogger.log(message, error);
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new MessageProcessFailureEvent(
                            getSchema().getId(), getSchema().getName(), error));
        }
        disconnectIfPossible();
    }

    /**
     * Attempted to retrieve data from backend and failed.
     */
    @Action(verifyRequired = false)
    void onMessageReadFailure() {
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new MessageReadFailureEvent(
                            getSchema().getId(), getSchema().getName()));
        }
        disconnectIfPossible();
    }

    void disconnectIfPossible() {
        if (!ConnectorState.isTransitionState(getState())) {
            if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
                getReplicantContext()
                        .getSpy()
                        .reportSpyEvent(new RestartEvent(
                                getSchema().getId(), getSchema().getName()));
            }
            disconnect();
        }
    }

    void onInSync() {
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new InSyncEvent(getSchema().getId()));
        }
    }

    void onOutOfSync() {
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new OutOfSyncEvent(getSchema().getId()));
        }
    }

    @Action
    void onSubscribeStarted(@NonNull final DatasetAddress datasetAddress) {
        updateAreaOfInterest(datasetAddress, AreaOfInterest.Status.LOADING);
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new SubscribeStartedEvent(
                            getSchema().getId(), getSchema().getName(), datasetAddress));
        }
    }

    @Action
    void onSubscribeCompleted(@NonNull final DatasetAddress datasetAddress) {
        final Subscription subscription = getReplicantContext().findSubscription(datasetAddress);
        if (null != subscription) {
            subscription.setMode(SubscriptionMode.EXPLICIT);
        }
        final AreaOfInterest areaOfInterest = getReplicantContext().findAreaOfInterestByDatasetAddress(datasetAddress);
        if (null != areaOfInterest && AreaOfInterest.Status.DATASET_ADDRESS_INVALIDATED != areaOfInterest.getStatus()) {
            areaOfInterest.updateAreaOfInterest(AreaOfInterest.Status.LOADED, null);
        }
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new SubscribeCompletedEvent(
                            getSchema().getId(), getSchema().getName(), datasetAddress));
        }
    }

    @Action
    void onUnsubscribeStarted(@NonNull final DatasetAddress datasetAddress) {
        updateAreaOfInterest(datasetAddress, AreaOfInterest.Status.UNLOADING);
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new UnsubscribeStartedEvent(
                            getSchema().getId(), getSchema().getName(), datasetAddress));
        }
    }

    @Action
    void onUnsubscribeCompleted(@NonNull final DatasetAddress datasetAddress) {
        updateAreaOfInterest(datasetAddress, AreaOfInterest.Status.UNLOADED);
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new UnsubscribeCompletedEvent(
                            getSchema().getId(), getSchema().getName(), datasetAddress));
        }
    }

    @Action
    void onSubscriptionUpdateStarted(@NonNull final DatasetAddress datasetAddress) {
        updateAreaOfInterest(datasetAddress, AreaOfInterest.Status.UPDATING);
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new SubscriptionUpdateStartedEvent(
                            getSchema().getId(), getSchema().getName(), datasetAddress));
        }
    }

    @Action
    void onSubscriptionUpdateCompleted(@NonNull final DatasetAddress datasetAddress) {
        updateAreaOfInterest(datasetAddress, AreaOfInterest.Status.UPDATED);
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new SubscriptionUpdateCompletedEvent(
                            getSchema().getId(), getSchema().getName(), datasetAddress));
        }
    }

    private void updateAreaOfInterest(
            @NonNull final DatasetAddress datasetAddress, final AreaOfInterest.@NonNull Status status) {
        final AreaOfInterest areaOfInterest = getReplicantContext().findAreaOfInterestByDatasetAddress(datasetAddress);
        if (null != areaOfInterest) {
            areaOfInterest.updateAreaOfInterest(status, null);
        }
    }

    @NonNull
    ReplicantRuntime getReplicantRuntime() {
        return getReplicantContext().getRuntime();
    }

    @ContextRef
    @NonNull
    abstract ArezContext context();

    @Override
    public String toString() {
        return Replicant.areNamesEnabled() ? "Connector[" + getSchema().getName() + "]" : super.toString();
    }

    @Nullable
    Disposable getSchedulerLock() {
        return _schedulerLock;
    }

    @Nullable
    SafeProcedure getPostMessageResponseAction() {
        return _postMessageResponseAction;
    }

    @Nullable
    Connection getConnection() {
        return _connection;
    }

    @NonNull
    Transport getTransport() {
        return _transport;
    }
}
