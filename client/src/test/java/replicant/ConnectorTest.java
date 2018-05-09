package replicant;

import arez.Arez;
import arez.Disposable;
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
import replicant.spy.SubscribeStartedEvent;
import replicant.spy.SubscriptionUpdateCompletedEvent;
import replicant.spy.SubscriptionUpdateFailedEvent;
import replicant.spy.SubscriptionUpdateStartedEvent;
import replicant.spy.UnsubscribeCompletedEvent;
import replicant.spy.UnsubscribeFailedEvent;
import replicant.spy.UnsubscribeStartedEvent;
import static org.testng.Assert.*;

public class ConnectorTest
  extends AbstractReplicantTest
{
  @Test
  public void construct()
    throws Exception
  {
    final Disposable schedulerLock = Arez.context().pauseScheduler();
    final ReplicantRuntime replicantRuntime = ReplicantRuntime.create();

    Arez.context().safeAction( () -> assertEquals( replicantRuntime.getConnectors().size(), 0 ) );

    final TestConnector connector = TestConnector.create( G.class, replicantRuntime );

    Arez.context().safeAction( () -> assertEquals( replicantRuntime.getConnectors().size(), 1 ) );

    assertEquals( connector.getReplicantRuntime(), replicantRuntime );

    Arez.context()
      .safeAction( () -> Assert.assertEquals( connector.getState(), ConnectorState.DISCONNECTED ) );

    schedulerLock.dispose();

    Arez.context().safeAction( () -> Assert.assertEquals( connector.getState(), ConnectorState.CONNECTING ) );
  }

  @Test
  public void dispose()
  {
    final ReplicantRuntime replicantRuntime = ReplicantRuntime.create();

    Arez.context().safeAction( () -> assertEquals( replicantRuntime.getConnectors().size(), 0 ) );

    final TestConnector connector = TestConnector.create( G.class, replicantRuntime );

    Arez.context().safeAction( () -> assertEquals( replicantRuntime.getConnectors().size(), 1 ) );

    Disposable.dispose( connector );

    Arez.context().safeAction( () -> assertEquals( replicantRuntime.getConnectors().size(), 0 ) );
  }

  @Test
  public void connect()
  {
    final ReplicantRuntime replicantRuntime = ReplicantRuntime.create();
    Arez.context().pauseScheduler();

    final TestConnector connector = TestConnector.create( G.class, replicantRuntime );
    Arez.context().safeAction( () -> Assert.assertEquals( connector.getState(), ConnectorState.DISCONNECTED ) );

    Arez.context().safeAction( connector::connect );

    Arez.context().safeAction( () -> Assert.assertEquals( connector.getState(), ConnectorState.CONNECTING ) );
  }

  @Test
  public void connect_causesError()
  {
    final ReplicantRuntime replicantRuntime = ReplicantRuntime.create();
    Arez.context().pauseScheduler();

    final TestConnector connector = TestConnector.create( G.class, replicantRuntime );
    Arez.context().safeAction( () -> Assert.assertEquals( connector.getState(), ConnectorState.DISCONNECTED ) );

    connector.setErrorOnConnect( true );
    assertThrows( () -> Arez.context().safeAction( connector::connect ) );

    Arez.context().safeAction( () -> Assert.assertEquals( connector.getState(), ConnectorState.ERROR ) );
  }

  @Test
  public void disconnect()
  {
    final ReplicantRuntime replicantRuntime = ReplicantRuntime.create();
    Arez.context().pauseScheduler();

    final TestConnector connector = TestConnector.create( G.class, replicantRuntime );
    Arez.context().safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    Arez.context().safeAction( connector::disconnect );

    Arez.context().safeAction( () -> Assert.assertEquals( connector.getState(), ConnectorState.DISCONNECTING ) );
  }

  @Test
  public void disconnect_causesError()
  {
    final ReplicantRuntime replicantRuntime = ReplicantRuntime.create();
    Arez.context().pauseScheduler();

    final TestConnector connector = TestConnector.create( G.class, replicantRuntime );
    Arez.context().safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    connector.setErrorOnDisconnect( true );
    assertThrows( () -> Arez.context().safeAction( connector::disconnect ) );

    Arez.context().safeAction( () -> Assert.assertEquals( connector.getState(), ConnectorState.ERROR ) );
  }

  @Test
  public void onDisconnected()
  {
    final TestConnector connector = TestConnector.create( G.class );

    Arez.context().safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    Arez.context().safeAction( () -> assertEquals( connector.getReplicantRuntime().getState(),
                                                   RuntimeState.CONNECTING ) );

    // Pause scheduler so runtime does not try to update state
    Arez.context().pauseScheduler();

    Arez.context().safeAction( connector::onDisconnected );

    Arez.context().safeAction( () -> assertEquals( connector.getState(), ConnectorState.DISCONNECTED ) );
    Arez.context().safeAction( () -> assertEquals( connector.getReplicantRuntime().getState(),
                                                   RuntimeState.DISCONNECTED ) );
  }

  @Test
  public void onDisconnected_generatesSpyMessage()
    throws Exception
  {
    final TestConnector connector = TestConnector.create( G.class );

    Arez.context().safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    // Pause scheduler so runtime does not try to update state
    Arez.context().pauseScheduler();

    Arez.context().safeAction( connector::onDisconnected );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( DisconnectedEvent.class,
                             e -> assertEquals( e.getSystemType(), connector.getSystemType() ) );
  }

  @Test
  public void onDisconnectFailure()
    throws Exception
  {
    final TestConnector connector = TestConnector.create( G.class );

    Arez.context().safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    Arez.context().safeAction( () -> assertEquals( connector.getReplicantRuntime().getState(),
                                                   RuntimeState.CONNECTING ) );

    final Throwable error = new Throwable();

    // Pause scheduler so runtime does not try to update state
    Arez.context().pauseScheduler();

    Arez.context().safeAction( () -> connector.onDisconnectFailure( error ) );

    Arez.context().safeAction( () -> assertEquals( connector.getState(), ConnectorState.ERROR ) );
    Arez.context().safeAction( () -> assertEquals( connector.getReplicantRuntime().getState(),
                                                   RuntimeState.ERROR ) );
  }

  @Test
  public void onDisconnectFailure_generatesSpyMessage()
    throws Exception
  {
    final TestConnector connector = TestConnector.create( G.class );

    Arez.context().safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    final Throwable error = new Throwable();

    // Pause scheduler so runtime does not try to update state
    Arez.context().pauseScheduler();

    Arez.context().safeAction( () -> connector.onDisconnectFailure( error ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( DisconnectFailureEvent.class, e -> {
      assertEquals( e.getSystemType(), connector.getSystemType() );
      assertEquals( e.getError(), error );
    } );
  }

  @Test
  public void onConnected()
    throws Exception
  {
    final TestConnector connector = TestConnector.create( G.class );

    Arez.context().safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    Arez.context().safeAction( () -> assertEquals( connector.getReplicantRuntime().getState(),
                                                   RuntimeState.CONNECTING ) );

    // Pause scheduler so runtime does not try to update state
    Arez.context().pauseScheduler();

    Arez.context().safeAction( connector::onConnected );

    Arez.context().safeAction( () -> assertEquals( connector.getState(), ConnectorState.CONNECTED ) );
    Arez.context().safeAction( () -> assertEquals( connector.getReplicantRuntime().getState(),
                                                   RuntimeState.CONNECTED ) );
  }

  @Test
  public void onConnected_generatesSpyMessage()
    throws Exception
  {
    final TestConnector connector = TestConnector.create( G.class );

    Arez.context().safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    Arez.context().safeAction( connector::onConnected );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( ConnectedEvent.class,
                             e -> assertEquals( e.getSystemType(), connector.getSystemType() ) );
  }

  @Test
  public void onConnectFailure()
    throws Exception
  {
    final TestConnector connector = TestConnector.create( G.class );

    Arez.context().safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    Arez.context().safeAction( () -> assertEquals( connector.getReplicantRuntime().getState(),
                                                   RuntimeState.CONNECTING ) );

    final Throwable error = new Throwable();

    // Pause scheduler so runtime does not try to update state
    Arez.context().pauseScheduler();

    Arez.context().safeAction( () -> connector.onConnectFailure( error ) );

    Arez.context().safeAction( () -> assertEquals( connector.getState(), ConnectorState.ERROR ) );
    Arez.context().safeAction( () -> assertEquals( connector.getReplicantRuntime().getState(),
                                                   RuntimeState.ERROR ) );
  }

  @Test
  public void onConnectFailure_generatesSpyMessage()
    throws Exception
  {
    final TestConnector connector = TestConnector.create( G.class );

    Arez.context().safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    final Throwable error = new Throwable();

    // Pause scheduler so runtime does not try to update state
    Arez.context().pauseScheduler();

    Arez.context().safeAction( () -> connector.onConnectFailure( error ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( ConnectFailureEvent.class, e -> {
      assertEquals( e.getSystemType(), connector.getSystemType() );
      assertEquals( e.getError(), error );
    } );
  }

  @Test
  public void onMessageProcessed_generatesSpyMessage()
    throws Exception
  {
    final TestConnector connector = TestConnector.create( G.class );

    Arez.context().safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    final DataLoadStatus status =
      new DataLoadStatus( ValueUtil.randomString(),
                          ValueUtil.randomInt(),
                          ValueUtil.randomString(),
                          ValueUtil.getRandom().nextInt( 10 ),
                          ValueUtil.getRandom().nextInt( 10 ),
                          ValueUtil.getRandom().nextInt( 10 ),
                          ValueUtil.getRandom().nextInt( 100 ),
                          ValueUtil.getRandom().nextInt( 100 ),
                          ValueUtil.getRandom().nextInt( 10 ) );

    Arez.context().safeAction( () -> connector.onMessageProcessed( status ) );

    handler.assertEventCount( 1 );

    handler.assertNextEvent( MessageProcessedEvent.class, e -> {
      assertEquals( e.getSystemType(), connector.getSystemType() );
      assertEquals( e.getDataLoadStatus(), status );
    } );
  }

  @Test
  public void onMessageProcessFailure()
    throws Exception
  {
    final TestConnector connector = TestConnector.create( G.class );
    Arez.context().safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    final Throwable error = new Throwable();

    // Pause scheduler so runtime does not try to update state
    Arez.context().pauseScheduler();

    Arez.context().safeAction( () -> connector.onMessageProcessFailure( error ) );

    Arez.context().safeAction( () -> assertEquals( connector.getState(), ConnectorState.DISCONNECTING ) );
  }

  @Test
  public void onMessageProcessFailure_generatesSpyMessage()
    throws Exception
  {
    final TestConnector connector = TestConnector.create( G.class );

    Arez.context().safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    final Throwable error = new Throwable();

    Arez.context().safeAction( () -> connector.onMessageProcessFailure( error ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( MessageProcessFailureEvent.class, e -> {
      assertEquals( e.getSystemType(), connector.getSystemType() );
      assertEquals( e.getError(), error );
    } );
  }

  @Test
  public void disconnectIfPossible()
    throws Exception
  {
    final TestConnector connector = TestConnector.create( G.class );

    Arez.context().safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    final Throwable error = new Throwable();

    Arez.context().safeAction( () -> connector.disconnectIfPossible( error ) );

    Arez.context().safeAction( () -> assertEquals( connector.getState(), ConnectorState.DISCONNECTING ) );
  }

  @Test
  public void disconnectIfPossible_noActionAsConnecting()
    throws Exception
  {
    final TestConnector connector = TestConnector.create( G.class );

    Arez.context().safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    final Throwable error = new Throwable();

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    Arez.context().safeAction( () -> connector.disconnectIfPossible( error ) );

    handler.assertEventCount( 0 );

    Arez.context().safeAction( () -> assertEquals( connector.getState(), ConnectorState.CONNECTING ) );
  }

  @Test
  public void disconnectIfPossible_generatesSpyEvent()
    throws Exception
  {
    final TestConnector connector = TestConnector.create( G.class );

    Arez.context().safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    final Throwable error = new Throwable();

    Arez.context().safeAction( () -> connector.disconnectIfPossible( error ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( RestartEvent.class, e -> {
      assertEquals( e.getSystemType(), connector.getSystemType() );
      assertEquals( e.getError(), error );
    } );
  }

  @Test
  public void onMessageReadFailure()
    throws Exception
  {
    final TestConnector connector = TestConnector.create( G.class );
    Arez.context().safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    final Throwable error = new Throwable();

    // Pause scheduler so runtime does not try to update state
    Arez.context().pauseScheduler();

    Arez.context().safeAction( () -> connector.onMessageReadFailure( error ) );

    Arez.context().safeAction( () -> assertEquals( connector.getState(), ConnectorState.DISCONNECTING ) );
  }

  @Test
  public void onMessageReadFailure_generatesSpyMessage()
    throws Exception
  {
    final TestConnector connector = TestConnector.create( G.class );

    Arez.context().safeAction( () -> connector.setState( ConnectorState.CONNECTING ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    final Throwable error = new Throwable();

    Arez.context().safeAction( () -> connector.onMessageReadFailure( error ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( MessageReadFailureEvent.class, e -> {
      assertEquals( e.getSystemType(), connector.getSystemType() );
      assertEquals( e.getError(), error );
    } );
  }

  @Test
  public void onSubscribeStarted()
    throws Exception
  {
    final TestConnector connector = TestConnector.create( G.class );

    final ChannelAddress address = new ChannelAddress( G.G1 );
    final AreaOfInterest areaOfInterest =
      Arez.context().safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    Arez.context().safeAction( () -> connector.onSubscribeStarted( address ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.LOADING ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( SubscribeStartedEvent.class, e -> {
      assertEquals( e.getSystemType(), connector.getSystemType() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @Test
  public void onSubscribeCompleted()
    throws Exception
  {
    final TestConnector connector = TestConnector.create( G.class );

    final ChannelAddress address = new ChannelAddress( G.G1 );
    final AreaOfInterest areaOfInterest =
      Arez.context().safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    final Subscription subscription =
      Arez.context().safeAction( () -> Replicant.context().createSubscription( address, null, true ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    Arez.context().safeAction( () -> connector.onSubscribeCompleted( address ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.LOADED ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), subscription ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( SubscribeCompletedEvent.class, e -> {
      assertEquals( e.getSystemType(), connector.getSystemType() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @Test
  public void onSubscribeFailed()
    throws Exception
  {
    final TestConnector connector = TestConnector.create( G.class );

    final ChannelAddress address = new ChannelAddress( G.G1 );
    final AreaOfInterest areaOfInterest =
      Arez.context().safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    final Throwable error = new Throwable();

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    Arez.context().safeAction( () -> connector.onSubscribeFailed( address, error ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.LOAD_FAILED ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), error ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( SubscribeFailedEvent.class, e -> {
      assertEquals( e.getSystemType(), connector.getSystemType() );
      assertEquals( e.getAddress(), address );
      assertEquals( e.getError(), error );
    } );
  }

  @Test
  public void onUnsubscribeStarted()
    throws Exception
  {
    final TestConnector connector = TestConnector.create( G.class );

    final ChannelAddress address = new ChannelAddress( G.G1 );
    final AreaOfInterest areaOfInterest =
      Arez.context().safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    final Subscription subscription =
      Arez.context().safeAction( () -> Replicant.context().createSubscription( address, null, true ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    Arez.context().safeAction( () -> connector.onUnsubscribeStarted( address ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.UNLOADING ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), subscription ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( UnsubscribeStartedEvent.class, e -> {
      assertEquals( e.getSystemType(), connector.getSystemType() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @Test
  public void onUnsubscribeCompleted()
    throws Exception
  {
    final TestConnector connector = TestConnector.create( G.class );

    final ChannelAddress address = new ChannelAddress( G.G1 );
    final AreaOfInterest areaOfInterest =
      Arez.context().safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    Arez.context().safeAction( () -> connector.onUnsubscribeCompleted( address ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.UNLOADED ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( UnsubscribeCompletedEvent.class, e -> {
      assertEquals( e.getSystemType(), connector.getSystemType() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @Test
  public void onUnsubscribeFailed()
    throws Exception
  {
    final TestConnector connector = TestConnector.create( G.class );

    final ChannelAddress address = new ChannelAddress( G.G1 );
    final AreaOfInterest areaOfInterest =
      Arez.context().safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    final Throwable error = new Throwable();

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    Arez.context().safeAction( () -> connector.onUnsubscribeFailed( address, error ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.UNLOADED ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( UnsubscribeFailedEvent.class, e -> {
      assertEquals( e.getSystemType(), connector.getSystemType() );
      assertEquals( e.getAddress(), address );
      assertEquals( e.getError(), error );
    } );
  }

  @Test
  public void onSubscriptionUpdateStarted()
    throws Exception
  {
    final TestConnector connector = TestConnector.create( G.class );

    final ChannelAddress address = new ChannelAddress( G.G1 );
    final AreaOfInterest areaOfInterest =
      Arez.context().safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    final Subscription subscription =
      Arez.context().safeAction( () -> Replicant.context().createSubscription( address, null, true ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    Arez.context().safeAction( () -> connector.onSubscriptionUpdateStarted( address ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.UPDATING ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), subscription ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( SubscriptionUpdateStartedEvent.class, e -> {
      assertEquals( e.getSystemType(), connector.getSystemType() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @Test
  public void onSubscriptionUpdateCompleted()
    throws Exception
  {
    final TestConnector connector = TestConnector.create( G.class );

    final ChannelAddress address = new ChannelAddress( G.G1 );
    final AreaOfInterest areaOfInterest =
      Arez.context().safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    final Subscription subscription =
      Arez.context().safeAction( () -> Replicant.context().createSubscription( address, null, true ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    Arez.context().safeAction( () -> connector.onSubscriptionUpdateCompleted( address ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.UPDATED ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), subscription ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( SubscriptionUpdateCompletedEvent.class, e -> {
      assertEquals( e.getSystemType(), connector.getSystemType() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @Test
  public void onSubscriptionUpdateFailed()
    throws Exception
  {
    final TestConnector connector = TestConnector.create( G.class );

    final ChannelAddress address = new ChannelAddress( G.G1 );
    final AreaOfInterest areaOfInterest =
      Arez.context().safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    final Throwable error = new Throwable();

    final Subscription subscription =
      Arez.context().safeAction( () -> Replicant.context().createSubscription( address, null, true ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    Arez.context().safeAction( () -> connector.onSubscriptionUpdateFailed( address, error ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.UPDATE_FAILED ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), subscription ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), error ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( SubscriptionUpdateFailedEvent.class, e -> {
      assertEquals( e.getSystemType(), connector.getSystemType() );
      assertEquals( e.getAddress(), address );
      assertEquals( e.getError(), error );
    } );
  }

  enum G
  {
    G1
  }
}
