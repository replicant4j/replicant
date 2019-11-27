package org.realityforge.replicant.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import java.util.Objects;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * FilterUtil as replaced by GWT compiler.
 */
public final class FilterUtil
{
  public static boolean filtersEqual( @Nullable final Object fitler1, @Nullable final Object fitler2 )
  {
    final String fitler1String = filterToString( fitler1 );
    final String fitler2String = filterToString( fitler2 );
    return Objects.equals( fitler1String, fitler2String );
  }

  @Nonnull
  public static String filterToString( @Nullable final Object filter )
  {
    return JsonUtils.stringify( (JavaScriptObject) filter );
  }

  private FilterUtil()
  {
  }
}
