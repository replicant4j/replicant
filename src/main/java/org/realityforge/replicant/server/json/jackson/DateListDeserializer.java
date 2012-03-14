package org.realityforge.replicant.server.json.jackson;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

public final class DateListDeserializer
  extends JsonDeserializer<List<Date>>
{
  @Override
  public List<Date> deserialize( final JsonParser parser, final DeserializationContext context )
    throws IOException
  {
    if ( !parser.isExpectedStartArrayToken() )
    {
      throw context.mappingException( List.class );
    }
    final ArrayList<Date> result = new ArrayList<Date>();

    final DateFormat formatter = DateUtil.newDateFormatter();
    JsonToken token;
    while ( JsonToken.END_ARRAY != ( token = parser.nextToken() ) )
    {
      if ( JsonToken.VALUE_NULL == token )
      {
        throw context.mappingException( List.class );
      }
      result.add( DateUtil.parse( formatter, parser.getText() ) );
    }
    return result;
  }
}