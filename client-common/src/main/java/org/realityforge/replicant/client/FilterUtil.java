package org.realityforge.replicant.client;

import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Simple utility that is extracted so it can be replcaed by GWT compiler.
 */
public final class FilterUtil
{
  public static boolean filtersEqual( @Nullable final Object fitler1, @Nullable final Object fitler2 )
  {
    return Objects.equals( fitler1, fitler2 );
  }

  private FilterUtil()
  {
  }
}
