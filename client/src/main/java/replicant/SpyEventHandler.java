package replicant;

import org.jspecify.annotations.NonNull;

/**
 * Interface for receiving spy events.
 */
@FunctionalInterface
public interface SpyEventHandler {
    /**
     * Report an event in the Replicant system.
     *
     * @param event the event that occurred.
     */
    void onSpyEvent(@NonNull Object event);
}
