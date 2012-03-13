package org.realityforge.replicant.server.json.jackson;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

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
    final SimpleDateFormat formatter = new SimpleDateFormat( "yyyy-MM-dd" );
    try
    {
      return formatter.parse( parser.getText() );
    }
    catch ( final ParseException pe )
    {
      throw new IOException( pe.getMessage(), pe );
    }
  }
}