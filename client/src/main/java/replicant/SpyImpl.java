package replicant;

import java.util.ArrayList;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.realityforge.anodoc.TestOnly;
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
  private final ArrayList<SpyEventHandler> _spyEventHandlers = new ArrayList<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public void addSpyEventHandler( @Nonnull final SpyEventHandler handler )
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> !_spyEventHandlers.contains( handler ),
                    () -> "Replicant-0102: Attempting to add handler " + handler + " that is already " +
                          "in the list of spy handlers." );
    }
    _spyEventHandlers.add( Objects.requireNonNull( handler ) );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeSpyEventHandler( @Nonnull final SpyEventHandler handler )
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> _spyEventHandlers.contains( handler ),
                    () -> "Replicant-0103: Attempting to remove handler " + handler + " that is not " +
                          "in the list of spy handlers." );
    }
    _spyEventHandlers.remove( Objects.requireNonNull( handler ) );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reportSpyEvent( @Nonnull final Object event )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( this::willPropagateSpyEvents,
                 () -> "Replicant-0104: Attempting to report SpyEvent '" + event + "' but " +
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

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean willPropagateSpyEvents()
  {
    return Replicant.areSpiesEnabled() && !getSpyEventHandlers().isEmpty();
  }

  @TestOnly
  @Nonnull
  ArrayList<SpyEventHandler> getSpyEventHandlers()
  {
    return _spyEventHandlers;
  }
}
