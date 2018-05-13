package replicant;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.spy.DataLoadStatus;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

@SuppressWarnings( "ResultOfMethodCallIgnored" )
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

  @Test
  public void incIgnoredUnlessSpyEnabled()
  {
    ReplicantTestUtil.disableSpies();

    final DataLoadAction action = new DataLoadAction( ValueUtil.randomString(), ValueUtil.randomBoolean() );

    assertEquals( action.getChannelAddCount(), 0 );
    assertEquals( action.getChannelUpdateCount(), 0 );
    assertEquals( action.getChannelRemoveCount(), 0 );
    assertEquals( action.getEntityUpdateCount(), 0 );
    assertEquals( action.getEntityRemoveCount(), 0 );
    assertEquals( action.getEntityLinkCount(), 0 );

    // We enforce this to make it easier for DCE
    action.incChannelAddCount();
    action.incChannelRemoveCount();
    action.incChannelUpdateCount();
    action.incEntityUpdateCount();
    action.incEntityRemoveCount();
    action.incEntityLinkCount();

    assertEquals( action.getChannelAddCount(), 0 );
    assertEquals( action.getChannelUpdateCount(), 0 );
    assertEquals( action.getChannelRemoveCount(), 0 );
    assertEquals( action.getEntityUpdateCount(), 0 );
    assertEquals( action.getEntityRemoveCount(), 0 );
    assertEquals( action.getEntityLinkCount(), 0 );
  }

  @Test
  public void testToString()
  {
    final DataLoadAction action = new DataLoadAction( ValueUtil.randomString(), ValueUtil.randomBoolean() );
    assertEquals( action.toString(),
                  "DataLoad[,RawJson.null?=false,ChangeSet.null?=true,ChangeIndex=0,Runnable.null?=true,UpdatedEntities.size=0,RemovedEntities.size=0,EntitiesToLink.size=null,EntityLinksCalculated=false]" );

    final int sequence = 33;
    final String requestId = "XXX";
    final ChangeSet changeSet =
      ChangeSet.create( sequence, requestId, null, new ChannelChange[ 0 ], new EntityChange[ 0 ] );
    action.setChangeSet( changeSet, null );

    assertEquals( action.toString(),
                  "DataLoad[,RawJson.null?=true,ChangeSet.null?=false,ChangeIndex=0,Runnable.null?=true,UpdatedEntities.size=0,RemovedEntities.size=0,EntitiesToLink.size=null,EntityLinksCalculated=false]" );

    action.calculateEntitiesToLink();

    assertEquals( action.toString(),
                  "DataLoad[,RawJson.null?=true,ChangeSet.null?=false,ChangeIndex=0,Runnable.null?=true,UpdatedEntities.size=null,RemovedEntities.size=null,EntitiesToLink.size=0,EntityLinksCalculated=true]" );

    ReplicantTestUtil.disableNames();

    assertEquals( action.toString(),
                  "replicant.DataLoadAction@" + Integer.toHexString( System.identityHashCode( action ) ) );
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
              EntityChange.create( ValueUtil.randomInt(),
                                   ValueUtil.randomInt(),
                                   new EntityChannel[ 0 ],
                                   JsPropertyMap.of() );
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

  // @Test( dataProvider = "actionDescriptions" )
  public void verifyActionLifecycle( final boolean normalCompletion,
                                     final boolean oob,
                                     final ChangeSet changeSet,
                                     final Object entity,
                                     final boolean expectedLink )
  {
    final Runnable runnable = mock( Runnable.class );
    final String rawJsonData = ValueUtil.randomString();
    final DataLoadAction action = new DataLoadAction( rawJsonData, oob );

    //Ensure the initial state is as expected
    assertEquals( action.getRawJsonData(), rawJsonData );
    assertEquals( action.getChangeSet(), null );

    assertEquals( action.areEntityLinksCalculated(), false );
    assertEquals( action.areEntityLinksPending(), false );
    assertEquals( action.areChangesPending(), false );
    assertEquals( action.hasWorldBeenNotified(), false );

    if ( oob )
    {
      action.setRunnable( runnable );
      action.setChangeSet( changeSet, null );
    }
    else
    {
      final String requestID = changeSet.getRequestID();
      assertNotNull( requestID );
      final String requestKey = ValueUtil.randomString();
      final RequestEntry request = new RequestEntry( requestID, requestKey, null );
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
    assertEquals( action.getRawJsonData(), null );

    assertEquals( action.areChangesPending(), true );
    final EntityChange change = action.nextChange();
    assertNotNull( change );
    assertEquals( change, changeSet.getEntityChanges()[ 0 ] );

    action.changeProcessed( change.isUpdate(), entity );

    if ( 1 == changeSet.getEntityChanges().length )
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
*/

  @Test
  public void lifeCycleWithNormallyCompletedRequest()
  {
    // ChangeSet details
    final int sequence = 1;
    final String requestId = ValueUtil.randomString();

    // Channel updates
    final ChannelChange[] channelChanges = new ChannelChange[ 0 ];

    // Entity Updates
    final int channelId = 22;

    // Entity update
    final EntityChange change1 =
      EntityChange.create( 50,
                           100,
                           new EntityChannel[]{ EntityChannel.create( channelId ) },
                           new TestEntityChangeData() );
    // Entity Remove
    final EntityChange change2 =
      EntityChange.create( 51,
                           100,
                           new EntityChannel[]{ EntityChannel.create( channelId ) } );
    // Entity update - non linkable
    final EntityChange change3 =
      EntityChange.create( 52,
                           100,
                           new EntityChannel[]{ EntityChannel.create( channelId ) },
                           new TestEntityChangeData() );
    final EntityChange[] entityChanges = new EntityChange[]{ change1, change2, change3 };

    final Object[] entities = new Object[]{ mock( Linkable.class ), new Object(), new Object() };

    final ChangeSet changeSet = ChangeSet.create( sequence, requestId, null, channelChanges, entityChanges );

    final Runnable runnable = mock( Runnable.class );
    final String rawJsonData = ValueUtil.randomString();
    final DataLoadAction action = new DataLoadAction( rawJsonData, false );

    final String requestKey = ValueUtil.randomString();
    final RequestEntry request = new RequestEntry( requestId, requestKey, null );

    //Ensure the initial state is as expected
    assertEquals( action.getRawJsonData(), rawJsonData );
    assertThrows( action::getChangeSet );

    assertEquals( action.needsChannelActionsProcessed(), false );
    assertEquals( action.areChangesPending(), false );
    assertEquals( action.areEntityLinksCalculated(), false );
    assertEquals( action.areEntityLinksPending(), false );
    assertEquals( action.hasWorldBeenNotified(), false );

    assertNull( action.getRunnable() );

    request.setNormalCompletionAction( runnable );

    action.setChangeSet( changeSet, request );

    assertEquals( action.getRunnable(), runnable );
    assertEquals( action.getChangeSet(), changeSet );
    assertEquals( action.getRequest(), request );
    assertEquals( action.getRawJsonData(), null );

    assertEquals( action.needsChannelActionsProcessed(), false );
    assertEquals( action.areChangesPending(), true );
    assertEquals( action.areEntityLinksCalculated(), false );
    assertEquals( action.areEntityLinksPending(), false );
    assertEquals( action.hasWorldBeenNotified(), false );

    // Process entity changes
    {
      assertEquals( action.nextChange(), entityChanges[ 0 ] );
      action.changeProcessed( entityChanges[ 0 ].isUpdate(), entities[ 0 ] );
      action.incEntityUpdateCount();

      assertEquals( action.areChangesPending(), true );

      assertEquals( action.nextChange(), entityChanges[ 1 ] );
      action.incEntityRemoveCount();

      assertEquals( action.areChangesPending(), true );

      assertEquals( action.nextChange(), entityChanges[ 2 ] );
      action.incEntityUpdateCount();
      action.changeProcessed( entityChanges[ 0 ].isUpdate(), entities[ 2 ] );

      assertEquals( action.areChangesPending(), false );

      assertEquals( action.nextChange(), null );

      assertEquals( action.areChangesPending(), false );

      assertEquals( action.getEntityUpdateCount(), 2 );
      assertEquals( action.getEntityRemoveCount(), 1 );
    }

    assertEquals( action.needsChannelActionsProcessed(), false );
    assertEquals( action.areChangesPending(), false );
    assertEquals( action.areEntityLinksCalculated(), false );
    assertEquals( action.areEntityLinksPending(), false );
    assertEquals( action.hasWorldBeenNotified(), false );

    action.calculateEntitiesToLink();

    assertEquals( action.areEntityLinksCalculated(), true );

    // process links
    {
      assertEquals( action.nextEntityToLink(), entities[ 0 ] );
      action.incEntityLinkCount();
      assertEquals( action.nextEntityToLink(), null );
      assertEquals( action.getEntityLinkCount(), 1 );
    }

    assertEquals( action.areEntityLinksPending(), false );

    assertEquals( action.areChangesPending(), false );
    assertEquals( action.areEntityLinksCalculated(), true );
    assertEquals( action.areEntityLinksPending(), false );
    assertEquals( action.hasWorldBeenNotified(), false );

    action.markWorldAsNotified();

    assertEquals( action.areChangesPending(), false );
    assertEquals( action.areEntityLinksCalculated(), true );
    assertEquals( action.areEntityLinksPending(), false );
    assertEquals( action.hasWorldBeenNotified(), true );
  }

  @Test
  public void lifeCycleWithChannelUpdates()
  {
    // ChangeSet details
    final int sequence = 1;
    final String requestId = ValueUtil.randomString();

    // Channel updates
    final String filter1 = ValueUtil.randomString();
    final String filter2 = ValueUtil.randomString();
    final ChannelChange channelChange1 = ChannelChange.create( 42, ChannelChange.Action.ADD, filter1 );
    final ChannelChange channelChange2 = ChannelChange.create( 43, 1, ChannelChange.Action.UPDATE, filter2 );
    final ChannelChange channelChange3 = ChannelChange.create( 43, 2, ChannelChange.Action.REMOVE, null );
    final ChannelChange[] channelChanges = new ChannelChange[]{ channelChange1, channelChange2, channelChange3 };

    final EntityChange[] entityChanges = new EntityChange[ 0 ];

    final ChangeSet changeSet = ChangeSet.create( sequence, requestId, null, channelChanges, entityChanges );

    final Runnable runnable = mock( Runnable.class );
    final String rawJsonData = ValueUtil.randomString();
    final DataLoadAction action = new DataLoadAction( rawJsonData, false );

    final String requestKey = ValueUtil.randomString();
    final RequestEntry request = new RequestEntry( requestId, requestKey, null );

    //Ensure the initial state is as expected
    assertEquals( action.getRawJsonData(), rawJsonData );
    assertThrows( action::getChangeSet );

    assertEquals( action.needsChannelActionsProcessed(), false );
    assertEquals( action.areChangesPending(), false );
    assertEquals( action.areEntityLinksCalculated(), false );
    assertEquals( action.areEntityLinksPending(), false );
    assertEquals( action.hasWorldBeenNotified(), false );

    assertNull( action.getRunnable() );

    request.setNormalCompletionAction( runnable );

    action.setChangeSet( changeSet, request );

    assertEquals( action.getRunnable(), runnable );
    assertEquals( action.getChangeSet(), changeSet );
    assertEquals( action.getRequest(), request );
    assertEquals( action.getRawJsonData(), null );

    assertEquals( action.needsChannelActionsProcessed(), true );
    assertEquals( action.needsChannelActionsProcessed(), true );
    assertEquals( action.areChangesPending(), false );
    assertEquals( action.areEntityLinksCalculated(), false );
    assertEquals( action.areEntityLinksPending(), false );
    assertEquals( action.hasWorldBeenNotified(), false );

    // processed as sinlge block in caller
    action.markChannelActionsProcessed();
    assertEquals( action.needsChannelActionsProcessed(), false );

    assertEquals( action.areChangesPending(), false );
    assertEquals( action.areEntityLinksCalculated(), false );
    assertEquals( action.areEntityLinksPending(), false );
    assertEquals( action.hasWorldBeenNotified(), false );

    action.calculateEntitiesToLink();

    assertEquals( action.needsChannelActionsProcessed(), false );
    assertEquals( action.areChangesPending(), false );
    assertEquals( action.areEntityLinksCalculated(), true );
    assertEquals( action.areEntityLinksPending(), false );
    assertEquals( action.hasWorldBeenNotified(), false );

    action.markWorldAsNotified();

    assertEquals( action.needsChannelActionsProcessed(), false );
    assertEquals( action.areChangesPending(), false );
    assertEquals( action.areEntityLinksCalculated(), true );
    assertEquals( action.areEntityLinksPending(), false );
    assertEquals( action.hasWorldBeenNotified(), true );
  }
}
