package org.realityforge.replicant.server.json.jackson;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * Serializer for jackson library that serializes date sets in a format as expected by RDate.
 */
public final class DateSetSerializer
  extends JsonSerializer<Set<Date>>
{
  @Override
  public void serialize( final Set<Date> dates,
                         final JsonGenerator generator,
                         final SerializerProvider provider )
    throws IOException
  {
    final SimpleDateFormat formatter = new SimpleDateFormat( "yyyy-MM-dd" );
    generator.writeStartArray();
    for ( final Date date : dates )
    {
      generator.writeString( formatter.format( date ) );
    }
    generator.writeEndArray();
  }
}