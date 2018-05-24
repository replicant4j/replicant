package replicant;

import javax.annotation.Nonnull;

/**
 * A thin abstraction for scheduling callbacks that works in JVM and GWT environments.
 */
final class Scheduler
{
  private static final SchedulerSupport c_support = new SchedulerSupport();

  public static void schedule( @Nonnull final SafeFunction<Boolean> command )
  {
    c_support.schedule( command );
  }

  /**
   * JVM Compatible variant which will have fields and methods stripped out during GWT compile and thus fallback to GWT variant.
   */
  private static final class SchedulerSupport
    extends AbstractSchedulerSupport
  {
    @GwtIncompatible
    @Override
    void schedule( @Nonnull final SafeFunction<Boolean> command )
    {
      //noinspection StatementWithEmptyBody
      while ( command.call() )
      {
      }
    }
  }

  private static abstract class AbstractSchedulerSupport
  {
    void schedule( @Nonnull final SafeFunction<Boolean> command )
    {
      com.google.gwt.core.client.Scheduler.get().scheduleIncremental( command::call );
    }
  }
}
