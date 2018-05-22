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
    final int requestId = 321;
    final String requestKey = "ABC.go";
    final String cacheKey = "G:G2";
    final RequestEntry e = new RequestEntry( requestId, requestKey, cacheKey );

    assertEquals( e.getRequestId(), requestId );
    assertEquals( e.getName(), requestKey );
    assertEquals( e.getCacheKey(), cacheKey );
    assertEquals( e.isExpectingResults(), false );
    assertEquals( e.toString(), "Request(ABC.go)[ID=321,Cache=G:G2]" );
  }

  @Test
  public void construct_MissingName()
  {
    assertThrows( NullPointerException.class, () -> new RequestEntry( 123, null, null ) );
  }

  @Test
  public void construct_NameWhenNamesDisabled()
  {
    ReplicantTestUtil.disableNames();
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> new RequestEntry( 123, "ABC", null ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0041: RequestEntry passed a name 'ABC' but Replicant.areNamesEnabled() is false" );
  }

  @Test
  public void minimalToString()
  {
    final int requestId = 123;
    final String name = "MyMagicMethod";
    final RequestEntry e = new RequestEntry( requestId, name, null );

    assertEquals( e.toString(), "Request(MyMagicMethod)[ID=123]" );
  }

  @Test
  public void toString_WhenNameDisabled()
  {
    ReplicantTestUtil.disableNames();
    final RequestEntry e = new RequestEntry( 123, null, null );

    assertEquals( e.toString(), "replicant.RequestEntry@" + Integer.toHexString( System.identityHashCode( e ) ) );
  }

  @Test
  public void getName_WhenNameDisabled()
  {
    ReplicantTestUtil.disableNames();
    final RequestEntry e = new RequestEntry( 123, null, null );
    final IllegalStateException exception = expectThrows( IllegalStateException.class, e::getName );

    assertEquals( exception.getMessage(),
                  "Replicant-0043: RequestEntry.getName() invoked when Replicant.areNamesEnabled() is false" );
  }

  @Test
  public void isExpectingResults()
  {
    final RequestEntry e = new RequestEntry( ValueUtil.randomInt(), ValueUtil.randomString(), null );
    assertEquals( e.isExpectingResults(), false );
    e.setExpectingResults( true );
    assertEquals( e.isExpectingResults(), true );
  }

  @Test
  public void setNormalCompletionAction()
  {
    final RequestEntry e = new RequestEntry( ValueUtil.randomInt(), ValueUtil.randomString(), null );

    final SafeProcedure action = mock( SafeProcedure.class );
    assertEquals( e.isCompletionDataPresent(), false );
    assertEquals( e.getCompletionAction(), null );

    e.setNormalCompletion( true );
    e.setCompletionAction( action );

    assertEquals( e.isCompletionDataPresent(), true );
    assertEquals( e.isNormalCompletion(), true );
    assertEquals( e.getCompletionAction(), action );
  }

  @Test
  public void setNonNormalCompletionAction()
  {
    final RequestEntry e = new RequestEntry( ValueUtil.randomInt(), ValueUtil.randomString(), null );

    final SafeProcedure action = mock( SafeProcedure.class );
    assertEquals( e.isCompletionDataPresent(), false );
    assertEquals( e.getCompletionAction(), null );

    e.setNormalCompletion( false );
    e.setCompletionAction( action );

    assertEquals( e.isCompletionDataPresent(), true );
    assertEquals( e.isNormalCompletion(), false );
    assertEquals( e.getCompletionAction(), action );
  }

  @Test
  public void markResultsAsArrived()
  {
    final RequestEntry e = new RequestEntry( ValueUtil.randomInt(), ValueUtil.randomString(), null );

    assertEquals( e.haveResultsArrived(), false );
    e.markResultsAsArrived();
    assertEquals( e.haveResultsArrived(), true );
  }

  @Test
  public void isNormalCompletion_beforeCompletionDataSpecified()
  {
    final RequestEntry e = new RequestEntry( ValueUtil.randomInt(), ValueUtil.randomString(), null );

    final IllegalStateException exception = expectThrows( IllegalStateException.class, e::isNormalCompletion );
    assertEquals( exception.getMessage(),
                  "Replicant-0008: isNormalCompletion invoked before completion data specified." );
  }
}
