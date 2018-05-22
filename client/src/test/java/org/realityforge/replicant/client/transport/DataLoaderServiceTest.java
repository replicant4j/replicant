package org.realityforge.replicant.client.transport;

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
