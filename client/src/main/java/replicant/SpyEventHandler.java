package replicant;

import javax.annotation.Nonnull;

/**
 * Interface for receiving spy events.
 */
@FunctionalInterface
public interface SpyEventHandler
{
  /**
   * Report an event in the Replicant system.
   *
   * @param event the event that occurred.
   */
  void onSpyEvent( @Nonnull Object event );
}
