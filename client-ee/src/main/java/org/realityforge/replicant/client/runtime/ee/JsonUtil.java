package org.realityforge.replicant.client.runtime.ee;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class JsonUtil
{
  private static final ObjectMapper c_jsonMapper = new ObjectMapper();

  private JsonUtil()
  {
  }

  @Nonnull
  static String toJsonString( @Nullable final Object object )
  {
    try
    {
      return c_jsonMapper.writeValueAsString( object );
    }
    catch ( final IOException ioe )
    {
      throw new IllegalStateException( ioe.getMessage(), ioe );
    }
  }
}
