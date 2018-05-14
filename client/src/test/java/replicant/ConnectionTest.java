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
  public void construct()
  {
    final TestConnector connector = TestConnector.create( G.class );
    final String connectionId = ValueUtil.randomString();
    final Connection connection = new Connection( connector, connectionId );

    assertEquals( connection.getConnectionId(), connectionId );
    assertEquals( connection.getLastRxSequence(), 0 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().size(), 0 );
    assertEquals( connection.getCurrentMessageResponse(), null );

    final MessageResponse response = new MessageResponse( "" );
    connection.setCurrentMessageResponse( response );

    assertEquals( connection.getCurrentMessageResponse(), response );

    connection.setLastRxSequence( 2 );
    assertEquals( connection.getLastRxSequence(), 2 );
  }

  @Test
  public void requestSubscribe()
  {
    final TestConnector connector = TestConnector.create( G.class );
    final String connectionId = ValueUtil.randomString();
    final Connection connection = new Connection( connector, connectionId );

    assertEquals( connection.getPendingAreaOfInterestRequests().size(), 0 );

    final ChannelAddress address1 = new ChannelAddress( G.G1 );
    final ChannelAddress address2 = new ChannelAddress( G.G2, 23 );
    final String filter1 = ValueUtil.randomString();
    final String filter2 = ValueUtil.randomString();

    connection.requestSubscribe( address1, filter1 );

    assertEquals( connection.getPendingAreaOfInterestRequests().size(), 1 );

    connection.requestSubscribe( address2, filter2 );

    assertEquals( connection.getPendingAreaOfInterestRequests().size(), 2 );

    final AreaOfInterestRequest request1 = connection.getPendingAreaOfInterestRequests().get( 0 );
    final AreaOfInterestRequest request2 = connection.getPendingAreaOfInterestRequests().get( 1 );

    assertEquals( request1.getAddress(), address1 );
    assertEquals( request1.getFilter(), filter1 );
    assertEquals( request1.getAction(), AreaOfInterestAction.ADD );

    assertEquals( request2.getAddress(), address2 );
    assertEquals( request2.getFilter(), filter2 );
    assertEquals( request2.getAction(), AreaOfInterestAction.ADD );
  }

  @Test
  public void requestSubscriptionUpdate()
  {
    final TestConnector connector = TestConnector.create( G.class );
    final String connectionId = ValueUtil.randomString();
    final Connection connection = new Connection( connector, connectionId );

    assertEquals( connection.getPendingAreaOfInterestRequests().size(), 0 );

    final ChannelAddress address1 = new ChannelAddress( G.G1 );
    final ChannelAddress address2 = new ChannelAddress( G.G2, 23 );
    final String filter1 = ValueUtil.randomString();
    final String filter2 = ValueUtil.randomString();

    connection.requestSubscriptionUpdate( address1, filter1 );

    assertEquals( connection.getPendingAreaOfInterestRequests().size(), 1 );

    connection.requestSubscriptionUpdate( address2, filter2 );

    assertEquals( connection.getPendingAreaOfInterestRequests().size(), 2 );

    final AreaOfInterestRequest request1 = connection.getPendingAreaOfInterestRequests().get( 0 );
    final AreaOfInterestRequest request2 = connection.getPendingAreaOfInterestRequests().get( 1 );

    assertEquals( request1.getAddress(), address1 );
    assertEquals( request1.getFilter(), filter1 );
    assertEquals( request1.getAction(), AreaOfInterestAction.UPDATE );

    assertEquals( request2.getAddress(), address2 );
    assertEquals( request2.getFilter(), filter2 );
    assertEquals( request2.getAction(), AreaOfInterestAction.UPDATE );
  }

  @Test
  public void requestUnsubscribe()
  {
    final TestConnector connector = TestConnector.create( G.class );
    final String connectionId = ValueUtil.randomString();
    final Connection connection = new Connection( connector, connectionId );

    assertEquals( connection.getPendingAreaOfInterestRequests().size(), 0 );

    final ChannelAddress address1 = new ChannelAddress( G.G1 );
    final ChannelAddress address2 = new ChannelAddress( G.G2, 23 );

    connection.requestUnsubscribe( address1 );

    assertEquals( connection.getPendingAreaOfInterestRequests().size(), 1 );

    connection.requestUnsubscribe( address2 );

    assertEquals( connection.getPendingAreaOfInterestRequests().size(), 2 );

    final AreaOfInterestRequest request1 = connection.getPendingAreaOfInterestRequests().get( 0 );
    final AreaOfInterestRequest request2 = connection.getPendingAreaOfInterestRequests().get( 1 );

    assertEquals( request1.getAddress(), address1 );
    assertEquals( request1.getFilter(), null );
    assertEquals( request1.getAction(), AreaOfInterestAction.REMOVE );

    assertEquals( request2.getAddress(), address2 );
    assertEquals( request2.getFilter(), null );
    assertEquals( request2.getAction(), AreaOfInterestAction.REMOVE );
  }

  @Test
  public void enqueueResponse()
  {
    final TestConnector connector = TestConnector.create( G.class );
    final String connectionId = ValueUtil.randomString();
    final Connection connection = new Connection( connector, connectionId );

    assertEquals( connection.getPendingAreaOfInterestRequests().size(), 0 );

    final String data1 = ValueUtil.randomString();
    final String data2 = ValueUtil.randomString();

    assertEquals( connection.getUnparsedResponses().size(), 0 );
    assertEquals( connection.getPendingResponses().size(), 0 );

    connection.enqueueResponse( data1 );

    assertEquals( connection.getUnparsedResponses().size(), 1 );
    assertEquals( connection.getPendingResponses().size(), 0 );

    connection.enqueueResponse( data2 );

    assertEquals( connection.getUnparsedResponses().size(), 2 );
    assertEquals( connection.getPendingResponses().size(), 0 );

    final MessageResponse response1 = connection.getUnparsedResponses().get( 0 );
    final MessageResponse response2 = connection.getUnparsedResponses().get( 1 );

    assertEquals( response1.getRawJsonData(), data1 );
    assertEquals( response2.getRawJsonData(), data2 );
  }

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
    G1, G2
  }
}
