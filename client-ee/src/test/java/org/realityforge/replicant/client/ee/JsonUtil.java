package org.realityforge.replicant.client.ee;

import java.io.StringReader;
import javax.annotation.Nonnull;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;

final class JsonUtil
{
  private static final JsonReaderFactory c_readerFactory = Json.createReaderFactory( null );

  private JsonUtil()
  {
  }

  @Nonnull
  static JsonObject toJsonObject( @Nonnull final String content )
  {
    final StringReader stringReader = new StringReader( content );
    try ( final JsonReader reader = c_readerFactory.createReader( stringReader ) )
    {
      return reader.readObject();
    }
  }
}
