package replicant;

import elemental2.dom.DomGlobal;
import java.util.Timer;
import java.util.TimerTask;
import javax.annotation.Nonnull;

/**
 * A thin abstraction for scheduling callbacks that works in JVM and GWT environments.
 */
final class Scheduler
{
  private static final SchedulerSupport c_support = new SchedulerSupport();

  static void schedule( @Nonnull final SafeFunction<Boolean> command )
  {
    c_support.schedule( command );
  }

  static void scheduleOnceOff( @Nonnull final SafeProcedure command, final int delay )
  {
    c_support.scheduleOnceOff( command, delay );
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

    @GwtIncompatible
    @Override
    void scheduleOnceOff( @Nonnull final SafeProcedure command, final int delay )
    {
      final TimerTask timerTask = new TimerTask()
      {
        @Override
        public void run()
        {
          command.call();
        }
      };
      final Timer timer = new Timer();
      timer.schedule( timerTask, delay );
    }
  }

  private static abstract class AbstractSchedulerSupport
  {
    void scheduleOnceOff( @Nonnull final SafeProcedure command, final int delay )
    {
      DomGlobal.setTimeout( v -> command.call(), delay );
    }

    void schedule( @Nonnull final SafeFunction<Boolean> command )
    {
      final long end = System.currentTimeMillis() + 14;
      while ( System.currentTimeMillis() < end )
      {
        if ( !command.call() )
        {
          return;
        }
      }
      DomGlobal.setTimeout( v -> schedule( command ) );
    }
  }

  private Scheduler()
  {
  }
}
