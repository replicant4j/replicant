package replicant;

import arez.Disposable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.intellij.lang.annotations.Language;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.Assert;
import org.testng.annotations.Test;
import replicant.spy.AreaOfInterestStatusUpdatedEvent;
import replicant.spy.ConnectFailureEvent;
import replicant.spy.ConnectedEvent;
import replicant.spy.DataLoadStatus;
import replicant.spy.DisconnectFailureEvent;
import replicant.spy.DisconnectedEvent;
import replicant.spy.MessageProcessFailureEvent;
import replicant.spy.MessageProcessedEvent;
import replicant.spy.MessageReadFailureEvent;
import replicant.spy.RestartEvent;
import replicant.spy.SubscribeCompletedEvent;
import replicant.spy.SubscribeFailedEvent;
import replicant.spy.SubscribeRequestQueuedEvent;
import replicant.spy.SubscribeStartedEvent;
import replicant.spy.SubscriptionCreatedEvent;
import replicant.spy.SubscriptionDisposedEvent;
import replicant.spy.SubscriptionUpdateCompletedEvent;
import replicant.spy.SubscriptionUpdateFailedEvent;
import replicant.spy.SubscriptionUpdateRequestQueuedEvent;
import replicant.spy.SubscriptionUpdateStartedEvent;
import replicant.spy.UnsubscribeCompletedEvent;
import replicant.spy.UnsubscribeFailedEvent;
import replicant.spy.UnsubscribeRequestQueuedEvent;
import replicant.spy.UnsubscribeStartedEvent;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

@SuppressWarnings( "NonJREEmulationClassesInClientCode" )
public class ConnectorTest
  extends AbstractReplicantTest
{
  @Test
  public void construct()
    throws Exception
  {
    final Disposable schedulerLock = pauseScheduler();
    final ReplicantRuntime runtime = Replicant.context().getRuntime();

    safeAction( () -> assertEquals( runtime.getConnectors().size(), 0 ) );

    final SystemSchema schema =
      new SystemSchema( ValueUtil.randomInt(),
                        ValueUtil.randomString(),
                        new ChannelSchema[ 0 ],
                        new EntitySchema[ 0 ] );
    final TestConnector connector = TestConnector.create( schema );

    assertEquals( connector.getSchema(), schema );

    safeAction( () -> assertEquals( runtime.getConnectors().size(), 1 ) );

    assertEquals( connector.getReplicantRuntime(), runtime );
    safeAction( () -> assertEquals( connector.getReplicantContext().getSchemaService().contains( schema ), true ) );

    safeAction( () -> Assert.assertEquals( connector.getState(), ConnectorState.DISCONNECTED ) );

    schedulerLock.dispose();

    safeAction( () -> Assert.assertEquals( connector.getState(), ConnectorState.CONNECTING ) );
  }

  @Test
  public void dispose()
  {
    final ReplicantRuntime runtime = Replicant.context().getRuntime();

    safeAction( () -> assertEquals( runtime.getConnectors().size(), 0 ) );

    final TestConnector connector = TestConnector.create();

    safeAction( () -> assertEquals( runtime.getConnectors().size(), 1 ) );

    Disposable.dispose( connector );

    safeAction( () -> assertEquals( runtime.getConnectors().size(), 0 ) );
  }

  @Test
  public void testToString()
    throws Exception
  {
    final TestConnector connector = TestConnector.create();
    assertEquals( connector.toString(), "Connector[Rose]" );
    ReplicantTestUtil.disableNames();
    assertEquals( connector.toString(), "replicant.Arez_TestConnector@" + Integer.toHexString( connector.hashCode() ) );
  }

  @Test
  public void connect()
  {
    pauseScheduler();

    final TestConnector connector = TestConnector.create();
    safeAction( () -> Assert.assertEquals( connector.getState(), ConnectorState.DISCONNECTED ) );

    safeAction( connector::connect );

    safeAction( () -> Assert.assertEquals( connector.getState(), ConnectorState.CONNECTING ) );
  }

  @Test
  public void connect_causesError()
  {
    pauseScheduler();

    final TestConnector connector = TestConnector.create();
    safeAction( () -> Assert.assertEquals( connector.getState(), ConnectorState.DISCONNECTED ) );

    connector.setErrorOnConnect( true );
    assertThrows( () -> safeAction( connector::connect ) );

    safeAction( () -> Assert.assertEquals( connector.getState(), ConnectorState.ERROR ) );
  }

  @Test
  public void disconnect()
  {
    pauseScheduler();

    final TestConnector connector = TestConnector.create();
    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    safeAction( connector::disconnect );

    safeAction( () -> Assert.assertEquals( connector.getState(), ConnectorState.DISCONNECTING ) );
  }

  @Test
  public void disconnect_causesError()
  {
    pauseScheduler();

    final TestConnector connector = TestConnector.create();
    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    connector.setErrorOnDisconnect( true );
    assertThrows( () -> safeAction( connector::disconnect ) );

    safeAction( () -> Assert.assertEquals( connector.getState(), ConnectorState.ERROR ) );
  }

  @Test
  public void onDisconnected()
  {
    final TestConnector connector = TestConnector.create();

    safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    safeAction( () -> assertEquals( connector.getReplicantRuntime().getState(),
                                    RuntimeState.CONNECTING ) );

    // Pause scheduler so runtime does not try to update state
    pauseScheduler();

    safeAction( connector::onDisconnected );

    safeAction( () -> assertEquals( connector.getState(), ConnectorState.DISCONNECTED ) );
    safeAction( () -> assertEquals( connector.getReplicantRuntime().getState(),
                                    RuntimeState.DISCONNECTED ) );
  }

  @Test
  public void onDisconnected_generatesSpyMessage()
    throws Exception
  {
    final TestConnector connector = TestConnector.create();

    safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    // Pause scheduler so runtime does not try to update state
    pauseScheduler();

    safeAction( connector::onDisconnected );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( DisconnectedEvent.class,
                             e -> assertEquals( e.getSchemaId(), connector.getSchema().getId() ) );
  }

  @Test
  public void onDisconnectFailure()
    throws Exception
  {
    final TestConnector connector = TestConnector.create();

    safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    safeAction( () -> assertEquals( connector.getReplicantRuntime().getState(),
                                    RuntimeState.CONNECTING ) );

    final Throwable error = new Throwable();

    // Pause scheduler so runtime does not try to update state
    pauseScheduler();

    safeAction( () -> connector.onDisconnectFailure( error ) );

    safeAction( () -> assertEquals( connector.getState(), ConnectorState.ERROR ) );
    safeAction( () -> assertEquals( connector.getReplicantRuntime().getState(),
                                    RuntimeState.ERROR ) );
  }

  @Test
  public void onDisconnectFailure_generatesSpyMessage()
    throws Exception
  {
    final TestConnector connector = TestConnector.create();

    safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final Throwable error = new Throwable();

    // Pause scheduler so runtime does not try to update state
    pauseScheduler();

    safeAction( () -> connector.onDisconnectFailure( error ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( DisconnectFailureEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getError(), error );
    } );
  }

  @Test
  public void onConnected()
    throws Exception
  {
    final TestConnector connector = TestConnector.create();

    safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    safeAction( () -> assertEquals( connector.getReplicantRuntime().getState(),
                                    RuntimeState.CONNECTING ) );

    // Pause scheduler so runtime does not try to update state
    pauseScheduler();

    safeAction( connector::onConnected );

    safeAction( () -> assertEquals( connector.getState(), ConnectorState.CONNECTED ) );
    safeAction( () -> assertEquals( connector.getReplicantRuntime().getState(),
                                    RuntimeState.CONNECTED ) );
  }

  @Test
  public void onConnected_generatesSpyMessage()
    throws Exception
  {
    final TestConnector connector = TestConnector.create();

    safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    safeAction( connector::onConnected );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( ConnectedEvent.class,
                             e -> assertEquals( e.getSchemaId(), connector.getSchema().getId() ) );
  }

  @Test
  public void onConnectFailure()
    throws Exception
  {
    final TestConnector connector = TestConnector.create();

    safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    safeAction( () -> assertEquals( connector.getReplicantRuntime().getState(),
                                    RuntimeState.CONNECTING ) );

    final Throwable error = new Throwable();

    // Pause scheduler so runtime does not try to update state
    pauseScheduler();

    safeAction( () -> connector.onConnectFailure( error ) );

    safeAction( () -> assertEquals( connector.getState(), ConnectorState.ERROR ) );
    safeAction( () -> assertEquals( connector.getReplicantRuntime().getState(),
                                    RuntimeState.ERROR ) );
  }

  @Test
  public void onConnectFailure_generatesSpyMessage()
    throws Exception
  {
    final TestConnector connector = TestConnector.create();

    safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final Throwable error = new Throwable();

    // Pause scheduler so runtime does not try to update state
    pauseScheduler();

    safeAction( () -> connector.onConnectFailure( error ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( ConnectFailureEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getError(), error );
    } );
  }

  @Test
  public void onMessageProcessed_generatesSpyMessage()
    throws Exception
  {
    final TestConnector connector = TestConnector.create();

    safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final DataLoadStatus status =
      new DataLoadStatus( ValueUtil.randomInt(),
                          ValueUtil.randomInt(),
                          ValueUtil.getRandom().nextInt( 10 ),
                          ValueUtil.getRandom().nextInt( 10 ),
                          ValueUtil.getRandom().nextInt( 10 ),
                          ValueUtil.getRandom().nextInt( 100 ),
                          ValueUtil.getRandom().nextInt( 100 ),
                          ValueUtil.getRandom().nextInt( 10 ) );

    safeAction( () -> connector.onMessageProcessed( status ) );

    handler.assertEventCount( 1 );

    handler.assertNextEvent( MessageProcessedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getDataLoadStatus(), status );
    } );
  }

  @Test
  public void onMessageProcessFailure()
    throws Exception
  {
    final TestConnector connector = TestConnector.create();
    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    final Throwable error = new Throwable();

    // Pause scheduler so runtime does not try to update state
    pauseScheduler();

    safeAction( () -> connector.onMessageProcessFailure( error ) );

    safeAction( () -> assertEquals( connector.getState(), ConnectorState.DISCONNECTING ) );
  }

  @Test
  public void onMessageProcessFailure_generatesSpyMessage()
    throws Exception
  {
    final TestConnector connector = TestConnector.create();

    safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final Throwable error = new Throwable();

    safeAction( () -> connector.onMessageProcessFailure( error ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( MessageProcessFailureEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getError(), error );
    } );
  }

  @Test
  public void disconnectIfPossible()
    throws Exception
  {
    final TestConnector connector = TestConnector.create();

    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    final Throwable error = new Throwable();

    safeAction( () -> connector.disconnectIfPossible( error ) );

    safeAction( () -> assertEquals( connector.getState(), ConnectorState.DISCONNECTING ) );
  }

  @Test
  public void disconnectIfPossible_noActionAsConnecting()
    throws Exception
  {
    final TestConnector connector = TestConnector.create();

    safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    final Throwable error = new Throwable();

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    safeAction( () -> connector.disconnectIfPossible( error ) );

    handler.assertEventCount( 0 );

    safeAction( () -> assertEquals( connector.getState(), ConnectorState.CONNECTING ) );
  }

  @Test
  public void disconnectIfPossible_generatesSpyEvent()
    throws Exception
  {
    final TestConnector connector = TestConnector.create();

    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final Throwable error = new Throwable();

    safeAction( () -> connector.disconnectIfPossible( error ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( RestartEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getError(), error );
    } );
  }

  @Test
  public void onMessageReadFailure()
    throws Exception
  {
    final TestConnector connector = TestConnector.create();
    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    final Throwable error = new Throwable();

    // Pause scheduler so runtime does not try to update state
    pauseScheduler();

    safeAction( () -> connector.onMessageReadFailure( error ) );

    safeAction( () -> assertEquals( connector.getState(), ConnectorState.DISCONNECTING ) );
  }

  @Test
  public void onMessageReadFailure_generatesSpyMessage()
    throws Exception
  {
    final TestConnector connector = TestConnector.create();

    safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final Throwable error = new Throwable();

    safeAction( () -> connector.onMessageReadFailure( error ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( MessageReadFailureEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getError(), error );
    } );
  }

  @Test
  public void onSubscribeStarted()
    throws Exception
  {
    final TestConnector connector = TestConnector.create();

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final AreaOfInterest areaOfInterest =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED ) );
    safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    safeAction( () -> connector.onSubscribeStarted( address ) );

    safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.LOADING ) );
    safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

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
    throws Exception
  {
    final TestConnector connector = TestConnector.create();

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final AreaOfInterest areaOfInterest =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED ) );
    safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    final Subscription subscription =
      safeAction( () -> Replicant.context().createSubscription( address, null, true ) );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    safeAction( () -> connector.onSubscribeCompleted( address ) );

    safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.LOADED ) );
    safeAction( () -> assertEquals( areaOfInterest.getSubscription(), subscription ) );
    safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( SubscribeCompletedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @Test
  public void onSubscribeFailed()
    throws Exception
  {
    final TestConnector connector = TestConnector.create();

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final AreaOfInterest areaOfInterest =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED ) );
    safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    final Throwable error = new Throwable();

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    safeAction( () -> connector.onSubscribeFailed( address, error ) );

    safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.LOAD_FAILED ) );
    safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    safeAction( () -> assertEquals( areaOfInterest.getError(), error ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( SubscribeFailedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
      assertEquals( e.getError(), error );
    } );
  }

  @Test
  public void onUnsubscribeStarted()
    throws Exception
  {
    final TestConnector connector = TestConnector.create();

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final AreaOfInterest areaOfInterest =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED ) );
    safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    final Subscription subscription =
      safeAction( () -> Replicant.context().createSubscription( address, null, true ) );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    safeAction( () -> connector.onUnsubscribeStarted( address ) );

    safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.UNLOADING ) );
    safeAction( () -> assertEquals( areaOfInterest.getSubscription(), subscription ) );
    safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

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
    throws Exception
  {
    final TestConnector connector = TestConnector.create();

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final AreaOfInterest areaOfInterest =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED ) );
    safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    safeAction( () -> connector.onUnsubscribeCompleted( address ) );

    safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.UNLOADED ) );
    safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( UnsubscribeCompletedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @Test
  public void onUnsubscribeFailed()
    throws Exception
  {
    final TestConnector connector = TestConnector.create();

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final AreaOfInterest areaOfInterest =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED ) );
    safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    final Throwable error = new Throwable();

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    safeAction( () -> connector.onUnsubscribeFailed( address, error ) );

    safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.UNLOADED ) );
    safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( UnsubscribeFailedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
      assertEquals( e.getError(), error );
    } );
  }

  @Test
  public void onSubscriptionUpdateStarted()
    throws Exception
  {
    final TestConnector connector = TestConnector.create();

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final AreaOfInterest areaOfInterest =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED ) );
    safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    final Subscription subscription =
      safeAction( () -> Replicant.context().createSubscription( address, null, true ) );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    safeAction( () -> connector.onSubscriptionUpdateStarted( address ) );

    safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.UPDATING ) );
    safeAction( () -> assertEquals( areaOfInterest.getSubscription(), subscription ) );
    safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

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
    throws Exception
  {
    final TestConnector connector = TestConnector.create();

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final AreaOfInterest areaOfInterest =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED ) );
    safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    final Subscription subscription =
      safeAction( () -> Replicant.context().createSubscription( address, null, true ) );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    safeAction( () -> connector.onSubscriptionUpdateCompleted( address ) );

    safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.UPDATED ) );
    safeAction( () -> assertEquals( areaOfInterest.getSubscription(), subscription ) );
    safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( SubscriptionUpdateCompletedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @Test
  public void onSubscriptionUpdateFailed()
    throws Exception
  {
    final TestConnector connector = TestConnector.create();

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final AreaOfInterest areaOfInterest =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED ) );
    safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    final Throwable error = new Throwable();

    final Subscription subscription =
      safeAction( () -> Replicant.context().createSubscription( address, null, true ) );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    safeAction( () -> connector.onSubscriptionUpdateFailed( address, error ) );

    safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.UPDATE_FAILED ) );
    safeAction( () -> assertEquals( areaOfInterest.getSubscription(), subscription ) );
    safeAction( () -> assertEquals( areaOfInterest.getError(), error ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( SubscriptionUpdateFailedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
      assertEquals( e.getError(), error );
    } );
  }

  @Test
  public void areaOfInterestRequestPendingQueries()
  {
    final TestConnector connector = TestConnector.create();

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final String filter = ValueUtil.randomString();

    assertEquals( connector.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.ADD, address, filter ), false );
    assertEquals( connector.lastIndexOfPendingAreaOfInterestRequest( AreaOfInterestRequest.Type.ADD, address, filter ),
                  -1 );

    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    assertEquals( connector.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.ADD, address, filter ), false );
    assertEquals( connector.lastIndexOfPendingAreaOfInterestRequest( AreaOfInterestRequest.Type.ADD, address, filter ),
                  -1 );

    connection.requestSubscribe( address, filter );

    assertEquals( connector.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.ADD, address, filter ), true );
    assertEquals( connector.lastIndexOfPendingAreaOfInterestRequest( AreaOfInterestRequest.Type.ADD, address, filter ),
                  1 );
  }

  @Test
  public void connection()
  {
    final TestConnector connector = TestConnector.create();

    assertEquals( connector.getConnection(), null );

    final Connection connection = new Connection( connector, ValueUtil.randomString() );

    connector.setConnection( connection );

    final Subscription subscription1 =
      safeAction( () -> Replicant.context().createSubscription( new ChannelAddress( 1, 0 ), null, true ) );

    assertEquals( connector.getConnection(), connection );
    assertEquals( connector.ensureConnection(), connection );
    assertEquals( Disposable.isDisposed( subscription1 ), false );

    connector.setConnection( null );

    assertEquals( connector.getConnection(), null );
    assertEquals( Disposable.isDisposed( subscription1 ), true );
  }

  @Test
  public void ensureConnection_WhenNoConnection()
  {
    final TestConnector connector = TestConnector.create();

    final IllegalStateException exception = expectThrows( IllegalStateException.class, connector::ensureConnection );

    assertEquals( exception.getMessage(),
                  "Replicant-0031: Connector.ensureConnection() when no connection is present." );
  }

  @Test
  public void purgeSubscriptions()
  {
    final TestConnector connector1 = TestConnector.create( newSchema( 1 ) );
    TestConnector.create( newSchema( 2 ) );

    final Subscription subscription1 =
      safeAction( () -> Replicant.context().createSubscription( new ChannelAddress( 1, 0 ), null, true ) );
    final Subscription subscription2 =
      safeAction( () -> Replicant.context().createSubscription( new ChannelAddress( 1, 1, 2 ), null, true ) );
    // The next two are from a different Connector
    final Subscription subscription3 =
      safeAction( () -> Replicant.context().createSubscription( new ChannelAddress( 2, 0, 1 ), null, true ) );
    final Subscription subscription4 =
      safeAction( () -> Replicant.context().createSubscription( new ChannelAddress( 2, 0, 2 ), null, true ) );

    assertEquals( Disposable.isDisposed( subscription1 ), false );
    assertEquals( Disposable.isDisposed( subscription2 ), false );
    assertEquals( Disposable.isDisposed( subscription3 ), false );
    assertEquals( Disposable.isDisposed( subscription4 ), false );

    connector1.purgeSubscriptions();

    assertEquals( Disposable.isDisposed( subscription1 ), true );
    assertEquals( Disposable.isDisposed( subscription2 ), true );
    assertEquals( Disposable.isDisposed( subscription3 ), false );
    assertEquals( Disposable.isDisposed( subscription4 ), false );
  }

  @Test
  public void triggerScheduler()
  {
    final TestConnector connector = TestConnector.create();

    assertEquals( connector.isSchedulerActive(), false );
    assertEquals( connector.getActivateSchedulerCount(), 0 );

    connector.triggerScheduler();

    assertEquals( connector.isSchedulerActive(), true );
    assertEquals( connector.getActivateSchedulerCount(), 1 );

    connector.triggerScheduler();

    assertEquals( connector.isSchedulerActive(), true );
    assertEquals( connector.getActivateSchedulerCount(), 1 );
  }

  @Test
  public void scheduleTick()
  {
    final TestConnector connector = TestConnector.create();
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final MessageResponse response = new MessageResponse( ValueUtil.randomString() );
    connection.setCurrentMessageResponse( response );
    response.recordChangeSet( ChangeSet.create( ValueUtil.randomInt(), null, null ), null );

    connector.triggerScheduler();

    assertNull( connector.getSchedulerLock() );

    //response needs worldValidated

    final boolean result1 = connector.scheduleTick();

    assertEquals( result1, true );
    final Disposable schedulerLock1 = connector.getSchedulerLock();
    assertNotNull( schedulerLock1 );

    final boolean result2 = connector.scheduleTick();

    assertEquals( result2, true );
    assertNotNull( connector.getSchedulerLock() );
    // Current message should be nulled and completed processing now
    assertNull( connection.getCurrentMessageResponse() );

    final boolean result3 = connector.scheduleTick();

    assertEquals( result3, false );
    assertNull( connector.getSchedulerLock() );
    assertTrue( Disposable.isDisposed( schedulerLock1 ) );
  }

  @Test
  public void scheduleTick_withError()
  {
    final TestConnector connector = TestConnector.create();
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    connector.triggerScheduler();

    connection.injectCurrentAreaOfInterestRequest( new AreaOfInterestRequest( new ChannelAddress( 0, 0 ),
                                                                              AreaOfInterestRequest.Type.REMOVE,
                                                                              null ) );
    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final boolean result2 = connector.scheduleTick();

    assertEquals( result2, false );

    assertNull( connector.getSchedulerLock() );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( MessageProcessFailureEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getError().getMessage(),
                    "Replicant-0046: Request to unsubscribe from channel at address 0.0 but not subscribed to channel." );
    } );
  }

  @Test
  public void requestSubscribe()
  {
    final TestConnector connector = TestConnector.create();
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );

    final ChannelAddress address = new ChannelAddress( 1, 0 );

    assertEquals( connector.getActivateSchedulerCount(), 0 );
    assertEquals( connector.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.ADD, address, null ), false );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    connector.requestSubscribe( address, null );

    assertEquals( connector.getActivateSchedulerCount(), 1 );
    assertEquals( connector.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.ADD, address, null ), true );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscribeRequestQueuedEvent.class, e -> assertEquals( e.getAddress(), address ) );
  }

  @Test
  public void requestSubscriptionUpdate()
  {
    final TestConnector connector = TestConnector.create();
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );

    final ChannelAddress address = new ChannelAddress( 1, 0 );

    assertEquals( connector.getActivateSchedulerCount(), 0 );
    assertEquals( connector.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.UPDATE, address, null ), false );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    connector.requestSubscriptionUpdate( address, null );

    assertEquals( connector.getActivateSchedulerCount(), 1 );
    assertEquals( connector.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.UPDATE, address, null ), true );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscriptionUpdateRequestQueuedEvent.class,
                             e -> assertEquals( e.getAddress(), address ) );
  }

  @Test
  public void requestUnsubscribe()
  {
    final TestConnector connector = TestConnector.create();
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );

    final ChannelAddress address = new ChannelAddress( 1, 0 );

    assertEquals( connector.getActivateSchedulerCount(), 0 );
    assertEquals( connector.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.REMOVE, address, null ), false );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    connector.requestUnsubscribe( address );

    assertEquals( connector.getActivateSchedulerCount(), 1 );
    assertEquals( connector.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.REMOVE, address, null ), true );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( UnsubscribeRequestQueuedEvent.class,
                             e -> assertEquals( e.getAddress(), address ) );
  }

  @Test
  public void updateSubscriptionForFilteredEntities()
  {
    final TestConnector connector = TestConnector.create();
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );

    final ChannelAddress address1 = new ChannelAddress( 1, 1, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 1, 2 );

    final Subscription subscription1 =
      safeAction( () -> Replicant.context().createSubscription( address1, ValueUtil.randomString(), true ) );
    final Subscription subscription2 =
      safeAction( () -> Replicant.context().createSubscription( address2, ValueUtil.randomString(), true ) );

    // Use Integer and String as arbitrary types for our entities...
    // Anything with id below 0 will be removed during update ...
    final Entity entity1 = safeAction( () -> Replicant.context().findOrCreateEntity( Integer.class, -1 ) );
    final Entity entity2 = safeAction( () -> Replicant.context().findOrCreateEntity( Integer.class, -2 ) );
    final Entity entity3 = safeAction( () -> Replicant.context().findOrCreateEntity( Integer.class, -3 ) );
    final Entity entity4 = safeAction( () -> Replicant.context().findOrCreateEntity( Integer.class, -4 ) );
    final Entity entity5 = safeAction( () -> Replicant.context().findOrCreateEntity( String.class, 5 ) );
    final Entity entity6 = safeAction( () -> Replicant.context().findOrCreateEntity( String.class, 6 ) );

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
      assertEquals( Disposable.isDisposed( entity1 ), true );
      assertEquals( Disposable.isDisposed( entity2 ), true );
      assertEquals( Disposable.isDisposed( entity3 ), false );
      assertEquals( Disposable.isDisposed( entity4 ), false );
      assertEquals( Disposable.isDisposed( entity5 ), false );
      assertEquals( Disposable.isDisposed( entity6 ), false );

      assertEquals( subscription1.getEntities().size(), 1 );
      assertEquals( subscription1.findAllEntitiesByType( Integer.class ).size(), 0 );
      assertEquals( subscription1.findAllEntitiesByType( String.class ).size(), 2 );
      assertEquals( subscription2.getEntities().size(), 1 );
      assertEquals( subscription2.findAllEntitiesByType( Integer.class ).size(), 2 );
    } );
  }

  @Test
  public void toAddress()
  {
    final TestConnector connector = TestConnector.create();
    assertEquals( connector.toAddress( ChannelChange.create( 0, ChannelChange.Action.ADD, null ) ),
                  new ChannelAddress( 1, 0 ) );
    assertEquals( connector.toAddress( ChannelChange.create( 1, 2, ChannelChange.Action.ADD, null ) ),
                  new ChannelAddress( 1, 1, 2 ) );
  }

  @Test
  public void processEntityChanges()
  {
    final int schemaId = 1;
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), true, ChannelSchema.FilterType.NONE, false, true );
    final EntitySchema entitySchema = new EntitySchema( 0, ValueUtil.randomString(), MyEntity.class );
    final SystemSchema schema =
      new SystemSchema( schemaId,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{ entitySchema } );
    final TestConnector connector = TestConnector.create( schema );
    connector.setLinksToProcessPerTick( 1 );

    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );
    final MessageResponse response = new MessageResponse( ValueUtil.randomString() );
    connection.setCurrentMessageResponse( response );

    final Linkable userObject1 = mock( Linkable.class );
    final Linkable userObject2 = mock( Linkable.class );

    // Pause scheduler to avoid converge of subscriptions
    pauseScheduler();

    final ChannelAddress address = new ChannelAddress( connector.getSchema().getId(), 1 );
    safeAction( () -> Replicant.context().createSubscription( address, null, true ) );

    // This entity is to be updated
    final Entity entity2 = safeAction( () -> Replicant.context().findOrCreateEntity( MyEntity.class, 2 ) );
    safeAction( () -> entity2.setUserObject( userObject2 ) );
    // This entity is to be removed
    final Entity entity3 = safeAction( () -> Replicant.context().findOrCreateEntity( MyEntity.class, 3 ) );

    final EntityChangeData data1 = mock( EntityChangeData.class );
    final EntityChangeData data2 = mock( EntityChangeData.class );
    final EntityChange[] entityChanges = {
      // Update changes
      EntityChange.create( 1, 0, new EntityChannel[]{ EntityChannel.create( 1 ) }, data1 ),
      EntityChange.create( 2, 0, new EntityChannel[]{ EntityChannel.create( 1 ) }, data2 ),
      // Remove change
      EntityChange.create( 3, 0, new EntityChannel[]{ EntityChannel.create( 1 ) } )
    };
    final ChangeSet changeSet = ChangeSet.create( ValueUtil.randomInt(), null, entityChanges );
    response.recordChangeSet( changeSet, null );

    when( connector.getChangeMapper().createEntity( entitySchema, 1, data1 ) ).thenReturn( userObject1 );

    assertEquals( response.getUpdatedEntities().size(), 0 );
    assertEquals( response.getEntityUpdateCount(), 0 );
    assertEquals( response.getEntityRemoveCount(), 0 );

    connector.setChangesToProcessPerTick( 1 );

    connector.processEntityChanges();

    verify( connector.getChangeMapper(), times( 1 ) ).createEntity( entitySchema, 1, data1 );
    verify( connector.getChangeMapper(), never() ).updateEntity( entitySchema, userObject1, data1 );
    verify( connector.getChangeMapper(), never() ).createEntity( entitySchema, 2, data2 );
    verify( connector.getChangeMapper(), never() ).updateEntity( entitySchema, userObject2, data2 );

    assertEquals( response.getUpdatedEntities().size(), 1 );
    assertEquals( response.getUpdatedEntities().contains( userObject1 ), true );
    assertEquals( response.getEntityUpdateCount(), 1 );
    assertEquals( response.getEntityRemoveCount(), 0 );

    connector.setChangesToProcessPerTick( 2 );

    connector.processEntityChanges();

    verify( connector.getChangeMapper(), times( 1 ) ).createEntity( entitySchema, 1, data1 );
    verify( connector.getChangeMapper(), never() ).updateEntity( entitySchema, userObject1, data1 );
    verify( connector.getChangeMapper(), never() ).createEntity( entitySchema, 2, data2 );
    verify( connector.getChangeMapper(), times( 1 ) ).updateEntity( entitySchema, userObject2, data2 );

    assertEquals( response.getUpdatedEntities().size(), 2 );
    assertEquals( response.getUpdatedEntities().contains( userObject1 ), true );
    assertEquals( response.getUpdatedEntities().contains( userObject2 ), true );
    assertEquals( response.getEntityUpdateCount(), 2 );
    assertEquals( response.getEntityRemoveCount(), 1 );
    assertEquals( Disposable.isDisposed( entity2 ), false );
    assertEquals( Disposable.isDisposed( entity3 ), true );
  }

  @Test
  public void processEntityChanges_referenceNonExistentSubscription()
  {
    final int schemaId = 1;
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), true, ChannelSchema.FilterType.NONE, false, true );
    final EntitySchema entitySchema = new EntitySchema( 0, ValueUtil.randomString(), MyEntity.class );
    final SystemSchema schema =
      new SystemSchema( schemaId,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{ entitySchema } );
    final TestConnector connector = TestConnector.create( schema );
    connector.setLinksToProcessPerTick( 1 );

    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );
    final MessageResponse response = new MessageResponse( ValueUtil.randomString() );
    connection.setCurrentMessageResponse( response );

    final Linkable userObject1 = mock( Linkable.class );

    // Pause scheduler to avoid converge of subscriptions
    pauseScheduler();

    final EntityChangeData data1 = mock( EntityChangeData.class );
    final EntityChange[] entityChanges = {
      EntityChange.create( 1, 0, new EntityChannel[]{ EntityChannel.create( 1 ) }, data1 )
    };
    final ChangeSet changeSet = ChangeSet.create( 42, null, entityChanges );
    response.recordChangeSet( changeSet, null );

    when( connector.getChangeMapper().createEntity( entitySchema, 1, data1 ) ).thenReturn( userObject1 );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, connector::processEntityChanges );
    assertEquals( exception.getMessage(),
                  "Replicant-0069: ChangeSet 42 contained an EntityChange message referencing channel 1.1 but no such subscription exists locally." );
  }

  @Test
  public void processEntityChanges_deleteNonExistingEntity()
  {
    final int schemaId = 1;
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), true, ChannelSchema.FilterType.NONE, false, true );
    final EntitySchema entitySchema = new EntitySchema( 0, ValueUtil.randomString(), MyEntity.class );
    final SystemSchema schema =
      new SystemSchema( schemaId,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{ entitySchema } );
    final TestConnector connector = TestConnector.create( schema );
    connector.setLinksToProcessPerTick( 1 );

    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );
    final MessageResponse response = new MessageResponse( ValueUtil.randomString() );
    connection.setCurrentMessageResponse( response );

    // Pause scheduler to avoid converge of subscriptions
    pauseScheduler();

    final EntityChange[] entityChanges = {
      // Remove change
      EntityChange.create( 3, 0, new EntityChannel[]{ EntityChannel.create( 1 ) } )
    };
    final ChangeSet changeSet = ChangeSet.create( 23, null, entityChanges );
    response.recordChangeSet( changeSet, null );

    connector.setChangesToProcessPerTick( 1 );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, connector::processEntityChanges );
    assertEquals( exception.getMessage(),
                  "Replicant-0068: ChangeSet 23 contained an EntityChange message to delete entity of type 0 and id 3 but no such entity exists locally." );
  }

  @Test
  public void processEntityLinks()
  {
    final TestConnector connector = TestConnector.create();
    connector.setLinksToProcessPerTick( 1 );

    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );
    final MessageResponse response = new MessageResponse( ValueUtil.randomString() );
    connection.setCurrentMessageResponse( response );

    final Linkable entity1 = mock( Linkable.class );
    final Linkable entity2 = mock( Linkable.class );
    final Linkable entity3 = mock( Linkable.class );
    final Linkable entity4 = mock( Linkable.class );

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
    final TestConnector connector = TestConnector.create();
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    connection.injectCurrentAreaOfInterestRequest( new AreaOfInterestRequest( new ChannelAddress( 1, 0 ),
                                                                              AreaOfInterestRequest.Type.ADD,
                                                                              null ) );

    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );
    assertEquals( connector.getActivateSchedulerCount(), 0 );

    connector.completeAreaOfInterestRequest();

    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), true );
    assertEquals( connector.getActivateSchedulerCount(), 1 );
  }

  @Test
  public void processChannelChanges_add()
  {
    final TestConnector connector = TestConnector.create();
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final MessageResponse response = new MessageResponse( ValueUtil.randomString() );
    connection.setCurrentMessageResponse( response );

    final int channelId = 0;
    final int subChannelId = ValueUtil.randomInt();
    final String filter = ValueUtil.randomString();
    final ChannelChange[] channelChanges =
      new ChannelChange[]{ ChannelChange.create( channelId, subChannelId, ChannelChange.Action.ADD, filter ) };
    response.recordChangeSet( ChangeSet.create( ValueUtil.randomInt(), channelChanges, null ), null );

    assertEquals( response.needsChannelChangesProcessed(), true );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    connector.processChannelChanges();

    assertEquals( response.needsChannelChangesProcessed(), false );

    final ChannelAddress address = new ChannelAddress( 1, channelId, subChannelId );
    final Subscription subscription =
      safeAction( () -> Replicant.context().findSubscription( address ) );
    assertNotNull( subscription );
    assertEquals( subscription.getAddress(), address );
    safeAction( () -> assertEquals( subscription.getFilter(), filter ) );
    safeAction( () -> assertEquals( subscription.isExplicitSubscription(), false ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscriptionCreatedEvent.class, e -> {
      assertEquals( e.getSubscription().getAddress(), address );
      safeAction( () -> assertEquals( e.getSubscription().getFilter(), filter ) );
    } );
  }

  @Test
  public void processChannelChanges_addConvertingImplicitToExplicit()
  {
    final TestConnector connector = TestConnector.create();
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final MessageResponse response = new MessageResponse( ValueUtil.randomString() );
    connection.setCurrentMessageResponse( response );

    final ChannelAddress address = new ChannelAddress( 1, 0, ValueUtil.randomInt() );
    final int channelId = address.getChannelId();
    final int subChannelId = Objects.requireNonNull( address.getId() );

    final String filter = ValueUtil.randomString();
    final ChannelChange[] channelChanges =
      new ChannelChange[]{ ChannelChange.create( channelId, subChannelId, ChannelChange.Action.ADD, filter ) };
    response.recordChangeSet( ChangeSet.create( ValueUtil.randomInt(), channelChanges, null ), null );

    final AreaOfInterestRequest request = new AreaOfInterestRequest( address, AreaOfInterestRequest.Type.ADD, filter );
    connection.injectCurrentAreaOfInterestRequest( request );
    request.markAsInProgress();

    assertEquals( response.needsChannelChangesProcessed(), true );
    assertEquals( response.getChannelAddCount(), 0 );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    connector.processChannelChanges();

    assertEquals( response.needsChannelChangesProcessed(), false );
    assertEquals( response.getChannelAddCount(), 1 );

    final Subscription subscription =
      safeAction( () -> Replicant.context().findSubscription( address ) );
    assertNotNull( subscription );
    assertEquals( subscription.getAddress(), address );
    safeAction( () -> assertEquals( subscription.getFilter(), filter ) );
    safeAction( () -> assertEquals( subscription.isExplicitSubscription(), true ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscriptionCreatedEvent.class, e -> {
      assertEquals( e.getSubscription().getAddress(), address );
      safeAction( () -> assertEquals( e.getSubscription().getFilter(), filter ) );
    } );
  }

  @Test
  public void processChannelChanges_remove()
  {
    final TestConnector connector = TestConnector.create();
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final MessageResponse response = new MessageResponse( ValueUtil.randomString() );
    connection.setCurrentMessageResponse( response );

    final ChannelAddress address = new ChannelAddress( 1, 0, ValueUtil.randomInt() );
    final int channelId = address.getChannelId();
    final int subChannelId = Objects.requireNonNull( address.getId() );

    final String filter = ValueUtil.randomString();
    final ChannelChange[] channelChanges =
      new ChannelChange[]{ ChannelChange.create( channelId, subChannelId, ChannelChange.Action.REMOVE, null ) };
    response.recordChangeSet( ChangeSet.create( ValueUtil.randomInt(), channelChanges, null ), null );

    final Subscription initialSubscription =
      safeAction( () -> Replicant.context().createSubscription( address, filter, true ) );

    assertEquals( response.needsChannelChangesProcessed(), true );
    assertEquals( response.getChannelRemoveCount(), 0 );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    connector.processChannelChanges();

    assertEquals( response.needsChannelChangesProcessed(), false );
    assertEquals( response.getChannelRemoveCount(), 1 );

    final Subscription subscription =
      safeAction( () -> Replicant.context().findSubscription( address ) );
    assertNull( subscription );
    assertEquals( Disposable.isDisposed( initialSubscription ), true );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscriptionDisposedEvent.class,
                             e -> assertEquals( e.getSubscription().getAddress(), address ) );
  }

  @Test
  public void processChannelChanges_remove_WithMissingSubscription()
  {
    final TestConnector connector = TestConnector.create();
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final MessageResponse response = new MessageResponse( ValueUtil.randomString() );
    connection.setCurrentMessageResponse( response );

    final ChannelAddress address = new ChannelAddress( 1, 0, 72 );
    final int channelId = address.getChannelId();
    final int subChannelId = Objects.requireNonNull( address.getId() );

    final ChannelChange[] channelChanges =
      new ChannelChange[]{ ChannelChange.create( channelId, subChannelId, ChannelChange.Action.REMOVE, null ) };
    response.recordChangeSet( ChangeSet.create( ValueUtil.randomInt(), channelChanges, null ), null );

    assertEquals( response.needsChannelChangesProcessed(), true );
    assertEquals( response.getChannelRemoveCount(), 0 );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, connector::processChannelChanges );

    assertEquals( response.needsChannelChangesProcessed(), true );
    assertEquals( response.getChannelRemoveCount(), 0 );
    assertEquals( exception.getMessage(),
                  "Replicant-0028: Received ChannelChange of type REMOVE for address 1.0.72 but no such subscription exists." );

    handler.assertEventCount( 0 );
  }

  @Test
  public void processChannelChanges_update()
  {
    final TestConnector connector = TestConnector.create();
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final MessageResponse response = new MessageResponse( ValueUtil.randomString() );
    connection.setCurrentMessageResponse( response );

    final ChannelAddress address = new ChannelAddress( 1, 0, ValueUtil.randomInt() );
    final int channelId = address.getChannelId();
    final int subChannelId = Objects.requireNonNull( address.getId() );

    final String oldFilter = ValueUtil.randomString();
    final String newFilter = ValueUtil.randomString();
    final ChannelChange[] channelChanges =
      new ChannelChange[]{ ChannelChange.create( channelId, subChannelId, ChannelChange.Action.UPDATE, newFilter ) };
    response.recordChangeSet( ChangeSet.create( ValueUtil.randomInt(), channelChanges, null ), null );

    final Subscription initialSubscription =
      safeAction( () -> Replicant.context().createSubscription( address, oldFilter, true ) );

    assertEquals( response.needsChannelChangesProcessed(), true );
    assertEquals( response.getChannelUpdateCount(), 0 );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    connector.processChannelChanges();

    assertEquals( response.needsChannelChangesProcessed(), false );
    assertEquals( response.getChannelUpdateCount(), 1 );

    final Subscription subscription =
      safeAction( () -> Replicant.context().findSubscription( address ) );
    assertNotNull( subscription );
    assertEquals( Disposable.isDisposed( initialSubscription ), false );

    handler.assertEventCount( 0 );
  }

  @Test
  public void processChannelChanges_update_implicitSubscription()
  {
    final TestConnector connector = TestConnector.create();
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final MessageResponse response = new MessageResponse( ValueUtil.randomString() );
    connection.setCurrentMessageResponse( response );

    final ChannelAddress address = new ChannelAddress( 1, 0, 33 );
    final int channelId = address.getChannelId();
    final int subChannelId = Objects.requireNonNull( address.getId() );

    final String oldFilter = ValueUtil.randomString();
    final String newFilter = ValueUtil.randomString();
    final ChannelChange[] channelChanges =
      new ChannelChange[]{ ChannelChange.create( channelId, subChannelId, ChannelChange.Action.UPDATE, newFilter ) };
    response.recordChangeSet( ChangeSet.create( ValueUtil.randomInt(), channelChanges, null ), null );

    safeAction( () -> Replicant.context().createSubscription( address, oldFilter, false ) );

    assertEquals( response.needsChannelChangesProcessed(), true );
    assertEquals( response.getChannelUpdateCount(), 0 );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, connector::processChannelChanges );

    assertEquals( response.needsChannelChangesProcessed(), true );
    assertEquals( response.getChannelUpdateCount(), 0 );

    assertEquals( exception.getMessage(),
                  "Replicant-0029: Received ChannelChange of type UPDATE for address 1.0.33 but subscription is implicitly subscribed." );

    handler.assertEventCount( 0 );
  }

  @Test
  public void processChannelChanges_update_missingSubscription()
  {
    final TestConnector connector = TestConnector.create();
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final MessageResponse response = new MessageResponse( ValueUtil.randomString() );
    connection.setCurrentMessageResponse( response );

    final ChannelAddress address = new ChannelAddress( 1, 0, 42 );
    final int channelId = address.getChannelId();
    final int subChannelId = Objects.requireNonNull( address.getId() );

    final String newFilter = ValueUtil.randomString();
    final ChannelChange[] channelChanges =
      new ChannelChange[]{ ChannelChange.create( channelId, subChannelId, ChannelChange.Action.UPDATE, newFilter ) };
    response.recordChangeSet( ChangeSet.create( ValueUtil.randomInt(), channelChanges, null ), null );

    assertEquals( response.needsChannelChangesProcessed(), true );
    assertEquals( response.getChannelUpdateCount(), 0 );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, connector::processChannelChanges );

    assertEquals( response.needsChannelChangesProcessed(), true );
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

    final TestConnector connector = TestConnector.create();

    final ChannelAddress address1 = new ChannelAddress( 1, 1, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 1, 2 );
    final ChannelAddress address3 = new ChannelAddress( 1, 1, 3 );

    final ArrayList<AreaOfInterestRequest> requests = new ArrayList<>();
    requests.add( new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.REMOVE, null ) );
    requests.add( new AreaOfInterestRequest( address2, AreaOfInterestRequest.Type.REMOVE, null ) );
    requests.add( new AreaOfInterestRequest( address3, AreaOfInterestRequest.Type.REMOVE, null ) );

    final Subscription subscription1 =
      safeAction( () -> Replicant.context().createSubscription( address1, null, true ) );
    // Address2 is already implicit ...
    safeAction( () -> Replicant.context().createSubscription( address2, null, false ) );
    // Address3 has no subscription ... maybe not converged yet

    safeAction( () -> connector.removeExplicitSubscriptions( requests ) );

    safeAction( () -> assertEquals( subscription1.isExplicitSubscription(), false ) );
  }

  @Test
  public void removeExplicitSubscriptions_passedBadAction()
  {
    // Pause converger
    pauseScheduler();

    final TestConnector connector = TestConnector.create();

    final ChannelAddress address1 = new ChannelAddress( 1, 1, 1 );

    final ArrayList<AreaOfInterestRequest> requests = new ArrayList<>();
    requests.add( new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.ADD, null ) );

    safeAction( () -> Replicant.context().createSubscription( address1, null, true ) );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> connector.removeExplicitSubscriptions( requests ) ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0034: Connector.removeExplicitSubscriptions() invoked with request with type that is not REMOVE. Request: AreaOfInterestRequest[Type=ADD Address=1.1.1]" );
  }

  @Test
  public void removeUnneededAddRequests_upgradeExisting()
  {
    final TestConnector connector = TestConnector.create();

    final ChannelAddress address1 = new ChannelAddress( 1, 1, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 1, 2 );

    final ArrayList<AreaOfInterestRequest> requests = new ArrayList<>();
    final AreaOfInterestRequest request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.ADD, null );
    final AreaOfInterestRequest request2 =
      new AreaOfInterestRequest( address2, AreaOfInterestRequest.Type.ADD, null );
    requests.add( request1 );
    requests.add( request2 );

    requests.forEach( AreaOfInterestRequest::markAsInProgress );

    // Address1 is implicitly subscribed
    final Subscription subscription1 =
      safeAction( () -> Replicant.context().createSubscription( address1, null, false ) );

    safeAction( () -> connector.removeUnneededAddRequests( requests ) );

    assertEquals( requests.size(), 1 );
    assertEquals( requests.contains( request2 ), true );
    assertEquals( request1.isInProgress(), false );
    assertEquals( request2.isInProgress(), true );
    safeAction( () -> assertEquals( subscription1.isExplicitSubscription(), true ) );
  }

  @Test
  public void removeUnneededAddRequests_explicitAlreadyPresent()
  {
    final TestConnector connector = TestConnector.create();

    final ChannelAddress address1 = new ChannelAddress( 1, 1, 1 );

    final ArrayList<AreaOfInterestRequest> requests = new ArrayList<>();
    final AreaOfInterestRequest request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.ADD, null );
    requests.add( request1 );

    requests.forEach( AreaOfInterestRequest::markAsInProgress );

    safeAction( () -> Replicant.context().createSubscription( address1, null, true ) );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> connector.removeUnneededAddRequests( requests ) ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0030: Request to add channel at address 1.1.1 but already explicitly subscribed to channel." );
  }

  @Test
  public void removeUnneededRemoveRequests_whenInvariantsDisabled()
  {
    final TestConnector connector = TestConnector.create();

    final ChannelAddress address1 = new ChannelAddress( 1, 1, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 1, 2 );
    final ChannelAddress address3 = new ChannelAddress( 1, 1, 3 );

    final ArrayList<AreaOfInterestRequest> requests = new ArrayList<>();
    final AreaOfInterestRequest request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.REMOVE, null );
    final AreaOfInterestRequest request2 =
      new AreaOfInterestRequest( address2, AreaOfInterestRequest.Type.REMOVE, null );
    final AreaOfInterestRequest request3 =
      new AreaOfInterestRequest( address3, AreaOfInterestRequest.Type.REMOVE, null );
    requests.add( request1 );
    requests.add( request2 );
    requests.add( request3 );

    requests.forEach( AreaOfInterestRequest::markAsInProgress );

    safeAction( () -> Replicant.context().createSubscription( address1, null, true ) );
    // Address2 is already implicit ...
    safeAction( () -> Replicant.context().createSubscription( address2, null, false ) );
    // Address3 has no subscription ... maybe not converged yet

    ReplicantTestUtil.noCheckInvariants();

    safeAction( () -> connector.removeUnneededRemoveRequests( requests ) );

    assertEquals( requests.size(), 1 );
    assertEquals( requests.contains( request1 ), true );
    assertEquals( request1.isInProgress(), true );
    assertEquals( request2.isInProgress(), false );
    assertEquals( request3.isInProgress(), false );
  }

  @Test
  public void removeUnneededRemoveRequests_noSubscription()
  {
    final TestConnector connector = TestConnector.create();

    final ChannelAddress address1 = new ChannelAddress( 1, 1, 1 );

    final ArrayList<AreaOfInterestRequest> requests = new ArrayList<>();
    final AreaOfInterestRequest request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.REMOVE, null );
    requests.add( request1 );

    requests.forEach( AreaOfInterestRequest::markAsInProgress );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> connector.removeUnneededRemoveRequests( requests ) ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0046: Request to unsubscribe from channel at address 1.1.1 but not subscribed to channel." );
  }

  @Test
  public void removeUnneededRemoveRequests_implicitSubscription()
  {
    final TestConnector connector = TestConnector.create();

    final ChannelAddress address1 = new ChannelAddress( 1, 1, 1 );

    final ArrayList<AreaOfInterestRequest> requests = new ArrayList<>();
    final AreaOfInterestRequest request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.REMOVE, null );
    requests.add( request1 );

    requests.forEach( AreaOfInterestRequest::markAsInProgress );

    safeAction( () -> Replicant.context().createSubscription( address1, null, false ) );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> connector.removeUnneededRemoveRequests( requests ) ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0047: Request to unsubscribe from channel at address 1.1.1 but subscription is not an explicit subscription." );
  }

  @Test
  public void removeUnneededUpdateRequests_whenInvariantsDisabled()
  {
    final TestConnector connector = TestConnector.create();

    final ChannelAddress address1 = new ChannelAddress( 1, 1, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 1, 2 );
    final ChannelAddress address3 = new ChannelAddress( 1, 1, 3 );

    final ArrayList<AreaOfInterestRequest> requests = new ArrayList<>();
    final AreaOfInterestRequest request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.UPDATE, null );
    final AreaOfInterestRequest request2 =
      new AreaOfInterestRequest( address2, AreaOfInterestRequest.Type.UPDATE, null );
    final AreaOfInterestRequest request3 =
      new AreaOfInterestRequest( address3, AreaOfInterestRequest.Type.UPDATE, null );
    requests.add( request1 );
    requests.add( request2 );
    requests.add( request3 );

    requests.forEach( AreaOfInterestRequest::markAsInProgress );

    safeAction( () -> Replicant.context().createSubscription( address1, null, true ) );
    // Address2 is already implicit ...
    safeAction( () -> Replicant.context().createSubscription( address2, null, false ) );
    // Address3 has no subscription ... maybe not converged yet

    ReplicantTestUtil.noCheckInvariants();

    safeAction( () -> connector.removeUnneededUpdateRequests( requests ) );

    assertEquals( requests.size(), 2 );
    assertEquals( requests.contains( request1 ), true );
    assertEquals( requests.contains( request2 ), true );
    assertEquals( request1.isInProgress(), true );
    assertEquals( request2.isInProgress(), true );
    assertEquals( request3.isInProgress(), false );
  }

  @Test
  public void removeUnneededUpdateRequests_noSubscription()
  {
    final TestConnector connector = TestConnector.create();

    final ChannelAddress address1 = new ChannelAddress( 1, 1, 1 );

    final ArrayList<AreaOfInterestRequest> requests = new ArrayList<>();
    final AreaOfInterestRequest request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.UPDATE, null );
    requests.add( request1 );

    requests.forEach( AreaOfInterestRequest::markAsInProgress );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> connector.removeUnneededUpdateRequests( requests ) ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0048: Request to update channel at address 1.1.1 but not subscribed to channel." );
  }

  @Test
  public void validateWorld_invalidEntity()
  {
    final TestConnector connector = TestConnector.create();
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );
    final MessageResponse response = new MessageResponse( ValueUtil.randomString() );
    connector.ensureConnection().setCurrentMessageResponse( response );

    assertEquals( response.hasWorldBeenValidated(), false );

    final EntityService entityService = Replicant.context().getEntityService();
    final Entity entity1 =
      safeAction( () -> entityService.findOrCreateEntity( "MyEntity/1", MyEntity.class, 1 ) );
    final Exception error = new Exception();
    safeAction( () -> entity1.setUserObject( new MyEntity( error ) ) );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, connector::validateWorld );

    assertEquals( exception.getMessage(),
                  "Replicant-0065: Entity failed to verify during validation process. Entity = MyEntity/1" );

    assertEquals( response.hasWorldBeenValidated(), true );
  }

  @Test
  public void validateWorld_invalidEntity_ignoredIfCOmpileSettingDisablesValidation()
  {
    ReplicantTestUtil.noValidateEntitiesOnLoad();
    final TestConnector connector = TestConnector.create();
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );
    final MessageResponse response = new MessageResponse( ValueUtil.randomString() );
    connector.ensureConnection().setCurrentMessageResponse( response );

    assertEquals( response.hasWorldBeenValidated(), true );

    final EntityService entityService = Replicant.context().getEntityService();
    final Entity entity1 =
      safeAction( () -> entityService.findOrCreateEntity( "MyEntity/1", MyEntity.class, 1 ) );
    final Exception error = new Exception();
    safeAction( () -> entity1.setUserObject( new MyEntity( error ) ) );

    connector.validateWorld();

    assertEquals( response.hasWorldBeenValidated(), true );
  }

  @Test
  public void validateWorld_validEntity()
  {
    final TestConnector connector = TestConnector.create();
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );
    final MessageResponse response = new MessageResponse( ValueUtil.randomString() );
    connector.ensureConnection().setCurrentMessageResponse( response );

    assertEquals( response.hasWorldBeenValidated(), false );

    final EntityService entityService = Replicant.context().getEntityService();
    safeAction( () -> entityService.findOrCreateEntity( "MyEntity/1", MyEntity.class, 1 ) );

    connector.validateWorld();

    assertEquals( response.hasWorldBeenValidated(), true );
  }

  static class MyEntity
    implements Verifiable
  {
    @Nullable
    private final Exception _exception;

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
  public void parseMessageResponse_basicMessage()
  {
    final TestConnector connector = TestConnector.create();
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    @Language( "json" )
    final String rawJsonData = "{\"last_id\": 1}";
    final MessageResponse response = new MessageResponse( rawJsonData );

    connection.setCurrentMessageResponse( response );
    assertEquals( connection.getPendingResponses().size(), 0 );

    connector.parseMessageResponse();

    assertEquals( response.getRawJsonData(), null );
    final ChangeSet changeSet = response.getChangeSet();
    assertNotNull( changeSet );
    assertEquals( changeSet.getSequence(), 1 );
    assertNull( response.getRequest() );

    assertEquals( connection.getPendingResponses().size(), 1 );
    assertEquals( connection.getCurrentMessageResponse(), null );
  }

  @Test
  public void parseMessageResponse_requestPresent()
  {
    final TestConnector connector = TestConnector.create();
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final RequestEntry request = connection.newRequest( "SomeAction", null );

    final int requestId = request.getRequestId();

    @Language( "json" )
    final String rawJsonData = "{\"last_id\": 1, \"requestId\": " + requestId + "}";
    final MessageResponse response = new MessageResponse( rawJsonData );

    connection.setCurrentMessageResponse( response );
    assertEquals( connection.getPendingResponses().size(), 0 );

    connector.parseMessageResponse();

    assertEquals( response.getRawJsonData(), null );
    final ChangeSet changeSet = response.getChangeSet();
    assertNotNull( changeSet );
    assertEquals( changeSet.getSequence(), 1 );
    assertEquals( response.getRequest(), request );

    assertEquals( connection.getPendingResponses().size(), 1 );
    assertEquals( connection.getCurrentMessageResponse(), null );
  }

  @Test
  public void parseMessageResponse_cacheResult()
  {
    final TestConnector connector = TestConnector.create();
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final String cacheKey = ValueUtil.randomString();
    final RequestEntry request = connection.newRequest( "SomeAction", cacheKey );

    final int requestId = request.getRequestId();

    final String etag = ValueUtil.randomString();

    final String rawJsonData =
      "{\"last_id\": 1, \"requestId\": " + requestId + ", \"etag\": \"" + etag + "\"}";
    final MessageResponse response = new MessageResponse( rawJsonData );

    connection.setCurrentMessageResponse( response );
    assertEquals( connection.getPendingResponses().size(), 0 );

    final CacheService cacheService = new TestCacheService();
    Replicant.context().setCacheService( cacheService );
    assertNull( cacheService.lookup( cacheKey ) );

    connector.parseMessageResponse();

    assertEquals( response.getRawJsonData(), null );
    final ChangeSet changeSet = response.getChangeSet();
    assertNotNull( changeSet );
    assertEquals( changeSet.getSequence(), 1 );
    assertEquals( response.getRequest(), request );

    assertEquals( connection.getPendingResponses().size(), 1 );
    assertEquals( connection.getCurrentMessageResponse(), null );

    final CacheEntry entry = cacheService.lookup( cacheKey );
    assertNotNull( entry );
    assertEquals( entry.getKey(), cacheKey );
    assertEquals( entry.getETag(), etag );
    assertEquals( entry.getContent(), rawJsonData );
  }

  @Test
  public void parseMessageResponse_oob()
  {
    final TestConnector connector = TestConnector.create();
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final RequestEntry request = connection.newRequest( "SomeAction", null );

    final int requestId = request.getRequestId();

    @Language( "json" )
    final String rawJsonData = "{\"last_id\": 1, \"requestId\": " + requestId + "}";
    final SafeProcedure oobCompletionAction = () -> {
    };
    final MessageResponse response = new MessageResponse( rawJsonData, oobCompletionAction );

    connection.setCurrentMessageResponse( response );
    assertEquals( connection.getPendingResponses().size(), 0 );

    connector.parseMessageResponse();

    assertEquals( response.getRawJsonData(), null );
    final ChangeSet changeSet = response.getChangeSet();
    assertNotNull( changeSet );
    assertEquals( changeSet.getSequence(), 1 );
    assertNull( response.getRequest() );

    assertEquals( connection.getPendingResponses().size(), 1 );
    assertEquals( connection.getCurrentMessageResponse(), null );
  }

  @Test
  public void parseMessageResponse_invalidRequestId()
  {
    final TestConnector connector = TestConnector.create();
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    @Language( "json" )
    final String rawJsonData = "{\"last_id\": 1, \"requestId\": 22}";
    final MessageResponse response = new MessageResponse( rawJsonData );

    connection.setCurrentMessageResponse( response );
    assertEquals( connection.getPendingResponses().size(), 0 );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, connector::parseMessageResponse );

    assertEquals( exception.getMessage(),
                  "Replicant-0066: Unable to locate request with id '22' specified for ChangeSet with sequence 1. Existing Requests: {}" );
  }

  @Test
  public void completeMessageResponse()
  {
    final TestConnector connector = TestConnector.create();
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final MessageResponse response = new MessageResponse( "" );

    final ChangeSet changeSet = ChangeSet.create( 23, null, null );
    response.recordChangeSet( changeSet, null );

    connection.setLastRxSequence( 22 );
    connection.setCurrentMessageResponse( response );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    connector.completeMessageResponse();

    assertEquals( connection.getLastRxSequence(), 23 );
    assertEquals( connection.getCurrentMessageResponse(), null );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( MessageProcessedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getSchemaName(), connector.getSchema().getName() );
      assertEquals( e.getDataLoadStatus().getSequence(), changeSet.getSequence() );
    } );
  }

  @Test
  public void completeMessageResponse_withPostAction()
  {
    final TestConnector connector = TestConnector.create();
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final MessageResponse response = new MessageResponse( "" );

    final ChangeSet changeSet = ChangeSet.create( 23, null, null );
    response.recordChangeSet( changeSet, null );

    connection.setLastRxSequence( 22 );
    connection.setCurrentMessageResponse( response );

    final AtomicInteger postActionCallCount = new AtomicInteger();
    connector.setPostMessageResponseAction( postActionCallCount::incrementAndGet );

    assertEquals( postActionCallCount.get(), 0 );

    connector.completeMessageResponse();

    assertEquals( postActionCallCount.get(), 1 );
  }

  @Test
  public void completeMessageResponse_OOBMessage()
  {
    final TestConnector connector = TestConnector.create();
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final AtomicInteger completionCallCount = new AtomicInteger();
    final MessageResponse response = new MessageResponse( "", completionCallCount::incrementAndGet );

    final ChangeSet changeSet = ChangeSet.create( 23, 1234, null, null, null );
    response.recordChangeSet( changeSet, null );

    connection.setLastRxSequence( 22 );
    connection.setCurrentMessageResponse( response );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    assertEquals( completionCallCount.get(), 0 );

    connector.completeMessageResponse();

    assertEquals( completionCallCount.get(), 1 );
    assertEquals( connection.getLastRxSequence(), 22 );
    assertEquals( connection.getCurrentMessageResponse(), null );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( MessageProcessedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getSchemaName(), connector.getSchema().getName() );
      assertEquals( e.getDataLoadStatus().getSequence(), changeSet.getSequence() );
    } );
  }

  @Test
  public void completeMessageResponse_MessageWithRequest_RPCComplete()
  {
    final TestConnector connector = TestConnector.create();
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final RequestEntry request = connection.newRequest( "SomeAction", null );

    final AtomicInteger completionCalled = new AtomicInteger();
    final int requestId = request.getRequestId();
    request.setNormalCompletion( true );
    request.setCompletionAction( completionCalled::incrementAndGet );

    final MessageResponse response = new MessageResponse( "" );

    final ChangeSet changeSet = ChangeSet.create( 23, requestId, null, null, null );
    response.recordChangeSet( changeSet, request );

    connection.setLastRxSequence( 22 );
    connection.setCurrentMessageResponse( response );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    assertEquals( request.haveResultsArrived(), false );
    assertEquals( completionCalled.get(), 0 );
    assertEquals( connection.getRequest( requestId ), request );

    connector.completeMessageResponse();

    assertEquals( request.haveResultsArrived(), true );
    assertEquals( connection.getLastRxSequence(), 23 );
    assertEquals( connection.getCurrentMessageResponse(), null );
    assertEquals( completionCalled.get(), 1 );
    assertEquals( connection.getRequest( requestId ), null );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( MessageProcessedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getSchemaName(), connector.getSchema().getName() );
      assertEquals( e.getDataLoadStatus().getSequence(), changeSet.getSequence() );
    } );
  }

  @Test
  public void completeMessageResponse_MessageWithRequest_RPCNotComplete()
  {
    final TestConnector connector = TestConnector.create();
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final RequestEntry request = connection.newRequest( "SomeAction", null );

    final AtomicInteger completionCalled = new AtomicInteger();
    final int requestId = request.getRequestId();

    final MessageResponse response = new MessageResponse( "" );

    final ChangeSet changeSet = ChangeSet.create( 23, requestId, null, null, null );
    response.recordChangeSet( changeSet, request );

    connection.setLastRxSequence( 22 );
    connection.setCurrentMessageResponse( response );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    assertEquals( request.haveResultsArrived(), false );
    assertEquals( completionCalled.get(), 0 );
    assertEquals( connection.getRequest( requestId ), request );

    connector.completeMessageResponse();

    assertEquals( request.haveResultsArrived(), true );
    assertEquals( connection.getLastRxSequence(), 23 );
    assertEquals( connection.getCurrentMessageResponse(), null );
    assertEquals( completionCalled.get(), 0 );
    assertEquals( connection.getRequest( requestId ), request );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( MessageProcessedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getSchemaName(), connector.getSchema().getName() );
      assertEquals( e.getDataLoadStatus().getSequence(), changeSet.getSequence() );
    } );
  }

  @SuppressWarnings( "ResultOfMethodCallIgnored" )
  @Test
  public void progressResponseProcessing()
  {
    /*
     * This test steps through each stage of a message processing.
     */

    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), true, ChannelSchema.FilterType.NONE, false, true );
    final EntitySchema entitySchema = new EntitySchema( 0, ValueUtil.randomString(), MyEntity.class );
    final SystemSchema schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{ entitySchema } );

    final TestConnector connector = TestConnector.create( schema );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );

    connector.setConnection( connection );

    @Language( "json" )
    final String rawJsonData =
      "{" +
      "\"last_id\": 1, " +
      // Add Channel 0
      "\"channel_actions\": [ { \"cid\": 0, \"action\": \"add\"} ], " +
      // Add Entity 1 of type 0 from channel 0
      "\"changes\": [{\"id\": 1,\"type\":0,\"channels\":[{\"cid\": 0}], \"data\":{}}] " +
      "}";
    connection.enqueueResponse( rawJsonData );

    final MessageResponse response = connection.getUnparsedResponses().get( 0 );
    {
      assertEquals( connection.getCurrentMessageResponse(), null );
      assertEquals( connection.getUnparsedResponses().size(), 1 );

      // Select response
      assertTrue( connector.progressResponseProcessing() );

      assertEquals( connection.getCurrentMessageResponse(), response );
      assertEquals( connection.getUnparsedResponses().size(), 0 );
    }

    {
      assertEquals( response.getRawJsonData(), rawJsonData );
      assertThrows( response::getChangeSet );
      assertEquals( response.needsParsing(), true );
      assertEquals( connection.getPendingResponses().size(), 0 );

      // Parse response
      assertTrue( connector.progressResponseProcessing() );

      assertEquals( response.getRawJsonData(), null );
      assertNotNull( response.getChangeSet() );
      assertEquals( response.needsParsing(), false );
    }

    {
      assertEquals( connection.getCurrentMessageResponse(), null );
      assertEquals( connection.getPendingResponses().size(), 1 );

      // Pickup parsed response and set it as current
      assertTrue( connector.progressResponseProcessing() );

      assertEquals( connection.getCurrentMessageResponse(), response );
      assertEquals( connection.getPendingResponses().size(), 0 );
    }

    {
      assertEquals( response.needsChannelChangesProcessed(), true );

      // Process Channel Changes in response
      assertTrue( connector.progressResponseProcessing() );

      assertEquals( response.needsChannelChangesProcessed(), false );
    }

    {
      assertEquals( response.areEntityChangesPending(), true );

      when( connector.getChangeMapper().createEntity( any( EntitySchema.class ),
                                                      anyInt(),
                                                      any( EntityChangeData.class ) ) )
        .thenReturn( mock( Linkable.class ) );

      // Process Entity Changes in response
      assertTrue( connector.progressResponseProcessing() );

      assertEquals( response.areEntityChangesPending(), false );
    }

    {
      assertEquals( response.areEntityLinksPending(), true );

      // Process Entity Links in response
      assertTrue( connector.progressResponseProcessing() );

      assertEquals( response.areEntityLinksPending(), false );
    }

    {
      assertEquals( response.hasWorldBeenValidated(), false );

      // Validate World
      assertTrue( connector.progressResponseProcessing() );

      assertEquals( response.hasWorldBeenValidated(), true );
    }

    {
      assertEquals( connection.getCurrentMessageResponse(), response );

      // Complete message
      assertTrue( connector.progressResponseProcessing() );

      assertEquals( connection.getCurrentMessageResponse(), null );
    }
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void progressAreaOfInterestAddRequest_onSuccess()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), true, ChannelSchema.FilterType.STATIC, false, true );
    final SystemSchema schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final TestConnector connector = TestConnector.create( schema );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final String filter = ValueUtil.randomString();
    final AreaOfInterestRequest request =
      new AreaOfInterestRequest( address, AreaOfInterestRequest.Type.ADD, filter );

    pauseScheduler();

    connection.injectCurrentAreaOfInterestRequest( request );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final AtomicReference<SafeProcedure> onSuccess = new AtomicReference<>();
    final AtomicInteger callCount = new AtomicInteger();
    doAnswer( i -> {
      callCount.incrementAndGet();
      assertEquals( i.getArguments()[ 0 ], address );
      onSuccess.set( (SafeProcedure) i.getArguments()[ 5 ] );
      return null;
    } )
      .when( connector.getTransport() )
      .requestSubscribe( eq( address ),
                         eq( filter ),
                         any(),
                         any(),
                         any(),
                         any( SafeProcedure.class ),
                         any() );

    assertEquals( callCount.get(), 0 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );

    connector.progressAreaOfInterestAddRequest( request );

    assertEquals( callCount.get(), 1 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );
    assertNull( safeAction( () -> Replicant.context().findSubscription( address ) ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscribeStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
    } );

    onSuccess.get().call();

    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), true );
    assertNull( safeAction( () -> Replicant.context().findSubscription( address ) ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( SubscribeCompletedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void progressAreaOfInterestAddRequest_onSuccess_CachedValueNotInLocalCache()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), true, ChannelSchema.FilterType.STATIC, true, true );
    final SystemSchema schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final TestConnector connector = TestConnector.create( schema );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final String filter = ValueUtil.randomString();
    final AreaOfInterestRequest request =
      new AreaOfInterestRequest( address, AreaOfInterestRequest.Type.ADD, filter );

    pauseScheduler();

    connection.injectCurrentAreaOfInterestRequest( request );

    Replicant.context().setCacheService( new TestCacheService() );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final AtomicReference<SafeProcedure> onSuccess = new AtomicReference<>();
    final AtomicInteger callCount = new AtomicInteger();
    doAnswer( i -> {
      callCount.incrementAndGet();
      assertEquals( i.getArguments()[ 0 ], address );
      onSuccess.set( (SafeProcedure) i.getArguments()[ 5 ] );
      return null;
    } )
      .when( connector.getTransport() )
      .requestSubscribe( eq( address ),
                         eq( filter ),
                         any(),
                         any(),
                         any(),
                         any( SafeProcedure.class ),
                         any() );

    assertEquals( callCount.get(), 0 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );

    connector.progressAreaOfInterestAddRequest( request );

    assertEquals( callCount.get(), 1 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );
    assertNull( safeAction( () -> Replicant.context().findSubscription( address ) ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscribeStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
    } );

    onSuccess.get().call();

    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), true );
    assertNull( safeAction( () -> Replicant.context().findSubscription( address ) ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( SubscribeCompletedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void progressAreaOfInterestAddRequest_onSuccess_CachedValueInLocalCache()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), true, ChannelSchema.FilterType.STATIC, true, true );
    final SystemSchema schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final TestConnector connector = TestConnector.create( schema );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final String filter = ValueUtil.randomString();
    final AreaOfInterestRequest request =
      new AreaOfInterestRequest( address, AreaOfInterestRequest.Type.ADD, filter );

    pauseScheduler();

    connection.injectCurrentAreaOfInterestRequest( request );

    final TestCacheService cacheService = new TestCacheService();
    Replicant.context().setCacheService( cacheService );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final String key = "1.0";
    final String eTag = "";
    cacheService.store( key, eTag, ValueUtil.randomString() );
    final AtomicReference<SafeProcedure> onCacheValid = new AtomicReference<>();
    final AtomicInteger callCount = new AtomicInteger();
    doAnswer( i -> {
      callCount.incrementAndGet();
      assertEquals( i.getArguments()[ 0 ], address );
      assertEquals( i.getArguments()[ 2 ], key );
      assertEquals( i.getArguments()[ 3 ], eTag );
      assertNotNull( i.getArguments()[ 4 ] );
      onCacheValid.set( (SafeProcedure) i.getArguments()[ 4 ] );
      return null;
    } )
      .when( connector.getTransport() )
      .requestSubscribe( eq( address ),
                         eq( filter ),
                         any(),
                         any(),
                         any( SafeProcedure.class ),
                         any( SafeProcedure.class ),
                         any() );

    assertEquals( callCount.get(), 0 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );

    connector.progressAreaOfInterestAddRequest( request );

    assertEquals( callCount.get(), 1 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );
    assertNull( safeAction( () -> Replicant.context().findSubscription( address ) ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscribeStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
    } );

    assertEquals( connector.isSchedulerActive(), false );
    assertEquals( connection.getOutOfBandResponses().size(), 0 );

    onCacheValid.get().call();

    assertEquals( connector.isSchedulerActive(), true );

    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), true );
    assertEquals( connection.getOutOfBandResponses().size(), 1 );
    assertNull( safeAction( () -> Replicant.context().findSubscription( address ) ) );

    handler.assertEventCount( 1 );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void progressAreaOfInterestAddRequest_onSuccess_CachedValueInLocalCacheForNonCacheableGraph()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), true, ChannelSchema.FilterType.STATIC, false, true );
    final SystemSchema schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final TestConnector connector = TestConnector.create( schema );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final String filter = ValueUtil.randomString();
    final AreaOfInterestRequest request =
      new AreaOfInterestRequest( address, AreaOfInterestRequest.Type.ADD, filter );

    pauseScheduler();


    connection.injectCurrentAreaOfInterestRequest( request );

    final TestCacheService cacheService = new TestCacheService();
    Replicant.context().setCacheService( cacheService );

    cacheService.store( "1.0", ValueUtil.randomString(), ValueUtil.randomString() );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> connector.progressAreaOfInterestAddRequest( request ) );

    assertEquals( exception.getMessage(), "Replicant-0072: Found cache entry for non-cacheable channel." );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void progressAreaOfInterestAddRequest_onFailure()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), true, ChannelSchema.FilterType.STATIC, false, true );
    final SystemSchema schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final TestConnector connector = TestConnector.create( schema );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final String filter = ValueUtil.randomString();
    final AreaOfInterestRequest request =
      new AreaOfInterestRequest( address, AreaOfInterestRequest.Type.ADD, filter );

    pauseScheduler();

    connection.injectCurrentAreaOfInterestRequest( request );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final AtomicReference<Consumer<Throwable>> onFailure = new AtomicReference<>();
    final AtomicInteger callCount = new AtomicInteger();
    doAnswer( i -> {
      callCount.incrementAndGet();
      assertEquals( i.getArguments()[ 0 ], address );
      onFailure.set( (Consumer<Throwable>) i.getArguments()[ 6 ] );
      return null;
    } )
      .when( connector.getTransport() )
      .requestSubscribe( eq( address ),
                         eq( filter ),
                         any(),
                         any(),
                         any(),
                         any( SafeProcedure.class ),
                         any() );

    assertEquals( callCount.get(), 0 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );

    connector.progressAreaOfInterestAddRequest( request );

    assertEquals( callCount.get(), 1 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );
    assertNull( safeAction( () -> Replicant.context().findSubscription( address ) ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscribeStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
    } );

    final Throwable error = new Throwable();
    onFailure.get().accept( error );

    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), true );
    assertNull( safeAction( () -> Replicant.context().findSubscription( address ) ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( SubscribeFailedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
      assertEquals( e.getError(), error );
    } );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void progressBulkAreaOfInterestAddRequests_onSuccess()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), false, ChannelSchema.FilterType.STATIC, false, true );
    final SystemSchema schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final TestConnector connector = TestConnector.create( schema );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final ChannelAddress address1 = new ChannelAddress( 1, 0, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 0, 2 );
    final ChannelAddress address3 = new ChannelAddress( 1, 0, 3 );
    final String filter = ValueUtil.randomString();
    final AreaOfInterestRequest request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.ADD, filter );
    final AreaOfInterestRequest request2 =
      new AreaOfInterestRequest( address2, AreaOfInterestRequest.Type.ADD, filter );
    final AreaOfInterestRequest request3 =
      new AreaOfInterestRequest( address3, AreaOfInterestRequest.Type.ADD, filter );

    pauseScheduler();

    final Subscription subscription1 =
      safeAction( () -> Replicant.context().createSubscription( address1, null, true ) );
    final Subscription subscription2 =
      safeAction( () -> Replicant.context().createSubscription( address2, null, true ) );
    final Subscription subscription3 =
      safeAction( () -> Replicant.context().createSubscription( address3, null, true ) );

    connection.injectCurrentAreaOfInterestRequest( request1 );
    connection.injectCurrentAreaOfInterestRequest( request2 );
    connection.injectCurrentAreaOfInterestRequest( request3 );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final AtomicReference<SafeProcedure> onSuccess = new AtomicReference<>();
    final AtomicInteger callCount = new AtomicInteger();
    doAnswer( i -> {
      callCount.incrementAndGet();
      final List<ChannelAddress> addresses = (List<ChannelAddress>) i.getArguments()[ 0 ];
      assertEquals( addresses.contains( address1 ), true );
      assertEquals( addresses.contains( address2 ), true );
      assertEquals( addresses.contains( address3 ), true );
      onSuccess.set( (SafeProcedure) i.getArguments()[ 2 ] );
      return null;
    } )
      .when( connector.getTransport() )
      .requestBulkSubscribe( anyListOf( ChannelAddress.class ),
                             eq( filter ),
                             any( SafeProcedure.class ),
                             any() );

    assertEquals( callCount.get(), 0 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );

    connector.progressBulkAreaOfInterestAddRequests( Arrays.asList( request1, request2, request3 ) );

    assertEquals( callCount.get(), 1 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );
    safeAction( () -> assertEquals( subscription1.isExplicitSubscription(), true ) );
    safeAction( () -> assertEquals( subscription2.isExplicitSubscription(), true ) );
    safeAction( () -> assertEquals( subscription3.isExplicitSubscription(), true ) );

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

    onSuccess.get().call();

    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), true );
    safeAction( () -> assertEquals( subscription1.isExplicitSubscription(), true ) );
    safeAction( () -> assertEquals( subscription2.isExplicitSubscription(), true ) );
    safeAction( () -> assertEquals( subscription3.isExplicitSubscription(), true ) );

    handler.assertEventCount( 6 );
    handler.assertNextEvent( SubscribeCompletedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address1 );
    } );
    handler.assertNextEvent( SubscribeCompletedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address2 );
    } );
    handler.assertNextEvent( SubscribeCompletedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address3 );
    } );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void progressBulkAreaOfInterestAddRequests_onFailure()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), false, ChannelSchema.FilterType.STATIC, false, true );
    final SystemSchema schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final TestConnector connector = TestConnector.create( schema );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final ChannelAddress address1 = new ChannelAddress( 1, 0, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 0, 2 );
    final ChannelAddress address3 = new ChannelAddress( 1, 0, 3 );
    final String filter = ValueUtil.randomString();
    final AreaOfInterestRequest request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.ADD, filter );
    final AreaOfInterestRequest request2 =
      new AreaOfInterestRequest( address2, AreaOfInterestRequest.Type.ADD, filter );
    final AreaOfInterestRequest request3 =
      new AreaOfInterestRequest( address3, AreaOfInterestRequest.Type.ADD, filter );

    pauseScheduler();

    final Subscription subscription1 =
      safeAction( () -> Replicant.context().createSubscription( address1, null, true ) );
    final Subscription subscription2 =
      safeAction( () -> Replicant.context().createSubscription( address2, null, true ) );
    final Subscription subscription3 =
      safeAction( () -> Replicant.context().createSubscription( address3, null, true ) );

    connection.injectCurrentAreaOfInterestRequest( request1 );
    connection.injectCurrentAreaOfInterestRequest( request2 );
    connection.injectCurrentAreaOfInterestRequest( request3 );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final AtomicReference<Consumer<Throwable>> onFailure = new AtomicReference<>();
    final AtomicInteger callCount = new AtomicInteger();
    doAnswer( i -> {
      callCount.incrementAndGet();
      final List<ChannelAddress> addresses = (List<ChannelAddress>) i.getArguments()[ 0 ];
      assertEquals( addresses.contains( address1 ), true );
      assertEquals( addresses.contains( address2 ), true );
      assertEquals( addresses.contains( address3 ), true );
      onFailure.set( (Consumer<Throwable>) i.getArguments()[ 3 ] );
      return null;
    } )
      .when( connector.getTransport() )
      .requestBulkSubscribe( anyListOf( ChannelAddress.class ),
                             eq( filter ),
                             any( SafeProcedure.class ),
                             any() );

    assertEquals( callCount.get(), 0 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );

    connector.progressBulkAreaOfInterestAddRequests( Arrays.asList( request1, request2, request3 ) );

    assertEquals( callCount.get(), 1 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );
    safeAction( () -> assertEquals( subscription1.isExplicitSubscription(), true ) );
    safeAction( () -> assertEquals( subscription2.isExplicitSubscription(), true ) );
    safeAction( () -> assertEquals( subscription3.isExplicitSubscription(), true ) );

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

    final Throwable error = new Throwable();
    onFailure.get().accept( error );

    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), true );
    safeAction( () -> assertEquals( subscription1.isExplicitSubscription(), true ) );
    safeAction( () -> assertEquals( subscription2.isExplicitSubscription(), true ) );
    safeAction( () -> assertEquals( subscription3.isExplicitSubscription(), true ) );

    handler.assertEventCount( 6 );
    handler.assertNextEvent( SubscribeFailedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address1 );
      assertEquals( e.getError(), error );
    } );
    handler.assertNextEvent( SubscribeFailedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address2 );
      assertEquals( e.getError(), error );
    } );
    handler.assertNextEvent( SubscribeFailedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address3 );
      assertEquals( e.getError(), error );
    } );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void progressAreaOfInterestAddRequests_onFailure_singleRequests()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), false, ChannelSchema.FilterType.STATIC, false, true );
    final SystemSchema schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final TestConnector connector = TestConnector.create( schema );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final ChannelAddress address1 = new ChannelAddress( 1, 0, 1 );
    final String filter = ValueUtil.randomString();
    final AreaOfInterestRequest request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.ADD, filter );

    pauseScheduler();

    connection.injectCurrentAreaOfInterestRequest( request1 );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final AtomicReference<Consumer<Throwable>> onFailure = new AtomicReference<>();
    final AtomicInteger callCount = new AtomicInteger();
    doAnswer( i -> {
      callCount.incrementAndGet();
      onFailure.set( (Consumer<Throwable>) i.getArguments()[ 6 ] );
      return null;
    } )
      .when( connector.getTransport() )
      .requestSubscribe( eq( address1 ),
                         eq( filter ),
                         any(),
                         any(),
                         any(),
                         any( SafeProcedure.class ),
                         any() );

    assertEquals( callCount.get(), 0 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );

    final ArrayList<AreaOfInterestRequest> requests = new ArrayList<>();
    requests.add( request1 );
    connector.progressAreaOfInterestAddRequests( requests );

    assertEquals( callCount.get(), 1 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscribeStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address1 );
    } );

    final Throwable error = new Throwable();
    onFailure.get().accept( error );

    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), true );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( SubscribeFailedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address1 );
      assertEquals( e.getError(), error );
    } );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void progressAreaOfInterestAddRequests_onFailure_zeroRequests()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), false, ChannelSchema.FilterType.STATIC, false, true );
    final SystemSchema schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final TestConnector connector = TestConnector.create( schema );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final ChannelAddress address1 = new ChannelAddress( 1, 0, 1 );
    final String filter = ValueUtil.randomString();
    final AreaOfInterestRequest request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.ADD, filter );

    pauseScheduler();

    connection.injectCurrentAreaOfInterestRequest( request1 );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );

    final ArrayList<AreaOfInterestRequest> requests = new ArrayList<>();
    // Pass in empty requests list to simulate that they are all filtered out
    connector.progressAreaOfInterestAddRequests( requests );

    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), true );

    handler.assertEventCount( 0 );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void progressAreaOfInterestAddRequests_onFailure_multipleRequests()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), false, ChannelSchema.FilterType.STATIC, false, true );
    final SystemSchema schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final TestConnector connector = TestConnector.create( schema );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final ChannelAddress address1 = new ChannelAddress( 1, 0, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 0, 2 );
    final String filter = ValueUtil.randomString();
    final AreaOfInterestRequest request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.ADD, filter );
    final AreaOfInterestRequest request2 =
      new AreaOfInterestRequest( address2, AreaOfInterestRequest.Type.ADD, filter );

    pauseScheduler();

    connection.injectCurrentAreaOfInterestRequest( request1 );
    connection.injectCurrentAreaOfInterestRequest( request2 );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final AtomicReference<Consumer<Throwable>> onFailure = new AtomicReference<>();
    final AtomicInteger callCount = new AtomicInteger();
    doAnswer( i -> {
      callCount.incrementAndGet();
      final List<ChannelAddress> addresses = (List<ChannelAddress>) i.getArguments()[ 0 ];
      assertEquals( addresses.contains( address1 ), true );
      assertEquals( addresses.contains( address2 ), true );
      onFailure.set( (Consumer<Throwable>) i.getArguments()[ 3 ] );
      return null;
    } )
      .when( connector.getTransport() )
      .requestBulkSubscribe( anyListOf( ChannelAddress.class ),
                             eq( filter ),
                             any( SafeProcedure.class ),
                             any() );

    assertEquals( callCount.get(), 0 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );

    final ArrayList<AreaOfInterestRequest> requests = new ArrayList<>();
    requests.add( request1 );
    requests.add( request2 );
    connector.progressAreaOfInterestAddRequests( requests );

    assertEquals( callCount.get(), 1 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( SubscribeStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address1 );
    } );
    handler.assertNextEvent( SubscribeStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address2 );
    } );

    final Throwable error = new Throwable();
    onFailure.get().accept( error );

    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), true );

    handler.assertEventCount( 4 );
    handler.assertNextEvent( SubscribeFailedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address1 );
      assertEquals( e.getError(), error );
    } );
    handler.assertNextEvent( SubscribeFailedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address2 );
      assertEquals( e.getError(), error );
    } );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void progressAreaOfInterestUpdateRequest_onSuccess()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), true, ChannelSchema.FilterType.DYNAMIC, false, true );
    final SystemSchema schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final TestConnector connector = TestConnector.create( schema );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final String filter = ValueUtil.randomString();
    final AreaOfInterestRequest request =
      new AreaOfInterestRequest( address, AreaOfInterestRequest.Type.UPDATE, filter );

    pauseScheduler();

    final Subscription subscription = safeAction( () -> Replicant.context().createSubscription( address, null, true ) );

    connection.injectCurrentAreaOfInterestRequest( request );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final AtomicReference<SafeProcedure> onSuccess = new AtomicReference<>();
    final AtomicInteger callCount = new AtomicInteger();
    doAnswer( i -> {
      callCount.incrementAndGet();
      assertEquals( i.getArguments()[ 0 ], address );
      onSuccess.set( (SafeProcedure) i.getArguments()[ 2 ] );
      return null;
    } )
      .when( connector.getTransport() )
      .requestSubscriptionUpdate( eq( address ), eq( filter ), any( SafeProcedure.class ), any() );

    assertEquals( callCount.get(), 0 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );

    connector.progressAreaOfInterestUpdateRequest( request );

    assertEquals( callCount.get(), 1 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );
    safeAction( () -> assertEquals( subscription.isExplicitSubscription(), true ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscriptionUpdateStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
    } );

    onSuccess.get().call();

    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), true );
    safeAction( () -> assertEquals( subscription.isExplicitSubscription(), true ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( SubscriptionUpdateCompletedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void progressAreaOfInterestUpdateRequest_onFailure()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), true, ChannelSchema.FilterType.DYNAMIC, false, true );
    final SystemSchema schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final TestConnector connector = TestConnector.create( schema );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final String filter = ValueUtil.randomString();
    final AreaOfInterestRequest request =
      new AreaOfInterestRequest( address, AreaOfInterestRequest.Type.UPDATE, filter );

    pauseScheduler();

    final Subscription subscription = safeAction( () -> Replicant.context().createSubscription( address, null, true ) );

    connection.injectCurrentAreaOfInterestRequest( request );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final AtomicReference<Consumer<Throwable>> onFailure = new AtomicReference<>();
    final AtomicInteger callCount = new AtomicInteger();
    doAnswer( i -> {
      callCount.incrementAndGet();
      assertEquals( i.getArguments()[ 0 ], address );
      onFailure.set( (Consumer<Throwable>) i.getArguments()[ 3 ] );
      return null;
    } )
      .when( connector.getTransport() )
      .requestSubscriptionUpdate( eq( address ), eq( filter ), any( SafeProcedure.class ), any() );

    assertEquals( callCount.get(), 0 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );

    connector.progressAreaOfInterestUpdateRequest( request );

    assertEquals( callCount.get(), 1 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );
    safeAction( () -> assertEquals( subscription.isExplicitSubscription(), true ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscriptionUpdateStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
    } );

    final Throwable error = new Throwable();
    onFailure.get().accept( error );

    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), true );
    safeAction( () -> assertEquals( subscription.isExplicitSubscription(), true ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( SubscriptionUpdateFailedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
      assertEquals( e.getError(), error );
    } );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void progressBulkAreaOfInterestUpdateRequests_onSuccess()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), false, ChannelSchema.FilterType.DYNAMIC, false, true );
    final SystemSchema schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final TestConnector connector = TestConnector.create( schema );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final ChannelAddress address1 = new ChannelAddress( 1, 0, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 0, 2 );
    final ChannelAddress address3 = new ChannelAddress( 1, 0, 3 );
    final String filter = ValueUtil.randomString();
    final AreaOfInterestRequest request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.UPDATE, filter );
    final AreaOfInterestRequest request2 =
      new AreaOfInterestRequest( address2, AreaOfInterestRequest.Type.UPDATE, filter );
    final AreaOfInterestRequest request3 =
      new AreaOfInterestRequest( address3, AreaOfInterestRequest.Type.UPDATE, filter );

    pauseScheduler();

    final Subscription subscription1 =
      safeAction( () -> Replicant.context().createSubscription( address1, null, true ) );
    final Subscription subscription2 =
      safeAction( () -> Replicant.context().createSubscription( address2, null, true ) );
    final Subscription subscription3 =
      safeAction( () -> Replicant.context().createSubscription( address3, null, true ) );

    connection.injectCurrentAreaOfInterestRequest( request1 );
    connection.injectCurrentAreaOfInterestRequest( request2 );
    connection.injectCurrentAreaOfInterestRequest( request3 );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final AtomicReference<SafeProcedure> onSuccess = new AtomicReference<>();
    final AtomicInteger callCount = new AtomicInteger();
    doAnswer( i -> {
      callCount.incrementAndGet();
      final List<ChannelAddress> addresses = (List<ChannelAddress>) i.getArguments()[ 0 ];
      assertEquals( addresses.contains( address1 ), true );
      assertEquals( addresses.contains( address2 ), true );
      assertEquals( addresses.contains( address3 ), true );
      onSuccess.set( (SafeProcedure) i.getArguments()[ 2 ] );
      return null;
    } )
      .when( connector.getTransport() )
      .requestBulkSubscriptionUpdate( anyListOf( ChannelAddress.class ),
                                      eq( filter ),
                                      any( SafeProcedure.class ),
                                      any() );

    assertEquals( callCount.get(), 0 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );

    connector.progressBulkAreaOfInterestUpdateRequests( Arrays.asList( request1, request2, request3 ) );

    assertEquals( callCount.get(), 1 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );
    safeAction( () -> assertEquals( subscription1.isExplicitSubscription(), true ) );
    safeAction( () -> assertEquals( subscription2.isExplicitSubscription(), true ) );
    safeAction( () -> assertEquals( subscription3.isExplicitSubscription(), true ) );

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

    onSuccess.get().call();

    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), true );
    safeAction( () -> assertEquals( subscription1.isExplicitSubscription(), true ) );
    safeAction( () -> assertEquals( subscription2.isExplicitSubscription(), true ) );
    safeAction( () -> assertEquals( subscription3.isExplicitSubscription(), true ) );

    handler.assertEventCount( 6 );
    handler.assertNextEvent( SubscriptionUpdateCompletedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address1 );
    } );
    handler.assertNextEvent( SubscriptionUpdateCompletedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address2 );
    } );
    handler.assertNextEvent( SubscriptionUpdateCompletedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address3 );
    } );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void progressBulkAreaOfInterestUpdateRequests_onFailure()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), false, ChannelSchema.FilterType.DYNAMIC, false, true );
    final SystemSchema schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final TestConnector connector = TestConnector.create( schema );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final ChannelAddress address1 = new ChannelAddress( 1, 0, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 0, 2 );
    final ChannelAddress address3 = new ChannelAddress( 1, 0, 3 );
    final String filter = ValueUtil.randomString();
    final AreaOfInterestRequest request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.UPDATE, filter );
    final AreaOfInterestRequest request2 =
      new AreaOfInterestRequest( address2, AreaOfInterestRequest.Type.UPDATE, filter );
    final AreaOfInterestRequest request3 =
      new AreaOfInterestRequest( address3, AreaOfInterestRequest.Type.UPDATE, filter );

    pauseScheduler();

    final Subscription subscription1 =
      safeAction( () -> Replicant.context().createSubscription( address1, null, true ) );
    final Subscription subscription2 =
      safeAction( () -> Replicant.context().createSubscription( address2, null, true ) );
    final Subscription subscription3 =
      safeAction( () -> Replicant.context().createSubscription( address3, null, true ) );

    connection.injectCurrentAreaOfInterestRequest( request1 );
    connection.injectCurrentAreaOfInterestRequest( request2 );
    connection.injectCurrentAreaOfInterestRequest( request3 );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final AtomicReference<Consumer<Throwable>> onFailure = new AtomicReference<>();
    final AtomicInteger callCount = new AtomicInteger();
    doAnswer( i -> {
      callCount.incrementAndGet();
      final List<ChannelAddress> addresses = (List<ChannelAddress>) i.getArguments()[ 0 ];
      assertEquals( addresses.contains( address1 ), true );
      assertEquals( addresses.contains( address2 ), true );
      assertEquals( addresses.contains( address3 ), true );
      onFailure.set( (Consumer<Throwable>) i.getArguments()[ 3 ] );
      return null;
    } )
      .when( connector.getTransport() )
      .requestBulkSubscriptionUpdate( anyListOf( ChannelAddress.class ),
                                      eq( filter ),
                                      any( SafeProcedure.class ),
                                      any() );

    assertEquals( callCount.get(), 0 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );

    connector.progressBulkAreaOfInterestUpdateRequests( Arrays.asList( request1, request2, request3 ) );

    assertEquals( callCount.get(), 1 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );
    safeAction( () -> assertEquals( subscription1.isExplicitSubscription(), true ) );
    safeAction( () -> assertEquals( subscription2.isExplicitSubscription(), true ) );
    safeAction( () -> assertEquals( subscription3.isExplicitSubscription(), true ) );

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

    final Throwable error = new Throwable();
    onFailure.get().accept( error );

    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), true );
    safeAction( () -> assertEquals( subscription1.isExplicitSubscription(), true ) );
    safeAction( () -> assertEquals( subscription2.isExplicitSubscription(), true ) );
    safeAction( () -> assertEquals( subscription3.isExplicitSubscription(), true ) );

    handler.assertEventCount( 6 );
    handler.assertNextEvent( SubscriptionUpdateFailedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address1 );
      assertEquals( e.getError(), error );
    } );
    handler.assertNextEvent( SubscriptionUpdateFailedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address2 );
      assertEquals( e.getError(), error );
    } );
    handler.assertNextEvent( SubscriptionUpdateFailedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address3 );
      assertEquals( e.getError(), error );
    } );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void progressAreaOfInterestUpdateRequests_onFailure_singleRequests()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), false, ChannelSchema.FilterType.DYNAMIC, false, true );
    final SystemSchema schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final TestConnector connector = TestConnector.create( schema );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final ChannelAddress address1 = new ChannelAddress( 1, 0, 1 );
    final String filter = ValueUtil.randomString();
    final AreaOfInterestRequest request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.UPDATE, filter );

    pauseScheduler();

    final Subscription subscription1 =
      safeAction( () -> Replicant.context().createSubscription( address1, null, true ) );

    connection.injectCurrentAreaOfInterestRequest( request1 );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final AtomicReference<Consumer<Throwable>> onFailure = new AtomicReference<>();
    final AtomicInteger callCount = new AtomicInteger();
    doAnswer( i -> {
      callCount.incrementAndGet();
      onFailure.set( (Consumer<Throwable>) i.getArguments()[ 3 ] );
      return null;
    } )
      .when( connector.getTransport() )
      .requestSubscriptionUpdate( eq( address1 ), eq( filter ), any( SafeProcedure.class ), any() );

    assertEquals( callCount.get(), 0 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );

    final ArrayList<AreaOfInterestRequest> requests = new ArrayList<>();
    requests.add( request1 );
    connector.progressAreaOfInterestUpdateRequests( requests );

    assertEquals( callCount.get(), 1 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );
    safeAction( () -> assertEquals( subscription1.isExplicitSubscription(), true ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscriptionUpdateStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address1 );
    } );

    final Throwable error = new Throwable();
    onFailure.get().accept( error );

    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), true );
    safeAction( () -> assertEquals( subscription1.isExplicitSubscription(), true ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( SubscriptionUpdateFailedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address1 );
      assertEquals( e.getError(), error );
    } );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void progressAreaOfInterestUpdateRequests_onFailure_zeroRequests()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), false, ChannelSchema.FilterType.DYNAMIC, false, true );
    final SystemSchema schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final TestConnector connector = TestConnector.create( schema );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final ChannelAddress address1 = new ChannelAddress( 1, 0, 1 );
    final String filter = ValueUtil.randomString();
    final AreaOfInterestRequest request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.UPDATE, filter );

    pauseScheduler();

    connection.injectCurrentAreaOfInterestRequest( request1 );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );

    final ArrayList<AreaOfInterestRequest> requests = new ArrayList<>();
    // Pass in empty requests list to simulate that they are all filtered out
    connector.progressAreaOfInterestUpdateRequests( requests );

    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), true );

    handler.assertEventCount( 0 );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void progressAreaOfInterestUpdateRequests_onFailure_multipleRequests()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), false, ChannelSchema.FilterType.DYNAMIC, false, true );
    final SystemSchema schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final TestConnector connector = TestConnector.create( schema );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final ChannelAddress address1 = new ChannelAddress( 1, 0, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 0, 2 );
    final String filter = ValueUtil.randomString();
    final AreaOfInterestRequest request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.UPDATE, filter );
    final AreaOfInterestRequest request2 =
      new AreaOfInterestRequest( address2, AreaOfInterestRequest.Type.UPDATE, filter );

    pauseScheduler();

    final Subscription subscription1 =
      safeAction( () -> Replicant.context().createSubscription( address1, null, true ) );
    final Subscription subscription2 =
      safeAction( () -> Replicant.context().createSubscription( address2, null, true ) );

    connection.injectCurrentAreaOfInterestRequest( request1 );
    connection.injectCurrentAreaOfInterestRequest( request2 );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final AtomicReference<Consumer<Throwable>> onFailure = new AtomicReference<>();
    final AtomicInteger callCount = new AtomicInteger();
    doAnswer( i -> {
      callCount.incrementAndGet();
      final List<ChannelAddress> addresses = (List<ChannelAddress>) i.getArguments()[ 0 ];
      assertEquals( addresses.contains( address1 ), true );
      assertEquals( addresses.contains( address2 ), true );
      onFailure.set( (Consumer<Throwable>) i.getArguments()[ 3 ] );
      return null;
    } )
      .when( connector.getTransport() )
      .requestBulkSubscriptionUpdate( anyListOf( ChannelAddress.class ),
                                      eq( filter ),
                                      any( SafeProcedure.class ),
                                      any() );

    assertEquals( callCount.get(), 0 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );

    final ArrayList<AreaOfInterestRequest> requests = new ArrayList<>();
    requests.add( request1 );
    requests.add( request2 );
    connector.progressAreaOfInterestUpdateRequests( requests );

    assertEquals( callCount.get(), 1 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );
    safeAction( () -> assertEquals( subscription1.isExplicitSubscription(), true ) );
    safeAction( () -> assertEquals( subscription2.isExplicitSubscription(), true ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( SubscriptionUpdateStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address1 );
    } );
    handler.assertNextEvent( SubscriptionUpdateStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address2 );
    } );

    final Throwable error = new Throwable();
    onFailure.get().accept( error );

    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), true );
    safeAction( () -> assertEquals( subscription1.isExplicitSubscription(), true ) );
    safeAction( () -> assertEquals( subscription2.isExplicitSubscription(), true ) );

    handler.assertEventCount( 4 );
    handler.assertNextEvent( SubscriptionUpdateFailedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address1 );
      assertEquals( e.getError(), error );
    } );
    handler.assertNextEvent( SubscriptionUpdateFailedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address2 );
      assertEquals( e.getError(), error );
    } );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void progressAreaOfInterestRemoveRequest_onSuccess()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), true, ChannelSchema.FilterType.NONE, false, true );
    final SystemSchema schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final TestConnector connector = TestConnector.create( schema );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final AreaOfInterestRequest request = new AreaOfInterestRequest( address, AreaOfInterestRequest.Type.REMOVE, null );

    pauseScheduler();

    final Subscription subscription = safeAction( () -> Replicant.context().createSubscription( address, null, true ) );

    connection.injectCurrentAreaOfInterestRequest( request );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final AtomicReference<SafeProcedure> onSuccess = new AtomicReference<>();
    final AtomicInteger callCount = new AtomicInteger();
    doAnswer( i -> {
      callCount.incrementAndGet();
      assertEquals( i.getArguments()[ 0 ], address );
      onSuccess.set( (SafeProcedure) i.getArguments()[ 1 ] );
      return null;
    } )
      .when( connector.getTransport() )
      .requestUnsubscribe( eq( address ), any( SafeProcedure.class ), any() );

    assertEquals( callCount.get(), 0 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );

    connector.progressAreaOfInterestRemoveRequest( request );

    assertEquals( callCount.get(), 1 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );
    safeAction( () -> assertEquals( subscription.isExplicitSubscription(), true ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( UnsubscribeStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
    } );

    onSuccess.get().call();

    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), true );
    safeAction( () -> assertEquals( subscription.isExplicitSubscription(), false ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( UnsubscribeCompletedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void progressAreaOfInterestRemoveRequest_onFailure()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), true, ChannelSchema.FilterType.NONE, false, true );
    final SystemSchema schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final TestConnector connector = TestConnector.create( schema );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final AreaOfInterestRequest request = new AreaOfInterestRequest( address, AreaOfInterestRequest.Type.REMOVE, null );

    pauseScheduler();

    final Subscription subscription = safeAction( () -> Replicant.context().createSubscription( address, null, true ) );

    connection.injectCurrentAreaOfInterestRequest( request );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final AtomicReference<Consumer<Throwable>> onFailure = new AtomicReference<>();
    final AtomicInteger callCount = new AtomicInteger();
    doAnswer( i -> {
      callCount.incrementAndGet();
      assertEquals( i.getArguments()[ 0 ], address );
      onFailure.set( (Consumer<Throwable>) i.getArguments()[ 2 ] );
      return null;
    } )
      .when( connector.getTransport() )
      .requestUnsubscribe( eq( address ), any( SafeProcedure.class ), any() );

    assertEquals( callCount.get(), 0 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );

    connector.progressAreaOfInterestRemoveRequest( request );

    assertEquals( callCount.get(), 1 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );
    safeAction( () -> assertEquals( subscription.isExplicitSubscription(), true ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( UnsubscribeStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
    } );

    final Throwable error = new Throwable();
    onFailure.get().accept( error );

    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), true );
    safeAction( () -> assertEquals( subscription.isExplicitSubscription(), false ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( UnsubscribeFailedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address );
      assertEquals( e.getError(), error );
    } );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void progressBulkAreaOfInterestRemoveRequests_onSuccess()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), false, ChannelSchema.FilterType.NONE, false, true );
    final SystemSchema schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final TestConnector connector = TestConnector.create( schema );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final ChannelAddress address1 = new ChannelAddress( 1, 0, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 0, 2 );
    final ChannelAddress address3 = new ChannelAddress( 1, 0, 3 );
    final AreaOfInterestRequest request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.REMOVE, null );
    final AreaOfInterestRequest request2 =
      new AreaOfInterestRequest( address2, AreaOfInterestRequest.Type.REMOVE, null );
    final AreaOfInterestRequest request3 =
      new AreaOfInterestRequest( address3, AreaOfInterestRequest.Type.REMOVE, null );

    pauseScheduler();

    final Subscription subscription1 =
      safeAction( () -> Replicant.context().createSubscription( address1, null, true ) );
    final Subscription subscription2 =
      safeAction( () -> Replicant.context().createSubscription( address2, null, true ) );
    final Subscription subscription3 =
      safeAction( () -> Replicant.context().createSubscription( address3, null, true ) );

    connection.injectCurrentAreaOfInterestRequest( request1 );
    connection.injectCurrentAreaOfInterestRequest( request2 );
    connection.injectCurrentAreaOfInterestRequest( request3 );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final AtomicReference<SafeProcedure> onSuccess = new AtomicReference<>();
    final AtomicInteger callCount = new AtomicInteger();
    doAnswer( i -> {
      callCount.incrementAndGet();
      final List<ChannelAddress> addresses = (List<ChannelAddress>) i.getArguments()[ 0 ];
      assertEquals( addresses.contains( address1 ), true );
      assertEquals( addresses.contains( address2 ), true );
      assertEquals( addresses.contains( address3 ), true );
      onSuccess.set( (SafeProcedure) i.getArguments()[ 1 ] );
      return null;
    } )
      .when( connector.getTransport() )
      .requestBulkUnsubscribe( anyListOf( ChannelAddress.class ), any( SafeProcedure.class ), any() );

    assertEquals( callCount.get(), 0 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );

    connector.progressBulkAreaOfInterestRemoveRequests( Arrays.asList( request1, request2, request3 ) );

    assertEquals( callCount.get(), 1 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );
    safeAction( () -> assertEquals( subscription1.isExplicitSubscription(), true ) );
    safeAction( () -> assertEquals( subscription2.isExplicitSubscription(), true ) );
    safeAction( () -> assertEquals( subscription3.isExplicitSubscription(), true ) );

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

    onSuccess.get().call();

    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), true );
    safeAction( () -> assertEquals( subscription1.isExplicitSubscription(), false ) );
    safeAction( () -> assertEquals( subscription2.isExplicitSubscription(), false ) );
    safeAction( () -> assertEquals( subscription3.isExplicitSubscription(), false ) );

    handler.assertEventCount( 6 );
    handler.assertNextEvent( UnsubscribeCompletedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address1 );
    } );
    handler.assertNextEvent( UnsubscribeCompletedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address2 );
    } );
    handler.assertNextEvent( UnsubscribeCompletedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address3 );
    } );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void progressBulkAreaOfInterestRemoveRequests_onFailure()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), false, ChannelSchema.FilterType.NONE, false, true );
    final SystemSchema schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final TestConnector connector = TestConnector.create( schema );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final ChannelAddress address1 = new ChannelAddress( 1, 0, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 0, 2 );
    final ChannelAddress address3 = new ChannelAddress( 1, 0, 3 );
    final AreaOfInterestRequest request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.REMOVE, null );
    final AreaOfInterestRequest request2 =
      new AreaOfInterestRequest( address2, AreaOfInterestRequest.Type.REMOVE, null );
    final AreaOfInterestRequest request3 =
      new AreaOfInterestRequest( address3, AreaOfInterestRequest.Type.REMOVE, null );

    pauseScheduler();

    final Subscription subscription1 =
      safeAction( () -> Replicant.context().createSubscription( address1, null, true ) );
    final Subscription subscription2 =
      safeAction( () -> Replicant.context().createSubscription( address2, null, true ) );
    final Subscription subscription3 =
      safeAction( () -> Replicant.context().createSubscription( address3, null, true ) );

    connection.injectCurrentAreaOfInterestRequest( request1 );
    connection.injectCurrentAreaOfInterestRequest( request2 );
    connection.injectCurrentAreaOfInterestRequest( request3 );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final AtomicReference<Consumer<Throwable>> onFailure = new AtomicReference<>();
    final AtomicInteger callCount = new AtomicInteger();
    doAnswer( i -> {
      callCount.incrementAndGet();
      final List<ChannelAddress> addresses = (List<ChannelAddress>) i.getArguments()[ 0 ];
      assertEquals( addresses.contains( address1 ), true );
      assertEquals( addresses.contains( address2 ), true );
      assertEquals( addresses.contains( address3 ), true );
      onFailure.set( (Consumer<Throwable>) i.getArguments()[ 2 ] );
      return null;
    } )
      .when( connector.getTransport() )
      .requestBulkUnsubscribe( anyListOf( ChannelAddress.class ), any( SafeProcedure.class ), any() );

    assertEquals( callCount.get(), 0 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );

    connector.progressBulkAreaOfInterestRemoveRequests( Arrays.asList( request1, request2, request3 ) );

    assertEquals( callCount.get(), 1 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );
    safeAction( () -> assertEquals( subscription1.isExplicitSubscription(), true ) );
    safeAction( () -> assertEquals( subscription2.isExplicitSubscription(), true ) );
    safeAction( () -> assertEquals( subscription3.isExplicitSubscription(), true ) );

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

    final Throwable error = new Throwable();
    onFailure.get().accept( error );

    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), true );
    safeAction( () -> assertEquals( subscription1.isExplicitSubscription(), false ) );
    safeAction( () -> assertEquals( subscription2.isExplicitSubscription(), false ) );
    safeAction( () -> assertEquals( subscription3.isExplicitSubscription(), false ) );

    handler.assertEventCount( 6 );
    handler.assertNextEvent( UnsubscribeFailedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address1 );
      assertEquals( e.getError(), error );
    } );
    handler.assertNextEvent( UnsubscribeFailedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address2 );
      assertEquals( e.getError(), error );
    } );
    handler.assertNextEvent( UnsubscribeFailedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address3 );
      assertEquals( e.getError(), error );
    } );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void progressAreaOfInterestRemoveRequests_onFailure_singleRequests()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), false, ChannelSchema.FilterType.NONE, false, true );
    final SystemSchema schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final TestConnector connector = TestConnector.create( schema );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final ChannelAddress address1 = new ChannelAddress( 1, 0, 1 );
    final AreaOfInterestRequest request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.REMOVE, null );

    pauseScheduler();

    final Subscription subscription1 =
      safeAction( () -> Replicant.context().createSubscription( address1, null, true ) );

    connection.injectCurrentAreaOfInterestRequest( request1 );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final AtomicReference<Consumer<Throwable>> onFailure = new AtomicReference<>();
    final AtomicInteger callCount = new AtomicInteger();
    doAnswer( i -> {
      callCount.incrementAndGet();
      onFailure.set( (Consumer<Throwable>) i.getArguments()[ 2 ] );
      return null;
    } )
      .when( connector.getTransport() )
      .requestUnsubscribe( eq( address1 ), any( SafeProcedure.class ), any() );

    assertEquals( callCount.get(), 0 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );

    final ArrayList<AreaOfInterestRequest> requests = new ArrayList<>();
    requests.add( request1 );
    connector.progressAreaOfInterestRemoveRequests( requests );

    assertEquals( callCount.get(), 1 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );
    safeAction( () -> assertEquals( subscription1.isExplicitSubscription(), true ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( UnsubscribeStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address1 );
    } );

    final Throwable error = new Throwable();
    onFailure.get().accept( error );

    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), true );
    safeAction( () -> assertEquals( subscription1.isExplicitSubscription(), false ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( UnsubscribeFailedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address1 );
      assertEquals( e.getError(), error );
    } );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void progressAreaOfInterestRemoveRequests_onFailure_zeroRequests()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), false, ChannelSchema.FilterType.NONE, false, true );
    final SystemSchema schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final TestConnector connector = TestConnector.create( schema );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final ChannelAddress address1 = new ChannelAddress( 1, 0, 1 );
    final AreaOfInterestRequest request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.REMOVE, null );

    pauseScheduler();

    connection.injectCurrentAreaOfInterestRequest( request1 );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );

    final ArrayList<AreaOfInterestRequest> requests = new ArrayList<>();
    // Pass in empty requests list to simulate that they are all filtered out
    connector.progressAreaOfInterestRemoveRequests( requests );

    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), true );

    handler.assertEventCount( 0 );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void progressAreaOfInterestRemoveRequests_onFailure_multipleRequests()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), false, ChannelSchema.FilterType.NONE, false, true );
    final SystemSchema schema =
      new SystemSchema( 1,
                        ValueUtil.randomString(),
                        new ChannelSchema[]{ channelSchema },
                        new EntitySchema[]{} );

    final TestConnector connector = TestConnector.create( schema );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final ChannelAddress address1 = new ChannelAddress( 1, 0, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 0, 2 );
    final AreaOfInterestRequest request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.REMOVE, null );
    final AreaOfInterestRequest request2 =
      new AreaOfInterestRequest( address2, AreaOfInterestRequest.Type.REMOVE, null );

    pauseScheduler();

    final Subscription subscription1 =
      safeAction( () -> Replicant.context().createSubscription( address1, null, true ) );
    final Subscription subscription2 =
      safeAction( () -> Replicant.context().createSubscription( address2, null, true ) );

    connection.injectCurrentAreaOfInterestRequest( request1 );
    connection.injectCurrentAreaOfInterestRequest( request2 );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final AtomicReference<Consumer<Throwable>> onFailure = new AtomicReference<>();
    final AtomicInteger callCount = new AtomicInteger();
    doAnswer( i -> {
      callCount.incrementAndGet();
      final List<ChannelAddress> addresses = (List<ChannelAddress>) i.getArguments()[ 0 ];
      assertEquals( addresses.contains( address1 ), true );
      assertEquals( addresses.contains( address2 ), true );
      onFailure.set( (Consumer<Throwable>) i.getArguments()[ 2 ] );
      return null;
    } )
      .when( connector.getTransport() )
      .requestBulkUnsubscribe( anyListOf( ChannelAddress.class ), any( SafeProcedure.class ), any() );

    assertEquals( callCount.get(), 0 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );

    final ArrayList<AreaOfInterestRequest> requests = new ArrayList<>();
    requests.add( request1 );
    requests.add( request2 );
    connector.progressAreaOfInterestRemoveRequests( requests );

    assertEquals( callCount.get(), 1 );
    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), false );
    safeAction( () -> assertEquals( subscription1.isExplicitSubscription(), true ) );
    safeAction( () -> assertEquals( subscription2.isExplicitSubscription(), true ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( UnsubscribeStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address1 );
    } );
    handler.assertNextEvent( UnsubscribeStartedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address2 );
    } );

    final Throwable error = new Throwable();
    onFailure.get().accept( error );

    assertEquals( connection.getCurrentAreaOfInterestRequests().isEmpty(), true );
    safeAction( () -> assertEquals( subscription1.isExplicitSubscription(), false ) );
    safeAction( () -> assertEquals( subscription2.isExplicitSubscription(), false ) );

    handler.assertEventCount( 4 );
    handler.assertNextEvent( UnsubscribeFailedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address1 );
      assertEquals( e.getError(), error );
    } );
    handler.assertNextEvent( UnsubscribeFailedEvent.class, e -> {
      assertEquals( e.getSchemaId(), connector.getSchema().getId() );
      assertEquals( e.getAddress(), address2 );
      assertEquals( e.getError(), error );
    } );
  }
}
