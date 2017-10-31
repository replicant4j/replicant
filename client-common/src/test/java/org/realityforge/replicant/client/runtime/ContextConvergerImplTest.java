package org.realityforge.replicant.client.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.realityforge.replicant.client.ChannelDescriptor;
import org.realityforge.replicant.client.ChannelSubscriptionEntry;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.client.EntitySubscriptionManagerImpl;
import org.realityforge.replicant.client.transport.AreaOfInterestAction;
import org.realityforge.replicant.client.transport.DataLoaderService;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

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
    final ChannelDescriptor descriptor = new ChannelDescriptor( TestGraphA.A );

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
    final ChannelDescriptor descriptor = new ChannelDescriptor( TestGraphA.A );

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
    final ChannelDescriptor descriptor = new ChannelDescriptor( TestGraphA.A );

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
    final ChannelDescriptor descriptor = new ChannelDescriptor( TestGraphA.A );

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

    final HashSet<ChannelDescriptor> expected = new HashSet<>();

    when( subscriptionManager.getSubscription( descriptor ) ).
      thenReturn( new ChannelSubscriptionEntry( descriptor, null, true ) );

    c.removeSubscriptionIfOrphan( expected, descriptor );

    verify( service ).requestUnsubscribe( descriptor );
  }

  @Test
  public void removeSubscriptionIfOrphan_expected()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final ChannelDescriptor descriptor = new ChannelDescriptor( TestGraphA.A );
    final HashSet<ChannelDescriptor> expected = new HashSet<>();
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
    final ChannelDescriptor descriptor = new ChannelDescriptor( TestGraphA.A );

    final EntitySubscriptionManager subscriptionManager = mock( EntitySubscriptionManager.class );

    final ContextConvergerImpl c =
      new TestContextConvergerImpl( subscriptionManager,
                                    mock( AreaOfInterestService.class ),
                                    clientSystem );

    final HashSet<ChannelDescriptor> expected = new HashSet<>();

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
    final ChannelDescriptor descriptor = new ChannelDescriptor( TestGraphA.A );

    final HashSet<ChannelDescriptor> expected = new HashSet<>();
    expected.add( new ChannelDescriptor( TestGraphA.B, 1 ) );
    expected.add( new ChannelDescriptor( TestGraphA.B, 2 ) );
    expected.add( new ChannelDescriptor( TestGraphA.B, 3 ) );
    expected.add( new ChannelDescriptor( TestGraphA.B, 4 ) );
    expected.add( new ChannelDescriptor( TestGraphA.B, 5 ) );

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
    final Set<ChannelDescriptor> expectedChannels = new HashSet<>();

    final ChannelDescriptor descriptor = new ChannelDescriptor( TestGraphA.A );
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

    c.convergeSubscription( expectedChannels, subscription );

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

    final ChannelDescriptor descriptor = new ChannelDescriptor( TestGraphA.A );
    final Subscription subscription = new Subscription( areaOfInterestService, descriptor );
    final ChannelDescriptor descriptorB = new ChannelDescriptor( TestGraphA.B );
    final Subscription subscriptionB = new Subscription( areaOfInterestService, descriptorB );
    subscription.requireSubscription( subscriptionB );
    final Set<ChannelDescriptor> expectedChannels = new HashSet<>();

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

    c.convergeSubscription( expectedChannels, subscription );

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
    final Set<ChannelDescriptor> expectedChannels = new HashSet<>();

    final ChannelDescriptor descriptor = new ChannelDescriptor( TestGraphA.A );
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

    c.convergeSubscription( expectedChannels, subscription );

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
    final Set<ChannelDescriptor> expectedChannels = new HashSet<>();

    final ChannelDescriptor descriptor = new ChannelDescriptor( TestGraphA.A );
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

    c.convergeSubscription( expectedChannels, subscription );

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
    final Set<ChannelDescriptor> expectedChannels = new HashSet<>();

    final ChannelDescriptor descriptor = new ChannelDescriptor( TestGraphA.A );
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

    c.convergeSubscription( expectedChannels, subscription );

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
    final Set<ChannelDescriptor> expectedChannels = new HashSet<>();

    final ChannelDescriptor descriptor = new ChannelDescriptor( TestGraphA.A );
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

    c.convergeSubscription( expectedChannels, subscription );

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
    final Set<ChannelDescriptor> expectedChannels = new HashSet<>();

    final ChannelDescriptor descriptor = new ChannelDescriptor( TestGraphA.A );
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

    c.convergeSubscription( expectedChannels, subscription );

    verify( service, never() ).requestSubscribe( descriptor, "Filter1" );
    verify( service, never() ).requestUnsubscribe( descriptor );
    verify( service ).requestSubscriptionUpdate( descriptor, "Filter1" );
    assertEquals( expectedChannels.size(), 0 );
  }
}
