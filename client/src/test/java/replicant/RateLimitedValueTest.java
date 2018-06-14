package replicant;

import java.util.concurrent.CountDownLatch;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class RateLimitedValueTest
  extends AbstractReplicantTest
{
  static class TestRateLimitedValue
    extends RateLimitedValue
  {
    static long _currentTimeMillis;

    TestRateLimitedValue( final double tokensPerSecond )
    {
      super( tokensPerSecond );
    }

    TestRateLimitedValue( final double tokensPerSecond, final double maxTokenAmount )
    {
      super( tokensPerSecond, maxTokenAmount );
    }

    static void setCurrentTimeMillis( final long currentTimeMillis )
    {
      _currentTimeMillis = currentTimeMillis;
    }

    @Override
    long currentTimeMillis()
    {
      return _currentTimeMillis;
    }
  }

  @Test
  public void basicOperation()
    throws Exception
  {
    TestRateLimitedValue.setCurrentTimeMillis( 100L );
    final TestRateLimitedValue value = new TestRateLimitedValue( 10D, 30D );

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
    TestRateLimitedValue.setCurrentTimeMillis( 0L );
    final TestRateLimitedValue value = new TestRateLimitedValue( 2000D );
    value.setTokenCount( 0D );

    assertEquals( value.getTokenCount(), 0D );
    assertEquals( getLastRegenTime( value ), 0L );

    value.regenerateTokens();

    assertEquals( value.getTokenCount(), 0D );
    assertEquals( getLastRegenTime( value ), 0L );

    TestRateLimitedValue.setCurrentTimeMillis( 50L );

    value.regenerateTokens();

    assertEquals( value.getTokenCount(), 100D, 0.1 );
    assertEquals( getLastRegenTime( value ), 50L );
  }

  private Object getLastRegenTime( final RateLimitedValue value )
    throws Exception
  {
    return getFieldValue( value, "_lastRegenTime" );
  }
}
