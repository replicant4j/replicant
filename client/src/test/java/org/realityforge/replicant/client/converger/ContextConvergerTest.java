package org.realityforge.replicant.client.converger;

import arez.Arez;
import arez.Disposable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.realityforge.replicant.client.runtime.DataLoaderEntry;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;
import org.realityforge.replicant.client.transport.AreaOfInterestAction;
import org.realityforge.replicant.client.transport.DataLoaderService;
import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.ITestResult;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.AreaOfInterest;
import replicant.Channel;
import replicant.ChannelAddress;
import replicant.Replicant;
import replicant.Subscription;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ContextConvergerTest
  extends AbstractReplicantTest
  implements IHookable
{
  private enum TestSystemA
  {
    A, B
  }

  @Override
  public void run( final IHookCallBack callBack, final ITestResult testResult )
  {
    Arez.context().safeAction( () -> callBack.runTestMethod( testResult ) );
  }

  @Test
  public void listenerManagement()
  {
    final ReplicantClientSystem system = mock( ReplicantClientSystem.class );

    final DataLoaderService dl1 = mock( DataLoaderService.class );
    final DataLoaderService dl2 = mock( DataLoaderService.class );
    final DataLoaderService dl3 = mock( DataLoaderService.class );

    final ArrayList<DataLoaderEntry> dataLoaders = new ArrayList<>();
    dataLoaders.add( new DataLoaderEntry( dl1, true ) );
    dataLoaders.add( new DataLoaderEntry( dl2, true ) );
    dataLoaders.add( new DataLoaderEntry( dl3, false ) );
    when( system.getDataLoaders() ).thenReturn( dataLoaders );

    final ContextConverger c =
      new TestContextConverger( system );

    verify( dl1 ).addDataLoaderListener( any( ContextConverger.ConvergerDataLoaderListener.class ) );
    verify( dl2 ).addDataLoaderListener( any( ContextConverger.ConvergerDataLoaderListener.class ) );
    verify( dl3 ).addDataLoaderListener( any( ContextConverger.ConvergerDataLoaderListener.class ) );

    verify( system ).addReplicantSystemListener( any( ContextConverger.ConvergerReplicantSystemListener.class ) );

    c.release();

    verify( dl1 ).removeDataLoaderListener( any( ContextConverger.ConvergerDataLoaderListener.class ) );
    verify( dl2 ).removeDataLoaderListener( any( ContextConverger.ConvergerDataLoaderListener.class ) );
    verify( dl3 ).removeDataLoaderListener( any( ContextConverger.ConvergerDataLoaderListener.class ) );

    verify( system ).
      removeReplicantSystemListener( any( ContextConverger.ConvergerReplicantSystemListener.class ) );
  }

  @Test
  public void preConvergeAction()
  {
    final ContextConverger c = new TestContextConverger( mock( ReplicantClientSystem.class ) );

    // should do nothing ... particularly not crash
    c.preConverge();

    final Runnable action = mock( Runnable.class );

    c.setPreConvergeAction( action );

    c.preConverge();

    verify( action ).run();

    c.preConverge();

    verify( action, times( 2 ) ).run();

    c.setPreConvergeAction( null );

    c.preConverge();

    verify( action, times( 2 ) ).run();
  }

  @Test
  public void removeOrphanSubscription()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );
    final ChannelAddress address = new ChannelAddress( TestSystemA.A );

    final ContextConverger c = new TestContextConverger( clientSystem );

    when( clientSystem.getDataLoaderService( TestSystemA.A ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, address, null ) ).
      thenReturn( -1 );

    c.removeOrphanSubscription( address );

    verify( service ).requestUnsubscribe( address );
  }

  @Test
  public void removeOrphanSubscription_DISCONNECTED()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );
    final ChannelAddress address = new ChannelAddress( TestSystemA.A );

    final ContextConverger c = new TestContextConverger( clientSystem );

    when( clientSystem.getDataLoaderService( TestSystemA.A ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.DISCONNECTED );

    c.removeOrphanSubscription( address );

    verify( service, never() ).requestUnsubscribe( address );
  }

  @Test
  public void removeOrphanSubscription_AOI_Pending()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );
    final ChannelAddress address = new ChannelAddress( TestSystemA.A );

    final ContextConverger c = new TestContextConverger( clientSystem );

    when( clientSystem.getDataLoaderService( TestSystemA.A ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.isAreaOfInterestActionPending( AreaOfInterestAction.REMOVE, address, null ) ).
      thenReturn( Boolean.TRUE );

    c.removeOrphanSubscription( address );

    verify( service, never() ).requestUnsubscribe( address );
  }

  @Test
  public void removeSubscriptionIfOrphan()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );
    final ChannelAddress address = new ChannelAddress( TestSystemA.A );

    final ContextConverger c = new TestContextConverger( clientSystem );

    when( clientSystem.getDataLoaderService( TestSystemA.A ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, address, null ) ).
      thenReturn( -1 );

    final HashSet<ChannelAddress> expected = new HashSet<>();

    final Subscription subscription = Replicant.context().createSubscription( address, null, true );

    c.removeSubscriptionIfOrphan( expected, subscription );

    verify( service ).requestUnsubscribe( address );
  }

  @Test
  public void removeSubscriptionIfOrphan_expected()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final ChannelAddress address = new ChannelAddress( TestSystemA.A );
    final HashSet<ChannelAddress> expected = new HashSet<>();
    expected.add( address );

    final ContextConverger c = new TestContextConverger( clientSystem );

    final Subscription subscription = Replicant.context().createSubscription( address, null, true );
    c.removeSubscriptionIfOrphan( expected, subscription );

    verify( clientSystem, never() ).getDataLoaderService( TestSystemA.A );
  }

  @Test
  public void removeSubscriptionIfOrphan_notExplicit()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final ChannelAddress address = new ChannelAddress( TestSystemA.A );

    final ContextConverger c = new TestContextConverger( clientSystem );

    final HashSet<ChannelAddress> expected = new HashSet<>();

    final Subscription subscription = Replicant.context().createSubscription( address, null, false );

    c.removeSubscriptionIfOrphan( expected, subscription );

    verify( clientSystem, never() ).getDataLoaderService( TestSystemA.A );
  }

  @Test
  public void removeOrphanSubscriptions()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );
    final ChannelAddress address = new ChannelAddress( TestSystemA.A );

    final HashSet<ChannelAddress> expected = new HashSet<>();
    expected.add( new ChannelAddress( TestSystemA.B, 1 ) );
    expected.add( new ChannelAddress( TestSystemA.B, 2 ) );
    expected.add( new ChannelAddress( TestSystemA.B, 3 ) );
    expected.add( new ChannelAddress( TestSystemA.B, 4 ) );
    expected.add( new ChannelAddress( TestSystemA.B, 5 ) );

    final ContextConverger c = new TestContextConverger( clientSystem );

    when( clientSystem.getDataLoaderService( TestSystemA.A ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, address, null ) ).
      thenReturn( -1 );

    Replicant.context().createSubscription( address, null, true );

    c.removeOrphanSubscriptions( expected );

    verify( service ).requestUnsubscribe( address );
  }

  @Test
  public void convergeSubscription()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );

    final ChannelAddress address = new ChannelAddress( TestSystemA.A );
    final Channel channel = Channel.create( address, null );
    final AreaOfInterest areaOfInterest = AreaOfInterest.create( channel );

    final ContextConverger c = new TestContextConverger( clientSystem );

    when( clientSystem.getDataLoaderService( address.getChannelType() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, address, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, address, null ) ).
      thenReturn( -1 );

    assertEquals( c.convergeAreaOfInterest( areaOfInterest, null, null, true ),
                  ContextConverger.ConvergeAction.SUBMITTED_ADD );

    verify( service ).requestSubscribe( address, null );
    verify( service, never() ).requestUnsubscribe( address );
    verify( service, never() ).requestSubscriptionUpdate( address, null );
  }

  @Test
  public void convergeSubscription_subscribedButRemovePending()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );

    final ChannelAddress address = new ChannelAddress( TestSystemA.A );
    final Channel channel = Channel.create( address, null );
    final AreaOfInterest areaOfInterest = AreaOfInterest.create( channel );

    final ContextConverger c = new TestContextConverger( clientSystem );

    when( clientSystem.getDataLoaderService( address.getChannelType() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.isSubscribed( address ) ).thenReturn( Boolean.TRUE );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, address, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, address, null ) ).
      thenReturn( 0 );

    assertEquals( c.convergeAreaOfInterest( areaOfInterest, null, null, true ),
                  ContextConverger.ConvergeAction.SUBMITTED_ADD );

    verify( service ).requestSubscribe( address, null );
    verify( service, never() ).requestUnsubscribe( address );
    verify( service, never() ).requestSubscriptionUpdate( address, null );
  }

  @Test
  public void convergeSubscription_subscribed()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );

    final ChannelAddress address = new ChannelAddress( TestSystemA.A );
    final Channel channel = Channel.create( address, null );
    final AreaOfInterest areaOfInterest = AreaOfInterest.create( channel );

    final ContextConverger c = new TestContextConverger( clientSystem );

    when( clientSystem.getDataLoaderService( address.getChannelType() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.isSubscribed( address ) ).thenReturn( Boolean.TRUE );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, address, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, address, null ) ).
      thenReturn( -1 );

    Replicant.context().createSubscription( address, null, false );

    assertEquals( c.convergeAreaOfInterest( areaOfInterest, null, null, true ),
                  ContextConverger.ConvergeAction.IN_PROGRESS );

    verify( service, never() ).requestSubscribe( address, null );
    verify( service, never() ).requestUnsubscribe( address );
    verify( service, never() ).requestSubscriptionUpdate( address, null );
  }

  @Test
  public void convergeSubscription_addPending()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );

    final ChannelAddress address = new ChannelAddress( TestSystemA.A );
    final Channel channel = Channel.create( address, null );
    final AreaOfInterest areaOfInterest = AreaOfInterest.create( channel );

    final ContextConverger c = new TestContextConverger( clientSystem );

    when( clientSystem.getDataLoaderService( address.getChannelType() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, address, null ) ).
      thenReturn( 1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, address, null ) ).
      thenReturn( -1 );

    assertEquals( c.convergeAreaOfInterest( areaOfInterest, null, null, true ),
                  ContextConverger.ConvergeAction.IN_PROGRESS );

    verify( service, never() ).requestSubscribe( address, null );
    verify( service, never() ).requestUnsubscribe( address );
    verify( service, never() ).requestSubscriptionUpdate( address, null );
  }

  @Test
  public void convergeSubscription_updatePending()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );

    final ChannelAddress address = new ChannelAddress( TestSystemA.A );
    final Channel channel = Channel.create( address, null );
    channel.setFilter( "Filter1" );
    final AreaOfInterest areaOfInterest = AreaOfInterest.create( channel );

    final ContextConverger c = new TestContextConverger( clientSystem );

    when( clientSystem.getDataLoaderService( address.getChannelType() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    Replicant.context().createSubscription( address, "Filter1", true );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, address, "Filter1" ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, address, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.UPDATE, address, "Filter1" ) ).
      thenReturn( 4 );

    assertEquals( c.convergeAreaOfInterest( areaOfInterest, null, null, true ),
                  ContextConverger.ConvergeAction.IN_PROGRESS );

    verify( service, never() ).requestSubscribe( address, "Filter1" );
    verify( service, never() ).requestUnsubscribe( address );
    verify( service, never() ).requestSubscriptionUpdate( address, "Filter1" );
  }

  @Test
  public void convergeSubscription_requestSubscriptionUpdate()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );

    final ChannelAddress address = new ChannelAddress( TestSystemA.A );
    final Channel channel = Channel.create( address, null );
    channel.setFilter( "Filter1" );
    final AreaOfInterest areaOfInterest = AreaOfInterest.create( channel );

    final ContextConverger c = new TestContextConverger( clientSystem );

    when( clientSystem.getDataLoaderService( address.getChannelType() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.isSubscribed( address ) ).thenReturn( Boolean.TRUE );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, address, "Filter1" ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, address, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.UPDATE, address, "Filter1" ) ).
      thenReturn( -1 );

    Replicant.context().createSubscription( address, "OldFIlter", false );

    assertEquals( c.convergeAreaOfInterest( areaOfInterest, null, null, true ),
                  ContextConverger.ConvergeAction.SUBMITTED_UPDATE );

    verify( service, never() ).requestSubscribe( address, "Filter1" );
    verify( service, never() ).requestUnsubscribe( address );
    verify( service ).requestSubscriptionUpdate( address, "Filter1" );
  }

  @Test
  public void canGroup()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );

    final ContextConverger c = new TestContextConverger( clientSystem );

    final ChannelAddress address = new ChannelAddress( TestSystemA.A );
    final Channel channel = Channel.create( address, null );
    final AreaOfInterest areaOfInterest = AreaOfInterest.create( channel );

    assertTrue( c.canGroup( areaOfInterest, AreaOfInterestAction.ADD, areaOfInterest, AreaOfInterestAction.ADD ) );
    assertFalse( c.canGroup( areaOfInterest, AreaOfInterestAction.ADD, areaOfInterest, AreaOfInterestAction.UPDATE ) );
    assertFalse( c.canGroup( areaOfInterest, AreaOfInterestAction.ADD, areaOfInterest, AreaOfInterestAction.REMOVE ) );
    assertFalse( c.canGroup( areaOfInterest, AreaOfInterestAction.UPDATE, areaOfInterest, AreaOfInterestAction.ADD ) );
    assertTrue( c.canGroup( areaOfInterest,
                            AreaOfInterestAction.UPDATE,
                            areaOfInterest,
                            AreaOfInterestAction.UPDATE ) );
    assertFalse( c.canGroup( areaOfInterest,
                             AreaOfInterestAction.UPDATE,
                             areaOfInterest,
                             AreaOfInterestAction.REMOVE ) );
    assertFalse( c.canGroup( areaOfInterest, AreaOfInterestAction.REMOVE, areaOfInterest, AreaOfInterestAction.ADD ) );
    assertFalse( c.canGroup( areaOfInterest,
                             AreaOfInterestAction.REMOVE,
                             areaOfInterest,
                             AreaOfInterestAction.UPDATE ) );
    assertTrue( c.canGroup( areaOfInterest,
                            AreaOfInterestAction.REMOVE,
                            areaOfInterest,
                            AreaOfInterestAction.REMOVE ) );

    final ChannelAddress channel2 = new ChannelAddress( TestSystemA.A, 2 );
    final AreaOfInterest areaOfInterest2 = AreaOfInterest.create( Channel.create( channel2, null ) );
    assertTrue( c.canGroup( areaOfInterest, AreaOfInterestAction.ADD, areaOfInterest2, AreaOfInterestAction.ADD ) );

    final ChannelAddress address3 = new ChannelAddress( TestSystemA.B, 1 );
    final Channel channel3 = Channel.create( address3, null );
    final AreaOfInterest areaOfInterest3 = AreaOfInterest.create( channel3 );
    assertFalse( c.canGroup( areaOfInterest, AreaOfInterestAction.ADD, areaOfInterest3, AreaOfInterestAction.ADD ) );
    assertFalse( c.canGroup( areaOfInterest, AreaOfInterestAction.ADD, areaOfInterest3, AreaOfInterestAction.UPDATE ) );
    assertFalse( c.canGroup( areaOfInterest, AreaOfInterestAction.ADD, areaOfInterest3, AreaOfInterestAction.REMOVE ) );

    final ChannelAddress descriptor4 = new ChannelAddress( TestSystemA.A, 1 );
    final Channel channel4 = Channel.create( descriptor4, null );
    channel4.setFilter( "Filter" );
    final AreaOfInterest areaOfInterest4 = AreaOfInterest.create( channel4 );
    assertFalse( c.canGroup( areaOfInterest, AreaOfInterestAction.ADD, areaOfInterest4, AreaOfInterestAction.ADD ) );
    areaOfInterest.getChannel().setFilter( "Filter" );
    assertTrue( c.canGroup( areaOfInterest, AreaOfInterestAction.ADD, areaOfInterest4, AreaOfInterestAction.ADD ) );
  }

  @Test
  public void convergeSubscription_inactiveSubscription()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );

    final ChannelAddress address = new ChannelAddress( TestSystemA.A, 1 );
    final Channel channel = Channel.create( address, null );
    final AreaOfInterest areaOfInterest = AreaOfInterest.create( channel );
    Disposable.dispose( areaOfInterest );

    final ContextConverger c = new TestContextConverger( clientSystem );

    when( clientSystem.getDataLoaderService( address.getChannelType() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    assertEquals( c.convergeAreaOfInterest( areaOfInterest, null, null, true ),
                  ContextConverger.ConvergeAction.NO_ACTION );
    verify( service, never() ).requestSubscribe( address, null );
    verify( service, never() ).requestUnsubscribe( address );
    verify( service, never() ).requestSubscriptionUpdate( address, null );
  }

  @Test
  public void convergeSubscription_groupWithAdd()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );

    final ChannelAddress address = new ChannelAddress( TestSystemA.A, 1 );
    final Channel channel1 = Channel.create( address, null );
    final AreaOfInterest areaOfInterest1 = AreaOfInterest.create( channel1 );

    final ChannelAddress address2 = new ChannelAddress( TestSystemA.A, 2 );
    final Channel channel2 = Channel.create( address2, null );
    final AreaOfInterest areaOfInterest2 = AreaOfInterest.create( channel2 );

    final ChannelAddress address3 = new ChannelAddress( TestSystemA.A, 3 );
    final Channel channel3 = Channel.create( address3, null );
    final AreaOfInterest areaOfInterest3 = AreaOfInterest.create( channel3 );

    final ContextConverger c = new TestContextConverger( clientSystem );

    when( clientSystem.getDataLoaderService( address.getChannelType() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, address2, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, address2, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, address3, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, address3, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, address3, "Filter" ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, address3, "Filter" ) ).
      thenReturn( -1 );

    assertEquals( c.convergeAreaOfInterest( areaOfInterest2, areaOfInterest1, AreaOfInterestAction.ADD, true ),
                  ContextConverger.ConvergeAction.SUBMITTED_ADD );
    verify( service ).requestSubscribe( address2, null );
    verify( service, never() ).requestUnsubscribe( address2 );
    verify( service, never() ).requestSubscriptionUpdate( address2, null );

    assertEquals( c.convergeAreaOfInterest( areaOfInterest3, areaOfInterest1, AreaOfInterestAction.ADD, false ),
                  ContextConverger.ConvergeAction.TERMINATE );
    verify( service, never() ).requestSubscribe( address3, null );
    verify( service, never() ).requestUnsubscribe( address3 );
    verify( service, never() ).requestSubscriptionUpdate( address3, null );

    areaOfInterest3.getChannel().setFilter( "Filter" );
    assertEquals( c.convergeAreaOfInterest( areaOfInterest3, areaOfInterest1, AreaOfInterestAction.ADD, true ),
                  ContextConverger.ConvergeAction.NO_ACTION );
    verify( service, never() ).requestSubscribe( address3, null );
    verify( service, never() ).requestUnsubscribe( address3 );
    verify( service, never() ).requestSubscriptionUpdate( address3, null );
  }

  @Test
  public void convergeSubscription_groupWithUpdate()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );

    final ChannelAddress address = new ChannelAddress( TestSystemA.A, 1 );
    final Channel channel1 = Channel.create( address, null );
    final AreaOfInterest areaOfInterest1 = AreaOfInterest.create( channel1 );

    final ChannelAddress address2 = new ChannelAddress( TestSystemA.A, 2 );
    final Channel channel2 = Channel.create( address2, null );
    final AreaOfInterest areaOfInterest2 = AreaOfInterest.create( channel2 );

    final ChannelAddress address3 = new ChannelAddress( TestSystemA.A, 3 );
    final Channel channel3 = Channel.create( address3, null );
    final AreaOfInterest areaOfInterest3 = AreaOfInterest.create( channel3 );

    final ContextConverger c = new TestContextConverger( clientSystem );

    when( clientSystem.getDataLoaderService( address.getChannelType() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.isSubscribed( address2 ) ).thenReturn( Boolean.TRUE );
    when( service.isSubscribed( address3 ) ).thenReturn( Boolean.TRUE );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, address2, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, address2, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.UPDATE, address2, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, address3, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, address3, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.UPDATE, address3, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, address3, "Filter" ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, address3, "Filter" ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.UPDATE, address3, "Filter" ) ).
      thenReturn( -1 );

    Replicant.context().createSubscription( address2, "OldFilter", true );
    Replicant.context().createSubscription( address3, "OldFilter", true );

    assertEquals( c.convergeAreaOfInterest( areaOfInterest2, areaOfInterest1, AreaOfInterestAction.UPDATE, true ),
                  ContextConverger.ConvergeAction.SUBMITTED_UPDATE );
    verify( service, never() ).requestSubscribe( address2, null );
    verify( service, never() ).requestUnsubscribe( address2 );
    verify( service ).requestSubscriptionUpdate( address2, null );

    assertEquals( c.convergeAreaOfInterest( areaOfInterest3, areaOfInterest1, AreaOfInterestAction.UPDATE, false ),
                  ContextConverger.ConvergeAction.TERMINATE );
    verify( service, never() ).requestSubscribe( address3, null );
    verify( service, never() ).requestUnsubscribe( address3 );
    verify( service, never() ).requestSubscriptionUpdate( address3, null );

    areaOfInterest3.getChannel().setFilter( "Filter" );
    assertEquals( c.convergeAreaOfInterest( areaOfInterest3, areaOfInterest1, AreaOfInterestAction.UPDATE, true ),
                  ContextConverger.ConvergeAction.NO_ACTION );
    verify( service, never() ).requestSubscribe( address3, null );
    verify( service, never() ).requestUnsubscribe( address3 );
    verify( service, never() ).requestSubscriptionUpdate( address3, null );
  }

  @Test
  public void isIdle()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service1 = mock( DataLoaderService.class );
    final DataLoaderService service2 = mock( DataLoaderService.class );

    when( clientSystem.getState() ).thenReturn( ReplicantClientSystem.State.CONNECTED );
    when( service1.getState() ).thenReturn( DataLoaderService.State.CONNECTED );
    when( service2.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    final List<DataLoaderEntry> dataLoaders = new ArrayList<>( 2 );
    dataLoaders.add( new DataLoaderEntry( service1, true ) );
    dataLoaders.add( new DataLoaderEntry( service2, true ) );

    when( clientSystem.getDataLoaders() ).thenReturn( dataLoaders );

    final ContextConverger c = new TestContextConverger( clientSystem );

    when( service1.isIdle() ).thenReturn( true );
    when( service2.isIdle() ).thenReturn( true );

    // Convergers are not idle until they have attempted to converge at least once
    assertFalse( c.isIdle() );
    c.convergeStep();
    assertTrue( c.isIdle() );
    when( service1.isIdle() ).thenReturn( false );
    when( service2.isIdle() ).thenReturn( false );
    assertFalse( c.isIdle() );
    when( service2.isIdle() ).thenReturn( true );
    assertFalse( c.isIdle() );
    when( service1.isIdle() ).thenReturn( true );
    assertTrue( c.isIdle() );
  }
}
