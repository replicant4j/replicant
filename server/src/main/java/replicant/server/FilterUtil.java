package replicant.server;

import java.util.Objects;
import javax.annotation.Nullable;
import javax.json.JsonObject;

public final class FilterUtil
{
  private FilterUtil()
  {
  }

  public static boolean filtersEqual( @Nullable final JsonObject filter1, @Nullable final JsonObject filter2 )
  {
    return Objects.equals( filter1, filter2 );
  }
}
