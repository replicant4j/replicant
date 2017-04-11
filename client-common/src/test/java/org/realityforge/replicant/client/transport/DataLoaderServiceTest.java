package org.realityforge.replicant.client.transport;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.mockito.InOrder;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.client.Change;
import org.realityforge.replicant.client.ChangeMapper;
import org.realityforge.replicant.client.ChannelAction;
import org.realityforge.replicant.client.ChannelAction.Action;
import org.realityforge.replicant.client.ChannelDescriptor;
import org.realityforge.replicant.client.ChannelSubscriptionEntry;
import org.realityforge.replicant.client.EntityChangeBroker;
import org.realityforge.replicant.client.EntityRepository;
import org.realityforge.replicant.client.EntitySubscriptionEntry;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.client.Linkable;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

@SuppressWarnings( "NonJREEmulationClassesInClientCode" )
public class DataLoaderServiceTest
{
  @Test
  public void purgeSubscriptions()
    throws Exception
  {
    final TestDataLoadService service = new TestDataLoadService();
    configureService( service );
    final EntitySubscriptionManager sm = service.getSubscriptionManager();
    final EntityRepository repository = service.getRepository();
    final EntityChangeBroker broker = service.getChangeBroker();

    //LinkedHashSet means keys come out in "wrong" order
    // and will need to be resorted in purgeSubscriptions
    final HashSet<Enum> instanceGraphs = new LinkedHashSet<>();
    instanceGraphs.add( TestGraph.B );
    instanceGraphs.add( TestGraph.A );

    //LinkedHashSet means keys come out in "wrong" order
    // and will need to be resorted in purgeSubscriptions
    final HashSet<Enum> typeGraphs = new LinkedHashSet<>();
    typeGraphs.add( TestGraph.D );
    typeGraphs.add( TestGraph.C );

    final HashSet<Object> aGraph = new HashSet<>();
    aGraph.add( "1" );
    final HashSet<Object> bGraph = new HashSet<>();
    bGraph.add( "2" );

    final ChannelSubscriptionEntry entryA =
      new ChannelSubscriptionEntry( new ChannelDescriptor( TestGraph.A, "1" ), null, true );
    final ChannelSubscriptionEntry entryB =
      new ChannelSubscriptionEntry( new ChannelDescriptor( TestGraph.B, "2" ), null, true );
    final ChannelSubscriptionEntry entryC =
      new ChannelSubscriptionEntry( new ChannelDescriptor( TestGraph.C ), null, true );
    final ChannelSubscriptionEntry entryD =
      new ChannelSubscriptionEntry( new ChannelDescriptor( TestGraph.D ), null, true );

    insertSubscription( entryA, String.class, "A1" );
    insertSubscription( entryB, String.class, "B1" );
    insertSubscription( entryC, String.class, "C1" );
    insertSubscription( entryD, String.class, "D1" );

    when( sm.getInstanceSubscriptionKeys() ).thenReturn( instanceGraphs );
    when( sm.getInstanceSubscriptions( TestGraph.A ) ).thenReturn( aGraph );
    when( sm.getInstanceSubscriptions( TestGraph.B ) ).thenReturn( bGraph );
    when( sm.getTypeSubscriptions() ).thenReturn( typeGraphs );
    when( sm.removeSubscription( new ChannelDescriptor( TestGraph.A, "1" ) ) ).thenReturn( entryA );
    when( sm.removeSubscription( new ChannelDescriptor( TestGraph.B, "2" ) ) ).thenReturn( entryB );
    when( sm.removeSubscription( new ChannelDescriptor( TestGraph.C ) ) ).thenReturn( entryC );
    when( sm.removeSubscription( new ChannelDescriptor( TestGraph.D ) ) ).thenReturn( entryD );

    when( repository.deregisterEntity( String.class, "A1" ) ).thenReturn( "A1" );
    when( repository.deregisterEntity( String.class, "B1" ) ).thenReturn( "B1" );
    when( repository.deregisterEntity( String.class, "C1" ) ).thenReturn( "C1" );
    when( repository.deregisterEntity( String.class, "D1" ) ).thenReturn( "D1" );

    service.purgeSubscriptions();

    final InOrder inOrder = inOrder( repository, sm, broker );
    inOrder.verify( sm ).removeSubscription( new ChannelDescriptor( TestGraph.B, "2" ) );
    inOrder.verify( repository ).deregisterEntity( String.class, "B1" );
    inOrder.verify( broker ).removeAllChangeListeners( "B1" );
    inOrder.verify( sm ).removeSubscription( new ChannelDescriptor( TestGraph.A, "1" ) );
    inOrder.verify( repository ).deregisterEntity( String.class, "A1" );
    inOrder.verify( broker ).removeAllChangeListeners( "A1" );
    inOrder.verify( sm ).removeSubscription( new ChannelDescriptor( TestGraph.D ) );
    inOrder.verify( repository ).deregisterEntity( String.class, "D1" );
    inOrder.verify( broker ).removeAllChangeListeners( "D1" );
    inOrder.verify( sm ).removeSubscription( new ChannelDescriptor( TestGraph.C ) );
    inOrder.verify( repository ).deregisterEntity( String.class, "C1" );
    inOrder.verify( broker ).removeAllChangeListeners( "C1" );
    inOrder.verifyNoMoreInteractions();
  }

  private void insertSubscription( final ChannelSubscriptionEntry entry, final Class<?> type, final String id )
    throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
  {
    final Map<Class<?>, Map<Object, EntitySubscriptionEntry>> entities = getRwEntities( entry );
    final Map<Object, EntitySubscriptionEntry> typeMap = entities.computeIfAbsent( type, k -> new HashMap<>() );
    final EntitySubscriptionEntry entitySubscriptionEntry = new EntitySubscriptionEntry( type, id );
    getRwGraphSubscriptions( entitySubscriptionEntry ).put( entry.getDescriptor(), entry );
    typeMap.put( id, entitySubscriptionEntry );
  }

  @SuppressWarnings( "unchecked" )
  private Map<ChannelDescriptor, ChannelSubscriptionEntry> getRwGraphSubscriptions( final EntitySubscriptionEntry entry )
    throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
  {
    final Method method = entry.getClass().getDeclaredMethod( "getRwGraphSubscriptions" );
    method.setAccessible( true );
    return (Map<ChannelDescriptor, ChannelSubscriptionEntry>) method.invoke( entry );
  }

  @SuppressWarnings( "unchecked" )
  private Map<Class<?>, Map<Object, EntitySubscriptionEntry>> getRwEntities( final ChannelSubscriptionEntry entry )
    throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
  {
    final Method method = entry.getClass().getDeclaredMethod( "getRwEntities" );
    method.setAccessible( true );
    return (Map<Class<?>, Map<Object, EntitySubscriptionEntry>>) method.invoke( entry );
  }

  @Test
  public void setSession()
    throws Exception
  {
    final TestChangeSet changeSet =
      new TestChangeSet( 1, mock( Runnable.class ), true, new Change[ 0 ] );
    final TestDataLoadService service = newService( changeSet );
    final Runnable runnable1 = mock( Runnable.class );
    final String sessionID = ValueUtil.randomString();
    final ClientSession session1 = new ClientSession( service, ValueUtil.randomString() );

    session1.enqueueOOB( sessionID, null, false );

    assertEquals( service.getSessionContext().getSession(), null );

    when( service.getChangeBroker().isEnabled() ).thenReturn( true );

    service.setSession( session1, runnable1 );

    assertEquals( service.getSessionContext().getSession(), session1 );
    assertEquals( service.getSession(), session1 );
    verify( runnable1, times( 1 ) ).run();
    verify( service.getChangeBroker(), times( 1 ) ).disable( "TestGraph" );
    verify( service.getChangeBroker(), times( 1 ) ).enable( "TestGraph" );

    // Should be no oob actions left
    progressWorkTillDone( service, 7, 1 );

    service.setSession( session1, runnable1 );
    verify( runnable1, times( 2 ) ).run();
    // The following should not run as session is the same
    verify( service.getChangeBroker(), times( 1 ) ).disable( "TestGraph" );
    verify( service.getChangeBroker(), times( 1 ) ).enable( "TestGraph" );
  }

  @Test
  public void getTerminateCount()
    throws Exception
  {
    final TestChangeSet changeSet = new TestChangeSet( 1, mock( Runnable.class ), true, new Change[ 0 ] );
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
  public void verifyDataLoader_bulkDataLoad()
    throws Exception
  {
    final Linkable entity = mock( Linkable.class );
    final TestChangeSet changeSet =
      new TestChangeSet( 1, mock( Runnable.class ), true, new Change[]{ new TestChange( true ) } );

    final TestDataLoadService service = newService( changeSet );

    when( service.getChangeMapper().applyChange( changeSet.getChange( 0 ) ) ).thenReturn( entity );

    assertEquals( service.ensureSession().getLastRxSequence(), 0 );

    ensureEnqueueDataLoads( service );

    final RequestEntry request = ensureRequest( service, changeSet );

    progressWorkTillDone( service, 10, 1 );

    // Termination count is actually 2 as progressWorkTillDone will attempt to progress
    // once after it is initially terminates
    assertEquals( service.getTerminateCount(), 2 );

    assertEquals( service.ensureSession().getLastRxSequence(), changeSet.getSequence() );

    verify( service.getEntityRepositoryValidator(), times( 1 ) ).validate( service.getRepository() );
    verify( service.getChangeBroker() ).disable( "TestGraph" );
    verify( service.getChangeBroker() ).enable( "TestGraph" );
    assertTrue( service.isBulkLoadCompleteCalled() );
    assertFalse( service.isIncrementalLoadCompleteCalled() );

    assertTrue( service.isDataLoadComplete() );
    assertTrue( service.getStatus().isBulkLoad() );

    verify( service.getChangeMapper() ).applyChange( changeSet.getChange( 0 ) );
    verify( entity ).link();

    assertRequestProcessed( service, request );
  }

  @Test
  public void getSessionID()
    throws Exception
  {
    final TestDataLoadService service = newService( new TestChangeSet[ 0 ], true );
    assertEquals( service.getSessionID(), service.ensureSession().getSessionID() );
  }

  @Test
  public void cache_requestWithCacheKeyAndETag()
    throws Exception
  {
    final TestChangeSet changeSet =
      new TestChangeSet( 1, mock( Runnable.class ), true, new Change[ 0 ] );
    changeSet.setCacheKey( "MetaData" );
    changeSet.setEtag( "1 Jan 2020" );

    final TestDataLoadService service = newService( changeSet );
    final CacheService cacheService = service.getCacheService();

    assertEquals( service.ensureSession().getLastRxSequence(), 0 );

    service.ensureSession().enqueueDataLoad( "Data" );
    final RequestEntry request = configureRequest( changeSet, service );
    assertNotNull( request );

    progressWorkTillDone( service, 7, 1 );

    assertEquals( service.ensureSession().getLastRxSequence(), changeSet.getSequence() );

    verify( service.getEntityRepositoryValidator(), times( 1 ) ).validate( service.getRepository() );
    verify( cacheService ).store( "MetaData", "1 Jan 2020", "Data" );

    assertRequestProcessed( service, request );
  }

  @Test
  public void cache_withOOB()
    throws Exception
  {
    final TestChangeSet changeSet =
      new TestChangeSet( 1, mock( Runnable.class ), true, new Change[ 0 ] );
    changeSet.setCacheKey( "MetaData" );
    changeSet.setEtag( "1 Jan 2020" );

    final TestDataLoadService service = newService( changeSet );
    final CacheService cacheService = service.getCacheService();

    assertEquals( service.ensureSession().getLastRxSequence(), 0 );

    service.ensureSession().enqueueOOB( "Data", changeSet.getRunnable(), changeSet.isBulkChange() );

    progressWorkTillDone( service, 7, 1 );

    assertEquals( service.ensureSession().getLastRxSequence(), 0 );

    verify( service.getEntityRepositoryValidator(), times( 1 ) ).validate( service.getRepository() );
    verify( cacheService, never() ).store( anyString(), anyString(), anyString() );
  }

  private void assertRequestProcessed( final TestDataLoadService service, final RequestEntry request )
  {
    assertTrue( request.haveResultsArrived() );
    assertNotInRequestManager( service, request );
    assertTrue( service.isDataLoadComplete() );
    assertNotNull( service.getStatus().getRequestID() );

    verifyPostActionRun( request.getRunnable() );
  }

  @Test
  public void enqueueOOB()
    throws Exception
  {
    final Linkable entity = mock( Linkable.class );
    final TestChangeSet changeSet =
      new TestChangeSet( 1, mock( Runnable.class ), true, new Change[]{ new TestChange( true ) } );

    final TestDataLoadService service = newService( changeSet );

    when( service.getChangeMapper().applyChange( changeSet.getChange( 0 ) ) ).thenReturn( entity );

    assertEquals( service.ensureSession().getLastRxSequence(), 0 );

    assertFalse( service.isScheduleDataLoadCalled() );
    for ( final TestChangeSet cs : service.getChangeSets() )
    {
      service.ensureSession()
        .enqueueOOB( "BLAH:" + cs.getSequence(), changeSet.getRunnable(), changeSet.isBulkChange() );
    }
    assertTrue( service.isScheduleDataLoadCalled() );

    progressWorkTillDone( service, 10, 1 );

    assertEquals( service.ensureSession().getLastRxSequence(), 0 );

    verify( service.getEntityRepositoryValidator(), times( 1 ) ).validate( service.getRepository() );
    verify( service.getChangeBroker() ).disable( "TestGraph" );
    verify( service.getChangeBroker() ).enable( "TestGraph" );
    assertTrue( service.isBulkLoadCompleteCalled() );
    assertFalse( service.isIncrementalLoadCompleteCalled() );

    assertTrue( service.isDataLoadComplete() );
    assertTrue( service.getStatus().isBulkLoad() );
    assertNull( service.getStatus().getRequestID() );

    verify( service.getChangeMapper() ).applyChange( changeSet.getChange( 0 ) );
    verify( entity ).link();

    verifyPostActionRun( changeSet.getRunnable() );
  }

  @Test
  public void ordering()
    throws Exception
  {
    final TestChangeSet cs1 = new TestChangeSet( 1, null, true, new Change[ 0 ] );
    final TestChangeSet cs2 = new TestChangeSet( 2, null, true, new Change[ 0 ] );
    final TestChangeSet cs3 = new TestChangeSet( 3, null, true, new Change[ 0 ] );

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
      new TestChangeSet( 1, mock( Runnable.class ), true, new Change[ 0 ] );

    final TestDataLoadService service = newService( changeSet );

    final RequestEntry request =
      service.ensureSession().newRequestRegistration( "", null, changeSet.isBulkChange() );
    changeSet.setRequestID( request.getRequestID() );
    service.ensureSession().enqueueDataLoad( "blah" );

    progressWorkTillDone( service, 7, 1 );

    assertTrue( request.haveResultsArrived() );
    final String requestID = changeSet.getRequestID();
    assertNotNull( requestID );
    assertInRequestManager( service, request );
    assertNotNull( service.getStatus().getRequestID() );

    verifyPostActionNotRun( changeSet.getRunnable() );
  }

  @Test
  public void verifyDataLoader_dataLoadWithZeroChanges()
    throws Exception
  {
    final TestChangeSet changeSet = new TestChangeSet( 1, mock( Runnable.class ), false, new Change[ 0 ] );

    final TestDataLoadService service = newService( changeSet );

    assertEquals( service.ensureSession().getLastRxSequence(), 0 );

    ensureEnqueueDataLoads( service );

    progressWorkTillDone( service, 7, 1 );

    assertEquals( service.ensureSession().getLastRxSequence(), changeSet.getSequence() );

    verify( service.getEntityRepositoryValidator(), times( 1 ) ).validate( service.getRepository() );
    verify( service.getChangeBroker(), never() ).disable( "TEST" );
    verify( service.getChangeBroker(), never() ).enable( "TEST" );
    assertFalse( service.isBulkLoadCompleteCalled() );
    assertTrue( service.isIncrementalLoadCompleteCalled() );

    assertTrue( service.isDataLoadComplete() );
    assertFalse( service.getStatus().isBulkLoad() );
    assertNotNull( service.getStatus().getRequestID() );

    verifyPostActionRun( changeSet.getRunnable() );
  }

  @Test
  public void verifyDeletedEntityIsNotLinked()
    throws Exception
  {
    final Linkable entity = mock( Linkable.class );
    final TestChangeSet changeSet = new TestChangeSet( 1, null, true, new Change[]{ new TestChange( false ) } );

    final TestDataLoadService service = newService( changeSet );

    when( service.getChangeMapper().applyChange( changeSet.getChange( 0 ) ) ).thenReturn( entity );

    ensureEnqueueDataLoads( service );

    progressWorkTillDone( service, 9, 1 );

    verify( service.getEntityRepositoryValidator(), times( 1 ) ).validate( service.getRepository() );
    verify( service.getChangeMapper() ).applyChange( changeSet.getChange( 0 ) );
    verify( entity, never() ).link();
  }

  @Test
  public void verifyIncrementalChangeInvokesCorrectMethods()
    throws Exception
  {
    final TestChangeSet changeSet = new TestChangeSet( 1, null, false, new Change[]{ new TestChange( true ) } );

    final TestDataLoadService service = newService( changeSet );

    assertEquals( service.ensureSession().getLastRxSequence(), 0 );

    ensureEnqueueDataLoads( service );

    progressWorkTillDone( service, 9, 1 );

    verify( service.getEntityRepositoryValidator(), times( 1 ) ).validate( service.getRepository() );

    assertEquals( service.ensureSession().getLastRxSequence(), changeSet.getSequence() );

    final EntityChangeBroker changeBroker = service.getChangeBroker();
    verify( changeBroker ).pause( "TestGraph" );
    verify( changeBroker ).resume( "TestGraph" );
    assertFalse( service.isBulkLoadCompleteCalled() );
    assertTrue( service.isIncrementalLoadCompleteCalled() );
  }

  @Test
  public void verifyValidateIsNotCalled()
    throws Exception
  {
    final TestChangeSet changeSet = new TestChangeSet( 1, null, false, new Change[]{ new TestChange( true ) } );

    final TestDataLoadService service = newService( new TestChangeSet[]{ changeSet, changeSet }, false );

    ensureEnqueueDataLoads( service );

    progressWorkTillDone( service, 11, 2 );

    verify( service.getEntityRepositoryValidator(), never() ).validate( service.getRepository() );
  }

  @Test
  public void verifyDataLoader_sequenceFollowed()
    throws Exception
  {
    final Linkable entity = mock( Linkable.class );
    final TestChangeSet changeSet1 =
      new TestChangeSet( 1, mock( Runnable.class ), true, new Change[]{ new TestChange( true ) } );
    final TestChangeSet changeSet2 =
      new TestChangeSet( 2, mock( Runnable.class ), true, new Change[]{ new TestChange( true ) } );

    final TestDataLoadService service = newService( new TestChangeSet[]{ changeSet2, changeSet1 }, true );
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
    progressWorkTillDone( service, 17, 2 );

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
                         true,
                         new Change[ 0 ],
                         new ChannelAction[]{ new TestChannelAction( TestGraph.B.ordinal(), "S", Action.ADD ) } );

    final TestDataLoadService service = newService( new TestChangeSet[]{ changeSet1 }, true );

    assertEquals( service.ensureSession().getLastRxSequence(), 0 );

    configureRequests( service, service.getChangeSets() );
    service.ensureSession().enqueueDataLoad( "jsonData" );
    service.scheduleDataLoad();

    final LinkedList<DataLoadAction> actions = progressWorkTillDone( service, 8, 1 );
    verify( service.getSubscriptionManager() ).
      recordSubscription( new ChannelDescriptor( TestGraph.B, "S" ), null, false );

    final DataLoadAction action = actions.getLast();

    assertEquals( action.getChannelAddCount(), 1 );
    assertEquals( action.getChannelRemoveCount(), 0 );
  }

  @Test
  public void multipleChannelActions()
    throws Exception
  {
    final TestChannelAction a1 = new TestChannelAction( TestGraph.A.ordinal(), null, Action.ADD );
    final TestChannelAction a2 = new TestChannelAction( TestGraph.A.ordinal(), null, Action.REMOVE );
    final TestChannelAction a3 = new TestChannelAction( TestGraph.B.ordinal(), "S", Action.ADD );
    final TestChannelAction a4 = new TestChannelAction( TestGraph.B.ordinal(), 33, Action.REMOVE );
    final TestChangeSet changeSet1 =
      new TestChangeSet( 1,
                         mock( Runnable.class ),
                         true,
                         new Change[ 0 ],
                         new ChannelAction[]{ a1, a2, a3, a4 } );

    final TestDataLoadService service = newService( new TestChangeSet[]{ changeSet1 }, true );

    assertEquals( service.ensureSession().getLastRxSequence(), 0 );

    when( service.getSubscriptionManager().removeSubscription( new ChannelDescriptor( TestGraph.A ) ) ).
      thenReturn( new ChannelSubscriptionEntry( new ChannelDescriptor( TestGraph.A ), null, true ) );
    when( service.getSubscriptionManager().removeSubscription( new ChannelDescriptor( TestGraph.B, 33 ) ) ).
      thenReturn( new ChannelSubscriptionEntry( new ChannelDescriptor( TestGraph.B, 33 ), null, true ) );
    configureRequests( service, service.getChangeSets() );
    service.ensureSession().enqueueDataLoad( "jsonData" );
    service.scheduleDataLoad();

    final LinkedList<DataLoadAction> actions = progressWorkTillDone( service, 8, 1 );

    final DataLoadAction action = actions.getLast();

    assertEquals( action.getChannelAddCount(), 2 );
    assertEquals( action.getChannelRemoveCount(), 2 );

    final InOrder inOrder = inOrder( service.getSubscriptionManager() );
    inOrder.verify( service.getSubscriptionManager() ).
      recordSubscription( new ChannelDescriptor( TestGraph.A ), null, false );
    inOrder.verify( service.getSubscriptionManager() ).removeSubscription( new ChannelDescriptor( TestGraph.A ) );
    inOrder.verify( service.getSubscriptionManager() ).
      recordSubscription( new ChannelDescriptor( TestGraph.B, "S" ), null, false );
    inOrder.verify( service.getSubscriptionManager() ).removeSubscription( new ChannelDescriptor( TestGraph.B, 33 ) );
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void isAreaOfInterestActionPending()
    throws Exception
  {
    final TestDataLoadService service = new TestDataLoadService();
    configureService( service );
    assertNotNull( service.getSession() );

    // type graph
    {
      final ChannelDescriptor channel1 = new ChannelDescriptor( TestGraph.A );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.ADD, channel1, null ) );

      //Request a subscription so that it should be pending
      service.requestSubscribe( channel1, null );

      assertTrue( service.isAreaOfInterestActionPending( AreaOfInterestAction.ADD, channel1, null ) );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.REMOVE, channel1, null ) );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.UPDATE, channel1, null ) );
    }

    // instance graph
    {
      final ChannelDescriptor channel2 = new ChannelDescriptor( TestGraph.B, 2 );
      final ChannelDescriptor channel2b = new ChannelDescriptor( TestGraph.B );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.ADD, channel2, null ) );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.ADD, channel2b, null ) );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.REMOVE, channel2, null ) );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.UPDATE, channel2, null ) );

      //Request a subscription so that it should be pending
      service.requestSubscribe( channel2, null );

      assertTrue( service.isAreaOfInterestActionPending( AreaOfInterestAction.ADD, channel2, null ) );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.ADD, channel2b, null ) );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.REMOVE, channel2, null ) );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.UPDATE, channel2, null ) );
    }

    {
      final ChannelDescriptor channel3 = new ChannelDescriptor( TestGraph.C, 2 );
      assertFalse( service.isAreaOfInterestActionPending( AreaOfInterestAction.ADD, channel3, null ) );
      final AreaOfInterestEntry entry =
        new AreaOfInterestEntry( service.getKey(), channel3, AreaOfInterestAction.ADD, null );
      set( service, AbstractDataLoaderService.class, "_currentAoiAction", entry );

      assertTrue( service.isAreaOfInterestActionPending( AreaOfInterestAction.ADD, channel3, null ) );
    }

    // instance graph with filter
    {
      final Object filter = new Object();
      final ChannelDescriptor channel4 = new ChannelDescriptor( TestGraph.B, 55 );
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
    assertTrue( service.isScheduleDataLoadCalled() );
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
        service.ensureSession().newRequestRegistration( "", changeSet.getCacheKey(), changeSet.isBulkChange() );
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

  private TestDataLoadService newService( final TestChangeSet changeSet )
    throws Exception
  {
    final TestDataLoadService service = new TestDataLoadService();
    configureService( service );
    service.setValidateOnLoad( true );
    service.setChangeSets( changeSet );
    return service;
  }

  private TestDataLoadService newService( final TestChangeSet[] changeSets, final boolean validateOnLoad )
    throws Exception
  {
    final TestDataLoadService service = new TestDataLoadService();
    configureService( service );
    service.setValidateOnLoad( validateOnLoad );
    service.setChangeSets( changeSets );
    return service;
  }

  private void configureService( final TestDataLoadService service )
    throws Exception
  {
    set( service, AbstractDataLoaderService.class, "_session", new ClientSession( service, ValueUtil.randomString() ) );

    service.setChangesToProcessPerTick( 1 );
    service.setLinksToProcessPerTick( 1 );
  }

  private void set( final Object instance, final Class<?> clazz, final String fieldName, final Object value )
    throws Exception
  {
    final Field field5 = clazz.getDeclaredField( fieldName );
    field5.setAccessible( true );
    field5.set( instance, value );

  }

  private void assertNotInRequestManager( final TestDataLoadService service, final RequestEntry request )
  {
    assertNull( service.ensureSession().getRequest( request.getRequestID() ) );
  }

  private void assertInRequestManager( final TestDataLoadService service, final RequestEntry request )
  {
    assertNotNull( service.ensureSession().getRequest( request.getRequestID() ) );
  }

  private RequestEntry ensureRequest( final TestDataLoadService service, final TestChangeSet changeSet )
  {
    final String requestID = changeSet.getRequestID();
    assertNotNull( requestID );
    final RequestEntry request = service.ensureSession().getRequest( requestID );
    assertNotNull( request );
    return request;
  }
}
