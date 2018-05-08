package org.realityforge.replicant.client.transport;

import arez.Arez;
import arez.Disposable;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.Replicant;
import replicant.TestSpyEventHandler;
import replicant.spy.DataLoaderDisconnectEvent;
import replicant.spy.DataLoaderInvalidDisconnectEvent;
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
  public void onDisconnect()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();

    Arez.context().safeAction( () -> service.setState( DataLoaderService.State.CONNECTING ) );

    Arez.context().safeAction( () -> service.getReplicantClientSystem().updateStatus() );
    assertEquals( service.getReplicantClientSystem().getState(), ReplicantClientSystem.State.CONNECTING );

    Arez.context().safeAction( service::onDisconnect );

    Arez.context().safeAction( () -> assertEquals( service.getState(), DataLoaderService.State.DISCONNECTED ) );
    assertEquals( service.getReplicantClientSystem().getState(), ReplicantClientSystem.State.DISCONNECTED );
  }

  @Test
  public void onDisconnect_generatesSpyMessage()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();

    Arez.context().safeAction( () -> service.setState( DataLoaderService.State.CONNECTING ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    Arez.context().safeAction( service::onDisconnect );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( DataLoaderDisconnectEvent.class,
                             e -> assertEquals( e.getSystemType(), service.getSystemType() ) );
  }

  @Test
  public void onInvalidDisconnect()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();

    Arez.context().safeAction( () -> service.setState( DataLoaderService.State.CONNECTING ) );

    Arez.context().safeAction( () -> service.getReplicantClientSystem().updateStatus() );
    assertEquals( service.getReplicantClientSystem().getState(), ReplicantClientSystem.State.CONNECTING );

    final Throwable error = new Throwable();

    Arez.context().safeAction( () -> service.onInvalidDisconnect( error ) );

    Arez.context().safeAction( () -> assertEquals( service.getState(), DataLoaderService.State.ERROR ) );
    assertEquals( service.getReplicantClientSystem().getState(), ReplicantClientSystem.State.ERROR );
  }

  @Test
  public void onInvalidDisconnect_generatesSpyMessage()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();

    Arez.context().safeAction( () -> service.setState( DataLoaderService.State.CONNECTING ) );

    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );

    final Throwable error = new Throwable();

    Arez.context().safeAction( () -> service.onInvalidDisconnect( error ) );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( DataLoaderInvalidDisconnectEvent.class, e -> {
      assertEquals( e.getSystemType(), service.getSystemType() );
      assertEquals( e.getError(), error );
    } );
  }
}
