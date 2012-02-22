package org.realityforge.replicant.client;

import java.lang.reflect.Field;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class DataLoaderServiceTest
{
  @Test
  public void verifyDataLoader()
    throws Exception
  {
    final Linkable entity = mock( Linkable.class );
    final TestChangeSet changeSet = new TestChangeSet( 42, new Change[]{ new TestChange( true ) } );
    final ChangeMapper changeMapper = mock( ChangeMapper.class );
    final EntityChangeBroker changeBroker = mock( EntityChangeBroker.class );

    final TestDataLoadService service = newService( changeSet, changeMapper, changeBroker );

    when( changeMapper.applyChange( changeSet.getChange( 0 ) ) ).thenReturn( entity );

    // last known id is negative before initial process
    assertTrue( service.getLastKnownChangeSet() < 0 );

    final Runnable runnable = mock( Runnable.class );

    ensureEnqueueDataLoads( service, true, runnable );

    final int stepCount = progressWorkTillDone( service );
    assertEquals( stepCount, 8 );

    assertEquals( service.getLastKnownChangeSet(), changeSet.getSequence() );

    verify( changeBroker ).disable();
    verify( changeBroker ).enable();
    assertTrue( service.isBulkLoadCompleteCalled() );
    assertFalse( service.isIncrementalLoadCompleteCalled() );

    verify( changeMapper ).applyChange( changeSet.getChange( 0 ) );
    verify( entity ).link();

    verifyPostActionRun( runnable );
  }

  @Test
  public void verifyDeletedEntityIsNotLinked()
    throws Exception
  {
    final Linkable entity = mock( Linkable.class );
    final TestChangeSet changeSet = new TestChangeSet( 42, new Change[]{ new TestChange( false ) } );
    final ChangeMapper changeMapper = mock( ChangeMapper.class );

    final TestDataLoadService service = newService( changeSet, changeMapper, mock( EntityChangeBroker.class ) );

    when( changeMapper.applyChange( changeSet.getChange( 0 ) ) ).thenReturn( entity );

    ensureEnqueueDataLoads( service, true, null );

    progressWorkTillDone( service );

    verify( changeMapper ).applyChange( changeSet.getChange( 0 ) );
    verify( entity, never() ).link();
  }

  @Test
  public void verifyIncrementalChangeInvokesCorrectMethods()
    throws Exception
  {
    final TestChangeSet changeSet = new TestChangeSet( 42, new Change[]{ new TestChange( true ) } );

    final EntityChangeBroker changeBroker = mock( EntityChangeBroker.class );
    final TestDataLoadService service = newService( changeSet, mock( ChangeMapper.class ), changeBroker );

    ensureEnqueueDataLoads( service, false, null );

    progressWorkTillDone( service );

    verify( changeBroker ).pause();
    verify( changeBroker ).resume();
    assertFalse( service.isBulkLoadCompleteCalled() );
    assertTrue( service.isIncrementalLoadCompleteCalled() );
  }

  private void verifyPostActionRun( final Runnable runnable )
  {
    verify( runnable ).run();
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
                                          final EntityChangeBroker changeBroker )
    throws NoSuchFieldException, IllegalAccessException
  {
    final TestDataLoadService service = new TestDataLoadService( changeSet );
    final Field field1 = AbstractDataLoaderService.class.getDeclaredField( "_changeMapper" );
    field1.setAccessible( true );
    field1.set( service, changeMapper );
    final Field field2 = AbstractDataLoaderService.class.getDeclaredField( "_changeBroker" );
    field2.setAccessible( true );
    field2.set( service, changeBroker );
    service.setChangesToProcessPerTick( 1 );
    service.setLinksToProcessPerTick( 1 );
    return service;
  }

  static final class TestDataLoadService
    extends AbstractDataLoaderService
  {
    private boolean _bulkLoadCompleteCalled;
    private boolean _incrementalLoadCompleteCalled;
    private boolean _scheduleDataLoadCalled;
    private ChangeSet _changeSet;

    TestDataLoadService( final ChangeSet changeSet )
    {
      _changeSet = changeSet;
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
    protected ChangeSet parseChangeSet( final String rawJsonData )
    {
      return _changeSet;
    }
  }
}
