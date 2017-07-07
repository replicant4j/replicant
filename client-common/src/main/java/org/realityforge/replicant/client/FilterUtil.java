package org.realityforge.replicant.client;

import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Simple utility that is extracted so it can be replaced by GWT compiler.
 */
public final class FilterUtil
{
  public static boolean filtersEqual( @Nullable final Object filter1, @Nullable final Object filter2 )
  {
    return Objects.equals( filter1, filter2 );
  }

  private FilterUtil()
  {
  }
}
