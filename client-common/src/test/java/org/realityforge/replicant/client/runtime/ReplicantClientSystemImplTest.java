package org.realityforge.replicant.client.runtime;

import org.realityforge.replicant.client.transport.DataLoaderListenerSupport;
import org.realityforge.replicant.client.transport.DataLoaderService;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ReplicantClientSystemImplTest
{
  private enum TestGraphA
  {
    A
  }

  private enum TestGraphB
  {
    B
  }

  private enum TestGraphC
  {
    C
  }

  @Test
  public void getDataLoaderService()
  {
    final TestDataLoaderService service1 = newServiceA();
    final TestDataLoaderService service2 = newServiceB();

    final ReplicantClientSystemImpl system =
      new TestReplicantClientSystemImpl( new DataLoaderEntry[]{ requiredEntry( service1 ), requiredEntry( service2 ) } );

    assertEquals( system.getDataLoaderService( TestGraphA.A ), service1 );
    assertEquals( system.getDataLoaderService( TestGraphB.B ), service2 );

    assertThrows( IllegalArgumentException.class, () -> system.getDataLoaderService( TestGraphC.C ) );
  }

  @Test
  public void listenersSetup()
  {
    final ReplicantSystemListener listener = mock( ReplicantSystemListener.class );

    final TestDataLoaderService service1 = newServiceA();
    service1.setState( DataLoaderService.State.DISCONNECTED );
    final DataLoaderEntry entry1 = requiredEntry( service1 );

    final ReplicantClientSystemImpl system = new TestReplicantClientSystemImpl( new DataLoaderEntry[]{ entry1 } );
    system.addReplicantSystemListener( listener );

    final DataLoaderListenerSupport l = service1.getListener();
    assertEquals( l.getListeners().size(), 1 );
    assertEquals( l.getListeners().contains( system.getDataLoaderListener() ), true );

    service1.setState( DataLoaderService.State.CONNECTED );
    reset( listener );
    l.onConnect( service1 );
    verify( listener, times( 1 ) ).
      stateChanged( system, ReplicantClientSystem.State.CONNECTED, ReplicantClientSystem.State.DISCONNECTED );

    service1.setState( DataLoaderService.State.ERROR );
    reset( listener );
    l.onInvalidConnect( service1, new Throwable() );
    verify( listener, times( 1 ) ).
      stateChanged( system, ReplicantClientSystem.State.ERROR, ReplicantClientSystem.State.CONNECTED );

    service1.setState( DataLoaderService.State.DISCONNECTED );
    reset( listener );
    l.onDisconnect( service1 );
    verify( listener, times( 1 ) ).
      stateChanged( system, ReplicantClientSystem.State.DISCONNECTED, ReplicantClientSystem.State.ERROR );

    service1.setState( DataLoaderService.State.ERROR );
    reset( listener );
    l.onInvalidDisconnect( service1, new Throwable() );
    verify( listener, times( 1 ) ).
      stateChanged( system, ReplicantClientSystem.State.ERROR, ReplicantClientSystem.State.DISCONNECTED );

    /// This tests that disconnects occurs if not in transition state

    service1.reset();
    l.onPollFailure( service1, new Throwable() );
    assertEquals( service1.isDisconnectCalled(), true );

    service1.setState( DataLoaderService.State.ERROR );
    service1.reset();
    l.onPollFailure( service1, new Throwable() );
    assertEquals( service1.isDisconnectCalled(), true );

    service1.setState( DataLoaderService.State.ERROR );
    service1.reset();
    l.onPollFailure( service1, new Throwable() );
    assertEquals( service1.isDisconnectCalled(), true );

    service1.setState( DataLoaderService.State.CONNECTING );
    service1.reset();
    l.onPollFailure( service1, new Throwable() );
    assertEquals( service1.isDisconnectCalled(), false );

    service1.setState( DataLoaderService.State.DISCONNECTING );
    service1.reset();
    l.onPollFailure( service1, new Throwable() );
    assertEquals( service1.isDisconnectCalled(), false );

    service1.setState( DataLoaderService.State.CONNECTED );
    service1.reset();
    l.onDataLoadFailure( service1, new Throwable() );
    assertEquals( service1.isDisconnectCalled(), true );

    service1.setState( DataLoaderService.State.ERROR );
    service1.reset();
    l.onDataLoadFailure( service1, new Throwable() );
    assertEquals( service1.isDisconnectCalled(), true );

    service1.setState( DataLoaderService.State.ERROR );
    service1.reset();
    l.onDataLoadFailure( service1, new Throwable() );
    assertEquals( service1.isDisconnectCalled(), true );

    service1.setState( DataLoaderService.State.CONNECTING );
    service1.reset();
    l.onDataLoadFailure( service1, new Throwable() );
    assertEquals( service1.isDisconnectCalled(), false );

    service1.setState( DataLoaderService.State.DISCONNECTING );
    service1.reset();
    l.onDataLoadFailure( service1, new Throwable() );
    assertEquals( service1.isDisconnectCalled(), false );
  }

  @Test
  public void activate()
  {
    final ReplicantSystemListener listener = mock( ReplicantSystemListener.class );

    final TestDataLoaderService service1 = newServiceA();
    service1.setState( DataLoaderService.State.DISCONNECTED );
    final DataLoaderEntry entry1 = requiredEntry( service1 );

    final ReplicantClientSystemImpl system = new TestReplicantClientSystemImpl( new DataLoaderEntry[]{ entry1 } );
    system.addReplicantSystemListener( listener );

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
    final ReplicantSystemListener listener = mock( ReplicantSystemListener.class );

    final TestDataLoaderService service1 = newServiceA();
    service1.setState( DataLoaderService.State.DISCONNECTED );
    final DataLoaderEntry entry1 = requiredEntry( service1 );

    final TestDataLoaderService service3 = newServiceC();
    service3.setState( DataLoaderService.State.DISCONNECTED );
    final DataLoaderEntry entry3 = requiredEntry( service3 );

    final ReplicantClientSystemImpl system = new TestReplicantClientSystemImpl( new DataLoaderEntry[]{ entry1, entry3 } );
    system.addReplicantSystemListener( listener );

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
    final ReplicantSystemListener listener = mock( ReplicantSystemListener.class );

    final TestDataLoaderService service1 = newServiceA();
    service1.setState( DataLoaderService.State.CONNECTED );
    final DataLoaderEntry entry1 = requiredEntry( service1 );

    final ReplicantClientSystemImpl system = new TestReplicantClientSystemImpl( new DataLoaderEntry[]{ entry1 } );
    system.addReplicantSystemListener( listener );

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
    final ReplicantSystemListener listener = mock( ReplicantSystemListener.class );

    final TestDataLoaderService service1 = newServiceA();
    service1.setState( DataLoaderService.State.CONNECTED );
    final DataLoaderEntry entry1 = requiredEntry( service1 );

    final TestDataLoaderService service3 = newServiceC();
    service3.setState( DataLoaderService.State.CONNECTED );
    final DataLoaderEntry entry3 = optionalEntry( service3 );

    final ReplicantClientSystemImpl system = new TestReplicantClientSystemImpl( new DataLoaderEntry[]{ entry1, entry3 } );
    system.addReplicantSystemListener( listener );

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

  private void assertUpdateState( final ReplicantClientSystem.State systemState,
                                  final DataLoaderService.State service1State )
  {
    final TestDataLoaderService service1 = newServiceA();
    service1.setState( service1State );

    verifyUpdateState( systemState, new DataLoaderEntry[]{ requiredEntry( service1 ) } );
  }

  private void assertUpdateState( final ReplicantClientSystem.State systemState,
                                  final DataLoaderService.State service1State,
                                  final DataLoaderService.State service2State )
  {
    final TestDataLoaderService service1 = newServiceA();
    service1.setState( service1State );

    final TestDataLoaderService service2 = newServiceB();
    service2.setState( service2State );

    verifyUpdateState( systemState, new DataLoaderEntry[]{ requiredEntry( service1 ), requiredEntry( service2 ) } );
  }

  private void assertUpdateState( final ReplicantClientSystem.State systemState,
                                  final DataLoaderService.State service1State,
                                  final DataLoaderService.State service2State,
                                  final DataLoaderService.State service3State )
  {
    final TestDataLoaderService service1 = newServiceA();
    service1.setState( service1State );

    final TestDataLoaderService service2 = newServiceB();
    service2.setState( service2State );

    final TestDataLoaderService service3 = newServiceC();
    service3.setState( service3State );

    verifyUpdateState( systemState,
                       new DataLoaderEntry[]{ requiredEntry( service1 ),
                                              requiredEntry( service2 ),
                                              optionalEntry( service3 ) } );
  }

  private void verifyUpdateState( final ReplicantClientSystem.State expectedSystemState,
                                  final DataLoaderEntry[] dataLoaders )
  {
    final ReplicantSystemListener listener = mock( ReplicantSystemListener.class );
    final ReplicantClientSystemImpl system = new TestReplicantClientSystemImpl( dataLoaders );
    system.addReplicantSystemListener( listener );
    assertEquals( system.getState(), ReplicantClientSystem.State.DISCONNECTED );
    reset( listener );
    system.updateStatus();
    assertEquals( system.getState(), expectedSystemState );
    if ( expectedSystemState == ReplicantClientSystem.State.DISCONNECTED )
    {
      verify( listener, never() ).stateChanged( any(), any(), any() );
    }
    else
    {
      verify( listener, times( 1 ) ).stateChanged( eq( system ),
                                                   eq( expectedSystemState ),
                                                   eq( ReplicantClientSystem.State.DISCONNECTED ) );
    }
  }

  @Test
  public void convergingDisconnectedSystemDoesNothing()
  {
    final ReplicantSystemListener listener = mock( ReplicantSystemListener.class );

    final DataLoaderEntry[] dataLoaders = { requiredEntry( newServiceA() ) };

    final ReplicantClientSystemImpl system = new TestReplicantClientSystemImpl( dataLoaders );
    system.addReplicantSystemListener( listener );

    assertEquals( system.getState(), ReplicantClientSystem.State.DISCONNECTED );
    assertEquals( system.isActive(), false );
    assertEquals( newServiceA().getState(), DataLoaderService.State.DISCONNECTED );

    system.converge();

    verify( listener, never() ).stateChanged( any(), any(), any() );

    assertEquals( system.getState(), ReplicantClientSystem.State.DISCONNECTED );
    assertEquals( system.isActive(), false );
    assertEquals( newServiceA().getState(), DataLoaderService.State.DISCONNECTED );
  }

  private DataLoaderEntry requiredEntry( final TestDataLoaderService service )
  {
    return new DataLoaderEntry( service, true );
  }

  private DataLoaderEntry optionalEntry( final TestDataLoaderService service )
  {
    return new DataLoaderEntry( service, false );
  }

  private TestDataLoaderService newServiceA()
  {
    return new TestDataLoaderService( "A", TestGraphA.class );
  }

  private TestDataLoaderService newServiceB()
  {
    return new TestDataLoaderService( "B", TestGraphB.class );
  }

  private TestDataLoaderService newServiceC()
  {
    return new TestDataLoaderService( "C", TestGraphC.class );
  }
}
