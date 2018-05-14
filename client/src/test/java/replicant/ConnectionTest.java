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
    final Connection connection = new Connection( null, ValueUtil.randomString() );
    final String requestName = ValueUtil.randomString();
    final String cacheKey = ValueUtil.randomString();

    final RequestEntry request = connection.newRequest( requestName, cacheKey );
    assertEquals( request.getName(), requestName );
    assertEquals( request.getCacheKey(), cacheKey );

    assertEquals( connection.getRequest( request.getRequestId() ), request );
    assertEquals( connection.getRequests().get( request.getRequestId() ), request );
    assertEquals( connection.getRequest( "NotHere" + request.getRequestId() ), null );

    assertTrue( connection.removeRequest( request.getRequestId() ) );
    assertFalse( connection.removeRequest( request.getRequestId() ) );

    assertEquals( connection.getRequest( request.getRequestId() ), null );
  }

  @Test
  public void completeNormalRequest()
  {
    final Connection connection = new Connection( null, ValueUtil.randomString() );
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
    final Connection connection = new Connection( null, ValueUtil.randomString() );
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
    final Connection connection = new Connection( null, ValueUtil.randomString() );
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
    final Connection connection = new Connection( null, ValueUtil.randomString() );
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
    final Connection connection = new Connection( null, ValueUtil.randomString() );
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
    final Connection connection = new Connection( null, ValueUtil.randomString() );
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
