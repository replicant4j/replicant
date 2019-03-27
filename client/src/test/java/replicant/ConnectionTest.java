package replicant;

import arez.Disposable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.messages.ChangeSetMessage;
import replicant.spy.RequestCompletedEvent;
import replicant.spy.RequestStartedEvent;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ConnectionTest
  extends AbstractReplicantTest
{
  @SuppressWarnings( "ResultOfMethodCallIgnored" )
  @Test
  public void construct()
  {
    final Connection connection = createConnection();

    assertFalse( connection.isDisposed() );
    assertNull( connection.getCurrentMessageResponse() );
    assertNotNull( connection.getTransportContext() );
    assertThrows( connection::ensureCurrentMessageResponse );

    final MessageResponse response = new MessageResponse( "" );
    connection.setCurrentMessageResponse( response );

    assertEquals( connection.getCurrentMessageResponse(), response );
    assertEquals( connection.ensureCurrentMessageResponse(), response );
  }

  @Test
  public void dispose()
  {
    final Connection connection = createConnection();

    assertFalse( connection.isDisposed() );
    assertFalse( Disposable.isDisposed( connection.getTransportContext() ) );

    connection.dispose();

    assertTrue( connection.isDisposed() );
    assertTrue( Disposable.isDisposed( connection.getTransportContext() ) );
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
  public void selectNextMessageResponse_unparsedMessage()
  {
    final Connection connection = createConnection();

    // Add an unparsed message to queue
    connection.enqueueResponse( "" );

    assertNull( connection.getCurrentMessageResponse() );
    assertEquals( connection.getUnparsedResponses().size(), 1 );

    final boolean selectedMessage = connection.selectNextMessageResponse();

    assertTrue( selectedMessage );
    assertNotNull( connection.getCurrentMessageResponse() );
    assertEquals( connection.getUnparsedResponses().size(), 0 );
  }

  @Test
  public void selectNextMessageResponse_parsedMessage()
  {
    final Connection connection = createConnection();

    final MessageResponse response = new MessageResponse( "" );
    response.recordChangeSet( ChangeSetMessage.create( null, null, null, null, null ), null );

    // Add a "parsed" message with sequence indicating it is next
    connection.injectPendingResponses( response );

    assertNull( connection.getCurrentMessageResponse() );
    assertEquals( connection.getPendingResponses().size(), 1 );

    final boolean selectedMessage = connection.selectNextMessageResponse();

    assertTrue( selectedMessage );
    final MessageResponse currentMessageResponse = connection.getCurrentMessageResponse();
    assertNotNull( currentMessageResponse );
    assertEquals( connection.getPendingResponses().size(), 0 );
  }

  @Test
  public void queueCurrentResponse()
  {
    final Connection connection = createConnection();

    final MessageResponse response2 = new MessageResponse( "" );
    final MessageResponse response3 = new MessageResponse( "" );

    response2.recordChangeSet( ChangeSetMessage.create( null, null, null, null, null ), null );
    response3.recordChangeSet( ChangeSetMessage.create( null, null, null, null, null ), null );

    connection.setCurrentMessageResponse( response2 );

    assertEquals( connection.getPendingResponses().size(), 0 );

    connection.queueCurrentResponse();

    assertEquals( connection.getPendingResponses().size(), 1 );
    assertEquals( connection.getPendingResponses().get( 0 ), response2 );
    assertNull( connection.getCurrentMessageResponse() );

    connection.setCurrentMessageResponse( response3 );

    connection.queueCurrentResponse();

    assertEquals( connection.getPendingResponses().size(), 2 );
    assertEquals( connection.getPendingResponses().get( 0 ), response2 );
    assertEquals( connection.getPendingResponses().get( 1 ), response3 );
    assertNull( connection.getCurrentMessageResponse() );
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
    final Connection connection = createConnection();
    final Connector connector = connection.getConnector();
    final String requestName = ValueUtil.randomString();

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final Request request = connection.newRequest( requestName );
    final RequestEntry entry = request.getEntry();

    assertEquals( entry.getName(), requestName );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( RequestStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getRequestId(), request.getRequestId() );
      assertEquals( e.getName(), requestName );
    } );

    assertEquals( connection.getRequest( request.getRequestId() ), entry );
    assertEquals( connection.getRequests().get( request.getRequestId() ), entry );
    assertNull( connection.getRequest( ValueUtil.randomInt() ) );

    connection.removeRequest( request.getRequestId(), false );

    assertNull( connection.getRequest( request.getRequestId() ) );
  }

  @Test
  public void removeRequestWhenNoRequest()
  {
    final Connection connection = createConnection();

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> connection.removeRequest( 789, false ) );
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

    request1.markAsInProgress();
    request2.markAsInProgress();

    assertTrue( request1.isInProgress() );
    assertTrue( request1.isInProgress() );
    assertEquals( connection.getCurrentAreaOfInterestRequests().size(), 2 );

    connection.completeAreaOfInterestRequest();

    assertFalse( request1.isInProgress() );
    assertFalse( request1.isInProgress() );
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
  public void completeRequest()
  {
    final Connection connection = createConnection();
    final Request request = connection.newRequest( ValueUtil.randomString() );
    final RequestEntry entry = request.getEntry();
    final SafeProcedure action = mock( SafeProcedure.class );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    entry.setNormalCompletion( false );

    connection.completeRequest( entry, action );

    verify( action ).call();
    assertNull( entry.getCompletionAction() );
    assertNull( connection.getRequest( request.getRequestId() ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( RequestCompletedEvent.class, ev -> {
      assertEquals( ev.getSchemaId(), 1 );
      assertEquals( ev.getRequestId(), request.getRequestId() );
      assertEquals( ev.getName(), entry.getName() );
      assertFalse( ev.isNormalCompletion() );
      assertFalse( ev.isExpectingResults() );
      assertFalse( ev.haveResultsArrived() );
    } );
  }

  @Test
  public void completeRequest_expectingResults()
  {
    final Connection connection = createConnection();
    final Request request = connection.newRequest( ValueUtil.randomString() );
    final RequestEntry entry = request.getEntry();
    final SafeProcedure action = mock( SafeProcedure.class );

    entry.setNormalCompletion( true );
    entry.setExpectingResults( true );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    connection.completeRequest( entry, action );

    verify( action, never() ).call();
    assertEquals( entry.getCompletionAction(), action );
    assertNotNull( connection.getRequest( request.getRequestId() ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( RequestCompletedEvent.class, ev -> {
      assertEquals( ev.getSchemaId(), 1 );
      assertEquals( ev.getRequestId(), request.getRequestId() );
      assertEquals( ev.getName(), entry.getName() );
      assertTrue( ev.isNormalCompletion() );
      assertTrue( ev.isExpectingResults() );
      assertFalse( ev.haveResultsArrived() );
    } );
  }

  @Test
  public void completeRequest_resultsArrived()
  {
    final Connection connection = createConnection();
    final Request request = connection.newRequest( ValueUtil.randomString() );

    // Remove request as it will be removed when the response arrived and was processed
    connection.removeRequest( request.getRequestId(), false );
    final RequestEntry entry = request.getEntry();
    final SafeProcedure action = mock( SafeProcedure.class );

    entry.setNormalCompletion( true );
    entry.setExpectingResults( true );
    entry.markResultsAsArrived();

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    connection.completeRequest( entry, action );

    verify( action ).call();
    assertNull( entry.getCompletionAction() );
    assertNull( connection.getRequest( request.getRequestId() ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( RequestCompletedEvent.class, ev -> {
      assertEquals( ev.getSchemaId(), 1 );
      assertEquals( ev.getRequestId(), request.getRequestId() );
      assertEquals( ev.getName(), entry.getName() );
      assertTrue( ev.isNormalCompletion() );
      assertTrue( ev.isExpectingResults() );
      assertTrue( ev.haveResultsArrived() );
    } );
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

    cacheService.store( addressA.getCacheKey(), ValueUtil.randomString(), ValueUtil.randomString() );

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
    throws Exception
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
    throws Exception
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
    throws Exception
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
    throws Exception
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
    throws Exception
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
    throws Exception
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
