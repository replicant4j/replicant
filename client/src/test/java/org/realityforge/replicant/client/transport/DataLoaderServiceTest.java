package org.realityforge.replicant.client.transport;

import arez.Arez;
import arez.Disposable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mockito.InOrder;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.client.Linkable;
import org.realityforge.replicant.client.transport.ChannelAction.Action;
import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.ITestResult;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.AreaOfInterestAction;
import replicant.ChannelAddress;
import replicant.Replicant;
import replicant.ReplicantTestUtil;
import replicant.SafeProcedure;
import replicant.Subscription;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

@SuppressWarnings( "NonJREEmulationClassesInClientCode" )
public class DataLoaderServiceTest
  extends AbstractReplicantTest
  implements IHookable
{
  @Override
  public void run( final IHookCallBack callBack, final ITestResult testResult )
  {
    Arez.context().safeAction( () -> callBack.runTestMethod( testResult ) );
  }

  static class MyType
    implements Disposable
  {
    private boolean _disposed;

    @Override
    public void dispose()
    {
      _disposed = true;
    }

    @Override
    public boolean isDisposed()
    {
      return _disposed;
    }
  }

  /*
  @Test( enabled = false )
  public void purgeSubscriptions()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();
    configureService( service );
    final EntityService sm = service.getEntityService();

    final MyType myTypeA = new MyType();
    final MyType myTypeB = new MyType();
    final MyType myTypeC = new MyType();
    final MyType myTypeD = new MyType();

    sm.addChannelSubscriptions( MyType.class, "A1", new ChannelAddress[ 0 ], myTypeA );
    sm.addChannelSubscriptions( MyType.class, "B1", new ChannelAddress[ 0 ], myTypeB );
    sm.addChannelSubscriptions( MyType.class, "C1", new ChannelAddress[ 0 ], myTypeC );
    sm.addChannelSubscriptions( MyType.class, "D1", new ChannelAddress[ 0 ], myTypeD );

    //LinkedHashSet means keys come out in "wrong" order
    // and will need to be resorted in purgeSubscriptions
    final HashSet<Enum> instanceChannelTypes = new LinkedHashSet<>();
    instanceChannelTypes.add( TestSystem.B );
    instanceChannelTypes.add( TestSystem.A );

    //LinkedHashSet means keys come out in "wrong" order
    // and will need to be resorted in purgeSubscriptions
    final HashSet<Enum> typeChannels = new LinkedHashSet<>();
    typeChannels.add( TestSystem.D );
    typeChannels.add( TestSystem.C );

    final HashSet<Object> aChannelType = new HashSet<>();
    aChannelType.add( "1" );
    final HashSet<Object> bChannelType = new HashSet<>();
    bChannelType.add( "2" );

    final Subscription entryA =
      Subscription.create( Channel.create( new ChannelAddress( TestSystem.A, "1" ), null ), true );
    final Subscription entryB =
      Subscription.create( Channel.create( new ChannelAddress( TestSystem.B, "2" ), null ), true );
    final Subscription entryC =
      Subscription.create( Channel.create( new ChannelAddress( TestSystem.C ), null ), true );
    final Subscription entryD =
      Subscription.create( Channel.create( new ChannelAddress( TestSystem.D ), null ), true );

    registerEntity( entryA, MyType.class, "A1" );
    registerEntity( entryB, MyType.class, "B1" );
    registerEntity( entryC, MyType.class, "C1" );
    registerEntity( entryD, MyType.class, "D1" );

    when( sm.getInstanceSubscriptions() ).thenReturn( instanceChannelTypes );
    when( sm.getInstanceSubscriptionIds( TestSystem.A ) ).thenReturn( aChannelType );
    when( sm.getInstanceSubscriptionIds( TestSystem.B ) ).thenReturn( bChannelType );
    when( sm.getTypeSubscriptions() ).thenReturn( typeChannels );
    when( sm.removeSubscription( new ChannelAddress( TestSystem.A, "1" ) ) ).thenReturn( entryA );
    when( sm.removeSubscription( new ChannelAddress( TestSystem.B, "2" ) ) ).thenReturn( entryB );
    when( sm.removeSubscription( new ChannelAddress( TestSystem.C ) ) ).thenReturn( entryC );
    when( sm.removeSubscription( new ChannelAddress( TestSystem.D ) ) ).thenReturn( entryD );

    assertFalse( myTypeA.isDisposed() );
    assertFalse( myTypeB.isDisposed() );
    assertFalse( myTypeC.isDisposed() );
    assertFalse( myTypeD.isDisposed() );

    service.purgeSubscriptions();

    final InOrder inOrder = inOrder( sm );
    inOrder.verify( sm ).removeSubscription( new ChannelAddress( TestSystem.B, "2" ) );
    inOrder.verify( sm ).removeSubscription( new ChannelAddress( TestSystem.A, "1" ) );
    inOrder.verify( sm ).removeSubscription( new ChannelAddress( TestSystem.D ) );
    inOrder.verify( sm ).removeSubscription( new ChannelAddress( TestSystem.C ) );
    inOrder.verifyNoMoreInteractions();

    assertTrue( myTypeA.isDisposed() );
    assertTrue( myTypeB.isDisposed() );
    assertTrue( myTypeC.isDisposed() );
    assertTrue( myTypeD.isDisposed() );
  }


  private void registerEntity( @Nonnull final Subscription entry,
                               @Nonnull final Class<?> type,
                               @Nonnull final String id )
  {
    final Map<Class<?>, Map<Object, Entity>> entities = entry.getEntities();
    final Map<Object, Entity> typeMap = entities.computeIfAbsent( type, k -> new HashMap<>() );
    final Entity entity = Entity.create( type, id );
    entity.addChannelSubscriptions( new Subscription[]{ entry}, new Object() );
    typeMap.put( id, entity );
  }
  */

  @Test
  public void setSession()
    throws Exception
  {
    final TestChangeSet changeSet =
      new TestChangeSet( 1, mock( Runnable.class ), new Change[ 0 ] );
    final TestDataLoadService service = newService( changeSet );
    final SafeProcedure runnable1 = mock( SafeProcedure.class );
    final String sessionID = ValueUtil.randomString();
    final ClientSession session1 = new ClientSession( ValueUtil.randomString() );

    session1.enqueueOOB( sessionID, null );

    assertEquals( service.getSessionContext().getSession(), null );

    service.setSession( session1, runnable1 );

    assertEquals( service.getSessionContext().getSession(), session1 );
    assertEquals( service.getSession(), session1 );
    verify( runnable1, times( 1 ) ).call();

    // Should be no oob actions left
    progressWorkTillDone( service, 7, 1 );

    service.setSession( session1, runnable1 );
    verify( runnable1, times( 2 ) ).call();
  }

  @Test
  public void getTerminateCount()
    throws Exception
  {
    final TestChangeSet changeSet = new TestChangeSet( 1, mock( Runnable.class ), new Change[ 0 ] );
    final TestDataLoadService service = newService( changeSet );
    ensureEnqueueDataLoads( service );

    for ( int i = 0; i < 6; i++ )
    {
      assertTrue( service.progressDataLoad() );
      assertEquals( service.getTerminateCount(), 0 );
    }

    assertFalse( service.progressDataLoad() );
    assertEquals( service.getTerminateCount(), 1 );
  }

  @Test
  public void cache_requestWithCacheKeyAndETag()
    throws Exception
  {
    final TestChangeSet changeSet =
      new TestChangeSet( 1, mock( Runnable.class ), new Change[ 0 ] );
    final String cacheKey = ValueUtil.randomString();
    final String etag = ValueUtil.randomString();

    changeSet.setCacheKey( cacheKey );
    changeSet.setEtag( etag );

    final TestDataLoadService service = newService( changeSet );
    final CacheService cacheService = service.getCacheService();

    assertEquals( service.ensureSession().getLastRxSequence(), 0 );

    service.ensureSession().enqueueDataLoad( "Data" );
    final RequestEntry request = configureRequest( changeSet, service );
    assertNotNull( request );

    progressWorkTillDone( service, 7, 1 );

    assertEquals( service.ensureSession().getLastRxSequence(), changeSet.getSequence() );
    assertEquals( service.getValidateRepositoryCallCount(), 1 );

    verify( cacheService ).store( cacheKey, etag, "Data" );

    assertRequestProcessed( service, request );
  }

  @Test
  public void cache_withOOB()
    throws Exception
  {
    final TestChangeSet changeSet =
      new TestChangeSet( 1, mock( Runnable.class ), new Change[ 0 ] );
    changeSet.setCacheKey( ValueUtil.randomString() );
    changeSet.setEtag( ValueUtil.randomString() );

    final TestDataLoadService service = newService( changeSet );
    final CacheService cacheService = service.getCacheService();

    assertEquals( service.ensureSession().getLastRxSequence(), 0 );

    service.ensureSession().enqueueOOB( "Data", changeSet.getRunnable() );

    progressWorkTillDone( service, 7, 1 );

    assertEquals( service.ensureSession().getLastRxSequence(), 0 );

    assertEquals( service.getValidateRepositoryCallCount(), 1 );
    verify( cacheService, never() ).store( anyString(), anyString(), anyString() );
  }

  private void assertRequestProcessed( final TestDataLoadService service, final RequestEntry request )
  {
    assertTrue( request.haveResultsArrived() );
    assertNotInRequestManager( service, request );
    assertTrue( service.isDataLoadComplete() );
    assertNotNull( service.getStatus().getRequestId() );

    verifyPostActionRun( request.getCompletionAction() );
  }

  @Test
  public void enqueueOOB()
    throws Exception
  {
    final Linkable entity = mock( Linkable.class );
    final TestChangeSet changeSet =
      new TestChangeSet( 1, mock( Runnable.class ), new Change[]{ new TestChange( true ) } );

    final TestDataLoadService service = newService( changeSet );

    when( service.getChangeMapper().applyChange( changeSet.getChange( 0 ) ) ).thenReturn( entity );

    assertEquals( service.ensureSession().getLastRxSequence(), 0 );

    assertFalse( service.isScheduleDataLoadCalled() );
    for ( final TestChangeSet cs : service.getChangeSets() )
    {
      service.ensureSession().enqueueOOB( "BLAH:" + cs.getSequence(), changeSet.getRunnable() );
    }

    progressWorkTillDone( service, 9, 1 );

    assertEquals( service.ensureSession().getLastRxSequence(), 0 );

    assertEquals( service.getValidateRepositoryCallCount(), 1 );

    assertTrue( service.isDataLoadComplete() );
    assertNull( service.getStatus().getRequestId() );

    verify( service.getChangeMapper() ).applyChange( changeSet.getChange( 0 ) );
    verify( entity ).link();

    verifyPostActionRun( changeSet.getRunnable() );
  }

  @Test
  public void ordering()
    throws Exception
  {
    final TestChangeSet cs1 = new TestChangeSet( 1, null, new Change[ 0 ] );
    final TestChangeSet cs2 = new TestChangeSet( 2, null, new Change[ 0 ] );
    final TestChangeSet cs3 = new TestChangeSet( 3, null, new Change[ 0 ] );

    final DataLoadAction oob1 = new DataLoadAction( "oob1", true );
    final DataLoadAction oob2 = new DataLoadAction( "oob2", true );
    final DataLoadAction oob3 = new DataLoadAction( "oob3", true );

    final DataLoadAction s1 = new DataLoadAction( "s1", false );
    s1.setChangeSet( cs1, null );
    final DataLoadAction s2 = new DataLoadAction( "s2", false );
    s2.setChangeSet( cs2, null );
    final DataLoadAction s3 = new DataLoadAction( "s3", false );
    s3.setChangeSet( cs3, null );

    final List<DataLoadAction> l1 = Arrays.asList( s2, s3, s1, oob1, oob2, oob3 );
    Collections.sort( l1 );
    assertEquals( l1, Arrays.asList( oob1, oob2, oob3, s1, s2, s3 ) );
  }

  @Test
  public void verifyDataLoader_doesNotRemoveRequestIfNotYetHandled()
    throws Exception
  {
    final TestChangeSet changeSet =
      new TestChangeSet( 1, mock( Runnable.class ), new Change[ 0 ] );

    final TestDataLoadService service = newService( changeSet );

    final RequestEntry request =
      service.ensureSession().newRequest( "", null );
    changeSet.setRequestID( request.getRequestID() );
    service.ensureSession().enqueueDataLoad( "blah" );

    progressWorkTillDone( service, 7, 1 );

    assertTrue( request.haveResultsArrived() );
    final String requestID = changeSet.getRequestID();
    assertNotNull( requestID );
    assertInRequestManager( service, request );
    assertNotNull( service.getStatus().getRequestId() );

    verifyPostActionNotRun( changeSet.getRunnable() );
  }

  @Test
  public void verifyDataLoader_dataLoadWithZeroChanges()
    throws Exception
  {
    final TestChangeSet changeSet = new TestChangeSet( 1, mock( Runnable.class ), new Change[ 0 ] );

    final TestDataLoadService service = newService( changeSet );

    assertEquals( service.ensureSession().getLastRxSequence(), 0 );

    ensureEnqueueDataLoads( service );

    progressWorkTillDone( service, 7, 1 );

    assertEquals( service.ensureSession().getLastRxSequence(), changeSet.getSequence() );

    assertEquals( service.getValidateRepositoryCallCount(), 1 );

    assertTrue( service.isDataLoadComplete() );
    assertNotNull( service.getStatus().getRequestId() );

    verifyPostActionRun( changeSet.getRunnable() );
  }

  @Test
  public void verifyDeletedEntityIsNotLinked()
    throws Exception
  {
    final Linkable entity = mock( Linkable.class );
    final TestChangeSet changeSet = new TestChangeSet( 1, null, new Change[]{ new TestChange( false ) } );

    final TestDataLoadService service = newService( changeSet );

    when( service.getChangeMapper().applyChange( changeSet.getChange( 0 ) ) ).thenReturn( entity );

    ensureEnqueueDataLoads( service );

    progressWorkTillDone( service, 8, 1 );

    assertEquals( service.getValidateRepositoryCallCount(), 1 );
    verify( service.getChangeMapper() ).applyChange( changeSet.getChange( 0 ) );
    verify( entity, never() ).link();
  }

  @Test
  public void verifyIncrementalChangeInvokesCorrectMethods()
    throws Exception
  {
    final TestChangeSet changeSet = new TestChangeSet( 1, null, new Change[]{ new TestChange( true ) } );

    final TestDataLoadService service = newService( changeSet );

    assertEquals( service.ensureSession().getLastRxSequence(), 0 );

    ensureEnqueueDataLoads( service );

    progressWorkTillDone( service, 8, 1 );

    assertEquals( service.getValidateRepositoryCallCount(), 1 );

    assertEquals( service.ensureSession().getLastRxSequence(), changeSet.getSequence() );
  }

  @Test
  public void verifyValidateIsNotCalled()
    throws Exception
  {
    ReplicantTestUtil.noValidateRepositoryOnLoad();
    final TestChangeSet changeSet = new TestChangeSet( 1, null, new Change[]{ new TestChange( true ) } );

    final TestDataLoadService service = newService( changeSet, changeSet );

    ensureEnqueueDataLoads( service );

    progressWorkTillDone( service, 10, 2 );

    assertEquals( service.getValidateRepositoryCallCount(), 0 );
  }

  @Test
  public void verifyDataLoader_sequenceFollowed()
    throws Exception
  {
    final Linkable entity = mock( Linkable.class );
    final TestChangeSet changeSet1 =
      new TestChangeSet( 1, mock( Runnable.class ), new Change[]{ new TestChange( true ) } );
    final TestChangeSet changeSet2 =
      new TestChangeSet( 2, mock( Runnable.class ), new Change[]{ new TestChange( true ) } );

    final TestDataLoadService service = newService( changeSet2, changeSet1 );
    final ChangeMapper changeMapper = service.getChangeMapper();
    when( changeMapper.applyChange( changeSet1.getChange( 0 ) ) ).thenReturn( entity );
    when( changeMapper.applyChange( changeSet2.getChange( 0 ) ) ).thenReturn( entity );

    assertEquals( service.ensureSession().getLastRxSequence(), 0 );

    configureRequests( service, service.getChangeSets() );
    service.ensureSession().enqueueDataLoad( "jsonData" );
    progressWorkTillDone( service, 3, 1 );

    //No progress should have been made other than parsing packet as out of sequence
    assertEquals( service.ensureSession().getLastRxSequence(), 0 );
    verifyPostActionNotRun( changeSet2.getRunnable() );
    verifyPostActionNotRun( changeSet1.getRunnable() );

    service.ensureSession().enqueueDataLoad( "jsonData" );
    progressWorkTillDone( service, 15, 2 );

    //Progress should have been made as all sequence appears
    assertEquals( service.ensureSession().getLastRxSequence(), 2 );
    final InOrder inOrder = inOrder( changeSet2.getRunnable(), changeSet1.getRunnable() );
    inOrder.verify( changeSet1.getRunnable() ).run();
    inOrder.verify( changeSet2.getRunnable() ).run();
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void channelActions()
    throws Exception
  {
    final TestChangeSet changeSet1 =
      new TestChangeSet( 1,
                         mock( Runnable.class ),
                         new Change[ 0 ],
                         new ChannelAction[]{ new TestChannelAction( TestSystem.B.ordinal(), 72, Action.ADD ) } );

    final TestDataLoadService service = newService( changeSet1 );

    assertEquals( service.ensureSession().getLastRxSequence(), 0 );

    configureRequests( service, service.getChangeSets() );
    service.ensureSession().enqueueDataLoad( "jsonData" );
    service.scheduleDataLoad();

    final LinkedList<DataLoadAction> actions = progressWorkTillDone( service, 8, 1 );
    final ChannelAddress address = new ChannelAddress( TestSystem.B, 72 );

    final Subscription subscription = Replicant.context().findSubscription( address );
    assertNotNull( subscription );
    assertEquals( subscription.getAddress(), address );
    assertEquals( subscription.getFilter(), null );
    assertEquals( subscription.isExplicitSubscription(), false );

    final DataLoadAction action = actions.getLast();

    assertEquals( action.getChannelAddCount(), 1 );
    assertEquals( action.getChannelRemoveCount(), 0 );
  }

  @Test
  public void multipleChannelActions()
    throws Exception
  {
    final TestChannelAction a1 = new TestChannelAction( TestSystem.A.ordinal(), null, Action.ADD );
    final TestChannelAction a2 = new TestChannelAction( TestSystem.A.ordinal(), null, Action.REMOVE );
    final TestChannelAction a3 = new TestChannelAction( TestSystem.B.ordinal(), 72, Action.ADD );
    final TestChannelAction a4 = new TestChannelAction( TestSystem.B.ordinal(), 33, Action.REMOVE );
    final TestChangeSet changeSet1 =
      new TestChangeSet( 1,
                         mock( Runnable.class ),
                         new Change[ 0 ],
                         new ChannelAction[]{ a1, a2, a3, a4 } );

    final TestDataLoadService service = newService( changeSet1 );

    assertEquals( service.ensureSession().getLastRxSequence(), 0 );

    Replicant.context().createSubscription( new ChannelAddress( TestSystem.B, 33 ), null, true );

    configureRequests( service, service.getChangeSets() );
    service.ensureSession().enqueueDataLoad( "jsonData" );
    service.scheduleDataLoad();

    final LinkedList<DataLoadAction> actions = progressWorkTillDone( service, 8, 1 );

    final DataLoadAction action = actions.getLast();

    assertEquals( action.getChannelAddCount(), 2 );
    assertEquals( action.getChannelRemoveCount(), 2 );

    assertNull( Replicant.context().findSubscription( new ChannelAddress( TestSystem.A ) ) );
    assertNull( Replicant.context().findSubscription( new ChannelAddress( TestSystem.B, 33 ) ) );
    assertNotNull( Replicant.context().findSubscription( new ChannelAddress( TestSystem.B, 72 ) ) );
  }

  @Test
  public void isAreaOfInterestActionPending()
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();
    configureService( service );
    assertNotNull( service.getSession() );

    // type channel
    {
      service.ensureSession().getPendingAreaOfInterestActions().clear();

      final ChannelAddress channel1 = new ChannelAddress( TestSystem.A );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.ADD, channel1, null ) );
      assertEquals( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, channel1, null ), -1 );

      //Request a subscription so that it should be pending
      service.requestSubscribe( channel1, null );

      assertTrue( service.isAreaOfInterestActionPending( AreaOfInterestAction.ADD, channel1, null ) );
      assertEquals( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, channel1, null ),
                    1 );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.REMOVE, channel1, null ) );
      assertEquals( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, channel1, null ),
                    -1 );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.UPDATE, channel1, null ) );
      assertEquals( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.UPDATE, channel1, null ),
                    -1 );
    }

    // instance channel
    {
      service.ensureSession().getPendingAreaOfInterestActions().clear();

      final ChannelAddress channel2 = new ChannelAddress( TestSystem.B, 2 );
      final ChannelAddress channel2b = new ChannelAddress( TestSystem.B );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.ADD, channel2, null ) );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.ADD, channel2b, null ) );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.REMOVE, channel2, null ) );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.UPDATE, channel2, null ) );

      //Request a subscription so that it should be pending
      service.requestSubscribe( channel2, null );

      assertTrue( service.isAreaOfInterestActionPending( AreaOfInterestAction.ADD, channel2, null ) );
      assertEquals( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, channel2, null ),
                    1 );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.ADD, channel2b, null ) );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.REMOVE, channel2, null ) );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.UPDATE, channel2, null ) );
    }

    {
      service.ensureSession().getPendingAreaOfInterestActions().clear();

      final ChannelAddress channel3 = new ChannelAddress( TestSystem.C, 2 );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.ADD, channel3, null ) );
      final AreaOfInterestEntry entry =
        new AreaOfInterestEntry( channel3, AreaOfInterestAction.ADD, null );
      addAoiAction( service, entry );

      assertTrue( service.isAreaOfInterestActionPending( AreaOfInterestAction.ADD, channel3, null ) );
      assertEquals( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, channel3, null ),
                    0 );
    }

    // instance channel with filter
    {
      service.ensureSession().getPendingAreaOfInterestActions().clear();

      final Object filter = new Object();
      final ChannelAddress channel4 = new ChannelAddress( TestSystem.B, 55 );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.ADD, channel4, filter ) );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.REMOVE, channel4, filter ) );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.UPDATE, channel4, filter ) );

      //Request a subscription so that it should be pending
      service.requestSubscribe( channel4, filter );

      assertTrue( service.isAreaOfInterestActionPending( AreaOfInterestAction.ADD, channel4, filter ) );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.ADD, channel4, "OtherFilterKey" ) );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.ADD, channel4, null ) );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.REMOVE, channel4, filter ) );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.UPDATE, channel4, filter ) );
    }

    {
      service.ensureSession().getPendingAreaOfInterestActions().clear();

      final ChannelAddress channel1 = new ChannelAddress( TestSystem.B, 1 );
      final ChannelAddress channel2 = new ChannelAddress( TestSystem.B, 2 );
      final ChannelAddress channel3 = new ChannelAddress( TestSystem.B, 3 );
      final ChannelAddress channel4 = new ChannelAddress( TestSystem.B, 4 );

      service.requestSubscribe( channel1, null );
      service.requestSubscribe( channel2, null );
      service.requestSubscriptionUpdate( channel3, null );
      service.requestUnsubscribe( channel4 );

      assertTrue( service.isAreaOfInterestActionPending( AreaOfInterestAction.ADD, channel1, null ) );
      assertEquals( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, channel1, null ),
                    1 );

      assertTrue( service.isAreaOfInterestActionPending( AreaOfInterestAction.ADD, channel2, null ) );
      assertEquals( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, channel2, null ),
                    2 );

      assertTrue( service.isAreaOfInterestActionPending( AreaOfInterestAction.UPDATE, channel3, null ) );
      assertEquals( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.UPDATE, channel3, null ),
                    3 );

      assertTrue( service.isAreaOfInterestActionPending( AreaOfInterestAction.REMOVE, channel4, null ) );
      assertEquals( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, channel4, null ),
                    4 );

      service.requestUnsubscribe( channel4 );

      assertTrue( service.isAreaOfInterestActionPending( AreaOfInterestAction.REMOVE, channel4, null ) );
      assertEquals( service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, channel4, null ),
                    5 );
    }
  }

  @Test
  public void progressAreaOfInterestActions()
    throws Exception
  {
    final ChannelAddress channel1 = new ChannelAddress( TestSystem.A, 1 );
    final ChannelAddress channel2 = new ChannelAddress( TestSystem.B, 1 );

    final TestDataLoadService service = TestDataLoadService.create();
    configureService( service );
    assertNotNull( service.getSession() );

    assertEquals( false, service.progressAreaOfInterestActions() );
    assertEquals( 0, service.getCurrentAOIActions().size() );

    service.requestSubscribe( channel1, null );
    assertEquals( service.progressAreaOfInterestActions(), true );
    assertEquals( service.getCurrentAOIActions().size(), 1 );
    assertEquals( service.getCurrentAOIActions().get( 0 ).isInProgress(), true );
    assertEquals( service.getCurrentAOIActions().get( 0 ).getAddress(), channel1 );

    // Nothing to do until the first AOI completes
    assertEquals( false, service.progressAreaOfInterestActions() );
    assertEquals( service.getCurrentAOIActions().get( 0 ).getAddress(), channel1 );
    service.requestSubscribe( channel2, null );
    assertEquals( false, service.progressAreaOfInterestActions() );
    assertEquals( 1, service.getCurrentAOIActions().size() );
    assertEquals( service.getCurrentAOIActions().get( 0 ).getAddress(), channel1 );

    completeOutstandingAOIs( service );

    assertEquals( true, service.progressAreaOfInterestActions() );
    assertEquals( 1, service.getCurrentAOIActions().size() );
    assertEquals( service.getCurrentAOIActions().get( 0 ).getAddress(), channel2 );
  }

  @Test
  public void dontGroupActionsWithETags()
    throws Exception
  {
    final ChannelAddress channel1 = new ChannelAddress( TestSystem.A, 1 );
    final ChannelAddress channel2 = new ChannelAddress( TestSystem.A, 2 );
    final ChannelAddress channel3 = new ChannelAddress( TestSystem.A, 3 );

    final TestDataLoadService service = TestDataLoadService.create();
    configureService( service );
    assertNotNull( service.getSession() );

    service.requestSubscribe( channel1, null );
    service.requestSubscribe( channel2, null );
    service.requestSubscribe( channel3, null );

    final AreaOfInterestEntry channel2AOI = service.ensureSession().getPendingAreaOfInterestActions().get( 1 );
    when( service.getCacheService().lookup(
      eq( channel2AOI.getCacheKey() ) ) ).thenReturn(
      new CacheEntry( channel2AOI.getCacheKey(), "woo", "har" ) );

    assertEquals( service.progressAreaOfInterestActions(), true );
    assertEquals( service.getCurrentAOIActions().size(), 1 );
    assertEquals( service.getCurrentAOIActions().get( 0 ).isInProgress(), true );
    assertEquals( service.getCurrentAOIActions().get( 0 ).getAddress(), channel1 );

    completeOutstandingAOIs( service );
    assertEquals( service.progressAreaOfInterestActions(), true );
    assertEquals( service.getCurrentAOIActions().size(), 1 );
    assertEquals( service.getCurrentAOIActions().get( 0 ).isInProgress(), true );
    assertEquals( service.getCurrentAOIActions().get( 0 ).getAddress(), channel2 );

    completeOutstandingAOIs( service );
    assertEquals( service.progressAreaOfInterestActions(), true );
    assertEquals( service.getCurrentAOIActions().size(), 1 );
    assertEquals( service.getCurrentAOIActions().get( 0 ).isInProgress(), true );
    assertEquals( service.getCurrentAOIActions().get( 0 ).getAddress(), channel3 );
  }

  @Test
  public void progressBulkAreaOfInterestAddActions()
    throws Exception
  {
    final ChannelAddress channelA1 = new ChannelAddress( TestSystem.A, 1 );
    final ChannelAddress channelA2 = new ChannelAddress( TestSystem.A, 2 );
    final ChannelAddress channelA3 = new ChannelAddress( TestSystem.A, 3 );
    final ChannelAddress channelB1 = new ChannelAddress( TestSystem.B, 1 );
    final ChannelAddress channelB2 = new ChannelAddress( TestSystem.B, 2 );

    final TestDataLoadService service = TestDataLoadService.create();
    configureService( service );
    assertNotNull( service.getSession() );

    assertEquals( false, service.progressAreaOfInterestActions() );
    assertEquals( 0, service.getCurrentAOIActions().size() );

    service.requestSubscribe( channelA1, null );
    service.requestSubscribe( channelA2, null );
    service.requestSubscribe( channelB1, null );
    service.requestSubscribe( channelB2, null );
    service.requestSubscribe( channelA3, null );
    service.requestUnsubscribe( channelA1 );
    service.requestUnsubscribe( channelA2 );
    service.requestUnsubscribe( channelB1 );

    assertEquals( service.ensureSession().getPendingAreaOfInterestActions().size(), 8 );

    assertEquals( service.progressAreaOfInterestActions(), true );
    assertEquals( service.getCurrentAOIActions().size(), 2 );
    assertEquals( service.getCurrentAOIActions().get( 0 ).getAddress(), channelA1 );
    assertEquals( service.getCurrentAOIActions().get( 1 ).getAddress(), channelA2 );

    completeOutstandingAOIs( service );
    assertEquals( service.progressAreaOfInterestActions(), true );
    assertEquals( service.getCurrentAOIActions().size(), 2 );
    assertEquals( service.getCurrentAOIActions().get( 0 ).getAddress(), channelB1 );
    assertEquals( service.getCurrentAOIActions().get( 1 ).getAddress(), channelB2 );

    completeOutstandingAOIs( service );
    assertEquals( service.progressAreaOfInterestActions(), true );
    assertEquals( service.getCurrentAOIActions().size(), 1 );
    assertEquals( service.getCurrentAOIActions().get( 0 ).getAddress(), channelA3 );

    Replicant.context().createSubscription( channelA1, null, true );
    Replicant.context().createSubscription( channelA2, null, true );

    completeOutstandingAOIs( service );
    assertEquals( service.progressAreaOfInterestActions(), true );
    assertEquals( service.getCurrentAOIActions().size(), 2 );
    assertEquals( service.getCurrentAOIActions().get( 0 ).getAddress(), channelA1 );
    assertEquals( service.getCurrentAOIActions().get( 1 ).getAddress(), channelA2 );

    Replicant.context().createSubscription( channelB1, null, true );

    completeOutstandingAOIs( service );
    assertEquals( service.progressAreaOfInterestActions(), true );
    assertEquals( service.getCurrentAOIActions().size(), 1 );
    assertEquals( service.getCurrentAOIActions().get( 0 ).getAddress(), channelB1 );

    assertEquals( service.progressAreaOfInterestActions(), false );
    completeOutstandingAOIs( service );
    assertEquals( service.progressAreaOfInterestActions(), false );
  }

  @Test
  public void progressBulkAreaOfInterestAddActionsWithFilters()
    throws Exception
  {
    final ChannelAddress channelA1 = new ChannelAddress( TestSystem.A, 1 );
    final ChannelAddress channelA2 = new ChannelAddress( TestSystem.A, 2 );

    final TestDataLoadService service = TestDataLoadService.create();
    configureService( service );
    assertNotNull( service.getSession() );

    assertEquals( false, service.progressAreaOfInterestActions() );
    assertEquals( 0, service.getCurrentAOIActions().size() );

    service.requestSubscribe( channelA1, null );
    service.requestSubscribe( channelA2, "FilterA" );
    service.requestSubscriptionUpdate( channelA1, "FilterB" );
    service.requestSubscriptionUpdate( channelA2, "FilterB" );

    assertEquals( service.ensureSession().getPendingAreaOfInterestActions().size(), 4 );

    assertEquals( service.progressAreaOfInterestActions(), true );
    assertEquals( service.getCurrentAOIActions().size(), 1 );
    assertEquals( service.getCurrentAOIActions().get( 0 ).getAddress(), channelA1 );

    completeOutstandingAOIs( service );
    assertEquals( service.progressAreaOfInterestActions(), true );
    assertEquals( service.getCurrentAOIActions().size(), 1 );
    assertEquals( service.getCurrentAOIActions().get( 0 ).getAddress(), channelA2 );

    completeOutstandingAOIs( service );

    Replicant.context().createSubscription( channelA1, null, true );
    Replicant.context().createSubscription( channelA2, "FilterB", true );

    assertEquals( service.progressAreaOfInterestActions(), true );
    assertEquals( service.getCurrentAOIActions().size(), 2 );
    assertEquals( service.getCurrentAOIActions().get( 0 ).getAddress(), channelA1 );
    assertEquals( service.getCurrentAOIActions().get( 0 ).getFilterParameter(), "FilterB" );
    assertEquals( service.getCurrentAOIActions().get( 1 ).getAddress(), channelA2 );
    assertEquals( service.getCurrentAOIActions().get( 1 ).getFilterParameter(), "FilterB" );
  }

  @Test
  public void progressBulkAreaOfInterestAddActionsWithIgnorableActions()
    throws Exception
  {
    final ChannelAddress channelA1 = new ChannelAddress( TestSystem.A, 1 );
    final ChannelAddress channelA2 = new ChannelAddress( TestSystem.A, 2 );
    final ChannelAddress channelA3 = new ChannelAddress( TestSystem.A, 3 );

    final TestDataLoadService service = TestDataLoadService.create();
    configureService( service );
    assertNotNull( service.getSession() );

    final Subscription subscriptionA1 = Replicant.context().createSubscription( channelA1, "boo", true );
    final Subscription subscriptionA2 = Replicant.context().createSubscription( channelA2, "boo", true );

    service.requestSubscribe( channelA1, null );
    service.requestSubscribe( channelA2, null );

    assertEquals( service.ensureSession().getPendingAreaOfInterestActions().size(), 2 );
    assertEquals( service.progressAreaOfInterestActions(), true );
    assertEquals( service.getCurrentAOIActions().size(), 0 );
    assertEquals( service.ensureSession().getPendingAreaOfInterestActions().size(), 0 );

    Disposable.dispose( subscriptionA1 );
    Disposable.dispose( subscriptionA2 );

    service.requestSubscriptionUpdate( channelA1, "boo" );
    service.requestSubscriptionUpdate( channelA2, "boo" );

    assertEquals( service.ensureSession().getPendingAreaOfInterestActions().size(), 2 );
    assertEquals( service.progressAreaOfInterestActions(), true );
    assertEquals( service.getCurrentAOIActions().size(), 0 );
    assertEquals( service.ensureSession().getPendingAreaOfInterestActions().size(), 0 );

    service.requestUnsubscribe( channelA1 );
    service.requestUnsubscribe( channelA2 );
    service.requestUnsubscribe( channelA3 );

    assertEquals( service.ensureSession().getPendingAreaOfInterestActions().size(), 3 );
    assertEquals( service.progressAreaOfInterestActions(), true );
    assertEquals( service.getCurrentAOIActions().size(), 0 );
    assertEquals( service.ensureSession().getPendingAreaOfInterestActions().size(), 0 );
  }

  private void completeOutstandingAOIs( final TestDataLoadService service )
  {
    service.getCurrentAOIActions().clear();
  }

  private void verifyPostActionRun( final Runnable runnable )
  {
    verify( runnable ).run();
  }

  private void verifyPostActionNotRun( final Runnable runnable )
  {
    verify( runnable, never() ).run();
  }

  private void ensureEnqueueDataLoads( final TestDataLoadService service )
  {
    configureRequests( service, service.getChangeSets() );
    assertFalse( service.isScheduleDataLoadCalled() );
    for ( final TestChangeSet cs : service.getChangeSets() )
    {
      service.ensureSession().enqueueDataLoad( "BLAH:" + cs.getRequestID() );
    }
  }

  private void configureRequests( final TestDataLoadService service, final LinkedList<TestChangeSet> changeSets )
  {
    for ( final TestChangeSet changeSet : changeSets )
    {
      configureRequest( changeSet, service );
    }
  }

  @Nullable
  private RequestEntry configureRequest( final TestChangeSet changeSet, final TestDataLoadService service )
  {
    if ( changeSet.isResponseToRequest() )
    {
      final RequestEntry request =
        service.ensureSession().newRequest( "", changeSet.getCacheKey() );
      request.setNormalCompletionAction( changeSet.getRunnable() );
      changeSet.setRequestID( request.getRequestID() );
      return request;
    }
    return null;
  }

  private LinkedList<DataLoadAction> progressWorkTillDone( final TestDataLoadService service,
                                                           final int expectedStepCount,
                                                           final int expectedActions )
  {
    final LinkedList<DataLoadAction> actionsProcessed = new LinkedList<>();
    int count = 0;
    while ( true )
    {
      count++;
      final boolean moreWork = service.progressDataLoad();
      final DataLoadAction currentAction = service.getCurrentAction();
      if ( null != currentAction && !actionsProcessed.contains( currentAction ) )
      {
        actionsProcessed.add( currentAction );
      }
      if ( !moreWork )
      {
        break;
      }
    }

    assertFalse( service.progressDataLoad() );
    assertEquals( count, expectedStepCount );
    assertEquals( actionsProcessed.size(), expectedActions );

    return actionsProcessed;
  }

  private TestDataLoadService newService( final TestChangeSet... changeSet )
    throws Exception
  {
    final TestDataLoadService service = TestDataLoadService.create();
    configureService( service );
    service.setChangeSets( changeSet );
    return service;
  }

  private void configureService( final TestDataLoadService service )
    throws Exception
  {
    set( service, AbstractDataLoaderService.class, "_session", new ClientSession( ValueUtil.randomString() ) );

    service.setChangesToProcessPerTick( 1 );
    service.setLinksToProcessPerTick( 1 );
  }

  @SuppressWarnings( "SameParameterValue" )
  private void set( final Object instance, final Class<?> clazz, final String fieldName, final Object value )
    throws Exception
  {
    final Field field5 = clazz.getDeclaredField( fieldName );
    field5.setAccessible( true );
    field5.set( instance, value );
  }

  @SuppressWarnings( "unchecked" )
  private void addAoiAction( @Nonnull final AbstractDataLoaderService instance,
                             @Nonnull final AreaOfInterestEntry value )
    throws Exception
  {
    final Field field = AbstractDataLoaderService.class.getDeclaredField( "_currentAoiActions" );
    field.setAccessible( true );
    final List<AreaOfInterestEntry> currentAoiActions = (List<AreaOfInterestEntry>) field.get( instance );
    currentAoiActions.add( value );
  }

  private void assertNotInRequestManager( final TestDataLoadService service, final RequestEntry request )
  {
    assertNull( service.ensureSession().getRequest( request.getRequestID() ) );
  }

  private void assertInRequestManager( final TestDataLoadService service, final RequestEntry request )
  {
    assertNotNull( service.ensureSession().getRequest( request.getRequestID() ) );
  }
}
