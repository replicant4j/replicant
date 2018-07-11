package replicant;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.spy.MessageReadFailureEvent;
import replicant.spy.RestartEvent;
import static org.testng.Assert.*;

public class TransportContextImplTest
  extends AbstractReplicantTest
{
  @Test
  public void getSchemaId()
  {
    final Connector connector = createConnector();
    final TransportContextImpl context = new TransportContextImpl( connector );

    assertEquals( context.getSchemaId(), connector.getSchema().getId() );
  }

  @Test
  public void getLastRxSequence()
  {
    final Connector connector = createConnector();
    final Connection connection = newConnection( connector );
    final TransportContextImpl context = new TransportContextImpl( connector );

    assertFalse( context.isDisposed() );
    assertEquals( context.getLastRxSequence(), connection.getLastRxSequence() );

    context.dispose();

    assertTrue( context.isDisposed() );

    assertEquals( context.getLastRxSequence(), -1 );
  }

  @Test
  public void getConnectionId()
  {
    final Connector connector = createConnector();
    final Connection connection = newConnection( connector );
    final TransportContextImpl context = new TransportContextImpl( connector );

    assertFalse( context.isDisposed() );
    assertEquals( context.getConnectionId(), connection.getConnectionId() );

    context.dispose();

    assertTrue( context.isDisposed() );

    assertEquals( context.getConnectionId(), null );
  }

  @Test
  public void onMessageReceived()
  {
    final Connector connector = createConnector();
    connector.pauseMessageScheduler();
    pauseScheduler();

    final Connection connection = newConnection( connector );
    final TransportContextImpl context = new TransportContextImpl( connector );

    assertEquals( connection.getUnparsedResponses().size(), 0 );
    context.onMessageReceived( ValueUtil.randomString() );
    assertEquals( connection.getUnparsedResponses().size(), 1 );

    assertFalse( context.isDisposed() );

    context.dispose();

    assertTrue( context.isDisposed() );

    assertEquals( connection.getUnparsedResponses().size(), 1 );

    // This should be ignored
    context.onMessageReceived( ValueUtil.randomString() );

    assertEquals( connection.getUnparsedResponses().size(), 1 );
  }

  @Test
  public void onMessageReadFailure()
  {
    final Connector connector = createConnector();
    connector.pauseMessageScheduler();
    pauseScheduler();

    newConnection( connector );
    final TransportContextImpl context = new TransportContextImpl( connector );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();
    handler.assertEventCount( 0 );
    context.onMessageReadFailure( new Throwable() );
    handler.assertEventCount( 2 );

    handler.assertEvent( MessageReadFailureEvent.class );
    handler.assertEvent( RestartEvent.class );

    assertFalse( context.isDisposed() );

    context.dispose();

    assertTrue( context.isDisposed() );

    handler.assertEventCount( 2 );

    // This should be ignored
    context.onMessageReadFailure( new Throwable() );

    handler.assertEventCount( 2 );
  }

  @Test
  public void disconnect()
  {
    final Connector connector = createConnector();
    connector.pauseMessageScheduler();
    pauseScheduler();

    newConnection( connector );
    final TransportContextImpl context = new TransportContextImpl( connector );

    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    context.disconnect();

    safeAction( () -> assertEquals( connector.getState(), ConnectorState.DISCONNECTING ) );

    assertFalse( context.isDisposed() );

    context.dispose();

    assertTrue( context.isDisposed() );

    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    // This should be ignored
    context.disconnect();

    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );
  }
}
