package org.realityforge.replicant.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class RDateTest
{
  @DataProvider( name = "validDates" )
  public Object[][] validDates()
  {
    return new Object[][]{
      { "2001-1-1", new RDate( 2001, 1, 1 ) },
      { "2001-10-1", new RDate( 2001, 10, 1 ) },
      { "2001-2-28", new RDate( 2001, 2, 28 ) },
      { "2021-12-31", new RDate( 2021, 12, 31 ) },
    };
  }

  @Test( dataProvider = "validDates" )
  public void validDate( final String input, final RDate expected )
  {
    assertEquals( RDate.parse( input ), expected );
  }

  @DataProvider( name = "invalidDates" )
  public Object[][] invalidDates()
  {
    return new Object[][]{
      { "x2001-12-1" },
      { "2001x-12-1" },
      { "2001-12x-1" },
      { "2001-x12-1" },
      { "2001-10-1x" },
      { "2001-10-x1" },
      { "2001-10-1-" },
    };
  }

  @Test( dataProvider = "invalidDates", expectedExceptions = IllegalArgumentException.class )
  public void invalidDate( final String input )
  {
    RDate.parse( input );
  }

  @DataProvider( name = "compared" )
  public Object[][] comparableDates()
  {
    return new Object[][]{
      { new RDate( 2001, 1, 1 ), new RDate( 2001, 1, 1 ), 0 },
      { new RDate( 2001, 1, 1 ), new RDate( 2002, 1, 1 ), 1 },
      { new RDate( 2001, 1, 1 ), new RDate( 2000, 1, 1 ), -1 },
      { new RDate( 2001, 1, 1 ), new RDate( 2001, 2, 1 ), 1 },
      { new RDate( 2001, 2, 1 ), new RDate( 2001, 1, 1 ), -1 },
      { new RDate( 2001, 1, 1 ), new RDate( 2001, 1, 2 ), 1 },
      { new RDate( 2001, 1, 2 ), new RDate( 2001, 1, 1 ), -1 },
    };
  }

  @Test( dataProvider = "compared" )
  public void compareDates( final RDate source, final RDate target, final int result )
  {
    assertEquals( result, source.compareTo( target ) );
  }

  @Test
  public void ensureHashCodeEqualWhenEqual()
  {
    final RDate d1 = new RDate( 2001, 1, 1 );
    final RDate d2 = new RDate( 2001, 1, 1 );
    final RDate d3 = new RDate( 2002, 1, 1 );
    assertEquals( d1, d2 );
    assertEquals( d1.hashCode(), d2.hashCode() );
    assertNotSame( d1, d3 );
    assertFalse( d1.hashCode() == d3.hashCode() );
    assertNotSame( d2, d3 );
    assertFalse( d2.hashCode() == d3.hashCode() );
  }

  @DataProvider( name = "transformDates" )
  public Object[][] transformDates()
  {
    return new Object[][]{
      { new RDate( 2001, 1, 1 ) },
      { new RDate( 2001, 2, 1 ) },
      { new RDate( 2001, 1, 3 ) },
    };
  }

  @Test( dataProvider = "transformDates" )
  public void transformDate( final RDate date )
  {
    assertEquals( RDate.parse( date.toString() ), date );
  }

  @Test( dataProvider = "transformDates" )
  public void serializeDates( final RDate date )
    throws Exception
  {
    assertEquals( readRDate( serializeToBytes( date ) ), date );
  }

  private RDate readRDate( final byte[] bytes )
    throws Exception
  {
    final ByteArrayInputStream in = new ByteArrayInputStream( bytes );
    final ObjectInputStream inputStream = new ObjectInputStream( in );
    final RDate result = (RDate) inputStream.readObject();
    inputStream.close();
    return result;
  }

  private byte[] serializeToBytes( final RDate date )
    throws IOException
  {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final ObjectOutputStream outputStream = new ObjectOutputStream( out );
    outputStream.writeObject( date );
    outputStream.close();
    return out.toByteArray();
  }
}
