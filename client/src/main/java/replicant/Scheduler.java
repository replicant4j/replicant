package replicant;

import org.jspecify.annotations.NonNull;
import zemeckis.Zemeckis;

/**
 * A thin abstraction for scheduling callbacks that works in JVM and GWT environments.
 */
final class Scheduler {
    private static final SchedulerSupport c_support = new SchedulerSupport();

    static void schedule(@NonNull final SafeFunction<Boolean> task) {
        c_support.schedule(task);
    }

    /**
     * JVM Compatible variant which will have fields and methods stripped out during GWT compile and thus fallback to GWT variant.
     */
    private static final class SchedulerSupport extends AbstractSchedulerSupport {
        @GwtIncompatible
        @Override
        void schedule(@NonNull final SafeFunction<Boolean> task) {
            //noinspection StatementWithEmptyBody
            while (task.call()) {}
        }
    }

    private abstract static class AbstractSchedulerSupport {
        void schedule(@NonNull final SafeFunction<Boolean> task) {
            final long end = System.currentTimeMillis() + 14;
            while (System.currentTimeMillis() < end) {
                if (!task.call()) {
                    return;
                }
            }
            Zemeckis.delayedTask(
                    Zemeckis.areNamesEnabled() ? "ReplicantIncrementalTask" : null, () -> schedule(task), 0);
        }
    }

    private Scheduler() {}
}
