package replicant;

import arez.Arez;
import arez.ArezContext;
import arez.Disposable;
import java.util.ArrayList;
import java.util.Objects;
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

public class ConnectorTest
  extends AbstractReplicantTest
{
  @Test
  public void construct()
    throws Exception
  {
    final Disposable schedulerLock = Arez.context().pauseScheduler();
    final ReplicantRuntime runtime = Replicant.context().getRuntime();

    Arez.context().safeAction( () -> assertEquals( runtime.getConnectors().size(), 0 ) );

    final TestConnector connector = TestConnector.create( G.class );

    Arez.context().safeAction( () -> assertEquals( runtime.getConnectors().size(), 1 ) );

    assertEquals( connector.getReplicantRuntime(), runtime );

    Arez.context()
      .safeAction( () -> Assert.assertEquals( connector.getState(), ConnectorState.DISCONNECTED ) );

    schedulerLock.dispose();

    Arez.context().safeAction( () -> Assert.assertEquals( connector.getState(), ConnectorState.CONNECTING ) );
  }

  @Test
  public void dispose()
  {
    final ReplicantRuntime runtime = Replicant.context().getRuntime();

    Arez.context().safeAction( () -> assertEquals( runtime.getConnectors().size(), 0 ) );

    final TestConnector connector = TestConnector.create( G.class );

    Arez.context().safeAction( () -> assertEquals( runtime.getConnectors().size(), 1 ) );

    Disposable.dispose( connector );

    Arez.context().safeAction( () -> assertEquals( runtime.getConnectors().size(), 0 ) );
  }

  @Test
  public void connect()
  {
    Arez.context().pauseScheduler();

    final TestConnector connector = TestConnector.create( G.class );
    Arez.context().safeAction( () -> Assert.assertEquals( connector.getState(), ConnectorState.DISCONNECTED ) );

    Arez.context().safeAction( connector::connect );

    Arez.context().safeAction( () -> Assert.assertEquals( connector.getState(), ConnectorState.CONNECTING ) );
  }

  @Test
  public void connect_causesError()
  {
    Arez.context().pauseScheduler();

    final TestConnector connector = TestConnector.create( G.class );
    Arez.context().safeAction( () -> Assert.assertEquals( connector.getState(), ConnectorState.DISCONNECTED ) );

    connector.setErrorOnConnect( true );
    assertThrows( () -> Arez.context().safeAction( connector::connect ) );

    Arez.context().safeAction( () -> Assert.assertEquals( connector.getState(), ConnectorState.ERROR ) );
  }

  @Test
  public void disconnect()
  {
    Arez.context().pauseScheduler();

    final TestConnector connector = TestConnector.create( G.class );
    Arez.context().safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    Arez.context().safeAction( connector::disconnect );

    Arez.context().safeAction( () -> Assert.assertEquals( connector.getState(), ConnectorState.DISCONNECTING ) );
  }

  @Test
  public void disconnect_causesError()
  {
    Arez.context().pauseScheduler();

    final TestConnector connector = TestConnector.create( G.class );
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

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

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

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

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

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

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

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

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

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final DataLoadStatus status =
      new DataLoadStatus( ValueUtil.randomInt(),
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

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

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

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

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

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

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

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

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

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

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

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

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

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

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

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

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

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

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

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

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

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

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

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

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

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

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

  @Test
  public void areaOfInterestRequestPendingQueries()
  {
    final TestConnector connector = TestConnector.create( G.class );

    final ChannelAddress address = new ChannelAddress( G.G1 );

    assertEquals( connector.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.ADD, address, null ), false );
    assertEquals( connector.lastIndexOfPendingAreaOfInterestRequest( AreaOfInterestRequest.Type.ADD, address, null ),
                  -1 );

    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    assertEquals( connector.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.ADD, address, null ), false );
    assertEquals( connector.lastIndexOfPendingAreaOfInterestRequest( AreaOfInterestRequest.Type.ADD, address, null ),
                  -1 );

    connection.requestSubscribe( address, null );

    assertEquals( connector.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.ADD, address, null ), true );
    assertEquals( connector.lastIndexOfPendingAreaOfInterestRequest( AreaOfInterestRequest.Type.ADD, address, null ),
                  1 );
  }

  @Test
  public void connection()
  {
    final TestConnector connector = TestConnector.create( G.class );

    assertEquals( connector.getConnection(), null );

    final Connection connection = new Connection( connector, ValueUtil.randomString() );

    connector.setConnection( connection );

    final Subscription subscription1 =
      Arez.context()
        .safeAction( () -> Replicant.context().createSubscription( new ChannelAddress( G.G1 ), null, true ) );

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
    final TestConnector connector = TestConnector.create( G.class );

    final IllegalStateException exception = expectThrows( IllegalStateException.class, connector::ensureConnection );

    assertEquals( exception.getMessage(),
                  "Replicant-0031: Connector.ensureConnection() when no connection is present." );
  }

  @Test
  public void purgeSubscriptions()
  {
    final TestConnector connector1 = TestConnector.create( G.class );
    TestConnector.create( F.class );

    final ArezContext context = Arez.context();
    final ReplicantContext rContext = Replicant.context();

    final Subscription subscription1 =
      context.safeAction( () -> rContext.createSubscription( new ChannelAddress( G.G1 ), null, true ) );
    final Subscription subscription2 =
      context.safeAction( () -> rContext.createSubscription( new ChannelAddress( G.G2, 2 ), null, true ) );
    // The next two are from a different Connector
    final Subscription subscription3 =
      context.safeAction( () -> rContext.createSubscription( new ChannelAddress( F.F2, 1 ), null, true ) );
    final Subscription subscription4 =
      context.safeAction( () -> rContext.createSubscription( new ChannelAddress( F.F2, 2 ), null, true ) );

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
    final TestConnector connector = TestConnector.create( G.class );

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
    final TestConnector connector = TestConnector.create( G.class );

    connector.triggerScheduler();

    assertEquals( connector.getProgressAreaOfInterestRequestProcessingCount(), 0 );
    assertEquals( connector.getProgressResponseProcessingCount(), 0 );
    assertNull( connector.getSchedulerLock() );

    connector.setProgressAreaOfInterestRequestProcessing( () -> true );
    connector.setProgressResponseProcessing( () -> true );

    final boolean result1 = connector.scheduleTick();

    assertEquals( result1, true );
    assertEquals( connector.getProgressAreaOfInterestRequestProcessingCount(), 1 );
    assertEquals( connector.getProgressResponseProcessingCount(), 1 );
    final Disposable schedulerLock1 = connector.getSchedulerLock();
    assertNotNull( schedulerLock1 );

    final boolean result2 = connector.scheduleTick();

    assertEquals( result2, true );
    assertEquals( connector.getProgressAreaOfInterestRequestProcessingCount(), 2 );
    assertEquals( connector.getProgressResponseProcessingCount(), 2 );
    assertNotNull( connector.getSchedulerLock() );

    connector.setProgressAreaOfInterestRequestProcessing( () -> false );
    connector.setProgressResponseProcessing( () -> false );

    final boolean result3 = connector.scheduleTick();

    assertEquals( result3, false );
    assertEquals( connector.getProgressAreaOfInterestRequestProcessingCount(), 3 );
    assertEquals( connector.getProgressResponseProcessingCount(), 3 );
    assertNull( connector.getSchedulerLock() );
    assertTrue( Disposable.isDisposed( schedulerLock1 ) );
  }

  @Test
  public void scheduleTick_withError()
  {
    final TestConnector connector = TestConnector.create( G.class );

    connector.triggerScheduler();

    assertEquals( connector.getProgressAreaOfInterestRequestProcessingCount(), 0 );
    assertEquals( connector.getProgressResponseProcessingCount(), 0 );
    assertNull( connector.getSchedulerLock() );

    connector.setProgressAreaOfInterestRequestProcessing( () -> true );
    connector.setProgressResponseProcessing( () -> true );

    final boolean result1 = connector.scheduleTick();

    assertEquals( result1, true );
    assertEquals( connector.getProgressAreaOfInterestRequestProcessingCount(), 1 );
    assertEquals( connector.getProgressResponseProcessingCount(), 1 );
    final Disposable schedulerLock1 = connector.getSchedulerLock();
    assertNotNull( schedulerLock1 );

    final IllegalStateException error = new IllegalStateException();
    connector.setProgressAreaOfInterestRequestProcessing( () -> {
      throw error;
    } );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final boolean result2 = connector.scheduleTick();

    assertEquals( result2, false );
    assertEquals( connector.getProgressAreaOfInterestRequestProcessingCount(), 2 );
    assertEquals( connector.getProgressResponseProcessingCount(), 1 );

    assertNull( connector.getSchedulerLock() );
    assertTrue( Disposable.isDisposed( schedulerLock1 ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( MessageProcessFailureEvent.class, e -> {
      assertEquals( e.getSystemType(), connector.getSystemType() );
      assertEquals( e.getError(), error );
    } );
  }

  @Test
  public void requestSubscribe()
  {
    final TestConnector connector = TestConnector.create( G.class );
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );

    final ChannelAddress address = new ChannelAddress( G.G1 );

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
    final TestConnector connector = TestConnector.create( G.class );
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );

    final ChannelAddress address = new ChannelAddress( G.G1 );

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
    final TestConnector connector = TestConnector.create( G.class );
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );

    final ChannelAddress address = new ChannelAddress( G.G1 );

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
    final TestConnector connector = TestConnector.create( G.class );
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );

    final ChannelAddress address1 = new ChannelAddress( G.G2, 1 );
    final ChannelAddress address2 = new ChannelAddress( G.G2, 2 );

    final ArezContext context = Arez.context();
    final ReplicantContext rContext = Replicant.context();

    final Subscription subscription1 =
      context.safeAction( () -> rContext.createSubscription( address1, ValueUtil.randomString(), true ) );
    final Subscription subscription2 =
      context.safeAction( () -> rContext.createSubscription( address2, ValueUtil.randomString(), true ) );

    // Use Integer and String as arbitrary types for our entities...
    // Anything with id below 0 will be removed during update ...
    final Entity entity1 = context.safeAction( () -> rContext.findOrCreateEntity( Integer.class, -1 ) );
    final Entity entity2 = context.safeAction( () -> rContext.findOrCreateEntity( Integer.class, -2 ) );
    final Entity entity3 = context.safeAction( () -> rContext.findOrCreateEntity( Integer.class, -3 ) );
    final Entity entity4 = context.safeAction( () -> rContext.findOrCreateEntity( Integer.class, -4 ) );
    final Entity entity5 = context.safeAction( () -> rContext.findOrCreateEntity( String.class, 5 ) );
    final Entity entity6 = context.safeAction( () -> rContext.findOrCreateEntity( String.class, 6 ) );

    context.safeAction( () -> {
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

    context.safeAction( () -> connector.updateSubscriptionForFilteredEntities( subscription1 ) );

    context.safeAction( () -> {
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
    final TestConnector connector = TestConnector.create( G.class );
    assertEquals( connector.toAddress( ChannelChange.create( 0, ChannelChange.Action.ADD, null ) ),
                  new ChannelAddress( G.G1 ) );
    assertEquals( connector.toAddress( ChannelChange.create( 1, 2, ChannelChange.Action.ADD, null ) ),
                  new ChannelAddress( G.G2, 2 ) );
  }

  @Test
  public void processEntityChanges()
  {
    final TestConnector connector = TestConnector.create( G.class );
    connector.setLinksToProcessPerTick( 1 );

    final MessageResponse response = new MessageResponse( ValueUtil.randomString() );

    final Linkable entity1 = mock( Linkable.class );
    final Linkable entity2 = mock( Linkable.class );
    final Linkable entity3 = mock( Linkable.class );

    final EntityChange[] entityChanges = {
      // Update changes
      EntityChange.create( 1, 1, new EntityChannel[]{ EntityChannel.create( 1 ) }, mock( EntityChangeData.class ) ),
      EntityChange.create( 2, 1, new EntityChannel[]{ EntityChannel.create( 1 ) }, mock( EntityChangeData.class ) ),
      // Remove change
      EntityChange.create( 3, 1, new EntityChannel[]{ EntityChannel.create( 1 ) } )
    };
    final ChangeSet changeSet = ChangeSet.create( ValueUtil.randomInt(), null, null, null, entityChanges );
    response.recordChangeSet( changeSet, null );

    when( connector.getChangeMapper().applyChange( entityChanges[ 0 ] ) ).thenReturn( entity1 );
    when( connector.getChangeMapper().applyChange( entityChanges[ 1 ] ) ).thenReturn( entity2 );
    when( connector.getChangeMapper().applyChange( entityChanges[ 2 ] ) ).thenReturn( entity3 );

    verify( connector.getChangeMapper(), never() ).applyChange( any( EntityChange.class ) );

    assertEquals( response.getUpdatedEntities().size(), 0 );
    assertEquals( response.getEntityUpdateCount(), 0 );
    assertEquals( response.getEntityRemoveCount(), 0 );

    connector.setChangesToProcessPerTick( 1 );

    connector.processEntityChanges( response );

    verify( connector.getChangeMapper(), times( 1 ) ).applyChange( any( EntityChange.class ) );

    assertEquals( response.getUpdatedEntities().size(), 1 );
    assertEquals( response.getUpdatedEntities().contains( entity1 ), true );
    assertEquals( response.getEntityUpdateCount(), 1 );
    assertEquals( response.getEntityRemoveCount(), 0 );

    connector.setChangesToProcessPerTick( 2 );

    connector.processEntityChanges( response );

    verify( connector.getChangeMapper(), times( 3 ) ).applyChange( any( EntityChange.class ) );

    assertEquals( response.getUpdatedEntities().size(), 2 );
    assertEquals( response.getUpdatedEntities().contains( entity1 ), true );
    assertEquals( response.getUpdatedEntities().contains( entity2 ), true );
    assertEquals( response.getEntityUpdateCount(), 2 );
    assertEquals( response.getEntityRemoveCount(), 1 );
  }

  @Test
  public void processEntityLinks()
  {
    final TestConnector connector = TestConnector.create( G.class );
    connector.setLinksToProcessPerTick( 1 );

    final MessageResponse response = new MessageResponse( ValueUtil.randomString() );

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

    connector.processEntityLinks( response );

    assertEquals( response.getEntityLinkCount(), 1 );
    verify( entity1, times( 1 ) ).link();
    verify( entity2, never() ).link();
    verify( entity3, never() ).link();
    verify( entity4, never() ).link();

    connector.setLinksToProcessPerTick( 2 );

    connector.processEntityLinks( response );

    assertEquals( response.getEntityLinkCount(), 3 );
    verify( entity1, times( 1 ) ).link();
    verify( entity2, times( 1 ) ).link();
    verify( entity3, times( 1 ) ).link();
    verify( entity4, never() ).link();
  }

  @Test
  public void completeAreaOfInterestRequest()
  {
    final TestConnector connector = TestConnector.create( G.class );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    connection.injectCurrentAreaOfInterestRequest( new AreaOfInterestRequest( new ChannelAddress( G.G1 ),
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
    final TestConnector connector = TestConnector.create( G.class );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final MessageResponse response = new MessageResponse( ValueUtil.randomString() );

    final int channelId = G.G1.ordinal();
    final int subChannelId = ValueUtil.randomInt();
    final String filter = ValueUtil.randomString();
    final ChannelChange[] channelChanges =
      new ChannelChange[]{ ChannelChange.create( channelId, subChannelId, ChannelChange.Action.ADD, filter ) };
    response.recordChangeSet( ChangeSet.create( ValueUtil.randomInt(), null, null, channelChanges, null ), null );

    assertEquals( response.needsChannelChangesProcessed(), true );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    connector.processChannelChanges( response );

    assertEquals( response.needsChannelChangesProcessed(), false );

    final ChannelAddress address = new ChannelAddress( G.G1, subChannelId );
    final Subscription subscription =
      Arez.context().safeAction( () -> Replicant.context().findSubscription( address ) );
    assertNotNull( subscription );
    assertEquals( subscription.getAddress(), address );
    Arez.context().safeAction( () -> assertEquals( subscription.getFilter(), filter ) );
    Arez.context().safeAction( () -> assertEquals( subscription.isExplicitSubscription(), false ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscriptionCreatedEvent.class, e -> {
      assertEquals( e.getSubscription().getAddress(), address );
      Arez.context().safeAction( () -> assertEquals( e.getSubscription().getFilter(), filter ) );
    } );
  }

  @Test
  public void processChannelChanges_addConvertingImplicitToExplicit()
  {
    final TestConnector connector = TestConnector.create( G.class );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final MessageResponse response = new MessageResponse( ValueUtil.randomString() );

    final ChannelAddress address = new ChannelAddress( G.G1, ValueUtil.randomInt() );
    final int channelId = address.getChannelType().ordinal();
    final int subChannelId = Objects.requireNonNull( address.getId() );

    final String filter = ValueUtil.randomString();
    final ChannelChange[] channelChanges =
      new ChannelChange[]{ ChannelChange.create( channelId, subChannelId, ChannelChange.Action.ADD, filter ) };
    response.recordChangeSet( ChangeSet.create( ValueUtil.randomInt(), null, null, channelChanges, null ), null );

    final AreaOfInterestRequest request = new AreaOfInterestRequest( address, AreaOfInterestRequest.Type.ADD, filter );
    connection.injectCurrentAreaOfInterestRequest( request );
    request.markAsInProgress();

    assertEquals( response.needsChannelChangesProcessed(), true );
    assertEquals( response.getChannelAddCount(), 0 );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    connector.processChannelChanges( response );

    assertEquals( response.needsChannelChangesProcessed(), false );
    assertEquals( response.getChannelAddCount(), 1 );

    final Subscription subscription =
      Arez.context().safeAction( () -> Replicant.context().findSubscription( address ) );
    assertNotNull( subscription );
    assertEquals( subscription.getAddress(), address );
    Arez.context().safeAction( () -> assertEquals( subscription.getFilter(), filter ) );
    Arez.context().safeAction( () -> assertEquals( subscription.isExplicitSubscription(), true ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscriptionCreatedEvent.class, e -> {
      assertEquals( e.getSubscription().getAddress(), address );
      Arez.context().safeAction( () -> assertEquals( e.getSubscription().getFilter(), filter ) );
    } );
  }

  @Test
  public void processChannelChanges_remove()
  {
    final TestConnector connector = TestConnector.create( G.class );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final MessageResponse response = new MessageResponse( ValueUtil.randomString() );

    final ChannelAddress address = new ChannelAddress( G.G1, ValueUtil.randomInt() );
    final int channelId = address.getChannelType().ordinal();
    final int subChannelId = Objects.requireNonNull( address.getId() );

    final String filter = ValueUtil.randomString();
    final ChannelChange[] channelChanges =
      new ChannelChange[]{ ChannelChange.create( channelId, subChannelId, ChannelChange.Action.REMOVE, null ) };
    response.recordChangeSet( ChangeSet.create( ValueUtil.randomInt(), null, null, channelChanges, null ), null );

    final Subscription initialSubscription =
      Arez.context().safeAction( () -> Replicant.context().createSubscription( address, filter, true ) );

    assertEquals( response.needsChannelChangesProcessed(), true );
    assertEquals( response.getChannelRemoveCount(), 0 );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    connector.processChannelChanges( response );

    assertEquals( response.needsChannelChangesProcessed(), false );
    assertEquals( response.getChannelRemoveCount(), 1 );

    final Subscription subscription =
      Arez.context().safeAction( () -> Replicant.context().findSubscription( address ) );
    assertNull( subscription );
    assertEquals( Disposable.isDisposed( initialSubscription ), true );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscriptionDisposedEvent.class,
                             e -> assertEquals( e.getSubscription().getAddress(), address ) );
  }

  @Test
  public void processChannelChanges_remove_WithMissingSubscription()
  {
    final TestConnector connector = TestConnector.create( G.class );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final MessageResponse response = new MessageResponse( ValueUtil.randomString() );

    final ChannelAddress address = new ChannelAddress( G.G1, 72 );
    final int channelId = address.getChannelType().ordinal();
    final int subChannelId = Objects.requireNonNull( address.getId() );

    final ChannelChange[] channelChanges =
      new ChannelChange[]{ ChannelChange.create( channelId, subChannelId, ChannelChange.Action.REMOVE, null ) };
    response.recordChangeSet( ChangeSet.create( ValueUtil.randomInt(), null, null, channelChanges, null ), null );

    assertEquals( response.needsChannelChangesProcessed(), true );
    assertEquals( response.getChannelRemoveCount(), 0 );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> connector.processChannelChanges( response ) );

    assertEquals( response.needsChannelChangesProcessed(), true );
    assertEquals( response.getChannelRemoveCount(), 0 );
    assertEquals( exception.getMessage(),
                  "Replicant-0028: Received ChannelChange of type REMOVE for address G.G1:72 but no such subscription exists." );

    handler.assertEventCount( 0 );
  }

  @Test
  public void processChannelChanges_update()
  {
    final TestConnector connector = TestConnector.create( G.class );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final MessageResponse response = new MessageResponse( ValueUtil.randomString() );

    final ChannelAddress address = new ChannelAddress( G.G1, ValueUtil.randomInt() );
    final int channelId = address.getChannelType().ordinal();
    final int subChannelId = Objects.requireNonNull( address.getId() );

    final String oldFilter = ValueUtil.randomString();
    final String newFilter = ValueUtil.randomString();
    final ChannelChange[] channelChanges =
      new ChannelChange[]{ ChannelChange.create( channelId, subChannelId, ChannelChange.Action.UPDATE, newFilter ) };
    response.recordChangeSet( ChangeSet.create( ValueUtil.randomInt(), null, null, channelChanges, null ), null );

    final Subscription initialSubscription =
      Arez.context().safeAction( () -> Replicant.context().createSubscription( address, oldFilter, true ) );

    assertEquals( response.needsChannelChangesProcessed(), true );
    assertEquals( response.getChannelUpdateCount(), 0 );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    connector.processChannelChanges( response );

    assertEquals( response.needsChannelChangesProcessed(), false );
    assertEquals( response.getChannelUpdateCount(), 1 );

    final Subscription subscription =
      Arez.context().safeAction( () -> Replicant.context().findSubscription( address ) );
    assertNotNull( subscription );
    assertEquals( Disposable.isDisposed( initialSubscription ), false );

    handler.assertEventCount( 0 );
  }

  @Test
  public void processChannelChanges_update_implicitSubscription()
  {
    final TestConnector connector = TestConnector.create( G.class );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final MessageResponse response = new MessageResponse( ValueUtil.randomString() );

    final ChannelAddress address = new ChannelAddress( G.G1, 33 );
    final int channelId = address.getChannelType().ordinal();
    final int subChannelId = Objects.requireNonNull( address.getId() );

    final String oldFilter = ValueUtil.randomString();
    final String newFilter = ValueUtil.randomString();
    final ChannelChange[] channelChanges =
      new ChannelChange[]{ ChannelChange.create( channelId, subChannelId, ChannelChange.Action.UPDATE, newFilter ) };
    response.recordChangeSet( ChangeSet.create( ValueUtil.randomInt(), null, null, channelChanges, null ), null );

    Arez.context().safeAction( () -> Replicant.context().createSubscription( address, oldFilter, false ) );

    assertEquals( response.needsChannelChangesProcessed(), true );
    assertEquals( response.getChannelUpdateCount(), 0 );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> connector.processChannelChanges( response ) );

    assertEquals( response.needsChannelChangesProcessed(), true );
    assertEquals( response.getChannelUpdateCount(), 0 );

    assertEquals( exception.getMessage(),
                  "Replicant-0029: Received ChannelChange of type UPDATE for address G.G1:33 but subscription is implicitly subscribed." );

    handler.assertEventCount( 0 );
  }

  @Test
  public void processChannelChanges_update_missingSubscription()
  {
    final TestConnector connector = TestConnector.create( G.class );
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final MessageResponse response = new MessageResponse( ValueUtil.randomString() );

    final ChannelAddress address = new ChannelAddress( G.G1, 42 );
    final int channelId = address.getChannelType().ordinal();
    final int subChannelId = Objects.requireNonNull( address.getId() );

    final String newFilter = ValueUtil.randomString();
    final ChannelChange[] channelChanges =
      new ChannelChange[]{ ChannelChange.create( channelId, subChannelId, ChannelChange.Action.UPDATE, newFilter ) };
    response.recordChangeSet( ChangeSet.create( ValueUtil.randomInt(), null, null, channelChanges, null ), null );

    assertEquals( response.needsChannelChangesProcessed(), true );
    assertEquals( response.getChannelUpdateCount(), 0 );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> connector.processChannelChanges( response ) );

    assertEquals( response.needsChannelChangesProcessed(), true );
    assertEquals( response.getChannelUpdateCount(), 0 );

    assertEquals( exception.getMessage(),
                  "Replicant-0033: Received ChannelChange of type UPDATE for address G.G1:42 but no such subscription exists." );

    handler.assertEventCount( 0 );
  }

  @Test
  public void removeExplicitSubscriptions()
  {
    // Pause converger
    Arez.context().pauseScheduler();

    final TestConnector connector = TestConnector.create( G.class );

    final ChannelAddress address1 = new ChannelAddress( G.G2, 1 );
    final ChannelAddress address2 = new ChannelAddress( G.G2, 2 );
    final ChannelAddress address3 = new ChannelAddress( G.G2, 3 );

    final ArrayList<AreaOfInterestRequest> requests = new ArrayList<>();
    requests.add( new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.REMOVE, null ) );
    requests.add( new AreaOfInterestRequest( address2, AreaOfInterestRequest.Type.REMOVE, null ) );
    requests.add( new AreaOfInterestRequest( address3, AreaOfInterestRequest.Type.REMOVE, null ) );

    final Subscription subscription1 =
      Arez.context().safeAction( () -> Replicant.context().createSubscription( address1, null, true ) );
    // Address2 is already implicit ...
    Arez.context().safeAction( () -> Replicant.context().createSubscription( address2, null, false ) );
    // Address3 has no subscription ... maybe not converged yet

    Arez.context().safeAction( () -> connector.removeExplicitSubscriptions( requests ) );

    Arez.context().safeAction( () -> assertEquals( subscription1.isExplicitSubscription(), false ) );
  }

  @Test
  public void removeExplicitSubscriptions_passedBadAction()
  {
    // Pause converger
    Arez.context().pauseScheduler();

    final TestConnector connector = TestConnector.create( G.class );

    final ChannelAddress address1 = new ChannelAddress( G.G2, 1 );

    final ArrayList<AreaOfInterestRequest> requests = new ArrayList<>();
    requests.add( new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.ADD, null ) );

    Arez.context().safeAction( () -> Replicant.context().createSubscription( address1, null, true ) );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> Arez.context().safeAction( () -> connector.removeExplicitSubscriptions( requests ) ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0034: Connector.removeExplicitSubscriptions() invoked with request with type that is not REMOVE. Request: AreaOfInterestRequest[Type=ADD Address=G.G2:1]" );
  }

  @Test
  public void removeUnneededRemoveRequests_whenInvariantsDisabled()
  {
    final TestConnector connector = TestConnector.create( G.class );

    final ChannelAddress address1 = new ChannelAddress( G.G2, 1 );
    final ChannelAddress address2 = new ChannelAddress( G.G2, 2 );
    final ChannelAddress address3 = new ChannelAddress( G.G2, 3 );

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

    final Subscription subscription1 =
      Arez.context().safeAction( () -> Replicant.context().createSubscription( address1, null, true ) );
    // Address2 is already implicit ...
    Arez.context().safeAction( () -> Replicant.context().createSubscription( address2, null, false ) );
    // Address3 has no subscription ... maybe not converged yet

    ReplicantTestUtil.noCheckInvariants();

    Arez.context().safeAction( () -> connector.removeUnneededRemoveRequests( requests ) );

    assertEquals( requests.size(), 1 );
    assertEquals( requests.contains( request1 ), true );
    assertEquals( request1.isInProgress(), true );
    assertEquals( request2.isInProgress(), false );
    assertEquals( request3.isInProgress(), false );
  }

  @Test
  public void removeUnneededRemoveRequests_noSubscription()
  {
    final TestConnector connector = TestConnector.create( G.class );

    final ChannelAddress address1 = new ChannelAddress( G.G2, 1 );

    final ArrayList<AreaOfInterestRequest> requests = new ArrayList<>();
    final AreaOfInterestRequest request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.REMOVE, null );
    requests.add( request1 );

    requests.forEach( AreaOfInterestRequest::markAsInProgress );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> Arez.context().safeAction( () -> connector.removeUnneededRemoveRequests( requests ) ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0046: Request to unsubscribe from channel at address G.G2:1 but not subscribed to channel." );
  }

  @Test
  public void removeUnneededRemoveRequests_implicitSubscription()
  {
    final TestConnector connector = TestConnector.create( G.class );

    final ChannelAddress address1 = new ChannelAddress( G.G2, 1 );

    final ArrayList<AreaOfInterestRequest> requests = new ArrayList<>();
    final AreaOfInterestRequest request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.REMOVE, null );
    requests.add( request1 );

    requests.forEach( AreaOfInterestRequest::markAsInProgress );

    Arez.context().safeAction( () -> Replicant.context().createSubscription( address1, null, false ) );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> Arez.context().safeAction( () -> connector.removeUnneededRemoveRequests( requests ) ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0047: Request to unsubscribe from channel at address G.G2:1 but subscription is not an explicit subscription." );
  }

  @Test
  public void removeUnneededUpdateRequests_whenInvariantsDisabled()
  {
    final TestConnector connector = TestConnector.create( G.class );

    final ChannelAddress address1 = new ChannelAddress( G.G2, 1 );
    final ChannelAddress address2 = new ChannelAddress( G.G2, 2 );
    final ChannelAddress address3 = new ChannelAddress( G.G2, 3 );

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

    final Subscription subscription1 =
      Arez.context().safeAction( () -> Replicant.context().createSubscription( address1, null, true ) );
    // Address2 is already implicit ...
    Arez.context().safeAction( () -> Replicant.context().createSubscription( address2, null, false ) );
    // Address3 has no subscription ... maybe not converged yet

    ReplicantTestUtil.noCheckInvariants();

    Arez.context().safeAction( () -> connector.removeUnneededUpdateRequests( requests ) );

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
    final TestConnector connector = TestConnector.create( G.class );

    final ChannelAddress address1 = new ChannelAddress( G.G2, 1 );

    final ArrayList<AreaOfInterestRequest> requests = new ArrayList<>();
    final AreaOfInterestRequest request1 =
      new AreaOfInterestRequest( address1, AreaOfInterestRequest.Type.UPDATE, null );
    requests.add( request1 );

    requests.forEach( AreaOfInterestRequest::markAsInProgress );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> Arez.context().safeAction( () -> connector.removeUnneededUpdateRequests( requests ) ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0048: Request to update channel at address G.G2:1 but not subscribed to channel." );
  }

  enum G
  {
    G1, G2
  }

  enum F
  {
    F2
  }
}
