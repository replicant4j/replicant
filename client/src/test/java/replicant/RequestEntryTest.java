package replicant;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class RequestEntryTest
  extends AbstractReplicantTest
{
  @Test
  public void construct()
  {
    final String requestId = "321";
    final String requestKey = "ABC.go";
    final String cacheKey = "G:G2";
    final RequestEntry e = new RequestEntry( requestId, requestKey, cacheKey );

    assertEquals( e.getRequestId(), requestId );
    assertEquals( e.getRequestKey(), requestKey );
    assertEquals( e.getCacheKey(), cacheKey );
    assertEquals( e.isExpectingResults(), false );
    assertEquals( e.toString(), "Request(ABC.go)[ID=321,Cache=G:G2]" );
  }

  @Test
  public void minimalToString()
  {
    final String requestId = "123";
    final RequestEntry e = new RequestEntry( requestId, null, null );

    assertEquals( e.toString(), "Request(?)[ID=123]" );
  }

  @Test
  public void toString_WhenNameDisabled()
  {
    ReplicantTestUtil.disableNames();
    final RequestEntry e = new RequestEntry( "123", null, null );

    assertEquals( e.toString(), "replicant.RequestEntry@" + Integer.toHexString( System.identityHashCode( e ) ) );
  }

  @Test
  public void isExpectingResults()
  {
    final RequestEntry e = new RequestEntry( ValueUtil.randomString(), null, null );
    assertEquals( e.isExpectingResults(), false );
    e.setExpectingResults( true );
    assertEquals( e.isExpectingResults(), true );
  }

  @Test
  public void setNormalCompletionAction()
  {
    final RequestEntry e = new RequestEntry( ValueUtil.randomString(), ValueUtil.randomString(), null );

    final Runnable action = mock( Runnable.class );
    assertEquals( e.isCompletionDataPresent(), false );
    assertEquals( e.getCompletionAction(), null );

    e.setNormalCompletionAction( action );

    assertEquals( e.isCompletionDataPresent(), true );
    assertEquals( e.isNormalCompletion(), true );
    assertEquals( e.getCompletionAction(), action );
  }

  @Test
  public void setNonNormalCompletionAction()
  {
    final RequestEntry e = new RequestEntry( ValueUtil.randomString(), ValueUtil.randomString(), null );

    final Runnable action = mock( Runnable.class );
    assertEquals( e.isCompletionDataPresent(), false );
    assertEquals( e.getCompletionAction(), null );

    e.setNonNormalCompletionAction( action );

    assertEquals( e.isCompletionDataPresent(), true );
    assertEquals( e.isNormalCompletion(), false );
    assertEquals( e.getCompletionAction(), action );
  }

  @Test
  public void markResultsAsArrived()
  {
    final RequestEntry e = new RequestEntry( ValueUtil.randomString(), null, null );

    assertEquals( e.haveResultsArrived(), false );
    e.markResultsAsArrived();
    assertEquals( e.haveResultsArrived(), true );
  }

  @Test
  public void isNormalCompletion_beforeCompletionDataSpecified()
  {
    final RequestEntry e = new RequestEntry( ValueUtil.randomString(), null, null );

    final IllegalStateException exception = expectThrows( IllegalStateException.class, e::isNormalCompletion );
    assertEquals( exception.getMessage(),
                  "Replicant-0008: isNormalCompletion invoked before completion data specified." );
  }
}
