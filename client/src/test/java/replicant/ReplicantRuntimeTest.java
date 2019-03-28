package replicant;

import arez.Disposable;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ReplicantRuntimeTest
  extends AbstractReplicantTest
{
  @Test
  public void registerAndDeregisterLifecycle()
  {
    final ReplicantRuntime runtime1 = Replicant.context().getRuntime();
    final ReplicantRuntime runtime2 = ReplicantRuntime.create();

    final AtomicInteger callCount1 = new AtomicInteger();
    observer( () -> {
      if ( Disposable.isNotDisposed( runtime1 ) )
      {
        runtime1.getConnectors();
      }
      callCount1.incrementAndGet();
    } );

    final AtomicInteger callCount2 = new AtomicInteger();
    observer( () -> {
      if ( Disposable.isNotDisposed( runtime2 ) )
      {
        runtime2.getConnectors();
      }
      callCount2.incrementAndGet();
    } );

    safeAction( () -> assertEquals( runtime1.getConnectors().size(), 0 ) );
    safeAction( () -> assertEquals( runtime2.getConnectors().size(), 0 ) );
    assertEquals( callCount1.get(), 1 );
    assertEquals( callCount2.get(), 1 );

    // This connector will self-register to runtime1
    final Connector connector1 = createConnector();

    safeAction( () -> assertEquals( runtime1.getConnectors().size(), 1 ) );
    safeAction( () -> assertEquals( runtime2.getConnectors().size(), 0 ) );
    assertEquals( callCount1.get(), 2 );
    assertEquals( callCount2.get(), 1 );

    // Manually register to runtime2  - never happens in app but useful during testing
    runtime2.registerConnector( connector1 );

    safeAction( () -> assertEquals( runtime1.getConnectors().size(), 1 ) );
    safeAction( () -> assertEquals( runtime2.getConnectors().size(), 1 ) );
    assertEquals( callCount1.get(), 2 );
    assertEquals( callCount2.get(), 2 );

    // Manually deregister from runtime2  - never happens in app but useful during testing
    safeAction( () -> runtime2.deregisterConnector( connector1 ) );

    safeAction( () -> assertEquals( runtime1.getConnectors().size(), 1 ) );
    safeAction( () -> assertEquals( runtime2.getConnectors().size(), 0 ) );
    assertEquals( callCount1.get(), 2 );
    assertEquals( callCount2.get(), 3 );

    Disposable.dispose( connector1 );

    safeAction( () -> assertEquals( runtime1.getConnectors().size(), 0 ) );
    safeAction( () -> assertEquals( runtime2.getConnectors().size(), 0 ) );
    assertEquals( callCount1.get(), 3 );
    assertEquals( callCount2.get(), 3 );
  }

  @Test
  public void duplicateRegister()
  {
    final ReplicantRuntime runtime1 = Replicant.context().getRuntime();

    final SystemSchema schema = newSchema();

    // This connector will self-register to runtime1
    final Connector connector1 = createConnector( schema );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> runtime1.registerConnector( connector1 ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0015: Invoked registerConnector for system schema named '" +
                  schema.getName() + "' but a Connector for specified schema exists." );
  }

  @Test
  public void deregisterWhenNoRegistered()
  {
    final ReplicantRuntime runtime2 = Replicant.context().getRuntime();

    final SystemSchema schema = newSchema();
    // This connector will self-register to runtime1
    final Connector connector1 = createConnector( schema );

    safeAction( () -> runtime2.deregisterConnector( connector1 ) );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> runtime2.deregisterConnector( connector1 ) ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0006: Invoked deregisterConnector for schema named '" +
                  schema.getName() + "' but no Connector for specified schema exists." );
  }

  @Test
  public void getConnector()
  {
    final SystemSchema schema1 = newSchema();
    final SystemSchema schema2 = newSchema();
    final SystemSchema schema3 = newSchema();
    final ReplicantRuntime runtime = Replicant.context().getRuntime();
    final Connector service1 = createConnector( schema1 );
    final Connector service2 = createConnector( schema2 );

    assertEquals( runtime.getConnector( service1.getSchema().getId() ), service1 );
    assertEquals( runtime.getConnector( service2.getSchema().getId() ), service2 );

    assertThrows( IllegalStateException.class, () -> runtime.getConnector( schema3.getId() ) );
  }

  @Test
  public void activate()
  {
    final SystemSchema schema1 = newSchema();

    final ReplicantRuntime runtime = Replicant.context().getRuntime();
    final Connector service1 = createConnector( schema1 );

    final ConnectorEntry entry1 = runtime.getConnectorEntryBySchemaId( service1.getSchema().getId() );

    final Disposable schedulerLock1 = pauseScheduler();
    runtime.deactivate();
    reset( service1.getTransport() );
    safeAction( () -> service1.setState( ConnectorState.DISCONNECTED ) );
    entry1.getRateLimiter().fillBucket();
    runtime.activate();
    schedulerLock1.dispose();
    verify( service1.getTransport(), times( 1 ) )
      .requestConnect( any( Transport.Context.class ) );

    reset( service1.getTransport() );

    // set service state to in transition so connect is not called

    final Disposable schedulerLock2 = pauseScheduler();
    runtime.deactivate();
    safeAction( () -> service1.setState( ConnectorState.CONNECTING ) );
    entry1.getRateLimiter().fillBucket();
    runtime.activate();
    schedulerLock2.dispose();
    verify( service1.getTransport(), never() ).requestConnect( any( Transport.Context.class )
    );

    runtime.deactivate();
    safeAction( () -> service1.setState( ConnectorState.DISCONNECTING ) );
    entry1.getRateLimiter().fillBucket();
    runtime.activate();
    verify( service1.getTransport(), never() )
      .requestConnect( any( Transport.Context.class ) );

    // set service state to connected so no action required

    final Disposable schedulerLock3 = pauseScheduler();
    runtime.deactivate();
    newConnection( service1 );
    safeAction( () -> service1.setState( ConnectorState.CONNECTED ) );
    entry1.getRateLimiter().fillBucket();
    runtime.activate();
    schedulerLock3.dispose();
    verify( service1.getTransport(), never() )
      .requestConnect( any( Transport.Context.class ) );

    // set service state to disconnected but rate limit it

    final Disposable schedulerLock4 = pauseScheduler();
    runtime.deactivate();
    safeAction( () -> service1.setState( ConnectorState.DISCONNECTED ) );
    entry1.getRateLimiter().setTokenCount( 0 );
    runtime.activate();
    schedulerLock4.dispose();
    verify( service1.getTransport(), never() )
      .requestConnect( any( Transport.Context.class ) );

    // set service state to disconnected but rate limit it

    final Disposable schedulerLock5 = pauseScheduler();
    runtime.deactivate();
    safeAction( () -> service1.setState( ConnectorState.DISCONNECTED ) );
    entry1.getRateLimiter().fillBucket();
    runtime.activate();
    schedulerLock5.dispose();
    verify( service1.getTransport(), times( 1 ) )
      .requestConnect( any( Transport.Context.class ) );
  }

  @Test
  public void activateMultiple()
  {
    final SystemSchema schema1 = newSchema();
    final SystemSchema schema3 = newSchema();

    final ReplicantRuntime runtime = Replicant.context().getRuntime();

    final Connector service1 = createConnector( schema1 );
    final ConnectorEntry entry1 = runtime.getConnectorEntryBySchemaId( service1.getSchema().getId() );

    final Connector service3 = createConnector( schema3 );
    final ConnectorEntry entry3 = runtime.getConnectorEntryBySchemaId( service3.getSchema().getId() );

    reset( service1.getTransport() );
    reset( service3.getTransport() );

    final Disposable schedulerLock1 = pauseScheduler();
    safeAction( () -> service1.setState( ConnectorState.DISCONNECTED ) );
    safeAction( () -> service3.setState( ConnectorState.DISCONNECTED ) );
    runtime.deactivate();
    entry1.getRateLimiter().fillBucket();
    entry3.getRateLimiter().fillBucket();
    runtime.activate();
    schedulerLock1.dispose();
    verify( service1.getTransport(), times( 1 ) )
      .requestConnect( any( Transport.Context.class ) );
    verify( service3.getTransport(), times( 1 ) )
      .requestConnect( any( Transport.Context.class ) );

    reset( service1.getTransport() );
    reset( service3.getTransport() );

    // set service state to in transition so connect is not called

    final Disposable schedulerLock2 = pauseScheduler();
    runtime.deactivate();
    safeAction( () -> service1.setState( ConnectorState.CONNECTING ) );
    entry1.getRateLimiter().fillBucket();
    safeAction( () -> service3.setState( ConnectorState.CONNECTING ) );
    entry3.getRateLimiter().fillBucket();
    runtime.activate();
    schedulerLock2.dispose();
    verify( service1.getTransport(), never() )
      .requestConnect( any( Transport.Context.class ) );
    verify( service3.getTransport(), never() )
      .requestConnect( any( Transport.Context.class ) );

    final Disposable schedulerLock3 = pauseScheduler();
    runtime.deactivate();
    safeAction( () -> service1.setState( ConnectorState.DISCONNECTING ) );
    entry1.getRateLimiter().fillBucket();
    safeAction( () -> service3.setState( ConnectorState.DISCONNECTING ) );
    entry3.getRateLimiter().fillBucket();
    runtime.activate();
    schedulerLock3.dispose();
    verify( service1.getTransport(), never() )
      .requestConnect( any( Transport.Context.class ) );
    verify( service3.getTransport(), never() )
      .requestConnect( any( Transport.Context.class ) );

    // set service state to connected so no action required

    final Disposable schedulerLock4 = pauseScheduler();
    runtime.deactivate();
    newConnection( service1 );
    safeAction( () -> service1.setState( ConnectorState.CONNECTED ) );
    entry1.getRateLimiter().fillBucket();
    newConnection( service3 );
    safeAction( () -> service3.setState( ConnectorState.CONNECTED ) );
    entry3.getRateLimiter().fillBucket();
    runtime.activate();
    schedulerLock4.dispose();
    verify( service1.getTransport(), never() )
      .requestConnect( any( Transport.Context.class ) );
    verify( service3.getTransport(), never() )
      .requestConnect( any( Transport.Context.class ) );

    // set service state to disconnected but rate limit it

    final Disposable schedulerLock5 = pauseScheduler();
    runtime.deactivate();
    safeAction( () -> service1.setState( ConnectorState.DISCONNECTED ) );
    entry1.getRateLimiter().setTokenCount( 0 );
    safeAction( () -> service3.setState( ConnectorState.DISCONNECTED ) );
    entry3.getRateLimiter().setTokenCount( 0 );
    runtime.activate();
    schedulerLock5.dispose();
    verify( service1.getTransport(), never() )
      .requestConnect( any( Transport.Context.class ) );
    verify( service3.getTransport(), never() )
      .requestConnect( any( Transport.Context.class ) );

    // set service state to disconnected but rate limit it

    final Disposable schedulerLock6 = pauseScheduler();
    runtime.deactivate();
    safeAction( () -> service1.setState( ConnectorState.DISCONNECTED ) );
    entry1.getRateLimiter().fillBucket();
    safeAction( () -> service3.setState( ConnectorState.DISCONNECTED ) );
    entry3.getRateLimiter().fillBucket();
    runtime.activate();
    schedulerLock6.dispose();
    verify( service1.getTransport(), times( 1 ) )
      .requestConnect( any( Transport.Context.class ) );
    verify( service3.getTransport(), times( 1 ) )
      .requestConnect( any( Transport.Context.class ) );
  }

  @Test
  public void deactivate()
  {
    final SystemSchema schema1 = newSchema();

    final ReplicantRuntime runtime = Replicant.context().getRuntime();
    final Connector service1 = createConnector( schema1 );
    newConnection( service1 );
    safeAction( () -> service1.setState( ConnectorState.CONNECTED ) );

    final ConnectorEntry entry1 = runtime.getConnectorEntryBySchemaId( service1.getSchema().getId() );

    runtime.activate();

    reset( service1.getTransport() );

    entry1.getRateLimiter().fillBucket();
    runtime.deactivate();
    verify( service1.getTransport(), times( 1 ) ).requestDisconnect();

    reset( service1.getTransport() );

    // set service state to in transition so connect is not called

    runtime.activate();
    safeAction( () -> service1.setState( ConnectorState.CONNECTING ) );
    entry1.getRateLimiter().fillBucket();
    runtime.deactivate();
    verify( service1.getTransport(), never() ).requestDisconnect();

    runtime.activate();
    safeAction( () -> service1.setState( ConnectorState.DISCONNECTING ) );
    entry1.getRateLimiter().fillBucket();
    runtime.deactivate();
    verify( service1.getTransport(), never() ).requestDisconnect();

    // set service state to DISCONNECTED so no action required

    runtime.activate();
    safeAction( () -> service1.setState( ConnectorState.DISCONNECTED ) );
    entry1.getRateLimiter().fillBucket();
    runtime.deactivate();
    verify( service1.getTransport(), never() ).requestDisconnect();

    // set service state to ERROR so no action required

    runtime.activate();
    safeAction( () -> service1.setState( ConnectorState.ERROR ) );
    entry1.getRateLimiter().fillBucket();
    runtime.deactivate();
    verify( service1.getTransport(), never() ).requestDisconnect();

    // set service state to connected but rate limit it

    runtime.activate();
    safeAction( () -> service1.setState( ConnectorState.CONNECTED ) );
    entry1.getRateLimiter().setTokenCount( 0 );
    runtime.deactivate();
    verify( service1.getTransport(), never() ).requestDisconnect();

    // set service state to connected

    runtime.activate();
    safeAction( () -> service1.setState( ConnectorState.CONNECTED ) );
    entry1.getRateLimiter().fillBucket();
    runtime.deactivate();
    verify( service1.getTransport(), times( 1 ) ).requestDisconnect();
  }

  @Test
  public void deactivateMultipleDataSources()
  {
    final SystemSchema schema1 = newSchema();
    final SystemSchema schema2 = newSchema();

    final ReplicantRuntime runtime = Replicant.context().getRuntime();
    final Connector service1 = createConnector( schema1 );
    newConnection( service1 );
    safeAction( () -> service1.setState( ConnectorState.CONNECTED ) );

    final Connector service3 = createConnector( schema2 );
    newConnection( service3 );
    safeAction( () -> service3.setState( ConnectorState.CONNECTED ) );

    final ConnectorEntry entry1 = runtime.getConnectorEntryBySchemaId( service1.getSchema().getId() );
    final ConnectorEntry entry3 = runtime.getConnectorEntryBySchemaId( service3.getSchema().getId() );
    entry3.setRequired( false );

    runtime.activate();

    reset( service1.getTransport() );
    reset( service3.getTransport() );

    entry1.getRateLimiter().fillBucket();
    entry3.getRateLimiter().fillBucket();
    runtime.deactivate();
    verify( service1.getTransport(), times( 1 ) ).requestDisconnect();
    verify( service3.getTransport(), times( 1 ) ).requestDisconnect();
  }

  @Test
  public void updateStatus()
  {
    // No connectors just active/inactive state

    assertUpdateState( RuntimeState.CONNECTED, true );
    assertUpdateState( RuntimeState.DISCONNECTED, false );

    // Single data source, required

    assertUpdateState( RuntimeState.DISCONNECTED, ConnectorState.DISCONNECTED );
    assertUpdateState( RuntimeState.CONNECTED, ConnectorState.CONNECTED );
    assertUpdateState( RuntimeState.CONNECTING, ConnectorState.CONNECTING );
    assertUpdateState( RuntimeState.DISCONNECTING, ConnectorState.DISCONNECTING );
    assertUpdateState( RuntimeState.ERROR, ConnectorState.ERROR );

    // 2 Data sources, both required

    assertUpdateState( RuntimeState.ERROR,
                       ConnectorState.CONNECTED,
                       ConnectorState.ERROR );
    assertUpdateState( RuntimeState.ERROR,
                       ConnectorState.CONNECTING,
                       ConnectorState.ERROR );
    assertUpdateState( RuntimeState.ERROR,
                       ConnectorState.DISCONNECTED,
                       ConnectorState.ERROR );
    assertUpdateState( RuntimeState.ERROR,
                       ConnectorState.DISCONNECTING,
                       ConnectorState.ERROR );
    assertUpdateState( RuntimeState.ERROR,
                       ConnectorState.ERROR,
                       ConnectorState.ERROR );

    assertUpdateState( RuntimeState.CONNECTED,
                       ConnectorState.CONNECTED,
                       ConnectorState.CONNECTED );
    assertUpdateState( RuntimeState.CONNECTING,
                       ConnectorState.CONNECTING,
                       ConnectorState.CONNECTED );
    assertUpdateState( RuntimeState.DISCONNECTED,
                       ConnectorState.DISCONNECTED,
                       ConnectorState.CONNECTED );
    assertUpdateState( RuntimeState.DISCONNECTING,
                       ConnectorState.DISCONNECTING,
                       ConnectorState.CONNECTED );

    assertUpdateState( RuntimeState.CONNECTING,
                       ConnectorState.CONNECTING,
                       ConnectorState.CONNECTING );
    assertUpdateState( RuntimeState.DISCONNECTED,
                       ConnectorState.DISCONNECTED,
                       ConnectorState.CONNECTING );
    assertUpdateState( RuntimeState.DISCONNECTING,
                       ConnectorState.DISCONNECTING,
                       ConnectorState.CONNECTING );

    assertUpdateState( RuntimeState.DISCONNECTED,
                       ConnectorState.DISCONNECTED,
                       ConnectorState.DISCONNECTING );
    assertUpdateState( RuntimeState.DISCONNECTING,
                       ConnectorState.DISCONNECTING,
                       ConnectorState.DISCONNECTING );

    assertUpdateState( RuntimeState.DISCONNECTED,
                       ConnectorState.DISCONNECTED,
                       ConnectorState.DISCONNECTED );

    // 3 Data sources, first two required, third optional

    assertUpdateState( RuntimeState.CONNECTED,
                       ConnectorState.CONNECTED,
                       ConnectorState.CONNECTED,
                       ConnectorState.CONNECTED );
    assertUpdateState( RuntimeState.CONNECTED,
                       ConnectorState.CONNECTED,
                       ConnectorState.CONNECTED,
                       ConnectorState.ERROR );
    assertUpdateState( RuntimeState.CONNECTED,
                       ConnectorState.CONNECTED,
                       ConnectorState.CONNECTED,
                       ConnectorState.CONNECTING );
    assertUpdateState( RuntimeState.CONNECTED,
                       ConnectorState.CONNECTED,
                       ConnectorState.CONNECTED,
                       ConnectorState.DISCONNECTING );
    assertUpdateState( RuntimeState.CONNECTED,
                       ConnectorState.CONNECTED,
                       ConnectorState.CONNECTED,
                       ConnectorState.DISCONNECTED );

    assertUpdateState( RuntimeState.CONNECTING,
                       ConnectorState.CONNECTING,
                       ConnectorState.CONNECTED,
                       ConnectorState.ERROR );
    assertUpdateState( RuntimeState.CONNECTING,
                       ConnectorState.CONNECTING,
                       ConnectorState.CONNECTED,
                       ConnectorState.CONNECTED );
    assertUpdateState( RuntimeState.CONNECTING,
                       ConnectorState.CONNECTING,
                       ConnectorState.CONNECTED,
                       ConnectorState.CONNECTING );
    assertUpdateState( RuntimeState.CONNECTING,
                       ConnectorState.CONNECTING,
                       ConnectorState.CONNECTED,
                       ConnectorState.DISCONNECTED );
    assertUpdateState( RuntimeState.CONNECTING,
                       ConnectorState.CONNECTING,
                       ConnectorState.CONNECTED,
                       ConnectorState.DISCONNECTING );
  }

  private void assertUpdateState( @Nonnull final RuntimeState expectedSystemState,
                                  final boolean isActive )
  {
    ReplicantTestUtil.resetState();
    final Disposable schedulerLock = pauseScheduler();
    final ReplicantRuntime runtime = Replicant.context().getRuntime();
    if ( isActive )
    {
      runtime.activate();
    }
    else
    {
      runtime.deactivate();
    }
    safeAction( () -> assertEquals( runtime.getState(), expectedSystemState ) );

    schedulerLock.dispose();
  }

  private void assertUpdateState( @Nonnull final RuntimeState expectedSystemState,
                                  @Nonnull final ConnectorState state )
  {
    ReplicantTestUtil.resetState();

    final Disposable schedulerLock = pauseScheduler();
    final ReplicantRuntime runtime = Replicant.context().getRuntime();
    createConnectorInState( state );

    safeAction( () -> assertEquals( runtime.getState(), expectedSystemState ) );

    schedulerLock.dispose();
  }

  @Nonnull
  private Connector createConnectorInState( @Nonnull final ConnectorState state )
  {
    final Connector connector = createConnector( newSchema() );
    if ( ConnectorState.CONNECTED == state )
    {
      newConnection( connector );
    }
    safeAction( () -> connector.setState( state ) );
    return connector;
  }

  private void assertUpdateState( @Nonnull final RuntimeState expectedSystemState,
                                  @Nonnull final ConnectorState service1State,
                                  @Nonnull final ConnectorState service2State )
  {
    ReplicantTestUtil.resetState();

    final Disposable schedulerLock = pauseScheduler();
    final ReplicantRuntime runtime = Replicant.context().getRuntime();
    createConnectorInState( service1State );
    createConnectorInState( service2State );

    safeAction( () -> assertEquals( runtime.getState(), expectedSystemState ) );

    schedulerLock.dispose();
  }

  private void assertUpdateState( @Nonnull final RuntimeState expectedSystemState,
                                  @Nonnull final ConnectorState service1State,
                                  @Nonnull final ConnectorState service2State,
                                  @Nonnull final ConnectorState service3State )
  {
    ReplicantTestUtil.resetState();

    final Disposable schedulerLock = pauseScheduler();

    final ReplicantRuntime runtime = Replicant.context().getRuntime();
    createConnectorInState( service1State );
    createConnectorInState( service2State );

    final Connector connector3 = createConnectorInState( service3State );

    runtime.setConnectorRequired( connector3.getSchema().getId(), false );

    safeAction( () -> assertEquals( runtime.getState(), expectedSystemState ) );

    schedulerLock.dispose();
  }
}
