package replicant.spy.tools;

import elemental2.dom.DomGlobal;
import javax.annotation.Nonnull;
import org.realityforge.anodoc.Unsupported;
import replicant.spy.AreaOfInterestCreatedEvent;

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
  }

  /**
   * Handle the AreaOfInterestCreatedEvent.
   *
   * @param e the event.
   */
  protected void onAreaOfInterestCreated( @Nonnull final AreaOfInterestCreatedEvent e )
  {
    log( "%cAreaOfInterest Created " + e.getAreaOfInterest().getChannel(), AREA_OF_INTEREST_COLOR );
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
