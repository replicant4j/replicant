package org.realityforge.replicant.client.transport;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;
import org.mockito.InOrder;
import org.realityforge.replicant.client.Change;
import org.realityforge.replicant.client.ChangeMapper;
import org.realityforge.replicant.client.EntityChangeBroker;
import org.realityforge.replicant.client.EntityRepository;
import org.realityforge.replicant.client.Linkable;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class DataLoaderServiceTest
{
  @Test
  public void setSession()
    throws Exception
  {
    final TestChangeSet changeSet =
      new TestChangeSet( 1, mock( Runnable.class ), true, new Change[ 0 ] );
    final TestDataLoadService service = newService( changeSet, true );
    final Runnable runnable1 = mock( Runnable.class );
    final TestClientSession session1 = new TestClientSession( service, "X" );

    session1.enqueueOOB( "X", null, false );

    assertEquals( SessionContext.getSession(), null );

    service.setSession( session1, runnable1 );

    assertEquals( SessionContext.getSession(), session1 );
    assertEquals( service.getSession(), session1 );
    verify( runnable1, times( 1 ) ).run();
    verify( service.getChangeBroker(), times( 1 ) ).disable();
    verify( service.getChangeBroker(), times( 1 ) ).enable();

    // Should be no oob actions left
    assertEquals( progressWorkTillDone( service ), 7 );

    service.setSession( session1, runnable1 );
    verify( runnable1, times( 2 ) ).run();
    // The following should not run as session is the same
    verify( service.getChangeBroker(), times( 1 ) ).disable();
    verify( service.getChangeBroker(), times( 1 ) ).enable();
  }

  @Test
  public void getTerminateCount()
    throws Exception
  {
    final TestChangeSet changeSet = new TestChangeSet( 1, mock( Runnable.class ), true, new Change[ 0 ] );
    final TestDataLoadService service = newService( changeSet, true );
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

    final TestDataLoadService service = newService( changeSet, true );

    when( service.getChangeMapper().applyChange( changeSet.getChange( 0 ) ) ).thenReturn( entity );

    assertEquals( service.getSession().getLastRxSequence(), 0 );

    ensureEnqueueDataLoads( service );

    final RequestEntry request = ensureRequest( service, changeSet );

    final int stepCount = progressWorkTillDone( service );
    assertEquals( stepCount, 9 );

    // Termination count is actually 2 as progressWorkTillDone will attempt to progress
    // once after it is initially terminates
    assertEquals( service.getTerminateCount(), 2 );

    assertEquals( service.getSession().getLastRxSequence(), changeSet.getSequence() );

    verify( service.getRepository(), times( 1 ) ).validate();
    verify( service.getChangeBroker() ).disable();
    verify( service.getChangeBroker() ).enable();
    assertTrue( service.isBulkLoadCompleteCalled() );
    assertFalse( service.isIncrementalLoadCompleteCalled() );

    assertTrue( service.isDataLoadComplete() );
    assertTrue( service.isBulkLoad() );

    verify( service.getChangeMapper() ).applyChange( changeSet.getChange( 0 ) );
    verify( entity ).link();

    assertRequestProcessed( service, request );
  }

  @Test
  public void getSessionID()
    throws Exception
  {
    final TestDataLoadService service = newService( new TestChangeSet[ 0 ], true );
    assertEquals( service.getSessionID(), service.getSession().getSessionID() );
  }

  @Test
  public void cache_requestWithCacheKeyAndETag()
    throws Exception
  {
    final TestChangeSet changeSet =
      new TestChangeSet( 1, mock( Runnable.class ), true, new Change[ 0 ] );
    changeSet.setCacheKey( "MetaData" );
    changeSet.setEtag( "1 Jan 2020" );

    final TestDataLoadService service = newService( changeSet, true );
    final CacheService cacheService = service.getCacheService();

    assertEquals( service.getSession().getLastRxSequence(), 0 );

    service.getSession().enqueueDataLoad( "Data" );
    final RequestEntry request = configureRequest( changeSet, service );
    assertNotNull( request );

    final int stepCount = progressWorkTillDone( service );
    assertEquals( stepCount, 7 );

    assertEquals( service.getSession().getLastRxSequence(), changeSet.getSequence() );

    verify( service.getRepository(), times( 1 ) ).validate();
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

    final TestDataLoadService service = newService( changeSet, true );
    final CacheService cacheService = service.getCacheService();

    assertEquals( service.getSession().getLastRxSequence(), 0 );

    service.getSession().enqueueOOB( "Data", changeSet.getRunnable(), changeSet.isBulkChange() );

    final int stepCount = progressWorkTillDone( service );
    assertEquals( stepCount, 7 );

    assertEquals( service.getSession().getLastRxSequence(), 0 );

    verify( service.getRepository(), times( 1 ) ).validate();
    verify( cacheService, never() ).store( anyString(), anyString(), anyString() );
  }

  private void assertRequestProcessed( final TestDataLoadService service, final RequestEntry request )
  {
    assertTrue( request.haveResultsArrived() );
    assertNotInRequestManager( service, request );
    assertTrue( service.isDataLoadComplete() );
    assertNotNull( service.getRequestID() );

    verifyPostActionRun( request.getRunnable() );
  }

  @Test
  public void enqueueOOB()
    throws Exception
  {
    final Linkable entity = mock( Linkable.class );
    final TestChangeSet changeSet =
      new TestChangeSet( 1, mock( Runnable.class ), true, new Change[]{ new TestChange( true ) } );

    final TestDataLoadService service = newService( changeSet, true );

    when( service.getChangeMapper().applyChange( changeSet.getChange( 0 ) ) ).thenReturn( entity );

    assertEquals( service.getSession().getLastRxSequence(), 0 );

    assertFalse( service.isScheduleDataLoadCalled() );
    for ( final TestChangeSet cs : service.getChangeSets() )
    {
      service.getSession().enqueueOOB( "BLAH:" + cs.getSequence(), changeSet.getRunnable(), changeSet.isBulkChange() );
    }
    assertTrue( service.isScheduleDataLoadCalled() );

    final int stepCount = progressWorkTillDone( service );
    assertEquals( stepCount, 9 );

    assertEquals( service.getSession().getLastRxSequence(), 0 );

    verify( service.getRepository(), times( 1 ) ).validate();
    verify( service.getChangeBroker() ).disable();
    verify( service.getChangeBroker() ).enable();
    assertTrue( service.isBulkLoadCompleteCalled() );
    assertFalse( service.isIncrementalLoadCompleteCalled() );

    assertTrue( service.isDataLoadComplete() );
    assertTrue( service.isBulkLoad() );
    assertNull( service.getRequestID() );

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

    final TestDataLoadService service = newService( changeSet, true );

    final RequestEntry request =
      service.getSession().newRequestRegistration( "", null, changeSet.isBulkChange() );
    changeSet.setRequestID( request.getRequestID() );
    service.getSession().enqueueDataLoad( "blah" );

    progressWorkTillDone( service );

    assertTrue( request.haveResultsArrived() );
    final String requestID = changeSet.getRequestID();
    assertNotNull( requestID );
    assertInRequestManager( service, request );
    assertNotNull( service.getRequestID() );

    verifyPostActionNotRun( changeSet.getRunnable() );
  }

  @Test
  public void verifyDataLoader_dataLoadWithZeroChanges()
    throws Exception
  {
    final TestChangeSet changeSet = new TestChangeSet( 1, mock( Runnable.class ), false, new Change[ 0 ] );

    final TestDataLoadService service = newService( changeSet, true );

    assertEquals( service.getSession().getLastRxSequence(), 0 );

    ensureEnqueueDataLoads( service );

    final int stepCount = progressWorkTillDone( service );
    assertEquals( stepCount, 7 );

    assertEquals( service.getSession().getLastRxSequence(), changeSet.getSequence() );

    verify( service.getRepository(), times( 1 ) ).validate();
    verify( service.getChangeBroker(), never() ).disable();
    verify( service.getChangeBroker(), never() ).enable();
    assertFalse( service.isBulkLoadCompleteCalled() );
    assertTrue( service.isIncrementalLoadCompleteCalled() );

    assertTrue( service.isDataLoadComplete() );
    assertFalse( service.isBulkLoad() );
    assertNotNull( service.getRequestID() );

    verifyPostActionRun( changeSet.getRunnable() );
  }

  @Test
  public void verifyDeletedEntityIsNotLinked()
    throws Exception
  {
    final Linkable entity = mock( Linkable.class );
    final TestChangeSet changeSet = new TestChangeSet( 1, null, true, new Change[]{ new TestChange( false ) } );

    final TestDataLoadService service = newService( changeSet, true );

    when( service.getChangeMapper().applyChange( changeSet.getChange( 0 ) ) ).thenReturn( entity );

    ensureEnqueueDataLoads( service );

    progressWorkTillDone( service );

    verify( service.getRepository(), times( 1 ) ).validate();
    verify( service.getChangeMapper() ).applyChange( changeSet.getChange( 0 ) );
    verify( entity, never() ).link();
  }

  @Test
  public void verifyIncrementalChangeInvokesCorrectMethods()
    throws Exception
  {
    final TestChangeSet changeSet = new TestChangeSet( 1, null, false, new Change[]{ new TestChange( true ) } );

    final TestDataLoadService service = newService( changeSet, true );

    assertEquals( service.getSession().getLastRxSequence(), 0 );

    ensureEnqueueDataLoads( service );

    progressWorkTillDone( service );

    verify( service.getRepository(), times( 1 ) ).validate();

    assertEquals( service.getSession().getLastRxSequence(), changeSet.getSequence() );

    final EntityChangeBroker changeBroker = service.getChangeBroker();
    verify( changeBroker ).pause();
    verify( changeBroker ).resume();
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

    progressWorkTillDone( service );

    verify( service.getRepository(), never() ).validate();
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

    assertEquals( service.getSession().getLastRxSequence(), 0 );

    configureRequests( service, service.getChangeSets() );
    service.getSession().enqueueDataLoad( "jsonData" );
    final int stepCount = progressWorkTillDone( service );
    assertEquals( stepCount, 3 );

    //No progress should have been made other than parsing packet as out of sequence
    assertEquals( service.getSession().getLastRxSequence(), 0 );
    verifyPostActionNotRun( changeSet2.getRunnable() );
    verifyPostActionNotRun( changeSet1.getRunnable() );

    service.getSession().enqueueDataLoad( "jsonData" );
    final int stepCount2 = progressWorkTillDone( service );
    assertEquals( stepCount2, 15 );

    //Progress should have been made as all sequence appears
    assertEquals( service.getSession().getLastRxSequence(), 2 );
    final InOrder inOrder = inOrder( changeSet2.getRunnable(), changeSet1.getRunnable() );
    inOrder.verify( changeSet1.getRunnable() ).run();
    inOrder.verify( changeSet2.getRunnable() ).run();
    inOrder.verifyNoMoreInteractions();
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
      service.getSession().enqueueDataLoad( "BLAH:" + cs.getRequestID() );
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
        service.getSession().newRequestRegistration( "", changeSet.getCacheKey(), changeSet.isBulkChange() );
      request.setNormalCompletionAction( changeSet.getRunnable() );
      changeSet.setRequestID( request.getRequestID() );
      return request;
    }
    return null;
  }

  private int progressWorkTillDone( final TestDataLoadService service )
  {
    int count = 0;
    while ( true )
    {
      count++;
      final boolean moreWork = service.progressDataLoad();
      if ( !moreWork )
      {
        break;
      }
    }

    assertFalse( service.progressDataLoad() );

    return count;
  }

  private TestDataLoadService newService( final TestChangeSet changeSet,
                                          final boolean validateOnLoad )
    throws Exception
  {
    final TestDataLoadService service = new TestDataLoadService( validateOnLoad, changeSet );
    configureService( service );
    return service;
  }

  private TestDataLoadService newService( final TestChangeSet[] changeSets, final boolean validateOnLoad )
    throws Exception
  {
    final TestDataLoadService service = new TestDataLoadService( validateOnLoad, changeSets );
    configureService( service );
    return service;
  }

  private void configureService( final TestDataLoadService service )
    throws Exception
  {
    set( service, AbstractDataLoaderService.class, "_changeMapper", mock( ChangeMapper.class ) );
    set( service, AbstractDataLoaderService.class, "_changeBroker", mock( EntityChangeBroker.class ) );
    set( service, AbstractDataLoaderService.class, "_repository", mock( EntityRepository.class ) );
    set( service, AbstractDataLoaderService.class, "_cacheService", mock( CacheService.class ) );
    set( service, AbstractDataLoaderService.class, "_session", new TestClientSession( service, "1" ) );

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
    assertNull( service.getSession().getRequest( request.getRequestID() ) );
  }

  private void assertInRequestManager( final TestDataLoadService service, final RequestEntry request )
  {
    assertNotNull( service.getSession().getRequest( request.getRequestID() ) );
  }

  private RequestEntry ensureRequest( final TestDataLoadService service, final TestChangeSet changeSet )
  {
    final String requestID = changeSet.getRequestID();
    assertNotNull( requestID );
    final RequestEntry request = service.getSession().getRequest( requestID );
    assertNotNull( request );
    return request;
  }
}
