package replicant.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.StringReader;
import javax.annotation.Nonnull;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReaderFactory;

final class JsonUtil
{
  @Nonnull
  private static final ObjectMapper c_jsonMapper = new ObjectMapper();
  @Nonnull
  private static final JsonReaderFactory c_readerFactory = Json.createReaderFactory( null );

  private JsonUtil()
  {
  }

  @Nonnull
  static JsonObject toJsonObject( @Nonnull final Object object )
  {
    try
    {
      final var stringReader = new StringReader( c_jsonMapper.writeValueAsString( object ) );
      try ( final var reader = c_readerFactory.createReader( stringReader ) )
      {
        return reader.readObject();
      }
    }
    catch ( final IOException ioe )
    {
      throw new IllegalStateException( ioe.getMessage(), ioe );
    }
  }
}
