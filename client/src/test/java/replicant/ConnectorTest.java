package replicant;

import arez.Disposable;
import arez.component.Linkable;
import arez.component.Verifiable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.testng.annotations.Test;
import replicant.messages.ChannelChange;
import replicant.messages.EntityChange;
import replicant.messages.EntityChangeData;
import replicant.messages.EntityChangeDataImpl;
import replicant.messages.OkMessage;
import replicant.messages.ServerToClientMessage;
import replicant.messages.UpdateMessage;
import replicant.spy.AreaOfInterestDisposedEvent;
import replicant.spy.AreaOfInterestStatusUpdatedEvent;
import replicant.spy.ConnectFailureEvent;
import replicant.spy.ConnectedEvent;
import replicant.spy.DisconnectFailureEvent;
import replicant.spy.DisconnectedEvent;
import replicant.spy.ExecCompletedEvent;
import replicant.spy.ExecRequestQueuedEvent;
import replicant.spy.ExecStartedEvent;
import replicant.spy.InSyncEvent;
import replicant.spy.MessageProcessFailureEvent;
import replicant.spy.MessageProcessedEvent;
import replicant.spy.MessageReadFailureEvent;
import replicant.spy.OutOfSyncEvent;
import replicant.spy.RestartEvent;
import replicant.spy.SubscribeCompletedEvent;
import replicant.spy.SubscribeRequestQueuedEvent;
import replicant.spy.SubscribeStartedEvent;
import replicant.spy.SubscriptionCreatedEvent;
import replicant.spy.SubscriptionDisposedEvent;
import replicant.spy.SubscriptionUpdateCompletedEvent;
import replicant.spy.SubscriptionUpdateRequestQueuedEvent;
import replicant.spy.SubscriptionUpdateStartedEvent;
import replicant.spy.SyncRequestEvent;
import replicant.spy.UnsubscribeCompletedEvent;
import replicant.spy.UnsubscribeRequestQueuedEvent;
import replicant.spy.UnsubscribeStartedEvent;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

@SuppressWarnings( { "NonJREEmulationClassesInClientCode" } )
public final class ConnectorTest
  extends AbstractReplicantTest
{
  @Test
  public void construct()
  {
    final var schedulerLock = pauseScheduler();
    final var runtime = Replicant.context().getRuntime();

    safeAction( () -> assertEquals( runtime.getConnectors().size(), 0 ) );

    final var schema =
      new SystemSchema( ValueUtil.randomInt(),
                        ValueUtil.randomString(),
                        new ChannelSchema[ 0 ],
                        new EntitySchema[ 0 ] );
    final var connector = createConnector( schema );

    assertEquals( connector.getSchema(), schema );

    safeAction( () -> assertEquals( runtime.getConnectors().size(), 1 ) );

    assertEquals( connector.getReplicantRuntime(), runtime );
    assertTrue( connector.getReplicantContext().getSchemaService().getSchemas().contains( schema ) );

    assertEquals( connector.getState(), ConnectorState.DISCONNECTED );

    schedulerLock.dispose();

    assertEquals( connector.getState(), ConnectorState.CONNECTING );
  }

  @Test
  public void dispose()
  {
    final var runtime = Replicant.context().getRuntime();

    safeAction( () -> assertEquals( runtime.getConnectors().size(), 0 ) );

    final var schema = newSchema();
    final var connector = createConnector( schema );

    safeAction( () -> assertEquals( runtime.getConnectors().size(), 1 ) );
    assertTrue( connector.getReplicantContext().getSchemaService().getSchemas().contains( schema ) );

    Disposable.dispose( connector );

    safeAction( () -> assertEquals( runtime.getConnectors().size(), 0 ) );
    assertFalse( connector.getReplicantContext().getSchemaService().getSchemas().contains( schema ) );
  }

  @Test
  public void testToString()
  {
    final var schema = newSchema();
    final var connector = createConnector( schema );
    assertEquals( connector.toString(), "Connector[" + schema.getName() + "]" );
    ReplicantTestUtil.disableNames();
    assertEquals( connector.toString(), "replicant.Arez_Connector@" + Integer.toHexString( connector.hashCode() ) );
  }

  @Test
  public void setConnection_whenConnectorProcessingMessage()
  {
    final var connector = createConnector();

    final var connection = newConnection( connector );

    pauseScheduler();
    connector.pauseMessageScheduler();

    setCurrentMessageResponse( connection, UpdateMessage.create( null, null, null, null, null, null ) );

    final var address = new ChannelAddress( connector.getSchema().getId(), 0 );
    final var subscription = createSubscription( address, null, true );

    connector.onConnection( ValueUtil.randomString() );

    // Connection not swapped yet but will do one MessageProcess completes
    assertFalse( Disposable.isDisposed( subscription ) );
    assertEquals( connector.getConnection(), connection );
    assertNotNull( connector.getPostMessageResponseAction() );
  }

  @Test
  public void setConnection_whenExistingConnection()
  {
    final var connector = createConnector();

    final var connection = newConnection( connector );

    pauseScheduler();
    connector.pauseMessageScheduler();

    assertEquals( connector.getConnection(), connection );

    final var newConnectionId = ValueUtil.randomString();
    connector.onConnection( newConnectionId );

    assertEquals( connector.ensureConnection().getConnectionId(), newConnectionId );

    assertTrue( connector.ensureConnection().getPendingResponses().isEmpty() );
  }

  @Test
  public void connect()
  {
    pauseScheduler();

    final var connector = createConnector();
    assertEquals( connector.getState(), ConnectorState.DISCONNECTED );

    safeAction( connector::connect );

    verify( connector.getTransport() ).requestConnect( any( TransportContext.class ) );

    assertEquals( connector.getState(), ConnectorState.CONNECTING );
  }

  @Test
  public void connect_causesError()
  {
    pauseScheduler();

    final var connector = createConnector();
    assertEquals( connector.getState(), ConnectorState.DISCONNECTED );

    reset( connector.getTransport() );

    final var exception = new IllegalStateException();
    doAnswer( i -> {
      throw exception;
    } ).when( connector.getTransport() ).requestConnect( any( TransportContext.class ) );

    final var actual =
      expectThrows( IllegalStateException.class, () -> safeAction( connector::connect ) );

    assertEquals( actual, exception );
    assertEquals( connector.getState(), ConnectorState.ERROR );

    verify( connector.getTransport() ).unbind();
  }

  @Test
  public void disconnect()
  {
    pauseScheduler();

    final var connector = createConnector();
    newConnection( connector );

    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    safeAction( connector::disconnect );

    verify( connector.getTransport() ).requestDisconnect();

    assertEquals( connector.getState(), ConnectorState.DISCONNECTING );

    verify( connector.getTransport(), never() ).unbind();
  }

  @Test
  public void disconnect_causesError()
  {
    pauseScheduler();

    final var connector = createConnector();
    newConnection( connector );
    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    reset( connector.getTransport() );

    final var exception = new IllegalStateException();
    doAnswer( i -> {
      throw exception;
    } ).when( connector.getTransport() ).requestDisconnect();

    final var actual =
      expectThrows( IllegalStateException.class, () -> safeAction( connector::disconnect ) );

    assertEquals( actual, exception );

    assertEquals( connector.getState(), ConnectorState.ERROR );
    verify( connector.getTransport() ).unbind();
  }

  @Test
  public void onDisconnected()
  {
    final var connector = createConnector();

    newConnection( connector );

    safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    assertEquals( connector.getReplicantRuntime().getState(), RuntimeState.CONNECTING );

    // Pause scheduler so runtime does not try to update state
    pauseScheduler();

    reset( connector.getTransport() );

    connector.onDisconnected();

    assertEquals( connector.getState(), ConnectorState.DISCONNECTED );
    assertEquals( connector.getReplicantRuntime().getState(), RuntimeState.DISCONNECTED );

    verify( connector.getTransport() ).unbind();
  }

  @Test
  public void onDisconnected_generatesSpyMessage()
  {
    final var connector = createConnector();

    safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    final var handler = registerTestSpyEventHandler();

    // Pause scheduler so runtime does not try to update state
    pauseScheduler();

    connector.onDisconnected();

    handler.assertEventCount( 1 );
    handler.assertNextEvent( DisconnectedEvent.class,
                             e -> assertEquals( e.getSchemaId(), connector.getSchema().getId() ) );
  }

  @Test
  public void onDisconnectFailure()
  {
    final var connector = createConnector();

    safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    assertEquals( connector.getReplicantRuntime().getState(), RuntimeState.CONNECTING );

    // Pause scheduler so runtime does not try to update state
    pauseScheduler();

    connector.onDisconnectFailure();

    assertEquals( connector.getState(), ConnectorState.ERROR );
    assertEquals( connector.getReplicantRuntime().getState(), RuntimeState.ERROR );
  }

  @Test
  public void onDisconnectFailure_generatesSpyMessage()
  {
    final var connector = createConnector();

    safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    final var handler = registerTestSpyEventHandler();

    // Pause scheduler so runtime does not try to update state
    pauseScheduler();

    connector.onDisconnectFailure();

    handler.assertEventCount( 1 );
    handler.assertNextEvent( DisconnectFailureEvent.class,
                             e -> assertEquals( e.getSchemaId(), connector.getSchema().getId() ) );
  }

  @Test
  public void onConnected()
    throws Exception
  {
    final var connection = createConnection();
    final var connector = connection.getConnector();

    safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    assertEquals( connector.getReplicantRuntime().getState(), RuntimeState.CONNECTING );

    // Pause scheduler so runtime does not try to update state
    pauseScheduler();

    final var field = Connector.class.getDeclaredField( "_connection" );
    field.setAccessible( true );
    field.set( connector, connection );

    connector.onConnected();

    assertEquals( connector.getState(), ConnectorState.CONNECTED );
    assertEquals( connector.getReplicantRuntime().getState(), RuntimeState.CONNECTED );
  }

  @Test
  public void onConnected_generatesSpyMessage()
  {
    final var connector = createConnector();
    newConnection( connector );

    safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    final var handler = registerTestSpyEventHandler();

    connector.onConnected();

    handler.assertEventCount( 1 );
    handler.assertNextEvent( ConnectedEvent.class,
                             e -> assertEquals( e.getSchemaId(), connector.getSchema().getId() ) );
  }

  @Test
  public void onConnectFailure()
  {
    final var connector = createConnector();

    safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    assertEquals( connector.getReplicantRuntime().getState(), RuntimeState.CONNECTING );

    // Pause scheduler so runtime does not try to update state
    pauseScheduler();

    connector.onConnectFailure();

    assertEquals( connector.getState(), ConnectorState.ERROR );
    assertEquals( connector.getReplicantRuntime().getState(), RuntimeState.ERROR );
  }

  @Test
  public void onConnectFailure_generatesSpyMessage()
  {
    final var connector = createConnector();

    safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    final var handler = registerTestSpyEventHandler();

    // Pause scheduler so runtime does not try to update state
    pauseScheduler();

    connector.onConnectFailure();

    handler.assertEventCount( 1 );
    handler.assertNextEvent( ConnectFailureEvent.class,
                             e -> assertEquals( e.getSchemaId(), connector.getSchema().getId() ) );
  }

  @Test
  public void onMessageReceived()
  {
    final var connector = createConnector();
    final var connection = newConnection( connector );

    pauseScheduler();
    connector.pauseMessageScheduler();

    assertEquals( connection.getPendingResponses().size(), 0 );
    assertFalse( connector.isSchedulerActive() );

    final var message = UpdateMessage.create( null, null, null, null, null, null );
    connector.onMessageReceived( message );

    assertEquals( connection.getPendingResponses().size(), 1 );
    assertEquals( connection.getPendingResponses().get( 0 ).getMessage(), message );
  }

  @Test
  public void onMessageProcessed()
  {
    final var connector = createConnector();

    safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    final var handler = registerTestSpyEventHandler();

    final var response =
      new MessageResponse( 1, UpdateMessage.create( null, null, null, null, null, null ), null );
    connector.onMessageProcessed( response );

    handler.assertEventCount( 1 );

    handler.assertNextEvent( MessageProcessedEvent.class,
                             e -> assertEquals( e.getSchemaId(), connector.getSchema().getId() ) );
  }

  @Test
  public void onMessageProcessFailure()
  {
    final var connector = createConnector();
    newConnection( connector );

    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    final var error = new Throwable();

    // Pause scheduler so runtime does not try to update state
    pauseScheduler();

    connector.onMessageProcessFailure( error );

    assertEquals( connector.getState(), ConnectorState.DISCONNECTING );
  }

  @Test
  public void onMessageProcessFailure_generatesSpyMessage()
  {
    final var connector = createConnector();

    safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    final var handler = registerTestSpyEventHandler();

    final var error = new Throwable();

    connector.onMessageProcessFailure( error );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( MessageProcessFailureEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getError(), error );
    } );
  }

  @Test
  public void disconnectIfPossible()
  {
    final var connector = createConnector();
    newConnection( connector );

    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    safeAction( connector::disconnectIfPossible );

    assertEquals( connector.getState(), ConnectorState.DISCONNECTING );
  }

  @Test
  public void disconnectIfPossible_noActionAsConnecting()
  {
    final var connector = createConnector();

    safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    final var handler = registerTestSpyEventHandler();

    connector.disconnectIfPossible();

    handler.assertEventCount( 0 );

    assertEquals( connector.getState(), ConnectorState.CONNECTING );
  }

  @Test
  public void disconnectIfPossible_generatesSpyEvent()
  {
    final var connector = createConnector();
    newConnection( connector );

    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    final var handler = registerTestSpyEventHandler();

    safeAction( connector::disconnectIfPossible );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( RestartEvent.class, e -> assertEquals( e.getSchemaId(), connector.getSchema().getId() ) );
  }

  @Test
  public void onMessageReadFailure()
  {
    final var connector = createConnector();
    newConnection( connector );

    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    // Pause scheduler so runtime does not try to update state
    pauseScheduler();

    connector.onMessageReadFailure();

    assertEquals( connector.getState(), ConnectorState.DISCONNECTING );
  }

  @Test
  public void onMessageReadFailure_generatesSpyMessage()
  {
    final var connector = createConnector();

    safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    final var handler = registerTestSpyEventHandler();

    connector.onMessageReadFailure();

    handler.assertEventCount( 1 );
    handler.assertNextEvent( MessageReadFailureEvent.class,
                             e -> assertEquals( e.getSchemaId(), connector.getSchema().getId() ) );
  }

  @Test
  public void onSubscribeStarted()
  {
    final var connector = createConnector();

    final var address = new ChannelAddress( 1, 0 );
    final var areaOfInterest =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED );
    safeAction( () -> assertNull( areaOfInterest.getSubscription() ) );
    safeAction( () -> assertNull( areaOfInterest.getError() ) );

    final var handler = registerTestSpyEventHandler();

    connector.onSubscribeStarted( address );

    assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.LOADING );
    safeAction( () -> assertNull( areaOfInterest.getSubscription() ) );
    safeAction( () -> assertNull( areaOfInterest.getError() ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( SubscribeStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @Test
  public void onSubscribeCompleted()
  {
    final var connector = createConnector();

    final var address = new ChannelAddress( 1, 0 );
    final var areaOfInterest =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED );
    safeAction( () -> assertNull( areaOfInterest.getSubscription() ) );
    safeAction( () -> assertNull( areaOfInterest.getError() ) );

    final var subscription = createSubscription( address, null, true );

    final var handler = registerTestSpyEventHandler();

    connector.onSubscribeCompleted( address );

    assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.LOADED );
    safeAction( () -> assertEquals( areaOfInterest.getSubscription(), subscription ) );
    safeAction( () -> assertNull( areaOfInterest.getError() ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( SubscribeCompletedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @Test
  public void onSubscribeCompleted_DeletedSubscription()
  {
    final var connector = createConnector();

    final var address = new ChannelAddress( 1, 0 );
    final var areaOfInterest =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    safeAction( () -> areaOfInterest.setStatus( AreaOfInterest.Status.DELETED ) );

    createSubscription( address, null, true );

    final var handler = registerTestSpyEventHandler();

    connector.onSubscribeCompleted( address );

    assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.DELETED );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscribeCompletedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @Test
  public void onUnsubscribeStarted()
  {
    final var connector = createConnector();

    final var address = new ChannelAddress( 1, 0 );
    final var areaOfInterest =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED );
    safeAction( () -> assertNull( areaOfInterest.getSubscription() ) );
    safeAction( () -> assertNull( areaOfInterest.getError() ) );

    final var subscription = createSubscription( address, null, true );

    final var handler = registerTestSpyEventHandler();

    connector.onUnsubscribeStarted( address );

    assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.UNLOADING );
    safeAction( () -> assertEquals( areaOfInterest.getSubscription(), subscription ) );
    safeAction( () -> assertNull( areaOfInterest.getError() ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( UnsubscribeStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @Test
  public void onUnsubscribeCompleted()
  {
    final var connector = createConnector();

    final var address = new ChannelAddress( 1, 0 );
    final var areaOfInterest =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED );
    safeAction( () -> assertNull( areaOfInterest.getSubscription() ) );
    safeAction( () -> assertNull( areaOfInterest.getError() ) );

    final var handler = registerTestSpyEventHandler();

    connector.onUnsubscribeCompleted( address );

    assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.UNLOADED );
    safeAction( () -> assertNull( areaOfInterest.getSubscription() ) );
    safeAction( () -> assertNull( areaOfInterest.getError() ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( UnsubscribeCompletedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @Test
  public void onSubscriptionUpdateStarted()
  {
    final var connector = createConnector();

    final var address = new ChannelAddress( 1, 0 );
    final var areaOfInterest =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED );
    safeAction( () -> assertNull( areaOfInterest.getSubscription() ) );
    safeAction( () -> assertNull( areaOfInterest.getError() ) );

    final var subscription = createSubscription( address, null, true );

    final var handler = registerTestSpyEventHandler();

    connector.onSubscriptionUpdateStarted( address );

    assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.UPDATING );
    safeAction( () -> assertEquals( areaOfInterest.getSubscription(), subscription ) );
    safeAction( () -> assertNull( areaOfInterest.getError() ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( SubscriptionUpdateStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @Test
  public void onSubscriptionUpdateCompleted()
  {
    final var connector = createConnector();

    final var address = new ChannelAddress( 1, 0 );
    final var areaOfInterest =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED );
    safeAction( () -> assertNull( areaOfInterest.getSubscription() ) );
    safeAction( () -> assertNull( areaOfInterest.getError() ) );

    final var subscription = createSubscription( address, null, true );

    final var handler = registerTestSpyEventHandler();

    connector.onSubscriptionUpdateCompleted( address );

    assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.UPDATED );
    safeAction( () -> assertEquals( areaOfInterest.getSubscription(), subscription ) );
    safeAction( () -> assertNull( areaOfInterest.getError() ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( SubscriptionUpdateCompletedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @Test
  public void areaOfInterestRequestPendingQueries()
  {
    final var connector = createConnector();

    final var address = new ChannelAddress( 1, 0 );
    final var filter = ValueUtil.randomString();

    assertFalse( connector.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.ADD, address, filter ) );
    assertEquals( connector.lastIndexOfPendingAreaOfInterestRequest( AreaOfInterestRequest.Type.ADD, address, filter ),
                  -1 );

    final var connection = newConnection( connector );

    assertFalse( connector.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.ADD, address, filter ) );
    assertEquals( connector.lastIndexOfPendingAreaOfInterestRequest( AreaOfInterestRequest.Type.ADD, address, filter ),
                  -1 );

    connection.requestSubscribe( address, filter );

    assertTrue( connector.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.ADD, address, filter ) );
    assertEquals( connector.lastIndexOfPendingAreaOfInterestRequest( AreaOfInterestRequest.Type.ADD, address, filter ),
                  1 );
  }

  @Test
  public void connection()
  {
    final var connector = createConnector();

    assertNull( connector.getConnection() );

    final var connection = newConnection( connector );

    final var subscription1 = createSubscription( new ChannelAddress( 1, 0 ), null, true );

    assertEquals( connector.getConnection(), connection );
    assertEquals( connector.ensureConnection(), connection );
    assertFalse( Disposable.isDisposed( subscription1 ) );

    connector.onDisconnection();

    assertNull( connector.getConnection() );
    assertTrue( Disposable.isDisposed( subscription1 ) );
  }

  @Test
  public void ensureConnection_WhenNoConnection()
  {
    final var connector = createConnector();

    final var exception = expectThrows( IllegalStateException.class, connector::ensureConnection );

    assertEquals( exception.getMessage(),
                  "Replicant-0031: Connector.ensureConnection() when no connection is present." );
  }

  @Test
  public void purgeSubscriptions()
  {
    final var connector1 = createConnector( newSchema( 1 ) );
    createConnector( newSchema( 2 ) );

    final var subscription1 = createSubscription( new ChannelAddress( 1, 0 ), null, true );
    final var subscription2 = createSubscription( new ChannelAddress( 1, 1, 2 ), null, true );
    // The next two are from a different Connector
    final var subscription3 = createSubscription( new ChannelAddress( 2, 0, 1 ), null, true );
    final var subscription4 = createSubscription( new ChannelAddress( 2, 0, 2 ), null, true );

    assertFalse( Disposable.isDisposed( subscription1 ) );
    assertFalse( Disposable.isDisposed( subscription2 ) );
    assertFalse( Disposable.isDisposed( subscription3 ) );
    assertFalse( Disposable.isDisposed( subscription4 ) );

    connector1.purgeSubscriptions();

    assertTrue( Disposable.isDisposed( subscription1 ) );
    assertTrue( Disposable.isDisposed( subscription2 ) );
    assertFalse( Disposable.isDisposed( subscription3 ) );
    assertFalse( Disposable.isDisposed( subscription4 ) );
  }

  @Test
  public void progressMessages()
  {
    final var connector = createConnector();
    final var connection = newConnection( connector );

    final var channels = new String[]{ "+0" };
    final var response =
      setCurrentMessageResponse( connection, UpdateMessage.create( null, null, channels, null, null, null ) );
    response.setParsedChannelChanges( Collections.singletonList( ChannelChangeDescriptor.from( 1, channels[ 0 ] ) ) );

    assertNull( connector.getSchedulerLock() );

    connector.resumeMessageScheduler();

    //response needs processing of channel messages

    final var result0 = connector.progressMessages();

    assertTrue( result0 );
    final var schedulerLock0 = connector.getSchedulerLock();
    assertNotNull( schedulerLock0 );

    //response needs worldValidated

    final var result1 = connector.progressMessages();

    assertTrue( result1 );
    assertNull( connector.getSchedulerLock() );
    assertTrue( Disposable.isDisposed( schedulerLock0 ) );

    final var result2 = connector.progressMessages();

    assertTrue( result2 );
    // Current message should be nulled and completed processing now
    assertNull( connection.getCurrentMessageResponse() );

    final var result3 = connector.progressMessages();

    assertFalse( result3 );
    assertNull( connector.getSchedulerLock() );
  }

  @Test
  public void progressMessages_whenConnectionHasBeenDisconnectedInMeantime()
  {
    final var connector = createConnector();
    final var connection = newConnection( connector );

    final var channels = new String[]{ "+0" };
    final var response =
      setCurrentMessageResponse( connection, UpdateMessage.create( null, null, channels, null, null, null ) );
    response.setParsedChannelChanges( Collections.singletonList( ChannelChangeDescriptor.from( 1, channels[ 0 ] ) ) );

    final var callCount = new AtomicInteger();
    connector.setPostMessageResponseAction( callCount::incrementAndGet );

    assertNull( connector.getSchedulerLock() );
    assertEquals( callCount.get(), 0 );

    connector.resumeMessageScheduler();

    assertNull( connector.getSchedulerLock() );
    assertEquals( callCount.get(), 0 );

    assertTrue( connector.progressMessages() );

    assertEquals( callCount.get(), 0 );
    assertNotNull( connector.getSchedulerLock() );

    safeAction( () -> {
      connector.setState( ConnectorState.ERROR );
      connector.setConnection( null );
    } );

    // The rest of the message has been skipped as no connection left
    assertFalse( connector.progressMessages() );

    assertNull( connector.getSchedulerLock() );
    assertEquals( callCount.get(), 1 );
  }

  @Test
  public void progressMessages_withError()
  {
    final var connector = createConnector();
    final var connection = newConnection( connector );

    connection.injectCurrentAreaOfInterestRequest( new AreaOfInterestRequest( new ChannelAddress( 0, 0 ),
                                                                              AreaOfInterestRequest.Type.REMOVE,
                                                                              null ) );
    final var handler = registerTestSpyEventHandler();

    connector.resumeMessageScheduler();

    final var result2 = connector.progressMessages();

    assertFalse( result2 );

    assertNull( connector.getSchedulerLock() );

    handler.assertEventCountAtLeast( 1 );
    handler.assertNextEvent( MessageProcessFailureEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getError().getMessage(),
                    "Replicant-0046: Request to unsubscribe from channel at address 0.0 but not subscribed to channel." );
    } );
  }

  @Test
  public void requestSubscribe()
  {
    final var connector = createConnector();
    newConnection( connector );
    connector.pauseMessageScheduler();

    final var address = new ChannelAddress( 1, 0 );

    assertFalse( connector.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.ADD, address, null ) );

    final var handler = registerTestSpyEventHandler();

    connector.requestSubscribe( address, null );

    assertTrue( connector.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.ADD, address, null ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscribeRequestQueuedEvent.class, e -> assertEquals( e.getAddress(), address ) );
  }

  @Test
  public void requestSubscribe_requiresFilterInstanceId_forDynamicInstancedChannel()
  {
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         null,
                         ChannelSchema.FilterType.DYNAMIC_INSTANCED,
                         ( f, e ) -> true,
                         false, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1, ValueUtil.randomString(), new ChannelSchema[]{ channelSchema }, new EntitySchema[ 0 ] );
    final var connector = createConnector( schema );
    newConnection( connector );

    final var address = new ChannelAddress( 1, 0 );

    final var exception =
      expectThrows( IllegalStateException.class, () -> connector.requestSubscribe( address, null ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0098: Channel 1.0 requires a filter instance id but none was supplied." );
  }

  @Test
  public void requestSubscribe_requiresFilterInstanceId_forStaticInstancedChannel()
  {
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         null,
                         ChannelSchema.FilterType.STATIC_INSTANCED,
                         null,
                         false, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1, ValueUtil.randomString(), new ChannelSchema[]{ channelSchema }, new EntitySchema[ 0 ] );
    final var connector = createConnector( schema );
    newConnection( connector );

    final var address = new ChannelAddress( 1, 0 );

    final var exception =
      expectThrows( IllegalStateException.class, () -> connector.requestSubscribe( address, null ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0098: Channel 1.0 requires a filter instance id but none was supplied." );
  }

  @Test
  public void requestSubscribe_rejectsFilterInstanceId_forNonInstancedChannel()
  {
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         null,
                         ChannelSchema.FilterType.DYNAMIC,
                         ( f, e ) -> true,
                         false, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1, ValueUtil.randomString(), new ChannelSchema[]{ channelSchema }, new EntitySchema[ 0 ] );
    final var connector = createConnector( schema );
    newConnection( connector );

    final var address = new ChannelAddress( 1, 0, null, "inst" );

    final var exception =
      expectThrows( IllegalStateException.class, () -> connector.requestSubscribe( address, null ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0099: Channel 1.0#inst does not support filter instance ids but one was supplied." );
  }

  @Test
  public void requestSubscribe_dynamicInstanced_withFilterInstanceId()
  {
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         null,
                         ChannelSchema.FilterType.DYNAMIC_INSTANCED,
                         ( f, e ) -> true,
                         false, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1, ValueUtil.randomString(), new ChannelSchema[]{ channelSchema }, new EntitySchema[ 0 ] );
    final var connector = createConnector( schema );
    newConnection( connector );
    connector.pauseMessageScheduler();

    final var address = new ChannelAddress( 1, 0, null, "inst" );

    assertFalse( connector.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.ADD, address, null ) );

    connector.requestSubscribe( address, null );

    assertTrue( connector.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.ADD, address, null ) );
  }

  @Test
  public void requestSubscribe_staticInstanced_withFilterInstanceId()
  {
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         null,
                         ChannelSchema.FilterType.STATIC_INSTANCED,
                         null,
                         false, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1, ValueUtil.randomString(), new ChannelSchema[]{ channelSchema }, new EntitySchema[ 0 ] );
    final var connector = createConnector( schema );
    newConnection( connector );
    connector.pauseMessageScheduler();

    final var address = new ChannelAddress( 1, 0, null, "inst" );

    assertFalse( connector.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.ADD, address, null ) );

    connector.requestSubscribe( address, null );

    assertTrue( connector.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.ADD, address, null ) );
  }

  @Test
  public void requestSubscriptionUpdate()
  {
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         null,
                         ChannelSchema.FilterType.DYNAMIC,
                         ( f, e ) -> true,
                         false, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1, ValueUtil.randomString(), new ChannelSchema[]{ channelSchema }, new EntitySchema[ 0 ] );
    final var connector = createConnector( schema );
    newConnection( connector );
    connector.pauseMessageScheduler();

    final var address = new ChannelAddress( 1, 0 );

    assertFalse( connector.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.UPDATE, address, null ) );

    final var handler = registerTestSpyEventHandler();

    connector.requestSubscriptionUpdate( address, null );

    assertTrue( connector.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.UPDATE, address, null ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscriptionUpdateRequestQueuedEvent.class,
                             e -> assertEquals( e.getAddress(), address ) );
  }

  @Test
  public void requestSubscriptionUpdate_requiresFilterInstanceId_forDynamicInstancedChannel()
  {
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         null,
                         ChannelSchema.FilterType.DYNAMIC_INSTANCED,
                         ( f, e ) -> true,
                         false, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1, ValueUtil.randomString(), new ChannelSchema[]{ channelSchema }, new EntitySchema[ 0 ] );
    final var connector = createConnector( schema );
    newConnection( connector );
    connector.pauseMessageScheduler();

    final var address = new ChannelAddress( 1, 0 );

    final var exception =
      expectThrows( IllegalStateException.class, () -> connector.requestSubscriptionUpdate( address, null ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0098: Channel 1.0 requires a filter instance id but none was supplied." );
  }

  @Test
  public void requestSubscriptionUpdate_ChannelNot_DYNAMIC_Filter()
  {
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         null,
                         ChannelSchema.FilterType.STATIC,
                         null,
                         false, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1, ValueUtil.randomString(), new ChannelSchema[]{ channelSchema }, new EntitySchema[ 0 ] );
    final var connector = createConnector( schema );
    newConnection( connector );
    connector.pauseMessageScheduler();

    final var address = new ChannelAddress( 1, 0 );

    assertFalse( connector.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.UPDATE, address, null ) );

    final var exception =
      expectThrows( IllegalStateException.class, () -> connector.requestSubscriptionUpdate( address, null ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0082: Connector.requestSubscriptionUpdate invoked for channel 1.0 but channel does not have a dynamic filter." );
  }

  @Test
  public void requestUnsubscribe_requiresFilterInstanceId_forDynamicInstancedChannel()
  {
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         null,
                         ChannelSchema.FilterType.DYNAMIC_INSTANCED,
                         ( f, e ) -> true,
                         false, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1, ValueUtil.randomString(), new ChannelSchema[]{ channelSchema }, new EntitySchema[ 0 ] );
    final var connector = createConnector( schema );
    newConnection( connector );

    final var address = new ChannelAddress( 1, 0 );

    final var exception =
      expectThrows( IllegalStateException.class, () -> connector.requestUnsubscribe( address ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0098: Channel 1.0 requires a filter instance id but none was supplied." );
  }

  @Test
  public void requestUnsubscribe_requiresFilterInstanceId_forStaticInstancedChannel()
  {
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         null,
                         ChannelSchema.FilterType.STATIC_INSTANCED,
                         null,
                         false, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1, ValueUtil.randomString(), new ChannelSchema[]{ channelSchema }, new EntitySchema[ 0 ] );
    final var connector = createConnector( schema );
    newConnection( connector );

    final var address = new ChannelAddress( 1, 0 );

    final var exception =
      expectThrows( IllegalStateException.class, () -> connector.requestUnsubscribe( address ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0098: Channel 1.0 requires a filter instance id but none was supplied." );
  }

  @Test
  public void requestUnsubscribe()
    throws Exception
  {
    final var connector = createConnector();
    pauseScheduler();
    connector.pauseMessageScheduler();
    newConnection( connector );

    final var address = new ChannelAddress( 1, 0 );

    createSubscription( address, null, true );

    assertFalse( connector.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.REMOVE, address, null ) );

    final var handler = registerTestSpyEventHandler();

    connector.requestUnsubscribe( address );

    Thread.sleep( 100 );

    assertTrue( connector.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.REMOVE, address, null ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( UnsubscribeRequestQueuedEvent.class,
                             e -> assertEquals( e.getAddress(), address ) );
  }

  @Test
  public void updateSubscriptionForFilteredEntities()
  {
    final var filter = (SubscriptionUpdateEntityFilter<String>) ( f, entity ) -> entity.getId() > 0;
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         null,
                         ChannelSchema.FilterType.DYNAMIC,
                         filter,
                         true, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[ 0 ] );

    final var connector = createConnector( schema );
    newConnection( connector );

    final var address1 = new ChannelAddress( 1, 0, 1 );
    final var address2 = new ChannelAddress( 1, 0, 2 );

    final var subscription1 = createSubscription( address1, ValueUtil.randomString(), true );
    final var subscription2 = createSubscription( address2, ValueUtil.randomString(), true );

    // Use Integer and String as arbitrary types for our entities...
    // Anything with id below 0 will be removed during update ...
    final var entity1 = findOrCreateEntity( Integer.class, -1 );
    final var entity2 = findOrCreateEntity( Integer.class, -2 );
    final var entity3 = findOrCreateEntity( Integer.class, -3 );
    final var entity4 = findOrCreateEntity( Integer.class, -4 );
    final var entity5 = findOrCreateEntity( String.class, 5 );
    final var entity6 = findOrCreateEntity( String.class, 6 );

    safeAction( () -> {
      entity1.linkToSubscription( subscription1 );
      entity2.linkToSubscription( subscription1 );
      entity3.linkToSubscription( subscription1 );
      entity4.linkToSubscription( subscription1 );
      entity5.linkToSubscription( subscription1 );
      entity6.linkToSubscription( subscription1 );

      entity3.linkToSubscription( subscription2 );
      entity4.linkToSubscription( subscription2 );

      assertEquals( subscription1.getEntities().size(), 2 );
      assertEquals( subscription1.findAllEntitiesByType( Integer.class ).size(), 4 );
      assertEquals( subscription1.findAllEntitiesByType( String.class ).size(), 2 );
      assertEquals( subscription2.getEntities().size(), 1 );
      assertEquals( subscription2.findAllEntitiesByType( Integer.class ).size(), 2 );
    } );

    safeAction( () -> connector.updateSubscriptionForFilteredEntities( subscription1 ) );

    safeAction( () -> {
      assertTrue( Disposable.isDisposed( entity1 ) );
      assertTrue( Disposable.isDisposed( entity2 ) );
      assertFalse( Disposable.isDisposed( entity3 ) );
      assertFalse( Disposable.isDisposed( entity4 ) );
      assertFalse( Disposable.isDisposed( entity5 ) );
      assertFalse( Disposable.isDisposed( entity6 ) );

      assertEquals( subscription1.getEntities().size(), 1 );
      assertEquals( subscription1.findAllEntitiesByType( Integer.class ).size(), 0 );
      assertEquals( subscription1.findAllEntitiesByType( String.class ).size(), 2 );
      assertEquals( subscription2.getEntities().size(), 1 );
      assertEquals( subscription2.findAllEntitiesByType( Integer.class ).size(), 2 );
    } );
  }

  @Test
  public void updateSubscriptionForFilteredEntities_badFilterType()
  {
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         null,
                         ChannelSchema.FilterType.STATIC,
                         null,
                         true, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[ 0 ] );

    final var connector = createConnector( schema );
    newConnection( connector );

    final var address1 = new ChannelAddress( 1, 0, 1 );

    final var subscription1 = createSubscription( address1, ValueUtil.randomString(), true );

    final var exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> connector.updateSubscriptionForFilteredEntities( subscription1 ) ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0079: Connector.updateSubscriptionForFilteredEntities invoked for address 1.0.1 but the channel does not have a DYNAMIC filter." );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void processEntityChanges()
  {
    final var schemaId = 1;
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         null,
                         ChannelSchema.FilterType.NONE,
                         null,
                         false, true,
                         Collections.emptyList() );
    final var creator = mock( EntitySchema.Creator.class );
    final var updater = mock( EntitySchema.Updater.class );
    final var entitySchema =
      new EntitySchema( 0, ValueUtil.randomString(), Linkable.class, creator, updater, new ChannelLinkSchema[ 0 ] );
    final var schema =
      new SystemSchema( schemaId,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{ entitySchema } );
    final var connector = createConnector( schema );
    connector.setLinksToProcessPerTick( 1 );

    final var connection = newConnection( connector );

    final var userObject1 = mock( Linkable.class );
    final var userObject2 = mock( Linkable.class );

    // Pause scheduler to avoid converge of subscriptions
    pauseScheduler();

    final var address = new ChannelAddress( connector.getSchema().getId(), 1 );
    final var subscription = createSubscription( address, null, true );

    // This entity is to be updated
    final var entity2 = findOrCreateEntity( Linkable.class, 2 );
    safeAction( () -> entity2.setUserObject( userObject2 ) );
    // It is already subscribed to channel and that should be fine
    safeAction( () -> entity2.linkToSubscription( subscription ) );
    // This entity is to be removed
    final var entity3 = findOrCreateEntity( Linkable.class, 3 );

    final var data1 = mock( EntityChangeData.class );
    final var data2 = mock( EntityChangeData.class );
    final var entityChanges = new EntityChange[]{
      // Update changes
      EntityChange.create( 0, 1, new String[]{ "1" }, data1 ),
      EntityChange.create( 0, 2, new String[]{ "1" }, data2 ),
      // Remove change
      EntityChange.create( 0, 3, new String[]{ "1" } )
    };
    final var response =
      setCurrentMessageResponse( connection, UpdateMessage.create( null, null, null, null, entityChanges, null ) );

    when( creator.createEntity( 1, data1 ) ).thenReturn( userObject1 );

    assertEquals( response.getEntityUpdateCount(), 0 );
    assertEquals( response.getEntityRemoveCount(), 0 );

    connector.setChangesToProcessPerTick( 1 );

    connector.processEntityChanges();

    verify( creator, times( 1 ) ).createEntity( 1, data1 );
    verify( updater, never() ).updateEntity( userObject1, data1 );
    verify( creator, never() ).createEntity( 2, data2 );
    verify( updater, never() ).updateEntity( userObject2, data2 );

    assertEquals( response.getEntityUpdateCount(), 1 );
    assertEquals( response.getEntityRemoveCount(), 0 );

    connector.setChangesToProcessPerTick( 2 );

    connector.processEntityChanges();

    verify( creator, times( 1 ) ).createEntity( 1, data1 );
    verify( updater, never() ).updateEntity( userObject1, data1 );
    verify( creator, never() ).createEntity( 2, data2 );
    verify( updater, times( 1 ) ).updateEntity( userObject2, data2 );

    assertEquals( response.getEntityUpdateCount(), 2 );
    assertEquals( response.getEntityRemoveCount(), 1 );
    assertFalse( Disposable.isDisposed( entity2 ) );
    assertTrue( Disposable.isDisposed( entity3 ) );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void processEntityChanges_withFilterInstanceIdChannel()
  {
    final var schemaId = 1;
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         Linkable.class,
                         ChannelSchema.FilterType.DYNAMIC_INSTANCED,
                         ( f, e ) -> true,
                         false, true,
                         Collections.emptyList() );
    final var creator = mock( EntitySchema.Creator.class );
    final var updater = mock( EntitySchema.Updater.class );
    final var entitySchema =
      new EntitySchema( 0, ValueUtil.randomString(), Linkable.class, creator, updater, new ChannelLinkSchema[ 0 ] );
    final var schema =
      new SystemSchema( schemaId,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{ entitySchema } );
    final var connector = createConnector( schema );
    connector.setLinksToProcessPerTick( 1 );

    final var connection = newConnection( connector );

    // Pause scheduler to avoid converge of subscriptions
    pauseScheduler();

    final var rootId = ValueUtil.randomInt();
    final var address = new ChannelAddress( connector.getSchema().getId(), 0, rootId, "fi" );
    final var subscription = createSubscription( address, null, true );

    final var data = mock( EntityChangeData.class );
    final var entityChanges = new EntityChange[]{
      EntityChange.create( 0, 1, new String[]{ "0." + rootId + "#fi" }, data )
    };
    setCurrentMessageResponse( connection, UpdateMessage.create( null, null, null, null, entityChanges, null ) );

    when( creator.createEntity( 1, data ) ).thenReturn( mock( Linkable.class ) );

    connector.processEntityChanges();

    safeAction( () -> assertNotNull( subscription.findEntityByTypeAndId( Linkable.class, 1 ) ) );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void processEntityChanges_referenceNonExistentSubscription()
  {
    final var schemaId = 1;
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         null,
                         ChannelSchema.FilterType.NONE,
                         null,
                         false, true,
                         Collections.emptyList() );
    final var creator = mock( EntitySchema.Creator.class );
    final var updater = mock( EntitySchema.Updater.class );
    final var entitySchema =
      new EntitySchema( 0, ValueUtil.randomString(), Linkable.class, creator, updater, new ChannelLinkSchema[ 0 ] );
    final var schema =
      new SystemSchema( schemaId,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{ entitySchema } );
    final var connector = createConnector( schema );
    connector.setLinksToProcessPerTick( 1 );

    final var connection = newConnection( connector );

    final var userObject1 = mock( Linkable.class );

    // Pause scheduler to avoid converge of subscriptions
    pauseScheduler();

    final var data1 = mock( EntityChangeData.class );
    final var entityChanges = new EntityChange[]{
      EntityChange.create( 0, 1, new String[]{ "1" }, data1 )
    };
    setCurrentMessageResponse( connection, UpdateMessage.create( null, null, null, null, entityChanges, null ) );

    when( creator.createEntity( 1, data1 ) ).thenReturn( userObject1 );

    final var exception =
      expectThrows( IllegalStateException.class, connector::processEntityChanges );
    assertEquals( exception.getMessage(),
                  "Replicant-0069: UpdateMessage contained an EntityChange message referencing channel 1.1 but no such subscription exists locally." );
  }

  @Test
  public void processEntityChanges_deleteNonExistingEntity()
  {
    final var schemaId = 1;
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         null,
                         ChannelSchema.FilterType.NONE,
                         null,
                         false, true,
                         Collections.emptyList() );
    final var entitySchema =
      new EntitySchema( 0,
                        ValueUtil.randomString(),
                        MyEntity.class,
                        ( i, d ) -> new MyEntity(),
                        null,
                        new ChannelLinkSchema[ 0 ] );
    final var schema =
      new SystemSchema( schemaId,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{ entitySchema } );
    final var connector = createConnector( schema );
    connector.setLinksToProcessPerTick( 1 );

    final var connection = newConnection( connector );

    // Pause scheduler to avoid converge of subscriptions
    pauseScheduler();

    final var entityChanges = new EntityChange[]{
      // Remove change
      EntityChange.create( 0, 3, new String[]{ "1" } )
    };
    final var response =
      setCurrentMessageResponse( connection, UpdateMessage.create( null, null, null, null, entityChanges, null ) );

    connector.setChangesToProcessPerTick( 1 );

    connector.processEntityChanges();

    assertEquals( response.getEntityRemoveCount(), 0 );
  }

  @Test
  public void processEntityLinks()
  {
    final var connector = createConnector();
    connector.setLinksToProcessPerTick( 1 );

    final var connection = newConnection( connector );
    final var response =
      setCurrentMessageResponse( connection,
                                 UpdateMessage.create( null,
                                                       null,
                                                       new String[ 0 ],
                                                       null,
                                                       new EntityChange[ 0 ],
                                                       null ) );

    final var entity1 = mock( Linkable.class );
    final var entity2 = mock( Linkable.class );
    final var entity3 = mock( Linkable.class );
    final var entity4 = mock( Linkable.class );

    response.changeProcessed( entity1 );
    response.changeProcessed( entity2 );
    response.changeProcessed( entity3 );
    response.changeProcessed( entity4 );

    verify( entity1, never() ).link();
    verify( entity2, never() ).link();
    verify( entity3, never() ).link();
    verify( entity4, never() ).link();

    assertEquals( response.getEntityLinkCount(), 0 );

    connector.setLinksToProcessPerTick( 1 );

    connector.processEntityLinks();

    assertEquals( response.getEntityLinkCount(), 1 );
    verify( entity1, times( 1 ) ).link();
    verify( entity2, never() ).link();
    verify( entity3, never() ).link();
    verify( entity4, never() ).link();

    connector.setLinksToProcessPerTick( 2 );

    connector.processEntityLinks();

    assertEquals( response.getEntityLinkCount(), 3 );
    verify( entity1, times( 1 ) ).link();
    verify( entity2, times( 1 ) ).link();
    verify( entity3, times( 1 ) ).link();
    verify( entity4, never() ).link();
  }

  @Test
  public void completeAreaOfInterestRequest()
  {
    final var connector = createConnector();
    final var connection = newConnection( connector );

    connection.injectCurrentAreaOfInterestRequest( new AreaOfInterestRequest( new ChannelAddress( 1, 0 ),
                                                                              AreaOfInterestRequest.Type.ADD,
                                                                              null ) );

    assertFalse( connection.getCurrentAreaOfInterestRequests().isEmpty() );

    connector.completeAreaOfInterestRequest();

    assertTrue( connection.getCurrentAreaOfInterestRequests().isEmpty() );
  }

  @Test
  public void processChannelChanges_add()
  {
    final var connector = createConnector();
    final var connection = newConnection( connector );

    final var channelId = 0;
    final var rootId = ValueUtil.randomInt();
    final var filter = (String) null;
    final var channels = new String[]{ "+0." + rootId };

    final var response =
      setCurrentMessageResponse( connection, UpdateMessage.create( null, null, channels, null, null, null ) );
    response.setParsedChannelChanges( Collections.singletonList( ChannelChangeDescriptor.from( 1, channels[ 0 ] ) ) );

    assertTrue( response.needsChannelChangesProcessed() );

    final var handler = registerTestSpyEventHandler();

    connector.processChannelChanges();

    assertFalse( response.needsChannelChangesProcessed() );

    final var address = new ChannelAddress( 1, channelId, rootId );
    final var subscription =
      Replicant.context().findSubscription( address );
    assertNotNull( subscription );
    assertEquals( subscription.address(), address );
    safeAction( () -> assertEquals( subscription.getFilter(), filter ) );
    safeAction( () -> assertFalse( subscription.isExplicitSubscription() ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscriptionCreatedEvent.class, e -> {
      assertEquals( e.getSubscription().address(), address );
      safeAction( () -> assertEquals( e.getSubscription().getFilter(), filter ) );
    } );
  }

  @Test
  public void processChannelChanges_add_withFilter()
  {
    final var connector = createConnector();
    final var connection = newConnection( connector );

    final var channelId = 0;
    final var rootId = ValueUtil.randomInt();
    final var filter = ValueUtil.randomString();
    final var fchannels = new ChannelChange[]{ ChannelChange.create( "+0." + rootId, filter ) };
    final var response =
      setCurrentMessageResponse( connection, UpdateMessage.create( null, null, null, fchannels, null, null ) );
    response.setParsedChannelChanges( Collections.singletonList( ChannelChangeDescriptor.from( 1, fchannels[ 0 ] ) ) );

    assertTrue( response.needsChannelChangesProcessed() );

    final var handler = registerTestSpyEventHandler();

    connector.processChannelChanges();

    assertFalse( response.needsChannelChangesProcessed() );

    final var address = new ChannelAddress( 1, channelId, rootId );
    final var subscription =
      Replicant.context().findSubscription( address );
    assertNotNull( subscription );
    assertEquals( subscription.address(), address );
    safeAction( () -> assertEquals( subscription.getFilter(), filter ) );
    safeAction( () -> assertFalse( subscription.isExplicitSubscription() ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscriptionCreatedEvent.class, e -> {
      assertEquals( e.getSubscription().address(), address );
      safeAction( () -> assertEquals( e.getSubscription().getFilter(), filter ) );
    } );
  }

  @Test
  public void processChannelChanges_add_withCorrespondingAreaOfInterest()
  {
    final var connector = createConnector();
    final var connection = newConnection( connector );

    final var channelId = 0;
    final var rootId = ValueUtil.randomInt();

    final var address = new ChannelAddress( 1, channelId, rootId );

    safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    final var channels = new String[]{ "+0." + rootId };
    final var response =
      setCurrentMessageResponse( connection, UpdateMessage.create( null, null, channels, null, null, null ) );
    response.setParsedChannelChanges( Collections.singletonList( ChannelChangeDescriptor.from( 1, channels[ 0 ] ) ) );

    assertTrue( response.needsChannelChangesProcessed() );

    final var handler = registerTestSpyEventHandler();

    connector.processChannelChanges();

    assertFalse( response.needsChannelChangesProcessed() );

    final var subscription =
      Replicant.context().findSubscription( address );
    assertNotNull( subscription );
    assertEquals( subscription.address(), address );
    safeAction( () -> assertNull( subscription.getFilter() ) );
    safeAction( () -> assertTrue( subscription.isExplicitSubscription() ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscriptionCreatedEvent.class, e -> {
      assertEquals( e.getSubscription().address(), address );
      safeAction( () -> assertNull( e.getSubscription().getFilter() ) );
    } );
  }

  @Test
  public void processChannelChanges_addConvertingImplicitToExplicit()
  {
    final var connector = createConnector();
    final var connection = newConnection( connector );

    connector.pauseMessageScheduler();

    final var address = new ChannelAddress( 1, 0, ValueUtil.randomInt() );

    safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    final var channels = new String[]{ "+0." + address.rootId() };
    final var response =
      setCurrentMessageResponse( connection, UpdateMessage.create( null, null, channels, null, null, null ) );
    response.setParsedChannelChanges( Collections.singletonList( ChannelChangeDescriptor.from( 1, channels[ 0 ] ) ) );

    final var request = new AreaOfInterestRequest( address, AreaOfInterestRequest.Type.ADD, null );
    connection.injectCurrentAreaOfInterestRequest( request );
    request.markAsInProgress( newRequest( connection ).getRequestId() );

    assertTrue( response.needsChannelChangesProcessed() );
    assertEquals( response.getChannelAddCount(), 0 );

    final var handler = registerTestSpyEventHandler();

    connector.processChannelChanges();

    assertFalse( response.needsChannelChangesProcessed() );
    assertEquals( response.getChannelAddCount(), 1 );

    final var subscription =
      Replicant.context().findSubscription( address );
    assertNotNull( subscription );
    assertEquals( subscription.address(), address );
    safeAction( () -> assertNull( subscription.getFilter() ) );
    safeAction( () -> assertTrue( subscription.isExplicitSubscription() ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscriptionCreatedEvent.class, e -> {
      assertEquals( e.getSubscription().address(), address );
      safeAction( () -> assertNull( e.getSubscription().getFilter() ) );
    } );
  }

  @Test
  public void processChannelChanges_remove()
  {
    final var connector = createConnector();
    connector.pauseMessageScheduler();

    final var connection = newConnection( connector );

    final var address = new ChannelAddress( 1, 0, ValueUtil.randomInt() );

    final var channels = new String[]{ "-0." + address.rootId() };
    final var response =
      setCurrentMessageResponse( connection, UpdateMessage.create( null, null, channels, null, null, null ) );
    response.setParsedChannelChanges( Collections.singletonList( ChannelChangeDescriptor.from( 1, channels[ 0 ] ) ) );

    final var initialSubscription = createSubscription( address, ValueUtil.randomString(), true );

    assertTrue( response.needsChannelChangesProcessed() );
    assertEquals( response.getChannelRemoveCount(), 0 );

    final var handler = registerTestSpyEventHandler();

    connector.processChannelChanges();

    assertFalse( response.needsChannelChangesProcessed() );
    assertEquals( response.getChannelRemoveCount(), 1 );

    final var subscription =
      Replicant.context().findSubscription( address );
    assertNull( subscription );
    assertTrue( Disposable.isDisposed( initialSubscription ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscriptionDisposedEvent.class,
                             e -> assertEquals( e.getSubscription().address(), address ) );
  }

  @Test
  public void processChannelChanges_remove_withAreaOfInterest()
  {
    final var connector = createConnector();
    connector.pauseMessageScheduler();

    final var connection = newConnection( connector );

    final var address = new ChannelAddress( 1, 0, ValueUtil.randomInt() );

    final var areaOfInterest =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    final var channels = new String[]{ "-0." + address.rootId() };
    final var response =
      setCurrentMessageResponse( connection, UpdateMessage.create( null, null, channels, null, null, null ) );
    response.setParsedChannelChanges( Collections.singletonList( ChannelChangeDescriptor.from( 1, channels[ 0 ] ) ) );
    final var initialSubscription = createSubscription( address, ValueUtil.randomString(), true );

    assertTrue( response.needsChannelChangesProcessed() );
    assertEquals( response.getChannelRemoveCount(), 0 );

    final var handler = registerTestSpyEventHandler();

    connector.processChannelChanges();

    assertFalse( response.needsChannelChangesProcessed() );
    assertEquals( response.getChannelRemoveCount(), 1 );

    final var subscription =
      Replicant.context().findSubscription( address );
    assertNull( subscription );
    assertTrue( Disposable.isDisposed( initialSubscription ) );

    assertTrue( Disposable.isDisposed( areaOfInterest ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( SubscriptionDisposedEvent.class,
                             e -> assertEquals( e.getSubscription().address(), address ) );
    handler.assertNextEvent( AreaOfInterestDisposedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest().getAddress(), address ) );
  }

  @Test
  public void processChannelChanges_remove_WithMissingSubscription()
  {
    final var connector = createConnector();
    final var connection = newConnection( connector );

    final var channels = new String[]{ "-0.72" };
    final var response =
      setCurrentMessageResponse( connection, UpdateMessage.create( null, null, channels, null, null, null ) );
    response.setParsedChannelChanges( Collections.singletonList( ChannelChangeDescriptor.from( 1, channels[ 0 ] ) ) );
    assertTrue( response.needsChannelChangesProcessed() );
    assertEquals( response.getChannelRemoveCount(), 0 );

    final var handler = registerTestSpyEventHandler();

    connector.processChannelChanges();

    assertFalse( response.needsChannelChangesProcessed() );
    assertEquals( response.getChannelRemoveCount(), 1 );

    handler.assertEventCount( 0 );
  }

  @Test
  public void processChannelChanges_remove_WithMissingSubscription_butAreaOfInterestPresent()
  {
    final var connector = createConnector();
    final var connection = newConnection( connector );

    final var address = new ChannelAddress( 1, 0, ValueUtil.randomInt() );

    final var areaOfInterest =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    final var channels = new String[]{ "-0." + address.rootId() };
    final var response =
      setCurrentMessageResponse( connection, UpdateMessage.create( null, null, channels, null, null, null ) );
    response.setParsedChannelChanges( Collections.singletonList( ChannelChangeDescriptor.from( 1, channels[ 0 ] ) ) );
    assertTrue( response.needsChannelChangesProcessed() );
    assertEquals( response.getChannelRemoveCount(), 0 );

    final var handler = registerTestSpyEventHandler();

    connector.processChannelChanges();

    assertFalse( response.needsChannelChangesProcessed() );
    assertEquals( response.getChannelRemoveCount(), 1 );

    assertTrue( Disposable.isDisposed( areaOfInterest ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( AreaOfInterestDisposedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest().getAddress(), address ) );
  }

  @Test
  public void processChannelChanges_delete_WithMissingSubscription_butAreaOfInterestPresent()
  {
    final var connector = createConnector();
    final var connection = newConnection( connector );

    final var address = new ChannelAddress( 1, 0, ValueUtil.randomInt() );

    final var areaOfInterest =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    final var channels = new String[]{ "!0." + address.rootId() };
    final var response =
      setCurrentMessageResponse( connection, UpdateMessage.create( null, null, channels, null, null, null ) );
    response.setParsedChannelChanges( Collections.singletonList( ChannelChangeDescriptor.from( 1, channels[ 0 ] ) ) );
    assertTrue( response.needsChannelChangesProcessed() );
    assertEquals( response.getChannelRemoveCount(), 0 );

    final var handler = registerTestSpyEventHandler();

    connector.processChannelChanges();

    assertFalse( response.needsChannelChangesProcessed() );
    assertEquals( response.getChannelRemoveCount(), 1 );

    assertFalse( Disposable.isDisposed( areaOfInterest ) );
    assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.DELETED );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest().getAddress(), address ) );
  }

  @Test
  public void processChannelChanges_update()
  {
    final var filter = mock( SubscriptionUpdateEntityFilter.class );
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         null,
                         ChannelSchema.FilterType.DYNAMIC,
                         filter,
                         true, true,
                         Collections.emptyList() );
    final var entitySchema =
      new EntitySchema( 0, ValueUtil.randomString(), String.class, ( i, d ) -> "", null, new ChannelLinkSchema[ 0 ] );
    final var schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{ entitySchema } );

    final var connector = createConnector( schema );
    connector.pauseMessageScheduler();

    final var connection = newConnection( connector );

    final var address = new ChannelAddress( 1, 0, ValueUtil.randomInt() );

    final var oldFilter = ValueUtil.randomString();
    final var newFilter = ValueUtil.randomString();
    final var channelChanges =
      new ChannelChange[]{ ChannelChange.create( "=0." + address.rootId(), newFilter ) };
    final var response =
      setCurrentMessageResponse( connection, UpdateMessage.create( null, null, null, channelChanges, null, null ) );
    response
      .setParsedChannelChanges( Collections.singletonList( ChannelChangeDescriptor.from( 1, channelChanges[ 0 ] ) ) );

    final var initialSubscription = createSubscription( address, oldFilter, true );

    assertTrue( response.needsChannelChangesProcessed() );
    assertEquals( response.getChannelUpdateCount(), 0 );

    final var handler = registerTestSpyEventHandler();

    connector.processChannelChanges();

    assertFalse( response.needsChannelChangesProcessed() );
    assertEquals( response.getChannelUpdateCount(), 1 );

    final var subscription =
      Replicant.context().findSubscription( address );
    assertNotNull( subscription );
    assertFalse( Disposable.isDisposed( initialSubscription ) );

    handler.assertEventCount( 0 );
  }

  @Test
  public void processChannelChanges_update_withFilterInstanceId()
  {
    final var filter = mock( SubscriptionUpdateEntityFilter.class );
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         Integer.class,
                         ChannelSchema.FilterType.DYNAMIC_INSTANCED,
                         filter,
                         true, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[ 0 ] );

    final var connector = createConnector( schema );
    connector.pauseMessageScheduler();

    final var connection = newConnection( connector );

    final var rootId = ValueUtil.randomInt();
    final var address = new ChannelAddress( 1, 0, rootId, "fi" );

    final var oldFilter = ValueUtil.randomString();
    final var newFilter = ValueUtil.randomString();
    final var channelChanges =
      new ChannelChange[]{ ChannelChange.create( "=0." + rootId + "#fi", newFilter ) };
    final var response =
      setCurrentMessageResponse( connection, UpdateMessage.create( null, null, null, channelChanges, null, null ) );
    response
      .setParsedChannelChanges( Collections.singletonList( ChannelChangeDescriptor.from( 1, channelChanges[ 0 ] ) ) );

    final var subscription = createSubscription( address, oldFilter, true );

    assertTrue( response.needsChannelChangesProcessed() );
    assertEquals( response.getChannelUpdateCount(), 0 );

    connector.processChannelChanges();

    assertFalse( response.needsChannelChangesProcessed() );
    assertEquals( response.getChannelUpdateCount(), 1 );
    safeAction( () -> assertEquals( subscription.getFilter(), newFilter ) );
  }

  @Test
  public void processChannelChanges_update_forNonDYNAMICChannel()
  {
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         null,
                         ChannelSchema.FilterType.NONE,
                         null,
                         true,
                         true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1, ValueUtil.randomString(), new ChannelSchema[]{ channelSchema }, new EntitySchema[ 0 ] );
    final var connector = createConnector( schema );
    final var connection = newConnection( connector );

    final var oldFilter = ValueUtil.randomString();
    final var newFilter = ValueUtil.randomString();
    final var channelChanges =
      new ChannelChange[]{ ChannelChange.create( "=0.2223", newFilter ) };
    final var response =
      setCurrentMessageResponse( connection, UpdateMessage.create( null, null, null, channelChanges, null, null ) );
    response.setParsedChannelChanges( Collections.singletonList( ChannelChangeDescriptor.from( 1,
                                                                                               channelChanges[ 0 ] ) ) );
    createSubscription( new ChannelAddress( 1, 0, 2223 ), oldFilter, true );

    final var exception =
      expectThrows( IllegalStateException.class, connector::processChannelChanges );

    assertEquals( exception.getMessage(),
                  "Replicant-0078: Received ChannelChange of type UPDATE for address 1.0.2223 but the channel does not have a DYNAMIC filter." );
  }

  @Test
  public void processChannelChanges_update_missingSubscription()
  {
    final var connector = createConnector();
    final var connection = newConnection( connector );

    final var newFilter = ValueUtil.randomString();
    final var channelChanges =
      new ChannelChange[]{ ChannelChange.create( "=0.42", newFilter ) };
    final var response =
      setCurrentMessageResponse( connection, UpdateMessage.create( null, null, null, channelChanges, null, null ) );
    response.setParsedChannelChanges( Collections.singletonList( ChannelChangeDescriptor.from( 1,
                                                                                               channelChanges[ 0 ] ) ) );
    assertTrue( response.needsChannelChangesProcessed() );
    assertEquals( response.getChannelUpdateCount(), 0 );

    final var handler = registerTestSpyEventHandler();

    final var exception =
      expectThrows( IllegalStateException.class, connector::processChannelChanges );

    assertTrue( response.needsChannelChangesProcessed() );
    assertEquals( response.getChannelUpdateCount(), 0 );

    assertEquals( exception.getMessage(),
                  "Replicant-0033: Received ChannelChange of type UPDATE for address 1.0.42 but no such subscription exists." );

    handler.assertEventCount( 0 );
  }

  @Test
  public void removeExplicitSubscriptions()
  {
    // Pause converger
    pauseScheduler();

    final var connector = createConnector();

    final var address1 = new ChannelAddress( 1, 1, 1 );
    final var address2 = new ChannelAddress( 1, 1, 2 );
    final var address3 = new ChannelAddress( 1, 1, 3 );

    final var requests = new ArrayList<AreaOfInterestRequest>();
    requests.add( new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.REMOVE, null ) );
    requests.add( new AreaOfInterestRequest( address2, AreaOfInterestRequest.Type.REMOVE, null ) );
    requests.add( new AreaOfInterestRequest( address3, AreaOfInterestRequest.Type.REMOVE, null ) );

    final var subscription1 = createSubscription( address1, null, true );
    // Address2 is already implicit ...
    createSubscription( address2, null, false );
    // Address3 has no subscription ... maybe not converged yet

    connector.removeExplicitSubscriptions( requests );

    safeAction( () -> assertFalse( subscription1.isExplicitSubscription() ) );
  }

  @Test
  public void removeExplicitSubscriptions_passedBadAction()
  {
    // Pause converger
    pauseScheduler();

    final var connector = createConnector();

    final var address1 = new ChannelAddress( 1, 1, 1 );

    final var requests = new ArrayList<AreaOfInterestRequest>();
    requests.add( new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.ADD, null ) );

    createSubscription( address1, null, true );

    final var exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> connector.removeExplicitSubscriptions( requests ) ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0034: Connector.removeExplicitSubscriptions() invoked with request with type that is not REMOVE. Request: AreaOfInterestRequest[Type=ADD Address=1.1.1]" );
  }

  @Test
  public void removeUnneededRemoveRequests_whenInvariantsDisabled()
  {
    final var address1 = new ChannelAddress( 1, 1, 1 );
    final var address2 = new ChannelAddress( 1, 1, 2 );
    final var address3 = new ChannelAddress( 1, 1, 3 );

    final var requests = new ArrayList<AreaOfInterestRequest>();
    final var request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.REMOVE, null );
    final var request2 =
      new AreaOfInterestRequest( address2, AreaOfInterestRequest.Type.REMOVE, null );
    final var request3 =
      new AreaOfInterestRequest( address3, AreaOfInterestRequest.Type.REMOVE, null );
    requests.add( request1 );
    requests.add( request2 );
    requests.add( request3 );

    final var connection = createConnection();
    final var connector = connection.getConnector();

    final var request = newRequest( connection );

    requests.forEach( r -> r.markAsInProgress( request.getRequestId() ) );

    createSubscription( address1, null, true );
    // Address2 is already implicit ...
    createSubscription( address2, null, false );
    // Address3 has no subscription ... maybe not converged yet

    ReplicantTestUtil.noCheckInvariants();

    connector.removeUnneededRemoveRequests( requests );

    assertEquals( requests.size(), 1 );
    assertTrue( requests.contains( request1 ) );
    assertTrue( request1.isInProgress() );
    assertFalse( request2.isInProgress() );
    assertFalse( request3.isInProgress() );
  }

  @Test
  public void removeUnneededRemoveRequests_noSubscription()
  {
    final var connector = createConnector();

    final var address1 = new ChannelAddress( 1, 1, 1 );

    final var requests = new ArrayList<AreaOfInterestRequest>();
    final var request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.REMOVE, null );
    requests.add( request1 );

    final var requestId = newRequest( newConnection( connector ) ).getRequestId();
    requests.forEach( r -> r.markAsInProgress( requestId ) );

    final var exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> connector.removeUnneededRemoveRequests( requests ) ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0046: Request to unsubscribe from channel at address 1.1.1 but not subscribed to channel." );
  }

  @Test
  public void removeUnneededRemoveRequests_implicitSubscription()
  {
    final var connector = createConnector();

    final var address1 = new ChannelAddress( 1, 1, 1 );

    final var requests = new ArrayList<AreaOfInterestRequest>();
    final var request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.REMOVE, null );
    requests.add( request1 );

    final var requestId = newRequest( newConnection( connector ) ).getRequestId();
    requests.forEach( r -> r.markAsInProgress( requestId ) );

    createSubscription( address1, null, false );

    final var exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> connector.removeUnneededRemoveRequests( requests ) ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0047: Request to unsubscribe from channel at address 1.1.1 but subscription is not an explicit subscription." );
  }

  @Test
  public void removeUnneededUpdateRequests_whenInvariantsDisabled()
  {
    final var connector = createConnector();

    final var address1 = new ChannelAddress( 1, 1, 1 );
    final var address2 = new ChannelAddress( 1, 1, 2 );
    final var address3 = new ChannelAddress( 1, 1, 3 );

    final var requests = new ArrayList<AreaOfInterestRequest>();
    final var request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.UPDATE, null );
    final var request2 =
      new AreaOfInterestRequest( address2, AreaOfInterestRequest.Type.UPDATE, null );
    final var request3 =
      new AreaOfInterestRequest( address3, AreaOfInterestRequest.Type.UPDATE, null );
    requests.add( request1 );
    requests.add( request2 );
    requests.add( request3 );

    final var requestId = newRequest( newConnection( connector ) ).getRequestId();
    requests.forEach( r -> r.markAsInProgress( requestId ) );

    createSubscription( address1, null, true );
    // Address2 is already implicit ...
    createSubscription( address2, null, false );
    // Address3 has no subscription ... maybe not converged yet

    ReplicantTestUtil.noCheckInvariants();

    connector.removeUnneededUpdateRequests( requests );

    assertEquals( requests.size(), 2 );
    assertTrue( requests.contains( request1 ) );
    assertTrue( requests.contains( request2 ) );
    assertTrue( request1.isInProgress() );
    assertTrue( request2.isInProgress() );
    assertFalse( request3.isInProgress() );
  }

  @Test
  public void removeUnneededUpdateRequests_noSubscription()
  {
    final var connector = createConnector();

    final var address1 = new ChannelAddress( 1, 1, 1 );

    final var requests = new ArrayList<AreaOfInterestRequest>();
    final var request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.UPDATE, null );
    requests.add( request1 );

    final var requestId = newRequest( newConnection( connector ) ).getRequestId();
    requests.forEach( r -> r.markAsInProgress( requestId ) );

    final var exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> connector.removeUnneededUpdateRequests( requests ) ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0048: Request to update channel at address 1.1.1 but not subscribed to channel." );
  }

  @Test
  public void validateWorld_invalidEntity()
  {
    final var connector = createConnector();
    newConnection( connector );
    final var response = setCurrentMessageResponse( connector.ensureConnection(),
                                                                UpdateMessage.create( null,
                                                                                      null,
                                                                                      null,
                                                                                      null,
                                                                                      null, null ) );

    assertFalse( response.hasWorldBeenValidated() );

    final var entityService = Replicant.context().getEntityService();
    final var entity1 =
      safeAction( () -> entityService.findOrCreateEntity( "MyEntity/1", MyEntity.class, 1 ) );
    final var error = new Exception();
    safeAction( () -> entity1.setUserObject( new MyEntity( error ) ) );

    final var exception =
      expectThrows( IllegalStateException.class, connector::validateWorld );

    assertEquals( exception.getMessage(),
                  "Replicant-0065: Entity failed to verify during validation process. Entity = MyEntity/1" );

    assertTrue( response.hasWorldBeenValidated() );
  }

  @Test
  public void validateWorld_invalidEntity_ignoredIfCompileSettingDisablesValidation()
  {
    ReplicantTestUtil.noValidateEntitiesOnLoad();
    final var connector = createConnector();
    newConnection( connector );
    final var response =
      setCurrentMessageResponse( connector.ensureConnection(),
                                 UpdateMessage.create( null, null, null, null, null, null ) );

    assertTrue( response.hasWorldBeenValidated() );

    final var entityService = Replicant.context().getEntityService();
    final var entity1 =
      safeAction( () -> entityService.findOrCreateEntity( "MyEntity/1", MyEntity.class, 1 ) );
    final var error = new Exception();
    safeAction( () -> entity1.setUserObject( new MyEntity( error ) ) );

    connector.validateWorld();

    assertTrue( response.hasWorldBeenValidated() );
  }

  @Test
  public void validateWorld_validEntity()
  {
    final var connector = createConnector();
    newConnection( connector );
    final var response =
      setCurrentMessageResponse( connector.ensureConnection(),
                                 UpdateMessage.create( null, null, null, null, null, null ) );

    assertFalse( response.hasWorldBeenValidated() );

    final var entityService = Replicant.context().getEntityService();
    safeAction( () -> entityService.findOrCreateEntity( "MyEntity/1", MyEntity.class, 1 ) );

    connector.validateWorld();

    assertTrue( response.hasWorldBeenValidated() );
  }

  static class MyEntity
    implements Verifiable
  {
    @Nullable
    private final Exception _exception;

    MyEntity()
    {
      this( null );
    }

    MyEntity( @Nullable final Exception exception )
    {
      _exception = exception;
    }

    @Override
    public void verify()
      throws Exception
    {
      if ( null != _exception )
      {
        throw _exception;
      }
    }
  }

  @Test
  public void completeMessageResponse()
  {
    final var connector = createConnector();
    final var connection = newConnection( connector );

    setCurrentMessageResponse( connection, UpdateMessage.create( null, null, null, null, null, null ) );

    final var handler = registerTestSpyEventHandler();

    connector.completeMessageResponse();

    assertNull( connection.getCurrentMessageResponse() );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( MessageProcessedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getSchemaName(), connector.getSchema().getName() );
    } );
  }

  @Test
  public void completeMessageResponse_hasContent()
  {
    final var connector = createConnector();
    final var connection = newConnection( connector );
    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    final var request = newRequest( connection );
    final var changeSet =
      UpdateMessage.create( request.getRequestId(), null, new String[]{ "+1" }, null, null, null );

    setCurrentMessageResponse( connection, changeSet, request );

    final var handler = registerTestSpyEventHandler();

    connector.completeMessageResponse();

    assertNull( connection.getCurrentMessageResponse() );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( MessageProcessedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getSchemaName(), connector.getSchema().getName() );
    } );
    handler.assertNextEvent( SyncRequestEvent.class,
                             e -> assertEquals( e.getSchemaId(), connector.getSchema().getId() ) );
  }

  @Test
  public void completeMessageResponse_stillMessagesPending()
  {
    final var connector = createConnector();
    final var connection = newConnection( connector );

    setCurrentMessageResponse( connection, UpdateMessage.create( null, null, null, null, null, null ) );

    connection.enqueueResponse( UpdateMessage.create( null, null, null, null, null, null ), null );

    connector.completeMessageResponse();

    assertFalse( connector.ensureConnection().getPendingResponses().isEmpty() );
  }

  @Test
  public void completeMessageResponse_withPostAction()
  {
    final var connector = createConnector();
    final var connection = newConnection( connector );

    setCurrentMessageResponse( connection, UpdateMessage.create( null, null, null, null, null, null ) );

    final var postActionCallCount = new AtomicInteger();
    connector.setPostMessageResponseAction( postActionCallCount::incrementAndGet );

    assertEquals( postActionCallCount.get(), 0 );

    connector.completeMessageResponse();

    assertEquals( postActionCallCount.get(), 1 );
  }

  @Test
  public void completeMessageResponse_MessageWithRequest_RPCComplete()
  {
    final var connector = createConnector();
    final var connection = newConnection( connector );

    final var request = newRequest( connection );

    final var requestId = request.getRequestId();

    setCurrentMessageResponse( connection, OkMessage.create( requestId ), request );

    final var handler = registerTestSpyEventHandler();

    assertEquals( connection.getRequest( requestId ), request );

    connector.completeMessageResponse();

    assertNull( connection.getCurrentMessageResponse() );
    assertNull( connection.getRequests().get( requestId ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( MessageProcessedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getSchemaName(), connector.getSchema().getName() );
    } );
  }

  @SuppressWarnings( { "unchecked" } )
  @Test
  public void progressResponseProcessing()
  {
    /*
     * This test steps through each stage of a message processing.
     */

    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         null,
                         ChannelSchema.FilterType.NONE,
                         null,
                         false, true,
                         Collections.emptyList() );
    final var creator = mock( EntitySchema.Creator.class );
    final var updater = mock( EntitySchema.Updater.class );
    final var entitySchema =
      new EntitySchema( 0, ValueUtil.randomString(), Linkable.class, creator, updater, new ChannelLinkSchema[ 0 ] );
    final var schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{ entitySchema } );

    final var connector = createConnector( schema );
    final var connection = newConnection( connector );

    final var message =
      UpdateMessage.create( null,
                            null,
                            new String[]{ "+0" },
                            null,
                            new EntityChange[]{ EntityChange.create( 0,
                                                                     1,
                                                                     new String[]{ "0" },
                                                                     new EntityChangeDataImpl() ) }, null );
    connection.enqueueResponse( message, null );
    assertNull( connection.getCurrentMessageResponse() );
    assertEquals( connection.getPendingResponses().size(), 1 );

    final var response = connection.getPendingResponses().get( 0 );

    // Pickup parsed response and set it as current
    assertTrue( connector.progressResponseProcessing() );

    assertEquals( connection.getCurrentMessageResponse(), response );
    assertEquals( connection.getPendingResponses().size(), 0 );

    {
      assertTrue( response.needsChannelChangesProcessed() );

      // Process Channel Changes in response
      assertTrue( connector.progressResponseProcessing() );

      assertFalse( response.needsChannelChangesProcessed() );
    }

    {
      assertTrue( response.areEntityChangesPending() );

      when( creator.createEntity( anyInt(), any( EntityChangeData.class ) ) ).thenReturn( mock( Linkable.class ) );

      // Process Entity Changes in response
      assertTrue( connector.progressResponseProcessing() );

      assertFalse( response.areEntityChangesPending() );
    }

    {
      assertTrue( response.areEntityLinksPending() );

      // Process Entity Links in response
      assertTrue( connector.progressResponseProcessing() );

      assertFalse( response.areEntityLinksPending() );
    }

    {
      assertTrue( response.areEntityUpdateActionsPending() );

      // EntityUpdateActions processed
      assertTrue( connector.progressResponseProcessing() );

      assertFalse( response.areEntityUpdateActionsPending() );
    }

    {
      assertFalse( response.hasWorldBeenValidated() );

      // Validate World
      assertTrue( connector.progressResponseProcessing() );

      assertTrue( response.hasWorldBeenValidated() );
    }

    {
      assertEquals( connection.getCurrentMessageResponse(), response );

      // Complete message
      assertTrue( connector.progressResponseProcessing() );

      assertNull( connection.getCurrentMessageResponse() );
    }
  }

  @Test
  public void progressAreaOfInterestAddRequest_onSuccess()
  {
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         null,
                         ChannelSchema.FilterType.STATIC,
                         null,
                         false, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final var connector = createConnector( schema );
    final var connection = newConnection( connector );

    final var address = new ChannelAddress( 1, 0 );
    final var filter = ValueUtil.randomString();
    final var request =
      new AreaOfInterestRequest( address, AreaOfInterestRequest.Type.ADD, filter );

    pauseScheduler();

    connection.injectCurrentAreaOfInterestRequest( request );

    final var handler = registerTestSpyEventHandler();

    final var callCount = new AtomicInteger();
    doAnswer( i -> {
      callCount.incrementAndGet();
      assertEquals( i.getArguments()[ 0 ], address );
      assertEquals( i.getArguments()[ 1 ], filter );
      return null;
    } )
      .when( connector.getTransport() )
      .requestSubscribe( eq( address ),
                         eq( filter )
      );

    assertEquals( callCount.get(), 0 );
    assertFalse( connection.getCurrentAreaOfInterestRequests().isEmpty() );

    connector.progressAreaOfInterestAddRequest( request );

    assertEquals( callCount.get(), 1 );
    assertFalse( connection.getCurrentAreaOfInterestRequests().isEmpty() );
    assertNull( Replicant.context().findSubscription( address ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscribeStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @Test
  public void progressAreaOfInterestAddRequest_onSuccess_CachedValueNotInLocalCache()
  {
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         null,
                         ChannelSchema.FilterType.STATIC,
                         null,
                         true, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final var connector = createConnector( schema );
    final var connection = newConnection( connector );
    pauseScheduler();
    connector.pauseMessageScheduler();

    final var address = new ChannelAddress( 1, 0 );
    final var filter = ValueUtil.randomString();
    final var request =
      new AreaOfInterestRequest( address, AreaOfInterestRequest.Type.ADD, filter );

    connection.injectCurrentAreaOfInterestRequest( request );

    Replicant.context().setCacheService( new TestCacheService() );

    final var handler = registerTestSpyEventHandler();

    final var callCount = new AtomicInteger();
    doAnswer( i -> {
      callCount.incrementAndGet();
      return null;
    } )
      .when( connector.getTransport() )
      .requestSubscribe( eq( address ), eq( filter ) );

    assertEquals( callCount.get(), 0 );
    assertFalse( connection.getCurrentAreaOfInterestRequests().isEmpty() );

    connector.progressAreaOfInterestAddRequest( request );

    assertEquals( callCount.get(), 1 );
    assertFalse( connection.getCurrentAreaOfInterestRequests().isEmpty() );
    assertNull( Replicant.context().findSubscription( address ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscribeStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @Test
  public void progressAreaOfInterestAddRequest_onSuccess_CachedValueInLocalCache()
  {
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         null,
                         ChannelSchema.FilterType.STATIC,
                         null,
                         true, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final var connector = createConnector( schema );
    final var connection = newConnection( connector );

    final var address = new ChannelAddress( 1, 0 );
    final var filter = ValueUtil.randomString();
    final var request =
      new AreaOfInterestRequest( address, AreaOfInterestRequest.Type.ADD, filter );

    pauseScheduler();
    connector.pauseMessageScheduler();

    connection.injectCurrentAreaOfInterestRequest( request );

    final var cacheService = new TestCacheService();
    Replicant.context().setCacheService( cacheService );

    final var handler = registerTestSpyEventHandler();

    final var eTag = "";
    cacheService.store( address, eTag, ValueUtil.randomString() );
    final var callCount = new AtomicInteger();
    doAnswer( i -> {
      callCount.incrementAndGet();
      return null;
    } )
      .when( connector.getTransport() ).requestSubscribe( eq( address ), eq( filter ) );

    assertEquals( callCount.get(), 0 );
    assertFalse( connection.getCurrentAreaOfInterestRequests().isEmpty() );

    connector.progressAreaOfInterestAddRequest( request );

    assertEquals( callCount.get(), 1 );
    assertFalse( connection.getCurrentAreaOfInterestRequests().isEmpty() );
    assertNull( Replicant.context().findSubscription( address ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscribeStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
    } );

    assertFalse( connector.isSchedulerActive() );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void progressBulkAreaOfInterestAddRequests_onSuccess()
  {
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         String.class,
                         ChannelSchema.FilterType.STATIC,
                         null,
                         false, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final var connector = createConnector( schema );
    final var connection = newConnection( connector );

    final var address1 = new ChannelAddress( 1, 0, 1 );
    final var address2 = new ChannelAddress( 1, 0, 2 );
    final var address3 = new ChannelAddress( 1, 0, 3 );
    final var filter = ValueUtil.randomString();
    final var request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.ADD, filter );
    final var request2 =
      new AreaOfInterestRequest( address2, AreaOfInterestRequest.Type.ADD, filter );
    final var request3 =
      new AreaOfInterestRequest( address3, AreaOfInterestRequest.Type.ADD, filter );

    pauseScheduler();

    final var subscription1 = createSubscription( address1, null, true );
    final var subscription2 = createSubscription( address2, null, true );
    final var subscription3 = createSubscription( address3, null, true );

    connection.injectCurrentAreaOfInterestRequest( request1 );
    connection.injectCurrentAreaOfInterestRequest( request2 );
    connection.injectCurrentAreaOfInterestRequest( request3 );

    final var handler = registerTestSpyEventHandler();

    final var callCount = new AtomicInteger();
    doAnswer( i -> {
      callCount.incrementAndGet();
      final var addresses = (List<ChannelAddress>) i.getArguments()[ 0 ];
      assertTrue( addresses.contains( address1 ) );
      assertTrue( addresses.contains( address2 ) );
      assertTrue( addresses.contains( address3 ) );
      return null;
    } )
      .when( connector.getTransport() ).requestBulkSubscribe( any(), eq( filter ) );

    assertEquals( callCount.get(), 0 );
    assertFalse( connection.getCurrentAreaOfInterestRequests().isEmpty() );

    connector.progressBulkAreaOfInterestAddRequests( Arrays.asList( request1, request2, request3 ) );

    assertEquals( callCount.get(), 1 );
    assertFalse( connection.getCurrentAreaOfInterestRequests().isEmpty() );
    safeAction( () -> assertTrue( subscription1.isExplicitSubscription() ) );
    safeAction( () -> assertTrue( subscription2.isExplicitSubscription() ) );
    safeAction( () -> assertTrue( subscription3.isExplicitSubscription() ) );

    handler.assertEventCount( 3 );
    handler.assertNextEvent( SubscribeStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address1 );
    } );
    handler.assertNextEvent( SubscribeStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address2 );
    } );
    handler.assertNextEvent( SubscribeStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address3 );
    } );
  }

  @Test
  public void progressAreaOfInterestAddRequests_onFailure_zeroRequests()
  {
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         String.class,
                         ChannelSchema.FilterType.STATIC,
                         null,
                         false, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final var connector = createConnector( schema );
    final var connection = newConnection( connector );

    final var address1 = new ChannelAddress( 1, 0, 1 );
    final var filter = ValueUtil.randomString();
    final var request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.ADD, filter );

    pauseScheduler();

    connection.injectCurrentAreaOfInterestRequest( request1 );

    final var handler = registerTestSpyEventHandler();

    assertFalse( connection.getCurrentAreaOfInterestRequests().isEmpty() );

    final var requests = new ArrayList<AreaOfInterestRequest>();
    // Pass in empty requests list to simulate that they are all filtered out
    connector.progressAreaOfInterestAddRequests( requests );

    assertTrue( connection.getCurrentAreaOfInterestRequests().isEmpty() );

    handler.assertEventCount( 0 );
  }

  @Test
  public void progressAreaOfInterestUpdateRequest_onSuccess()
  {
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         null,
                         ChannelSchema.FilterType.DYNAMIC,
                         mock( SubscriptionUpdateEntityFilter.class ),
                         false, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final var connector = createConnector( schema );
    final var connection = newConnection( connector );

    final var address = new ChannelAddress( 1, 0 );
    final var filter = ValueUtil.randomString();
    final var request =
      new AreaOfInterestRequest( address, AreaOfInterestRequest.Type.UPDATE, filter );

    pauseScheduler();

    final var subscription = createSubscription( address, null, true );

    connection.injectCurrentAreaOfInterestRequest( request );

    final var handler = registerTestSpyEventHandler();

    final var callCount = new AtomicInteger();
    doAnswer( i -> {
      callCount.incrementAndGet();
      return null;
    } )
      .when( connector.getTransport() ).requestSubscribe( eq( address ), eq( filter ) );

    assertEquals( callCount.get(), 0 );
    assertFalse( connection.getCurrentAreaOfInterestRequests().isEmpty() );

    connector.progressAreaOfInterestUpdateRequest( request );

    assertEquals( callCount.get(), 1 );
    assertFalse( connection.getCurrentAreaOfInterestRequests().isEmpty() );
    safeAction( () -> assertTrue( subscription.isExplicitSubscription() ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscriptionUpdateStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void progressBulkAreaOfInterestUpdateRequests_onSuccess()
  {
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         String.class,
                         ChannelSchema.FilterType.DYNAMIC,
                         mock( SubscriptionUpdateEntityFilter.class ),
                         false, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final var connector = createConnector( schema );
    final var connection = newConnection( connector );

    final var address1 = new ChannelAddress( 1, 0, 1 );
    final var address2 = new ChannelAddress( 1, 0, 2 );
    final var address3 = new ChannelAddress( 1, 0, 3 );
    final var filter = ValueUtil.randomString();
    final var request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.UPDATE, filter );
    final var request2 =
      new AreaOfInterestRequest( address2, AreaOfInterestRequest.Type.UPDATE, filter );
    final var request3 =
      new AreaOfInterestRequest( address3, AreaOfInterestRequest.Type.UPDATE, filter );

    pauseScheduler();

    final var subscription1 = createSubscription( address1, null, true );
    final var subscription2 = createSubscription( address2, null, true );
    final var subscription3 = createSubscription( address3, null, true );

    connection.injectCurrentAreaOfInterestRequest( request1 );
    connection.injectCurrentAreaOfInterestRequest( request2 );
    connection.injectCurrentAreaOfInterestRequest( request3 );

    final var handler = registerTestSpyEventHandler();

    final var callCount = new AtomicInteger();
    doAnswer( i -> {
      callCount.incrementAndGet();
      final var addresses = (List<ChannelAddress>) i.getArguments()[ 0 ];
      assertTrue( addresses.contains( address1 ) );
      assertTrue( addresses.contains( address2 ) );
      assertTrue( addresses.contains( address3 ) );
      return null;
    } )
      .when( connector.getTransport() ).requestBulkSubscribe( any(), eq( filter ) );

    assertEquals( callCount.get(), 0 );
    assertFalse( connection.getCurrentAreaOfInterestRequests().isEmpty() );

    connector.progressBulkAreaOfInterestUpdateRequests( Arrays.asList( request1, request2, request3 ) );

    assertEquals( callCount.get(), 1 );
    assertFalse( connection.getCurrentAreaOfInterestRequests().isEmpty() );
    safeAction( () -> assertTrue( subscription1.isExplicitSubscription() ) );
    safeAction( () -> assertTrue( subscription2.isExplicitSubscription() ) );
    safeAction( () -> assertTrue( subscription3.isExplicitSubscription() ) );

    handler.assertEventCount( 3 );
    handler.assertNextEvent( SubscriptionUpdateStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address1 );
    } );
    handler.assertNextEvent( SubscriptionUpdateStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address2 );
    } );
    handler.assertNextEvent( SubscriptionUpdateStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address3 );
    } );
  }

  @Test
  public void progressAreaOfInterestUpdateRequests_onFailure_zeroRequests()
  {
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         String.class,
                         ChannelSchema.FilterType.DYNAMIC,
                         mock( SubscriptionUpdateEntityFilter.class ),
                         false, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final var connector = createConnector( schema );
    final var connection = newConnection( connector );

    final var address1 = new ChannelAddress( 1, 0, 1 );
    final var filter = ValueUtil.randomString();
    final var request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.UPDATE, filter );

    pauseScheduler();

    connection.injectCurrentAreaOfInterestRequest( request1 );

    final var handler = registerTestSpyEventHandler();

    assertFalse( connection.getCurrentAreaOfInterestRequests().isEmpty() );

    final var requests = new ArrayList<AreaOfInterestRequest>();
    // Pass in empty requests list to simulate that they are all filtered out
    connector.progressAreaOfInterestUpdateRequests( requests );

    assertTrue( connection.getCurrentAreaOfInterestRequests().isEmpty() );

    handler.assertEventCount( 0 );
  }

  @Test
  public void progressAreaOfInterestRemoveRequest_onSuccess()
  {
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         null,
                         ChannelSchema.FilterType.NONE,
                         null,
                         false, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final var connector = createConnector( schema );
    final var connection = newConnection( connector );

    final var address = new ChannelAddress( 1, 0 );
    final var request = new AreaOfInterestRequest( address, AreaOfInterestRequest.Type.REMOVE, null );

    pauseScheduler();

    final var subscription = createSubscription( address, null, true );

    connection.injectCurrentAreaOfInterestRequest( request );

    final var handler = registerTestSpyEventHandler();

    final var callCount = new AtomicInteger();
    doAnswer( i -> {
      callCount.incrementAndGet();
      return null;
    } )
      .when( connector.getTransport() )
      .requestUnsubscribe( eq( address ) );

    assertEquals( callCount.get(), 0 );
    assertFalse( connection.getCurrentAreaOfInterestRequests().isEmpty() );

    connector.progressAreaOfInterestRemoveRequest( request );

    assertEquals( callCount.get(), 1 );
    assertFalse( connection.getCurrentAreaOfInterestRequests().isEmpty() );
    safeAction( () -> assertTrue( subscription.isExplicitSubscription() ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( UnsubscribeStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void progressBulkAreaOfInterestRemoveRequests_onSuccess()
  {
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         String.class,
                         ChannelSchema.FilterType.NONE,
                         null,
                         false, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final var connector = createConnector( schema );
    final var connection = newConnection( connector );

    final var address1 = new ChannelAddress( 1, 0, 1 );
    final var address2 = new ChannelAddress( 1, 0, 2 );
    final var address3 = new ChannelAddress( 1, 0, 3 );
    final var request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.REMOVE, null );
    final var request2 =
      new AreaOfInterestRequest( address2, AreaOfInterestRequest.Type.REMOVE, null );
    final var request3 =
      new AreaOfInterestRequest( address3, AreaOfInterestRequest.Type.REMOVE, null );

    pauseScheduler();

    final var subscription1 = createSubscription( address1, null, true );
    final var subscription2 = createSubscription( address2, null, true );
    final var subscription3 = createSubscription( address3, null, true );

    connection.injectCurrentAreaOfInterestRequest( request1 );
    connection.injectCurrentAreaOfInterestRequest( request2 );
    connection.injectCurrentAreaOfInterestRequest( request3 );

    final var handler = registerTestSpyEventHandler();

    final var callCount = new AtomicInteger();
    doAnswer( i -> {
      callCount.incrementAndGet();
      final var addresses = (List<ChannelAddress>) i.getArguments()[ 0 ];
      assertTrue( addresses.contains( address1 ) );
      assertTrue( addresses.contains( address2 ) );
      assertTrue( addresses.contains( address3 ) );
      return null;
    } )
      .when( connector.getTransport() ).requestBulkUnsubscribe( any() );

    assertEquals( callCount.get(), 0 );
    assertFalse( connection.getCurrentAreaOfInterestRequests().isEmpty() );

    connector.progressBulkAreaOfInterestRemoveRequests( Arrays.asList( request1, request2, request3 ) );

    assertEquals( callCount.get(), 1 );
    assertFalse( connection.getCurrentAreaOfInterestRequests().isEmpty() );
    safeAction( () -> assertTrue( subscription1.isExplicitSubscription() ) );
    safeAction( () -> assertTrue( subscription2.isExplicitSubscription() ) );
    safeAction( () -> assertTrue( subscription3.isExplicitSubscription() ) );

    handler.assertEventCount( 3 );
    handler.assertNextEvent( UnsubscribeStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address1 );
    } );
    handler.assertNextEvent( UnsubscribeStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address2 );
    } );
    handler.assertNextEvent( UnsubscribeStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address3 );
    } );
  }

  @Test
  public void progressAreaOfInterestRemoveRequests_onFailure_zeroRequests()
  {
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         String.class,
                         ChannelSchema.FilterType.NONE,
                         null,
                         false, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final var connector = createConnector( schema );
    final var connection = newConnection( connector );

    final var address1 = new ChannelAddress( 1, 0, 1 );
    final var request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.REMOVE, null );

    pauseScheduler();

    connection.injectCurrentAreaOfInterestRequest( request1 );

    final var handler = registerTestSpyEventHandler();

    assertFalse( connection.getCurrentAreaOfInterestRequests().isEmpty() );

    final var requests = new ArrayList<AreaOfInterestRequest>();
    // Pass in empty requests list to simulate that they are all filtered out
    connector.progressAreaOfInterestRemoveRequests( requests );

    assertTrue( connection.getCurrentAreaOfInterestRequests().isEmpty() );

    handler.assertEventCount( 0 );
  }

  @Test
  public void progressAreaOfInterestRequestProcessing_Noop()
  {
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         String.class,
                         ChannelSchema.FilterType.STATIC,
                         null,
                         false, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final var connector = createConnector( schema );
    final var connection = newConnection( connector );

    pauseScheduler();

    final var handler = registerTestSpyEventHandler();

    assertTrue( connection.getCurrentAreaOfInterestRequests().isEmpty() );

    connector.progressAreaOfInterestRequestProcessing();

    assertTrue( connection.getCurrentAreaOfInterestRequests().isEmpty() );

    handler.assertEventCount( 0 );
  }

  @Test
  public void progressAreaOfInterestRequestProcessing_InProgress()
  {
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         String.class,
                         ChannelSchema.FilterType.STATIC,
                         null,
                         false, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final var connector = createConnector( schema );
    final var connection = newConnection( connector );

    final var address1 = new ChannelAddress( 1, 0, 1 );
    final var filter = ValueUtil.randomString();
    final var request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.ADD, filter );

    pauseScheduler();

    request1.markAsInProgress( newRequest( connection ).getRequestId() );

    connection.injectCurrentAreaOfInterestRequest( request1 );

    final var handler = registerTestSpyEventHandler();

    assertFalse( connection.getCurrentAreaOfInterestRequests().isEmpty() );

    connector.progressAreaOfInterestRequestProcessing();

    assertFalse( connection.getCurrentAreaOfInterestRequests().isEmpty() );

    handler.assertEventCount( 0 );
  }

  @Test
  public void progressAreaOfInterestRequestProcessing_Add()
  {
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         String.class,
                         ChannelSchema.FilterType.STATIC,
                         null,
                         false, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final var connector = createConnector( schema );
    final var connection = newConnection( connector );

    final var address1 = new ChannelAddress( 1, 0, 1 );
    final var filter = ValueUtil.randomString();
    final var request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.ADD, filter );

    pauseScheduler();

    connection.injectCurrentAreaOfInterestRequest( request1 );

    final var handler = registerTestSpyEventHandler();

    assertFalse( connection.getCurrentAreaOfInterestRequests().isEmpty() );

    connector.progressAreaOfInterestRequestProcessing();

    assertFalse( connection.getCurrentAreaOfInterestRequests().isEmpty() );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscribeStartedEvent.class, e -> assertEquals( e.getAddress(), address1 ) );
  }

  @Test
  public void progressAreaOfInterestRequestProcessing_Update()
  {
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         String.class,
                         ChannelSchema.FilterType.DYNAMIC,
                         mock( SubscriptionUpdateEntityFilter.class ),
                         false, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final var connector = createConnector( schema );
    final var connection = newConnection( connector );

    final var address1 = new ChannelAddress( 1, 0, 1 );
    final var filter = ValueUtil.randomString();
    final var request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.UPDATE, filter );

    pauseScheduler();

    createSubscription( address1, null, true );
    connection.injectCurrentAreaOfInterestRequest( request1 );

    final var handler = registerTestSpyEventHandler();

    assertFalse( connection.getCurrentAreaOfInterestRequests().isEmpty() );

    connector.progressAreaOfInterestRequestProcessing();

    assertFalse( connection.getCurrentAreaOfInterestRequests().isEmpty() );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscriptionUpdateStartedEvent.class, e -> assertEquals( e.getAddress(), address1 ) );
  }

  @Test
  public void progressAreaOfInterestRequestProcessing_Remove()
  {
    final var channelSchema =
      new ChannelSchema( 0,
                         ValueUtil.randomString(),
                         String.class,
                         ChannelSchema.FilterType.NONE,
                         null,
                         false, true,
                         Collections.emptyList() );
    final var schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final var connector = createConnector( schema );
    final var connection = newConnection( connector );

    final var address1 = new ChannelAddress( 1, 0, 1 );
    final var request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.REMOVE, null );

    pauseScheduler();

    createSubscription( address1, null, true );

    connection.injectCurrentAreaOfInterestRequest( request1 );

    final var handler = registerTestSpyEventHandler();

    assertFalse( connection.getCurrentAreaOfInterestRequests().isEmpty() );

    connector.progressAreaOfInterestRequestProcessing();

    assertFalse( connection.getCurrentAreaOfInterestRequests().isEmpty() );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( UnsubscribeStartedEvent.class, e -> assertEquals( e.getAddress(), address1 ) );
  }

  @Test
  public void pauseMessageScheduler()
  {
    final var connector = createConnector();
    newConnection( connector );

    connector.pauseMessageScheduler();

    assertTrue( connector.isSchedulerPaused() );
    assertFalse( connector.isSchedulerActive() );

    connector.requestSubscribe( new ChannelAddress( 1, 0, 1 ), null );

    assertTrue( connector.isSchedulerActive() );

    connector.resumeMessageScheduler();

    assertFalse( connector.isSchedulerPaused() );
    assertFalse( connector.isSchedulerActive() );

    connector.pauseMessageScheduler();

    assertTrue( connector.isSchedulerPaused() );
    assertFalse( connector.isSchedulerActive() );

    // No progress
    assertFalse( connector.progressMessages() );

    Disposable.dispose( connector );

    assertFalse( connector.isSchedulerActive() );
    assertTrue( connector.isSchedulerPaused() );

    assertNull( connector.getSchedulerLock() );
  }

  @Test
  public void isSynchronized_notConnected()
  {
    final var connector = createConnector();
    safeAction( () -> connector.setState( ConnectorState.DISCONNECTED ) );
    safeAction( () -> assertFalse( connector.isSynchronized() ) );
  }

  @Test
  public void isSynchronized_connected()
  {
    final var connector = createConnector();
    newConnection( connector );
    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );
    safeAction( () -> assertTrue( connector.isSynchronized() ) );
  }

  @Test
  public void shouldRequestSync_notConnected()
  {
    final var connector = createConnector();
    safeAction( () -> connector.setState( ConnectorState.DISCONNECTED ) );
    assertFalse( connector.shouldRequestSync() );
  }

  @Test
  public void shouldRequestSync_connected()
  {
    final var connector = createConnector();
    newConnection( connector );
    safeAction( () -> {
      connector.setState( ConnectorState.CONNECTED );
      assertFalse( connector.shouldRequestSync() );
    } );
  }

  @Test
  public void shouldRequestSync_sentRequest_NotSynced()
  {
    final var connector = createConnector();
    newConnection( connector );
    safeAction( () -> {
      connector.setState( ConnectorState.CONNECTED );
      newRequest( connector.ensureConnection() );
      assertFalse( connector.shouldRequestSync() );
    } );
  }

  @Test
  public void shouldRequestSync_receivedRequestResponse_NotSynced()
  {
    final var connector = createConnector();
    final var connection = newConnection( connector );
    safeAction( () -> {
      connector.setState( ConnectorState.CONNECTED );
      connection.removeRequest( newRequest( connection ).getRequestId() );
      assertTrue( connector.shouldRequestSync() );
    } );
  }

  @Test
  public void shouldRequestSync_receivedSyncRequestResponse_Synced()
  {
    final var connector = createConnector();
    final var connection = newConnection( connector );
    safeAction( () -> {
      connector.setState( ConnectorState.CONNECTED );
      final var entry = connection.newRequest( ValueUtil.randomString(), true, null );
      connection.removeRequest( entry.getRequestId() );
      assertFalse( connector.shouldRequestSync() );
    } );
  }

  @Test
  public void shouldRequestSync_receivedSyncRequestResponseButResponsesQueued_NotSynced()
  {
    final var connector = createConnector();
    final var connection = newConnection( connector );
    safeAction( () -> {
      connector.setState( ConnectorState.CONNECTED );
      connection.removeRequest( newRequest( connection ).getRequestId() );
      connection.enqueueResponse( UpdateMessage.create( null, null, null, null, null, null ), null );
      assertFalse( connector.shouldRequestSync() );
    } );
  }

  @Test
  public void onInSync()
  {
    final var connector = createConnector();

    final var handler = registerTestSpyEventHandler();

    connector.onInSync();

    handler.assertEventCount( 1 );
    handler.assertNextEvent( InSyncEvent.class,
                             e -> assertEquals( e.getSchemaId(), connector.getSchema().getId() ) );
  }

  @Test
  public void onOutOfSync()
  {
    final var connector = createConnector();

    final var handler = registerTestSpyEventHandler();

    connector.onOutOfSync();

    handler.assertEventCount( 1 );
    handler.assertNextEvent( OutOfSyncEvent.class,
                             e -> assertEquals( e.getSchemaId(), connector.getSchema().getId() ) );
  }

  @Test
  public void requestSync()
  {
    final var connector = createConnector();

    final var handler = registerTestSpyEventHandler();

    connector.requestSync();

    verify( connector.getTransport() ).requestSync();

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SyncRequestEvent.class,
                             e -> assertEquals( e.getSchemaId(), connector.getSchema().getId() ) );
  }

  @Test
  public void maybeRequestSync()
  {
    final var connector = createConnector();
    final var connection = newConnection( connector );

    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    assertFalse( connector.shouldRequestSync() );
    connector.maybeRequestSync();

    assertTrue( connector.ensureConnection().getRequests().isEmpty() );

    connection.removeRequest( newRequest( connection ).getRequestId() );
    assertTrue( connector.shouldRequestSync() );

    connector.maybeRequestSync();

    verify( connector.getTransport() ).requestSync();
  }

  @Test
  public void onExecStarted()
  {
    final var connector = createConnector();

    final var handler = registerTestSpyEventHandler();

    final var command = ValueUtil.randomString();
    final var requestId = ValueUtil.randomInt();
    connector.onExecStarted( command, requestId );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( ExecStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getSchemaName(), connector.getSchema().getName() );
      assertEquals( e.getCommand(), command );
      assertEquals( e.getRequestId(), requestId );
    } );
  }

  @Test
  public void onExecCompleted()
  {
    final var connector = createConnector();

    final var handler = registerTestSpyEventHandler();

    final var command = ValueUtil.randomString();
    final var requestId = ValueUtil.randomInt();
    connector.onExecCompleted( command, requestId );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( ExecCompletedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getSchemaName(), connector.getSchema().getName() );
      assertEquals( e.getCommand(), command );
      assertEquals( e.getRequestId(), requestId );
    } );
  }

  @Test
  public void requestExec()
  {
    final var connector = createConnector();
    connector.pauseMessageScheduler();
    newConnection( connector );

    final var handler = registerTestSpyEventHandler();

    final var command = ValueUtil.randomString();
    final var payload = new Object();
    connector.requestExec( command, payload, null );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( ExecRequestQueuedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getSchemaName(), connector.getSchema().getName() );
      assertEquals( e.getCommand(), command );
    } );

    final var requests = connector.ensureConnection().getPendingExecRequests();
    assertEquals( requests.size(), 1 );
    final var request = requests.get( 0 );
    assertEquals( request.getCommand(), command );
    assertEquals( request.getPayload(), payload );
  }

  @Nonnull
  private RequestEntry newRequest( @Nonnull final Connection connection )
  {
    return connection.newRequest( ValueUtil.randomString(), false, null );
  }

  @Nonnull
  private MessageResponse setCurrentMessageResponse( @Nonnull final Connection connection,
                                                     @Nonnull final ServerToClientMessage message )
  {
    return setCurrentMessageResponse( connection, message, null );
  }

  @Nonnull
  private MessageResponse setCurrentMessageResponse( @Nonnull final Connection connection,
                                                     @Nonnull final ServerToClientMessage message,
                                                     @Nullable final RequestEntry request )
  {
    connection.enqueueResponse( message, request );
    connection.selectNextMessageResponse();
    final var response = connection.getCurrentMessageResponse();
    assert null != response;
    return response;
  }
}
