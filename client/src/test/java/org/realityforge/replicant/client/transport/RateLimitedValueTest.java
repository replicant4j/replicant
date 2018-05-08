package org.realityforge.replicant.client.transport;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import javax.annotation.Nonnull;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.testng.IObjectFactory;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

@PrepareForTest( { System.class, RateLimitedValue.class } )
public class RateLimitedValueTest
{
  @ObjectFactory
  public IObjectFactory getObjectFactory()
  {
    return new PowerMockObjectFactory();
  }

  @Test
  public void basicOperation()
    throws Exception
  {
    PowerMockito.mockStatic( System.class );
    PowerMockito.when( System.currentTimeMillis() ).thenReturn( 100L );

    final RateLimitedValue value = new RateLimitedValue( 10D, 30D );

    assertEquals( getLastRegenTime( value ), 100L );

    assertEquals( value.getMaxTokenCount(), 30D );
    assertEquals( value.getTokensPerSecond(), 10D );

    // Starts out at max token count
    assertEquals( value.getTokenCount(), 30D );

    value.setMaxTokenCount( 2D );
    assertEquals( value.getMaxTokenCount(), 2D );
    assertEquals( value.getTokensPerSecond(), 10D );
    assertEquals( value.getTokenCount(), 2D );

    value.setMaxTokenCount( 200D );
    value.setTokensPerSecond( 50D );

    assertEquals( value.getMaxTokenCount(), 200D );
    assertEquals( value.getTokensPerSecond(), 50D );
    assertEquals( value.getTokenCount(), 2D );
    assertEquals( value.isBucketFull(), false );

    value.fillBucket();

    assertEquals( value.getTokenCount(), 200D );
    assertEquals( value.isBucketFull(), true );

    value.setTokenCount( 0D );
    assertEquals( value.getTokenCount(), 0D );
    assertEquals( value.isBucketFull(), false );

    // Make sure it is truncated to max token count
    value.setTokenCount( 400000D );
    assertEquals( value.getTokenCount(), 200D );
    assertEquals( value.isBucketFull(), true );

    assertFalse( value.consume( 1000D ) );
    assertEquals( value.getTokenCount(), 200D );

    assertTrue( value.consume( 10D ) );
    assertEquals( value.getTokenCount(), 190D );

    final CountDownLatch latch = new CountDownLatch( 1 );
    assertFalse( value.attempt( 1000D, latch::countDown ) );
    assertEquals( value.getTokenCount(), 190D );
    assertEquals( latch.getCount(), 1 );

    //noinspection Convert2Lambda
    assertTrue( value.attempt( 10D, new Runnable()
    {
      @Override
      public void run()
      {
        //Can not be converted to lambda as it confuses Powermock
        latch.countDown();
      }
    } ) );
    assertEquals( value.getTokenCount(), 180D );
    assertEquals( latch.getCount(), 0 );
  }

  @Test
  public void regenerateTokens()
    throws Exception
  {
    PowerMockito.mockStatic( System.class );
    PowerMockito.when( System.currentTimeMillis() ).thenReturn( 0L );

    final RateLimitedValue value = new RateLimitedValue( 2000D );
    value.setTokenCount( 0D );

    assertEquals( value.getTokenCount(), 0D );
    assertEquals( getLastRegenTime( value ), 0L );

    value.regenerateTokens();

    assertEquals( value.getTokenCount(), 0D );
    assertEquals( getLastRegenTime( value ), 0L );

    PowerMockito.when( System.currentTimeMillis() ).thenReturn( 50L );

    value.regenerateTokens();

    assertEquals( value.getTokenCount(), 100D, 0.1 );
    assertEquals( getLastRegenTime( value ), 50L );
  }

  private Object getLastRegenTime( final RateLimitedValue value )
    throws Exception
  {
    return getField( value, "_lastRegenTime" );
  }

  @Nonnull
  private Field toField( final Class<?> type, final String fieldName )
    throws NoSuchFieldException
  {
    Class<?> clazz = type;
    while ( null != clazz && Object.class != clazz )
    {
      try
      {
        final Field field = clazz.getDeclaredField( fieldName );
        field.setAccessible( true );
        return field;
      }
      catch ( final Throwable t )
      {
        clazz = clazz.getSuperclass();
      }
    }
    fail();
    return null;
  }

  @SuppressWarnings( "SameParameterValue" )
  private Object getField( @Nonnull final Object object, @Nonnull final String fieldName )
    throws Exception
  {
    return toField( object.getClass(), fieldName ).get( object );
  }
}
