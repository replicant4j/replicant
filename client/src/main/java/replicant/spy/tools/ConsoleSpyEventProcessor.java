package replicant.spy.tools;

import elemental2.dom.DomGlobal;
import javax.annotation.Nonnull;
import replicant.AreaOfInterest;
import replicant.FilterUtil;
import replicant.Subscription;
import replicant.spy.AreaOfInterestCreatedEvent;
import replicant.spy.AreaOfInterestDisposedEvent;
import replicant.spy.AreaOfInterestFilterUpdatedEvent;
import replicant.spy.AreaOfInterestStatusUpdatedEvent;
import replicant.spy.ConnectFailureEvent;
import replicant.spy.ConnectedEvent;
import replicant.spy.DisconnectFailureEvent;
import replicant.spy.DisconnectedEvent;
import replicant.spy.InSyncEvent;
import replicant.spy.MessageProcessFailureEvent;
import replicant.spy.MessageProcessedEvent;
import replicant.spy.MessageReadFailureEvent;
import replicant.spy.OutOfSyncEvent;
import replicant.spy.RequestCompletedEvent;
import replicant.spy.RequestStartedEvent;
import replicant.spy.RestartEvent;
import replicant.spy.SubscribeCompletedEvent;
import replicant.spy.SubscribeFailedEvent;
import replicant.spy.SubscribeRequestQueuedEvent;
import replicant.spy.SubscribeStartedEvent;
import replicant.spy.SubscriptionCreatedEvent;
import replicant.spy.SubscriptionDisposedEvent;
import replicant.spy.SubscriptionOrphanedEvent;
import replicant.spy.SubscriptionUpdateCompletedEvent;
import replicant.spy.SubscriptionUpdateFailedEvent;
import replicant.spy.SubscriptionUpdateRequestQueuedEvent;
import replicant.spy.SubscriptionUpdateStartedEvent;
import replicant.spy.SyncFailureEvent;
import replicant.spy.UnsubscribeCompletedEvent;
import replicant.spy.UnsubscribeFailedEvent;
import replicant.spy.UnsubscribeRequestQueuedEvent;
import replicant.spy.UnsubscribeStartedEvent;

/**
 * A SpyEventHandler that prints spy events to the tools console.
 * The events are colored to make them easy to digest. This class is designed to be easy to sub-class.
 */
@SuppressWarnings( "WeakerAccess" )
public class ConsoleSpyEventProcessor
  extends AbstractSpyEventProcessor
{
  @CssRules
  private static final String CONNECTOR_COLOR = "color: #F5A402; font-weight: normal;";
  @CssRules
  private static final String SUBSCRIPTION_COLOR = "color: #0FA13B; font-weight: normal;";
  @CssRules
  private static final String AREA_OF_INTEREST_COLOR = "color: #006AEB; font-weight: normal;";
  @CssRules
  private static final String ERROR_COLOR = "color: #A10001; font-weight: normal;";

  /**
   * Create the processor.
   */
  public ConsoleSpyEventProcessor()
  {
    on( AreaOfInterestCreatedEvent.class, this::onAreaOfInterestCreated );
    on( AreaOfInterestFilterUpdatedEvent.class, this::onAreaOfInterestFilterUpdated );
    on( AreaOfInterestStatusUpdatedEvent.class, this::onAreaOfInterestStatusUpdated );
    on( AreaOfInterestDisposedEvent.class, this::onAreaOfInterestDisposed );

    on( SubscriptionCreatedEvent.class, this::onSubscriptionCreated );
    on( SubscriptionDisposedEvent.class, this::onSubscriptionDisposed );
    on( SubscriptionOrphanedEvent.class, this::onSubscriptionOrphaned );

    on( ConnectedEvent.class, this::onConnected );
    on( ConnectFailureEvent.class, this::onConnectFailure );
    on( DisconnectedEvent.class, this::onDisconnected );
    on( DisconnectFailureEvent.class, this::onDisconnectFailure );
    on( MessageProcessedEvent.class, this::onMessageProcessed );
    on( MessageProcessFailureEvent.class, this::onMessageProcessFailure );
    on( MessageReadFailureEvent.class, this::onMessageReadFailure );
    on( RestartEvent.class, this::onRestart );
    on( SubscribeRequestQueuedEvent.class, this::onSubscribeRequestQueued );
    on( SubscribeCompletedEvent.class, this::onSubscribeCompleted );
    on( SubscribeFailedEvent.class, this::onSubscribeFailed );
    on( SubscribeStartedEvent.class, this::onSubscribeStarted );
    on( SubscriptionUpdateRequestQueuedEvent.class, this::onSubscriptionUpdateRequestQueued );
    on( SubscriptionUpdateCompletedEvent.class, this::onSubscriptionUpdateCompleted );
    on( SubscriptionUpdateFailedEvent.class, this::onSubscriptionUpdateFailed );
    on( SubscriptionUpdateStartedEvent.class, this::onSubscriptionUpdateStarted );
    on( UnsubscribeRequestQueuedEvent.class, this::onUnsubscribeRequestQueued );
    on( UnsubscribeCompletedEvent.class, this::onUnsubscribeCompleted );
    on( UnsubscribeFailedEvent.class, this::onUnsubscribeFailed );
    on( UnsubscribeStartedEvent.class, this::onUnsubscribeStarted );

    on( RequestStartedEvent.class, this::onRequestStarted );
    on( RequestCompletedEvent.class, this::onRequestCompleted );

    on( InSyncEvent.class, this::onInSync );
    on( OutOfSyncEvent.class, this::onOutOfSync );
    on( SyncFailureEvent.class, this::onSyncFailure );
  }

  /**
   * Handle the InSyncEvent.
   *
   * @param e the event.
   */
  protected void onInSync( @Nonnull final InSyncEvent e )
  {
    log( "%Sync completed. Backend synchronized. SchemaId: " + e.getSchemaId(), CONNECTOR_COLOR );
  }

  /**
   * Handle the OutOfSyncEvent.
   *
   * @param e the event.
   */
  protected void onOutOfSync( @Nonnull final OutOfSyncEvent e )
  {
    log( "%Sync completed. Backend not synchronized. SchemaId: " + e.getSchemaId(), CONNECTOR_COLOR );
  }

  /**
   * Handle the ConnectFailureEvent.
   *
   * @param e the event.
   */
  protected void onSyncFailure( @Nonnull final SyncFailureEvent e )
  {
    log( "%cSync Failed. SchemaId: " + e.getSchemaId() + " Error: " + e.getError(), ERROR_COLOR );
  }

  /**
   * Handle the RequestStartedEvent.
   *
   * @param e the event.
   */
  protected void onRequestStarted( @Nonnull final RequestStartedEvent e )
  {
    log( "%cRequest started. System: " + e.getName() + " Request: " + e.getName() +
         " RequestId: " + e.getRequestId(), CONNECTOR_COLOR );
  }

  /**
   * Handle the RequestCompletedEvent.
   *
   * @param e the event.
   */
  protected void onRequestCompleted( @Nonnull final RequestCompletedEvent e )
  {
    final String changeSetDescription =
      !e.isExpectingResults() ? "No Change set expected." :
      e.haveResultsArrived() ? "Change set has already arrived." :
      "Change set has not arrived.";

    log( "%cRequest completed " + ( e.isNormalCompletion() ? " normally" : "with an exception" ) +
         ". System: " + e.getSchemaName() + " Request: " + e.getName() +
         " RequestId: " + e.getRequestId() + " - " + changeSetDescription, CONNECTOR_COLOR );
  }

  /**
   * Handle the ConnectedEvent.
   *
   * @param e the event.
   */
  protected void onConnected( @Nonnull final ConnectedEvent e )
  {
    log( "%cConnector Connected. System: " + e.getSchemaName(), CONNECTOR_COLOR );
  }

  /**
   * Handle the ConnectFailureEvent.
   *
   * @param e the event.
   */
  protected void onConnectFailure( @Nonnull final ConnectFailureEvent e )
  {
    log( "%cConnector Connect Failed. System: " + e.getSchemaName() + " Error: " + e.getError(),
         ERROR_COLOR );
  }

  /**
   * Handle the DisconnectedEvent.
   *
   * @param e the event.
   */
  protected void onDisconnected( @Nonnull final DisconnectedEvent e )
  {
    log( "%cConnector Disconnected. System: " + e.getSchemaName(), CONNECTOR_COLOR );
  }

  /**
   * Handle the DisconnectFailureEvent.
   *
   * @param e the event.
   */
  protected void onDisconnectFailure( @Nonnull final DisconnectFailureEvent e )
  {
    log( "%cConnector Disconnect Failed. System: " + e.getSchemaName() + " Error: " + e.getError(),
         ERROR_COLOR );
  }

  /**
   * Handle the MessageProcessedEvent.
   *
   * @param e the event.
   */
  protected void onMessageProcessed( @Nonnull final MessageProcessedEvent e )
  {
    log( "%cConnector Processed Message " + e.getDataLoadStatus(), CONNECTOR_COLOR );
  }

  /**
   * Handle the MessageProcessFailureEvent.
   *
   * @param e the event.
   */
  protected void onMessageProcessFailure( @Nonnull final MessageProcessFailureEvent e )
  {
    log( "%cConnector Error Processing Message. System: " +
         e.getSchemaName() + " Error: " + e.getError(), ERROR_COLOR );
  }

  /**
   * Handle the MessageReadFailureEvent.
   *
   * @param e the event.
   */
  protected void onMessageReadFailure( @Nonnull final MessageReadFailureEvent e )
  {
    log( "%cConnector Error Reading Message. System: " + e.getSchemaName() + " Error: " + e.getError(),
         ERROR_COLOR );
  }

  /**
   * Handle the RestartEvent.
   *
   * @param e the event.
   */
  protected void onRestart( @Nonnull final RestartEvent e )
  {
    log( "%cConnector attempting to disconnect and restart due to error. System: " +
         e.getSchemaName() + " Error: " + e.getError(), ERROR_COLOR );
  }

  /**
   * Handle the SubscribeCompletedEvent.
   *
   * @param e the event.
   */
  protected void onSubscribeCompleted( @Nonnull final SubscribeCompletedEvent e )
  {
    log( "%cConnector completed subscribe. System: " + e.getSchemaName() +
         " Address: " + e.getAddress(), CONNECTOR_COLOR );
  }

  /**
   * Handle the SubscribeStartedEvent.
   *
   * @param e the event.
   */
  protected void onSubscribeFailed( @Nonnull final SubscribeFailedEvent e )
  {
    log( "%cConnector subscribe failed. System: " + e.getSchemaName() +
         " Address: " + e.getAddress() + " Error: " + e.getError(), ERROR_COLOR );
  }

  /**
   * Handle the SubscribeStartedEvent.
   *
   * @param e the event.
   */
  protected void onSubscribeStarted( @Nonnull final SubscribeStartedEvent e )
  {
    log( "%cConnector started subscribe. System: " + e.getSchemaName() +
         " Address: " + e.getAddress(), CONNECTOR_COLOR );
  }

  /**
   * Handle the SubscriptionUpdateCompletedEvent.
   *
   * @param e the event.
   */
  protected void onSubscriptionUpdateCompleted( @Nonnull final SubscriptionUpdateCompletedEvent e )
  {
    log( "%cConnector completed subscribe. System: " + e.getSchemaName() +
         " Address: " + e.getAddress(), CONNECTOR_COLOR );
  }

  /**
   * Handle the SubscriptionUpdateStartedEvent.
   *
   * @param e the event.
   */
  protected void onSubscriptionUpdateFailed( @Nonnull final SubscriptionUpdateFailedEvent e )
  {
    log( "%cConnector subscription update failed. System: " + e.getSchemaName() +
         " Address: " + e.getAddress() + " Error: " + e.getError(), ERROR_COLOR );
  }

  /**
   * Handle the SubscriptionUpdateStartedEvent.
   *
   * @param e the event.
   */
  protected void onSubscriptionUpdateStarted( @Nonnull final SubscriptionUpdateStartedEvent e )
  {
    log( "%cConnector started subscribe. System: " + e.getSchemaName() +
         " Address: " + e.getAddress(), CONNECTOR_COLOR );
  }

  /**
   * Handle the UnsubscribeCompletedEvent.
   *
   * @param e the event.
   */
  protected void onUnsubscribeCompleted( @Nonnull final UnsubscribeCompletedEvent e )
  {
    log( "%cConnector completed unsubscribe. System: " + e.getSchemaName() +
         " Address: " + e.getAddress(), CONNECTOR_COLOR );
  }

  /**
   * Handle the UnsubscribeStartedEvent.
   *
   * @param e the event.
   */
  protected void onUnsubscribeFailed( @Nonnull final UnsubscribeFailedEvent e )
  {
    log( "%cConnector unsubscribe failed. System: " + e.getSchemaName() +
         " Address: " + e.getAddress() + " Error: " + e.getError(), ERROR_COLOR );
  }

  /**
   * Handle the UnsubscribeStartedEvent.
   *
   * @param e the event.
   */
  protected void onUnsubscribeStarted( @Nonnull final UnsubscribeStartedEvent e )
  {
    log( "%cConnector started unsubscribe. System: " + e.getSchemaName() +
         " Address: " + e.getAddress(), CONNECTOR_COLOR );
  }

  /**
   * Handle the AreaOfInterestCreatedEvent.
   *
   * @param e the event.
   */
  protected void onAreaOfInterestCreated( @Nonnull final AreaOfInterestCreatedEvent e )
  {
    final AreaOfInterest areaOfInterest = e.getAreaOfInterest();
    final Object filter = areaOfInterest.getFilter();
    final String filterString = null == filter ? "" : " - " + FilterUtil.filterToString( filter );
    log( "%cAreaOfInterest Created " + areaOfInterest.getAddress() + filterString, AREA_OF_INTEREST_COLOR );
  }

  /**
   * Handle the AreaOfInterestFilterUpdatedEvent.
   *
   * @param e the event.
   */
  protected void onAreaOfInterestFilterUpdated( @Nonnull final AreaOfInterestFilterUpdatedEvent e )
  {
    final AreaOfInterest areaOfInterest = e.getAreaOfInterest();
    final Object filter = areaOfInterest.getFilter();
    final String filterString = FilterUtil.filterToString( filter );
    log( "%cAreaOfInterest Filter Updated " + areaOfInterest.getAddress() + " - " + filterString,
         AREA_OF_INTEREST_COLOR );
  }

  /**
   * Handle the AreaOfInterestStatusUpdatedEvent.
   *
   * @param e the event.
   */
  protected void onAreaOfInterestStatusUpdated( @Nonnull final AreaOfInterestStatusUpdatedEvent e )
  {
    final AreaOfInterest areaOfInterest = e.getAreaOfInterest();
    final Object filter = areaOfInterest.getFilter();
    final String filterString = null == filter ? "" : " - " + FilterUtil.filterToString( filter );
    log( "%cAreaOfInterest Status Updated " + areaOfInterest.getAddress() + filterString, AREA_OF_INTEREST_COLOR );
  }

  /**
   * Handle the AreaOfInterestDisposedEvent.
   *
   * @param e the event.
   */
  protected void onAreaOfInterestDisposed( @Nonnull final AreaOfInterestDisposedEvent e )
  {
    log( "%cAreaOfInterest Disposed " + e.getAreaOfInterest().getAddress(), AREA_OF_INTEREST_COLOR );
  }

  /**
   * Handle the SubscribeRequestQueuedEvent.
   *
   * @param e the event.
   */
  protected void onSubscribeRequestQueued( @Nonnull final SubscribeRequestQueuedEvent e )
  {
    final Object filter = e.getFilter();
    final String filterString = null == filter ? "" : " - " + FilterUtil.filterToString( filter );
    log( "%cSubscribe Request Queued " + e.getAddress() + filterString, SUBSCRIPTION_COLOR );
  }

  /**
   * Handle the SubscriptionUpdateRequestQueuedEvent.
   *
   * @param e the event.
   */
  protected void onSubscriptionUpdateRequestQueued( @Nonnull final SubscriptionUpdateRequestQueuedEvent e )
  {
    final Object filter = e.getFilter();
    final String filterString = null == filter ? "" : " - " + FilterUtil.filterToString( filter );
    log( "%cSubscription Update Request Queued " + e.getAddress() + filterString, SUBSCRIPTION_COLOR );
  }

  /**
   * Handle the UnsubscribeRequestQueuedEvent.
   *
   * @param e the event.
   */
  protected void onUnsubscribeRequestQueued( @Nonnull final UnsubscribeRequestQueuedEvent e )
  {
    log( "%cUnsubscribe Request Queued " + e.getAddress(), SUBSCRIPTION_COLOR );
  }

  /**
   * Handle the SubscriptionCreatedEvent.
   *
   * @param e the event.
   */
  protected void onSubscriptionCreated( @Nonnull final SubscriptionCreatedEvent e )
  {
    final Subscription subscription = e.getSubscription();
    final Object filter = subscription.getFilter();
    final String filterString = null == filter ? "" : " - " + FilterUtil.filterToString( filter );
    log( "%cSubscription Created " + subscription.getAddress() + filterString, SUBSCRIPTION_COLOR );
  }

  /**
   * Handle the SubscriptionDisposedEvent.
   *
   * @param e the event.
   */
  protected void onSubscriptionDisposed( @Nonnull final SubscriptionDisposedEvent e )
  {
    log( "%cSubscription Disposed " + e.getSubscription().getAddress(), SUBSCRIPTION_COLOR );
  }

  /**
   * Handle the SubscriptionOrphanedEvent.
   *
   * @param e the event.
   */
  protected void onSubscriptionOrphaned( @Nonnull final SubscriptionOrphanedEvent e )
  {
    log( "%cSubscription Orphaned " + e.getSubscription().getAddress(), SUBSCRIPTION_COLOR );
  }

  /**
   * Log specified message with parameters
   *
   * @param message the message.
   * @param styling the styling parameter. It is assumed that the message has a %c somewhere in it to identify the start of the styling.
   */
  protected void log( @Nonnull final String message,
                      @CssRules @Nonnull final String styling )
  {
    DomGlobal.console.log( message, styling );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void handleUnhandledEvent( @Nonnull final Object event )
  {
    DomGlobal.console.log( event );
  }
}
