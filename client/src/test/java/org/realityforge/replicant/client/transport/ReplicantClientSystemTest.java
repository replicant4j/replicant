package org.realityforge.replicant.client.transport;

import arez.Arez;
import arez.annotations.ArezComponent;
import arez.annotations.Observable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ChannelAddress;
import static org.testng.Assert.*;

public class ReplicantClientSystemTest
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
  public void getDataLoaderService()
  {
    final TestDataLoaderService service1 = TestDataLoaderService.create( TestSystemA.class );
    final TestDataLoaderService service2 = TestDataLoaderService.create( TestSystemB.class );

    final ReplicantClientSystem system = ReplicantClientSystem.create();
    Arez.context().safeAction( () -> system.registerDataSource( service1 ) );
    Arez.context().safeAction( () -> system.registerDataSource( service2 ) );

    assertEquals( system.getDataLoaderService( service1.getSystemType() ), service1 );
    assertEquals( system.getDataLoaderService( service2.getSystemType() ), service2 );

    assertThrows( IllegalStateException.class, () -> system.getDataLoaderService( TestSystemC.class ) );
  }

  @Test
  public void activate()
  {
    final TestDataLoaderService service1 = TestDataLoaderService.create( TestSystemA.class );
    service1.setState( DataLoaderService.State.DISCONNECTED );

    final ReplicantClientSystem system = ReplicantClientSystem.create();
    Arez.context().safeAction( () -> system.registerDataSource( service1 ) );
    final DataLoaderEntry entry1 = system.getDataLoaderEntryBySystemType( service1.getSystemType() );

    system.deactivate();
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    system.activate();
    assertEquals( service1.isConnectCalled(), true );

    // set service state to in transition so connect is not called

    system.deactivate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.CONNECTING ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    system.activate();
    assertEquals( service1.isConnectCalled(), false );

    system.deactivate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.DISCONNECTING ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    system.activate();
    assertEquals( service1.isConnectCalled(), false );

    // set service state to connected so no action required

    system.deactivate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.CONNECTED ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    system.activate();
    assertEquals( service1.isConnectCalled(), false );

    // set service state to disconnected but rate limit it

    system.deactivate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.DISCONNECTED ) );
    entry1.getRateLimiter().setTokenCount( 0 );
    service1.reset();
    system.activate();
    assertEquals( service1.isConnectCalled(), false );

    // set service state to disconnected but rate limit it

    system.deactivate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.DISCONNECTED ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    system.activate();
    assertEquals( service1.isConnectCalled(), true );
  }

  @Test
  public void activateMultiple()
  {
    final TestDataLoaderService service1 = TestDataLoaderService.create( TestSystemA.class );
    service1.setState( DataLoaderService.State.DISCONNECTED );

    final TestDataLoaderService service3 = TestDataLoaderService.create( TestSystemC.class );
    service3.setState( DataLoaderService.State.DISCONNECTED );

    final ReplicantClientSystem system = ReplicantClientSystem.create();
    Arez.context().safeAction( () -> system.registerDataSource( service1 ) );
    final DataLoaderEntry entry1 = system.getDataLoaderEntryBySystemType( service1.getSystemType() );
    Arez.context().safeAction( () -> system.registerDataSource( service3 ) );
    final DataLoaderEntry entry3 = system.getDataLoaderEntryBySystemType( service3.getSystemType() );

    entry1.getRateLimiter().fillBucket();
    service1.reset();
    entry3.getRateLimiter().fillBucket();
    service1.reset();
    service3.reset();
    system.activate();
    assertEquals( service1.isConnectCalled(), true );
    assertEquals( service3.isConnectCalled(), true );

    // set service state to in transition so connect is not called

    system.deactivate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.CONNECTING ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    Arez.context().safeAction( () -> service3.setState( DataLoaderService.State.CONNECTING ) );
    entry3.getRateLimiter().fillBucket();
    service3.reset();
    system.activate();
    assertEquals( service1.isConnectCalled(), false );
    assertEquals( service3.isConnectCalled(), false );

    system.deactivate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.DISCONNECTING ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    Arez.context().safeAction( () -> service3.setState( DataLoaderService.State.DISCONNECTING ) );
    entry3.getRateLimiter().fillBucket();
    service3.reset();
    system.activate();
    assertEquals( service1.isConnectCalled(), false );
    assertEquals( service3.isConnectCalled(), false );

    // set service state to connected so no action required

    system.deactivate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.CONNECTED ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    Arez.context().safeAction( () -> service3.setState( DataLoaderService.State.CONNECTED ) );
    entry3.getRateLimiter().fillBucket();
    service3.reset();
    system.activate();
    assertEquals( service1.isConnectCalled(), false );
    assertEquals( service3.isConnectCalled(), false );

    // set service state to disconnected but rate limit it

    system.deactivate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.DISCONNECTED ) );
    entry1.getRateLimiter().setTokenCount( 0 );
    service1.reset();
    Arez.context().safeAction( () -> service3.setState( DataLoaderService.State.DISCONNECTED ) );
    entry3.getRateLimiter().setTokenCount( 0 );
    service3.reset();
    system.activate();
    assertEquals( service1.isConnectCalled(), false );
    assertEquals( service3.isConnectCalled(), false );

    // set service state to disconnected but rate limit it

    system.deactivate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.DISCONNECTED ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    Arez.context().safeAction( () -> service3.setState( DataLoaderService.State.DISCONNECTED ) );
    entry3.getRateLimiter().fillBucket();
    service3.reset();
    system.activate();
    assertEquals( service1.isConnectCalled(), true );
    assertEquals( service3.isConnectCalled(), true );
  }

  @Test
  public void deactivate()
  {
    final TestDataLoaderService service1 = TestDataLoaderService.create( TestSystemA.class );
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.CONNECTED ) );

    final ReplicantClientSystem system = ReplicantClientSystem.create();
    Arez.context().safeAction( () -> system.registerDataSource( service1 ) );
    final DataLoaderEntry entry1 = system.getDataLoaderEntryBySystemType( service1.getSystemType() );

    system.activate();
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    system.deactivate();
    assertEquals( service1.isDisconnectCalled(), true );

    // set service state to in transition so connect is not called

    system.activate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.CONNECTING ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    system.deactivate();
    assertEquals( service1.isDisconnectCalled(), false );

    system.activate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.DISCONNECTING ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    system.deactivate();
    assertEquals( service1.isDisconnectCalled(), false );

    // set service state to DISCONNECTED so no action required

    system.activate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.DISCONNECTED ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    system.deactivate();
    assertEquals( service1.isDisconnectCalled(), false );

    // set service state to ERROR so no action required

    system.activate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.ERROR ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    system.deactivate();
    assertEquals( service1.isDisconnectCalled(), false );

    // set service state to connected but rate limit it

    system.activate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.CONNECTED ) );
    entry1.getRateLimiter().setTokenCount( 0 );
    service1.reset();
    system.deactivate();
    assertEquals( service1.isDisconnectCalled(), false );

    // set service state to connected

    system.activate();
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.CONNECTED ) );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    system.deactivate();
    assertEquals( service1.isDisconnectCalled(), true );
  }

  @Test
  public void deactivateMultipleDataSources()
  {
    final TestDataLoaderService service1 = TestDataLoaderService.create( TestSystemA.class );
    Arez.context().safeAction( () -> service1.setState( DataLoaderService.State.CONNECTED ) );

    final TestDataLoaderService service3 = TestDataLoaderService.create( TestSystemC.class );
    Arez.context().safeAction( () -> service3.setState( DataLoaderService.State.CONNECTED ) );

    final ReplicantClientSystem system = ReplicantClientSystem.create();
    Arez.context().safeAction( () -> system.registerDataSource( service1 ) );
    final DataLoaderEntry entry1 = system.getDataLoaderEntryBySystemType( service1.getSystemType() );
    Arez.context().safeAction( () -> system.registerDataSource( service3 ) );
    final DataLoaderEntry entry3 = system.getDataLoaderEntryBySystemType( service3.getSystemType() );
    entry3.setRequired( false );

    system.activate();
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    entry3.getRateLimiter().fillBucket();
    service3.reset();
    system.deactivate();
    assertEquals( service1.isDisconnectCalled(), true );
    assertEquals( service3.isDisconnectCalled(), true );
  }

  @Test
  public void updateStatus()
  {
    // Single data source, required

    assertUpdateState( ReplicantClientSystem.State.DISCONNECTED, DataLoaderService.State.DISCONNECTED );
    assertUpdateState( ReplicantClientSystem.State.CONNECTED, DataLoaderService.State.CONNECTED );
    assertUpdateState( ReplicantClientSystem.State.CONNECTING, DataLoaderService.State.CONNECTING );
    assertUpdateState( ReplicantClientSystem.State.DISCONNECTING, DataLoaderService.State.DISCONNECTING );
    assertUpdateState( ReplicantClientSystem.State.ERROR, DataLoaderService.State.ERROR );

    // 2 Data sources, both required

    assertUpdateState( ReplicantClientSystem.State.ERROR,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.ERROR );
    assertUpdateState( ReplicantClientSystem.State.ERROR,
                       DataLoaderService.State.CONNECTING,
                       DataLoaderService.State.ERROR );
    assertUpdateState( ReplicantClientSystem.State.ERROR,
                       DataLoaderService.State.DISCONNECTED,
                       DataLoaderService.State.ERROR );
    assertUpdateState( ReplicantClientSystem.State.ERROR,
                       DataLoaderService.State.DISCONNECTING,
                       DataLoaderService.State.ERROR );
    assertUpdateState( ReplicantClientSystem.State.ERROR,
                       DataLoaderService.State.ERROR,
                       DataLoaderService.State.ERROR );

    assertUpdateState( ReplicantClientSystem.State.CONNECTED,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.CONNECTED );
    assertUpdateState( ReplicantClientSystem.State.CONNECTING,
                       DataLoaderService.State.CONNECTING,
                       DataLoaderService.State.CONNECTED );
    assertUpdateState( ReplicantClientSystem.State.DISCONNECTED,
                       DataLoaderService.State.DISCONNECTED,
                       DataLoaderService.State.CONNECTED );
    assertUpdateState( ReplicantClientSystem.State.DISCONNECTING,
                       DataLoaderService.State.DISCONNECTING,
                       DataLoaderService.State.CONNECTED );

    assertUpdateState( ReplicantClientSystem.State.CONNECTING,
                       DataLoaderService.State.CONNECTING,
                       DataLoaderService.State.CONNECTING );
    assertUpdateState( ReplicantClientSystem.State.DISCONNECTED,
                       DataLoaderService.State.DISCONNECTED,
                       DataLoaderService.State.CONNECTING );
    assertUpdateState( ReplicantClientSystem.State.DISCONNECTING,
                       DataLoaderService.State.DISCONNECTING,
                       DataLoaderService.State.CONNECTING );

    assertUpdateState( ReplicantClientSystem.State.DISCONNECTED,
                       DataLoaderService.State.DISCONNECTED,
                       DataLoaderService.State.DISCONNECTING );
    assertUpdateState( ReplicantClientSystem.State.DISCONNECTING,
                       DataLoaderService.State.DISCONNECTING,
                       DataLoaderService.State.DISCONNECTING );

    assertUpdateState( ReplicantClientSystem.State.DISCONNECTED,
                       DataLoaderService.State.DISCONNECTED,
                       DataLoaderService.State.DISCONNECTED );

    // 3 Data sources, first two required, third optional

    assertUpdateState( ReplicantClientSystem.State.CONNECTED,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.CONNECTED );
    assertUpdateState( ReplicantClientSystem.State.CONNECTED,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.ERROR );
    assertUpdateState( ReplicantClientSystem.State.CONNECTED,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.CONNECTING );
    assertUpdateState( ReplicantClientSystem.State.CONNECTED,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.DISCONNECTING );
    assertUpdateState( ReplicantClientSystem.State.CONNECTED,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.DISCONNECTED );

    assertUpdateState( ReplicantClientSystem.State.CONNECTING,
                       DataLoaderService.State.CONNECTING,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.ERROR );
    assertUpdateState( ReplicantClientSystem.State.CONNECTING,
                       DataLoaderService.State.CONNECTING,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.CONNECTED );
    assertUpdateState( ReplicantClientSystem.State.CONNECTING,
                       DataLoaderService.State.CONNECTING,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.CONNECTING );
    assertUpdateState( ReplicantClientSystem.State.CONNECTING,
                       DataLoaderService.State.CONNECTING,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.DISCONNECTED );
    assertUpdateState( ReplicantClientSystem.State.CONNECTING,
                       DataLoaderService.State.CONNECTING,
                       DataLoaderService.State.CONNECTED,
                       DataLoaderService.State.DISCONNECTING );
  }

  private void assertUpdateState( @Nonnull final ReplicantClientSystem.State expectedSystemState,
                                  @Nonnull final DataLoaderService.State service1State )
  {
    final TestDataLoaderService service1 = TestDataLoaderService.create( TestSystemA.class );
    Arez.context().safeAction( () -> service1.setState( service1State ) );

    final ReplicantClientSystem system = ReplicantClientSystem.create();
    Arez.context().safeAction( () -> system.registerDataSource( service1 ) );

    Arez.context().safeAction( () -> assertEquals( system.getState(), expectedSystemState ) );
  }

  private void assertUpdateState( @Nonnull final ReplicantClientSystem.State expectedSystemState,
                                  @Nonnull final DataLoaderService.State service1State,
                                  @Nonnull final DataLoaderService.State service2State )
  {
    final TestDataLoaderService service1 = TestDataLoaderService.create( TestSystemA.class );
    Arez.context().safeAction( () -> service1.setState( service1State ) );

    final TestDataLoaderService service2 = TestDataLoaderService.create( TestSystemB.class );
    Arez.context().safeAction( () -> service2.setState( service2State ) );

    final ReplicantClientSystem system = ReplicantClientSystem.create();
    Arez.context().safeAction( () -> system.registerDataSource( service1 ) );
    Arez.context().safeAction( () -> system.registerDataSource( service2 ) );

    Arez.context().safeAction( () -> assertEquals( system.getState(), expectedSystemState ) );
  }

  private void assertUpdateState( @Nonnull final ReplicantClientSystem.State expectedSystemState,
                                  @Nonnull final DataLoaderService.State service1State,
                                  @Nonnull final DataLoaderService.State service2State,
                                  @Nonnull final DataLoaderService.State service3State )
  {
    final TestDataLoaderService service1 = TestDataLoaderService.create( TestSystemA.class );
    Arez.context().safeAction( () -> service1.setState( service1State ) );

    final TestDataLoaderService service2 = TestDataLoaderService.create( TestSystemB.class );
    Arez.context().safeAction( () -> service2.setState( service2State ) );

    final TestDataLoaderService service3 = TestDataLoaderService.create( TestSystemC.class );
    Arez.context().safeAction( () -> service3.setState( service3State ) );

    final ReplicantClientSystem system = ReplicantClientSystem.create();
    Arez.context().safeAction( () -> system.registerDataSource( service1 ) );
    Arez.context().safeAction( () -> system.registerDataSource( service2 ) );
    Arez.context().safeAction( () -> system.registerDataSource( service3 ) );
    system.getDataLoaderEntryBySystemType( service3.getSystemType() ).setRequired( false );

    Arez.context().safeAction( () -> assertEquals( system.getState(), expectedSystemState ) );
  }

  @Test
  public void convergingDisconnectedSystemDoesNothing()
  {
    final ReplicantClientSystem system = ReplicantClientSystem.create();
    final TestDataLoaderService service1 = TestDataLoaderService.create( TestSystemA.class );
    Arez.context().safeAction( () -> system.registerDataSource( service1 ) );

    Arez.context().safeAction( () -> assertEquals( system.getState(), ReplicantClientSystem.State.DISCONNECTED ) );
    Arez.context().safeAction( () -> assertEquals( system.isActive(), false ) );
    Arez.context().safeAction( () -> assertEquals( service1.getState(), DataLoaderService.State.DISCONNECTED ) );
  }

  @ArezComponent
  static abstract class TestDataLoaderService
    implements DataLoaderService
  {
    private final Class<? extends Enum> _systemType;
    private State _state;
    private boolean _connectCalled;
    private boolean _disconnectCalled;

    static TestDataLoaderService create( @Nonnull final Class<? extends Enum> systemType )
    {
      return new ReplicantClientSystemTest_Arez_TestDataLoaderService( systemType );
    }

    TestDataLoaderService( @Nonnull final Class<? extends Enum> systemType )
    {
      _systemType = systemType;
      _state = State.DISCONNECTED;
    }

    void setState( @Nonnull final State state )
    {
      _state = state;
    }

    @Nonnull
    @Override
    @Observable
    public State getState()
    {
      return _state;
    }

    void reset()
    {
      _connectCalled = false;
      _disconnectCalled = false;
    }

    @Override
    public void connect()
    {
      _connectCalled = true;
    }

    boolean isConnectCalled()
    {
      return _connectCalled;
    }

    @Override
    public void disconnect()
    {
      _disconnectCalled = true;
    }

    boolean isDisconnectCalled()
    {
      return _disconnectCalled;
    }

    @Nonnull
    @Override
    public Class<? extends Enum> getSystemType()
    {
      return _systemType;
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
