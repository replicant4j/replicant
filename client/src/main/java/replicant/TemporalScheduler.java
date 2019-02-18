package replicant;

import elemental2.dom.DomGlobal;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import static org.realityforge.braincheck.Guards.*;

/**
 * The scheduler is responsible for scheduling and executing tasks asynchronously.
 */
final class TemporalScheduler
{
  @Nonnull
  private static AbstractScheduler c_scheduler = new SchedulerImpl();

  private TemporalScheduler()
  {
  }

  /**
   * Schedules the execution of the given task after a specified delay.
   *
   * @param task  the task to execute.
   * @param delay the delay before the task should execute. Must not be a negative value.
   */
  @SuppressWarnings( "SameParameterValue" )
  static void schedule( @Nonnull final Runnable task, final int delay )
  {
    c_scheduler.schedule( task, delay );
  }

  private static final class SchedulerImpl
    extends AbstractScheduler
  {
    @GwtIncompatible
    private final ScheduledExecutorService _executorService = new ScheduledThreadPoolExecutor( 1 );

    @GwtIncompatible
    @Override
    final void doSchedule( @Nonnull final Runnable task, final int delay )
    {
      _executorService.schedule( task, delay, TimeUnit.MILLISECONDS );
    }
  }

  private static abstract class AbstractScheduler
  {
    final void schedule( @Nonnull final Runnable task, final int delay )
    {
      if ( Replicant.shouldCheckApiInvariants() )
      {
        apiInvariant( () -> delay >= 0,
                      () -> "Replicant-0018: TemporalScheduler.schedule(...) passed a negative delay. " +
                            "Actual value passed is " + delay );
      }
      doSchedule( task, delay );
    }

    void doSchedule( @Nonnull final Runnable task, final int delay )
    {
      DomGlobal.setTimeout( v -> task.run(), delay );
    }
  }
}
