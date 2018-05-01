package org.realityforge.replicant.client.converger;

import arez.Arez;
import arez.Disposable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.realityforge.replicant.client.AbstractReplicantTest;
import org.realityforge.replicant.client.Channel;
import org.realityforge.replicant.client.ChannelAddress;
import org.realityforge.replicant.client.aoi.AreaOfInterest;
import org.realityforge.replicant.client.aoi.AreaOfInterestService;
import org.realityforge.replicant.client.runtime.DataLoaderEntry;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;
import org.realityforge.replicant.client.subscription.ChannelSubscriptionEntry;
import org.realityforge.replicant.client.subscription.EntitySubscriptionManager;
import org.realityforge.replicant.client.transport.AreaOfInterestAction;
import org.realityforge.replicant.client.transport.DataLoaderService;
import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.ITestResult;
import org.testng.annotations.Test;
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

    final EntitySubscriptionManager subscriptionManager = EntitySubscriptionManager.create();
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final ContextConverger c = new TestContextConverger( subscriptionManager, areaOfInterestService, system );

    c.addListeners();

    verify( dl1 ).addDataLoaderListener( any( ContextConverger.ConvergerDataLoaderListener.class ) );
    verify( dl2 ).addDataLoaderListener( any( ContextConverger.ConvergerDataLoaderListener.class ) );
    verify( dl3 ).addDataLoaderListener( any( ContextConverger.ConvergerDataLoaderListener.class ) );

    verify( system ).addReplicantSystemListener( any( ContextConverger.ConvergerReplicantSystemListener.class ) );

    c.removeListeners();

    verify( dl1 ).removeDataLoaderListener( any( ContextConverger.ConvergerDataLoaderListener.class ) );
    verify( dl2 ).removeDataLoaderListener( any( ContextConverger.ConvergerDataLoaderListener.class ) );
    verify( dl3 ).removeDataLoaderListener( any( ContextConverger.ConvergerDataLoaderListener.class ) );

    verify( system ).
      removeReplicantSystemListener( any( ContextConverger.ConvergerReplicantSystemListener.class ) );

    c.release();

    verify( dl1, times( 2 ) ).removeDataLoaderListener( any( ContextConverger.ConvergerDataLoaderListener.class ) );
    verify( dl2, times( 2 ) ).removeDataLoaderListener( any( ContextConverger.ConvergerDataLoaderListener.class ) );
    verify( dl3, times( 2 ) ).removeDataLoaderListener( any( ContextConverger.ConvergerDataLoaderListener.class ) );

    verify( system, times( 2 ) ).
      removeReplicantSystemListener( any( ContextConverger.ConvergerReplicantSystemListener.class ) );
  }

  @Test
  public void preConvergeAction()
  {
    final ContextConverger c =
      new TestContextConverger( mock( EntitySubscriptionManager.class ),
                                mock( AreaOfInterestService.class ),
                                mock( ReplicantClientSystem.class ) );

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
    final ChannelAddress descriptor = new ChannelAddress( TestSystemA.A );

    final ContextConverger c =
      new TestContextConverger( mock( EntitySubscriptionManager.class ),
                                mock( AreaOfInterestService.class ),
                                clientSystem );

    when( clientSystem.getDataLoaderService( TestSystemA.A ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, descriptor, null ) ).
      thenReturn( -1 );

    c.removeOrphanSubscription( descriptor );

    verify( service ).requestUnsubscribe( descriptor );
  }

  @Test
  public void removeOrphanSubscription_DISCONNECTED()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );
    final ChannelAddress descriptor = new ChannelAddress( TestSystemA.A );

    final ContextConverger c =
      new TestContextConverger( mock( EntitySubscriptionManager.class ),
                                mock( AreaOfInterestService.class ),
                                clientSystem );

    when( clientSystem.getDataLoaderService( TestSystemA.A ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.DISCONNECTED );

    c.removeOrphanSubscription( descriptor );

    verify( service, never() ).requestUnsubscribe( descriptor );
  }

  @Test
  public void removeOrphanSubscription_AOI_Pending()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );
    final ChannelAddress descriptor = new ChannelAddress( TestSystemA.A );

    final ContextConverger c =
      new TestContextConverger( mock( EntitySubscriptionManager.class ),
                                mock( AreaOfInterestService.class ),
                                clientSystem );

    when( clientSystem.getDataLoaderService( TestSystemA.A ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.isAreaOfInterestActionPending( AreaOfInterestAction.REMOVE, descriptor, null ) ).
      thenReturn( Boolean.TRUE );

    c.removeOrphanSubscription( descriptor );

    verify( service, never() ).requestUnsubscribe( descriptor );
  }

  @Test
  public void removeSubscriptionIfOrphan()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );
    final ChannelAddress descriptor = new ChannelAddress( TestSystemA.A );

    final EntitySubscriptionManager subscriptionManager = mock( EntitySubscriptionManager.class );

    final ContextConverger c =
      new TestContextConverger( subscriptionManager,
                                mock( AreaOfInterestService.class ),
                                clientSystem );

    when( clientSystem.getDataLoaderService( TestSystemA.A ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, descriptor, null ) ).
      thenReturn( -1 );

    final HashSet<ChannelAddress> expected = new HashSet<>();

    when( subscriptionManager.getChannelSubscription( descriptor ) ).
      thenReturn( ChannelSubscriptionEntry.create( Channel.create( descriptor, null ), true ) );

    c.removeSubscriptionIfOrphan( expected, descriptor );

    verify( service ).requestUnsubscribe( descriptor );
  }

  @Test
  public void removeSubscriptionIfOrphan_expected()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final ChannelAddress descriptor = new ChannelAddress( TestSystemA.A );
    final HashSet<ChannelAddress> expected = new HashSet<>();
    expected.add( descriptor );

    final ContextConverger c =
      new TestContextConverger( mock( EntitySubscriptionManager.class ),
                                mock( AreaOfInterestService.class ),
                                clientSystem );

    c.removeSubscriptionIfOrphan( expected, descriptor );

    verify( clientSystem, never() ).getDataLoaderService( TestSystemA.A );
  }

  @Test
  public void removeSubscriptionIfOrphan_notExplicit()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final ChannelAddress descriptor = new ChannelAddress( TestSystemA.A );

    final EntitySubscriptionManager subscriptionManager = mock( EntitySubscriptionManager.class );

    final ContextConverger c =
      new TestContextConverger( subscriptionManager,
                                mock( AreaOfInterestService.class ),
                                clientSystem );

    final HashSet<ChannelAddress> expected = new HashSet<>();

    when( subscriptionManager.getChannelSubscription( descriptor ) ).
      thenReturn( ChannelSubscriptionEntry.create( Channel.create( descriptor, null ), false ) );

    c.removeSubscriptionIfOrphan( expected, descriptor );

    verify( clientSystem, never() ).getDataLoaderService( TestSystemA.A );
  }

  @Test
  public void removeOrphanSubscriptions()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );
    final ChannelAddress descriptor = new ChannelAddress( TestSystemA.A );

    final HashSet<ChannelAddress> expected = new HashSet<>();
    expected.add( new ChannelAddress( TestSystemA.B, 1 ) );
    expected.add( new ChannelAddress( TestSystemA.B, 2 ) );
    expected.add( new ChannelAddress( TestSystemA.B, 3 ) );
    expected.add( new ChannelAddress( TestSystemA.B, 4 ) );
    expected.add( new ChannelAddress( TestSystemA.B, 5 ) );

    final EntitySubscriptionManager subscriptionManager = mock( EntitySubscriptionManager.class );

    final ContextConverger c =
      new TestContextConverger( subscriptionManager,
                                mock( AreaOfInterestService.class ),
                                clientSystem );

    when( clientSystem.getDataLoaderService( TestSystemA.A ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, descriptor, null ) ).
      thenReturn( -1 );

    final ChannelSubscriptionEntry entry = ChannelSubscriptionEntry.create( Channel.create( descriptor, null ), true );
    when( subscriptionManager.getChannelSubscription( descriptor ) ).
      thenReturn( entry );

    when( subscriptionManager.getTypeChannelSubscriptions() ).thenReturn( Collections.singleton( entry ) );
    when( subscriptionManager.getInstanceChannelSubscriptionKeys() ).thenReturn( Collections.singleton( TestSystemA.B ) );
    when( subscriptionManager.getInstanceChannelSubscriptions( TestSystemA.B ) ).
      thenReturn( new HashSet<>( Arrays.asList( 1, 2, 3, 4, 5 ) ) );

    c.removeOrphanSubscriptions( expected );

    verify( service ).requestUnsubscribe( descriptor );
  }

  @Test
  public void convergeSubscription()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );
    final EntitySubscriptionManager subscriptionManager = mock( EntitySubscriptionManager.class );

    final ChannelAddress descriptor = new ChannelAddress( TestSystemA.A );
    final Channel channel = Channel.create( descriptor, null );
    final AreaOfInterest areaOfInterest = AreaOfInterest.create( channel );

    final ContextConverger c =
      new TestContextConverger( subscriptionManager, areaOfInterestService, clientSystem );

    when( clientSystem.getDataLoaderService( descriptor.getChannelType() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.isSubscribed( descriptor ) ).thenReturn( Boolean.FALSE );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, descriptor, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, descriptor, null ) ).
      thenReturn( -1 );

    assertEquals( c.convergeAreaOfInterest( areaOfInterest, null, null, true ),
                  ContextConverger.ConvergeAction.SUBMITTED_ADD );

    verify( service ).requestSubscribe( descriptor, null );
    verify( service, never() ).requestUnsubscribe( descriptor );
    verify( service, never() ).requestSubscriptionUpdate( descriptor, null );
  }

  @Test
  public void convergeSubscription_subscribedButRemovePending()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );
    final EntitySubscriptionManager subscriptionManager = mock( EntitySubscriptionManager.class );

    final ChannelAddress descriptor = new ChannelAddress( TestSystemA.A );
    final Channel channel = Channel.create( descriptor, null );
    final AreaOfInterest areaOfInterest = AreaOfInterest.create( channel );

    final ContextConverger c =
      new TestContextConverger( subscriptionManager, areaOfInterestService, clientSystem );

    when( clientSystem.getDataLoaderService( descriptor.getChannelType() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.isSubscribed( descriptor ) ).thenReturn( Boolean.TRUE );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, descriptor, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, descriptor, null ) ).
      thenReturn( 0 );

    assertEquals( c.convergeAreaOfInterest( areaOfInterest, null, null, true ),
                  ContextConverger.ConvergeAction.SUBMITTED_ADD );

    verify( service ).requestSubscribe( descriptor, null );
    verify( service, never() ).requestUnsubscribe( descriptor );
    verify( service, never() ).requestSubscriptionUpdate( descriptor, null );
  }

  @Test
  public void convergeSubscription_subscribed()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );
    final EntitySubscriptionManager subscriptionManager = mock( EntitySubscriptionManager.class );

    final ChannelAddress descriptor = new ChannelAddress( TestSystemA.A );
    final Channel channel = Channel.create( descriptor, null );
    final AreaOfInterest areaOfInterest = AreaOfInterest.create( channel );

    final ContextConverger c =
      new TestContextConverger( subscriptionManager, areaOfInterestService, clientSystem );

    when( clientSystem.getDataLoaderService( descriptor.getChannelType() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.isSubscribed( descriptor ) ).thenReturn( Boolean.TRUE );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, descriptor, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, descriptor, null ) ).
      thenReturn( -1 );

    when( subscriptionManager.getChannelSubscription( descriptor ) ).
      thenReturn( ChannelSubscriptionEntry.create( Channel.create( descriptor, null ), false ) );

    assertEquals( c.convergeAreaOfInterest( areaOfInterest, null, null, true ),
                  ContextConverger.ConvergeAction.IN_PROGRESS );

    verify( service, never() ).requestSubscribe( descriptor, null );
    verify( service, never() ).requestUnsubscribe( descriptor );
    verify( service, never() ).requestSubscriptionUpdate( descriptor, null );
  }

  @Test
  public void convergeSubscription_addPending()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );
    final EntitySubscriptionManager subscriptionManager = mock( EntitySubscriptionManager.class );

    final ChannelAddress descriptor = new ChannelAddress( TestSystemA.A );
    final Channel channel = Channel.create( descriptor, null );
    final AreaOfInterest areaOfInterest = AreaOfInterest.create( channel );

    final ContextConverger c =
      new TestContextConverger( subscriptionManager, areaOfInterestService, clientSystem );

    when( clientSystem.getDataLoaderService( descriptor.getChannelType() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.isSubscribed( descriptor ) ).thenReturn( Boolean.FALSE );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, descriptor, null ) ).
      thenReturn( 1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, descriptor, null ) ).
      thenReturn( -1 );

    assertEquals( c.convergeAreaOfInterest( areaOfInterest, null, null, true ),
                  ContextConverger.ConvergeAction.IN_PROGRESS );

    verify( service, never() ).requestSubscribe( descriptor, null );
    verify( service, never() ).requestUnsubscribe( descriptor );
    verify( service, never() ).requestSubscriptionUpdate( descriptor, null );
  }

  @Test
  public void convergeSubscription_updatePending()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );
    final EntitySubscriptionManager subscriptionManager = mock( EntitySubscriptionManager.class );

    final ChannelAddress descriptor = new ChannelAddress( TestSystemA.A );
    final Channel channel = Channel.create( descriptor, null );
    channel.setFilter( "Filter1" );
    final AreaOfInterest areaOfInterest = AreaOfInterest.create( channel );

    final ContextConverger c =
      new TestContextConverger( subscriptionManager, areaOfInterestService, clientSystem );

    when( clientSystem.getDataLoaderService( descriptor.getChannelType() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.isSubscribed( descriptor ) ).thenReturn( Boolean.TRUE );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, descriptor, "Filter1" ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, descriptor, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.UPDATE, descriptor, "Filter1" ) ).
      thenReturn( 4 );

    assertEquals( c.convergeAreaOfInterest( areaOfInterest, null, null, true ),
                  ContextConverger.ConvergeAction.IN_PROGRESS );

    verify( service, never() ).requestSubscribe( descriptor, "Filter1" );
    verify( service, never() ).requestUnsubscribe( descriptor );
    verify( service, never() ).requestSubscriptionUpdate( descriptor, "Filter1" );
  }

  @Test
  public void convergeSubscription_requestSubscriptionUpdate()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );
    final EntitySubscriptionManager subscriptionManager = mock( EntitySubscriptionManager.class );

    final ChannelAddress descriptor = new ChannelAddress( TestSystemA.A );
    final Channel channel = Channel.create( descriptor, null );
    channel.setFilter( "Filter1" );
    final AreaOfInterest areaOfInterest = AreaOfInterest.create( channel );

    final ContextConverger c =
      new TestContextConverger( subscriptionManager, areaOfInterestService, clientSystem );

    when( clientSystem.getDataLoaderService( descriptor.getChannelType() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.isSubscribed( descriptor ) ).thenReturn( Boolean.TRUE );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, descriptor, "Filter1" ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, descriptor, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.UPDATE, descriptor, "Filter1" ) ).
      thenReturn( -1 );

    when( subscriptionManager.getChannelSubscription( descriptor ) ).
      thenReturn( ChannelSubscriptionEntry.create( Channel.create( descriptor, "OldFIlter" ), true ) );

    assertEquals( c.convergeAreaOfInterest( areaOfInterest, null, null, true ),
                  ContextConverger.ConvergeAction.SUBMITTED_UPDATE );

    verify( service, never() ).requestSubscribe( descriptor, "Filter1" );
    verify( service, never() ).requestUnsubscribe( descriptor );
    verify( service ).requestSubscriptionUpdate( descriptor, "Filter1" );
  }

  @Test
  public void canGroup()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final EntitySubscriptionManager subscriptionManager = mock( EntitySubscriptionManager.class );

    final ContextConverger c =
      new TestContextConverger( subscriptionManager, areaOfInterestService, clientSystem );

    final ChannelAddress descriptor = new ChannelAddress( TestSystemA.A );
    final Channel channel = Channel.create( descriptor, null );
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

    final ChannelAddress descriptor3 = new ChannelAddress( TestSystemA.B, 1 );
    final Channel channel3 = Channel.create( descriptor3, null );
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
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );
    final EntitySubscriptionManager subscriptionManager = mock( EntitySubscriptionManager.class );

    final ChannelAddress descriptor = new ChannelAddress( TestSystemA.A, 1 );
    final Channel channel = Channel.create( descriptor, null );
    final AreaOfInterest areaOfInterest = AreaOfInterest.create( channel );
    Disposable.dispose( areaOfInterest );

    final ContextConverger c =
      new TestContextConverger( subscriptionManager, areaOfInterestService, clientSystem );

    when( clientSystem.getDataLoaderService( descriptor.getChannelType() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    assertEquals( c.convergeAreaOfInterest( areaOfInterest, null, null, true ),
                  ContextConverger.ConvergeAction.NO_ACTION );
    verify( service, never() ).requestSubscribe( descriptor, null );
    verify( service, never() ).requestUnsubscribe( descriptor );
    verify( service, never() ).requestSubscriptionUpdate( descriptor, null );
  }

  @Test
  public void convergeSubscription_groupWithAdd()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );
    final EntitySubscriptionManager subscriptionManager = mock( EntitySubscriptionManager.class );

    final ChannelAddress descriptor = new ChannelAddress( TestSystemA.A, 1 );
    final Channel channel1 = Channel.create( descriptor, null );
    final AreaOfInterest areaOfInterest1 = AreaOfInterest.create( channel1 );

    final ChannelAddress descriptor2 = new ChannelAddress( TestSystemA.A, 2 );
    final Channel channel2 = Channel.create( descriptor2, null );
    final AreaOfInterest areaOfInterest2 = AreaOfInterest.create( channel2 );

    final ChannelAddress descriptor3 = new ChannelAddress( TestSystemA.A, 3 );
    final Channel channel3 = Channel.create( descriptor3, null );
    final AreaOfInterest areaOfInterest3 = AreaOfInterest.create( channel3 );

    final ContextConverger c =
      new TestContextConverger( subscriptionManager, areaOfInterestService, clientSystem );

    when( clientSystem.getDataLoaderService( descriptor.getChannelType() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.isSubscribed( descriptor2 ) ).thenReturn( Boolean.FALSE );
    when( service.isSubscribed( descriptor3 ) ).thenReturn( Boolean.FALSE );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, descriptor2, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, descriptor2, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, descriptor3, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, descriptor3, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, descriptor3, "Filter" ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, descriptor3, "Filter" ) ).
      thenReturn( -1 );

    assertEquals( c.convergeAreaOfInterest( areaOfInterest2, areaOfInterest1, AreaOfInterestAction.ADD, true ),
                  ContextConverger.ConvergeAction.SUBMITTED_ADD );
    verify( service ).requestSubscribe( descriptor2, null );
    verify( service, never() ).requestUnsubscribe( descriptor2 );
    verify( service, never() ).requestSubscriptionUpdate( descriptor2, null );

    assertEquals( c.convergeAreaOfInterest( areaOfInterest3, areaOfInterest1, AreaOfInterestAction.ADD, false ),
                  ContextConverger.ConvergeAction.TERMINATE );
    verify( service, never() ).requestSubscribe( descriptor3, null );
    verify( service, never() ).requestUnsubscribe( descriptor3 );
    verify( service, never() ).requestSubscriptionUpdate( descriptor3, null );

    areaOfInterest3.getChannel().setFilter( "Filter" );
    assertEquals( c.convergeAreaOfInterest( areaOfInterest3, areaOfInterest1, AreaOfInterestAction.ADD, true ),
                  ContextConverger.ConvergeAction.NO_ACTION );
    verify( service, never() ).requestSubscribe( descriptor3, null );
    verify( service, never() ).requestUnsubscribe( descriptor3 );
    verify( service, never() ).requestSubscriptionUpdate( descriptor3, null );
  }

  @Test
  public void convergeSubscription_groupWithUpdate()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );
    final EntitySubscriptionManager subscriptionManager = mock( EntitySubscriptionManager.class );

    final ChannelAddress descriptor = new ChannelAddress( TestSystemA.A, 1 );
    final Channel channel1 = Channel.create( descriptor, null );
    final AreaOfInterest areaOfInterest1 = AreaOfInterest.create( channel1 );

    final ChannelAddress descriptor2 = new ChannelAddress( TestSystemA.A, 2 );
    final Channel channel2 = Channel.create( descriptor2, null );
    final AreaOfInterest areaOfInterest2 = AreaOfInterest.create( channel2 );

    final ChannelAddress descriptor3 = new ChannelAddress( TestSystemA.A, 3 );
    final Channel channel3 = Channel.create( descriptor3, null );
    final AreaOfInterest areaOfInterest3 = AreaOfInterest.create( channel3 );

    final ContextConverger c =
      new TestContextConverger( subscriptionManager, areaOfInterestService, clientSystem );

    when( clientSystem.getDataLoaderService( descriptor.getChannelType() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.isSubscribed( descriptor2 ) ).thenReturn( Boolean.TRUE );
    when( service.isSubscribed( descriptor3 ) ).thenReturn( Boolean.TRUE );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, descriptor2, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, descriptor2, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.UPDATE, descriptor2, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, descriptor3, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, descriptor3, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.UPDATE, descriptor3, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, descriptor3, "Filter" ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, descriptor3, "Filter" ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.UPDATE, descriptor3, "Filter" ) ).
      thenReturn( -1 );

    when( subscriptionManager.getChannelSubscription( descriptor2 ) ).
      thenReturn( ChannelSubscriptionEntry.create( Channel.create( descriptor2, "OldFilter" ), true ) );
    when( subscriptionManager.getChannelSubscription( descriptor3 ) ).
      thenReturn( ChannelSubscriptionEntry.create( Channel.create( descriptor3, "OldFilter" ), true ) );

    assertEquals( c.convergeAreaOfInterest( areaOfInterest2, areaOfInterest1, AreaOfInterestAction.UPDATE, true ),
                  ContextConverger.ConvergeAction.SUBMITTED_UPDATE );
    verify( service, never() ).requestSubscribe( descriptor2, null );
    verify( service, never() ).requestUnsubscribe( descriptor2 );
    verify( service ).requestSubscriptionUpdate( descriptor2, null );

    assertEquals( c.convergeAreaOfInterest( areaOfInterest3, areaOfInterest1, AreaOfInterestAction.UPDATE, false ),
                  ContextConverger.ConvergeAction.TERMINATE );
    verify( service, never() ).requestSubscribe( descriptor3, null );
    verify( service, never() ).requestUnsubscribe( descriptor3 );
    verify( service, never() ).requestSubscriptionUpdate( descriptor3, null );

    areaOfInterest3.getChannel().setFilter( "Filter" );
    assertEquals( c.convergeAreaOfInterest( areaOfInterest3, areaOfInterest1, AreaOfInterestAction.UPDATE, true ),
                  ContextConverger.ConvergeAction.NO_ACTION );
    verify( service, never() ).requestSubscribe( descriptor3, null );
    verify( service, never() ).requestUnsubscribe( descriptor3 );
    verify( service, never() ).requestSubscriptionUpdate( descriptor3, null );
  }

  @Test
  public void isIdle()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service1 = mock( DataLoaderService.class );
    final DataLoaderService service2 = mock( DataLoaderService.class );
    final EntitySubscriptionManager subscriptionManager = mock( EntitySubscriptionManager.class );

    when( clientSystem.getState() ).thenReturn( ReplicantClientSystem.State.CONNECTED );
    when( service1.getState() ).thenReturn( DataLoaderService.State.CONNECTED );
    when( service2.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    final List<DataLoaderEntry> dataLoaders = new ArrayList<>( 2 );
    dataLoaders.add( new DataLoaderEntry( service1, true ) );
    dataLoaders.add( new DataLoaderEntry( service2, true ) );

    when( clientSystem.getDataLoaders() ).thenReturn( dataLoaders );

    final ContextConverger c =
      new TestContextConverger( subscriptionManager, areaOfInterestService, clientSystem );

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
