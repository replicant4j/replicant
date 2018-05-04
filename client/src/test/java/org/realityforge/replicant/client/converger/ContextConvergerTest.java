package org.realityforge.replicant.client.converger;

import arez.Arez;
import arez.Disposable;
import java.util.HashSet;
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
  public void preConvergeAction()
  {
    final ContextConverger c = ContextConverger.create( mock( ReplicantClientSystem.class ) );

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

    final ContextConverger c = ContextConverger.create( clientSystem );

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

    final ContextConverger c = ContextConverger.create( clientSystem );

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

    final ContextConverger c = ContextConverger.create( clientSystem );

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

    final ContextConverger c = ContextConverger.create( clientSystem );

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

    final ContextConverger c = ContextConverger.create( clientSystem );

    final Subscription subscription = Replicant.context().createSubscription( address, null, true );
    c.removeSubscriptionIfOrphan( expected, subscription );

    verify( clientSystem, never() ).getDataLoaderService( TestSystemA.A );
  }

  @Test
  public void removeSubscriptionIfOrphan_notExplicit()
  {
    final ReplicantClientSystem clientSystem = mock( ReplicantClientSystem.class );
    final ChannelAddress address = new ChannelAddress( TestSystemA.A );

    final ContextConverger c = ContextConverger.create( clientSystem );

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

    final ContextConverger c = ContextConverger.create( clientSystem );

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

    final ContextConverger c = ContextConverger.create( clientSystem );

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
    Replicant.context().createSubscription( address, null, true );
    final Channel channel = Channel.create( address, null );
    final AreaOfInterest areaOfInterest = AreaOfInterest.create( channel );

    final ContextConverger c = ContextConverger.create( clientSystem );

    when( clientSystem.getDataLoaderService( address.getChannelType() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

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
    Replicant.context().createSubscription( address, null, true );
    final Channel channel = Channel.create( address, null );
    final AreaOfInterest areaOfInterest = AreaOfInterest.create( channel );

    final ContextConverger c = ContextConverger.create( clientSystem );

    when( clientSystem.getDataLoaderService( address.getChannelType() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, address, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, address, null ) ).
      thenReturn( -1 );

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

    final ContextConverger c = ContextConverger.create( clientSystem );

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

    final ContextConverger c = ContextConverger.create( clientSystem );

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
    Replicant.context().createSubscription( address, "OldFilter", false );
    final Channel channel = Channel.create( address, null );
    channel.setFilter( "Filter1" );
    final AreaOfInterest areaOfInterest = AreaOfInterest.create( channel );

    final ContextConverger c = ContextConverger.create( clientSystem );

    when( clientSystem.getDataLoaderService( address.getChannelType() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, address, "Filter1" ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, address, null ) ).
      thenReturn( -1 );
    when( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.UPDATE, address, "Filter1" ) ).
      thenReturn( -1 );

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

    final ContextConverger c = ContextConverger.create( clientSystem );

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

    final ContextConverger c = ContextConverger.create( clientSystem );

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

    final ContextConverger c = ContextConverger.create( clientSystem );

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
    Replicant.context().createSubscription( address2, "OldFilter", true );
    final Channel channel2 = Channel.create( address2, null );
    final AreaOfInterest areaOfInterest2 = AreaOfInterest.create( channel2 );

    final ChannelAddress address3 = new ChannelAddress( TestSystemA.A, 3 );
    Replicant.context().createSubscription( address3, "OldFilter", true );
    final Channel channel3 = Channel.create( address3, null );
    final AreaOfInterest areaOfInterest3 = AreaOfInterest.create( channel3 );

    final ContextConverger c = ContextConverger.create( clientSystem );

    when( clientSystem.getDataLoaderService( address.getChannelType() ) ).
      thenReturn( service );

    when( service.getState() ).thenReturn( DataLoaderService.State.CONNECTED );

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
}
