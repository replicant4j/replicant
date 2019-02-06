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
    final RequestEntry e = new RequestEntry( requestId, requestKey );

    assertEquals( e.getRequestId(), requestId );
    assertEquals( e.getName(), requestKey );
    assertFalse( e.isExpectingResults() );
    assertEquals( e.toString(), "Request(ABC.go)[Id=321]" );
  }

  @Test
  public void construct_MissingName()
  {
    assertThrows( NullPointerException.class, () -> new RequestEntry( 123, null ) );
  }

  @Test
  public void construct_NameWhenNamesDisabled()
  {
    ReplicantTestUtil.disableNames();
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> new RequestEntry( 123, "ABC" ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0041: RequestEntry passed a name 'ABC' but Replicant.areNamesEnabled() is false" );
  }

  @Test
  public void minimalToString()
  {
    final int requestId = 123;
    final String name = "MyMagicMethod";
    final RequestEntry e = new RequestEntry( requestId, name );

    assertEquals( e.toString(), "Request(MyMagicMethod)[Id=123]" );
  }

  @Test
  public void toString_WhenNameDisabled()
  {
    ReplicantTestUtil.disableNames();
    final RequestEntry e = new RequestEntry( 123, null );

    assertEquals( e.toString(), "replicant.RequestEntry@" + Integer.toHexString( System.identityHashCode( e ) ) );
  }

  @Test
  public void getName_WhenNameDisabled()
  {
    ReplicantTestUtil.disableNames();
    final RequestEntry e = new RequestEntry( 123, null );
    final IllegalStateException exception = expectThrows( IllegalStateException.class, e::getName );

    assertEquals( exception.getMessage(),
                  "Replicant-0043: RequestEntry.getName() invoked when Replicant.areNamesEnabled() is false" );
  }

  @Test
  public void isExpectingResults()
  {
    final RequestEntry e = new RequestEntry( ValueUtil.randomInt(), ValueUtil.randomString() );
    assertFalse( e.isExpectingResults() );
    e.setExpectingResults( true );
    assertTrue( e.isExpectingResults() );
  }

  @Test
  public void setNormalCompletionAction()
  {
    final RequestEntry e = new RequestEntry( ValueUtil.randomInt(), ValueUtil.randomString() );

    final SafeProcedure action = mock( SafeProcedure.class );
    assertFalse( e.hasCompleted() );
    assertNull( e.getCompletionAction() );

    e.setNormalCompletion( true );
    e.setCompletionAction( action );

    assertTrue( e.hasCompleted() );
    assertTrue( e.isNormalCompletion() );
    assertEquals( e.getCompletionAction(), action );
  }

  @Test
  public void setNonNormalCompletionAction()
  {
    final RequestEntry e = new RequestEntry( ValueUtil.randomInt(), ValueUtil.randomString() );

    final SafeProcedure action = mock( SafeProcedure.class );
    assertFalse( e.hasCompleted() );
    assertNull( e.getCompletionAction() );

    e.setNormalCompletion( false );
    e.setCompletionAction( action );

    assertTrue( e.hasCompleted() );
    assertFalse( e.isNormalCompletion() );
    assertEquals( e.getCompletionAction(), action );
  }

  @Test
  public void markResultsAsArrived()
  {
    final RequestEntry e = new RequestEntry( ValueUtil.randomInt(), ValueUtil.randomString() );

    assertFalse( e.haveResultsArrived() );
    e.markResultsAsArrived();
    assertTrue( e.haveResultsArrived() );
  }

  @Test
  public void isNormalCompletion_beforeCompletionDataSpecified()
  {
    final RequestEntry e = new RequestEntry( ValueUtil.randomInt(), ValueUtil.randomString() );

    final IllegalStateException exception = expectThrows( IllegalStateException.class, e::isNormalCompletion );
    assertEquals( exception.getMessage(),
                  "Replicant-0008: isNormalCompletion invoked before completion data specified." );
  }
}
