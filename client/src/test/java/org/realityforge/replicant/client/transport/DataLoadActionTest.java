package org.realityforge.replicant.client.transport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.client.Change;
import org.realityforge.replicant.client.ChannelAction;
import org.realityforge.replicant.client.Linkable;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class DataLoadActionTest
{
  @DataProvider( name = "actionDescriptions" )
  public Object[][] actionDescriptions()
  {
    final List<Boolean> flags = Arrays.asList( Boolean.TRUE, Boolean.FALSE );

    final ArrayList<Object[]> objects = new ArrayList<>();
    for ( final boolean normalCompletion : flags )
    {
      for ( final boolean useRunnable : flags )
      {
        for ( final boolean isLinkableEntity : flags )
        {
          for ( final boolean oob : flags )
          {
            for ( final boolean update : flags )
            {
              final boolean expectLink = isLinkableEntity && update;
              final TestChangeSet changeSet =
                new TestChangeSet( 42,
                                   useRunnable ? new MockRunner() : null,
                                   new Change[]{ new TestChange( update ) } );
              final Object entity = isLinkableEntity ? new MockLinkable() : new Object();
              objects.add( new Object[]{ normalCompletion, oob, changeSet, entity, expectLink } );
            }
            final boolean expectLink = false;
            final TestChangeSet changeSet =
              new TestChangeSet( 42,
                                 useRunnable ? new MockRunner() : null,
                                 new Change[]{ new TestChange( true ), new TestChange( false ) } );
            final Object entity = isLinkableEntity ? new MockLinkable() : new Object();
            objects.add( new Object[]{ normalCompletion, oob, changeSet, entity, expectLink } );
          }
        }
      }
    }

    return objects.toArray( new Object[ objects.size() ][] );
  }

  @Test( dataProvider = "actionDescriptions" )
  public void verifyActionLifecycle( final boolean normalCompletion,
                                     final boolean oob,
                                     final TestChangeSet changeSet,
                                     final Object entity,
                                     final boolean expectedLink )
  {
    final MockRunner runnable = (MockRunner) changeSet.getRunnable();
    final DataLoadAction action = new DataLoadAction( "BLAH", oob );

    //Ensure the initial state is as expected
    assertEquals( action.getRawJsonData(), "BLAH" );
    assertNull( action.getChangeSet() );
    if ( null != runnable )
    {
      assertEquals( runnable.getRunCount(), 0 );
    }

    assertFalse( action.areEntityLinksCalculated() );
    assertFalse( action.areEntityLinksPending() );
    assertFalse( action.areChangesPending() );
    assertFalse( action.hasWorldBeenNotified() );

    if ( oob )
    {
      action.setRunnable( changeSet.getRunnable() );
      action.setChangeSet( changeSet, null );
    }
    else
    {
      changeSet.setRequestId( ValueUtil.randomInt() );
      final Integer requestId = changeSet.getRequestId();
      assertNotNull( requestId );
      final RequestEntry request = new RequestEntry( requestId, "MyOperation", null );
      if ( normalCompletion )
      {
        request.setNormalCompletionAction( runnable );
      }
      else
      {
        request.setNonNormalCompletionAction( runnable );
      }
      action.setChangeSet( changeSet, request );
    }
    assertEquals( action.getRunnable(), runnable );

    assertEquals( action.getChangeSet(), changeSet );
    assertNull( action.getRawJsonData() );

    assertTrue( action.areChangesPending() );
    final Change change = action.nextChange();
    assertNotNull( change );
    assertEquals( change, changeSet.getChange( 0 ) );

    action.changeProcessed( change.isUpdate(), entity );

    if ( 1 == changeSet.getChangeCount() )
    {
      assertFalse( action.areChangesPending() );
    }
    else
    {
      while ( action.areChangesPending() )
      {
        final Change nextChange = action.nextChange();
        assertNotNull( nextChange );
        action.changeProcessed( nextChange.isUpdate(), entity );
      }
    }

    assertFalse( action.areEntityLinksCalculated() );

    action.calculateEntitiesToLink();

    assertTrue( action.areEntityLinksCalculated() );

    if ( expectedLink )
    {
      assertTrue( action.areEntityLinksPending() );
      assertEquals( action.nextEntityToLink(), entity );
      assertFalse( action.areEntityLinksPending() );
    }
    else
    {
      assertFalse( action.areEntityLinksPending() );
    }

    assertFalse( action.hasWorldBeenNotified() );

    action.markWorldAsNotified();

    assertTrue( action.hasWorldBeenNotified() );
  }

  @Test
  public void needsBrokerPause()
  {
    {
      //No changes and no channel actions
      final DataLoadAction action = new DataLoadAction( "", false );
      assertFalse( action.needsBrokerPause() );
    }

    {
      //Changes and no channel actions
      final DataLoadAction action = new DataLoadAction( "", false );
      action.setChangeSet( new TestChangeSet( ValueUtil.randomInt(),
                                              null,
                                              new Change[]{ new TestChange( ValueUtil.randomBoolean() ) },
                                              new ChannelAction[]{} ),
                           null );
      assertTrue( action.needsBrokerPause() );
      action.markBrokerPaused();
      // broker already paused
      assertFalse( action.needsBrokerPause() );
    }

    {
      //No changes and channel actions
      final DataLoadAction action = new DataLoadAction( "", false );
      action.setChangeSet( new TestChangeSet( ValueUtil.randomInt(),
                                              null,
                                              new Change[]{},
                                              new ChannelAction[]{ new TestChannelAction( ValueUtil.randomInt(),
                                                                                          null,
                                                                                          ChannelAction.Action.ADD ) } ),
                           null );
      assertTrue( action.needsBrokerPause() );
      action.markBrokerPaused();
      // broker already paused
      assertFalse( action.needsBrokerPause() );
    }

    {
      //No changes and channel actions
      final DataLoadAction action = new DataLoadAction( "", false );
      action.setChangeSet( new TestChangeSet( ValueUtil.randomInt(),
                                              null,
                                              new Change[]{ new TestChange( ValueUtil.randomBoolean() ) },
                                              new ChannelAction[]{ new TestChannelAction( ValueUtil.randomInt(),
                                                                                          null,
                                                                                          ChannelAction.Action.ADD ) } ),
                           null );
      assertTrue( action.needsBrokerPause() );
      action.markBrokerPaused();
      // broker already paused
      assertFalse( action.needsBrokerPause() );
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
    public boolean isLinked()
    {
      return false;
    }

    @Override
    public void link()
    {
    }

    @Override
    public void invalidate()
    {
    }

    @Override
    public boolean isValid()
    {
      return true;
    }

    @Override
    public String toString()
    {
      return "Entity:" + System.identityHashCode( this );
    }
  }
}
