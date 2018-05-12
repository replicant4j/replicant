package replicant;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.spy.DataLoadStatus;
import static org.testng.Assert.*;

public class DataLoadActionTest
  extends AbstractReplicantTest
{
  @Test
  public void construct()
  {
    final String rawJsonData = "";
    final boolean oob = false;
    final DataLoadAction action = new DataLoadAction( rawJsonData, oob );
    assertEquals( action.getRawJsonData(), rawJsonData );
    assertEquals( action.isOob(), oob );

    assertEquals( action.areEntityLinksCalculated(), false );
    assertEquals( action.areEntityLinksPending(), false );
    assertEquals( action.areChangesPending(), false );
    assertEquals( action.hasWorldBeenNotified(), false );

    assertEquals( action.getChannelAddCount(), 0 );
    assertEquals( action.getChannelUpdateCount(), 0 );
    assertEquals( action.getChannelRemoveCount(), 0 );
    assertEquals( action.getEntityUpdateCount(), 0 );
    assertEquals( action.getEntityRemoveCount(), 0 );
    assertEquals( action.getEntityLinkCount(), 0 );
  }

  @SuppressWarnings( "EqualsWithItself" )
  @Test
  public void compareTo()
  {
    final DataLoadAction action1 = new DataLoadAction( "", false );
    final DataLoadAction action2 = new DataLoadAction( "", false );
    final DataLoadAction action3 = new DataLoadAction( "", true );
    final DataLoadAction action4 = new DataLoadAction( "", true );

    final ChangeSet changeSet1 = ChangeSet.create( 1, null, null, new ChannelChange[ 0 ], new EntityChange[ 0 ] );
    final ChangeSet changeSet2 = ChangeSet.create( 2, null, null, new ChannelChange[ 0 ], new EntityChange[ 0 ] );
    final ChangeSet changeSet3 = ChangeSet.create( 3, null, null, new ChannelChange[ 0 ], new EntityChange[ 0 ] );
    final ChangeSet changeSet4 = ChangeSet.create( 4, null, null, new ChannelChange[ 0 ], new EntityChange[ 0 ] );

    action1.setChangeSet( changeSet1, null );
    action2.setChangeSet( changeSet2, null );
    action3.setChangeSet( changeSet3, null );
    action4.setChangeSet( changeSet4, null );

    assertEquals( action1.compareTo( action1 ), 0 );
    assertEquals( action1.compareTo( action2 ), -1 );
    assertEquals( action1.compareTo( action3 ), 1 );
    assertEquals( action1.compareTo( action4 ), 1 );
    assertEquals( action2.compareTo( action1 ), 1 );
    assertEquals( action2.compareTo( action2 ), 0 );
    assertEquals( action2.compareTo( action3 ), 1 );
    assertEquals( action2.compareTo( action4 ), 1 );
    assertEquals( action3.compareTo( action1 ), -1 );
    assertEquals( action3.compareTo( action2 ), -1 );
    assertEquals( action3.compareTo( action3 ), 0 );
    assertEquals( action3.compareTo( action4 ), 0 );
    assertEquals( action4.compareTo( action1 ), -1 );
    assertEquals( action4.compareTo( action2 ), -1 );
    assertEquals( action4.compareTo( action3 ), 0 );
    assertEquals( action4.compareTo( action4 ), 0 );
  }

  @Test
  public void toStatus()
  {
    final int sequence = ValueUtil.randomInt();
    final String requestId = ValueUtil.randomString();
    final ChangeSet changeSet =
      ChangeSet.create( sequence, requestId, null, new ChannelChange[ 0 ], new EntityChange[ 0 ] );

    final DataLoadAction action = new DataLoadAction( ValueUtil.randomString(), ValueUtil.randomBoolean() );
    action.setChangeSet( changeSet, null );

    action.incChannelAddCount();
    action.incChannelAddCount();
    action.incChannelRemoveCount();
    action.incChannelRemoveCount();
    action.incChannelRemoveCount();
    action.incChannelUpdateCount();
    action.incEntityUpdateCount();
    action.incEntityRemoveCount();
    action.incEntityRemoveCount();
    action.incEntityLinkCount();

    final DataLoadStatus status = action.toStatus();

    assertEquals( status.getSequence(), sequence );
    assertEquals( status.getRequestId(), requestId );
    assertEquals( status.getChannelAddCount(), 2 );
    assertEquals( status.getChannelUpdateCount(), 1 );
    assertEquals( status.getChannelRemoveCount(), 3 );
    assertEquals( status.getEntityUpdateCount(), 1 );
    assertEquals( status.getEntityRemoveCount(), 2 );
    assertEquals( status.getEntityLinkCount(), 1 );
  }

  /*
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
              final EntityChange changeUpdate =
                EntityChange.create( ValueUtil.randomInt(),
                               ValueUtil.randomInt(),
                               new EntityChannel[ 0 ],
                               update ? JsPropertyMap.of() : null );
              final TestChangeSet changeSet =
                new TestChangeSet( ChangeSet.create( ValueUtil.randomInt(),
                                                     null,
                                                     null,
                                                     new ChannelChange[ 0 ],
                                                     new EntityChange[]{ changeUpdate } ),
                                   useRunnable ? new MockRunner() : null );
              final Object entity = isLinkableEntity ? new MockLinkable() : new Object();
              objects.add( new Object[]{ normalCompletion, oob, changeSet, entity, expectLink } );
            }
            final boolean expectLink = false;
            final EntityChange changeUpdate =
              EntityChange.create( ValueUtil.randomInt(), ValueUtil.randomInt(), new EntityChannel[ 0 ], JsPropertyMap.of() );
            final EntityChange changeRemove =
              EntityChange.create( ValueUtil.randomInt(), ValueUtil.randomInt(), new EntityChannel[ 0 ], null );
            final ChangeSet cs =
              ChangeSet.create( ValueUtil.randomInt(),
                                ValueUtil.randomString(),
                                null,
                                new ChannelChange[ 0 ],
                                new EntityChange[]{ changeUpdate, changeRemove } );
            final TestChangeSet changeSet =
              new TestChangeSet( cs,
                                 useRunnable ? new MockRunner() : null );
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
    assertEquals( action.getChangeSet(), null );
    if ( null != runnable )
    {
      assertEquals( runnable.getRunCount(), 0 );
    }

    assertEquals( action.areEntityLinksCalculated(), false );
    assertEquals( action.areEntityLinksPending(), false );
    assertEquals( action.areChangesPending(), false );
    assertEquals( action.hasWorldBeenNotified(), false );

    if ( oob )
    {
      action.setRunnable( changeSet.getRunnable() );
      action.setChangeSet( changeSet.getChangeSet(), null );
    }
    else
    {
      final String requestID = changeSet.getChangeSet().getRequestID();
      assertNotNull( requestID );
      final RequestEntry request = new RequestEntry( requestID, "MyOperation", null );
      if ( normalCompletion )
      {
        request.setNormalCompletionAction( runnable );
      }
      else
      {
        request.setNonNormalCompletionAction( runnable );
      }
      action.setChangeSet( changeSet.getChangeSet(), request );
    }
    assertEquals( action.getRunnable(), runnable );

    assertEquals( action.getChangeSet(), changeSet.getChangeSet() );
    assertEquals( action.getRawJsonData(), null );

    assertEquals( action.areChangesPending(), true );
    final EntityChange change = action.nextChange();
    assertNotNull( change );
    assertEquals( change, changeSet.getChangeSet().getChange( 0 ) );

    action.changeProcessed( change.isUpdate(), entity );

    if ( 1 == changeSet.getChangeSet().getChangeCount() )
    {
      assertEquals( action.areChangesPending(), false );
    }
    else
    {
      while ( action.areChangesPending() )
      {
        final EntityChange nextChange = action.nextChange();
        assertNotNull( nextChange );
        action.changeProcessed( nextChange.isUpdate(), entity );
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
    public String toString()
    {
      return "Entity:" + System.identityHashCode( this );
    }
  }
  */
}
