package org.realityforge.replicant.server.json.jackson;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * Serializer for jackson library that serializes date list in a format as expected by RDate.
 */
public final class DateListSerializer
  extends JsonSerializer<List<Date>>
{
  @Override
  public void serialize( final List<Date> dates,
                         final JsonGenerator generator,
                         final SerializerProvider provider )
    throws IOException
  {
    final DateFormat formatter = DateUtil.newDateFormatter();
    generator.writeStartArray();
    for ( final Date date : dates )
    {
      generator.writeString( formatter.format( date ) );
    }
    generator.writeEndArray();
  }
}
