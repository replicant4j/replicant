package replicant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.spy.RequestCompletedEvent;
import replicant.spy.RequestStartedEvent;
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
  public void enqueueOutOfBandResponse()
  {
    final TestConnector connector = TestConnector.create( G.class );
    final String connectionId = ValueUtil.randomString();
    final Connection connection = new Connection( connector, connectionId );

    assertEquals( connection.getPendingAreaOfInterestRequests().size(), 0 );

    final String data1 = ValueUtil.randomString();
    final String data2 = ValueUtil.randomString();

    final SafeProcedure action1 = () -> {
    };
    final SafeProcedure action2 = () -> {
    };

    assertEquals( connection.getOutOfBandResponses().size(), 0 );

    connection.enqueueOutOfBandResponse( data1, action1 );

    assertEquals( connection.getOutOfBandResponses().size(), 1 );

    connection.enqueueOutOfBandResponse( data2, action2 );

    assertEquals( connection.getOutOfBandResponses().size(), 2 );

    final MessageResponse response1 = connection.getOutOfBandResponses().get( 0 );
    final MessageResponse response2 = connection.getOutOfBandResponses().get( 1 );

    assertEquals( response1.getRawJsonData(), data1 );
    assertEquals( response1.getCompletionAction(), action1 );

    assertEquals( response2.getRawJsonData(), data2 );
    assertEquals( response2.getCompletionAction(), action2 );
  }

  @Test
  public void basicRequestManagementWorkflow()
  {
    final Connection connection = new Connection( TestConnector.create( G.class ), ValueUtil.randomString() );
    final String requestName = ValueUtil.randomString();
    final String cacheKey = ValueUtil.randomString();

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final RequestEntry request = connection.newRequest( requestName, cacheKey );

    assertEquals( request.getName(), requestName );
    assertEquals( request.getCacheKey(), cacheKey );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( RequestStartedEvent.class, e -> {
      assertEquals( e.getSystemType(), G.class );
      assertEquals( e.getRequestId(), request.getRequestId() );
      assertEquals( e.getName(), request.getName() );
    } );

    assertEquals( connection.getRequest( request.getRequestId() ), request );
    assertEquals( connection.getRequests().get( request.getRequestId() ), request );
    assertEquals( connection.getRequest( "NotHere" + request.getRequestId() ), null );

    assertTrue( connection.removeRequest( request.getRequestId() ) );
    assertFalse( connection.removeRequest( request.getRequestId() ) );

    assertEquals( connection.getRequest( request.getRequestId() ), null );
  }

  @Test
  public void completeAreaOfInterestRequest()
  {
    final Connection connection = new Connection( TestConnector.create( G.class ), ValueUtil.randomString() );

    final ChannelAddress address1 = new ChannelAddress( G.G1, 1 );
    final ChannelAddress address2 = new ChannelAddress( G.G1, 2 );

    final Object filter1 = null;
    final Object filter2 = null;

    final AreaOfInterestRequest request1 = new AreaOfInterestRequest( address1, AreaOfInterestAction.ADD, filter1 );
    final AreaOfInterestRequest request2 = new AreaOfInterestRequest( address2, AreaOfInterestAction.ADD, filter2 );
    connection.getCurrentAreaOfInterestRequests().add( request1 );
    connection.getCurrentAreaOfInterestRequests().add( request2 );

    request1.markAsInProgress();
    request2.markAsInProgress();

    assertEquals( request1.isInProgress(), true );
    assertEquals( request1.isInProgress(), true );
    assertEquals( connection.getCurrentAreaOfInterestRequests().size(), 2 );

    connection.completeAreaOfInterestRequest();

    assertEquals( request1.isInProgress(), false );
    assertEquals( request1.isInProgress(), false );
    assertEquals( connection.getCurrentAreaOfInterestRequests().size(), 0 );
  }

  @Test
  public void completeAreaOfInterestRequest_whenNoRequests()
  {
    final Connection connection = new Connection( TestConnector.create( G.class ), ValueUtil.randomString() );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, connection::completeAreaOfInterestRequest );
    assertEquals( exception.getMessage(),
                  "Replicant-0023: Connection.completeAreaOfInterestRequest() invoked when there is no current AreaOfInterest requests." );
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

  @Test
  public void lastIndexOfPendingAreaOfInterestRequest_passingNonnullFilterForDelete()
    throws Exception
  {
    final Connection connection = new Connection( TestConnector.create( G.class ), ValueUtil.randomString() );

    final ChannelAddress address = new ChannelAddress( G.G1 );
    final String filter = "MyFilter";

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> connection.lastIndexOfPendingAreaOfInterestRequest( AreaOfInterestAction.REMOVE,
                                                                              address,
                                                                              filter ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0024: Connection.lastIndexOfPendingAreaOfInterestRequest passed a REMOVE request for address 'G.G1' with a non-null filter 'MyFilter'." );
  }

  @Test
  public void isAreaOfInterestRequestPending_passingNonnullFilterForDelete()
    throws Exception
  {
    final Connection connection = new Connection( TestConnector.create( G.class ), ValueUtil.randomString() );

    final ChannelAddress address = new ChannelAddress( G.G1 );
    final String filter = "MyFilter";

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> connection.isAreaOfInterestRequestPending( AreaOfInterestAction.REMOVE,
                                                                     address,
                                                                     filter ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0025: Connection.isAreaOfInterestRequestPending passed a REMOVE request for address 'G.G1' with a non-null filter 'MyFilter'." );
  }

  @Test
  public void pendingAreaOfInterestRequestQueries_noRequestsInConnection()
    throws Exception
  {
    final Connection connection = new Connection( TestConnector.create( G.class ), ValueUtil.randomString() );

    final ChannelAddress address1 = new ChannelAddress( G.G1 );

    assertRequestPending( connection, AreaOfInterestAction.ADD, address1, null, false );
    assertRequestPending( connection, AreaOfInterestAction.REMOVE, address1, null, false );
    assertRequestPending( connection, AreaOfInterestAction.UPDATE, address1, null, false );
    assertRequestPendingIndex( connection, AreaOfInterestAction.ADD, address1, null, -1 );
    assertRequestPendingIndex( connection, AreaOfInterestAction.REMOVE, address1, null, -1 );
    assertRequestPendingIndex( connection, AreaOfInterestAction.UPDATE, address1, null, -1 );
  }

  @Test
  public void pendingAreaOfInterestRequestQueries_requestPending()
    throws Exception
  {
    final Connection connection = new Connection( TestConnector.create( G.class ), ValueUtil.randomString() );

    final ChannelAddress address1 = new ChannelAddress( G.G1, 1 );
    final ChannelAddress address2 = new ChannelAddress( G.G1, 2 );
    final ChannelAddress address3 = new ChannelAddress( G.G1, 3 );

    final Object filter1 = null;
    final Object filter2 = ValueUtil.randomString();
    final Object filter3 = null;

    assertRequestPendingState( connection, address1, filter1, false, false, false, -1, -1, -1 );
    assertRequestPendingState( connection, address2, filter2, false, false, false, -1, -1, -1 );
    assertRequestPendingState( connection, address3, filter3, false, false, false, -1, -1, -1 );

    connection.requestSubscribe( address1, null );

    assertRequestPendingState( connection, address1, filter1, true, false, false, 1, -1, -1 );
    assertRequestPendingState( connection, address2, filter2, false, false, false, -1, -1, -1 );
    assertRequestPendingState( connection, address3, filter3, false, false, false, -1, -1, -1 );

    connection.requestSubscriptionUpdate( address2, filter2 );

    assertRequestPendingState( connection, address1, filter1, true, false, false, 1, -1, -1 );
    assertRequestPendingState( connection, address2, filter2, false, true, false, -1, 2, -1 );
    assertRequestPendingState( connection, address3, filter3, false, false, false, -1, -1, -1 );

    connection.requestUnsubscribe( address3 );

    assertRequestPendingState( connection, address1, filter1, true, false, false, 1, -1, -1 );
    assertRequestPendingState( connection, address2, filter2, false, true, false, -1, 2, -1 );
    assertRequestPendingState( connection, address3, filter3, false, false, true, -1, -1, 3 );
  }

  @Test
  public void pendingAreaOfInterestRequestQueries_currentPending()
    throws Exception
  {
    final Connection connection = new Connection( TestConnector.create( G.class ), ValueUtil.randomString() );

    final ChannelAddress address1 = new ChannelAddress( G.G1, 1 );
    final ChannelAddress address2 = new ChannelAddress( G.G1, 2 );
    final ChannelAddress address3 = new ChannelAddress( G.G1, 3 );

    final Object filter1 = null;
    final Object filter2 = null;
    final Object filter3 = null;

    assertRequestPendingState( connection, address1, filter1, false, false, false, -1, -1, -1 );
    assertRequestPendingState( connection, address2, filter2, false, false, false, -1, -1, -1 );
    assertRequestPendingState( connection, address3, filter3, false, false, false, -1, -1, -1 );

    connection.getCurrentAreaOfInterestRequests()
      .add( new AreaOfInterestRequest( address1, AreaOfInterestAction.ADD, filter1 ) );

    assertRequestPendingState( connection, address1, filter1, true, false, false, 0, -1, -1 );
    assertRequestPendingState( connection, address2, filter2, false, false, false, -1, -1, -1 );
    assertRequestPendingState( connection, address3, filter3, false, false, false, -1, -1, -1 );

    connection.getCurrentAreaOfInterestRequests()
      .add( new AreaOfInterestRequest( address2, AreaOfInterestAction.ADD, filter2 ) );

    assertRequestPendingState( connection, address1, filter1, true, false, false, 0, -1, -1 );
    assertRequestPendingState( connection, address2, filter2, true, false, false, 0, -1, -1 );
    assertRequestPendingState( connection, address3, filter3, false, false, false, -1, -1, -1 );
  }

  @Test
  public void pendingAreaOfInterestRequestQueries_jumbledAggregate()
    throws Exception
  {
    final Connection connection = new Connection( TestConnector.create( G.class ), ValueUtil.randomString() );

    final ChannelAddress address1 = new ChannelAddress( G.G1, 1 );
    final ChannelAddress address2 = new ChannelAddress( G.G1, 2 );
    final ChannelAddress address3 = new ChannelAddress( G.G1, 3 );
    final ChannelAddress address4 = new ChannelAddress( G.G2 );

    final Object filter1 = null;
    final Object filter2 = ValueUtil.randomString();
    final Object filter3 = null;
    final Object filter4 = null;

    assertRequestPendingState( connection, address1, filter1, false, false, false, -1, -1, -1 );
    assertRequestPendingState( connection, address2, filter2, false, false, false, -1, -1, -1 );
    assertRequestPendingState( connection, address3, filter3, false, false, false, -1, -1, -1 );
    assertRequestPendingState( connection, address4, filter4, false, false, false, -1, -1, -1 );

    // Same address with multiple chained requests
    connection.requestUnsubscribe( address1 );
    connection.requestSubscribe( address1, filter1 );
    connection.requestSubscriptionUpdate( address1, filter1 );

    // Same address - multiple update requests
    connection.requestSubscriptionUpdate( address2, filter2 );
    connection.requestSubscriptionUpdate( address2, filter2 );

    // Same address - bad requests sequence
    connection.requestUnsubscribe( address3 );
    connection.requestSubscriptionUpdate( address3, filter3 );

    // Same address - bad requests sequence
    connection.requestUnsubscribe( address3 );
    connection.requestSubscriptionUpdate( address3, filter3 );

    // Back to the first address
    connection.requestSubscribe( address1, filter1 );

    connection.getCurrentAreaOfInterestRequests()
      .add( new AreaOfInterestRequest( address1, AreaOfInterestAction.ADD, filter1 ) );
    connection.getCurrentAreaOfInterestRequests()
      .add( new AreaOfInterestRequest( address4, AreaOfInterestAction.ADD, filter4 ) );

    assertRequestPendingState( connection, address1, filter1, true, true, true, 10, 3, 1 );
    assertRequestPendingState( connection, address2, filter2, false, true, false, -1, 5, -1 );
    assertRequestPendingState( connection, address3, filter3, false, true, true, -1, 9, 8 );
    assertRequestPendingState( connection, address4, filter4, true, false, false, 0, -1, -1 );
  }

  private void assertRequestPendingState( final Connection connection,
                                          final ChannelAddress address,
                                          final Object filter,
                                          final boolean hasAdd,
                                          final boolean hasUpdate,
                                          final boolean hasRemove,
                                          final int addIndex,
                                          final int updateIndex,
                                          final int removeIndex )
  {
    assertRequestPending( connection, AreaOfInterestAction.ADD, address, filter, hasAdd );
    assertRequestPending( connection, AreaOfInterestAction.UPDATE, address, filter, hasUpdate );
    assertRequestPending( connection, AreaOfInterestAction.REMOVE, address, filter, hasRemove );
    assertRequestPendingIndex( connection, AreaOfInterestAction.ADD, address, filter, addIndex );
    assertRequestPendingIndex( connection, AreaOfInterestAction.UPDATE, address, filter, updateIndex );
    assertRequestPendingIndex( connection, AreaOfInterestAction.REMOVE, address, filter, removeIndex );
  }

  private void assertRequestPendingIndex( @Nonnull final Connection connection,
                                          @Nonnull final AreaOfInterestAction action,
                                          @Nonnull final ChannelAddress address,
                                          @Nullable final Object filter,
                                          final int expected )
  {
    assertEquals( connection.lastIndexOfPendingAreaOfInterestRequest( action, address, filter ), expected );
  }

  private void assertRequestPending( @Nonnull final Connection connection,
                                     @Nonnull final AreaOfInterestAction action,
                                     @Nonnull final ChannelAddress address,
                                     @Nullable final Object filter,
                                     final boolean expected )
  {
    assertEquals( connection.isAreaOfInterestRequestPending( action, address, filter ), expected );
  }

  enum G
  {
    G1, G2
  }
}
