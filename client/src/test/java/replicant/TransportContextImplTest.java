package replicant;

import org.testng.annotations.Test;
import replicant.messages.OkMessage;
import replicant.spy.DisconnectedEvent;
import replicant.spy.MessageReadFailureEvent;
import replicant.spy.RestartEvent;
import static org.testng.Assert.*;

public class TransportContextImplTest
  extends AbstractReplicantTest
{
  @Test
  public void onMessageReceived()
  {
    final var connector = createConnector();
    connector.pauseMessageScheduler();
    pauseScheduler();

    final var connection = newConnection( connector );
    final var context = new TransportContextImpl( connector );

    final var request = connection.newRequest( ValueUtil.randomString(), false, null );
    assertEquals( connection.getPendingResponses().size(), 0 );
    context.onMessageReceived( OkMessage.create( request.getRequestId() ) );
    assertEquals( connection.getPendingResponses().size(), 1 );

    assertFalse( context.isDisposed() );

    context.dispose();

    assertTrue( context.isDisposed() );

    assertEquals( connection.getPendingResponses().size(), 1 );

    // This should be ignored
    context.onMessageReceived( OkMessage.create( 1 ) );

    assertEquals( connection.getPendingResponses().size(), 1 );
  }

  @Test
  public void onMessageReadFailure()
  {
    final var connector = createConnector();
    connector.pauseMessageScheduler();
    pauseScheduler();

    newConnection( connector );
    final var context = new TransportContextImpl( connector );

    final var handler = registerTestSpyEventHandler();
    handler.assertEventCount( 0 );
    context.onError();
    handler.assertEventCount( 2 );

    handler.assertEvent( MessageReadFailureEvent.class );
    handler.assertEvent( RestartEvent.class );

    assertFalse( context.isDisposed() );

    context.dispose();

    assertTrue( context.isDisposed() );

    handler.assertEventCount( 2 );

    // This should be ignored
    context.onError();

    handler.assertEventCount( 2 );
  }

  @Test
  public void onDisconnect()
  {
    final var connector = createConnector();
    connector.pauseMessageScheduler();
    pauseScheduler();

    newConnection( connector );
    final var context = new TransportContextImpl( connector );

    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    final var handler = registerTestSpyEventHandler();

    context.onDisconnect();

    assertEquals( connector.getState(), ConnectorState.DISCONNECTED );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( DisconnectedEvent.class );
  }

  @Test
  public void onDisconnect_onDisposed()
  {
    final var connector = createConnector();
    connector.pauseMessageScheduler();
    pauseScheduler();

    newConnection( connector );
    final var context = new TransportContextImpl( connector );

    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    final var handler = registerTestSpyEventHandler();

    context.dispose();

    context.onDisconnect();

    assertEquals( connector.getState(), ConnectorState.CONNECTED );

    handler.assertEventCount( 0 );
  }
}
