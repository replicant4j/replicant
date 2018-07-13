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
import replicant.messages.ChangeSet;
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
    final Connector connector = createConnector();
    final String connectionId = ValueUtil.randomString();
    final Connection connection = new Connection( connector, connectionId );

    assertEquals( connection.isDisposed(), false );
    assertEquals( connection.getConnectionId(), connectionId );
    assertEquals( connection.getLastRxSequence(), 0 );
    assertEquals( connection.getLastTxRequestId(), 0 );
    assertEquals( connection.getCurrentMessageResponse(), null );
    assertNotNull( connection.getTransportContext() );
    assertThrows( connection::ensureCurrentMessageResponse );

    final MessageResponse response = new MessageResponse( "" );
    connection.setCurrentMessageResponse( response );

    assertEquals( connection.getCurrentMessageResponse(), response );
    assertEquals( connection.ensureCurrentMessageResponse(), response );

    connection.setLastRxSequence( 2 );
    assertEquals( connection.getLastRxSequence(), 2 );
  }

  @Test
  public void dispose()
  {
    final Connector connector = createConnector();
    final Connection connection = new Connection( connector, ValueUtil.randomString() );

    assertEquals( connection.isDisposed(), false );
    assertEquals( Disposable.isDisposed( connection.getTransportContext() ), false );

    connection.dispose();

    assertEquals( connection.isDisposed(), true );
    assertEquals( Disposable.isDisposed( connection.getTransportContext() ), true );
  }

  @Test
  public void selectNextMessageResponse_noMessages()
  {
    final Connector connector = createConnector();
    final String connectionId = ValueUtil.randomString();
    final Connection connection = new Connection( connector, connectionId );

    assertNull( connection.getCurrentMessageResponse() );

    final boolean selectedMessage = connection.selectNextMessageResponse();

    assertEquals( selectedMessage, false );
    assertNull( connection.getCurrentMessageResponse() );
  }

  @Test
  public void selectNextMessageResponse_unparsedOobMessage()
  {
    final Connector connector = createConnector();
    final String connectionId = ValueUtil.randomString();
    final Connection connection = new Connection( connector, connectionId );

    // Add an unparsed message to queue
    final SafeProcedure oobCompletionAction = () -> {
    };
    connection.enqueueOutOfBandResponse( "", oobCompletionAction );

    assertNull( connection.getCurrentMessageResponse() );
    assertEquals( connection.getOutOfBandResponses().size(), 1 );

    final boolean selectedMessage = connection.selectNextMessageResponse();

    assertEquals( selectedMessage, true );
    assertEquals( connection.getOutOfBandResponses().size(), 0 );
    assertNotNull( connection.getCurrentMessageResponse() );
  }

  @Test
  public void selectNextMessageResponse_unparsedMessage()
  {
    final Connector connector = createConnector();
    final String connectionId = ValueUtil.randomString();
    final Connection connection = new Connection( connector, connectionId );

    // Add an unparsed message to queue
    connection.enqueueResponse( "" );

    assertNull( connection.getCurrentMessageResponse() );
    assertEquals( connection.getUnparsedResponses().size(), 1 );

    final boolean selectedMessage = connection.selectNextMessageResponse();

    assertEquals( selectedMessage, true );
    assertNotNull( connection.getCurrentMessageResponse() );
    assertEquals( connection.getUnparsedResponses().size(), 0 );
  }

  @Test
  public void selectNextMessageResponse_parsedMessage()
  {
    final Connector connector = createConnector();
    final String connectionId = ValueUtil.randomString();
    final Connection connection = new Connection( connector, connectionId );

    final MessageResponse response1 = new MessageResponse( "" );
    response1.recordChangeSet( ChangeSet.create( 1, null, null ), null );

    // Add a "parsed" message with sequence indicating it is next
    connection.injectPendingResponses( response1 );

    assertNull( connection.getCurrentMessageResponse() );
    assertEquals( connection.getPendingResponses().size(), 1 );

    final boolean selectedMessage = connection.selectNextMessageResponse();

    assertEquals( selectedMessage, true );
    final MessageResponse currentMessageResponse = connection.getCurrentMessageResponse();
    assertNotNull( currentMessageResponse );
    assertEquals( currentMessageResponse.getChangeSet().getSequence(), 1 );
    assertEquals( connection.getPendingResponses().size(), 0 );
  }

  @Test
  public void selectNextMessageResponse_parsedOutOfSequenceMessage()
  {
    final Connector connector = createConnector();
    final String connectionId = ValueUtil.randomString();
    final Connection connection = new Connection( connector, connectionId );

    final MessageResponse response1 = new MessageResponse( "" );
    response1.recordChangeSet( ChangeSet.create( 22, null, null ), null );

    // Add a "parsed" message with sequence indicating it is next
    connection.injectPendingResponses( response1 );

    assertNull( connection.getCurrentMessageResponse() );
    assertEquals( connection.getPendingResponses().size(), 1 );

    final boolean selectedMessage = connection.selectNextMessageResponse();

    assertEquals( selectedMessage, false );
    assertNull( connection.getCurrentMessageResponse() );
    assertEquals( connection.getPendingResponses().size(), 1 );
  }

  @Test
  public void selectNextMessageResponse_parsedOutOfSequenceOutOfBandMessage()
  {
    final Connector connector = createConnector();
    final String connectionId = ValueUtil.randomString();
    final Connection connection = new Connection( connector, connectionId );

    final SafeProcedure oobCompletionAction = () -> {
    };
    final MessageResponse response1 = new MessageResponse( "", oobCompletionAction );
    response1.recordChangeSet( ChangeSet.create( 324, null, null ), null );

    // Add a "parsed" message with sequence indicating it is next
    connection.injectPendingResponses( response1 );

    assertNull( connection.getCurrentMessageResponse() );
    assertEquals( connection.getPendingResponses().size(), 1 );

    final boolean selectedMessage = connection.selectNextMessageResponse();

    assertEquals( selectedMessage, true );
    final MessageResponse currentMessageResponse = connection.getCurrentMessageResponse();
    assertNotNull( currentMessageResponse );
    assertEquals( currentMessageResponse.isOob(), true );
    assertEquals( currentMessageResponse.getChangeSet().getSequence(), 324 );
    assertEquals( connection.getPendingResponses().size(), 0 );
  }

  @Test
  public void queueCurrentResponse()
  {
    final Connector connector = createConnector();
    final String connectionId = ValueUtil.randomString();
    final Connection connection = new Connection( connector, connectionId );

    final MessageResponse response1 = new MessageResponse( "" );
    final MessageResponse response2 = new MessageResponse( "" );
    final MessageResponse response3 = new MessageResponse( "" );

    response1.recordChangeSet( ChangeSet.create( 1, null, null ), null );
    response2.recordChangeSet( ChangeSet.create( 2, null, null ), null );
    response3.recordChangeSet( ChangeSet.create( 3, null, null ), null );

    connection.setCurrentMessageResponse( response2 );

    assertEquals( connection.getPendingResponses().size(), 0 );

    connection.queueCurrentResponse();

    assertEquals( connection.getPendingResponses().size(), 1 );
    assertEquals( connection.getPendingResponses().get( 0 ), response2 );
    assertEquals( connection.getCurrentMessageResponse(), null );

    // This should be added to end of queue as it has later sequence
    connection.setCurrentMessageResponse( response3 );

    connection.queueCurrentResponse();

    assertEquals( connection.getPendingResponses().size(), 2 );
    assertEquals( connection.getPendingResponses().get( 0 ), response2 );
    assertEquals( connection.getPendingResponses().get( 1 ), response3 );
    assertEquals( connection.getCurrentMessageResponse(), null );

    // This should be added to start of queue as it has earlier sequence
    connection.setCurrentMessageResponse( response1 );

    connection.queueCurrentResponse();

    assertEquals( connection.getPendingResponses().size(), 3 );
    assertEquals( connection.getPendingResponses().get( 0 ), response1 );
    assertEquals( connection.getPendingResponses().get( 1 ), response2 );
    assertEquals( connection.getPendingResponses().get( 2 ), response3 );
    assertEquals( connection.getCurrentMessageResponse(), null );
  }

  @Test
  public void requestSubscribe()
  {
    final Connector connector = createConnector();
    final String connectionId = ValueUtil.randomString();
    final Connection connection = new Connection( connector, connectionId );

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
    final Connector connector = createConnector();
    final String connectionId = ValueUtil.randomString();
    final Connection connection = new Connection( connector, connectionId );

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
    final Connector connector = createConnector();
    final String connectionId = ValueUtil.randomString();
    final Connection connection = new Connection( connector, connectionId );

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
    assertEquals( request1.getFilter(), null );
    assertEquals( request1.getType(), AreaOfInterestRequest.Type.REMOVE );

    assertEquals( request2.getAddress(), address2 );
    assertEquals( request2.getFilter(), null );
    assertEquals( request2.getType(), AreaOfInterestRequest.Type.REMOVE );
  }

  @Test
  public void enqueueResponse()
  {
    final Connector connector = createConnector();
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
    final Connector connector = createConnector();
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
    final SystemSchema schema = newSchema( 1 );
    final Connector connector = createConnector( schema );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    final String requestName = ValueUtil.randomString();

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final int lastTxRequestId = connection.getLastTxRequestId();
    final Request request = connection.newRequest( requestName );
    final RequestEntry entry = request.getEntry();

    assertEquals( entry.getName(), requestName );
    assertEquals( entry.getRequestId(), lastTxRequestId + 1 );
    assertEquals( connection.getLastTxRequestId(), lastTxRequestId + 1 );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( RequestStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), schema.getId() );
      assertEquals( e.getRequestId(), request.getRequestId() );
      assertEquals( e.getName(), requestName );
    } );

    assertEquals( connection.getRequest( request.getRequestId() ), entry );
    assertEquals( connection.getRequests().get( request.getRequestId() ), entry );
    assertEquals( connection.getRequest( ValueUtil.randomInt() ), null );

    connection.removeRequest( request.getRequestId() );

    assertEquals( connection.getRequest( request.getRequestId() ), null );
  }

  @Test
  public void removeRequestWhenNoRequest()
  {
    final Connection connection = new Connection( createConnector(), "123" );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> connection.removeRequest( 789 ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0067: Attempted to remove request with id 789 from connection with id '123' but no such request exists." );
  }

  @Test
  public void completeAreaOfInterestRequest()
  {
    final Connection connection = new Connection( createConnector(), ValueUtil.randomString() );

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
    final Connection connection = new Connection( createConnector(), ValueUtil.randomString() );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, connection::completeAreaOfInterestRequest );
    assertEquals( exception.getMessage(),
                  "Replicant-0023: Connection.completeAreaOfInterestRequest() invoked when there is no current AreaOfInterest requests." );
  }

  @Test
  public void completeRequest()
  {
    final Connection connection = new Connection( createConnector( newSchema( 1 ) ), ValueUtil.randomString() );
    final Request request = connection.newRequest( ValueUtil.randomString() );
    final RequestEntry entry = request.getEntry();
    final SafeProcedure action = mock( SafeProcedure.class );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    entry.setNormalCompletion( false );

    connection.completeRequest( entry, action );

    verify( action ).call();
    assertEquals( entry.getCompletionAction(), null );
    assertNull( connection.getRequest( request.getRequestId() ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( RequestCompletedEvent.class, ev -> {
      assertEquals( ev.getSchemaId(), 1 );
      assertEquals( ev.getRequestId(), request.getRequestId() );
      assertEquals( ev.getName(), entry.getName() );
      assertEquals( ev.isNormalCompletion(), false );
      assertEquals( ev.isExpectingResults(), false );
      assertEquals( ev.haveResultsArrived(), false );
    } );
  }

  @Test
  public void completeRequest_expectingResults()
  {
    final Connection connection = new Connection( createConnector( newSchema( 1 ) ), ValueUtil.randomString() );
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
      assertEquals( ev.isNormalCompletion(), true );
      assertEquals( ev.isExpectingResults(), true );
      assertEquals( ev.haveResultsArrived(), false );
    } );
  }

  @Test
  public void completeRequest_resultsArrived()
  {
    final Connection connection = new Connection( createConnector( newSchema( 1 ) ), ValueUtil.randomString() );
    final Request request = connection.newRequest( ValueUtil.randomString() );
    final RequestEntry entry = request.getEntry();
    final SafeProcedure action = mock( SafeProcedure.class );

    entry.setNormalCompletion( true );
    entry.setExpectingResults( true );
    entry.markResultsAsArrived();

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    connection.completeRequest( entry, action );

    verify( action ).call();
    assertEquals( entry.getCompletionAction(), null );
    assertNull( connection.getRequest( request.getRequestId() ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( RequestCompletedEvent.class, ev -> {
      assertEquals( ev.getSchemaId(), 1 );
      assertEquals( ev.getRequestId(), request.getRequestId() );
      assertEquals( ev.getName(), entry.getName() );
      assertEquals( ev.isNormalCompletion(), true );
      assertEquals( ev.isExpectingResults(), true );
      assertEquals( ev.haveResultsArrived(), true );
    } );
  }

  @Test
  public void canGroupRequests()
  {
    final Connection connection = new Connection( createConnector( newSchema( 1 ) ), ValueUtil.randomString() );

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
    final Connection connection = new Connection( createConnector( newSchema( 1 ) ), ValueUtil.randomString() );

    final ChannelAddress addressA = new ChannelAddress( 1, 1, 1 );
    final ChannelAddress addressB = new ChannelAddress( 1, 1, 2 );

    final AreaOfInterestRequest requestA = new AreaOfInterestRequest( addressA, AreaOfInterestRequest.Type.ADD, null );
    final AreaOfInterestRequest requestB = new AreaOfInterestRequest( addressB, AreaOfInterestRequest.Type.ADD, null );

    assertEquals( connection.canGroupRequests( requestA, requestB ), true );
    assertEquals( connection.canGroupRequests( requestB, requestA ), true );

    final TestCacheService cacheService = new TestCacheService();
    Replicant.context().setCacheService( cacheService );

    cacheService.store( requestA.getCacheKey(), ValueUtil.randomString(), ValueUtil.randomString() );

    assertEquals( connection.canGroupRequests( requestA, requestB ), false );
    assertEquals( connection.canGroupRequests( requestB, requestA ), false );
  }

  @Test
  public void getCurrentAreaOfInterestRequests()
  {
    final Connection connection = new Connection( createConnector( newSchema( 1 ) ), ValueUtil.randomString() );

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
    final Connection connection = new Connection( createConnector( newSchema( 1 ) ), ValueUtil.randomString() );

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
    final Connection connection = new Connection( createConnector( newSchema( 1 ) ), ValueUtil.randomString() );

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
    final Connection connection = new Connection( createConnector( newSchema( 1 ) ), ValueUtil.randomString() );

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
    final Connection connection = new Connection( createConnector( newSchema( 1 ) ), ValueUtil.randomString() );

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
    final Connection connection = new Connection( createConnector( newSchema( 1 ) ), ValueUtil.randomString() );

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
    final Connection connection = new Connection( createConnector( newSchema( 1 ) ), ValueUtil.randomString() );

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
