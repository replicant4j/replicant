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
import replicant.AreaOfInterestRequest.Type;
import replicant.spy.SubscriptionOrphanedEvent;

@SuppressWarnings("BadImport")
@ArezComponent(disposeNotifier = Feature.DISABLE, requireId = Feature.DISABLE)
abstract class SubscriptionReconciler extends ReplicantService {
    /**
     * Enum describing an action during a reconciliation step.
     */
    enum Action {
        /**
         * The request has resulted in a subscribe request added to the AOI queue.
         */
        SUBMITTED_ADD,
        /**
         * The request has resulted in a subscription update request added to the AOI queue.
         */
        SUBMITTED_UPDATE,
        /**
         * The request to update subscription with static filter has resulted in a remove request added to the AOI queue.
         */
        SUBMITTED_REMOVE,
        /**
         * The request is already in progress, still waiting for a response.
         */
        IN_PROGRESS,
        /**
         * Nothing was done, fully reconciled.
         */
        NO_ACTION
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
        AreaOfInterestRequest.Type groupAction = null;
        for (final AreaOfInterest areaOfInterest : getReplicantContext().getAreasOfInterest()) {
            // Make sure we observe the filter so that changes trigger another reconciliation
            areaOfInterest.getFilter();

            // Make sure we observe the status so that the SubscriptionReconciler reruns when status updates.
            // This is usually not needed
            // except when multiple areaOfInterest are queued up simultaneously and the later can not be grouped
            // into first AreaOfInterest. If this is not here then the SubscriptionReconciler will not rerun.
            areaOfInterest.getStatus();

            if (AreaOfInterest.Status.DELETED != areaOfInterest.getStatus()) {
                final Action action = reconcileAreaOfInterest(areaOfInterest, groupTemplate, groupAction);
                switch (action) {
                    case SUBMITTED_ADD:
                        groupAction = AreaOfInterestRequest.Type.ADD;
                        groupTemplate = areaOfInterest;
                        break;
                    case SUBMITTED_UPDATE:
                        groupAction = AreaOfInterestRequest.Type.UPDATE;
                        groupTemplate = areaOfInterest;
                        break;
                    case SUBMITTED_REMOVE:
                        // A request to update a subscription that has static filter has resulted in remove being
                        // submitted.
                        return;
                    case IN_PROGRESS:
                        if (null == groupTemplate) {
                            // First thing in the subscription queue is in flight, so terminate
                            return;
                        }
                        break;
                    case NO_ACTION:
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
    Action reconcileAreaOfInterest(
            @NonNull final AreaOfInterest areaOfInterest,
            @Nullable final AreaOfInterest groupTemplate,
            @Nullable final Type groupAction) {
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
            final Object filter = areaOfInterest.getFilter();

            final int addIndex = connector.lastIndexOfPendingAreaOfInterestRequest(
                    AreaOfInterestRequest.Type.ADD, datasetAddress, filter);
            final int removeIndex = connector.lastIndexOfPendingAreaOfInterestRequest(
                    AreaOfInterestRequest.Type.REMOVE, datasetAddress, null);
            final int updateIndex = connector.lastIndexOfPendingAreaOfInterestRequest(
                    AreaOfInterestRequest.Type.UPDATE, datasetAddress, filter);

            if ((!subscribed && addIndex < 0) || removeIndex > addIndex) {
                if (null == groupTemplate
                        || canGroup(groupTemplate, groupAction, areaOfInterest, AreaOfInterestRequest.Type.ADD)) {
                    connector.requestSubscribe(datasetAddress, filter);
                    return Action.SUBMITTED_ADD;
                } else {
                    return Action.NO_ACTION;
                }
            } else if (addIndex >= 0) {
                // Must have add in pipeline so pause until it completed
                return Action.IN_PROGRESS;
            } else {
                // Must be subscribed...
                if (updateIndex >= 0) {
                    // Update in progress so wait till it completes
                    return Action.IN_PROGRESS;
                }

                final Subscription existingSubscription = Objects.requireNonNull(subscription);
                if (!FilterUtil.filtersEqual(filter, existingSubscription.getFilter())) {
                    final SystemSchema schema =
                            getReplicantContext().getSchemaService().getById(datasetAddress.schemaId());
                    final Dataset.FilterType filterType =
                            schema.getDataset(datasetAddress.datasetId()).getFilterType();
                    if (null == groupTemplate && Dataset.FilterType.DYNAMIC != filterType) {
                        /*
                        If the subscription needs an update but the backend does not support updates
                        and subscription is explicitly subscribed then need to do a remove. Eventually it will
                        fall through the add path once remove goes through. If the subscription is NOT explicitly
                        subscribed then generate an error and fail.
                        */
                        if (Replicant.shouldCheckInvariants()) {
                            invariant(
                                    existingSubscription::isExplicitSubscription,
                                    () -> "Replicant-0083: Attempting to update Dataset Address " + datasetAddress
                                            + " but the Dataset does not allow dynamic filter updates and has not"
                                            + " been explicitly subscribed.");
                        }
                        connector.requestUnsubscribe(datasetAddress);
                        return Action.SUBMITTED_REMOVE;
                    } else if (null == groupTemplate
                            || canGroup(
                                    groupTemplate, groupAction, areaOfInterest, AreaOfInterestRequest.Type.UPDATE)) {
                        connector.requestSubscriptionUpdate(datasetAddress, filter);
                        return Action.SUBMITTED_UPDATE;
                    } else {
                        return Action.NO_ACTION;
                    }
                } else {
                    /*
                     * The AreaOfInterest was added but an existing subscription matched it exactly.
                     * If the subscription is explicitly subscribed then just update the status of
                     * the AreaOfInterest, otherwise request subscription so that the server is aware
                     * of the explicit subscription.
                     */
                    if (AreaOfInterest.Status.NOT_ASKED == areaOfInterest.getStatus()) {
                        if (existingSubscription.isExplicitSubscription()) {
                            areaOfInterest.updateAreaOfInterest(AreaOfInterest.Status.LOADED, null);
                        } else {
                            connector.requestSubscribe(datasetAddress, filter);
                            return Action.SUBMITTED_ADD;
                        }
                    }
                }
            }
        }
        return Action.NO_ACTION;
    }

    boolean canGroup(
            @NonNull final AreaOfInterest groupTemplate,
            @Nullable final Type groupAction,
            @NonNull final AreaOfInterest areaOfInterest,
            @Nullable final Type action) {
        if (null != groupAction && null != action && !groupAction.equals(action)) {
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
                    && (AreaOfInterestRequest.Type.REMOVE == action
                            || FilterUtil.filtersEqual(groupTemplate.getFilter(), areaOfInterest.getFilter()));
        }
    }

    @arez.annotations.Action
    void removeOrphanSubscriptions() {
        final HashSet<DatasetAddress> expectedDatasetAddresses = new HashSet<>();
        getReplicantContext()
                .getAreasOfInterest()
                .forEach(aoi -> expectedDatasetAddresses.add(aoi.getDatasetAddress()));

        final SubscriptionService subscriptionService = getReplicantContext().getSubscriptionService();
        removeOrphanSubscriptions(subscriptionService.getTypeSubscriptions(), expectedDatasetAddresses);
        removeOrphanSubscriptions(subscriptionService.getInstanceSubscriptions(), expectedDatasetAddresses);
    }

    private void removeOrphanSubscriptions(
            @NonNull final Collection<Subscription> subscriptions,
            @NonNull final Set<DatasetAddress> expectedDatasetAddresses) {
        subscriptions.stream()
                // Subscription must be explicit
                .filter(Subscription::isExplicitSubscription)
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
                || connector.isAreaOfInterestRequestPending(AreaOfInterestRequest.Type.REMOVE, datasetAddress, null);
    }

    @NonNull
    private ReplicantRuntime getReplicantRuntime() {
        return getReplicantContext().getRuntime();
    }
}
