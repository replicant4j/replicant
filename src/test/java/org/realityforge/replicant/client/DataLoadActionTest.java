package org.realityforge.replicant.client;

import javax.annotation.Nullable;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class DataLoadActionTest
{
  @DataProvider( name = "actionDescriptions" )
  public Object[][] actionDescriptions()
  {
    final MockRunner runnable = new MockRunner();
    final String jsonData = "DATA";
    final TestChangeSet updateChangeSet = new TestChangeSet( 42, new Change[]{ new TestChange( true ) } );
    final TestChangeSet removeChangeSet = new TestChangeSet( 42, new Change[]{ new TestChange( false ) } );

    final TestChangeSet updateAndRemoveChangeSet =
      new TestChangeSet( 42, new Change[]{ new TestChange( true ), new TestChange( false ) } );

    final Object entity = new Object();
    final MockLinkable linkableEntity = new MockLinkable();


    return new Object[][]{
      { runnable, jsonData, true, updateChangeSet, entity, false },
      { runnable, jsonData, true, removeChangeSet, entity, false },
      { runnable, jsonData, false, updateChangeSet, entity, false },
      { runnable, jsonData, false, removeChangeSet, entity, false },
      { null, jsonData, true, updateChangeSet, entity, false },
      { null, jsonData, true, removeChangeSet, entity, false },
      { null, jsonData, false, updateChangeSet, entity, false },
      { null, jsonData, false, removeChangeSet, entity, false },
      { runnable, jsonData, true, updateChangeSet, linkableEntity, true },
      { runnable, jsonData, true, removeChangeSet, linkableEntity, false },
      { runnable, jsonData, false, updateChangeSet, linkableEntity, true },
      { runnable, jsonData, false, removeChangeSet, linkableEntity, false },
      { null, jsonData, true, updateChangeSet, linkableEntity, true },
      { null, jsonData, true, removeChangeSet, linkableEntity, false },
      { null, jsonData, false, updateChangeSet, linkableEntity, true },
      { null, jsonData, false, removeChangeSet, linkableEntity, false },
      { null, jsonData, false, updateAndRemoveChangeSet, linkableEntity, false }
    };
  }

  @Test( dataProvider = "actionDescriptions" )
  public void verifyActionLifecycle( final MockRunner runnable,
                                     final String jsonData,
                                     final boolean bulkLoad,
                                     final ChangeSet changeSet,
                                     final Object entity,
                                     final boolean expectedLink )
  {
    final DataLoadAction action = new DataLoadAction( bulkLoad, jsonData, runnable );

    //Ensure the initial state is as expected
    assertEquals( action.isBulkLoad(), bulkLoad );
    assertEquals( action.getRawJsonData(), jsonData );
    assertEquals( action.getRunnable(), runnable );
    assertEquals( action.getChangeSet(), null );
    assertRunCount( runnable, 0 );

    assertEquals( action.areEntityLinksCalculated(), false );
    assertEquals( action.areEntityLinksPending(), false );
    assertEquals( action.areChangesPending(), false );
    assertEquals( action.hasWorldBeenNotified(), false );

    action.setChangeSet( changeSet );

    assertEquals( action.getChangeSet(), changeSet );
    assertEquals( action.getRawJsonData(), null );

    assertEquals( action.areChangesPending(), true );
    final Change change = action.nextChange();
    assertEquals( change, changeSet.getChange( 0 ) );

    action.changeProcessed( change.isUpdate(), entity );

    if ( 1 == changeSet.getChangeCount() )
    {
      assertEquals( action.areChangesPending(), false );
    }
    else
    {
      while ( action.areChangesPending() )
      {
        action.changeProcessed( action.nextChange().isUpdate(), entity );
      }
    }

    assertEquals( action.areEntityLinksCalculated(), false );

    action.calculateEntitiesToLink();

    assertEquals( action.areEntityLinksCalculated(), true );

    if ( expectedLink )
    {
      assertEquals( action.areEntityLinksPending(), true );
      assertEquals( action.nextEntityToLink(), entity );
      assertEquals( action.areEntityLinksPending(), false );
    }
    else
    {
      assertEquals( action.areEntityLinksPending(), false );
    }

    assertEquals( action.hasWorldBeenNotified(), false );

    action.markWorldAsNotified();

    assertEquals( action.hasWorldBeenNotified(), true );
  }

  private void assertRunCount( @Nullable final MockRunner runnable, final int expected )
  {
    if ( null != runnable )
    {
      assertEquals( runnable.getRunCount(), expected );
    }
  }

  static final class MockRunner
    implements Runnable
  {
    private int _runCount;

    int getRunCount()
    {
      return _runCount;
    }

    @Override
    public void run()
    {
      _runCount++;
    }

    @Override
    public String toString()
    {
      return "Listener:" + System.identityHashCode( this );
    }
  }

  static final class MockLinkable
    implements Linkable
  {
    @Override
    public void link()
    {
    }

    @Override
    public void delink()
    {
    }

    @Override
    public void invalidate()
    {
    }

    @Override
    public String toString()
    {
      return "Entity:" + System.identityHashCode( this );
    }
  }
}
