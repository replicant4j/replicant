package org.realityforge.replicant.client.runtime;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class RateLimitedValueTest
{
  @Test
  public void basicOperation()
  {
    final RateLimitedValue value = new RateLimitedValue( 100L, 10D, 30D );

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

    value.fillBucket();

    assertEquals( value.getTokenCount(), 200D );

    value.setTokenCount( 0D );
    assertEquals( value.getTokenCount(), 0D );

    // Make sure it is truncated to max token count
    value.setTokenCount( 400000D );
    assertEquals( value.getTokenCount(), 200D );

    assertFalse( value.consume( 100L, 1000D ) );
    assertEquals( value.getTokenCount(), 200D );

    assertTrue( value.consume( 100L, 10D ) );
    assertEquals( value.getTokenCount(), 190D );

    final CountDownLatch latch = new CountDownLatch( 1 );
    assertFalse( value.attempt( 100L, 1000D, latch::countDown ) );
    assertEquals( value.getTokenCount(), 190D );
    assertEquals( latch.getCount(), 1 );

    assertTrue( value.attempt( 100L, 10D, latch::countDown ) );
    assertEquals( value.getTokenCount(), 180D );
    assertEquals( latch.getCount(), 0 );
  }

  @Test
  public void regenerateTokens()
  {
    final RateLimitedValue value = new RateLimitedValue( 0L, 2000D, 2000D );
    value.setTokenCount( 0D );

    assertEquals( value.getTokenCount(), 0D );
    assertEquals( getLastRegenTime( value ), 0L );

    value.regenerateTokens( 0L );

    assertEquals( value.getTokenCount(), 0D );
    assertEquals( getLastRegenTime( value ), 0L );

    value.regenerateTokens( 50L );

    assertEquals( value.getTokenCount(), 100D, 0.1 );
    assertEquals( getLastRegenTime( value ), 50L );
  }

  private long getLastRegenTime( final RateLimitedValue value )
  {
    try
    {
      final Field field = RateLimitedValue.class.getDeclaredField( "_lastRegenTime" );
      field.setAccessible( true );
      return (Long) field.get( value );
    }
    catch ( NoSuchFieldException | IllegalAccessException e )
    {
      throw new IllegalStateException( e );
    }
  }
}
