package replicant.server.transport;

import java.util.Objects;
import javax.annotation.Nullable;
import javax.json.JsonObject;

final class FilterUtil
{
  private FilterUtil()
  {
  }

  static boolean filtersEqual( @Nullable final JsonObject filter1, @Nullable final JsonObject filter2 )
  {
    return Objects.equals( filter1, filter2 );
  }
}
