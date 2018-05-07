package replicant.spy.tools;

import replicant.Replicant;

/**
 * Utility class for interacting with spy capabilities.
 */
public final class ReplicantSpyUtil
{
  private static final ConsoleSpyEventProcessor PROCESSOR =
    Replicant.areSpiesEnabled() ? new ConsoleSpyEventProcessor() : null;
  private static boolean c_loggingEnabled;

  /**
   * Return true if spy event logging is enabled.
   *
   * @return true if spy event logging is enabled.
   */
  public static boolean isSpyEventLoggingEnabled()
  {
    return c_loggingEnabled;
  }

  /**
   * Enable console logging of all spy events.
   * This is a noop if spies are not enabled or logging has already been enabled.
   */
  public static void enableSpyEventLogging()
  {
    if ( Replicant.areSpiesEnabled() && !isSpyEventLoggingEnabled() )
    {
      Replicant.context().getSpy().addSpyEventHandler( PROCESSOR );
      c_loggingEnabled = true;
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
      Replicant.context().getSpy().removeSpyEventHandler( PROCESSOR );
      c_loggingEnabled = false;
    }
  }

  private ReplicantSpyUtil()
  {
  }
}
