package replicant;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import static org.realityforge.braincheck.Guards.*;

/**
 * Application event system support class.
 */
public final class ApplicationEventBroker
{
  /**
   * The list of application handlers to call when an event is received.
   */
  @Nonnull
  private final List<ApplicationEventHandler> _applicationEventHandlers = new ArrayList<>();

  /**
   * Add a application handler to the list of handlers.
   * The handler should not already be in the list.
   *
   * @param handler the application handler.
   */
  public void addApplicationEventHandler( @Nonnull final ApplicationEventHandler handler )
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> !_applicationEventHandlers.contains( handler ),
                    () -> "Replicant-0089: Attempting to add handler " + handler + " that is already " +
                          "in the list of application handlers." );
    }
    _applicationEventHandlers.add( Objects.requireNonNull( handler ) );
  }

  /**
   * Remove application handler from list of existing handlers.
   * The handler should already be in the list.
   *
   * @param handler the application handler.
   */
  public void removeApplicationEventHandler( @Nonnull final ApplicationEventHandler handler )
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> _applicationEventHandlers.contains( handler ),
                    () -> "Replicant-0090: Attempting to remove handler " + handler + " that is not " +
                          "in the list of application handlers." );
    }
    _applicationEventHandlers.remove( Objects.requireNonNull( handler ) );
  }

  /**
   * Report an application event in the Replicant system.
   *
   * @param event the event that occurred.
   */
  public void reportApplicationEvent( @Nonnull final Object event )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( this::willPropagateApplicationEvents,
                 () -> "Replicant-0091: Attempting to report ApplicationEvent '" + event + "' but " +
                       "willPropagateApplicationEvents() returns false." );
    }
    for ( final ApplicationEventHandler handler : _applicationEventHandlers )
    {
      try
      {
        handler.onApplicationEvent( event );
      }
      catch ( final Throwable error )
      {
        final String message =
          ReplicantUtil.safeGetString( () -> "Exception when notifying application handler '" + handler +
                                             "' of '" + event + "' event." );
        ReplicantLogger.log( message, error );
      }
    }
  }

  /**
   * Return true if application events will be propagated.
   * This means application events are enabled and there is at least one application event handler present.
   *
   * @return true if application events will be propagated, false otherwise.
   */
  public boolean willPropagateApplicationEvents()
  {
    return Replicant.areEventsEnabled() && !getApplicationEventHandlers().isEmpty();
  }

  @Nonnull
  List<ApplicationEventHandler> getApplicationEventHandlers()
  {
    return _applicationEventHandlers;
  }
}
