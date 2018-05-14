package replicant;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ConnectionTest
  extends AbstractReplicantTest
{
  @Test
  public void basicRequestManagementWorkflow()
  {
    final Connection connection = new Connection( ValueUtil.randomString() );
    final RequestEntry e = connection.newRequest( "Y", "X" );
    assertEquals( e.getName(), "Y" );
    assertEquals( e.getCacheKey(), "X" );

    assertEquals( connection.getRequest( e.getRequestId() ), e );
    assertEquals( connection.getRequests().get( e.getRequestId() ), e );
    assertEquals( connection.getRequest( "NotHere" + e.getRequestId() ), null );

    assertTrue( connection.removeRequest( e.getRequestId() ) );
    assertFalse( connection.removeRequest( e.getRequestId() ) );

    assertEquals( connection.getRequest( e.getRequestId() ), null );
  }

  @Test
  public void completeNormalRequest()
  {
    final Connection connection = new Connection( ValueUtil.randomString() );
    final RequestEntry e = connection.newRequest( "Y", "X" );
    final SafeProcedure action = mock( SafeProcedure.class );

    connection.completeNormalRequest( e, action );

    verify( action ).call();
    assertFalse( e.isCompletionDataPresent() );
    assertNull( connection.getRequest( e.getRequestId() ) );
  }

  @Test
  public void completeNormalRequest_expectingResults()
  {
    final Connection connection = new Connection( ValueUtil.randomString() );
    final RequestEntry e = connection.newRequest( "Y", "X" );
    final SafeProcedure action = mock( SafeProcedure.class );

    e.setExpectingResults( true );

    connection.completeNormalRequest( e, action );

    verify( action, never() ).call();
    assertTrue( e.isCompletionDataPresent() );
    assertTrue( e.isNormalCompletion() );
    assertEquals( e.getCompletionAction(), action );
    assertNotNull( connection.getRequest( e.getRequestId() ) );
  }

  @Test
  public void completeNormalRequest_resultsArrived()
  {
    final Connection connection = new Connection( ValueUtil.randomString() );
    final RequestEntry e = connection.newRequest( "Y", "X" );
    final SafeProcedure action = mock( SafeProcedure.class );

    e.setExpectingResults( true );
    e.markResultsAsArrived();

    connection.completeNormalRequest( e, action );

    verify( action ).call();
    assertFalse( e.isCompletionDataPresent() );
    assertNull( connection.getRequest( e.getRequestId() ) );
  }

  @Test
  public void completeNonNormalRequest()
  {
    final Connection connection = new Connection( ValueUtil.randomString() );
    final RequestEntry e = connection.newRequest( "Y", "X" );
    final SafeProcedure action = mock( SafeProcedure.class );

    connection.completeNonNormalRequest( e, action );

    verify( action ).call();
    assertFalse( e.isCompletionDataPresent() );
    assertNull( connection.getRequest( e.getRequestId() ) );
  }

  @Test
  public void completeNonNormalRequest_expectingResults()
  {
    final Connection connection = new Connection( ValueUtil.randomString() );
    final RequestEntry e = connection.newRequest( "Y", "X" );
    final SafeProcedure action = mock( SafeProcedure.class );

    e.setExpectingResults( true );

    connection.completeNonNormalRequest( e, action );

    verify( action, never() ).call();
    assertTrue( e.isCompletionDataPresent() );
    assertFalse( e.isNormalCompletion() );
    assertEquals( e.getCompletionAction(), action );
    assertNotNull( connection.getRequest( e.getRequestId() ) );
  }

  @Test
  public void completeNonNormalRequest_resultsArrived()
  {
    final Connection connection = new Connection( ValueUtil.randomString() );
    final RequestEntry e = connection.newRequest( "Y", "X" );
    final SafeProcedure action = mock( SafeProcedure.class );

    e.setExpectingResults( true );
    e.markResultsAsArrived();

    connection.completeNonNormalRequest( e, action );

    verify( action ).call();
    assertFalse( e.isCompletionDataPresent() );
    assertNull( connection.getRequest( e.getRequestId() ) );
  }
}
