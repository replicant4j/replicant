package replicant.spy.tools;

import java.util.HashMap;
import java.util.Map;
import replicant.Replicant;
import replicant.ReplicantContext;

/**
 * Utility class for interacting with spy capabilities.
 */
public final class ReplicantSpyUtil
{
  private static final Map<ReplicantContext, ConsoleSpyEventProcessor> c_processors = new HashMap<>();

  /**
   * Return true if spy event logging is enabled.
   *
   * @return true if spy event logging is enabled.
   */
  public static boolean isSpyEventLoggingEnabled()
  {
    return c_processors.containsKey( Replicant.context() );
  }

  /**
   * Enable console logging of all spy events.
   * This is a noop if spies are not enabled or logging has already been enabled.
   */
  public static void enableSpyEventLogging()
  {
    if ( Replicant.areSpiesEnabled() && !isSpyEventLoggingEnabled() )
    {
      final ConsoleSpyEventProcessor handler = new ConsoleSpyEventProcessor();
      final ReplicantContext context = Replicant.context();
      context.getSpy().addSpyEventHandler( handler );
      c_processors.put( context, handler );
    }
  }

  /**
   * Disable console logging of all spy events.
   * This is a noop if spies are not enabled or logging is not enabled.
   */
  public static void disableSpyEventLogging()
  {
    if ( Replicant.areSpiesEnabled() && isSpyEventLoggingEnabled() )
    {
      final ReplicantContext context = Replicant.context();
      final ConsoleSpyEventProcessor handler = c_processors.remove( context );
      assert null != handler;
      context.getSpy().removeSpyEventHandler( handler );
    }
  }

  private ReplicantSpyUtil()
  {
  }
}
