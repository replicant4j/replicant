package org.realityforge.replicant.server.json.jackson;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

public final class DateSetDeserializer
  extends JsonDeserializer<Set<Date>>
{
  @Override
  public Set<Date> deserialize( final JsonParser parser, final DeserializationContext context )
    throws IOException
  {
    if ( !parser.isExpectedStartArrayToken() )
    {
      throw context.mappingException( Set.class );
    }
    final HashSet<Date> result = new HashSet<Date>();

    final DateFormat formatter = DateUtil.newDateFormatter();
    JsonToken token;
    while ( JsonToken.END_ARRAY != ( token = parser.nextToken() ) )
    {
      if ( JsonToken.VALUE_NULL == token )
      {
        throw context.mappingException( Set.class );
      }
      result.add( DateUtil.parse( formatter, parser.getText() ) );
    }
    return result;
  }
}
