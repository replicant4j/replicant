package replicant;

import static org.realityforge.braincheck.Guards.*;

import arez.Disposable;
import arez.annotations.ArezComponent;
import arez.annotations.DepType;
import arez.annotations.Feature;
import arez.annotations.Observable;
import arez.annotations.Observe;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.SubscriptionOperation.Type;
import replicant.spy.SubscriptionOrphanedEvent;

@SuppressWarnings("BadImport")
@ArezComponent(disposeNotifier = Feature.DISABLE, requireId = Feature.DISABLE)
abstract class SubscriptionReconciler extends ReplicantService {
    /**
     * Outcome of reconciling one Area of Interest.
     */
    enum Outcome {
        /**
         * A Subscribe Operation was added to the pending queue.
         */
        SUBSCRIBE_OPERATION_ISSUED,
        /**
         * An Update Operation was added to the pending queue.
         */
        UPDATE_OPERATION_ISSUED,
        /**
         * Updating a Fixed Filter Parameter issued an Unsubscribe Operation to replace the Subscription.
         */
        UNSUBSCRIBE_OPERATION_ISSUED,
        /**
         * A matching Subscription Operation is in progress.
         */
        OPERATION_IN_PROGRESS,
        /**
         * The Area of Interest and Subscription are reconciled.
         */
        RECONCILED
    }

    @NonNull
    static SubscriptionReconciler create(@Nullable final ReplicantContext context) {
        return new Arez_SubscriptionReconciler(context);
    }

    SubscriptionReconciler(@Nullable final ReplicantContext context) {
        super(context);
    }

    /**
     * Specify the action invoked before Subscription Reconciliation compares desired Areas of Interest with actual
     * Subscriptions.
     * This action is often used when subscriptions in one system trigger subscriptions in another system.
     * This property is an Arez observable.
     *
     * @param preReconciliationAction the action.
     */
    @Observable
    abstract void setPreReconciliationAction(@Nullable SafeProcedure preReconciliationAction);

    /**
     * Return the pre-reconciliation action. See {@link #setPreReconciliationAction(SafeProcedure)} for further details.
     * This property is an Arez observable.
     *
     * @return the action.
     */
    @Nullable
    abstract SafeProcedure getPreReconciliationAction();

    /**
     * Specify the action that is invoked after Subscription Reconciliation completes.
     * This property is an Arez observable.
     *
     * @param reconciliationCompleteAction the action.
     */
    @Observable
    abstract void setReconciliationCompleteAction(@Nullable SafeProcedure reconciliationCompleteAction);

    /**
     * Return the reconciliation-complete action. See {@link #setReconciliationCompleteAction(SafeProcedure)} for
     * further details.
     * This property is an Arez observable.
     *
     * @return the action.
     */
    @Nullable
    abstract SafeProcedure getReconciliationCompleteAction();

    // depType allows NONE as during dispose when runtime is component disposed there is nothing left to observe
    @Observe(mutation = true, nestedActionsAllowed = true, depType = DepType.AREZ_OR_NONE)
    void reconcile() {
        preReconciliation();
        final ReplicantRuntime runtime = getReplicantRuntime();
        if (Disposable.isNotDisposed(runtime) && RuntimeState.CONNECTED == runtime.getState()) {
            reconcileStep();
        }
    }

    @arez.annotations.Action(requireNewTransaction = true, verifyRequired = false)
    void preReconciliation() {
        final SafeProcedure preReconciliationAction = getPreReconciliationAction();
        if (null != preReconciliationAction) {
            preReconciliationAction.call();
        }
    }

    void reconciliationComplete() {
        final SafeProcedure reconciliationCompleteAction = getReconciliationCompleteAction();
        if (null != reconciliationCompleteAction) {
            reconciliationCompleteAction.call();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void reconcileStep() {
        AreaOfInterest groupTemplate = null;
        SubscriptionOperation.Type groupOperationType = null;
        for (final AreaOfInterest areaOfInterest : getReplicantContext().getAreasOfInterest()) {
            // Make sure we observe the Filter Parameter so that changes trigger another reconciliation
            areaOfInterest.getFilterParameter();

            if (AreaOfInterest.Status.INVALIDATED != areaOfInterest.getStatus()) {
                final Outcome outcome = reconcileAreaOfInterest(areaOfInterest, groupTemplate, groupOperationType);
                switch (outcome) {
                    case SUBSCRIBE_OPERATION_ISSUED:
                        groupOperationType = SubscriptionOperation.Type.SUBSCRIBE;
                        groupTemplate = areaOfInterest;
                        break;
                    case UPDATE_OPERATION_ISSUED:
                        groupOperationType = SubscriptionOperation.Type.UPDATE;
                        groupTemplate = areaOfInterest;
                        break;
                    case UNSUBSCRIBE_OPERATION_ISSUED:
                        // Updating a Fixed Filter Parameter issued an Unsubscribe Operation to replace the
                        // Subscription.
                        return;
                    case OPERATION_IN_PROGRESS:
                        if (null == groupTemplate) {
                            // First thing in the subscription queue is in flight, so terminate
                            return;
                        }
                        break;
                    case RECONCILED:
                        break;
                }
            }
        }
        if (null != groupTemplate) {
            return;
        }

        reconciliationComplete();
    }

    @arez.annotations.Action(requireNewTransaction = true, verifyRequired = false)
    @NonNull
    Outcome reconcileAreaOfInterest(
            @NonNull final AreaOfInterest areaOfInterest,
            @Nullable final AreaOfInterest groupTemplate,
            @Nullable final Type groupOperationType) {
        if (Replicant.shouldCheckInvariants()) {
            invariant(
                    () -> Disposable.isNotDisposed(areaOfInterest),
                    () -> "Replicant-0020: Invoked reconcileAreaOfInterest() with disposed AreaOfInterest.");
        }
        final DatasetAddress datasetAddress = areaOfInterest.getDatasetAddress();
        final Connector connector = getReplicantRuntime().getConnector(datasetAddress.schemaId());
        // Service can be disconnected if it is not required; reconciliation resumes when it reconnects.
        if (ConnectorState.CONNECTED == connector.getState()) {
            final Subscription subscription = getReplicantContext().findSubscription(datasetAddress);
            final boolean subscribed = null != subscription;
            final Object filterParameter = areaOfInterest.getFilterParameter();

            final int addIndex = connector.lastIndexOfPendingSubscriptionOperation(
                    SubscriptionOperation.Type.SUBSCRIBE, datasetAddress, filterParameter);
            final int removeIndex = connector.lastIndexOfPendingSubscriptionOperation(
                    SubscriptionOperation.Type.UNSUBSCRIBE, datasetAddress, null);
            final int updateIndex = connector.lastIndexOfPendingSubscriptionOperation(
                    SubscriptionOperation.Type.UPDATE, datasetAddress, filterParameter);

            if ((!subscribed && addIndex < 0) || removeIndex > addIndex) {
                if (null == groupTemplate
                        || canGroup(
                                groupTemplate,
                                groupOperationType,
                                areaOfInterest,
                                SubscriptionOperation.Type.SUBSCRIBE)) {
                    connector.requestSubscribe(datasetAddress, filterParameter);
                    return Outcome.SUBSCRIBE_OPERATION_ISSUED;
                } else {
                    return Outcome.RECONCILED;
                }
            } else if (addIndex >= 0) {
                // Must have add in pipeline so pause until it completed
                return Outcome.OPERATION_IN_PROGRESS;
            } else {
                // Must be subscribed...
                if (updateIndex >= 0) {
                    // Update in progress so wait till it completes
                    return Outcome.OPERATION_IN_PROGRESS;
                }

                final Subscription existingSubscription = Objects.requireNonNull(subscription);
                if (!FilterParameterUtil.filterParametersEqual(
                        filterParameter, existingSubscription.getFilterParameter())) {
                    final SystemSchema schema =
                            getReplicantContext().getSchemaService().getById(datasetAddress.schemaId());
                    final Dataset dataset = schema.getDataset(datasetAddress.datasetId());
                    if (null == groupTemplate && !dataset.hasUpdatableFilterParameter()) {
                        /*
                        If the subscription needs an update but the backend does not support updates
                        and the Subscription is in Explicit Subscription Mode then need to do a remove. Eventually it
                        will fall through the add path once remove goes through. If the Subscription is in Implicit
                        Subscription Mode then generate an error and fail.
                        */
                        if (Replicant.shouldCheckInvariants()) {
                            invariant(
                                    () -> SubscriptionMode.EXPLICIT == existingSubscription.getMode(),
                                    () -> "Replicant-0083: Attempting to update Dataset Address " + datasetAddress
                                            + " but the Dataset does not have an updatable Filter Parameter and has not"
                                            + " been placed in Explicit Subscription Mode.");
                        }
                        connector.requestUnsubscribe(datasetAddress);
                        return Outcome.UNSUBSCRIBE_OPERATION_ISSUED;
                    } else if (null == groupTemplate
                            || canGroup(
                                    groupTemplate,
                                    groupOperationType,
                                    areaOfInterest,
                                    SubscriptionOperation.Type.UPDATE)) {
                        connector.requestSubscriptionUpdate(datasetAddress, filterParameter);
                        return Outcome.UPDATE_OPERATION_ISSUED;
                    } else {
                        return Outcome.RECONCILED;
                    }
                } else {
                    // An Implicit Subscription must be promoted to Explicit so the server knows the Area of Interest
                    // is satisfied independently of any dependent Subscription.
                    if (SubscriptionMode.IMPLICIT == existingSubscription.getMode()) {
                        connector.requestSubscribe(datasetAddress, filterParameter);
                        return Outcome.SUBSCRIBE_OPERATION_ISSUED;
                    }
                }
            }
        }
        return Outcome.RECONCILED;
    }

    boolean canGroup(
            @NonNull final AreaOfInterest groupTemplate,
            @Nullable final Type groupOperationType,
            @NonNull final AreaOfInterest areaOfInterest,
            @Nullable final Type operationType) {
        if (null != groupOperationType && null != operationType && !groupOperationType.equals(operationType)) {
            return false;
        } else {
            final boolean sameDataset = groupTemplate.getDatasetAddress().schemaId()
                            == areaOfInterest.getDatasetAddress().schemaId()
                    && groupTemplate.getDatasetAddress().datasetId()
                            == areaOfInterest.getDatasetAddress().datasetId();
            final boolean sameDatasetKey = Objects.equals(
                    groupTemplate.getDatasetAddress().datasetKey(),
                    areaOfInterest.getDatasetAddress().datasetKey());

            return sameDataset
                    && sameDatasetKey
                    && (SubscriptionOperation.Type.UNSUBSCRIBE == operationType
                            || FilterParameterUtil.filterParametersEqual(
                                    groupTemplate.getFilterParameter(), areaOfInterest.getFilterParameter()));
        }
    }

    @arez.annotations.Action
    void removeOrphanSubscriptions() {
        final HashSet<DatasetAddress> expectedDatasetAddresses = new HashSet<>();
        getReplicantContext()
                .getAreasOfInterest()
                .forEach(aoi -> expectedDatasetAddresses.add(aoi.getDatasetAddress()));

        final SubscriptionService subscriptionService = getReplicantContext().getSubscriptionService();
        removeOrphanSubscriptions(subscriptionService.getTypeDatasetSubscriptions(), expectedDatasetAddresses);
        removeOrphanSubscriptions(subscriptionService.getInstanceDatasetSubscriptions(), expectedDatasetAddresses);
    }

    private void removeOrphanSubscriptions(
            @NonNull final Collection<Subscription> subscriptions,
            @NonNull final Set<DatasetAddress> expectedDatasetAddresses) {
        subscriptions.stream()
                // Subscription must be in Explicit Subscription Mode
                .filter(subscription -> SubscriptionMode.EXPLICIT == subscription.getMode())
                // Subscription should not be one of expected
                .map(Subscription::datasetAddress)
                .filter(datasetAddress -> !expectedDatasetAddresses.contains(datasetAddress))
                // Subscription should not have a remove pending
                .filter(datasetAddress -> !isRemovePending(datasetAddress))
                .forEachOrdered(this::removeOrphanSubscription);
    }

    private void removeOrphanSubscription(@NonNull final DatasetAddress datasetAddress) {
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            final Subscription subscription =
                    Objects.requireNonNull(getReplicantContext().findSubscription(datasetAddress));
            getReplicantContext().getSpy().reportSpyEvent(new SubscriptionOrphanedEvent(subscription));
        }
        getReplicantRuntime().getConnector(datasetAddress.schemaId()).requestUnsubscribe(datasetAddress);
    }

    /**
     * Return true if connector for Dataset Address has a remove pending for Dataset Address or the connector is not connected.
     *
     * @return true if connector for Dataset Address has a remove pending for Dataset Address or the connector is not connected.
     */
    private boolean isRemovePending(@NonNull final DatasetAddress datasetAddress) {
        final Connector connector = getReplicantRuntime().getConnector(datasetAddress.schemaId());
        return ConnectorState.CONNECTED != connector.getState()
                || connector.isSubscriptionOperationPending(
                        SubscriptionOperation.Type.UNSUBSCRIBE, datasetAddress, null);
    }

    @NonNull
    private ReplicantRuntime getReplicantRuntime() {
        return getReplicantContext().getRuntime();
    }
}
