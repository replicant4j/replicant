package replicant;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.spy.RequestCompletedEvent;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ConnectionTest
  extends AbstractReplicantTest
{
  @Test
  public void basicRequestManagementWorkflow()
  {
    final Connection connection = new Connection( TestConnector.create( G.class ), ValueUtil.randomString() );
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
  public void completeRequest()
  {
    final Connection connection = new Connection( TestConnector.create( G.class ), ValueUtil.randomString() );
    final RequestEntry e = connection.newRequest( ValueUtil.randomString(), ValueUtil.randomString() );
    final SafeProcedure action = mock( SafeProcedure.class );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    e.setNormalCompletion( false );

    connection.completeRequest( e, action );

    verify( action ).call();
    assertEquals( e.getCompletionAction(), null );
    assertNull( connection.getRequest( e.getRequestId() ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( RequestCompletedEvent.class, ev -> {
      assertEquals( ev.getSystemType(), G.class );
      assertEquals( ev.getRequestId(), e.getRequestId() );
      assertEquals( ev.getName(), e.getName() );
      assertEquals( ev.isNormalCompletion(), false );
      assertEquals( ev.isExpectingResults(), false );
      assertEquals( ev.haveResultsArrived(), false );
    } );
  }

  @Test
  public void completeRequest_expectingResults()
  {
    final Connection connection = new Connection( TestConnector.create( G.class ), ValueUtil.randomString() );
    final RequestEntry e = connection.newRequest( ValueUtil.randomString(), ValueUtil.randomString() );
    final SafeProcedure action = mock( SafeProcedure.class );

    e.setNormalCompletion( true );
    e.setExpectingResults( true );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    connection.completeRequest( e, action );

    verify( action, never() ).call();
    assertEquals( e.getCompletionAction(), action );
    assertNotNull( connection.getRequest( e.getRequestId() ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( RequestCompletedEvent.class, ev -> {
      assertEquals( ev.getSystemType(), G.class );
      assertEquals( ev.getRequestId(), e.getRequestId() );
      assertEquals( ev.getName(), e.getName() );
      assertEquals( ev.isNormalCompletion(), true );
      assertEquals( ev.isExpectingResults(), true );
      assertEquals( ev.haveResultsArrived(), false );
    } );
  }

  @Test
  public void completeRequest_resultsArrived()
  {
    final Connection connection = new Connection( TestConnector.create( G.class ), ValueUtil.randomString() );
    final RequestEntry e = connection.newRequest( ValueUtil.randomString(), ValueUtil.randomString() );
    final SafeProcedure action = mock( SafeProcedure.class );

    e.setNormalCompletion( true );
    e.setExpectingResults( true );
    e.markResultsAsArrived();

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    connection.completeRequest( e, action );

    verify( action ).call();
    assertEquals( e.getCompletionAction(), null );
    assertNull( connection.getRequest( e.getRequestId() ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( RequestCompletedEvent.class, ev -> {
      assertEquals( ev.getSystemType(), G.class );
      assertEquals( ev.getRequestId(), e.getRequestId() );
      assertEquals( ev.getName(), e.getName() );
      assertEquals( ev.isNormalCompletion(), true );
      assertEquals( ev.isExpectingResults(), true );
      assertEquals( ev.haveResultsArrived(), true );
    } );
  }

  enum G
  {
    G1
  }
}
