package org.realityforge.replicant.client.transport;

import arez.Arez;
import arez.Disposable;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.Replicant;
import replicant.TestSpyEventHandler;
import replicant.spy.DataLoaderConnectedEvent;
import replicant.spy.DataLoaderDisconnectedEvent;
import replicant.spy.DataLoaderConnectFailureEvent;
import replicant.spy.DataLoaderDisconnectFailureEvent;
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
}
