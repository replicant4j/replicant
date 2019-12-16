package replicant;

import arez.Arez;
import arez.Disposable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A utility class that contains reference to singleton context when zones are disabled.
 * This is extracted to a separate class to eliminate the <clinit> from Replicant and thus
 * make it much easier for GWT to optimize out code based on build time compilation parameters.
 */
final class ReplicantContextHolder
{
  @Nullable
  private static ReplicantContext c_context;

  static
  {
    // Instantiating the replicant context as part of the <clinit>
    // can result in scheduler being activated and in a GWT context
    // this may result in the converger executing and trying to reference
    // c_context before it has been initialized. Pausing the scheduler
    // works around this problem
    final Disposable schedulerLock = Arez.context().pauseScheduler();
    try
    {
      c_context = Replicant.areZonesEnabled() ? null : new ReplicantContext();
    }
    finally
    {
      schedulerLock.dispose();
    }
  }

  private ReplicantContextHolder()
  {
  }

  /**
   * Return the ReplicantContext from the provider.
   *
   * @return the ReplicantContext.
   */
  @Nonnull
  static ReplicantContext context()
  {
    assert null != c_context;
    return c_context;
  }

  /**
   * cleanup context.
   * This is dangerous as it may leave dangling references and should only be done in tests.
   */
  static void reset()
  {
    final Disposable schedulerLock = Arez.context().pauseScheduler();
    try
    {
      if ( null != c_context )
      {
        Disposable.dispose( c_context );
      }
      c_context = new ReplicantContext();
    }
    finally
    {
      schedulerLock.dispose();
    }
  }
}
