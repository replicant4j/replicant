package org.realityforge.replicant.client.transport;

import arez.Arez;
import arez.Disposable;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.Replicant;
import replicant.TestSpyEventHandler;
import replicant.spy.DataLoadStatus;
import replicant.spy.DataLoaderConnectFailureEvent;
import replicant.spy.DataLoaderConnectedEvent;
import replicant.spy.DataLoaderDisconnectFailureEvent;
import replicant.spy.DataLoaderDisconnectedEvent;
import replicant.spy.DataLoaderMessageProcessFailureEvent;
import replicant.spy.DataLoaderMessageProcessedEvent;
import static org.testng.Assert.*;

public class DataLoaderService2Test
  extends AbstractReplicantTest
{
  @Test
  public void construct()
    throws Exception
  {
    final ReplicantClientSystem replicantClientSystem = new ReplicantClientSystem();

    assertEquals( replicantClientSystem.getDataLoaders().size(), 0 );

    final TestDataLoadService service = TestDataLoadService.create( replicantClientSystem );

    assertEquals( replicantClientSystem.getDataLoaders().size(), 1 );

    assertEquals( service.getReplicantClientSystem(), replicantClientSystem );
    Arez.context().safeAction( () -> assertEquals( service.getState(), DataLoaderService.State.DISCONNECTED ) );
  }

  @Test
  public void dispose()
    throws Exception
  {
    final ReplicantClientSystem replicantClientSystem = new ReplicantClientSystem();

    assertEquals( replicantClientSystem.getDataLoaders().size(), 0 );

    final TestDataLoadService service = TestDataLoadService.create( replicantClientSystem );

    assertEquals( replicantClientSystem.getDataLoaders().size(), 1 );

    Disposable.dispose( service );

    assertEquals( replicantClientSystem.getDataLoaders().size(), 0 );
  }

  @Test
  public void onDisconnected()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();

    Arez.context().safeAction( () -> service.setState( DataLoaderService.State.CONNECTING ) );

    Arez.context().safeAction( () -> service.getReplicantClientSystem().updateStatus() );
    assertEquals( service.getReplicantClientSystem().getState(), ReplicantClientSystem.State.CONNECTING );

    Arez.context().safeAction( service::onDisconnected );

    Arez.context().safeAction( () -> assertEquals( service.getState(), DataLoaderService.State.DISCONNECTED ) );
    assertEquals( service.getReplicantClientSystem().getState(), ReplicantClientSystem.State.DISCONNECTED );
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
    handler.assertNextEvent( DataLoaderDisconnectedEvent.class,
                             e -> assertEquals( e.getSystemType(), service.getSystemType() ) );
  }

  @Test
  public void onDisconnectFailure()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();

    Arez.context().safeAction( () -> service.setState( DataLoaderService.State.CONNECTING ) );

    Arez.context().safeAction( () -> service.getReplicantClientSystem().updateStatus() );
    assertEquals( service.getReplicantClientSystem().getState(), ReplicantClientSystem.State.CONNECTING );

    final Throwable error = new Throwable();

    Arez.context().safeAction( () -> service.onDisconnectFailure( error ) );

    Arez.context().safeAction( () -> assertEquals( service.getState(), DataLoaderService.State.ERROR ) );
    assertEquals( service.getReplicantClientSystem().getState(), ReplicantClientSystem.State.ERROR );
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
    handler.assertNextEvent( DataLoaderDisconnectFailureEvent.class, e -> {
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

    Arez.context().safeAction( () -> service.getReplicantClientSystem().updateStatus() );
    assertEquals( service.getReplicantClientSystem().getState(), ReplicantClientSystem.State.CONNECTING );

    Arez.context().safeAction( service::onConnected );

    Arez.context().safeAction( () -> assertEquals( service.getState(), DataLoaderService.State.CONNECTED ) );
    assertEquals( service.getReplicantClientSystem().getState(), ReplicantClientSystem.State.CONNECTED );
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
    handler.assertNextEvent( DataLoaderConnectedEvent.class,
                             e -> assertEquals( e.getSystemType(), service.getSystemType() ) );
  }

  @Test
  public void onConnectFailure()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();

    Arez.context().safeAction( () -> service.setState( DataLoaderService.State.CONNECTING ) );

    Arez.context().safeAction( () -> service.getReplicantClientSystem().updateStatus() );
    assertEquals( service.getReplicantClientSystem().getState(), ReplicantClientSystem.State.CONNECTING );

    final Throwable error = new Throwable();

    Arez.context().safeAction( () -> service.onConnectFailure( error ) );

    Arez.context().safeAction( () -> assertEquals( service.getState(), DataLoaderService.State.ERROR ) );
    assertEquals( service.getReplicantClientSystem().getState(), ReplicantClientSystem.State.ERROR );
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
    handler.assertNextEvent( DataLoaderConnectFailureEvent.class, e -> {
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

    handler.assertNextEvent( DataLoaderMessageProcessedEvent.class, e -> {
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
    handler.assertNextEvent( DataLoaderMessageProcessFailureEvent.class, e -> {
      assertEquals( e.getSystemType(), service.getSystemType() );
      assertEquals( e.getError(), error );
    } );
  }
}
