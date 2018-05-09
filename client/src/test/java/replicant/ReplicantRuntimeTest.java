package replicant;

import arez.Arez;
import arez.Disposable;
import arez.annotations.ArezComponent;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.transport.AreaOfInterestAction;
import org.realityforge.replicant.client.transport.DataLoaderService;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ReplicantRuntimeTest
  extends AbstractReplicantTest
{
  private enum TestSystemA
  {
    A
  }

  private enum TestSystemB
  {
    B
  }

  private enum TestSystemC
  {
  }

  @Test
  public void registerAndDeregisterLifecycle()
  {
    final ReplicantRuntime runtime1 = ReplicantRuntime.create();
    final ReplicantRuntime runtime2 = ReplicantRuntime.create();

    final AtomicInteger callCount1 = new AtomicInteger();
    Arez.context().autorun( () -> {
      if ( !Disposable.isDisposed( runtime1 ) )
      {
        runtime1.getConnectors();
      }
      callCount1.incrementAndGet();
    } );

    final AtomicInteger callCount2 = new AtomicInteger();
    Arez.context().autorun( () -> {
      if ( !Disposable.isDisposed( runtime2 ) )
      {
        runtime2.getConnectors();
      }
      callCount2.incrementAndGet();
    } );

    Arez.context().safeAction( () -> assertEquals( runtime1.getConnectors().size(), 0 ) );
    Arez.context().safeAction( () -> assertEquals( runtime2.getConnectors().size(), 0 ) );
    assertEquals( callCount1.get(), 1 );
    assertEquals( callCount2.get(), 1 );

    // This connector will self-register to runtime1
    final Connector connector1 = TestConnector.create( TestSystemA.class, runtime1 );

    Arez.context().safeAction( () -> assertEquals( runtime1.getConnectors().size(), 1 ) );
    Arez.context().safeAction( () -> assertEquals( runtime2.getConnectors().size(), 0 ) );
    assertEquals( callCount1.get(), 2 );
    assertEquals( callCount2.get(), 1 );

    // Manually register to runtime2  - never happens in app but useful during testing
    Arez.context().safeAction( () -> runtime2.registerConnector( connector1 ) );

    Arez.context().safeAction( () -> assertEquals( runtime1.getConnectors().size(), 1 ) );
    Arez.context().safeAction( () -> assertEquals( runtime2.getConnectors().size(), 1 ) );
    assertEquals( callCount1.get(), 2 );
    assertEquals( callCount2.get(), 2 );

    // Manually deregister from runtime2  - never happens in app but useful during testing
    Arez.context().safeAction( () -> runtime2.deregisterConnector( connector1 ) );

    Arez.context().safeAction( () -> assertEquals( runtime1.getConnectors().size(), 1 ) );
    Arez.context().safeAction( () -> assertEquals( runtime2.getConnectors().size(), 0 ) );
    assertEquals( callCount1.get(), 2 );
    assertEquals( callCount2.get(), 3 );

    Disposable.dispose( connector1 );

    Arez.context().safeAction( () -> assertEquals( runtime1.getConnectors().size(), 0 ) );
    Arez.context().safeAction( () -> assertEquals( runtime2.getConnectors().size(), 0 ) );
    assertEquals( callCount1.get(), 3 );
    assertEquals( callCount2.get(), 3 );
  }

  @Test
  public void duplicateRegister()
  {
    final ReplicantRuntime runtime1 = ReplicantRuntime.create();

    // This connector will self-register to runtime1
    final Connector connector1 = TestConnector.create( TestSystemA.class, runtime1 );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> Arez.context().safeAction( () -> runtime1.registerConnector( connector1 ) ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0015: Invoked registerConnector for system type class replicant.ReplicantRuntimeTest$TestSystemA but a Connector for specified system type exists." );
  }

  @Test
  public void deregisterWhenNoRegistered()
  {
    final ReplicantRuntime runtime1 = ReplicantRuntime.create();
    final ReplicantRuntime runtime2 = ReplicantRuntime.create();

    // This connector will self-register to runtime1
    final Connector connector1 = TestConnector.create( TestSystemA.class, runtime1 );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> Arez.context().safeAction( () -> runtime2.deregisterConnector( connector1 ) ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0006: Invoked deregisterConnector for system type class replicant.ReplicantRuntimeTest$TestSystemA but no Connector for specified system type exists." );
  }

  @Test
  public void getConnector()
  {
    final ReplicantRuntime runtime = ReplicantRuntime.create();
    final TestDataLoaderService service1 = TestDataLoaderService.create( TestSystemA.class, runtime );
    final TestDataLoaderService service2 = TestDataLoaderService.create( TestSystemB.class, runtime );

    assertEquals( runtime.getConnector( service1.getSystemType() ), service1 );
    assertEquals( runtime.getConnector( service2.getSystemType() ), service2 );

    assertThrows( IllegalStateException.class, () -> runtime.getConnector( TestSystemC.class ) );
  }

  @Test
  public void activate()
  {
    final ReplicantRuntime runtime = ReplicantRuntime.create();
    final TestDataLoaderService service1 = TestDataLoaderService.create( TestSystemA.class, runtime );

    final ConnectorEntry entry1 = runtime.getConnectorEntryBySystemType( service1.getSystemType() );

    final Disposable schedulerLock1 = Arez.context().pauseScheduler();
    runtime.deactivate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.DISCONNECTED ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    runtime.activate();
    schedulerLock1.dispose();
    assertEquals( service1.isConnectCalled(), true );

    // set service state to in transition so connect is not called

    final Disposable schedulerLock2 = Arez.context().pauseScheduler();
    runtime.deactivate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.CONNECTING ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    runtime.activate();
    schedulerLock2.dispose();
    assertEquals( service1.isConnectCalled(), false );

    runtime.deactivate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.DISCONNECTING ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    runtime.activate();
    assertEquals( service1.isConnectCalled(), false );

    // set service state to connected so no action required

    final Disposable schedulerLock3 = Arez.context().pauseScheduler();
    runtime.deactivate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.CONNECTED ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    runtime.activate();
    schedulerLock3.dispose();
    assertEquals( service1.isConnectCalled(), false );

    // set service state to disconnected but rate limit it

    final Disposable schedulerLock4 = Arez.context().pauseScheduler();
    runtime.deactivate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.DISCONNECTED ) );
    entry1.getRateLimiter().setTokenCount( 0 );
    service1.reset();
    runtime.activate();
    schedulerLock4.dispose();
    assertEquals( service1.isConnectCalled(), false );

    // set service state to disconnected but rate limit it

    final Disposable schedulerLock5 = Arez.context().pauseScheduler();
    runtime.deactivate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.DISCONNECTED ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    runtime.activate();
    schedulerLock5.dispose();
    assertEquals( service1.isConnectCalled(), true );
  }

  @Test
  public void activateMultiple()
  {
    final ReplicantRuntime runtime = ReplicantRuntime.create();

    final TestDataLoaderService service1 = TestDataLoaderService.create( TestSystemA.class, runtime );
    final ConnectorEntry entry1 = runtime.getConnectorEntryBySystemType( service1.getSystemType() );

    final TestDataLoaderService service3 = TestDataLoaderService.create( TestSystemC.class, runtime );
    final ConnectorEntry entry3 = runtime.getConnectorEntryBySystemType( service3.getSystemType() );

    final Disposable schedulerLock1 = Arez.context().pauseScheduler();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.DISCONNECTED ) );
    Arez.context().safeAction( () -> service3.setState( DataLoaderService.State.DISCONNECTED ) );
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

    final Disposable schedulerLock2 = Arez.context().pauseScheduler();
    runtime.deactivate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.CONNECTING ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    Arez.context().safeAction( () -> service3.setState( DataLoaderService.State.CONNECTING ) );
    entry3.getRateLimiter().fillBucket();
    service3.reset();
    runtime.activate();
    schedulerLock2.dispose();
    assertEquals( service1.isConnectCalled(), false );
    assertEquals( service3.isConnectCalled(), false );

    final Disposable schedulerLock3 = Arez.context().pauseScheduler();
    runtime.deactivate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.DISCONNECTING ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    Arez.context().safeAction( () -> service3.setState( DataLoaderService.State.DISCONNECTING ) );
    entry3.getRateLimiter().fillBucket();
    service3.reset();
    runtime.activate();
    schedulerLock3.dispose();
    assertEquals( service1.isConnectCalled(), false );
    assertEquals( service3.isConnectCalled(), false );

    // set service state to connected so no action required

    final Disposable schedulerLock4 = Arez.context().pauseScheduler();
    runtime.deactivate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.CONNECTED ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    Arez.context().safeAction( () -> service3.setState( DataLoaderService.State.CONNECTED ) );
    entry3.getRateLimiter().fillBucket();
    service3.reset();
    runtime.activate();
    schedulerLock4.dispose();
    assertEquals( service1.isConnectCalled(), false );
    assertEquals( service3.isConnectCalled(), false );

    // set service state to disconnected but rate limit it

    final Disposable schedulerLock5 = Arez.context().pauseScheduler();
    runtime.deactivate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.DISCONNECTED ) );
    entry1.getRateLimiter().setTokenCount( 0 );
    service1.reset();
    Arez.context().safeAction( () -> service3.setState( DataLoaderService.State.DISCONNECTED ) );
    entry3.getRateLimiter().setTokenCount( 0 );
    service3.reset();
    runtime.activate();
    schedulerLock5.dispose();
    assertEquals( service1.isConnectCalled(), false );
    assertEquals( service3.isConnectCalled(), false );

    // set service state to disconnected but rate limit it

    final Disposable schedulerLock6 = Arez.context().pauseScheduler();
    runtime.deactivate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.DISCONNECTED ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    Arez.context().safeAction( () -> service3.setState( DataLoaderService.State.DISCONNECTED ) );
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
    final ReplicantRuntime runtime = ReplicantRuntime.create();
    final TestDataLoaderService service1 = TestDataLoaderService.create( TestSystemA.class, runtime );
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.CONNECTED ) );

    final ConnectorEntry entry1 = runtime.getConnectorEntryBySystemType( service1.getSystemType() );

    runtime.activate();
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    runtime.deactivate();
    assertEquals( service1.isDisconnectCalled(), true );

    // set service state to in transition so connect is not called

    runtime.activate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.CONNECTING ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    runtime.deactivate();
    assertEquals( service1.isDisconnectCalled(), false );

    runtime.activate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.DISCONNECTING ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    runtime.deactivate();
    assertEquals( service1.isDisconnectCalled(), false );

    // set service state to DISCONNECTED so no action required

    runtime.activate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.DISCONNECTED ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    runtime.deactivate();
    assertEquals( service1.isDisconnectCalled(), false );

    // set service state to ERROR so no action required

    runtime.activate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.ERROR ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    runtime.deactivate();
    assertEquals( service1.isDisconnectCalled(), false );

    // set service state to connected but rate limit it

    runtime.activate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.CONNECTED ) );
    entry1.getRateLimiter().setTokenCount( 0 );
    service1.reset();
    runtime.deactivate();
    assertEquals( service1.isDisconnectCalled(), false );

    // set service state to connected

    runtime.activate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.CONNECTED ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    runtime.deactivate();
    assertEquals( service1.isDisconnectCalled(), true );
  }

  @Test
  public void deactivateMultipleDataSources()
  {
    final ReplicantRuntime runtime = ReplicantRuntime.create();
    final TestDataLoaderService service1 = TestDataLoaderService.create( TestSystemA.class, runtime );
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.CONNECTED ) );

    final TestDataLoaderService service3 = TestDataLoaderService.create( TestSystemC.class, runtime );
    Arez.context().safeAction( () -> service3.setState( DataLoaderService.State.CONNECTED ) );

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
    // Single data source, required

    assertUpdateState( RuntimeState.DISCONNECTED, DataLoaderService.State.DISCONNECTED );
    assertUpdateState( RuntimeState.CONNECTED, DataLoaderService.State.CONNECTED );
    assertUpdateState( RuntimeState.CONNECTING, DataLoaderService.State.CONNECTING );
    assertUpdateState( RuntimeState.DISCONNECTING, DataLoaderService.State.DISCONNECTING );
    assertUpdateState( RuntimeState.ERROR, DataLoaderService.State.ERROR );

    // 2 Data sources, both required

    assertUpdateState( RuntimeState.ERROR,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.ERROR );
    assertUpdateState( RuntimeState.ERROR,
                       DataLoaderService.State.CONNECTING,
                       DataLoaderService.State.ERROR );
    assertUpdateState( RuntimeState.ERROR,
                       DataLoaderService.State.DISCONNECTED,
                       DataLoaderService.State.ERROR );
    assertUpdateState( RuntimeState.ERROR,
                       DataLoaderService.State.DISCONNECTING,
                       DataLoaderService.State.ERROR );
    assertUpdateState( RuntimeState.ERROR,
                       DataLoaderService.State.ERROR,
                       DataLoaderService.State.ERROR );

    assertUpdateState( RuntimeState.CONNECTED,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.CONNECTED );
    assertUpdateState( RuntimeState.CONNECTING,
                       DataLoaderService.State.CONNECTING,
                       DataLoaderService.State.CONNECTED );
    assertUpdateState( RuntimeState.DISCONNECTED,
                       DataLoaderService.State.DISCONNECTED,
                       DataLoaderService.State.CONNECTED );
    assertUpdateState( RuntimeState.DISCONNECTING,
                       DataLoaderService.State.DISCONNECTING,
                       DataLoaderService.State.CONNECTED );

    assertUpdateState( RuntimeState.CONNECTING,
                       DataLoaderService.State.CONNECTING,
                       DataLoaderService.State.CONNECTING );
    assertUpdateState( RuntimeState.DISCONNECTED,
                       DataLoaderService.State.DISCONNECTED,
                       DataLoaderService.State.CONNECTING );
    assertUpdateState( RuntimeState.DISCONNECTING,
                       DataLoaderService.State.DISCONNECTING,
                       DataLoaderService.State.CONNECTING );

    assertUpdateState( RuntimeState.DISCONNECTED,
                       DataLoaderService.State.DISCONNECTED,
                       DataLoaderService.State.DISCONNECTING );
    assertUpdateState( RuntimeState.DISCONNECTING,
                       DataLoaderService.State.DISCONNECTING,
                       DataLoaderService.State.DISCONNECTING );

    assertUpdateState( RuntimeState.DISCONNECTED,
                       DataLoaderService.State.DISCONNECTED,
                       DataLoaderService.State.DISCONNECTED );

    // 3 Data sources, first two required, third optional

    assertUpdateState( RuntimeState.CONNECTED,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.CONNECTED );
    assertUpdateState( RuntimeState.CONNECTED,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.ERROR );
    assertUpdateState( RuntimeState.CONNECTED,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.CONNECTING );
    assertUpdateState( RuntimeState.CONNECTED,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.DISCONNECTING );
    assertUpdateState( RuntimeState.CONNECTED,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.DISCONNECTED );

    assertUpdateState( RuntimeState.CONNECTING,
                       DataLoaderService.State.CONNECTING,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.ERROR );
    assertUpdateState( RuntimeState.CONNECTING,
                       DataLoaderService.State.CONNECTING,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.CONNECTED );
    assertUpdateState( RuntimeState.CONNECTING,
                       DataLoaderService.State.CONNECTING,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.CONNECTING );
    assertUpdateState( RuntimeState.CONNECTING,
                       DataLoaderService.State.CONNECTING,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.DISCONNECTED );
    assertUpdateState( RuntimeState.CONNECTING,
                       DataLoaderService.State.CONNECTING,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.DISCONNECTING );
  }

  private void assertUpdateState( @Nonnull final RuntimeState expectedSystemState,
                                  @Nonnull final DataLoaderService.State service1State )
  {
    final Disposable schedulerLock = Arez.context().pauseScheduler();
    final ReplicantRuntime runtime = ReplicantRuntime.create();
    final TestDataLoaderService service1 = TestDataLoaderService.create( TestSystemA.class, runtime );
    Arez.context().safeAction( () -> service1.setState( service1State ) );

    Arez.context().safeAction( () -> assertEquals( runtime.getState(), expectedSystemState ) );

    schedulerLock.dispose();
  }

  private void assertUpdateState( @Nonnull final RuntimeState expectedSystemState,
                                  @Nonnull final DataLoaderService.State service1State,
                                  @Nonnull final DataLoaderService.State service2State )
  {
    final Disposable schedulerLock = Arez.context().pauseScheduler();
    final ReplicantRuntime runtime = ReplicantRuntime.create();
    final TestDataLoaderService service1 = TestDataLoaderService.create( TestSystemA.class, runtime );
    Arez.context().safeAction( () -> service1.setState( service1State ) );
    final TestDataLoaderService service2 = TestDataLoaderService.create( TestSystemB.class, runtime );
    Arez.context().safeAction( () -> service2.setState( service2State ) );

    Arez.context().safeAction( () -> assertEquals( runtime.getState(), expectedSystemState ) );

    schedulerLock.dispose();
  }

  private void assertUpdateState( @Nonnull final RuntimeState expectedSystemState,
                                  @Nonnull final DataLoaderService.State service1State,
                                  @Nonnull final DataLoaderService.State service2State,
                                  @Nonnull final DataLoaderService.State service3State )
  {
    final Disposable schedulerLock = Arez.context().pauseScheduler();

    final ReplicantRuntime runtime = ReplicantRuntime.create();
    final TestDataLoaderService service1 = TestDataLoaderService.create( TestSystemA.class, runtime );
    Arez.context().safeAction( () -> service1.setState( service1State ) );

    final TestDataLoaderService service2 = TestDataLoaderService.create( TestSystemB.class, runtime );
    Arez.context().safeAction( () -> service2.setState( service2State ) );

    final TestDataLoaderService service3 = TestDataLoaderService.create( TestSystemC.class, runtime );
    Arez.context().safeAction( () -> service3.setState( service3State ) );

    runtime.setDataSourceRequired( service3.getSystemType(), false );

    Arez.context().safeAction( () -> assertEquals( runtime.getState(), expectedSystemState ) );

    schedulerLock.dispose();
  }

  @ArezComponent
  static abstract class TestDataLoaderService
    extends Connector
    implements DataLoaderService
  {
    private boolean _connectCalled;
    private boolean _disconnectCalled;

    static TestDataLoaderService create( @Nonnull final Class<?> systemType )
    {
      return create( systemType, ReplicantRuntime.create() );
    }

    static TestDataLoaderService create( @Nonnull final Class<?> systemType,
                                         @Nonnull final ReplicantRuntime runtime )
    {
      return Arez.context()
        .safeAction( () -> new ReplicantRuntimeTest_Arez_TestDataLoaderService( systemType, runtime ) );
    }

    TestDataLoaderService( @Nonnull final Class<?> systemType, @Nonnull final ReplicantRuntime runtime )
    {
      super( systemType, runtime );
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
    public boolean isAreaOfInterestActionPending( @Nonnull final AreaOfInterestAction action,
                                                  @Nonnull final ChannelAddress address,
                                                  @Nullable final Object filter )
    {
      return false;
    }

    @Override
    public void requestSubscribe( @Nonnull final ChannelAddress address, @Nullable final Object filterParameter )
    {
    }

    @Override
    public void requestSubscriptionUpdate( @Nonnull final ChannelAddress address,
                                           @Nullable final Object filterParameter )
    {
    }

    @Override
    public void requestUnsubscribe( @Nonnull final ChannelAddress address )
    {
    }

    @Override
    public int indexOfPendingAreaOfInterestAction( @Nonnull final AreaOfInterestAction action,
                                                   @Nonnull final ChannelAddress address,
                                                   @Nullable final Object filter )
    {
      return -1;
    }
  }
}
