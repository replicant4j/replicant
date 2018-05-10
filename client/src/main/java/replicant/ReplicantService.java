package replicant;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static org.realityforge.braincheck.Guards.*;

/**
 * A base class that interacts with ReplicantContext.
 */
abstract class ReplicantService
{
  /**
   * Reference to the context to which this service belongs.
   */
  @Nullable
  private final ReplicantContext _context;

  ReplicantService( @Nullable final ReplicantContext context )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      apiInvariant( () -> Replicant.areZonesEnabled() || null == context,
                    () -> "Replicant-0134: ReplicantService passed a context but Replicant.areZonesEnabled() is false" );
    }
    _context = Replicant.areZonesEnabled() ? Objects.requireNonNull( context ) : null;
  }

  @Nonnull
  final ReplicantContext getReplicantContext()
  {
    return Replicant.areZonesEnabled() ? Objects.requireNonNull( _context ) : Replicant.context();
  }
}
