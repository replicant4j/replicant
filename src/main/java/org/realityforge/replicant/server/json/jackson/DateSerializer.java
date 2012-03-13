package org.realityforge.replicant.server.json.jackson;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * Serializer for jackson library that serializes dates in a format as expected by RDate.
 */
public class DateSerializer
  extends JsonSerializer<Date>
{
  @Override
  public void serialize( final Date value,
                         final JsonGenerator generator,
                         final SerializerProvider provider )
    throws IOException
  {
    final SimpleDateFormat formatter = new SimpleDateFormat( "yyyy-MM-dd" );
    final String formattedDate = formatter.format( value );
    generator.writeString( formattedDate );
  }
}