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
import replicant.spy.SubscriptionCreatedEvent;
import replicant.spy.SubscriptionDisposedEvent;

/**
 * A SpyEventHandler that prints spy events to the tools console.
 * The events are colored to make them easy to digest. This class is designed to be easy to sub-class.
 */
@Unsupported( "This class relies on unstable spy API and will likely evolve as the api evolves" )
public class ConsoleSpyEventProcessor
  extends AbstractSpyEventProcessor
{
  @CssRules
  private static final String ENTITY_COLOR = "color: #CF8A3B; font-weight: normal;";
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
    final String filterString = FilterUtil.filterToString( filter );
    log( "%cAreaOfInterest Status Updated " + areaOfInterest.getAddress() + " - " + filterString,
         AREA_OF_INTEREST_COLOR );
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
