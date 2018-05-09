package org.realityforge.replicant.client.transport;

import arez.Arez;
import arez.Disposable;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.AreaOfInterest;
import replicant.ChannelAddress;
import replicant.Replicant;
import replicant.Subscription;
import replicant.TestSpyEventHandler;
import replicant.spy.AreaOfInterestStatusUpdatedEvent;
import replicant.spy.ConnectFailureEvent;
import replicant.spy.ConnectedEvent;
import replicant.spy.DataLoadStatus;
import replicant.spy.DisconnectFailureEvent;
import replicant.spy.DisconnectedEvent;
import replicant.spy.MessageProcessFailureEvent;
import replicant.spy.MessageProcessedEvent;
import replicant.spy.MessageReadFailureEvent;
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

public class DataLoaderService2Test
  extends AbstractReplicantTest
{
  @Test
  public void construct()
    throws Exception
  {
    final ReplicantClientSystem replicantClientSystem = ReplicantClientSystem.create();

    Arez.context().safeAction( () -> assertEquals( replicantClientSystem.getDataLoaders().size(), 0 ) );

    final TestDataLoadService service = TestDataLoadService.create( replicantClientSystem );

    Arez.context().safeAction( () -> assertEquals( replicantClientSystem.getDataLoaders().size(), 1 ) );

    assertEquals( service.getReplicantClientSystem(), replicantClientSystem );
    Arez.context().safeAction( () -> assertEquals( service.getState(), DataLoaderService.State.DISCONNECTED ) );
  }

  @Test
  public void dispose()
    throws Exception
  {
    final ReplicantClientSystem replicantClientSystem = ReplicantClientSystem.create();

    Arez.context().safeAction( () -> assertEquals( replicantClientSystem.getDataLoaders().size(), 0 ) );

    final TestDataLoadService service = TestDataLoadService.create( replicantClientSystem );

    Arez.context().safeAction( () -> assertEquals( replicantClientSystem.getDataLoaders().size(), 1 ) );

    Disposable.dispose( service );

    Arez.context().safeAction( () -> assertEquals( replicantClientSystem.getDataLoaders().size(), 0 ) );
  }

  @Test
  public void onDisconnected()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();

    Arez.context().safeAction( () -> service.setState( DataLoaderService.State.CONNECTING ) );

    Arez.context().safeAction( () -> assertEquals( service.getReplicantClientSystem().getState(),
                                                   ReplicantClientSystem.State.CONNECTING ) );

    Arez.context().safeAction( service::onDisconnected );

    Arez.context().safeAction( () -> assertEquals( service.getState(), DataLoaderService.State.DISCONNECTED ) );
    Arez.context().safeAction( () -> assertEquals( service.getReplicantClientSystem().getState(),
                                                   ReplicantClientSystem.State.DISCONNECTED ) );
  }

  @Test
  public void onDisconnected_generatesSpyMessage()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();

    Arez.context().safeAction( () -> service.setState( DataLoaderService.State.CONNECTING ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    Arez.context().safeAction( service::onDisconnected );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( DisconnectedEvent.class,
                             e -> assertEquals( e.getSystemType(), service.getSystemType() ) );
  }

  @Test
  public void onDisconnectFailure()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();

    Arez.context().safeAction( () -> service.setState( DataLoaderService.State.CONNECTING ) );

    Arez.context().safeAction( () -> assertEquals( service.getReplicantClientSystem().getState(),
                                                   ReplicantClientSystem.State.CONNECTING ) );

    final Throwable error = new Throwable();

    Arez.context().safeAction( () -> service.onDisconnectFailure( error ) );

    Arez.context().safeAction( () -> assertEquals( service.getState(), DataLoaderService.State.ERROR ) );
    Arez.context().safeAction( () -> assertEquals( service.getReplicantClientSystem().getState(),
                                                   ReplicantClientSystem.State.ERROR ) );
  }

  @Test
  public void onDisconnectFailure_generatesSpyMessage()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();

    Arez.context().safeAction( () -> service.setState( DataLoaderService.State.CONNECTING ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    final Throwable error = new Throwable();

    Arez.context().safeAction( () -> service.onDisconnectFailure( error ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( DisconnectFailureEvent.class, e -> {
      assertEquals( e.getSystemType(), service.getSystemType() );
      assertEquals( e.getError(), error );
    } );
  }

  @Test
  public void onConnected()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();

    Arez.context().safeAction( () -> service.setState( DataLoaderService.State.CONNECTING ) );

    Arez.context().safeAction( () -> assertEquals( service.getReplicantClientSystem().getState(),
                                                   ReplicantClientSystem.State.CONNECTING ) );

    Arez.context().safeAction( service::onConnected );

    Arez.context().safeAction( () -> assertEquals( service.getState(), DataLoaderService.State.CONNECTED ) );
    Arez.context().safeAction( () -> assertEquals( service.getReplicantClientSystem().getState(),
                                                   ReplicantClientSystem.State.CONNECTED ) );
  }

  @Test
  public void onConnected_generatesSpyMessage()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();

    Arez.context().safeAction( () -> service.setState( DataLoaderService.State.CONNECTING ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    Arez.context().safeAction( service::onConnected );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( ConnectedEvent.class,
                             e -> assertEquals( e.getSystemType(), service.getSystemType() ) );
  }

  @Test
  public void onConnectFailure()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();

    Arez.context().safeAction( () -> service.setState( DataLoaderService.State.CONNECTING ) );

    Arez.context().safeAction( () -> assertEquals( service.getReplicantClientSystem().getState(),
                                                   ReplicantClientSystem.State.CONNECTING ) );

    final Throwable error = new Throwable();

    Arez.context().safeAction( () -> service.onConnectFailure( error ) );

    Arez.context().safeAction( () -> assertEquals( service.getState(), DataLoaderService.State.ERROR ) );
    Arez.context().safeAction( () -> assertEquals( service.getReplicantClientSystem().getState(),
                                                   ReplicantClientSystem.State.ERROR ) );
  }

  @Test
  public void onConnectFailure_generatesSpyMessage()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();

    Arez.context().safeAction( () -> service.setState( DataLoaderService.State.CONNECTING ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    final Throwable error = new Throwable();

    Arez.context().safeAction( () -> service.onConnectFailure( error ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( ConnectFailureEvent.class, e -> {
      assertEquals( e.getSystemType(), service.getSystemType() );
      assertEquals( e.getError(), error );
    } );
  }

  @Test
  public void onMessageProcessed_generatesSpyMessage()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();

    Arez.context().safeAction( () -> service.setState( DataLoaderService.State.CONNECTING ) );

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

    Arez.context().safeAction( () -> service.onMessageProcessed( status ) );

    handler.assertEventCount( 1 );

    handler.assertNextEvent( MessageProcessedEvent.class, e -> {
      assertEquals( e.getSystemType(), service.getSystemType() );
      assertEquals( e.getDataLoadStatus(), status );
    } );
  }

  @Test
  public void onMessageProcessFailure()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();
    service.setSession( new ClientSession( service, ValueUtil.randomString() ), null );
    Arez.context().safeAction( () -> service.setState( DataLoaderService.State.CONNECTED ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    final Throwable error = new Throwable();

    Arez.context().safeAction( () -> service.onMessageProcessFailure( error ) );

    Arez.context().safeAction( () -> assertEquals( service.getState(), DataLoaderService.State.DISCONNECTING ) );
  }

  @Test
  public void onMessageProcessFailure_generatesSpyMessage()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();

    Arez.context().safeAction( () -> service.setState( DataLoaderService.State.CONNECTING ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    final Throwable error = new Throwable();

    Arez.context().safeAction( () -> service.onMessageProcessFailure( error ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( MessageProcessFailureEvent.class, e -> {
      assertEquals( e.getSystemType(), service.getSystemType() );
      assertEquals( e.getError(), error );
    } );
  }

  @Test
  public void onMessageReadFailure()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();
    service.setSession( new ClientSession( service, ValueUtil.randomString() ), null );
    Arez.context().safeAction( () -> service.setState( DataLoaderService.State.CONNECTED ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    final Throwable error = new Throwable();

    Arez.context().safeAction( () -> service.onMessageReadFailure( error ) );

    Arez.context().safeAction( () -> assertEquals( service.getState(), DataLoaderService.State.DISCONNECTING ) );
  }

  @Test
  public void onMessageReadFailure_generatesSpyMessage()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();

    Arez.context().safeAction( () -> service.setState( DataLoaderService.State.CONNECTING ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    final Throwable error = new Throwable();

    Arez.context().safeAction( () -> service.onMessageReadFailure( error ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( MessageReadFailureEvent.class, e -> {
      assertEquals( e.getSystemType(), service.getSystemType() );
      assertEquals( e.getError(), error );
    } );
  }

  @Test
  public void onSubscribeStarted()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();

    final ChannelAddress address = new ChannelAddress( G.G1 );
    final AreaOfInterest areaOfInterest =
      Arez.context().safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    Arez.context().safeAction( () -> service.onSubscribeStarted( address ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.LOADING ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( SubscribeStartedEvent.class, e -> {
      assertEquals( e.getSystemType(), service.getSystemType() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @Test
  public void onSubscribeCompleted()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();

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

    Arez.context().safeAction( () -> service.onSubscribeCompleted( address ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.LOADED ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), subscription ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( SubscribeCompletedEvent.class, e -> {
      assertEquals( e.getSystemType(), service.getSystemType() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @Test
  public void onSubscribeFailed()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();

    final ChannelAddress address = new ChannelAddress( G.G1 );
    final AreaOfInterest areaOfInterest =
      Arez.context().safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    final Throwable error = new Throwable();

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    Arez.context().safeAction( () -> service.onSubscribeFailed( address, error ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.LOAD_FAILED ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), error ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( SubscribeFailedEvent.class, e -> {
      assertEquals( e.getSystemType(), service.getSystemType() );
      assertEquals( e.getAddress(), address );
      assertEquals( e.getError(), error );
    } );
  }

  @Test
  public void onUnsubscribeStarted()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();

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

    Arez.context().safeAction( () -> service.onUnsubscribeStarted( address ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.UNLOADING ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), subscription ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( UnsubscribeStartedEvent.class, e -> {
      assertEquals( e.getSystemType(), service.getSystemType() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @Test
  public void onUnsubscribeCompleted()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();

    final ChannelAddress address = new ChannelAddress( G.G1 );
    final AreaOfInterest areaOfInterest =
      Arez.context().safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    Arez.context().safeAction( () -> service.onUnsubscribeCompleted( address ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.UNLOADED ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( UnsubscribeCompletedEvent.class, e -> {
      assertEquals( e.getSystemType(), service.getSystemType() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @Test
  public void onUnsubscribeFailed()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();

    final ChannelAddress address = new ChannelAddress( G.G1 );
    final AreaOfInterest areaOfInterest =
      Arez.context().safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    final Throwable error = new Throwable();

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    Arez.context().safeAction( () -> service.onUnsubscribeFailed( address, error ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.UNLOADED ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), null ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( UnsubscribeFailedEvent.class, e -> {
      assertEquals( e.getSystemType(), service.getSystemType() );
      assertEquals( e.getAddress(), address );
      assertEquals( e.getError(), error );
    } );
  }

  @Test
  public void onSubscriptionUpdateStarted()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();

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

    Arez.context().safeAction( () -> service.onSubscriptionUpdateStarted( address ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.UPDATING ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), subscription ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( SubscriptionUpdateStartedEvent.class, e -> {
      assertEquals( e.getSystemType(), service.getSystemType() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @Test
  public void onSubscriptionUpdateCompleted()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();

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

    Arez.context().safeAction( () -> service.onSubscriptionUpdateCompleted( address ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.UPDATED ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), subscription ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), null ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( SubscriptionUpdateCompletedEvent.class, e -> {
      assertEquals( e.getSystemType(), service.getSystemType() );
      assertEquals( e.getAddress(), address );
    } );
  }

  @Test
  public void onSubscriptionUpdateFailed()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();

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

    Arez.context().safeAction( () -> service.onSubscriptionUpdateFailed( address, error ) );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.UPDATE_FAILED ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getSubscription(), subscription ) );
    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getError(), error ) );

    handler.assertEventCount( 2 );
    handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    handler.assertNextEvent( SubscriptionUpdateFailedEvent.class, e -> {
      assertEquals( e.getSystemType(), service.getSystemType() );
      assertEquals( e.getAddress(), address );
      assertEquals( e.getError(), error );
    } );
  }

  enum G
  {
    G1
  }
}
