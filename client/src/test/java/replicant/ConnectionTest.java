package replicant;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.messages.OkMessage;
import replicant.messages.ServerToClientMessage;
import replicant.messages.UseCacheMessage;
import replicant.spy.RequestStartedEvent;
import static org.testng.Assert.*;

public class ConnectionTest
  extends AbstractReplicantTest
{
  @Test
  public void construct()
  {
    final Connection connection = createConnection();

    assertNull( connection.getCurrentMessageResponse() );
    assertThrows( connection::ensureCurrentMessageResponse );

    final RequestEntry request = connection.newRequest( ValueUtil.randomString(), false, null );
    final MessageResponse response =
      new MessageResponse( 1, OkMessage.create( request.getRequestId() ), request );
    connection.setCurrentMessageResponse( response );

    assertEquals( connection.getCurrentMessageResponse(), response );
    assertEquals( connection.ensureCurrentMessageResponse(), response );
  }

  @Test
  public void selectNextMessageResponse_noMessages()
  {
    final Connection connection = createConnection();

    assertNull( connection.getCurrentMessageResponse() );

    final boolean selectedMessage = connection.selectNextMessageResponse();

    assertFalse( selectedMessage );
    assertNull( connection.getCurrentMessageResponse() );
  }

  @Test
  public void selectNextMessageResponse()
  {
    final Connection connection = createConnection();

    connection.enqueueResponse( UseCacheMessage.create( null, "0", ValueUtil.randomString() ), null );

    assertNull( connection.getCurrentMessageResponse() );
    assertEquals( connection.getPendingResponses().size(), 1 );

    final boolean selectedMessage = connection.selectNextMessageResponse();

    assertTrue( selectedMessage );
    final MessageResponse currentMessageResponse = connection.getCurrentMessageResponse();
    assertNotNull( currentMessageResponse );
    assertEquals( connection.getPendingResponses().size(), 0 );
  }

  @Test
  public void requestExec()
  {
    final Connection connection = createConnection();

    assertEquals( connection.getPendingExecRequests().size(), 0 );

    final String command = ValueUtil.randomString();
    final Object payload = new Object();
    connection.requestExec( command, payload, null );

    final List<ExecRequest> requests = connection.getPendingExecRequests();
    assertEquals( requests.size(), 1 );
    final ExecRequest request = requests.get( 0 );
    assertEquals( request.getCommand(), command );
    assertEquals( request.getPayload(), payload );
  }

  @Test
  public void requestSubscribe()
  {
    final Connection connection = createConnection();

    assertEquals( connection.getPendingAreaOfInterestRequests().size(), 0 );

    final ChannelAddress address1 = new ChannelAddress( 1, 0 );
    final ChannelAddress address2 = new ChannelAddress( 1, 1, 23 );
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
    assertEquals( request1.getType(), AreaOfInterestRequest.Type.ADD );

    assertEquals( request2.getAddress(), address2 );
    assertEquals( request2.getFilter(), filter2 );
    assertEquals( request2.getType(), AreaOfInterestRequest.Type.ADD );
  }

  @Test
  public void requestSubscriptionUpdate()
  {
    final Connection connection = createConnection();

    assertEquals( connection.getPendingAreaOfInterestRequests().size(), 0 );

    final ChannelAddress address1 = new ChannelAddress( 1, 0 );
    final ChannelAddress address2 = new ChannelAddress( 1, 1, 23 );
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
    assertEquals( request1.getType(), AreaOfInterestRequest.Type.UPDATE );

    assertEquals( request2.getAddress(), address2 );
    assertEquals( request2.getFilter(), filter2 );
    assertEquals( request2.getType(), AreaOfInterestRequest.Type.UPDATE );
  }

  @Test
  public void requestUnsubscribe()
  {
    final Connection connection = createConnection();

    assertEquals( connection.getPendingAreaOfInterestRequests().size(), 0 );

    final ChannelAddress address1 = new ChannelAddress( 1, 0 );
    final ChannelAddress address2 = new ChannelAddress( 1, 1, 23 );

    connection.requestUnsubscribe( address1 );

    assertEquals( connection.getPendingAreaOfInterestRequests().size(), 1 );

    connection.requestUnsubscribe( address2 );

    assertEquals( connection.getPendingAreaOfInterestRequests().size(), 2 );

    final AreaOfInterestRequest request1 = connection.getPendingAreaOfInterestRequests().get( 0 );
    final AreaOfInterestRequest request2 = connection.getPendingAreaOfInterestRequests().get( 1 );

    assertEquals( request1.getAddress(), address1 );
    assertNull( request1.getFilter() );
    assertEquals( request1.getType(), AreaOfInterestRequest.Type.REMOVE );

    assertEquals( request2.getAddress(), address2 );
    assertNull( request2.getFilter() );
    assertEquals( request2.getType(), AreaOfInterestRequest.Type.REMOVE );
  }

  @Test
  public void enqueueResponse()
  {
    final Connection connection = createConnection();

    assertEquals( connection.getPendingAreaOfInterestRequests().size(), 0 );

    final ServerToClientMessage data1 = OkMessage.create( 1 );
    final ServerToClientMessage data2 = OkMessage.create( 1 );

    assertEquals( connection.getPendingResponses().size(), 0 );

    connection.enqueueResponse( data1, null );

    assertEquals( connection.getPendingResponses().size(), 1 );

    connection.enqueueResponse( data2, null );

    assertEquals( connection.getPendingResponses().size(), 2 );

    final MessageResponse response1 = connection.getPendingResponses().get( 0 );
    final MessageResponse response2 = connection.getPendingResponses().get( 1 );

    assertEquals( response1.getMessage(), data1 );
    assertEquals( response2.getMessage(), data2 );
  }

  @Test
  public void basicRequestManagementWorkflow()
  {
    final Connection connection = createConnection();
    final Connector connector = connection.getConnector();
    final String requestName = ValueUtil.randomString();

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final RequestEntry request = connection.newRequest( requestName, false, null );

    assertEquals( request.getName(), requestName );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( RequestStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getRequestId(), request.getRequestId() );
      assertEquals( e.getName(), requestName );
    } );

    assertEquals( connection.getRequest( request.getRequestId() ), request );
    assertEquals( connection.getRequests().get( request.getRequestId() ), request );
    assertNull( connection.getRequests().get( ValueUtil.randomInt() ) );

    connection.removeRequest( request.getRequestId() );

    assertNull( connection.getRequests().get( request.getRequestId() ) );
  }

  @Test
  public void execLifecycle()
  {
    final Connection connection = createConnection();

    assertEquals( connection.getActiveExecRequests().size(), 0 );
    assertEquals( connection.getPendingExecRequests().size(), 0 );

    final String command = ValueUtil.randomString();
    final Object payload = new Object();

    // Request Exec
    {
      connection.requestExec( command, payload, null );

      assertEquals( connection.getActiveExecRequests().size(), 0 );
      final List<ExecRequest> requests = connection.getPendingExecRequests();
      assertEquals( requests.size(), 1 );
      final ExecRequest request = requests.get( 0 );
      assertEquals( request.getCommand(), command );
      assertEquals( request.getPayload(), payload );
    }

    {
      final ExecRequest request = connection.nextExecRequest();
      assertNotNull( request );
      assertEquals( request.getCommand(), command );
      assertEquals( request.getPayload(), payload );
      assertEquals( request.getRequestId(), -1 );

      assertEquals( connection.getPendingExecRequests().size(), 0 );
      assertEquals( connection.getActiveExecRequests().size(), 0 );

      final int requestId = ValueUtil.randomInt();
      request.markAsInProgress( requestId );

      assertEquals( request.getRequestId(), requestId );

      connection.recordActiveExecRequest( request );

      assertEquals( connection.getPendingExecRequests().size(), 0 );
      assertEquals( connection.getActiveExecRequests().size(), 1 );
      assertEquals( connection.getActiveExecRequest( requestId ), request );
      assertTrue( request.isInProgress() );
      assertEquals( request.getRequestId(), requestId );

      connection.markExecRequestAsComplete( requestId );

      assertEquals( connection.getPendingExecRequests().size(), 0 );
      assertEquals( connection.getActiveExecRequests().size(), 0 );
      assertNull( connection.getActiveExecRequest( requestId ) );
      assertFalse( request.isInProgress() );
      assertEquals( request.getRequestId(), -1 );
    }

    {
      assertNull( connection.nextExecRequest() );
    }
  }

  @Test
  public void removeRequestWhenNoRequest()
  {
    final Connection connection = createConnection();

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> connection.removeRequest( 789 ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0067: Attempted to remove request with id 789 from connection with id '" +
                  connection.getConnectionId() + "' but no such request exists." );
  }

  @Test
  public void completeAreaOfInterestRequest()
  {
    final Connection connection = createConnection();

    final ChannelAddress address1 = new ChannelAddress( 1, 0, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 0, 2 );

    final Object filter1 = null;
    final Object filter2 = null;

    final AreaOfInterestRequest request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.ADD, filter1 );
    final AreaOfInterestRequest request2 =
      new AreaOfInterestRequest( address2, AreaOfInterestRequest.Type.ADD, filter2 );
    connection.injectCurrentAreaOfInterestRequest( request1 );
    connection.injectCurrentAreaOfInterestRequest( request2 );

    request1.markAsInProgress( 1 );
    request2.markAsInProgress( 2 );

    assertTrue( request1.isInProgress() );
    assertTrue( request2.isInProgress() );
    assertEquals( connection.getCurrentAreaOfInterestRequests().size(), 2 );

    connection.completeAreaOfInterestRequest();

    assertFalse( request1.isInProgress() );
    assertFalse( request2.isInProgress() );
    assertEquals( connection.getCurrentAreaOfInterestRequests().size(), 0 );
  }

  @Test
  public void completeAreaOfInterestRequest_whenNoRequests()
  {
    final Connection connection = createConnection();

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, connection::completeAreaOfInterestRequest );
    assertEquals( exception.getMessage(),
                  "Replicant-0023: Connection.completeAreaOfInterestRequest() invoked when there is no current AreaOfInterest requests." );
  }

  @Test
  public void canGroupRequests()
  {
    final Connection connection = createConnection();

    final ChannelAddress addressA = new ChannelAddress( 1, 0 );
    final ChannelAddress addressB = new ChannelAddress( 1, 1, 1 );
    final ChannelAddress addressC = new ChannelAddress( 1, 1, 2 );
    final ChannelAddress addressD = new ChannelAddress( 1, 1, 3 );
    final ChannelAddress addressE = new ChannelAddress( 1, 1, 4 );

    final String filterP = null;
    final String filterQ = "F1";
    final String filterR = "F2";

    final AreaOfInterestRequest request1 =
      new AreaOfInterestRequest( addressA, AreaOfInterestRequest.Type.ADD, filterP );
    final AreaOfInterestRequest request2 =
      new AreaOfInterestRequest( addressA, AreaOfInterestRequest.Type.REMOVE, filterP );
    final AreaOfInterestRequest request3 =
      new AreaOfInterestRequest( addressA, AreaOfInterestRequest.Type.UPDATE, filterP );
    final AreaOfInterestRequest request4 =
      new AreaOfInterestRequest( addressA, AreaOfInterestRequest.Type.ADD, filterP );

    final AreaOfInterestRequest request10 =
      new AreaOfInterestRequest( addressB, AreaOfInterestRequest.Type.ADD, filterQ );
    final AreaOfInterestRequest request11 =
      new AreaOfInterestRequest( addressC, AreaOfInterestRequest.Type.ADD, filterQ );
    final AreaOfInterestRequest request12 =
      new AreaOfInterestRequest( addressD, AreaOfInterestRequest.Type.REMOVE, null );
    final AreaOfInterestRequest request13 =
      new AreaOfInterestRequest( addressE, AreaOfInterestRequest.Type.REMOVE, null );
    final AreaOfInterestRequest request14 =
      new AreaOfInterestRequest( addressE, AreaOfInterestRequest.Type.UPDATE, filterQ );
    final AreaOfInterestRequest request15 =
      new AreaOfInterestRequest( addressE, AreaOfInterestRequest.Type.ADD, filterP );
    final AreaOfInterestRequest request16 =
      new AreaOfInterestRequest( addressE, AreaOfInterestRequest.Type.UPDATE, filterP );
    final AreaOfInterestRequest request17 =
      new AreaOfInterestRequest( addressE, AreaOfInterestRequest.Type.REMOVE, null );
    final AreaOfInterestRequest request18 =
      new AreaOfInterestRequest( addressE, AreaOfInterestRequest.Type.UPDATE, filterP );
    final AreaOfInterestRequest request19 =
      new AreaOfInterestRequest( addressE, AreaOfInterestRequest.Type.UPDATE, filterR );

    final List<AreaOfInterestRequest> requests =
      Arrays.asList( request1,
                     request2,
                     request3,
                     request4,
                     request10,
                     request11,
                     request12,
                     request13,
                     request14,
                     request15,
                     request16,
                     request17,
                     request18,
                     request19 );

    final HashMap<String, String> groupingPairs = new HashMap<>();

    groupingPairs.put( request10.toString(), request11.toString() );
    groupingPairs.put( request12.toString(), request13.toString() );
    groupingPairs.put( request12.toString(), request17.toString() );
    groupingPairs.put( request13.toString(), request17.toString() );
    groupingPairs.put( request16.toString(), request18.toString() );

    for ( final AreaOfInterestRequest r1 : requests )
    {
      for ( final AreaOfInterestRequest r2 : requests )
      {
        final boolean expected =
          ( r1 == r2 && null != r1.getAddress().getId() ) ||
          Objects.equals( String.valueOf( groupingPairs.get( r1.toString() ) ), r2.toString() ) ||
          Objects.equals( String.valueOf( groupingPairs.get( r2.toString() ) ), r1.toString() );
        assertEquals( connection.canGroupRequests( r1, r2 ),
                      expected,
                      "Comparing " + r1 + " versus " + r2 );
      }
    }
  }

  @Test
  public void canGroupRequests_presentInCache()
  {
    final Connection connection = createConnection();

    final ChannelAddress addressA = new ChannelAddress( 1, 1, 1 );
    final ChannelAddress addressB = new ChannelAddress( 1, 1, 2 );

    final AreaOfInterestRequest requestA = new AreaOfInterestRequest( addressA, AreaOfInterestRequest.Type.ADD, null );
    final AreaOfInterestRequest requestB = new AreaOfInterestRequest( addressB, AreaOfInterestRequest.Type.ADD, null );

    assertTrue( connection.canGroupRequests( requestA, requestB ) );
    assertTrue( connection.canGroupRequests( requestB, requestA ) );

    final TestCacheService cacheService = new TestCacheService();
    Replicant.context().setCacheService( cacheService );

    cacheService.store( addressA, ValueUtil.randomString(), ValueUtil.randomString() );

    assertFalse( connection.canGroupRequests( requestA, requestB ) );
    assertFalse( connection.canGroupRequests( requestB, requestA ) );
  }

  @Test
  public void getCurrentAreaOfInterestRequests()
  {
    final Connection connection = createConnection();

    final ChannelAddress addressA = new ChannelAddress( 1, 1, 1 );
    final ChannelAddress addressB = new ChannelAddress( 1, 1, 2 );
    final ChannelAddress addressC = new ChannelAddress( 1, 1, 3 );

    assertEquals( connection.getCurrentAreaOfInterestRequests().size(), 0 );

    connection.requestSubscribe( addressA, null );
    connection.requestSubscribe( addressB, null );

    assertEquals( connection.getPendingAreaOfInterestRequests().size(), 2 );

    // This should transfer the above two and group them
    assertEquals( connection.getCurrentAreaOfInterestRequests().size(), 2 );

    assertEquals( connection.getPendingAreaOfInterestRequests().size(), 0 );

    // These should all go to pending queue
    connection.requestSubscribe( addressA, null );
    connection.requestSubscribe( addressB, null );
    connection.requestSubscribe( addressC, null );

    assertEquals( connection.getCurrentAreaOfInterestRequests().size(), 2 );

    assertEquals( connection.getPendingAreaOfInterestRequests().size(), 3 );
  }

  @Test
  public void lastIndexOfPendingAreaOfInterestRequest_passingNonnullFilterForDelete()
  {
    final Connection connection = createConnection();

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final String filter = "MyFilter";

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> connection.lastIndexOfPendingAreaOfInterestRequest( AreaOfInterestRequest.Type.REMOVE,
                                                                              address,
                                                                              filter ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0024: Connection.lastIndexOfPendingAreaOfInterestRequest passed a REMOVE request for address '1.0' with a non-null filter 'MyFilter'." );
  }

  @Test
  public void isAreaOfInterestRequestPending_passingNonnullFilterForDelete()
  {
    final Connection connection = createConnection();

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final String filter = "MyFilter";

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> connection.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.REMOVE,
                                                                     address,
                                                                     filter ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0025: Connection.isAreaOfInterestRequestPending passed a REMOVE request for address '1.0' with a non-null filter 'MyFilter'." );
  }

  @Test
  public void pendingAreaOfInterestRequestQueries_noRequestsInConnection()
  {
    final Connection connection = createConnection();

    final ChannelAddress address1 = new ChannelAddress( 1, 0 );

    assertRequestPending( connection, AreaOfInterestRequest.Type.ADD, address1, null, false );
    assertRequestPending( connection, AreaOfInterestRequest.Type.REMOVE, address1, null, false );
    assertRequestPending( connection, AreaOfInterestRequest.Type.UPDATE, address1, null, false );
    assertRequestPendingIndex( connection, AreaOfInterestRequest.Type.ADD, address1, null, -1 );
    assertRequestPendingIndex( connection, AreaOfInterestRequest.Type.REMOVE, address1, null, -1 );
    assertRequestPendingIndex( connection, AreaOfInterestRequest.Type.UPDATE, address1, null, -1 );
  }

  @Test
  public void pendingAreaOfInterestRequestQueries_requestPending()
  {
    final Connection connection = createConnection();

    final ChannelAddress address1 = new ChannelAddress( 1, 0, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 0, 2 );
    final ChannelAddress address3 = new ChannelAddress( 1, 0, 3 );

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
  {
    final Connection connection = createConnection();

    final ChannelAddress address1 = new ChannelAddress( 1, 0, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 0, 2 );
    final ChannelAddress address3 = new ChannelAddress( 1, 0, 3 );

    final Object filter1 = null;
    final Object filter2 = null;
    final Object filter3 = null;

    assertRequestPendingState( connection, address1, filter1, false, false, false, -1, -1, -1 );
    assertRequestPendingState( connection, address2, filter2, false, false, false, -1, -1, -1 );
    assertRequestPendingState( connection, address3, filter3, false, false, false, -1, -1, -1 );

    connection.injectCurrentAreaOfInterestRequest( new AreaOfInterestRequest( address1,
                                                                              AreaOfInterestRequest.Type.ADD,
                                                                              filter1 ) );

    assertRequestPendingState( connection, address1, filter1, true, false, false, 0, -1, -1 );
    assertRequestPendingState( connection, address2, filter2, false, false, false, -1, -1, -1 );
    assertRequestPendingState( connection, address3, filter3, false, false, false, -1, -1, -1 );

    connection.injectCurrentAreaOfInterestRequest( new AreaOfInterestRequest( address2,
                                                                              AreaOfInterestRequest.Type.ADD,
                                                                              filter2 ) );

    assertRequestPendingState( connection, address1, filter1, true, false, false, 0, -1, -1 );
    assertRequestPendingState( connection, address2, filter2, true, false, false, 0, -1, -1 );
    assertRequestPendingState( connection, address3, filter3, false, false, false, -1, -1, -1 );
  }

  @Test
  public void pendingAreaOfInterestRequestQueries_jumbledAggregate()
  {
    final Connection connection = createConnection();

    final ChannelAddress address1 = new ChannelAddress( 1, 0, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 0, 2 );
    final ChannelAddress address3 = new ChannelAddress( 1, 0, 3 );
    final ChannelAddress address4 = new ChannelAddress( 1, 1 );

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

    connection.injectCurrentAreaOfInterestRequest( new AreaOfInterestRequest( address1,
                                                                              AreaOfInterestRequest.Type.ADD,
                                                                              filter1 ) );
    connection.injectCurrentAreaOfInterestRequest( new AreaOfInterestRequest( address4,
                                                                              AreaOfInterestRequest.Type.ADD,
                                                                              filter4 ) );

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
    assertRequestPending( connection, AreaOfInterestRequest.Type.ADD, address, filter, hasAdd );
    assertRequestPending( connection, AreaOfInterestRequest.Type.UPDATE, address, filter, hasUpdate );
    assertRequestPending( connection, AreaOfInterestRequest.Type.REMOVE, address, null, hasRemove );
    assertRequestPendingIndex( connection, AreaOfInterestRequest.Type.ADD, address, filter, addIndex );
    assertRequestPendingIndex( connection, AreaOfInterestRequest.Type.UPDATE, address, filter, updateIndex );
    assertRequestPendingIndex( connection, AreaOfInterestRequest.Type.REMOVE, address, null, removeIndex );
  }

  private void assertRequestPendingIndex( @Nonnull final Connection connection,
                                          @Nonnull final AreaOfInterestRequest.Type action,
                                          @Nonnull final ChannelAddress address,
                                          @Nullable final Object filter,
                                          final int expected )
  {
    assertEquals( connection.lastIndexOfPendingAreaOfInterestRequest( action, address, filter ), expected );
  }

  private void assertRequestPending( @Nonnull final Connection connection,
                                     @Nonnull final AreaOfInterestRequest.Type action,
                                     @Nonnull final ChannelAddress address,
                                     @Nullable final Object filter,
                                     final boolean expected )
  {
    assertEquals( connection.isAreaOfInterestRequestPending( action, address, filter ), expected );
  }
}
