package replicant;

import static org.realityforge.braincheck.Guards.*;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A base class for state owned by one {@link ReplicantContext}.
 */
abstract class ReplicantService {
    /**
     * Reference to the Replicant Context to which this service belongs when Zones are enabled.
     */
    @Nullable
    private final ReplicantContext _context;

    ReplicantService(@Nullable final ReplicantContext context) {
        if (Replicant.shouldCheckInvariants()) {
            invariant(
                    () -> Replicant.areZonesEnabled() || null == context,
                    () -> "Replicant-0037: ReplicantService passed a context but Replicant.areZonesEnabled() is false");
        }
        _context = Replicant.areZonesEnabled() ? Objects.requireNonNull(context) : null;
    }

    @NonNull
    protected final ReplicantContext getReplicantContext() {
        return Replicant.areZonesEnabled() ? Objects.requireNonNull(_context) : Replicant.context();
    }
}
