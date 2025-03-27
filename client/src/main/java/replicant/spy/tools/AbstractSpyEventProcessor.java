package replicant.spy.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import replicant.Replicant;
import replicant.SpyEventHandler;
import static org.realityforge.braincheck.Guards.*;

/**
 * Abstract base class for processing spy events.
 * Simplifies handling of events by delegating to a specific processor
 * based on types of the events. Note that the type must be the concrete
 * type of the subclass.
 */
public abstract class AbstractSpyEventProcessor
  implements SpyEventHandler
{
  /**
   * The processors that can be delegated to.
   */
  @Nonnull
  private final Map<Class<?>, Consumer<?>> _processors = new HashMap<>();

  /**
   * Method invoked by subclasses to register
   *
   * @param <T>       the event type.
   * @param type      the type of the event to register.
   * @param processor the processor to handle event with.
   */
  protected final <T> void on( @Nonnull final Class<T> type, @Nonnull final Consumer<T> processor )
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> !_processors.containsKey( type ),
                    () -> "Replicant-0036: Attempting to call AbstractSpyEventProcessor.on() to register a processor " +
                          "for type " + type + " but an existing processor already exists for type" );
    }
    _processors.put( type, processor );
  }

  /**
   * Handle the specified event by delegating to the registered processor.
   *
   * @param event the event that occurred.
   */
  @Override
  @SuppressWarnings( { "ConstantConditions", "unchecked" } )
  public final void onSpyEvent( @Nonnull final Object event )
  {
    assert null != event;
    final Consumer<Object> processor = (Consumer<Object>) _processors.get( event.getClass() );
    if ( null != processor )
    {
      processor.accept( event );
    }
    else
    {
      handleUnhandledEvent( event );
    }
  }

  /**
   * Handle the specified event that had no processors defined for it.
   *
   * @param event the unhandled event.
   */
  protected void handleUnhandledEvent( @Nonnull final Object event )
  {
  }
}
