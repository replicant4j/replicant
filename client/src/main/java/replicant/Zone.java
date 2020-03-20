package replicant;

import javax.annotation.Nonnull;

/**
 * An isolated Replicant context.
 */
public final class Zone
{
  /**
   * The underlying context for zone.
   */
  @Nonnull
  private final ReplicantContext _context = new ReplicantContext();

  /**
   * Return the context for the zone.
   *
   * @return the context for the zone.
   */
  @Nonnull
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
  public <T> T safeRun( @Nonnull final SafeFunction<T> action )
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
  public <T> T run( @Nonnull final Function<T> action )
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
  public void safeRun( @Nonnull final SafeProcedure action )
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
  public void run( @Nonnull final Procedure action )
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
