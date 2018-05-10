package replicant.spy.tools;

import elemental2.dom.DomGlobal;
import javax.annotation.Nonnull;
import org.realityforge.anodoc.Unsupported;
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
import replicant.spy.MessageProcessFailureEvent;
import replicant.spy.MessageProcessedEvent;
import replicant.spy.MessageReadFailureEvent;
import replicant.spy.RestartEvent;
import replicant.spy.SubscribeCompletedEvent;
import replicant.spy.SubscriptionCreatedEvent;
import replicant.spy.SubscriptionDisposedEvent;
import replicant.spy.SubscriptionOrphanedEvent;

/**
 * A SpyEventHandler that prints spy events to the tools console.
 * The events are colored to make them easy to digest. This class is designed to be easy to sub-class.
 */
@SuppressWarnings( "WeakerAccess" )
@Unsupported( "This class relies on unstable spy API and will likely evolve as the api evolves" )
public class ConsoleSpyEventProcessor
  extends AbstractSpyEventProcessor
{
  @CssRules
  private static final String ENTITY_COLOR = "color: #CF8A3B; font-weight: normal;";
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
    on( SubscribeCompletedEvent.class, this::onSubscribeCompleted );

  }

  /**
   * Handle the ConnectedEvent.
   *
   * @param e the event.
   */
  protected void onConnected( @Nonnull final ConnectedEvent e )
  {
    log( "%cConnector Connected. System: " + e.getSystemType().getSimpleName(), CONNECTOR_COLOR );
  }

  /**
   * Handle the ConnectFailureEvent.
   *
   * @param e the event.
   */
  protected void onConnectFailure( @Nonnull final ConnectFailureEvent e )
  {
    log( "%cConnector Connect Failed. System: " + e.getSystemType().getSimpleName() + " Error: " + e.getError(),
         ERROR_COLOR );
  }

  /**
   * Handle the DisconnectedEvent.
   *
   * @param e the event.
   */
  protected void onDisconnected( @Nonnull final DisconnectedEvent e )
  {
    log( "%cConnector Disconnected. System: " + e.getSystemType().getSimpleName(), CONNECTOR_COLOR );
  }

  /**
   * Handle the DisconnectFailureEvent.
   *
   * @param e the event.
   */
  protected void onDisconnectFailure( @Nonnull final DisconnectFailureEvent e )
  {
    log( "%cConnector Disconnect Failed. System: " + e.getSystemType().getSimpleName() + " Error: " + e.getError(),
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
         e.getSystemType().getSimpleName() + " Error: " + e.getError(), ERROR_COLOR );
  }

  /**
   * Handle the MessageReadFailureEvent.
   *
   * @param e the event.
   */
  protected void onMessageReadFailure( @Nonnull final MessageReadFailureEvent e )
  {
    log( "%cConnector Error Reading Message. System: " + e.getSystemType().getSimpleName() + " Error: " + e.getError(),
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
         e.getSystemType().getSimpleName() + " Error: " + e.getError(), ERROR_COLOR );
  }

  /**
   * Handle the SubscribeCompletedEvent.
   *
   * @param e the event.
   */
  protected void onSubscribeCompleted( @Nonnull final SubscribeCompletedEvent e )
  {
    log( "%cConnector completed subscribe. System: " + e.getSystemType().getSimpleName() +
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
