package org.realityforge.replicant.server.json.jackson;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.realityforge.replicant.client.RDate;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;

public final class DateListSerializerTest
{
  @Test
  public void serialize()
    throws Exception
  {
    final RDate rDate1 = new RDate( 2001, 1, 1 );
    final RDate rDate2 = new RDate( 2001, 2, 28 );
    final RDate[] dates = new RDate[]{ rDate1, rDate2 };
    final ArrayList<Date> datesCollection = new ArrayList<Date>();
    for ( final RDate date : dates )
    {
      final Calendar instance = Calendar.getInstance();
      instance.set( Calendar.YEAR, date.getYear() );
      instance.set( Calendar.MONTH, date.getMonth() - 1 );
      instance.set( Calendar.DAY_OF_MONTH, date.getDay() );
      instance.set( Calendar.HOUR, 0 );
      instance.set( Calendar.MINUTE, 0 );
      instance.set( Calendar.SECOND, 0 );
      instance.set( Calendar.MILLISECOND, 0 );
      datesCollection.add( instance.getTime() );
    }

    final DateListSerializer serializer = new DateListSerializer();
    final StringWriter writer = new StringWriter();
    final JsonGenerator generator = mock( JsonGenerator.class );
    final SerializerProvider provider = mock( SerializerProvider.class );

    serializer.serialize( datesCollection, generator, provider );

    verify( generator ).writeStartArray();
    verify( generator ).writeString( "2001-01-01" );
    verify( generator ).writeString( "2001-02-28" );
    verify( generator ).writeEndArray();
  }
}
