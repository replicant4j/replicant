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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.messages.ChangeSetMessage;
import replicant.messages.EntityChange;
import replicant.messages.EntityChangePayload;
import replicant.messages.ErrorMessage;
import replicant.messages.OkMessage;
import replicant.messages.ServerToClientMessage;
import replicant.messages.UseDatasetCacheEntryMessage;
import replicant.spy.ConnectFailureEvent;
import replicant.spy.ConnectedEvent;
import replicant.spy.DisconnectFailureEvent;
import replicant.spy.DisconnectedEvent;
import replicant.spy.ExecCompletedEvent;
import replicant.spy.ExecRequestQueuedEvent;
import replicant.spy.ExecStartedEvent;
import replicant.spy.MessageProcessFailureEvent;
import replicant.spy.MessageProcessedEvent;
import replicant.spy.MessageReadFailureEvent;
import replicant.spy.RestartEvent;
import replicant.spy.SubscribeCompletedEvent;
import replicant.spy.SubscribeRequestQueuedEvent;
import replicant.spy.SubscribeStartedEvent;
import replicant.spy.SubscriptionUpdateCompletedEvent;
import replicant.spy.SubscriptionUpdateRequestQueuedEvent;
import replicant.spy.SubscriptionUpdateStartedEvent;
import replicant.spy.SynchronizationPointPendingEvent;
import replicant.spy.SynchronizationPointReachedEvent;
import replicant.spy.SynchronizationPointRequestedEvent;
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
     * The System Schema that defines the replicated system exposed by the data source.
     */
    @NonNull
    private final SystemSchema _systemSchema;
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
     * Maximum number of Entity Changes processed in a single tick of the scheduler. After this many changes have
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

    /**
     * Dataset Addresses whose rejected Dataset Cache Entry must not be advertised until replaced by a successful
     * fresh store.
     */
    @NonNull
    private final Set<DatasetAddress> _rejectedDatasetCacheEntryAddresses = new HashSet<>();

    @NonNull
    static Connector create(
            @Nullable final ReplicantContext context,
            @NonNull final SystemSchema systemSchema,
            @NonNull final Transport transport) {
        return new Arez_Connector(context, systemSchema, transport);
    }

    Connector(
            @Nullable final ReplicantContext context,
            @NonNull final SystemSchema systemSchema,
            @NonNull final Transport transport) {
        super(context);
        _systemSchema = Objects.requireNonNull(systemSchema);
        _transport = Objects.requireNonNull(transport);
    }

    @PostConstruct
    void postConstruct() {
        getReplicantRuntime().registerConnector(this);
        getReplicantContext().getSystemSchemaService().registerSystemSchema(_systemSchema);
    }

    @PreDispose
    void preDispose() {
        _schedulerPaused = true;
        _schedulerActive = false;
        releaseSchedulerLock();
        getReplicantContext().getSystemSchemaService().deregisterSystemSchema(_systemSchema);
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
     * Return the System Schema associated with the connector.
     *
     * @return the System Schema associated with the connector.
     */
    @NonNull
    SystemSchema getSystemSchema() {
        return _systemSchema;
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
                sendDatasetCacheVersionsIfAny();
                onConnected();
            } else {
                onDisconnected();
            }
        }
        schedulerLock.dispose();
    }

    private void sendDatasetCacheVersionsIfAny() {
        final DatasetCacheService datasetCacheService = getReplicantContext().getDatasetCacheService();
        if (null != datasetCacheService) {
            final List<DatasetAddress> datasetAddresses;
            try {
                datasetAddresses = new ArrayList<>(datasetCacheService.getDatasetAddresses(
                        getSystemSchema().getId()));
            } catch (final Throwable t) {
                ReplicantLogger.log(
                        "Failed to enumerate Dataset Cache Entries for "
                                + getSystemSchema().getName() + ".",
                        t);
                return;
            }
            final HashMap<String, String> datasetCacheVersions = new HashMap<>();
            for (final DatasetAddress datasetAddress : datasetAddresses) {
                if (!_rejectedDatasetCacheEntryAddresses.contains(datasetAddress)) {
                    try {
                        final String datasetCacheVersion =
                                datasetCacheService.lookupDatasetCacheVersion(datasetAddress);
                        if (null == datasetCacheVersion) {
                            rejectDatasetCacheEntry(
                                    datasetCacheService, datasetAddress, "Dataset Cache Version is absent.", null);
                        } else {
                            datasetCacheVersions.put(datasetAddress.asDatasetAddressDescriptor(), datasetCacheVersion);
                        }
                    } catch (final Throwable t) {
                        rejectDatasetCacheEntry(
                                datasetCacheService, datasetAddress, "Dataset Cache Version is unreadable.", t);
                    }
                }
            }
            if (!datasetCacheVersions.isEmpty()) {
                _transport.updateDatasetCacheVersionsAndRequestSynchronizationPoint(datasetCacheVersions);
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
                .filter(s ->
                        s.datasetAddress().systemSchemaId() == getSystemSchema().getId())
                // Purge in reverse order. First Instance Dataset subscriptions then Type Dataset subscriptions
                .sorted(Comparator.reverseOrder())
                .forEachOrdered(Disposable::dispose);
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

    void requestSynchronizationPoint() {
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new SynchronizationPointRequestedEvent(
                            getSystemSchema().getId()));
        }
        _transport.requestSynchronizationPoint();
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
                            getSystemSchema().getId(), getSystemSchema().getName(), command));
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
                        return getSystemSchema()
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
            final SystemSchema systemSchema = getSystemSchema();
            if (systemSchema.hasDataset(datasetAddress.datasetId())) {
                final Dataset dataset = systemSchema.getDataset(datasetAddress.datasetId());
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
            // This MUST be done prior to validating Replicas.
            getReplicantContext().getSubscriptionReconciler().removeOrphanSubscriptions();
            response.markOrphanSubscriptionsRemoved();
            return true;
        } else {
            completeSubscriptionOperations(response);
            if (!response.hasReplicaValidationStarted()) {
                releaseSchedulerLock();
                // Validate all materialized Replicas in this Replicant Context after the response has been applied.
                validateReplicas();
            } else {
                // Also release the scheduler lock when optional Replica validation is disabled.
                releaseSchedulerLock();
                completeMessageResponse();
            }
            return true;
        }
    }

    /**
     * Return true if this Connector has reached a Synchronization Point and has no queued requests or responses.
     */
    @Memoize
    boolean isAtSynchronizationPoint() {
        return areRequestResponseQueuesEmpty() && ensureConnection().isSynchronizationPointReached();
    }

    /**
     * Return true if this Connector can request the next Synchronization Point.
     */
    boolean shouldRequestSynchronizationPoint() {
        return areRequestResponseQueuesEmpty() && !ensureConnection().isSynchronizationPointReached();
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

    @Action(verifyRequired = false)
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
                    final Dataset dataset = getSystemSchema().getDataset(datasetAddress.datasetId());
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

                if (SubscriptionChange.Type.INVALIDATE_DATASET_ADDRESS == changeType) {
                    getReplicantContext().getAreaOfInterestService().invalidateDatasetAddress(datasetAddress);
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
                                    return getSystemSchema()
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
        final OnReplicaUpdateAction action = getSystemSchema().getOnReplicaUpdateAction();
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
        final Dataset dataset = getSystemSchema().getDataset(datasetAddress.datasetId());
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

    private void completeSubscriptionOperations(@NonNull final MessageResponse response) {
        final RequestEntry request = response.getRequest();
        if (null != request) {
            final List<SubscriptionOperation> operations = ensureConnection().getActiveSubscriptionOperations();
            if (!operations.isEmpty() && operations.get(0).getRequestId() == request.getRequestId()) {
                completeSubscriptionOperations(operations);
            }
        }
    }

    void completeMessageResponse() {
        final Connection connection = ensureConnection();
        final MessageResponse response = connection.ensureCurrentMessageResponse();

        // Step: Run the post actions
        final RequestEntry request = response.getRequest();
        final ServerToClientMessage message = response.getMessage();
        final Integer requestId = message.getRequestId();

        final ExecRequest execRequest = null != requestId ? ensureConnection().getActiveExecRequest(requestId) : null;
        if (null != execRequest && null != request && message instanceof ChangeSetMessage) {
            @SuppressWarnings("PatternVariableCanBeUsed")
            final ChangeSetMessage changeSet = (ChangeSetMessage) message;
            final ResponseHandler responseHandler = request.getResponseHandler();
            if (null != responseHandler) {
                responseHandler.onResponse(Objects.requireNonNull(changeSet.getResponse()));
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

        completeSubscriptionOperations(response);
        //noinspection IfCanBeSwitch
        if (OkMessage.TYPE.equals(message.getType())) {
            if (null != requestId && connection.getLastReachedSynchronizationPointRequestId() == requestId) {
                if (connection.isSynchronizationPointReached()) {
                    onSynchronizationPointReached();
                    getReplicantContext().getSubscriptionReconciler().removeOrphanSubscriptions();
                } else {
                    onSynchronizationPointPending();
                }
                triggerMessageScheduler();
            }
        } else if (ChangeSetMessage.TYPE.equals(message.getType())) {
            // If message is not a ping response then try to establish a Synchronization Point.
            maybeRequestSynchronizationPoint();
            final ChangeSetMessage changeSet = (ChangeSetMessage) message;
            if (null != changeSet.getDatasetCacheVersion()) {
                storeDatasetCacheEntryIfPossible(response, changeSet);
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

    // This is in an action so observers can react to status changes caused by subscription completion. Observable
    // changes are not required because a subscribe response may have already created the Subscription in explicit mode.
    @Action(reportParameters = false, verifyRequired = false)
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

    void maybeRequestSynchronizationPoint() {
        if (shouldRequestSynchronizationPoint()) {
            requestSynchronizationPoint();
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
    void removeUnneededSubscriptionUpdateOperations(@NonNull final List<SubscriptionOperation> operations) {
        operations.removeIf(operation -> {
            final DatasetAddress datasetAddress = operation.getDatasetAddress();
            final Subscription subscription = getReplicantContext().findSubscription(datasetAddress);
            if (Replicant.shouldCheckInvariants()) {
                invariant(
                        () -> null != subscription,
                        () -> "Replicant-0048: Subscription Operation to update Subscription at Dataset Address "
                                + datasetAddress + " but no Subscription exists.");
            }
            // The following code can probably be removed but it was present in the previous system
            // and it is unclear if there is any scenarios where it can still happen. The code has
            // been left in until we can verify it is no longer an issue. The above invariants will trigger
            // in development mode to help us track down these scenarios
            if (null == subscription) {
                operation.markAsComplete();
                return true;
            } else {
                return false;
            }
        });
    }

    @Action(verifyRequired = false)
    void removeUnneededUnsubscribeOperations(@NonNull final List<SubscriptionOperation> operations) {
        operations.removeIf(operation -> {
            final DatasetAddress datasetAddress = operation.getDatasetAddress();
            final Subscription subscription = getReplicantContext().findSubscription(datasetAddress);
            if (Replicant.shouldCheckInvariants()) {
                invariant(
                        () -> null != subscription,
                        () -> "Replicant-0046: Unsubscribe Operation at Dataset Address " + datasetAddress
                                + " has no Subscription.");
                invariant(
                        () -> null == subscription || SubscriptionMode.EXPLICIT == subscription.getMode(),
                        () -> "Replicant-0047: Unsubscribe Operation at Dataset Address " + datasetAddress
                                + " targets a Subscription that is not in Explicit Subscription Mode.");
            }
            // The following code can probably be removed but it was present in the previous system
            // and it is unclear if there is any scenarios where it can still happen. The code has
            // been left in until we can verify it is no longer an issue. The above invariants will trigger
            // in development mode to help us track down these scenarios
            if (null == subscription || SubscriptionMode.EXPLICIT != subscription.getMode()) {
                // We were getting here if a Dataset Address Invalidation was reported after removal of its Dataset
                // Root was delivered to the client. That delivery explicitly unsubscribes, which gets sent back a
                // successful unsubscribe even though the Subscription had already been orphaned or invalidated.
                operation.markAsComplete();
                return true;
            } else {
                return false;
            }
        });
    }

    private void storeDatasetCacheEntryIfPossible(
            @NonNull final MessageResponse response, @NonNull final ChangeSetMessage changeSet) {
        final String datasetCacheVersion = changeSet.getDatasetCacheVersion();
        final DatasetCacheService datasetCacheService = getReplicantContext().getDatasetCacheService();

        boolean candidate = false;
        if (null != datasetCacheService
                && null != datasetCacheVersion
                && (changeSet.hasSubscriptionChanges() || changeSet.hasFilterParameterSubscriptionChanges())) {
            final List<SubscriptionChange> subscriptionChanges = response.getSubscriptionChanges();

            if (1 == subscriptionChanges.size()
                    && SubscriptionChange.Type.SUBSCRIBE
                            == subscriptionChanges.get(0).getType()
                    && getSystemSchema()
                            .getDataset(subscriptionChanges
                                    .get(0)
                                    .getDatasetAddress()
                                    .datasetId())
                            .isCacheable()) {
                final DatasetAddress datasetAddress = subscriptionChanges.get(0).getDatasetAddress();
                try {
                    if (datasetCacheService.storeDatasetCacheEntry(datasetAddress, datasetCacheVersion, changeSet)) {
                        _rejectedDatasetCacheEntryAddresses.remove(datasetAddress);
                    }
                } catch (final Throwable t) {
                    ReplicantLogger.log("Failed to store Dataset Cache Entry at " + datasetAddress + ".", t);
                }
                candidate = true;
            }
        }
        if (Replicant.shouldCheckApiInvariants()) {
            final boolean c = candidate;
            apiInvariant(
                    () -> null == datasetCacheVersion || null == datasetCacheService || c,
                    () -> "Replicant-0072: datasetCacheVersion in reply for ChangeSet but ChangeSet does not"
                            + " represent a Cacheable Dataset.");
        }
    }

    @SuppressWarnings("unchecked")
    @Action
    void processEntityChanges() {
        final MessageResponse response = ensureCurrentMessageResponse();
        EntityChange change;
        for (int i = 0; i < _changesToProcessPerTick && null != (change = response.nextEntityChange()); i++) {
            final int entityTypeId = change.getEntityTypeId();
            final int entityId = change.getEntityId();
            final EntityType entityType = getSystemSchema().getEntityType(entityTypeId);
            final Class<?> type = entityType.getType();
            ReplicaEntry replicaEntry =
                    getReplicantContext().getReplicaRegistry().findReplicaEntryByTypeAndEntityId(type, entityId);
            if (change.isRemove()) {
                /*
                 * Sometimes a remove can occur for an entity that is no longer present on the client. The most
                 * common cause of this is initiating an action that deletes an entity and then un-subscribing
                 * from the Subscription that contains the Entity. This can result in an Entity that has been removed
                 * locally but has a removal Entity Change in the queue. Other interleaved async operations can also
                 * trigger this scenario.
                 */
                if (null != replicaEntry) {
                    Disposable.dispose(replicaEntry);
                    response.incEntityRemoveCount();
                }
            } else {
                final EntityChangePayload payload = change.getPayload();
                if (null == replicaEntry) {
                    final String name = Replicant.areNamesEnabled() ? entityType.getName() + "/" + entityId : null;
                    replicaEntry =
                            getReplicantContext().getReplicaRegistry().findOrCreateReplicaEntry(name, type, entityId);
                    final Object replica = entityType.getCreator().createReplica(entityId, payload);
                    replicaEntry.setReplica(replica);
                } else {
                    @SuppressWarnings("rawtypes")
                    final EntityType.Updater updater = entityType.getUpdater();
                    if (null != updater) {
                        updater.updateReplica(replicaEntry.getReplica(), payload);
                    }
                }

                final String[] datasetAddressDescriptors = change.getDatasetAddresses();
                final int systemSchemaId = getSystemSchema().getId();
                for (final String datasetAddressDescriptor : datasetAddressDescriptors) {
                    try {
                        final DatasetAddress datasetAddress =
                                DatasetAddress.parse(systemSchemaId, datasetAddressDescriptor);
                        final Subscription subscription = getReplicantContext().findSubscription(datasetAddress);
                        if (Replicant.shouldCheckInvariants()) {
                            invariant(
                                    () -> null != subscription,
                                    () -> "Replicant-0069: ChangeSetMessage contained an Entity Change"
                                            + " referencing Dataset Address "
                                            + datasetAddress + " but no such subscription exists locally.");
                        }
                        if (null != subscription) {
                            replicaEntry.tryLinkToSubscription(subscription);
                        } else {
                            onSynchronizationPointPending();
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
                in the Entity Change could be removed here. However we assume the code generated for
                Subscription Changes will handle membership changes and remove Subscriptions no longer
                relevant.
                */

                response.incEntityUpdateCount();
                response.replicaProcessed(replicaEntry.getReplica());
            }
        }
    }

    /**
     * Validate every materialized Replica in this connector's Replicant Context that implements Arez
     * {@code Verifiable}.
     *
     * <p>This validates client Replica state after the current response has been applied. It does not validate server
     * Entities, Subscription metadata, or Replicant Context configuration.</p>
     */
    void validateReplicas() {
        ensureCurrentMessageResponse().markReplicaValidationStarted();
        if (Replicant.shouldValidateReplicasAfterMessageProcessing()) {
            getReplicantContext().getValidator().validateReplicas();
        }
    }

    /** Perform one step in sending one or more Subscription Operations to the server. */
    boolean progressSubscriptionOperationProcessing() {
        final List<SubscriptionOperation> operations =
                new ArrayList<>(ensureConnection().getCurrentSubscriptionOperations());
        if (operations.isEmpty()) {
            return false;
        } else if (operations.get(0).isInProgress()) {
            return false;
        } else {
            final SubscriptionOperation.Type type = operations.get(0).getType();
            if (SubscriptionOperation.Type.SUBSCRIBE == type) {
                progressSubscribeOperations(operations);
            } else if (SubscriptionOperation.Type.UNSUBSCRIBE == type) {
                progressUnsubscribeOperations(operations);
            } else {
                progressSubscriptionUpdateOperations(operations);
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

    void progressSubscribeOperations(@NonNull final List<SubscriptionOperation> operations) {
        // Do not strip out operations merely because there is a local Subscription. An equal local Subscription would
        // prevent the operation reaching this point, while the server must observe an operation that moves a
        // Subscription from Implicit to Explicit Subscription Mode.
        if (operations.isEmpty()) {
            completeSubscriptionOperation();
        } else if (1 == operations.size()) {
            progressSubscribeOperation(operations.get(0));
        } else {
            progressBulkSubscribeOperations(operations);
        }
    }

    void progressSubscribeOperation(@NonNull final SubscriptionOperation operation) {
        final DatasetAddress datasetAddress = operation.getDatasetAddress();
        onSubscribeStarted(datasetAddress);

        _transport.requestSubscribe(operation.getDatasetAddress(), operation.getFilterParameter());
        operation.markAsInProgress(ensureConnection().getLastTxRequestId());
    }

    void progressBulkSubscribeOperations(@NonNull final List<SubscriptionOperation> operations) {
        final List<DatasetAddress> datasetAddresses = operations.stream()
                .map(SubscriptionOperation::getDatasetAddress)
                .collect(Collectors.toList());
        datasetAddresses.forEach(this::onSubscribeStarted);

        _transport.requestBulkSubscribe(datasetAddresses, operations.get(0).getFilterParameter());
        final int requestId = ensureConnection().getLastTxRequestId();
        operations.forEach(operation -> operation.markAsInProgress(requestId));
    }

    void progressSubscriptionUpdateOperations(@NonNull final List<SubscriptionOperation> operations) {
        removeUnneededSubscriptionUpdateOperations(operations);

        if (operations.isEmpty()) {
            completeSubscriptionOperation();
        } else if (operations.size() > 1) {
            progressBulkSubscriptionUpdateOperations(operations);
        } else {
            progressSubscriptionUpdateOperation(operations.get(0));
        }
    }

    void progressSubscriptionUpdateOperation(@NonNull final SubscriptionOperation operation) {
        final DatasetAddress datasetAddress = operation.getDatasetAddress();
        onSubscriptionUpdateStarted(datasetAddress);

        final Object filterParameter = operation.getFilterParameter();
        assert null != filterParameter;
        _transport.requestSubscribe(datasetAddress, filterParameter);
        final int requestId = ensureConnection().getLastTxRequestId();
        operation.markAsInProgress(requestId);
    }

    void progressBulkSubscriptionUpdateOperations(@NonNull final List<SubscriptionOperation> operations) {
        final List<DatasetAddress> datasetAddresses = operations.stream()
                .map(SubscriptionOperation::getDatasetAddress)
                .collect(Collectors.toList());
        datasetAddresses.forEach(this::onSubscriptionUpdateStarted);

        // All Filter Parameters will be the same if they are grouped
        final Object filterParameter = operations.get(0).getFilterParameter();
        assert null != filterParameter;
        _transport.requestBulkSubscribe(datasetAddresses, filterParameter);
        final int requestId = ensureConnection().getLastTxRequestId();
        operations.forEach(operation -> operation.markAsInProgress(requestId));
    }

    void progressUnsubscribeOperations(@NonNull final List<SubscriptionOperation> operations) {
        removeUnneededUnsubscribeOperations(operations);

        if (operations.isEmpty()) {
            completeSubscriptionOperation();
        } else if (operations.size() > 1) {
            progressBulkUnsubscribeOperations(operations);
        } else {
            progressUnsubscribeOperation(operations.get(0));
        }
    }

    void progressUnsubscribeOperation(@NonNull final SubscriptionOperation operation) {
        final DatasetAddress datasetAddress = operation.getDatasetAddress();
        onUnsubscribeStarted(datasetAddress);

        _transport.requestUnsubscribe(datasetAddress);
        operation.markAsInProgress(ensureConnection().getLastTxRequestId());
    }

    void progressBulkUnsubscribeOperations(@NonNull final List<SubscriptionOperation> operations) {
        final List<DatasetAddress> datasetAddresses = operations.stream()
                .map(SubscriptionOperation::getDatasetAddress)
                .collect(Collectors.toList());
        datasetAddresses.forEach(this::onUnsubscribeStarted);

        _transport.requestBulkUnsubscribe(datasetAddresses);
        final int requestId = ensureConnection().getLastTxRequestId();
        operations.forEach(operation -> operation.markAsInProgress(requestId));
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
                    .reportSpyEvent(new ConnectedEvent(
                            getSystemSchema().getId(), getSystemSchema().getName()));
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
                            getSystemSchema().getId(), getSystemSchema().getName()));
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
                            getSystemSchema().getId(), getSystemSchema().getName()));
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
                            getSystemSchema().getId(), getSystemSchema().getName()));
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
        if (UseDatasetCacheEntryMessage.TYPE.equals(message.getType())) {
            final UseDatasetCacheEntryMessage useDatasetCacheEntryMessage = (UseDatasetCacheEntryMessage) message;
            final String datasetAddressDescriptor = useDatasetCacheEntryMessage.getDatasetAddress();
            final DatasetAddress datasetAddress;
            try {
                datasetAddress = DatasetAddress.parse(getSystemSchema().getId(), datasetAddressDescriptor);
            } catch (final Throwable t) {
                onMessageProcessFailure(t);
                return;
            }
            final String datasetCacheVersion = useDatasetCacheEntryMessage.getDatasetCacheVersion();

            final DatasetCacheService datasetCacheService =
                    getReplicantContext().getDatasetCacheService();
            if (null == datasetCacheService) {
                ReplicantLogger.log(
                        "Received a use-dataset-cache-entry message for Dataset Address " + datasetAddress
                                + " but no Dataset Cache Service is configured.",
                        null);
                _rejectedDatasetCacheEntryAddresses.add(datasetAddress);
                onMessageReadFailure();
                return;
            }

            final DatasetCacheEntry entry;
            try {
                entry = datasetCacheService.lookupDatasetCacheEntry(datasetAddress);
            } catch (final Throwable t) {
                rejectDatasetCacheEntry(datasetCacheService, datasetAddress, "Dataset Cache Entry is unreadable.", t);
                onMessageReadFailure();
                return;
            }
            if (null == entry) {
                rejectDatasetCacheEntry(datasetCacheService, datasetAddress, "Dataset Cache Entry is absent.", null);
                onMessageReadFailure();
                return;
            }
            if (!Objects.equals(entry.getDatasetCacheVersion(), datasetCacheVersion)) {
                rejectDatasetCacheEntry(
                        datasetCacheService, datasetAddress, "Dataset Cache Version does not match.", null);
                onMessageReadFailure();
                return;
            }
            try {
                messageToQueue =
                        Objects.requireNonNull(JSON.parse(entry.getChangeSet())).cast();
            } catch (final Throwable t) {
                rejectDatasetCacheEntry(datasetCacheService, datasetAddress, "Dataset Cache Entry is corrupt.", t);
                onMessageReadFailure();
                return;
            }
            messageToQueue.setRequestId(requestId);
        } else {
            messageToQueue = message;
        }

        connection.enqueueResponse(messageToQueue, request);
        triggerMessageScheduler();
    }

    private void rejectDatasetCacheEntry(
            @NonNull final DatasetCacheService datasetCacheService,
            @NonNull final DatasetAddress datasetAddress,
            @NonNull final String reason,
            @Nullable final Throwable error) {
        _rejectedDatasetCacheEntryAddresses.add(datasetAddress);
        ReplicantLogger.log("Rejected Dataset Cache Entry at " + datasetAddress + ". " + reason, error);
        try {
            datasetCacheService.invalidateDatasetCacheEntry(datasetAddress);
        } catch (final Throwable t) {
            ReplicantLogger.log("Failed to invalidate Dataset Cache Entry at " + datasetAddress + ".", t);
        }
    }

    /**
     * Invoked when a server-to-client transport message has been completely processed.
     *
     * @param response the message response.
     */
    void onMessageProcessed(@NonNull final MessageResponse response) {
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new MessageProcessedEvent(
                            getSystemSchema().getId(),
                            getSystemSchema().getName(),
                            response.toMessageProcessingSummary()));
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
                            getSystemSchema().getId(), getSystemSchema().getName(), command, requestId));
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
                            getSystemSchema().getId(), getSystemSchema().getName(), command, requestId));
        }
    }

    /**
     * Called when message processing has resulted in a failure.
     */
    @Action(verifyRequired = false)
    void onMessageProcessFailure(@NonNull final Throwable error) {
        final String message = ReplicantUtil.safeGetString(() -> "Exception processing replicant message.");
        ReplicantLogger.log(message, error);
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new MessageProcessFailureEvent(
                            getSystemSchema().getId(), getSystemSchema().getName(), error));
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
                            getSystemSchema().getId(), getSystemSchema().getName()));
        }
        disconnectIfPossible();
    }

    void disconnectIfPossible() {
        if (!ConnectorState.isTransitionState(getState())) {
            if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
                getReplicantContext()
                        .getSpy()
                        .reportSpyEvent(new RestartEvent(
                                getSystemSchema().getId(), getSystemSchema().getName()));
            }
            disconnect();
        }
    }

    void onSynchronizationPointReached() {
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new SynchronizationPointReachedEvent(
                            getSystemSchema().getId()));
        }
    }

    void onSynchronizationPointPending() {
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new SynchronizationPointPendingEvent(
                            getSystemSchema().getId()));
        }
    }

    void onSubscribeStarted(@NonNull final DatasetAddress datasetAddress) {
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new SubscribeStartedEvent(
                            getSystemSchema().getId(), getSystemSchema().getName(), datasetAddress));
        }
    }

    @Action
    void onSubscribeCompleted(@NonNull final DatasetAddress datasetAddress) {
        final Subscription subscription = getReplicantContext().findSubscription(datasetAddress);
        if (null != subscription) {
            subscription.setMode(SubscriptionMode.EXPLICIT);
        }
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new SubscribeCompletedEvent(
                            getSystemSchema().getId(), getSystemSchema().getName(), datasetAddress));
        }
    }

    void onUnsubscribeStarted(@NonNull final DatasetAddress datasetAddress) {
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new UnsubscribeStartedEvent(
                            getSystemSchema().getId(), getSystemSchema().getName(), datasetAddress));
        }
    }

    void onUnsubscribeCompleted(@NonNull final DatasetAddress datasetAddress) {
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new UnsubscribeCompletedEvent(
                            getSystemSchema().getId(), getSystemSchema().getName(), datasetAddress));
        }
    }

    void onSubscriptionUpdateStarted(@NonNull final DatasetAddress datasetAddress) {
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new SubscriptionUpdateStartedEvent(
                            getSystemSchema().getId(), getSystemSchema().getName(), datasetAddress));
        }
    }

    void onSubscriptionUpdateCompleted(@NonNull final DatasetAddress datasetAddress) {
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext()
                    .getSpy()
                    .reportSpyEvent(new SubscriptionUpdateCompletedEvent(
                            getSystemSchema().getId(), getSystemSchema().getName(), datasetAddress));
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
        return Replicant.areNamesEnabled() ? "Connector[" + getSystemSchema().getName() + "]" : super.toString();
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
