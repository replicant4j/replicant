package replicant;

import javax.annotation.Nonnull;

/**
 * Interface for receiving application events.
 */
@FunctionalInterface
public interface ApplicationEventHandler
{
  /**
   * Report an application event in the Replicant system.
   *
   * @param event the event that occurred.
   */
  void onApplicationEvent( @Nonnull Object event );
}
