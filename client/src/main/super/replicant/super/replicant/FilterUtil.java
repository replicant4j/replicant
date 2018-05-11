package replicant;

import elemental2.core.Global;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * FilterUtil as replaced by GWT compiler.
 */
public final class FilterUtil
{
  public static boolean filtersEqual( @Nullable final Object filter1, @Nullable final Object filter2 )
  {
    final String filter1String = null == filter1 ? null : filterToString( filter1 );
    final String filter2String = null == filter2 ? null : filterToString( filter2 );
    return Objects.equals( filter1String, filter2String );
  }

  @Nonnull
  public static String filterToString( @Nullable final Object filter )
  {
    return null == filter ? "" : Global.JSON.stringify( filter );
  }

  private FilterUtil()
  {
  }
}
