package replicant;

import org.jspecify.annotations.NonNull;

/**
 * An isolated Replicant context.
 */
public final class Zone
{
  /**
   * The underlying context for zone.
   */
  @NonNull
  private final ReplicantContext _context = new ReplicantContext();

  /**
   * Return the context for the zone.
   *
   * @return the context for the zone.
   */
  @NonNull
  public ReplicantContext getContext()
  {
    return _context;
  }

  /**
   * Create a zone.
   * Should only be done via {@link Replicant} methods.
   */
  Zone()
  {
  }

  public boolean isActive()
  {
    return Replicant.currentZone() == this;
  }

  /**
   * Run the specified function in the zone.
   * Activate the zone on entry, deactivate on exit.
   *
   * @param <T>    The type of the value returned from function.
   * @param action the function to execute.
   * @return the value returned from function.
   */
  public <T> T safeRun( @NonNull final SafeFunction<T> action )
  {
    Replicant.activateZone( this );
    try
    {
      return action.call();
    }
    finally
    {
      Replicant.deactivateZone( this );
    }
  }

  /**
   * Run the specified function in the zone.
   * Activate the zone on entry, deactivate on exit.
   *
   * @param <T>    The type of the value returned from function.
   * @param action the function to execute.
   * @return the value returned from function.
   * @throws Throwable if the function throws an exception.
   */
  public <T> T run( @NonNull final Function<T> action )
    throws Throwable
  {
    Replicant.activateZone( this );
    try
    {
      return action.call();
    }
    finally
    {
      Replicant.deactivateZone( this );
    }
  }

  /**
   * Run the specified procedure in the zone.
   * Activate the zone on entry, deactivate on exit.
   *
   * @param action the procedure to execute.
   */
  public void safeRun( @NonNull final SafeProcedure action )
  {
    Replicant.activateZone( this );
    try
    {
      action.call();
    }
    finally
    {
      Replicant.deactivateZone( this );
    }
  }

  /**
   * Run the specified procedure in the zone.
   * Activate the zone on entry, deactivate on exit.
   *
   * @param action the procedure to execute.
   * @throws Throwable if the procedure throws an exception.
   */
  public void run( @NonNull final Procedure action )
    throws Throwable
  {
    Replicant.activateZone( this );
    try
    {
      action.call();
    }
    finally
    {
      Replicant.deactivateZone( this );
    }
  }
}
