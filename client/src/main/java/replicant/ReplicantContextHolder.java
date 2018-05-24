package replicant;

import arez.Disposable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.anodoc.TestOnly;

/**
 * A utility class that contains reference to singleton context when zones are disabled.
 * This is extracted to a separate class to eliminate the <clinit> from Replicant and thus
 * make it much easier for GWT to optimize out code based on build time compilation parameters.
 */
final class ReplicantContextHolder
{
  @Nullable
  private static ReplicantContext c_context = Replicant.areZonesEnabled() ? null : new ReplicantContext();

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
  @TestOnly
  static void reset()
  {
    if ( null != c_context )
    {
      Disposable.dispose( c_context );
    }
    c_context = new ReplicantContext();
  }
}
