package replicant;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import static org.realityforge.braincheck.Guards.*;

/**
 * Class supporting the propagation of events to SpyEventHandler callbacks.
 */
final class SpyImpl
  implements Spy
{
  /**
   * The list of spy handlers to call when an event is received.
   */
  @Nonnull
  private final List<SpyEventHandler> _spyEventHandlers = new ArrayList<>();

  @Override
  public void addSpyEventHandler( @Nonnull final SpyEventHandler handler )
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> !_spyEventHandlers.contains( handler ),
                    () -> "Replicant-0040: Attempting to add handler " + handler + " that is already " +
                          "in the list of spy handlers." );
    }
    _spyEventHandlers.add( Objects.requireNonNull( handler ) );
  }

  @Override
  public void removeSpyEventHandler( @Nonnull final SpyEventHandler handler )
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> _spyEventHandlers.contains( handler ),
                    () -> "Replicant-0039: Attempting to remove handler " + handler + " that is not " +
                          "in the list of spy handlers." );
    }
    _spyEventHandlers.remove( Objects.requireNonNull( handler ) );
  }

  @Override
  public void reportSpyEvent( @Nonnull final Object event )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( this::willPropagateSpyEvents,
                 () -> "Replicant-0038: Attempting to report SpyEvent '" + event + "' but " +
                       "willPropagateSpyEvents() returns false." );
    }
    for ( final SpyEventHandler handler : _spyEventHandlers )
    {
      try
      {
        handler.onSpyEvent( event );
      }
      catch ( final Throwable error )
      {
        final String message =
          ReplicantUtil.safeGetString( () -> "Exception when notifying spy handler '" + handler +
                                             "' of '" + event + "' event." );
        ReplicantLogger.log( message, error );
      }
    }
  }

  @Override
  public boolean willPropagateSpyEvents()
  {
    return Replicant.areSpiesEnabled() && !getSpyEventHandlers().isEmpty();
  }

  @Nonnull
  List<SpyEventHandler> getSpyEventHandlers()
  {
    return _spyEventHandlers;
  }
}
