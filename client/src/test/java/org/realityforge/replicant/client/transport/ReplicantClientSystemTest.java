package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
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
    final TestDataLoaderService service1 = newServiceA();
    final TestDataLoaderService service2 = newServiceB();

    final ReplicantClientSystem system = new ReplicantClientSystem();
    system.registerDataSource( service1 );
    system.registerDataSource( service2 );

    assertEquals( system.getDataLoaderService( service1.getSystemType() ), service1 );
    assertEquals( system.getDataLoaderService( service2.getSystemType() ), service2 );

    assertThrows( IllegalStateException.class, () -> system.getDataLoaderService( TestSystemC.class ) );
  }

  @Test
  public void activate()
  {
    final TestDataLoaderService service1 = newServiceA();
    service1.setState( DataLoaderService.State.DISCONNECTED );

    final ReplicantClientSystem system = new ReplicantClientSystem();
    system.registerDataSource( service1 );
    final DataLoaderEntry entry1 = system.getDataLoaderEntryBySystemType( service1.getSystemType() );

    entry1.getRateLimiter().fillBucket();
    service1.reset();
    system.activate();
    assertEquals( service1.isConnectCalled(), true );

    // set service state to in transition so connect is not called

    service1.setState( DataLoaderService.State.CONNECTING );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    system.activate();
    assertEquals( service1.isConnectCalled(), false );

    service1.setState( DataLoaderService.State.DISCONNECTING );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    system.activate();
    assertEquals( service1.isConnectCalled(), false );

    // set service state to connected so no action required

    service1.setState( DataLoaderService.State.CONNECTED );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    system.activate();
    assertEquals( service1.isConnectCalled(), false );

    // set service state to disconnected but rate limit it

    service1.setState( DataLoaderService.State.DISCONNECTED );
    entry1.getRateLimiter().setTokenCount( 0 );
    service1.reset();
    system.activate();
    assertEquals( service1.isConnectCalled(), false );

    // set service state to disconnected but rate limit it

    service1.setState( DataLoaderService.State.DISCONNECTED );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    system.activate();
    assertEquals( service1.isConnectCalled(), true );
  }

  @Test
  public void activateMultiple()
  {
    final TestDataLoaderService service1 = newServiceA();
    service1.setState( DataLoaderService.State.DISCONNECTED );

    final TestDataLoaderService service3 = newServiceC();
    service3.setState( DataLoaderService.State.DISCONNECTED );

    final ReplicantClientSystem system = new ReplicantClientSystem();
    system.registerDataSource( service1 );
    final DataLoaderEntry entry1 = system.getDataLoaderEntryBySystemType( service1.getSystemType() );
    system.registerDataSource( service3 );
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

    service1.setState( DataLoaderService.State.CONNECTING );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    service3.setState( DataLoaderService.State.CONNECTING );
    entry3.getRateLimiter().fillBucket();
    service3.reset();
    system.activate();
    assertEquals( service1.isConnectCalled(), false );
    assertEquals( service3.isConnectCalled(), false );

    service1.setState( DataLoaderService.State.DISCONNECTING );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    service3.setState( DataLoaderService.State.DISCONNECTING );
    entry3.getRateLimiter().fillBucket();
    service3.reset();
    system.activate();
    assertEquals( service1.isConnectCalled(), false );
    assertEquals( service3.isConnectCalled(), false );

    // set service state to connected so no action required

    service1.setState( DataLoaderService.State.CONNECTED );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    service3.setState( DataLoaderService.State.CONNECTED );
    entry3.getRateLimiter().fillBucket();
    service3.reset();
    system.activate();
    assertEquals( service1.isConnectCalled(), false );
    assertEquals( service3.isConnectCalled(), false );

    // set service state to disconnected but rate limit it

    service1.setState( DataLoaderService.State.DISCONNECTED );
    entry1.getRateLimiter().setTokenCount( 0 );
    service1.reset();
    service3.setState( DataLoaderService.State.DISCONNECTED );
    entry3.getRateLimiter().setTokenCount( 0 );
    service3.reset();
    system.activate();
    assertEquals( service1.isConnectCalled(), false );
    assertEquals( service3.isConnectCalled(), false );

    // set service state to disconnected but rate limit it

    service1.setState( DataLoaderService.State.DISCONNECTED );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    service3.setState( DataLoaderService.State.DISCONNECTED );
    entry3.getRateLimiter().fillBucket();
    service3.reset();
    system.activate();
    assertEquals( service1.isConnectCalled(), true );
    assertEquals( service3.isConnectCalled(), true );
  }

  @Test
  public void deactivate()
  {
    final TestDataLoaderService service1 = newServiceA();
    service1.setState( DataLoaderService.State.CONNECTED );

    final ReplicantClientSystem system = new ReplicantClientSystem();
    system.registerDataSource( service1 );
    final DataLoaderEntry entry1 = system.getDataLoaderEntryBySystemType( service1.getSystemType() );

    entry1.getRateLimiter().fillBucket();
    service1.reset();
    system.deactivate();
    assertEquals( service1.isDisconnectCalled(), true );

    // set service state to in transition so connect is not called

    service1.setState( DataLoaderService.State.CONNECTING );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    system.deactivate();
    assertEquals( service1.isDisconnectCalled(), false );

    service1.setState( DataLoaderService.State.DISCONNECTING );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    system.deactivate();
    assertEquals( service1.isDisconnectCalled(), false );

    // set service state to DISCONNECTED so no action required

    service1.setState( DataLoaderService.State.DISCONNECTED );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    system.deactivate();
    assertEquals( service1.isDisconnectCalled(), false );

    // set service state to ERROR so no action required

    service1.setState( DataLoaderService.State.ERROR );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    system.deactivate();
    assertEquals( service1.isDisconnectCalled(), false );

    // set service state to connected but rate limit it

    service1.setState( DataLoaderService.State.CONNECTED );
    entry1.getRateLimiter().setTokenCount( 0 );
    service1.reset();
    system.deactivate();
    assertEquals( service1.isDisconnectCalled(), false );

    // set service state to connected

    service1.setState( DataLoaderService.State.CONNECTED );
    entry1.getRateLimiter().fillBucket();
    service1.reset();
    system.deactivate();
    assertEquals( service1.isDisconnectCalled(), true );
  }

  @Test
  public void deactivateMultipleDataSources()
  {
    final TestDataLoaderService service1 = newServiceA();
    service1.setState( DataLoaderService.State.CONNECTED );

    final TestDataLoaderService service3 = newServiceC();
    service3.setState( DataLoaderService.State.CONNECTED );

    final ReplicantClientSystem system = new ReplicantClientSystem();
    system.registerDataSource( service1 );
    final DataLoaderEntry entry1 = system.getDataLoaderEntryBySystemType( service1.getSystemType() );
    system.registerDataSource( service3 );
    final DataLoaderEntry entry3 = system.getDataLoaderEntryBySystemType( service3.getSystemType() );
    entry3.setRequired( false );

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
    final TestDataLoaderService service1 = newServiceA();
    service1.setState( service1State );

    final ReplicantClientSystem system = new ReplicantClientSystem();
    system.registerDataSource( service1 );

    assertEquals( system.getState(), ReplicantClientSystem.State.DISCONNECTED );
    system.updateStatus();
    assertEquals( system.getState(), expectedSystemState );
  }

  private void assertUpdateState( @Nonnull final ReplicantClientSystem.State expectedSystemState,
                                  @Nonnull final DataLoaderService.State service1State,
                                  @Nonnull final DataLoaderService.State service2State )
  {
    final TestDataLoaderService service1 = newServiceA();
    service1.setState( service1State );

    final TestDataLoaderService service2 = newServiceB();
    service2.setState( service2State );

    final ReplicantClientSystem system = new ReplicantClientSystem();
    system.registerDataSource( service1 );
    system.registerDataSource( service2 );

    assertEquals( system.getState(), ReplicantClientSystem.State.DISCONNECTED );
    system.updateStatus();
    assertEquals( system.getState(), expectedSystemState );
  }

  private void assertUpdateState( @Nonnull final ReplicantClientSystem.State expectedSystemState,
                                  @Nonnull final DataLoaderService.State service1State,
                                  @Nonnull final DataLoaderService.State service2State,
                                  @Nonnull final DataLoaderService.State service3State )
  {
    final TestDataLoaderService service1 = newServiceA();
    service1.setState( service1State );

    final TestDataLoaderService service2 = newServiceB();
    service2.setState( service2State );

    final TestDataLoaderService service3 = newServiceC();
    service3.setState( service3State );

    final ReplicantClientSystem system = new ReplicantClientSystem();
    system.registerDataSource( service1 );
    system.registerDataSource( service2 );
    system.registerDataSource( service3 );
    system.getDataLoaderEntryBySystemType( service3.getSystemType() ).setRequired( false );

    assertEquals( system.getState(), ReplicantClientSystem.State.DISCONNECTED );
    system.updateStatus();
    assertEquals( system.getState(), expectedSystemState );
  }

  @Test
  public void convergingDisconnectedSystemDoesNothing()
  {
    final ReplicantClientSystem system = new ReplicantClientSystem();
    final TestDataLoaderService service1 = newServiceA();
    system.registerDataSource( service1 );

    assertEquals( system.getState(), ReplicantClientSystem.State.DISCONNECTED );
    assertEquals( system.isActive(), false );
    assertEquals( service1.getState(), DataLoaderService.State.DISCONNECTED );

    system.converge();

    assertEquals( system.getState(), ReplicantClientSystem.State.DISCONNECTED );
    assertEquals( system.isActive(), false );
    assertEquals( service1.getState(), DataLoaderService.State.DISCONNECTED );
  }

  private TestDataLoaderService newServiceA()
  {
    return new TestDataLoaderService( TestSystemA.class );
  }

  private TestDataLoaderService newServiceB()
  {
    return new TestDataLoaderService( TestSystemB.class );
  }

  private TestDataLoaderService newServiceC()
  {
    return new TestDataLoaderService( TestSystemC.class );
  }
}
