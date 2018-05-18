package replicant;

import arez.Arez;
import arez.Disposable;
import arez.annotations.ArezComponent;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.transport.ChangeMapper;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ReplicantRuntimeTest
  extends AbstractReplicantTest
{
  private enum TestSystemA
  {
  }

  private enum TestSystemB
  {
  }

  private enum TestSystemC
  {
  }

  @Test
  public void registerAndDeregisterLifecycle()
  {
    final ReplicantRuntime runtime1 = Replicant.context().getRuntime();
    final ReplicantRuntime runtime2 = ReplicantRuntime.create();

    final AtomicInteger callCount1 = new AtomicInteger();
    autorun( () -> {
      if ( !Disposable.isDisposed( runtime1 ) )
      {
        runtime1.getConnectors();
      }
      callCount1.incrementAndGet();
    } );

    final AtomicInteger callCount2 = new AtomicInteger();
    autorun( () -> {
      if ( !Disposable.isDisposed( runtime2 ) )
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
    final Connector connector1 = TestConnector.create( TestSystemA.class );

    safeAction( () -> assertEquals( runtime1.getConnectors().size(), 1 ) );
    safeAction( () -> assertEquals( runtime2.getConnectors().size(), 0 ) );
    assertEquals( callCount1.get(), 2 );
    assertEquals( callCount2.get(), 1 );

    // Manually register to runtime2  - never happens in app but useful during testing
    safeAction( () -> runtime2.registerConnector( connector1 ) );

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

    // This connector will self-register to runtime1
    final Connector connector1 = TestConnector.create( TestSystemA.class );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> runtime1.registerConnector( connector1 ) ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0015: Invoked registerConnector for system type class replicant.ReplicantRuntimeTest$TestSystemA but a Connector for specified system type exists." );
  }

  @Test
  public void deregisterWhenNoRegistered()
  {
    final ReplicantRuntime runtime2 = ReplicantRuntime.create();

    // This connector will self-register to runtime1
    final Connector connector1 = TestConnector.create( TestSystemA.class );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> runtime2.deregisterConnector( connector1 ) ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0006: Invoked deregisterConnector for system type class replicant.ReplicantRuntimeTest$TestSystemA but no Connector for specified system type exists." );
  }

  @Test
  public void getConnector()
  {
    final ReplicantRuntime runtime = Replicant.context().getRuntime();
    final TestDataLoaderService service1 = TestDataLoaderService.create( TestSystemA.class );
    final TestDataLoaderService service2 = TestDataLoaderService.create( TestSystemB.class );

    assertEquals( runtime.getConnector( service1.getSystemType() ), service1 );
    assertEquals( runtime.getConnector( service2.getSystemType() ), service2 );

    assertThrows( IllegalStateException.class, () -> runtime.getConnector( TestSystemC.class ) );
  }

  @Test
  public void activate()
  {
    final ReplicantRuntime runtime = Replicant.context().getRuntime();
    final TestDataLoaderService service1 = TestDataLoaderService.create( TestSystemA.class );

    final ConnectorEntry entry1 = runtime.getConnectorEntryBySystemType( service1.getSystemType() );

    final Disposable schedulerLock1 = pauseScheduler();
    runtime.deactivate();
    safeAction( () -> service1.setState( ConnectorState.DISCONNECTED ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    runtime.activate();
    schedulerLock1.dispose();
    assertEquals( service1.isConnectCalled(), true );

    // set service state to in transition so connect is not called

    final Disposable schedulerLock2 = pauseScheduler();
    runtime.deactivate();
    safeAction( () -> service1.setState( ConnectorState.CONNECTING ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    runtime.activate();
    schedulerLock2.dispose();
    assertEquals( service1.isConnectCalled(), false );

    runtime.deactivate();
    safeAction( () -> service1.setState( ConnectorState.DISCONNECTING ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    runtime.activate();
    assertEquals( service1.isConnectCalled(), false );

    // set service state to connected so no action required

    final Disposable schedulerLock3 = pauseScheduler();
    runtime.deactivate();
    safeAction( () -> service1.setState( ConnectorState.CONNECTED ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    runtime.activate();
    schedulerLock3.dispose();
    assertEquals( service1.isConnectCalled(), false );

    // set service state to disconnected but rate limit it

    final Disposable schedulerLock4 = pauseScheduler();
    runtime.deactivate();
    safeAction( () -> service1.setState( ConnectorState.DISCONNECTED ) );
    entry1.getRateLimiter().setTokenCount( 0 );
    service1.reset();
    runtime.activate();
    schedulerLock4.dispose();
    assertEquals( service1.isConnectCalled(), false );

    // set service state to disconnected but rate limit it

    final Disposable schedulerLock5 = pauseScheduler();
    runtime.deactivate();
    safeAction( () -> service1.setState( ConnectorState.DISCONNECTED ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    runtime.activate();
    schedulerLock5.dispose();
    assertEquals( service1.isConnectCalled(), true );
  }

  @Test
  public void activateMultiple()
  {
    final ReplicantRuntime runtime = Replicant.context().getRuntime();

    final TestDataLoaderService service1 = TestDataLoaderService.create( TestSystemA.class );
    final ConnectorEntry entry1 = runtime.getConnectorEntryBySystemType( service1.getSystemType() );

    final TestDataLoaderService service3 = TestDataLoaderService.create( TestSystemC.class );
    final ConnectorEntry entry3 = runtime.getConnectorEntryBySystemType( service3.getSystemType() );

    final Disposable schedulerLock1 = pauseScheduler();
    safeAction( () -> service1.setState( ConnectorState.DISCONNECTED ) );
    safeAction( () -> service3.setState( ConnectorState.DISCONNECTED ) );
    runtime.deactivate();
    entry1.getRateLimiter().fillBucket();
    entry3.getRateLimiter().fillBucket();
    service1.reset();
    service3.reset();
    runtime.activate();
    schedulerLock1.dispose();
    assertEquals( service1.isConnectCalled(), true );
    assertEquals( service3.isConnectCalled(), true );

    // set service state to in transition so connect is not called

    final Disposable schedulerLock2 = pauseScheduler();
    runtime.deactivate();
    safeAction( () -> service1.setState( ConnectorState.CONNECTING ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    safeAction( () -> service3.setState( ConnectorState.CONNECTING ) );
    entry3.getRateLimiter().fillBucket();
    service3.reset();
    runtime.activate();
    schedulerLock2.dispose();
    assertEquals( service1.isConnectCalled(), false );
    assertEquals( service3.isConnectCalled(), false );

    final Disposable schedulerLock3 = pauseScheduler();
    runtime.deactivate();
    safeAction( () -> service1.setState( ConnectorState.DISCONNECTING ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    safeAction( () -> service3.setState( ConnectorState.DISCONNECTING ) );
    entry3.getRateLimiter().fillBucket();
    service3.reset();
    runtime.activate();
    schedulerLock3.dispose();
    assertEquals( service1.isConnectCalled(), false );
    assertEquals( service3.isConnectCalled(), false );

    // set service state to connected so no action required

    final Disposable schedulerLock4 = pauseScheduler();
    runtime.deactivate();
    safeAction( () -> service1.setState( ConnectorState.CONNECTED ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    safeAction( () -> service3.setState( ConnectorState.CONNECTED ) );
    entry3.getRateLimiter().fillBucket();
    service3.reset();
    runtime.activate();
    schedulerLock4.dispose();
    assertEquals( service1.isConnectCalled(), false );
    assertEquals( service3.isConnectCalled(), false );

    // set service state to disconnected but rate limit it

    final Disposable schedulerLock5 = pauseScheduler();
    runtime.deactivate();
    safeAction( () -> service1.setState( ConnectorState.DISCONNECTED ) );
    entry1.getRateLimiter().setTokenCount( 0 );
    service1.reset();
    safeAction( () -> service3.setState( ConnectorState.DISCONNECTED ) );
    entry3.getRateLimiter().setTokenCount( 0 );
    service3.reset();
    runtime.activate();
    schedulerLock5.dispose();
    assertEquals( service1.isConnectCalled(), false );
    assertEquals( service3.isConnectCalled(), false );

    // set service state to disconnected but rate limit it

    final Disposable schedulerLock6 = pauseScheduler();
    runtime.deactivate();
    safeAction( () -> service1.setState( ConnectorState.DISCONNECTED ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    safeAction( () -> service3.setState( ConnectorState.DISCONNECTED ) );
    entry3.getRateLimiter().fillBucket();
    service3.reset();
    runtime.activate();
    schedulerLock6.dispose();
    assertEquals( service1.isConnectCalled(), true );
    assertEquals( service3.isConnectCalled(), true );
  }

  @Test
  public void deactivate()
  {
    final ReplicantRuntime runtime = Replicant.context().getRuntime();
    final TestDataLoaderService service1 = TestDataLoaderService.create( TestSystemA.class );
    safeAction( () -> service1.setState( ConnectorState.CONNECTED ) );

    final ConnectorEntry entry1 = runtime.getConnectorEntryBySystemType( service1.getSystemType() );

    runtime.activate();
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    runtime.deactivate();
    assertEquals( service1.isDisconnectCalled(), true );

    // set service state to in transition so connect is not called

    runtime.activate();
    safeAction( () -> service1.setState( ConnectorState.CONNECTING ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    runtime.deactivate();
    assertEquals( service1.isDisconnectCalled(), false );

    runtime.activate();
    safeAction( () -> service1.setState( ConnectorState.DISCONNECTING ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    runtime.deactivate();
    assertEquals( service1.isDisconnectCalled(), false );

    // set service state to DISCONNECTED so no action required

    runtime.activate();
    safeAction( () -> service1.setState( ConnectorState.DISCONNECTED ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    runtime.deactivate();
    assertEquals( service1.isDisconnectCalled(), false );

    // set service state to ERROR so no action required

    runtime.activate();
    safeAction( () -> service1.setState( ConnectorState.ERROR ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    runtime.deactivate();
    assertEquals( service1.isDisconnectCalled(), false );

    // set service state to connected but rate limit it

    runtime.activate();
    safeAction( () -> service1.setState( ConnectorState.CONNECTED ) );
    entry1.getRateLimiter().setTokenCount( 0 );
    service1.reset();
    runtime.deactivate();
    assertEquals( service1.isDisconnectCalled(), false );

    // set service state to connected

    runtime.activate();
    safeAction( () -> service1.setState( ConnectorState.CONNECTED ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    runtime.deactivate();
    assertEquals( service1.isDisconnectCalled(), true );
  }

  @Test
  public void deactivateMultipleDataSources()
  {
    final ReplicantRuntime runtime = Replicant.context().getRuntime();
    final TestDataLoaderService service1 = TestDataLoaderService.create( TestSystemA.class );
    safeAction( () -> service1.setState( ConnectorState.CONNECTED ) );

    final TestDataLoaderService service3 = TestDataLoaderService.create( TestSystemC.class );
    safeAction( () -> service3.setState( ConnectorState.CONNECTED ) );

    final ConnectorEntry entry1 = runtime.getConnectorEntryBySystemType( service1.getSystemType() );
    final ConnectorEntry entry3 = runtime.getConnectorEntryBySystemType( service3.getSystemType() );
    entry3.setRequired( false );

    runtime.activate();
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    entry3.getRateLimiter().fillBucket();
    service3.reset();
    runtime.deactivate();
    assertEquals( service1.isDisconnectCalled(), true );
    assertEquals( service3.isDisconnectCalled(), true );
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
                                  @Nonnull final ConnectorState service1State )
  {
    ReplicantTestUtil.resetState();
    final Disposable schedulerLock = pauseScheduler();
    final ReplicantRuntime runtime = Replicant.context().getRuntime();
    final TestDataLoaderService service1 = TestDataLoaderService.create( TestSystemA.class );
    safeAction( () -> service1.setState( service1State ) );

    safeAction( () -> assertEquals( runtime.getState(), expectedSystemState ) );

    schedulerLock.dispose();
  }

  private void assertUpdateState( @Nonnull final RuntimeState expectedSystemState,
                                  @Nonnull final ConnectorState service1State,
                                  @Nonnull final ConnectorState service2State )
  {
    ReplicantTestUtil.resetState();
    final Disposable schedulerLock = pauseScheduler();
    final ReplicantRuntime runtime = Replicant.context().getRuntime();
    final TestDataLoaderService service1 = TestDataLoaderService.create( TestSystemA.class );
    safeAction( () -> service1.setState( service1State ) );
    final TestDataLoaderService service2 = TestDataLoaderService.create( TestSystemB.class );
    safeAction( () -> service2.setState( service2State ) );

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
    final TestDataLoaderService service1 = TestDataLoaderService.create( TestSystemA.class );
    safeAction( () -> service1.setState( service1State ) );

    final TestDataLoaderService service2 = TestDataLoaderService.create( TestSystemB.class );
    safeAction( () -> service2.setState( service2State ) );

    final TestDataLoaderService service3 = TestDataLoaderService.create( TestSystemC.class );
    safeAction( () -> service3.setState( service3State ) );

    runtime.setConnectorRequired( service3.getSystemType(), false );

    safeAction( () -> assertEquals( runtime.getState(), expectedSystemState ) );

    schedulerLock.dispose();
  }

  @ArezComponent
  static abstract class TestDataLoaderService
    extends Connector
  {
    private boolean _connectCalled;
    private boolean _disconnectCalled;

    static TestDataLoaderService create( @Nonnull final Class<?> systemType )
    {
      return create( Replicant.areZonesEnabled() ? Replicant.context() : null, systemType );
    }

    static TestDataLoaderService create( @Nullable final ReplicantContext context, @Nonnull final Class<?> systemType )
    {
      return Arez.context()
        .safeAction( () -> new ReplicantRuntimeTest_Arez_TestDataLoaderService( context, systemType ) );
    }

    TestDataLoaderService( @Nullable final ReplicantContext context, @Nonnull final Class<?> systemType )
    {
      super( context, systemType );
    }

    @Override
    protected void doConnect( @Nonnull final SafeProcedure action )
    {
      _connectCalled = true;
    }

    @Override
    protected void doDisconnect( @Nonnull final SafeProcedure action )
    {
      _disconnectCalled = true;
    }

    void reset()
    {
      _connectCalled = false;
      _disconnectCalled = false;
    }

    boolean isConnectCalled()
    {
      return _connectCalled;
    }

    boolean isDisconnectCalled()
    {
      return _disconnectCalled;
    }

    @Override
    protected void activateScheduler()
    {
    }

    @Override
    protected boolean progressAreaOfInterestRequestProcessing()
    {
      return false;
    }

    @Override
    protected boolean progressResponseProcessing()
    {
      return false;
    }

    @Nonnull
    @Override
    protected SubscriptionUpdateEntityFilter getSubscriptionUpdateFilter()
    {
      return ( address, filter, entity ) -> entity.getId() > 0;
    }

    @Nonnull
    @Override
    protected ChangeMapper getChangeMapper()
    {
      return mock( ChangeMapper.class );
    }
  }
}
