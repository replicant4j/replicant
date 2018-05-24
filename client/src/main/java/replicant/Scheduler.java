package replicant;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
    private final ScheduledExecutorService _executorService = Executors.newScheduledThreadPool( 1 );
    @GwtIncompatible
    @Nullable
    private ScheduledFuture<?> _future;

    @GwtIncompatible
    @Override
    void schedule( @Nonnull final SafeFunction<Boolean> command )
    {
      final Runnable action = () -> {
        if ( !command.call() && null != _future )
        {
          _future.cancel( false );
          _future = null;
        }
      };
      _future = _executorService.scheduleAtFixedRate( action, 0, 1, TimeUnit.MILLISECONDS );
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
