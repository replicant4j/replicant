package org.realityforge.replicant.server.json.jackson;

import java.io.IOException;
import java.util.Date;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * Serializer for jackson library that serializes dates in a format as expected by RDate.
 */
public final class DateSerializer
  extends JsonSerializer<Date>
{
  @Override
  public void serialize( final Date date,
                         final JsonGenerator generator,
                         final SerializerProvider provider )
    throws IOException
  {
    generator.writeString( DateUtil.newDateFormatter().format( date ) );
  }
}