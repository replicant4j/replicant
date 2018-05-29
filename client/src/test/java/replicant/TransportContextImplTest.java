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
    final TestConnector connector = TestConnector.create();
    final TransportContextImpl context = new TransportContextImpl( connector );

    assertEquals( context.getSchemaId(), connector.getSchema().getId() );
  }

  @Test
  public void getLastRxSequence()
  {
    final TestConnector connector = TestConnector.create();
    final Connection connection = newConnection( connector );
    final TransportContextImpl context = new TransportContextImpl( connector );

    assertFalse( context.isDisposed() );
    assertEquals( context.getLastRxSequence(), connection.getLastRxSequence() );

    context.dispose();

    assertTrue( context.isDisposed() );

    assertEquals( context.getLastRxSequence(), -1 );
  }

  @Test
  public void onMessageReceived()
  {
    final TestConnector connector = TestConnector.create();
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
    final TestConnector connector = TestConnector.create();
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
    final TestConnector connector = TestConnector.create();
    connector.pauseMessageScheduler();
    pauseScheduler();

    newConnection( connector );
    final TransportContextImpl context = new TransportContextImpl( connector );

    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    safeAction( context::disconnect );

    safeAction( () -> assertEquals( connector.getState(), ConnectorState.DISCONNECTING ) );

    assertFalse( context.isDisposed() );

    context.dispose();

    assertTrue( context.isDisposed() );

    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    // This should be ignored
    safeAction( context::disconnect );

    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );
  }
}
