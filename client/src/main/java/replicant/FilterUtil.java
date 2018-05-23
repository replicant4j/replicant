package replicant;

import elemental2.core.Global;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Simple utility that is extracted so it can be replaced by the GWT compiler.
 */
public final class FilterUtil
{
  private static final FilterSupport c_support = new FilterSupport();

  public static boolean filtersEqual( @Nullable final Object filter1, @Nullable final Object filter2 )
  {
    return c_support.filtersEqual( filter1, filter2 );
  }

  @Nonnull
  public static String filterToString( @Nullable final Object filter )
  {
    return c_support.filterToString( filter );
  }

  /**
   * Abstract support class with methods used by GWT.
   */
  private static abstract class AbstractFilterSupport
  {
    boolean filtersEqual( @Nullable final Object filter1, @Nullable final Object filter2 )
    {
      final String filter1String = null == filter1 ? null : filterToString( filter1 );
      final String filter2String = null == filter2 ? null : filterToString( filter2 );
      return Objects.equals( filter1String, filter2String );
    }

    @Nonnull
    String filterToString( @Nullable final Object filter )
    {
      return null == filter ? "" : Global.JSON.stringify( filter );
    }

  }

  /**
   * Concrete support class with methods used by JVM.
   */
  private static final class FilterSupport
    extends AbstractFilterSupport
  {
    @GwtIncompatible
    @Override
    boolean filtersEqual( @Nullable final Object filter1, @Nullable final Object filter2 )
    {
      return Objects.equals( filter1, filter2 );
    }

    @GwtIncompatible
    @Nonnull
    @Override
    String filterToString( @Nullable final Object filter )
    {
      return String.valueOf( filter );
    }
  }

  private FilterUtil()
  {
  }
}
