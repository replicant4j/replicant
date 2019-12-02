package org.realityforge.replicant.client.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.realityforge.replicant.client.ChannelAddress;
import org.realityforge.replicant.client.ChannelSubscriptionEntry;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.client.EntitySubscriptionManagerImpl;
import org.realityforge.replicant.client.transport.AreaOfInterestAction;
import org.realityforge.replicant.client.transport.DataLoaderService;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ContextConvergerImplTest
{
  private enum TestGraphA
  {
    A, B
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

    final EntitySubscriptionManagerImpl subscriptionManager = new EntitySubscriptionManagerImpl();
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final ContextConvergerImpl c = new TestContextConvergerImpl( subscriptionManager, areaOfInterestService, system );

    c.addListeners();

    verify( dl1 ).addDataLoaderListener( any( ContextConvergerImpl.ConvergerDataLoaderListener.class ) );
    verify( dl2 ).addDataLoaderListener( any( ContextConvergerImpl.ConvergerDataLoaderListener.class ) );
    verify( dl3 ).addDataLoaderListener( any( ContextConvergerImpl.ConvergerDataLoaderListener.class ) );

    verify( system ).addReplicantSystemListener( any( ContextConvergerImpl.ConvergerReplicantSystemListener.class ) );
    verify( areaOfInterestService ).
      addAreaOfInterestListener( any( ContextConvergerImpl.ConvergerAreaOfInterestListener.class ) );

    c.removeListeners();

    verify( dl1 ).removeDataLoaderListener( any( ContextConvergerImpl.ConvergerDataLoaderListener.class ) );
    verify( dl2 ).removeDataLoaderListener( any( ContextConvergerImpl.ConvergerDataLoaderListener.class ) );
    verify( dl3 ).removeDataLoaderListener( any( ContextConvergerImpl.ConvergerDataLoaderListener.class ) );

    verify( system ).
      removeReplicantSystemListener( any( ContextConvergerImpl.ConvergerReplicantSystemListener.class ) );
    verify( areaOfInterestService ).
      removeAreaOfInterestListener( any( ContextConvergerImpl.ConvergerAreaOfInterestListener.class ) );

    c.release();

    verify( dl1, times( 2 ) ).removeDataLoaderListener( any( ContextConvergerImpl.ConvergerDataLoaderListener.class ) );
    verify( dl2, times( 2 ) ).removeDataLoaderListener( any( ContextConvergerImpl.ConvergerDataLoaderListener.class ) );
    verify( dl3, times( 2 ) ).removeDataLoaderListener( any( ContextConvergerImpl.ConvergerDataLoaderListener.class ) );

    verify( system, times( 2 ) ).
      removeReplicantSystemListener( any( ContextConvergerImpl.ConvergerReplicantSystemListener.class ) );
    verify( areaOfInterestService, times( 2 ) ).
      removeAreaOfInterestListener( any( ContextConvergerImpl.ConvergerAreaOfInterestListener.class ) );
  }

  @Test
  public void preConvergeAction()
  {
    final ContextConvergerImpl c =
      new TestContextConvergerImpl( mock( EntitySubscriptionManager.class ),
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
    final ChannelAddress descriptor = new ChannelAddress( TestGraphA.A );

    final ContextConvergerImpl c =
      new TestContextConvergerImpl( mock( EntitySubscriptionManager.class ),
                                    mock( AreaOfInterestService.class ),
                                    clientSystem );

    when( clientSystem.getDataLoaderService( TestGraphA.A ) ).
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
    final ChannelAddress descriptor = new ChannelAddress( TestGraphA.A );

    final ContextConvergerImpl c =
      new TestContextConvergerImpl( mock( EntitySubscriptionManager.class ),
                                    mock( AreaOfInterestService.class ),
                                    clientSystem );

    when( clientSystem.getDataLoaderService( TestGraphA.A ) ).
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
    final ChannelAddress descriptor = new ChannelAddress( TestGraphA.A );

    final ContextConvergerImpl c =
      new TestContextConvergerImpl( mock( EntitySubscriptionManager.class ),
                                    mock( AreaOfInterestService.class ),
                                    clientSystem );

    when( clientSystem.getDataLoaderService( TestGraphA.A ) ).
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
    final ChannelAddress descriptor = new ChannelAddress( TestGraphA.A );

    final EntitySubscriptionManager subscriptionManager = mock( EntitySubscriptionManager.class );

    final ContextConvergerImpl c =
      new TestContextConvergerImpl( subscriptionManager,
                                    mock( AreaOfInterestService.class ),
                                    clientSystem );

    when( clientSystem.getDataLoaderService( TestGraphA.A ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, descriptor, null ) ).
      thenReturn( -1 );

    final HashSet<ChannelAddress> expected = new HashSet<>();

    when( subscriptionManager.getSubscription( descriptor ) ).
      thenReturn( new ChannelSubscriptionEntry( descriptor, null, true ) );

    c.removeSubscriptionIfOrphan( expected, descriptor );

    verify( service ).requestUnsubscribe( descriptor );
  }

  @Test
  public void removeSubscriptionIfOrphan_expected()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final ChannelAddress descriptor = new ChannelAddress( TestGraphA.A );
    final HashSet<ChannelAddress> expected = new HashSet<>();
    expected.add( descriptor );

    final ContextConvergerImpl c =
      new TestContextConvergerImpl( mock( EntitySubscriptionManager.class ),
                                    mock( AreaOfInterestService.class ),
                                    clientSystem );

    c.removeSubscriptionIfOrphan( expected, descriptor );

    verify( clientSystem, never() ).getDataLoaderService( TestGraphA.A );
  }

  @Test
  public void removeSubscriptionIfOrphan_notExplicit()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final ChannelAddress descriptor = new ChannelAddress( TestGraphA.A );

    final EntitySubscriptionManager subscriptionManager = mock( EntitySubscriptionManager.class );

    final ContextConvergerImpl c =
      new TestContextConvergerImpl( subscriptionManager,
                                    mock( AreaOfInterestService.class ),
                                    clientSystem );

    final HashSet<ChannelAddress> expected = new HashSet<>();

    when( subscriptionManager.getSubscription( descriptor ) ).
      thenReturn( new ChannelSubscriptionEntry( descriptor, null, false ) );

    c.removeSubscriptionIfOrphan( expected, descriptor );

    verify( clientSystem, never() ).getDataLoaderService( TestGraphA.A );
  }

  @Test
  public void removeOrphanSubscriptions()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );
    final ChannelAddress descriptor = new ChannelAddress( TestGraphA.A );

    final HashSet<ChannelAddress> expected = new HashSet<>();
    expected.add( new ChannelAddress( TestGraphA.B, 1 ) );
    expected.add( new ChannelAddress( TestGraphA.B, 2 ) );
    expected.add( new ChannelAddress( TestGraphA.B, 3 ) );
    expected.add( new ChannelAddress( TestGraphA.B, 4 ) );
    expected.add( new ChannelAddress( TestGraphA.B, 5 ) );

    final EntitySubscriptionManager subscriptionManager = mock( EntitySubscriptionManager.class );

    final ContextConvergerImpl c =
      new TestContextConvergerImpl( subscriptionManager,
                                    mock( AreaOfInterestService.class ),
                                    clientSystem );

    when( clientSystem.getDataLoaderService( TestGraphA.A ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, descriptor, null ) ).
      thenReturn( -1 );

    when( subscriptionManager.getSubscription( descriptor ) ).
      thenReturn( new ChannelSubscriptionEntry( descriptor, null, true ) );

    when( subscriptionManager.getTypeSubscriptions() ).thenReturn( Collections.singleton( TestGraphA.A ) );
    when( subscriptionManager.getInstanceSubscriptionKeys() ).thenReturn( Collections.singleton( TestGraphA.B ) );
    when( subscriptionManager.getInstanceSubscriptions( TestGraphA.B ) ).
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
    final Set<ChannelAddress> expectedChannels = new HashSet<>();

    final ChannelAddress descriptor = new ChannelAddress( TestGraphA.A );
    final Subscription subscription = new Subscription( areaOfInterestService, descriptor );

    final ContextConvergerImpl c =
      new TestContextConvergerImpl( subscriptionManager, areaOfInterestService, clientSystem );

    when( clientSystem.getDataLoaderService( descriptor.getGraph() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.isSubscribed( descriptor ) ).thenReturn( Boolean.FALSE );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, descriptor, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, descriptor, null ) ).
      thenReturn( -1 );

    assertEquals( c.convergeSubscription( expectedChannels, subscription, null, null, true ),
                  ContextConvergerImpl.ConvergeAction.SUBMITTED_ADD );

    verify( service ).requestSubscribe( descriptor, null );
    verify( service, never() ).requestUnsubscribe( descriptor );
    verify( service, never() ).requestSubscriptionUpdate( descriptor, null );
    assertEquals( expectedChannels.size(), 0 );
  }

  @Test
  public void convergeChildSubscription()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );
    final EntitySubscriptionManager subscriptionManager = mock( EntitySubscriptionManager.class );

    final ChannelAddress descriptor = new ChannelAddress( TestGraphA.A );
    final Subscription subscription = new Subscription( areaOfInterestService, descriptor );
    final ChannelAddress descriptorB = new ChannelAddress( TestGraphA.B );
    final Subscription subscriptionB = new Subscription( areaOfInterestService, descriptorB );
    subscription.requireSubscription( subscriptionB );
    final Set<ChannelAddress> expectedChannels = new HashSet<>();

    final ContextConvergerImpl c =
      new TestContextConvergerImpl( subscriptionManager, areaOfInterestService, clientSystem );

    when( clientSystem.getDataLoaderService( descriptor.getGraph() ) ).
      thenReturn( service );
    when( clientSystem.getDataLoaderService( descriptorB.getGraph() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.isSubscribed( descriptor ) ).thenReturn( Boolean.FALSE );
    when( service.isSubscribed( descriptorB ) ).thenReturn( Boolean.FALSE );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, descriptor, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, descriptor, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, descriptorB, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, descriptorB, null ) ).
      thenReturn( -1 );

    // We don't support "grouping" for subscription to required graphs
    assertEquals( c.convergeSubscription( expectedChannels, subscription, null, null, true ),
                  ContextConvergerImpl.ConvergeAction.TERMINATE );

    verify( service ).requestSubscribe( descriptorB, null );
    verify( service, never() ).requestUnsubscribe( descriptor );
    verify( service, never() ).requestUnsubscribe( descriptorB );
    verify( service, never() ).requestSubscriptionUpdate( descriptor, null );
    verify( service, never() ).requestSubscriptionUpdate( descriptorB, null );
    assertEquals( expectedChannels.size(), 1 );
    assertTrue( expectedChannels.contains( subscriptionB.getDescriptor() ) );
  }

  @Test
  public void convergeSubscription_subscribedButRemovePending()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );
    final EntitySubscriptionManager subscriptionManager = mock( EntitySubscriptionManager.class );
    final Set<ChannelAddress> expectedChannels = new HashSet<>();

    final ChannelAddress descriptor = new ChannelAddress( TestGraphA.A );
    final Subscription subscription = new Subscription( areaOfInterestService, descriptor );

    final ContextConvergerImpl c =
      new TestContextConvergerImpl( subscriptionManager, areaOfInterestService, clientSystem );

    when( clientSystem.getDataLoaderService( descriptor.getGraph() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.isSubscribed( descriptor ) ).thenReturn( Boolean.TRUE );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, descriptor, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, descriptor, null ) ).
      thenReturn( 0 );

    assertEquals( c.convergeSubscription( expectedChannels, subscription, null, null, true ),
                  ContextConvergerImpl.ConvergeAction.SUBMITTED_ADD );

    verify( service ).requestSubscribe( descriptor, null );
    verify( service, never() ).requestUnsubscribe( descriptor );
    verify( service, never() ).requestSubscriptionUpdate( descriptor, null );
    assertEquals( expectedChannels.size(), 0 );
  }

  @Test
  public void convergeSubscription_subscribed()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );
    final EntitySubscriptionManager subscriptionManager = mock( EntitySubscriptionManager.class );
    final Set<ChannelAddress> expectedChannels = new HashSet<>();

    final ChannelAddress descriptor = new ChannelAddress( TestGraphA.A );
    final Subscription subscription = new Subscription( areaOfInterestService, descriptor );

    final ContextConvergerImpl c =
      new TestContextConvergerImpl( subscriptionManager, areaOfInterestService, clientSystem );

    when( clientSystem.getDataLoaderService( descriptor.getGraph() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.isSubscribed( descriptor ) ).thenReturn( Boolean.TRUE );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, descriptor, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, descriptor, null ) ).
      thenReturn( -1 );

    when( subscriptionManager.getSubscription( descriptor ) ).
      thenReturn( new ChannelSubscriptionEntry( descriptor, null, false ) );

    assertEquals( c.convergeSubscription( expectedChannels, subscription, null, null, true ),
                  ContextConvergerImpl.ConvergeAction.IN_PROGRESS );

    verify( service, never() ).requestSubscribe( descriptor, null );
    verify( service, never() ).requestUnsubscribe( descriptor );
    verify( service, never() ).requestSubscriptionUpdate( descriptor, null );
    assertEquals( expectedChannels.size(), 0 );
  }

  @Test
  public void convergeSubscription_addPending()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );
    final EntitySubscriptionManager subscriptionManager = mock( EntitySubscriptionManager.class );
    final Set<ChannelAddress> expectedChannels = new HashSet<>();

    final ChannelAddress descriptor = new ChannelAddress( TestGraphA.A );
    final Subscription subscription = new Subscription( areaOfInterestService, descriptor );

    final ContextConvergerImpl c =
      new TestContextConvergerImpl( subscriptionManager, areaOfInterestService, clientSystem );

    when( clientSystem.getDataLoaderService( descriptor.getGraph() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.isSubscribed( descriptor ) ).thenReturn( Boolean.FALSE );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, descriptor, null ) ).
      thenReturn( 1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, descriptor, null ) ).
      thenReturn( -1 );

    assertEquals( c.convergeSubscription( expectedChannels, subscription, null, null, true ),
                  ContextConvergerImpl.ConvergeAction.IN_PROGRESS );

    verify( service, never() ).requestSubscribe( descriptor, null );
    verify( service, never() ).requestUnsubscribe( descriptor );
    verify( service, never() ).requestSubscriptionUpdate( descriptor, null );
    assertEquals( expectedChannels.size(), 0 );
  }

  @Test
  public void convergeSubscription_updatePending()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );
    final EntitySubscriptionManager subscriptionManager = mock( EntitySubscriptionManager.class );
    final Set<ChannelAddress> expectedChannels = new HashSet<>();

    final ChannelAddress descriptor = new ChannelAddress( TestGraphA.A );
    final Subscription subscription = new Subscription( areaOfInterestService, descriptor );
    subscription.setFilter( "Filter1" );

    final ContextConvergerImpl c =
      new TestContextConvergerImpl( subscriptionManager, areaOfInterestService, clientSystem );

    when( clientSystem.getDataLoaderService( descriptor.getGraph() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.isSubscribed( descriptor ) ).thenReturn( Boolean.TRUE );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, descriptor, "Filter1" ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, descriptor, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.UPDATE, descriptor, "Filter1" ) ).
      thenReturn( 4 );

    assertEquals( c.convergeSubscription( expectedChannels, subscription, null, null, true ),
                  ContextConvergerImpl.ConvergeAction.IN_PROGRESS );

    verify( service, never() ).requestSubscribe( descriptor, "Filter1" );
    verify( service, never() ).requestUnsubscribe( descriptor );
    verify( service, never() ).requestSubscriptionUpdate( descriptor, "Filter1" );
    assertEquals( expectedChannels.size(), 0 );
  }

  @Test
  public void convergeSubscription_requestSubscriptionUpdate()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );
    final EntitySubscriptionManager subscriptionManager = mock( EntitySubscriptionManager.class );
    final Set<ChannelAddress> expectedChannels = new HashSet<>();

    final ChannelAddress descriptor = new ChannelAddress( TestGraphA.A );
    final Subscription subscription = new Subscription( areaOfInterestService, descriptor );
    subscription.setFilter( "Filter1" );

    final ContextConvergerImpl c =
      new TestContextConvergerImpl( subscriptionManager, areaOfInterestService, clientSystem );

    when( clientSystem.getDataLoaderService( descriptor.getGraph() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.isSubscribed( descriptor ) ).thenReturn( Boolean.TRUE );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, descriptor, "Filter1" ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, descriptor, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.UPDATE, descriptor, "Filter1" ) ).
      thenReturn( -1 );

    when( subscriptionManager.getSubscription( descriptor ) ).
      thenReturn( new ChannelSubscriptionEntry( descriptor, "OldFIlter", true ) );

    assertEquals( c.convergeSubscription( expectedChannels, subscription, null, null, true ),
                  ContextConvergerImpl.ConvergeAction.SUBMITTED_UPDATE );

    verify( service, never() ).requestSubscribe( descriptor, "Filter1" );
    verify( service, never() ).requestUnsubscribe( descriptor );
    verify( service ).requestSubscriptionUpdate( descriptor, "Filter1" );
    assertEquals( expectedChannels.size(), 0 );
  }

  @Test
  public void canGroup()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final EntitySubscriptionManager subscriptionManager = mock( EntitySubscriptionManager.class );

    final ContextConvergerImpl c =
      new TestContextConvergerImpl( subscriptionManager, areaOfInterestService, clientSystem );

    final ChannelAddress descriptor = new ChannelAddress( TestGraphA.A );
    final Subscription subscription = new Subscription( areaOfInterestService, descriptor );

    assertTrue( c.canGroup( subscription, AreaOfInterestAction.ADD, subscription, AreaOfInterestAction.ADD ) );
    assertFalse( c.canGroup( subscription, AreaOfInterestAction.ADD, subscription, AreaOfInterestAction.UPDATE ) );
    assertFalse( c.canGroup( subscription, AreaOfInterestAction.ADD, subscription, AreaOfInterestAction.REMOVE ) );
    assertFalse( c.canGroup( subscription, AreaOfInterestAction.UPDATE, subscription, AreaOfInterestAction.ADD ) );
    assertTrue( c.canGroup( subscription, AreaOfInterestAction.UPDATE, subscription, AreaOfInterestAction.UPDATE ) );
    assertFalse( c.canGroup( subscription, AreaOfInterestAction.UPDATE, subscription, AreaOfInterestAction.REMOVE ) );
    assertFalse( c.canGroup( subscription, AreaOfInterestAction.REMOVE, subscription, AreaOfInterestAction.ADD ) );
    assertFalse( c.canGroup( subscription, AreaOfInterestAction.REMOVE, subscription, AreaOfInterestAction.UPDATE ) );
    assertTrue( c.canGroup( subscription, AreaOfInterestAction.REMOVE, subscription, AreaOfInterestAction.REMOVE ) );

    final ChannelAddress descriptor2 = new ChannelAddress( TestGraphA.A, 2 );
    final Subscription subscription2 = new Subscription( areaOfInterestService, descriptor2 );
    assertTrue( c.canGroup( subscription, AreaOfInterestAction.ADD, subscription2, AreaOfInterestAction.ADD ) );

    final ChannelAddress descriptor3 = new ChannelAddress( TestGraphA.B, 1 );
    final Subscription subscription3 = new Subscription( areaOfInterestService, descriptor3 );
    assertFalse( c.canGroup( subscription, AreaOfInterestAction.ADD, subscription3, AreaOfInterestAction.ADD ) );
    assertFalse( c.canGroup( subscription, AreaOfInterestAction.ADD, subscription3, AreaOfInterestAction.UPDATE ) );
    assertFalse( c.canGroup( subscription, AreaOfInterestAction.ADD, subscription3, AreaOfInterestAction.REMOVE ) );

    final ChannelAddress descriptor4 = new ChannelAddress( TestGraphA.A, 1 );
    final Subscription subscription4 = new Subscription( areaOfInterestService, descriptor4 );
    subscription4.setFilter( "Filter" );
    assertFalse( c.canGroup( subscription, AreaOfInterestAction.ADD, subscription4, AreaOfInterestAction.ADD ) );
    subscription.setFilter( "Filter" );
    assertTrue( c.canGroup( subscription, AreaOfInterestAction.ADD, subscription4, AreaOfInterestAction.ADD ) );
  }

  @Test
  public void convergeSubscription_inactiveSubscription()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final DataLoaderService service = mock( DataLoaderService.class );
    final EntitySubscriptionManager subscriptionManager = mock( EntitySubscriptionManager.class );
    final Set<ChannelAddress> expectedChannels = new HashSet<>();

    final ChannelAddress descriptor = new ChannelAddress( TestGraphA.A, 1 );
    final Subscription subscription1 = new Subscription( areaOfInterestService, descriptor );
    subscription1.delete();

    final ContextConvergerImpl c =
      new TestContextConvergerImpl( subscriptionManager, areaOfInterestService, clientSystem );

    when( clientSystem.getDataLoaderService( descriptor.getGraph() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    assertEquals( c.convergeSubscription( expectedChannels, subscription1, null, null, true ),
                  ContextConvergerImpl.ConvergeAction.NO_ACTION );
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
    final Set<ChannelAddress> expectedChannels = new HashSet<>();

    final ChannelAddress descriptor = new ChannelAddress( TestGraphA.A, 1 );
    final Subscription subscription1 = new Subscription( areaOfInterestService, descriptor );

    final ChannelAddress descriptor2 = new ChannelAddress( TestGraphA.A, 2 );
    final Subscription subscription2 = new Subscription( areaOfInterestService, descriptor2 );

    final ChannelAddress descriptor3 = new ChannelAddress( TestGraphA.A, 3 );
    final Subscription subscription3 = new Subscription( areaOfInterestService, descriptor3 );

    final ContextConvergerImpl c =
      new TestContextConvergerImpl( subscriptionManager, areaOfInterestService, clientSystem );

    when( clientSystem.getDataLoaderService( descriptor.getGraph() ) ).
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

    assertEquals( c.convergeSubscription( expectedChannels,
                                          subscription2,
                                          subscription1,
                                          AreaOfInterestAction.ADD,
                                          true ),
                  ContextConvergerImpl.ConvergeAction.SUBMITTED_ADD );
    verify( service ).requestSubscribe( descriptor2, null );
    verify( service, never() ).requestUnsubscribe( descriptor2 );
    verify( service, never() ).requestSubscriptionUpdate( descriptor2, null );

    assertEquals( c.convergeSubscription( expectedChannels,
                                          subscription3,
                                          subscription1,
                                          AreaOfInterestAction.ADD,
                                          false ),
                  ContextConvergerImpl.ConvergeAction.TERMINATE );
    verify( service, never() ).requestSubscribe( descriptor3, null );
    verify( service, never() ).requestUnsubscribe( descriptor3 );
    verify( service, never() ).requestSubscriptionUpdate( descriptor3, null );

    subscription3.setFilter( "Filter" );
    assertEquals( c.convergeSubscription( expectedChannels,
                                          subscription3,
                                          subscription1,
                                          AreaOfInterestAction.ADD,
                                          true ),
                  ContextConvergerImpl.ConvergeAction.NO_ACTION );
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
    final Set<ChannelAddress> expectedChannels = new HashSet<>();

    final ChannelAddress descriptor = new ChannelAddress( TestGraphA.A, 1 );
    final Subscription subscription1 = new Subscription( areaOfInterestService, descriptor );

    final ChannelAddress descriptor2 = new ChannelAddress( TestGraphA.A, 2 );
    final Subscription subscription2 = new Subscription( areaOfInterestService, descriptor2 );

    final ChannelAddress descriptor3 = new ChannelAddress( TestGraphA.A, 3 );
    final Subscription subscription3 = new Subscription( areaOfInterestService, descriptor3 );

    final ContextConvergerImpl c =
      new TestContextConvergerImpl( subscriptionManager, areaOfInterestService, clientSystem );

    when( clientSystem.getDataLoaderService( descriptor.getGraph() ) ).
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

    when( subscriptionManager.getSubscription( descriptor2 ) ).
      thenReturn( new ChannelSubscriptionEntry( descriptor2, "OldFilter", true ) );
    when( subscriptionManager.getSubscription( descriptor3 ) ).
      thenReturn( new ChannelSubscriptionEntry( descriptor3, "OldFilter", true ) );

    assertEquals( c.convergeSubscription( expectedChannels,
                                          subscription2,
                                          subscription1,
                                          AreaOfInterestAction.UPDATE,
                                          true ),
                  ContextConvergerImpl.ConvergeAction.SUBMITTED_UPDATE );
    verify( service, never() ).requestSubscribe( descriptor2, null );
    verify( service, never() ).requestUnsubscribe( descriptor2 );
    verify( service ).requestSubscriptionUpdate( descriptor2, null );

    assertEquals( c.convergeSubscription( expectedChannels,
                                          subscription3,
                                          subscription1,
                                          AreaOfInterestAction.UPDATE,
                                          false ),
                  ContextConvergerImpl.ConvergeAction.TERMINATE );
    verify( service, never() ).requestSubscribe( descriptor3, null );
    verify( service, never() ).requestUnsubscribe( descriptor3 );
    verify( service, never() ).requestSubscriptionUpdate( descriptor3, null );

    subscription3.setFilter( "Filter" );
    assertEquals( c.convergeSubscription( expectedChannels,
                                          subscription3,
                                          subscription1,
                                          AreaOfInterestAction.UPDATE,
                                          true ),
                  ContextConvergerImpl.ConvergeAction.NO_ACTION );
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

    final ContextConvergerImpl c =
      new TestContextConvergerImpl( subscriptionManager, areaOfInterestService, clientSystem );

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
