package replicant.spy.tools;

import akasha.Console;
import org.jspecify.annotations.NonNull;
import replicant.AreaOfInterest;
import replicant.FilterParameterUtil;
import replicant.Subscription;
import replicant.spy.AreaOfInterestCreatedEvent;
import replicant.spy.AreaOfInterestDisposedEvent;
import replicant.spy.AreaOfInterestFilterParameterUpdatedEvent;
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
import replicant.spy.RequestCompletedEvent;
import replicant.spy.RequestStartedEvent;
import replicant.spy.RestartEvent;
import replicant.spy.SubscribeCompletedEvent;
import replicant.spy.SubscribeRequestQueuedEvent;
import replicant.spy.SubscribeStartedEvent;
import replicant.spy.SubscriptionCreatedEvent;
import replicant.spy.SubscriptionDisposedEvent;
import replicant.spy.SubscriptionOrphanedEvent;
import replicant.spy.SubscriptionUpdateCompletedEvent;
import replicant.spy.SubscriptionUpdateRequestQueuedEvent;
import replicant.spy.SubscriptionUpdateStartedEvent;
import replicant.spy.SyncRequestEvent;
import replicant.spy.UnsubscribeCompletedEvent;
import replicant.spy.UnsubscribeRequestQueuedEvent;
import replicant.spy.UnsubscribeStartedEvent;

/**
 * A SpyEventHandler that prints spy events to the tools console.
 * The events are colored to make them easy to digest. This class is designed to be easy to sub-class.
 */
@SuppressWarnings("WeakerAccess")
public class ConsoleSpyEventProcessor extends AbstractSpyEventProcessor {
    @CssRules
    private static final String CONNECTOR_COLOR = "color: #F5A402; font-weight: normal;";

    @CssRules
    private static final String SUBSCRIPTION_COLOR = "color: #0FA13B; font-weight: normal;";

    @CssRules
    private static final String AREA_OF_INTEREST_COLOR = "color: #006AEB; font-weight: normal;";

    @CssRules
    private static final String EXEC_COLOR = "color: orangered; font-weight: normal;";

    @CssRules
    private static final String ERROR_COLOR = "color: #A10001; font-weight: normal;";

    /**
     * Create the processor.
     */
    public ConsoleSpyEventProcessor() {
        on(AreaOfInterestCreatedEvent.class, this::onAreaOfInterestCreated);
        on(AreaOfInterestFilterParameterUpdatedEvent.class, this::onAreaOfInterestFilterParameterUpdated);
        on(AreaOfInterestDisposedEvent.class, this::onAreaOfInterestDisposed);

        on(SubscriptionCreatedEvent.class, this::onSubscriptionCreated);
        on(SubscriptionDisposedEvent.class, this::onSubscriptionDisposed);
        on(SubscriptionOrphanedEvent.class, this::onSubscriptionOrphaned);

        on(ExecStartedEvent.class, this::onExecStarted);
        on(ExecCompletedEvent.class, this::onExecCompleted);
        on(ExecRequestQueuedEvent.class, this::onExecRequestQueued);

        on(ConnectedEvent.class, this::onConnected);
        on(ConnectFailureEvent.class, this::onConnectFailure);
        on(DisconnectedEvent.class, this::onDisconnected);
        on(DisconnectFailureEvent.class, this::onDisconnectFailure);
        on(MessageProcessedEvent.class, this::onMessageProcessed);
        on(MessageProcessFailureEvent.class, this::onMessageProcessFailure);
        on(MessageReadFailureEvent.class, this::onMessageReadFailure);
        on(RestartEvent.class, this::onRestart);
        on(SubscribeRequestQueuedEvent.class, this::onSubscribeRequestQueued);
        on(SubscribeCompletedEvent.class, this::onSubscribeCompleted);
        on(SubscribeStartedEvent.class, this::onSubscribeStarted);
        on(SubscriptionUpdateRequestQueuedEvent.class, this::onSubscriptionUpdateRequestQueued);
        on(SubscriptionUpdateCompletedEvent.class, this::onSubscriptionUpdateCompleted);
        on(SubscriptionUpdateStartedEvent.class, this::onSubscriptionUpdateStarted);
        on(UnsubscribeRequestQueuedEvent.class, this::onUnsubscribeRequestQueued);
        on(UnsubscribeCompletedEvent.class, this::onUnsubscribeCompleted);
        on(UnsubscribeStartedEvent.class, this::onUnsubscribeStarted);

        on(RequestStartedEvent.class, this::onRequestStarted);
        on(RequestCompletedEvent.class, this::onRequestCompleted);

        on(SyncRequestEvent.class, this::onSyncRequest);
        on(InSyncEvent.class, this::onInSync);
        on(OutOfSyncEvent.class, this::onOutOfSync);
    }

    /**
     * Handle the SyncRequestEvent.
     *
     * @param e the event.
     */
    protected void onSyncRequest(@NonNull final SyncRequestEvent e) {
        log("%cSync requested. System Schema ID: " + e.getSystemSchemaId(), CONNECTOR_COLOR);
    }

    /**
     * Handle the InSyncEvent.
     *
     * @param e the event.
     */
    protected void onInSync(@NonNull final InSyncEvent e) {
        log("%cSync completed. Backend synchronized. System Schema ID: " + e.getSystemSchemaId(), CONNECTOR_COLOR);
    }

    /**
     * Handle the OutOfSyncEvent.
     *
     * @param e the event.
     */
    protected void onOutOfSync(@NonNull final OutOfSyncEvent e) {
        log("%cSync completed. Backend not synchronized. System Schema ID: " + e.getSystemSchemaId(), CONNECTOR_COLOR);
    }

    /**
     * Handle the RequestStartedEvent.
     *
     * @param e the event.
     */
    protected void onRequestStarted(@NonNull final RequestStartedEvent e) {
        log(
                "%cRequest started. System Schema: " + e.getName() + " Request: " + e.getName() + " RequestId: "
                        + e.getRequestId(),
                CONNECTOR_COLOR);
    }

    /**
     * Handle the RequestCompletedEvent.
     *
     * @param e the event.
     */
    protected void onRequestCompleted(@NonNull final RequestCompletedEvent e) {
        final String changeSetDescription = !e.isExpectingResults()
                ? "No Change set expected."
                : e.haveResultsArrived() ? "Change set has already arrived." : "Change set has not arrived.";

        log(
                "%cRequest completed. System Schema: " + e.getSystemSchemaName() + " Request: " + e.getName()
                        + " RequestId: " + e.getRequestId() + " - " + changeSetDescription,
                CONNECTOR_COLOR);
    }

    /**
     * Handle the ConnectedEvent.
     *
     * @param e the event.
     */
    protected void onConnected(@NonNull final ConnectedEvent e) {
        log("%cConnector Connected. System Schema: " + e.getSystemSchemaName(), CONNECTOR_COLOR);
    }

    /**
     * Handle the ConnectFailureEvent.
     *
     * @param e the event.
     */
    protected void onConnectFailure(@NonNull final ConnectFailureEvent e) {
        log("%cConnector Connect Failed. System Schema: " + e.getSystemSchemaName(), ERROR_COLOR);
    }

    /**
     * Handle the DisconnectedEvent.
     *
     * @param e the event.
     */
    protected void onDisconnected(@NonNull final DisconnectedEvent e) {
        log("%cConnector Disconnected. System Schema: " + e.getSystemSchemaName(), CONNECTOR_COLOR);
    }

    /**
     * Handle the DisconnectFailureEvent.
     *
     * @param e the event.
     */
    protected void onDisconnectFailure(@NonNull final DisconnectFailureEvent e) {
        log("%cConnector Disconnect Failed. System Schema: " + e.getSystemSchemaName(), ERROR_COLOR);
    }

    /**
     * Handle the MessageProcessedEvent.
     *
     * @param e the event.
     */
    protected void onMessageProcessed(@NonNull final MessageProcessedEvent e) {
        log("%cConnector Processed Message " + e.getDataLoadStatus(), CONNECTOR_COLOR);
    }

    /**
     * Handle the MessageProcessFailureEvent.
     *
     * @param e the event.
     */
    protected void onMessageProcessFailure(@NonNull final MessageProcessFailureEvent e) {
        log(
                "%cConnector Error Processing Message. System Schema: " + e.getSystemSchemaName() + " Error: "
                        + e.getError(),
                ERROR_COLOR);
    }

    /**
     * Handle the MessageReadFailureEvent.
     *
     * @param e the event.
     */
    protected void onMessageReadFailure(@NonNull final MessageReadFailureEvent e) {
        log("%cConnector Error Reading Message. System Schema: " + e.getSystemSchemaName(), ERROR_COLOR);
    }

    /**
     * Handle the RestartEvent.
     *
     * @param e the event.
     */
    protected void onRestart(@NonNull final RestartEvent e) {
        log(
                "%cConnector attempting to disconnect and restart due to error. System Schema: "
                        + e.getSystemSchemaName(),
                ERROR_COLOR);
    }

    /**
     * Handle the SubscribeCompletedEvent.
     *
     * @param e the event.
     */
    protected void onSubscribeCompleted(@NonNull final SubscribeCompletedEvent e) {
        log(
                "%cConnector completed subscribe. System Schema: " + e.getSystemSchemaName() + " Dataset Address: "
                        + e.getDatasetAddress(),
                CONNECTOR_COLOR);
    }

    /**
     * Handle the SubscribeStartedEvent.
     *
     * @param e the event.
     */
    protected void onSubscribeStarted(@NonNull final SubscribeStartedEvent e) {
        log(
                "%cConnector started subscribe. System Schema: " + e.getSystemSchemaName() + " Dataset Address: "
                        + e.getDatasetAddress(),
                CONNECTOR_COLOR);
    }

    /**
     * Handle the SubscriptionUpdateCompletedEvent.
     *
     * @param e the event.
     */
    protected void onSubscriptionUpdateCompleted(@NonNull final SubscriptionUpdateCompletedEvent e) {
        log(
                "%cConnector completed subscription update. System Schema: " + e.getSystemSchemaName()
                        + " Dataset Address: " + e.getDatasetAddress(),
                CONNECTOR_COLOR);
    }

    /**
     * Handle the SubscriptionUpdateStartedEvent.
     *
     * @param e the event.
     */
    protected void onSubscriptionUpdateStarted(@NonNull final SubscriptionUpdateStartedEvent e) {
        log(
                "%cConnector started subscribe. System Schema: " + e.getSystemSchemaName() + " Dataset Address: "
                        + e.getDatasetAddress(),
                CONNECTOR_COLOR);
    }

    /**
     * Handle the UnsubscribeCompletedEvent.
     *
     * @param e the event.
     */
    protected void onUnsubscribeCompleted(@NonNull final UnsubscribeCompletedEvent e) {
        log(
                "%cConnector completed unsubscribe. System Schema: " + e.getSystemSchemaName() + " Dataset Address: "
                        + e.getDatasetAddress(),
                CONNECTOR_COLOR);
    }

    /**
     * Handle the UnsubscribeStartedEvent.
     *
     * @param e the event.
     */
    protected void onUnsubscribeStarted(@NonNull final UnsubscribeStartedEvent e) {
        log(
                "%cConnector started unsubscribe. System Schema: " + e.getSystemSchemaName() + " Dataset Address: "
                        + e.getDatasetAddress(),
                CONNECTOR_COLOR);
    }

    /**
     * Handle the AreaOfInterestCreatedEvent.
     *
     * @param e the event.
     */
    protected void onAreaOfInterestCreated(@NonNull final AreaOfInterestCreatedEvent e) {
        final AreaOfInterest areaOfInterest = e.getAreaOfInterest();
        final Object filterParameter = areaOfInterest.getFilterParameter();
        final String filterParameterString =
                null == filterParameter ? "" : " - " + FilterParameterUtil.filterParameterToString(filterParameter);
        log(
                "%cAreaOfInterest Created " + areaOfInterest.getDatasetAddress() + filterParameterString,
                AREA_OF_INTEREST_COLOR);
    }

    /**
     * Handle the AreaOfInterestFilterParameterUpdatedEvent.
     *
     * @param e the event.
     */
    protected void onAreaOfInterestFilterParameterUpdated(@NonNull final AreaOfInterestFilterParameterUpdatedEvent e) {
        final AreaOfInterest areaOfInterest = e.getAreaOfInterest();
        final Object filterParameter = areaOfInterest.getFilterParameter();
        final String filterParameterString = FilterParameterUtil.filterParameterToString(filterParameter);
        log(
                "%cAreaOfInterest Filter Parameter Updated " + areaOfInterest.getDatasetAddress() + " - "
                        + filterParameterString,
                AREA_OF_INTEREST_COLOR);
    }

    /**
     * Handle the AreaOfInterestDisposedEvent.
     *
     * @param e the event.
     */
    protected void onAreaOfInterestDisposed(@NonNull final AreaOfInterestDisposedEvent e) {
        log("%cAreaOfInterest Disposed " + e.getAreaOfInterest().getDatasetAddress(), AREA_OF_INTEREST_COLOR);
    }

    /**
     * Handle the SubscribeRequestQueuedEvent.
     *
     * @param e the event.
     */
    protected void onSubscribeRequestQueued(@NonNull final SubscribeRequestQueuedEvent e) {
        final Object filterParameter = e.getFilterParameter();
        final String filterParameterString =
                null == filterParameter ? "" : " - " + FilterParameterUtil.filterParameterToString(filterParameter);
        log("%cSubscribe Request Queued " + e.getDatasetAddress() + filterParameterString, SUBSCRIPTION_COLOR);
    }

    /**
     * Handle the SubscriptionUpdateRequestQueuedEvent.
     *
     * @param e the event.
     */
    protected void onSubscriptionUpdateRequestQueued(@NonNull final SubscriptionUpdateRequestQueuedEvent e) {
        final Object filterParameter = e.getFilterParameter();
        final String filterParameterString =
                null == filterParameter ? "" : " - " + FilterParameterUtil.filterParameterToString(filterParameter);
        log(
                "%cSubscription Update Request Queued " + e.getDatasetAddress() + filterParameterString,
                SUBSCRIPTION_COLOR);
    }

    /**
     * Handle the UnsubscribeRequestQueuedEvent.
     *
     * @param e the event.
     */
    protected void onUnsubscribeRequestQueued(@NonNull final UnsubscribeRequestQueuedEvent e) {
        log("%cUnsubscribe Request Queued " + e.getDatasetAddress(), SUBSCRIPTION_COLOR);
    }

    /**
     * Handle the SubscriptionCreatedEvent.
     *
     * @param e the event.
     */
    protected void onSubscriptionCreated(@NonNull final SubscriptionCreatedEvent e) {
        final Subscription subscription = e.getSubscription();
        final Object filterParameter = subscription.getFilterParameter();
        final String filterParameterString =
                null == filterParameter ? "" : " - " + FilterParameterUtil.filterParameterToString(filterParameter);
        log("%cSubscription Created " + subscription.datasetAddress() + filterParameterString, SUBSCRIPTION_COLOR);
    }

    /**
     * Handle the SubscriptionDisposedEvent.
     *
     * @param e the event.
     */
    protected void onSubscriptionDisposed(@NonNull final SubscriptionDisposedEvent e) {
        log("%cSubscription Disposed " + e.getSubscription().datasetAddress(), SUBSCRIPTION_COLOR);
    }

    /**
     * Handle the SubscriptionOrphanedEvent.
     *
     * @param e the event.
     */
    protected void onSubscriptionOrphaned(@NonNull final SubscriptionOrphanedEvent e) {
        log("%cSubscription Orphaned " + e.getSubscription().datasetAddress(), SUBSCRIPTION_COLOR);
    }

    /**
     * Handle the ExecStartedEvent.
     *
     * @param e the event.
     */
    protected void onExecStarted(@NonNull final ExecStartedEvent e) {
        log("%cExec Started " + e.getCommand(), EXEC_COLOR);
    }

    /**
     * Handle the ExecStartedEvent.
     *
     * @param e the event.
     */
    protected void onExecCompleted(@NonNull final ExecCompletedEvent e) {
        log("%cExec Completed " + e.getCommand(), EXEC_COLOR);
    }

    /**
     * Handle the ExecRequestQueuedEvent.
     *
     * @param e the event.
     */
    protected void onExecRequestQueued(@NonNull final ExecRequestQueuedEvent e) {
        log("%cExec Request Queued  " + e.getCommand(), EXEC_COLOR);
    }

    /**
     * Log specified message with parameters
     *
     * @param message the message.
     * @param styling the styling parameter. It is assumed that the message has a %c somewhere in it to identify the start of the styling.
     */
    protected void log(@NonNull final String message, @CssRules @NonNull final String styling) {
        Console.log(message, styling);
    }

    @Override
    protected void handleUnhandledEvent(@NonNull final Object event) {
        Console.log(event);
    }
}
