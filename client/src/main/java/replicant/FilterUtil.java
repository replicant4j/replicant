package replicant;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Simple utility that is extracted so it can be replaced by the GWT compiler.
 */
public final class FilterUtil
{
  public static boolean filtersEqual( @Nullable final Object filter1, @Nullable final Object filter2 )
  {
    return Objects.equals( filter1, filter2 );
  }

  @Nonnull
  public static String filterToString( @Nullable final Object filter )
  {
    return String.valueOf( filter );
  }

  private FilterUtil()
  {
  }
}
