package org.realityforge.replicant.server.ee;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.StringReader;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;

public final class JsonUtil
{
  private static final ObjectMapper c_jsonMapper = new ObjectMapper();
  private static final JsonReaderFactory c_readerFactory = Json.createReaderFactory( null );

  private JsonUtil()
  {
  }

  @Nonnull
  public static JsonObject toJsonObject( @Nonnull final String content )
  {
    final StringReader stringReader = new StringReader( content );
    try ( final JsonReader reader = c_readerFactory.createReader( stringReader ) )
    {
      return reader.readObject();
    }
  }

  @Nonnull
  public static JsonObject toJsonObject( @Nonnull final Object object )
  {
    return toJsonObject( toJsonString( object ) );
  }

  @Nonnull
  public static String toJsonString( @Nullable final Object object )
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
