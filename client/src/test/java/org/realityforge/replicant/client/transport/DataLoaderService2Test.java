package org.realityforge.replicant.client.transport;

import arez.Arez;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.Replicant;
import replicant.TestSpyEventHandler;
import replicant.spy.DataLoaderDisconnectEvent;
import static org.testng.Assert.*;

public class DataLoaderService2Test
  extends AbstractReplicantTest
{
  @Test
  public void onDisconnect()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();

    Arez.context().safeAction( () -> service.setState( DataLoaderService.State.CONNECTING ) );

    service.getReplicantClientSystem().updateStatus();
    assertEquals( service.getReplicantClientSystem().getState(), ReplicantClientSystem.State.CONNECTING );

    service.onDisconnect();

    assertEquals( service.getState(), DataLoaderService.State.DISCONNECTED );
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

    service.onDisconnect();

    handler.assertEventCount( 1 );
    handler.assertNextEvent( DataLoaderDisconnectEvent.class,
                             e -> assertEquals( e.getSystemType(), service.getSystemType() ) );
  }
}
