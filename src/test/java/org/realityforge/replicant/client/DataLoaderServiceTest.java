package org.realityforge.replicant.client;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedList;
import javax.annotation.Nullable;
import org.mockito.InOrder;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class DataLoaderServiceTest
{
  @Test
  public void verifyDataLoader_bulkDataLoad()
    throws Exception
  {
    final Linkable entity = mock( Linkable.class );
    final TestChangeSet changeSet = new TestChangeSet( 1, "j1", new Change[]{ new TestChange( true ) } );
    final ChangeMapper changeMapper = mock( ChangeMapper.class );
    final EntityChangeBroker changeBroker = mock( EntityChangeBroker.class );

    final TestDataLoadService service = newService( changeSet, changeMapper, changeBroker, true );

    when( changeMapper.applyChange( changeSet.getChange( 0 ) ) ).thenReturn( entity );

    assertEquals( service.getLastKnownChangeSet(), 0 );

    final Runnable runnable = mock( Runnable.class );

    ensureEnqueueDataLoads( service, true, runnable );

    final int stepCount = progressWorkTillDone( service );
    assertEquals( stepCount, 9 );

    assertEquals( service.getLastKnownChangeSet(), changeSet.getSequence() );

    verify( service.getRepository(), times( 1 ) ).validate();
    verify( changeBroker ).disable();
    verify( changeBroker ).enable();
    assertTrue( service.isBulkLoadCompleteCalled() );
    assertFalse( service.isIncrementalLoadCompleteCalled() );

    assertTrue( service.isDataLoadComplete() );
    assertTrue( service.isBulkLoad() );
    assertEquals( service.getJobID(), "j1" );

    verify( changeMapper ).applyChange( changeSet.getChange( 0 ) );
    verify( entity ).link();

    verifyPostActionRun( runnable );
  }

  @Test
  public void verifyDeletedEntityIsNotLinked()
    throws Exception
  {
    final Linkable entity = mock( Linkable.class );
    final TestChangeSet changeSet = new TestChangeSet( 1, null, new Change[]{ new TestChange( false ) } );
    final ChangeMapper changeMapper = mock( ChangeMapper.class );

    final TestDataLoadService service = newService( changeSet, changeMapper, mock( EntityChangeBroker.class ), true );

    when( changeMapper.applyChange( changeSet.getChange( 0 ) ) ).thenReturn( entity );

    ensureEnqueueDataLoads( service, true, null );

    progressWorkTillDone( service );

    verify( service.getRepository(), times( 1 ) ).validate();
    verify( changeMapper ).applyChange( changeSet.getChange( 0 ) );
    verify( entity, never() ).link();
  }

  @Test
  public void verifyIncrementalChangeInvokesCorrectMethods()
    throws Exception
  {
    final TestChangeSet changeSet = new TestChangeSet( 1, null, new Change[]{ new TestChange( true ) } );

    final EntityChangeBroker changeBroker = mock( EntityChangeBroker.class );
    final TestDataLoadService service = newService( changeSet, mock( ChangeMapper.class ), changeBroker, true );

    assertEquals( service.getLastKnownChangeSet(), 0 );

    ensureEnqueueDataLoads( service, false, null );

    progressWorkTillDone( service );

    verify( service.getRepository(), times( 1 ) ).validate();

    assertEquals( service.getLastKnownChangeSet(), changeSet.getSequence() );

    verify( changeBroker ).pause();
    verify( changeBroker ).resume();
    assertFalse( service.isBulkLoadCompleteCalled() );
    assertTrue( service.isIncrementalLoadCompleteCalled() );
  }

  @Test
  public void verifyValidateIsNotCalled()
    throws Exception
  {
    final TestChangeSet changeSet = new TestChangeSet( 1, null, new Change[]{ new TestChange( true ) } );

    final EntityChangeBroker changeBroker = mock( EntityChangeBroker.class );
    final TestDataLoadService service =
      newService( new TestChangeSet[]{ changeSet, changeSet },
                  mock( ChangeMapper.class ),
                  changeBroker,
                  false );

    ensureEnqueueDataLoads( service, false, null );
    service.enqueueDataLoad( true, "jsonData", null );

    progressWorkTillDone( service );

    verify( service.getRepository(), never() ).validate();
  }

  @Test
  public void verifyDataLoader_sequenceFollowed()
    throws Exception
  {
    final Linkable entity = mock( Linkable.class );
    final TestChangeSet changeSet1 = new TestChangeSet( 1, null, new Change[]{ new TestChange( true ) } );
    final TestChangeSet changeSet2 = new TestChangeSet( 2, null, new Change[]{ new TestChange( true ) } );
    final ChangeMapper changeMapper = mock( ChangeMapper.class );
    final EntityChangeBroker changeBroker = mock( EntityChangeBroker.class );

    final TestDataLoadService service =
      newService( new TestChangeSet[]{ changeSet2, changeSet1 },
                  changeMapper,
                  changeBroker,
                  true );
    when( changeMapper.applyChange( changeSet1.getChange( 0 ) ) ).thenReturn( entity );
    when( changeMapper.applyChange( changeSet2.getChange( 0 ) ) ).thenReturn( entity );

    assertEquals( service.getLastKnownChangeSet(), 0 );

    final Runnable runnable1 = mock( Runnable.class );
    final Runnable runnable2 = mock( Runnable.class );

    ensureEnqueueDataLoads( service, true, runnable1 );
    final int stepCount = progressWorkTillDone( service );
    assertEquals( stepCount, 3 );

    //No progress should have been made other than parsing packet as out of sequence
    assertEquals( service.getLastKnownChangeSet(), 0 );
    verifyPostActionNotRun( runnable1 );

    service.enqueueDataLoad( true, "jsonData", runnable2 );
    final int stepCount2 = progressWorkTillDone( service );
    assertEquals( stepCount2, 15 );

    //Progress should have been made as all sequence appears
    assertEquals( service.getLastKnownChangeSet(), 2 );
    final InOrder inOrder = inOrder( runnable1, runnable2 );
    inOrder.verify( runnable2 ).run();
    inOrder.verify( runnable1 ).run();
    verifyPostActionRun( runnable1 );
    verifyPostActionRun( runnable2 );
  }

  private void verifyPostActionRun( final Runnable runnable )
  {
    verify( runnable ).run();
  }

  private void verifyPostActionNotRun( final Runnable runnable )
  {
    verify( runnable, never() ).run();
  }

  private void ensureEnqueueDataLoads( final TestDataLoadService service,
                                       final boolean bulkLoad,
                                       final Runnable runnable )
  {
    assertFalse( service.isScheduleDataLoadCalled() );
    service.enqueueDataLoad( bulkLoad, "jsonData", runnable );
    assertTrue( service.isScheduleDataLoadCalled() );
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
                                          final ChangeMapper changeMapper,
                                          final EntityChangeBroker changeBroker,
                                          final boolean validateOnLoad )
    throws NoSuchFieldException, IllegalAccessException
  {
    final TestDataLoadService service = new TestDataLoadService( validateOnLoad, changeSet );
    configureService( changeMapper, changeBroker, service );
    return service;
  }

  private TestDataLoadService newService( final TestChangeSet[] changeSets,
                                          final ChangeMapper changeMapper,
                                          final EntityChangeBroker changeBroker,
                                          final boolean validateOnLoad )
    throws NoSuchFieldException, IllegalAccessException
  {
    final TestDataLoadService service = new TestDataLoadService( validateOnLoad, changeSets );
    configureService( changeMapper, changeBroker, service );
    return service;
  }

  private void configureService( final ChangeMapper changeMapper,
                                 final EntityChangeBroker changeBroker,
                                 final TestDataLoadService service )
    throws NoSuchFieldException, IllegalAccessException
  {
    final Field field1 = AbstractDataLoaderService.class.getDeclaredField( "_changeMapper" );
    field1.setAccessible( true );
    field1.set( service, changeMapper );
    final Field field2 = AbstractDataLoaderService.class.getDeclaredField( "_changeBroker" );
    field2.setAccessible( true );
    field2.set( service, changeBroker );
    final Field field3 = AbstractDataLoaderService.class.getDeclaredField( "_repository" );
    field3.setAccessible( true );
    field3.set( service, mock( EntityRepository.class ) );
    service.setChangesToProcessPerTick( 1 );
    service.setLinksToProcessPerTick( 1 );
  }

  static final class TestDataLoadService
    extends AbstractDataLoaderService
  {
    private final boolean _validateOnLoad;
    private boolean _bulkLoadCompleteCalled;
    private boolean _incrementalLoadCompleteCalled;
    private boolean _scheduleDataLoadCalled;
    private LinkedList<ChangeSet> _changeSets;
    private boolean _dataLoadComplete;
    private boolean _bulkLoad;
    private String _jobID;

    TestDataLoadService( final boolean validateOnLoad, final ChangeSet... changeSets )
    {
      _changeSets = new LinkedList<>( Arrays.asList( changeSets ) );
      _validateOnLoad = validateOnLoad;
    }

    protected boolean isBulkLoadCompleteCalled()
    {
      return _bulkLoadCompleteCalled;
    }

    protected boolean isIncrementalLoadCompleteCalled()
    {
      return _incrementalLoadCompleteCalled;
    }

    protected boolean isScheduleDataLoadCalled()
    {
      return _scheduleDataLoadCalled;
    }

    @Override
    protected void onDataLoadComplete( final boolean bulkLoad, @Nullable final String jobID )
    {
      _dataLoadComplete = true;
      _bulkLoad = bulkLoad;
      _jobID = jobID;
    }

    public boolean isDataLoadComplete()
    {
      return _dataLoadComplete;
    }

    public boolean isBulkLoad()
    {
      return _bulkLoad;
    }

    public String getJobID()
    {
      return _jobID;
    }

    @Override
    protected void onBulkLoadComplete()
    {
      _bulkLoadCompleteCalled = true;
    }

    @Override
    protected void onIncrementalLoadComplete()
    {
      _incrementalLoadCompleteCalled = true;
    }

    @Override
    protected void scheduleDataLoad()
    {
      _scheduleDataLoadCalled = true;
    }

    @Override
    protected boolean shouldValidateOnLoad()
    {
      return _validateOnLoad;
    }

    @Override
    protected ChangeSet parseChangeSet( final String rawJsonData )
    {
      return _changeSets.pop();
    }
  }
}
