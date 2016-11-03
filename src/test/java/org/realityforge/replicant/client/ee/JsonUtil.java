package org.realityforge.replicant.client.ee;

import java.io.StringReader;
import javax.annotation.Nonnull;
import javax.json.Json;
import javax.json.JsonObject;

public final class JsonUtil
{
  private JsonUtil()
  {
  }

  @Nonnull
  static JsonObject toJsonObject( @Nonnull final String content )
  {
    return Json.createReader( new StringReader( content ) ).readObject();
  }
}
