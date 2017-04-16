package org.realityforge.replicant.client.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import org.realityforge.replicant.client.ChannelDescriptor;
import org.realityforge.replicant.client.ChannelSubscriptionEntry;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.client.EntitySubscriptionManagerImpl;
import org.realityforge.replicant.client.transport.AreaOfInterestAction;
import org.realityforge.replicant.client.transport.DataLoaderService;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;

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

    when( service.isAreaOfInterestActionPending( AreaOfInterestAction.REMOVE, descriptor, null ) ).
      thenReturn( Boolean.FALSE );

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

    when( service.isAreaOfInterestActionPending( AreaOfInterestAction.REMOVE, descriptor, null ) ).
      thenReturn( Boolean.FALSE );

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

    when( service.isAreaOfInterestActionPending( AreaOfInterestAction.REMOVE, descriptor, null ) ).
      thenReturn( Boolean.FALSE );

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

    final ChannelDescriptor descriptor = new ChannelDescriptor( TestGraphA.A );
    final Subscription subscription = new Subscription( areaOfInterestService, descriptor );

    final ContextConvergerImpl c =
      new TestContextConvergerImpl( subscriptionManager, areaOfInterestService, clientSystem );

    when( clientSystem.getDataLoaderService( descriptor.getGraph() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.isSubscribed( descriptor ) ).thenReturn( Boolean.FALSE );

    when( service.isAreaOfInterestActionPending( AreaOfInterestAction.ADD, descriptor, null ) ).
      thenReturn( Boolean.FALSE );
    when( service.isAreaOfInterestActionPending( AreaOfInterestAction.REMOVE, descriptor, null ) ).
      thenReturn( Boolean.FALSE );

    c.convergeSubscription( subscription );

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

    final ChannelDescriptor descriptor = new ChannelDescriptor( TestGraphA.A );
    final Subscription subscription = new Subscription( areaOfInterestService, descriptor );

    final ContextConvergerImpl c =
      new TestContextConvergerImpl( subscriptionManager, areaOfInterestService, clientSystem );

    when( clientSystem.getDataLoaderService( descriptor.getGraph() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.isSubscribed( descriptor ) ).thenReturn( Boolean.TRUE );

    when( service.isAreaOfInterestActionPending( AreaOfInterestAction.ADD, descriptor, null ) ).
      thenReturn( Boolean.FALSE );
    when( service.isAreaOfInterestActionPending( AreaOfInterestAction.REMOVE, descriptor, null ) ).
      thenReturn( Boolean.TRUE );

    c.convergeSubscription( subscription );

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

    final ChannelDescriptor descriptor = new ChannelDescriptor( TestGraphA.A );
    final Subscription subscription = new Subscription( areaOfInterestService, descriptor );

    final ContextConvergerImpl c =
      new TestContextConvergerImpl( subscriptionManager, areaOfInterestService, clientSystem );

    when( clientSystem.getDataLoaderService( descriptor.getGraph() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.isSubscribed( descriptor ) ).thenReturn( Boolean.TRUE );

    when( service.isAreaOfInterestActionPending( AreaOfInterestAction.ADD, descriptor, null ) ).
      thenReturn( Boolean.FALSE );
    when( service.isAreaOfInterestActionPending( AreaOfInterestAction.REMOVE, descriptor, null ) ).
      thenReturn( Boolean.FALSE );

    when( subscriptionManager.getSubscription( descriptor ) ).
      thenReturn( new ChannelSubscriptionEntry( descriptor, null, false ) );

    c.convergeSubscription( subscription );

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

    final ChannelDescriptor descriptor = new ChannelDescriptor( TestGraphA.A );
    final Subscription subscription = new Subscription( areaOfInterestService, descriptor );

    final ContextConvergerImpl c =
      new TestContextConvergerImpl( subscriptionManager, areaOfInterestService, clientSystem );

    when( clientSystem.getDataLoaderService( descriptor.getGraph() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.isSubscribed( descriptor ) ).thenReturn( Boolean.FALSE );

    when( service.isAreaOfInterestActionPending( AreaOfInterestAction.ADD, descriptor, null ) ).
      thenReturn( Boolean.TRUE );
    when( service.isAreaOfInterestActionPending( AreaOfInterestAction.REMOVE, descriptor, null ) ).
      thenReturn( Boolean.FALSE );

    c.convergeSubscription( subscription );

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

    final ChannelDescriptor descriptor = new ChannelDescriptor( TestGraphA.A );
    final Subscription subscription = new Subscription( areaOfInterestService, descriptor );
    subscription.setFilter( "Filter1" );

    final ContextConvergerImpl c =
      new TestContextConvergerImpl( subscriptionManager, areaOfInterestService, clientSystem );

    when( clientSystem.getDataLoaderService( descriptor.getGraph() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.isSubscribed( descriptor ) ).thenReturn( Boolean.TRUE );

    when( service.isAreaOfInterestActionPending( AreaOfInterestAction.ADD, descriptor, "Filter1" ) ).
      thenReturn( Boolean.FALSE );
    when( service.isAreaOfInterestActionPending( AreaOfInterestAction.REMOVE, descriptor, null ) ).
      thenReturn( Boolean.FALSE );
    when( service.isAreaOfInterestActionPending( AreaOfInterestAction.UPDATE, descriptor, "Filter1" ) ).
      thenReturn( Boolean.TRUE );

    c.convergeSubscription( subscription );

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

    final ChannelDescriptor descriptor = new ChannelDescriptor( TestGraphA.A );
    final Subscription subscription = new Subscription( areaOfInterestService, descriptor );
    subscription.setFilter( "Filter1" );

    final ContextConvergerImpl c =
      new TestContextConvergerImpl( subscriptionManager, areaOfInterestService, clientSystem );

    when( clientSystem.getDataLoaderService( descriptor.getGraph() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.isSubscribed( descriptor ) ).thenReturn( Boolean.TRUE );

    when( service.isAreaOfInterestActionPending( AreaOfInterestAction.ADD, descriptor, "Filter1" ) ).
      thenReturn( Boolean.FALSE );
    when( service.isAreaOfInterestActionPending( AreaOfInterestAction.REMOVE, descriptor, null ) ).
      thenReturn( Boolean.FALSE );
    when( service.isAreaOfInterestActionPending( AreaOfInterestAction.UPDATE, descriptor, "Filter1" ) ).
      thenReturn( Boolean.FALSE );

    when( subscriptionManager.getSubscription( descriptor ) ).
      thenReturn( new ChannelSubscriptionEntry( descriptor, "OldFIlter", true ) );

    c.convergeSubscription( subscription );

    verify( service, never() ).requestSubscribe( descriptor, "Filter1" );
    verify( service, never() ).requestUnsubscribe( descriptor );
    verify( service ).requestSubscriptionUpdate( descriptor, "Filter1" );
  }
}
