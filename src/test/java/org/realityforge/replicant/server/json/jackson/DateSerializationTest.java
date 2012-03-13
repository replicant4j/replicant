package org.realityforge.replicant.server.json.jackson;

import java.util.Calendar;
import java.util.Date;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.SerializerProvider;
import org.realityforge.replicant.client.RDate;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public final class DateSerializationTest
{
  @DataProvider( name = "validDates" )
  public Object[][] validDates()
  {
    return new Object[][]{
      { "2001-01-01", toDate( 2001, 1, 1 ), new RDate( 2001, 1, 1 ) },
      { "2001-10-01", toDate( 2001, 10, 1 ), new RDate( 2001, 10, 1 ) },
      { "2001-02-28", toDate( 2001, 2, 28 ), new RDate( 2001, 2, 28 ) },
      { "2021-12-31", toDate( 2021, 12, 31 ), new RDate( 2021, 12, 31 ) },
    };
  }

  @Test( dataProvider = "validDates" )
  public void serialize( final String expectedFormat, final Date date, final RDate rDate )
    throws Exception
  {
    final JsonGenerator generator = mock( JsonGenerator.class );

    new DateSerializer().serialize( date, generator, mock( SerializerProvider.class ) );
    verify( generator ).writeString( expectedFormat );

    final JsonParser parser = mock( JsonParser.class );
    when( parser.getText() ).thenReturn( expectedFormat );
    final Date result = new DateDeserializer().deserialize( parser, mock( DeserializationContext.class ) );

    assertEquals( RDate.parse( expectedFormat ), rDate );
    assertEquals( RDate.fromDate( result ), rDate );
  }

  public static Date toDate( final int year, final int month, final int day )
  {
    final Calendar calendar = Calendar.getInstance();
    calendar.set( Calendar.YEAR, year );
    calendar.set( Calendar.MONTH, month - 1 );
    calendar.set( Calendar.DAY_OF_MONTH, day );
    calendar.set( Calendar.HOUR, 0 );
    calendar.set( Calendar.MINUTE, 0 );
    calendar.set( Calendar.SECOND, 0 );
    calendar.set( Calendar.MILLISECOND, 0 );
    return calendar.getTime();
  }
}
