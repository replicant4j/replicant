package replicant;

import arez.Arez;
import arez.Disposable;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A utility class that contains the singleton Replicant Context when Zones are disabled.
 * This is extracted to a separate class to eliminate the <clinit> from Replicant and thus
 * make it much easier for GWT to optimize out code based on build time compilation parameters.
 */
final class ReplicantContextHolder {
    @Nullable
    private static ReplicantContext c_context;

    static {
        // Instantiating the replicant context as part of the <clinit>
        // can result in scheduler being activated and in a GWT context
        // this may result in the SubscriptionReconciler executing and trying to reference
        // c_context before it has been initialized. Pausing the scheduler
        // works around this problem
        final Disposable schedulerLock = Arez.context().pauseScheduler();
        try {
            c_context = Replicant.areZonesEnabled() ? null : new ReplicantContext();
        } finally {
            schedulerLock.dispose();
        }
    }

    private ReplicantContextHolder() {}

    /**
     * Return the singleton Replicant Context.
     *
     * @return the singleton Replicant Context.
     */
    @NonNull
    static ReplicantContext context() {
        return Objects.requireNonNull(c_context);
    }

    /**
     * Reset the singleton Replicant Context.
     * This is dangerous as it may leave dangling references and should only be done in tests.
     */
    static void reset() {
        final Disposable schedulerLock = Arez.context().pauseScheduler();
        try {
            if (null != c_context) {
                Disposable.dispose(c_context);
            }
            c_context = new ReplicantContext();
        } finally {
            schedulerLock.dispose();
        }
    }
}
