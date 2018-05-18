package org.realityforge.replicant.client.transport;

import arez.Disposable;
import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.ITestResult;
import replicant.AbstractReplicantTest;

@SuppressWarnings( "NonJREEmulationClassesInClientCode" )
public class DataLoaderServiceTest
  extends AbstractReplicantTest
  implements IHookable
{
  @Override
  public void run( final IHookCallBack callBack, final ITestResult testResult )
  {
    safeAction( () -> callBack.runTestMethod( testResult ) );
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
/*
  @Test
  public void setConnection()
    throws Exception
  {
    final TestChangeSet changeSet =
      new TestChangeSet( ChangeSet.create( 1, null, null, new ChannelChange[ 0 ], new EntityChange[ 0 ] ),
                         mock( Runnable.class ) );
    final TestDataLoadService service = newService( changeSet );
    final SafeProcedure runnable1 = mock( SafeProcedure.class );
    final String connectionId = ValueUtil.randomString();
    final Connection session1 = new Connection( ValueUtil.randomString() );

    session1.enqueueOutOfBandResponse( connectionId, null );

    assertEquals( service.getSessionContext().getConnection(), null );

    service.setConnection( session1, runnable1 );

    assertEquals( service.getSessionContext().getConnection(), session1 );
    assertEquals( service.getConnection(), session1 );
    verify( runnable1, times( 1 ) ).call();

    // Should be no oob actions left
    progressWorkTillDone( service, 7, 1 );

    service.setConnection( session1, runnable1 );
    verify( runnable1, times( 2 ) ).call();
  }

  @Test
  public void getTerminateCount()
    throws Exception
  {
    final TestChangeSet changeSet = new TestChangeSet( ChangeSet.create( 1, null, null, new ChannelChange[ 0 ],
                                                                         new EntityChange[ 0 ] ), mock( Runnable.class ) );
    final TestDataLoadService service = newService( changeSet );
    ensureEnqueueDataLoads( service );

    for ( int i = 0; i < 6; i++ )
    {
      assertTrue( service.progressResponseProcessing() );
      assertEquals( service.getTerminateCount(), 0 );
    }

    assertFalse( service.progressResponseProcessing() );
    assertEquals( service.getTerminateCount(), 1 );
  }

  @Test
  public void cache_requestWithCacheKeyAndETag()
    throws Exception
  {
    final String cacheKey = ValueUtil.randomString();
    final String etag = ValueUtil.randomString();
    final TestChangeSet changeSet =
      new TestChangeSet( ChangeSet.create( 1, null, etag, new ChannelChange[ 0 ], new EntityChange[ 0 ] ),
                         mock( Runnable.class ) );

    changeSet.setCacheKey( cacheKey );
    changeSet.setEtag( etag );

    final TestDataLoadService service = newService( changeSet );
    final CacheService cacheService = service.getCacheService();

    assertEquals( service.ensureConnection().getLastRxSequence(), 0 );

    service.ensureConnection().enqueueResponse( "Data" );
    final RequestEntry request = configureRequest( changeSet, service );
    assertNotNull( request );

    progressWorkTillDone( service, 7, 1 );

    assertEquals( service.ensureConnection().getLastRxSequence(), changeSet.getSequence() );
    assertEquals( service.getValidateRepositoryCallCount(), 1 );

    verify( cacheService ).store( cacheKey, etag, "Data" );

    assertRequestProcessed( service, request );
  }

  @Test
  public void cache_withOOB()
    throws Exception
  {
    final TestChangeSet changeSet =
      new TestChangeSet( ChangeSet.create( 1, null, null, new ChannelChange[ 0 ], new EntityChange[ 0 ] ),
                         mock( Runnable.class ) );
    changeSet.setCacheKey( ValueUtil.randomString() );
    changeSet.setEtag( ValueUtil.randomString() );

    final TestDataLoadService service = newService( changeSet );
    final CacheService cacheService = service.getCacheService();

    assertEquals( service.ensureConnection().getLastRxSequence(), 0 );

    service.ensureConnection().enqueueOutOfBandResponse( "Data", changeSet.getRunnable() );

    progressWorkTillDone( service, 7, 1 );

    assertEquals( service.ensureConnection().getLastRxSequence(), 0 );

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
  public void enqueueOutOfBandResponse()
    throws Exception
  {
    final Linkable entity = mock( Linkable.class );
    final TestChangeSet changeSet =
      new TestChangeSet( ChangeSet.create( 1, null, null, new ChannelChange[ 0 ],
                                           new EntityChange[]{ new TestChange( true ) } ), mock( Runnable.class ) );

    final TestDataLoadService service = newService( changeSet );

    when( service.getChangeMapper().applyChange( changeSet.getChange( 0 ) ) ).thenReturn( entity );

    assertEquals( service.ensureConnection().getLastRxSequence(), 0 );

    assertFalse( service.isScheduleDataLoadCalled() );
    for ( final TestChangeSet cs : service.getChangeSets() )
    {
      service.ensureConnection().enqueueOutOfBandResponse( "BLAH:" + cs.getSequence(), changeSet.getRunnable() );
    }

    progressWorkTillDone( service, 9, 1 );

    assertEquals( service.ensureConnection().getLastRxSequence(), 0 );

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
    final TestChangeSet cs1 = new TestChangeSet( ChangeSet.create( 1, null, null, new ChannelChange[ 0 ],
                                                                   new EntityChange[ 0 ] ), null );
    final TestChangeSet cs2 = new TestChangeSet( ChangeSet.create( 2, null, null, new ChannelChange[ 0 ],
                                                                   new EntityChange[ 0 ] ), null );
    final TestChangeSet cs3 = new TestChangeSet( ChangeSet.create( 3, null, null, new ChannelChange[ 0 ],
                                                                   new EntityChange[ 0 ] ), null );

    final MessageResponse oob1 = new MessageResponse( "oob1", true );
    final MessageResponse oob2 = new MessageResponse( "oob2", true );
    final MessageResponse oob3 = new MessageResponse( "oob3", true );

    final MessageResponse s1 = new MessageResponse( "s1", false );
    s1.recordChangeSet( cs1, null );
    final MessageResponse s2 = new MessageResponse( "s2", false );
    s2.recordChangeSet( cs2, null );
    final MessageResponse s3 = new MessageResponse( "s3", false );
    s3.recordChangeSet( cs3, null );

    final List<MessageResponse> l1 = Arrays.asList( s2, s3, s1, oob1, oob2, oob3 );
    Collections.sort( l1 );
    assertEquals( l1, Arrays.asList( oob1, oob2, oob3, s1, s2, s3 ) );
  }

  @Test
  public void verifyDataLoader_doesNotRemoveRequestIfNotYetHandled()
    throws Exception
  {
    final TestChangeSet changeSet =
      new TestChangeSet( ChangeSet.create( 1, null, null, new ChannelChange[ 0 ], new EntityChange[ 0 ] ),
                         mock( Runnable.class ) );

    final TestDataLoadService service = newService( changeSet );

    final RequestEntry request =
      service.ensureConnection().newRequest( "", null );
    changeSet.setRequestID( request.getRequestID() );
    service.ensureConnection().enqueueResponse( "blah" );

    progressWorkTillDone( service, 7, 1 );

    assertTrue( request.haveResultsArrived() );
    final String requestID = changeSet.getRequestID();
    assertNotNull( requestID );
    assertInRequestManager( service, request );
    assertNotNull( service.getStatus().getRequestId() );
  }

  @Test
  public void verifyDataLoader_dataLoadWithZeroChanges()
    throws Exception
  {
    final TestChangeSet changeSet = new TestChangeSet( ChangeSet.create( 1, null, null, new ChannelChange[ 0 ],
                                                                         new EntityChange[ 0 ] ), mock( Runnable.class ) );

    final TestDataLoadService service = newService( changeSet );

    assertEquals( service.ensureConnection().getLastRxSequence(), 0 );

    ensureEnqueueDataLoads( service );

    progressWorkTillDone( service, 7, 1 );

    assertEquals( service.ensureConnection().getLastRxSequence(), changeSet.getSequence() );

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
    final ChangeSet changeSet = new TestChangeSet( ChangeSet.create( 1, null, null, new ChannelChange[ 0 ],
                                                                     new EntityChange[]{ new TestChange( false ) } ), null );

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
    final TestChangeSet changeSet = new TestChangeSet( ChangeSet.create( 1, null, null, new ChannelChange[ 0 ],
                                                                         new EntityChange[]{ new TestChange( true ) } ), null );

    final TestDataLoadService service = newService( changeSet );

    assertEquals( service.ensureConnection().getLastRxSequence(), 0 );

    ensureEnqueueDataLoads( service );

    progressWorkTillDone( service, 8, 1 );

    assertEquals( service.getValidateRepositoryCallCount(), 1 );

    assertEquals( service.ensureConnection().getLastRxSequence(), changeSet.getSequence() );
  }

  @Test
  public void verifyValidateIsNotCalled()
    throws Exception
  {
    ReplicantTestUtil.noValidateRepositoryOnLoad();
    final TestChangeSet changeSet = new TestChangeSet( ChangeSet.create( 1, null, null, new ChannelChange[ 0 ],
                                                                         new EntityChange[]{ new TestChange( true ) } ), null );

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
      new TestChangeSet( ChangeSet.create( 1, null, null, new ChannelChange[ 0 ],
                                           new EntityChange[]{ new TestChange( true ) } ), null );
    final TestChangeSet changeSet2 =
      new TestChangeSet( ChangeSet.create( 2, null, null, new ChannelChange[ 0 ],
                                           new EntityChange[]{ new TestChange( true ) } ), null );

    final TestDataLoadService service = newService( changeSet2, changeSet1 );
    final ChangeMapper changeMapper = service.getChangeMapper();
    when( changeMapper.applyChange( changeSet1.getChange( 0 ) ) ).thenReturn( entity );
    when( changeMapper.applyChange( changeSet2.getChange( 0 ) ) ).thenReturn( entity );

    assertEquals( service.ensureConnection().getLastRxSequence(), 0 );

    configureRequests( service, service.getChangeSets() );
    service.ensureConnection().enqueueResponse( "jsonData" );
    progressWorkTillDone( service, 3, 1 );

    //No progress should have been made other than parsing packet as out of sequence
    assertEquals( service.ensureConnection().getLastRxSequence(), 0 );

    service.ensureConnection().enqueueResponse( "jsonData" );
    progressWorkTillDone( service, 15, 2 );

    //Progress should have been made as all sequence appears
    assertEquals( service.ensureConnection().getLastRxSequence(), 2 );
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
                         new EntityChange[ 0 ],
                         new ChannelChange[]{ new TestChannelAction( TestSystem.B.ordinal(), 72, Action.ADD ) } );

    final TestDataLoadService service = newService( changeSet1 );

    assertEquals( service.ensureConnection().getLastRxSequence(), 0 );

    configureRequests( service, service.getChangeSets() );
    service.ensureConnection().enqueueResponse( "jsonData" );
    service.triggerScheduler();

    final LinkedList<MessageResponse> actions = progressWorkTillDone( service, 8, 1 );
    final ChannelAddress address = new ChannelAddress( TestSystem.B, 72 );

    final Subscription subscription = Replicant.context().findSubscription( address );
    assertNotNull( subscription );
    assertEquals( subscription.getAddress(), address );
    assertEquals( subscription.getFilter(), null );
    assertEquals( subscription.isExplicitSubscription(), false );

    final MessageResponse action = actions.getLast();

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
                         new EntityChange[ 0 ],
                         new ChannelChange[]{ a1, a2, a3, a4 } );

    final TestDataLoadService service = newService( changeSet1 );

    assertEquals( service.ensureConnection().getLastRxSequence(), 0 );

    Replicant.context().createSubscription( new ChannelAddress( TestSystem.B, 33 ), null, true );

    configureRequests( service, service.getChangeSets() );
    service.ensureConnection().enqueueResponse( "jsonData" );
    service.triggerScheduler();

    final LinkedList<MessageResponse> actions = progressWorkTillDone( service, 8, 1 );

    final MessageResponse action = actions.getLast();

    assertEquals( action.getChannelAddCount(), 2 );
    assertEquals( action.getChannelRemoveCount(), 2 );

    assertNull( Replicant.context().findSubscription( new ChannelAddress( TestSystem.A ) ) );
    assertNull( Replicant.context().findSubscription( new ChannelAddress( TestSystem.B, 33 ) ) );
    assertNotNull( Replicant.context().findSubscription( new ChannelAddress( TestSystem.B, 72 ) ) );
  }

  @Test
  public void progressAreaOfInterestRequestProcessing()
    throws Exception
  {
    final ChannelAddress channel1 = new ChannelAddress( TestSystem.A, 1 );
    final ChannelAddress channel2 = new ChannelAddress( TestSystem.B, 1 );

    final TestDataLoadService service = TestDataLoadService.create();
    configureService( service );
    assertNotNull( service.getConnection() );

    assertEquals( false, service.progressAreaOfInterestRequestProcessing() );
    assertEquals( 0, service.getCurrentAOIActions().size() );

    service.requestSubscribe( channel1, null );
    assertEquals( service.progressAreaOfInterestRequestProcessing(), true );
    assertEquals( service.getCurrentAOIActions().size(), 1 );
    assertEquals( service.getCurrentAOIActions().get( 0 ).isInProgress(), true );
    assertEquals( service.getCurrentAOIActions().get( 0 ).getAddress(), channel1 );

    // Nothing to do until the first AOI completes
    assertEquals( false, service.progressAreaOfInterestRequestProcessing() );
    assertEquals( service.getCurrentAOIActions().get( 0 ).getAddress(), channel1 );
    service.requestSubscribe( channel2, null );
    assertEquals( false, service.progressAreaOfInterestRequestProcessing() );
    assertEquals( 1, service.getCurrentAOIActions().size() );
    assertEquals( service.getCurrentAOIActions().get( 0 ).getAddress(), channel1 );

    completeOutstandingAOIs( service );

    assertEquals( true, service.progressAreaOfInterestRequestProcessing() );
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
    assertNotNull( service.getConnection() );

    service.requestSubscribe( channel1, null );
    service.requestSubscribe( channel2, null );
    service.requestSubscribe( channel3, null );

    final AreaOfInterestRequest channel2AOI = service.ensureConnection().getPendingAreaOfInterestRequests().get( 1 );
    when( service.getCacheService().lookup(
      eq( channel2AOI.getCacheKey() ) ) ).thenReturn(
      new CacheEntry( channel2AOI.getCacheKey(), "woo", "har" ) );

    assertEquals( service.progressAreaOfInterestRequestProcessing(), true );
    assertEquals( service.getCurrentAOIActions().size(), 1 );
    assertEquals( service.getCurrentAOIActions().get( 0 ).isInProgress(), true );
    assertEquals( service.getCurrentAOIActions().get( 0 ).getAddress(), channel1 );

    completeOutstandingAOIs( service );
    assertEquals( service.progressAreaOfInterestRequestProcessing(), true );
    assertEquals( service.getCurrentAOIActions().size(), 1 );
    assertEquals( service.getCurrentAOIActions().get( 0 ).isInProgress(), true );
    assertEquals( service.getCurrentAOIActions().get( 0 ).getAddress(), channel2 );

    completeOutstandingAOIs( service );
    assertEquals( service.progressAreaOfInterestRequestProcessing(), true );
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
    assertNotNull( service.getConnection() );

    assertEquals( false, service.progressAreaOfInterestRequestProcessing() );
    assertEquals( 0, service.getCurrentAOIActions().size() );

    service.requestSubscribe( channelA1, null );
    service.requestSubscribe( channelA2, null );
    service.requestSubscribe( channelB1, null );
    service.requestSubscribe( channelB2, null );
    service.requestSubscribe( channelA3, null );
    service.requestUnsubscribe( channelA1 );
    service.requestUnsubscribe( channelA2 );
    service.requestUnsubscribe( channelB1 );

    assertEquals( service.ensureConnection().getPendingAreaOfInterestRequests().size(), 8 );

    assertEquals( service.progressAreaOfInterestRequestProcessing(), true );
    assertEquals( service.getCurrentAOIActions().size(), 2 );
    assertEquals( service.getCurrentAOIActions().get( 0 ).getAddress(), channelA1 );
    assertEquals( service.getCurrentAOIActions().get( 1 ).getAddress(), channelA2 );

    completeOutstandingAOIs( service );
    assertEquals( service.progressAreaOfInterestRequestProcessing(), true );
    assertEquals( service.getCurrentAOIActions().size(), 2 );
    assertEquals( service.getCurrentAOIActions().get( 0 ).getAddress(), channelB1 );
    assertEquals( service.getCurrentAOIActions().get( 1 ).getAddress(), channelB2 );

    completeOutstandingAOIs( service );
    assertEquals( service.progressAreaOfInterestRequestProcessing(), true );
    assertEquals( service.getCurrentAOIActions().size(), 1 );
    assertEquals( service.getCurrentAOIActions().get( 0 ).getAddress(), channelA3 );

    Replicant.context().createSubscription( channelA1, null, true );
    Replicant.context().createSubscription( channelA2, null, true );

    completeOutstandingAOIs( service );
    assertEquals( service.progressAreaOfInterestRequestProcessing(), true );
    assertEquals( service.getCurrentAOIActions().size(), 2 );
    assertEquals( service.getCurrentAOIActions().get( 0 ).getAddress(), channelA1 );
    assertEquals( service.getCurrentAOIActions().get( 1 ).getAddress(), channelA2 );

    Replicant.context().createSubscription( channelB1, null, true );

    completeOutstandingAOIs( service );
    assertEquals( service.progressAreaOfInterestRequestProcessing(), true );
    assertEquals( service.getCurrentAOIActions().size(), 1 );
    assertEquals( service.getCurrentAOIActions().get( 0 ).getAddress(), channelB1 );

    assertEquals( service.progressAreaOfInterestRequestProcessing(), false );
    completeOutstandingAOIs( service );
    assertEquals( service.progressAreaOfInterestRequestProcessing(), false );
  }

  @Test
  public void progressBulkAreaOfInterestAddActionsWithFilters()
    throws Exception
  {
    final ChannelAddress channelA1 = new ChannelAddress( TestSystem.A, 1 );
    final ChannelAddress channelA2 = new ChannelAddress( TestSystem.A, 2 );

    final TestDataLoadService service = TestDataLoadService.create();
    configureService( service );
    assertNotNull( service.getConnection() );

    assertEquals( false, service.progressAreaOfInterestRequestProcessing() );
    assertEquals( 0, service.getCurrentAOIActions().size() );

    service.requestSubscribe( channelA1, null );
    service.requestSubscribe( channelA2, "FilterA" );
    service.requestSubscriptionUpdate( channelA1, "FilterB" );
    service.requestSubscriptionUpdate( channelA2, "FilterB" );

    assertEquals( service.ensureConnection().getPendingAreaOfInterestRequests().size(), 4 );

    assertEquals( service.progressAreaOfInterestRequestProcessing(), true );
    assertEquals( service.getCurrentAOIActions().size(), 1 );
    assertEquals( service.getCurrentAOIActions().get( 0 ).getAddress(), channelA1 );

    completeOutstandingAOIs( service );
    assertEquals( service.progressAreaOfInterestRequestProcessing(), true );
    assertEquals( service.getCurrentAOIActions().size(), 1 );
    assertEquals( service.getCurrentAOIActions().get( 0 ).getAddress(), channelA2 );

    completeOutstandingAOIs( service );

    Replicant.context().createSubscription( channelA1, null, true );
    Replicant.context().createSubscription( channelA2, "FilterB", true );

    assertEquals( service.progressAreaOfInterestRequestProcessing(), true );
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
    assertNotNull( service.getConnection() );

    final Subscription subscriptionA1 = Replicant.context().createSubscription( channelA1, "boo", true );
    final Subscription subscriptionA2 = Replicant.context().createSubscription( channelA2, "boo", true );

    service.requestSubscribe( channelA1, null );
    service.requestSubscribe( channelA2, null );

    assertEquals( service.ensureConnection().getPendingAreaOfInterestRequests().size(), 2 );
    assertEquals( service.progressAreaOfInterestRequestProcessing(), true );
    assertEquals( service.getCurrentAOIActions().size(), 0 );
    assertEquals( service.ensureConnection().getPendingAreaOfInterestRequests().size(), 0 );

    Disposable.dispose( subscriptionA1 );
    Disposable.dispose( subscriptionA2 );

    service.requestSubscriptionUpdate( channelA1, "boo" );
    service.requestSubscriptionUpdate( channelA2, "boo" );

    assertEquals( service.ensureConnection().getPendingAreaOfInterestRequests().size(), 2 );
    assertEquals( service.progressAreaOfInterestRequestProcessing(), true );
    assertEquals( service.getCurrentAOIActions().size(), 0 );
    assertEquals( service.ensureConnection().getPendingAreaOfInterestRequests().size(), 0 );

    service.requestUnsubscribe( channelA1 );
    service.requestUnsubscribe( channelA2 );
    service.requestUnsubscribe( channelA3 );

    assertEquals( service.ensureConnection().getPendingAreaOfInterestRequests().size(), 3 );
    assertEquals( service.progressAreaOfInterestRequestProcessing(), true );
    assertEquals( service.getCurrentAOIActions().size(), 0 );
    assertEquals( service.ensureConnection().getPendingAreaOfInterestRequests().size(), 0 );
  }

  private void completeOutstandingAOIs( final TestDataLoadService service )
  {
    service.getCurrentAOIActions().clear();
  }

  private void verifyPostActionRun( final Runnable runnable )
  {
    verify( runnable ).run();
  }

  private void ensureEnqueueDataLoads( final TestDataLoadService service )
  {
    configureRequests( service, service.getChangeSets() );
    assertFalse( service.isScheduleDataLoadCalled() );
    for ( final TestChangeSet cs : service.getChangeSets() )
    {
      service.ensureConnection().enqueueResponse( "BLAH:" + cs.getRequestID() );
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
        service.ensureConnection().newRequest( "", changeSet.getCacheKey() );
      request.setNormalCompletionAction( changeSet.getRunnable() );
      changeSet.setRequestID( request.getRequestID() );
      return request;
    }
    return null;
  }

  private LinkedList<MessageResponse> progressWorkTillDone( final TestDataLoadService service,
                                                           final int expectedStepCount,
                                                           final int expectedActions )
  {
    final LinkedList<MessageResponse> actionsProcessed = new LinkedList<>();
    int count = 0;
    while ( true )
    {
      count++;
      final boolean moreWork = service.progressResponseProcessing();
      final MessageResponse currentAction = service.getCurrentMessageResponse();
      if ( null != currentAction && !actionsProcessed.contains( currentAction ) )
      {
        actionsProcessed.add( currentAction );
      }
      if ( !moreWork )
      {
        break;
      }
    }

    assertFalse( service.progressResponseProcessing() );
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
    set( service, AbstractDataLoaderService.class, "_session", new Connection( ValueUtil.randomString() ) );

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
                             @Nonnull final AreaOfInterestRequest value )
    throws Exception
  {
    final Field field = AbstractDataLoaderService.class.getDeclaredField( "_currentAoiActions" );
    field.setAccessible( true );
    final List<AreaOfInterestRequest> currentAoiActions = (List<AreaOfInterestRequest>) field.get( instance );
    currentAoiActions.add( value );
  }

  private void assertNotInRequestManager( final TestDataLoadService service, final RequestEntry request )
  {
    assertNull( service.ensureConnection().getRequest( request.getRequestID() ) );
  }

  private void assertInRequestManager( final TestDataLoadService service, final RequestEntry request )
  {
    assertNotNull( service.ensureConnection().getRequest( request.getRequestID() ) );
  }
  */
}
