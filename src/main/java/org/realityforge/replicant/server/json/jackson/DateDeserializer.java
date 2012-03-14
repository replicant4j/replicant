package org.realityforge.replicant.server.json.jackson;

import java.io.IOException;
import java.util.Date;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

/**
 * Serializer for jackson library that serializes dates in a format as expected by RDate.
 */
public final class DateDeserializer
  extends JsonDeserializer<Date>
{
  @Override
  public Date deserialize( final JsonParser parser, final DeserializationContext context )
    throws IOException
  {
    return DateUtil.parse( DateUtil.newDateFormatter(), parser.getText() );
  }
}