package replicant;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.spy.DataLoadStatus;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

@SuppressWarnings( "ResultOfMethodCallIgnored" )
public class MessageResponseTest
  extends AbstractReplicantTest
{
  @Test
  public void construct()
  {
    final String rawJsonData = "";
    final MessageResponse action = new MessageResponse( rawJsonData );
    assertEquals( action.getRawJsonData(), rawJsonData );
    assertEquals( action.isOob(), false );

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
    final MessageResponse action1 = new MessageResponse( "" );
    final MessageResponse action2 = new MessageResponse( "" );
    final MessageResponse action3 = new MessageResponse( "", mock( SafeProcedure.class ) );
    final MessageResponse action4 = new MessageResponse( "", mock( SafeProcedure.class ) );

    final ChangeSet changeSet1 = ChangeSet.create( 1, null, null, new ChannelChange[ 0 ], new EntityChange[ 0 ] );
    final ChangeSet changeSet2 = ChangeSet.create( 2, null, null, new ChannelChange[ 0 ], new EntityChange[ 0 ] );
    final ChangeSet changeSet3 = ChangeSet.create( 3, null, null, new ChannelChange[ 0 ], new EntityChange[ 0 ] );
    final ChangeSet changeSet4 = ChangeSet.create( 4, null, null, new ChannelChange[ 0 ], new EntityChange[ 0 ] );

    action1.recordChangeSet( changeSet1, null );
    action2.recordChangeSet( changeSet2, null );
    action3.recordChangeSet( changeSet3, null );
    action4.recordChangeSet( changeSet4, null );

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

    final MessageResponse action = new MessageResponse( ValueUtil.randomString() );
    action.recordChangeSet( changeSet, null );

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

    final MessageResponse action = new MessageResponse( ValueUtil.randomString() );

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
    final MessageResponse action = new MessageResponse( ValueUtil.randomString() );
    assertEquals( action.toString(),
                  "DataLoad[,RawJson.null?=false,ChangeSet.null?=true,ChangeIndex=0,CompletionAction.null?=true,UpdatedEntities.size=0]" );

    final int sequence = 33;
    final String requestId = "XXX";
    final ChangeSet changeSet =
      ChangeSet.create( sequence, requestId, null, new ChannelChange[ 0 ], new EntityChange[ 0 ] );
    action.recordChangeSet( changeSet, null );

    assertEquals( action.toString(),
                  "DataLoad[,RawJson.null?=true,ChangeSet.null?=false,ChangeIndex=0,CompletionAction.null?=true,UpdatedEntities.size=0]" );

    // Null out Entities
    action.nextEntityToLink();

    assertEquals( action.toString(),
                  "DataLoad[,RawJson.null?=true,ChangeSet.null?=false,ChangeIndex=0,CompletionAction.null?=true,UpdatedEntities.size=0]" );

    ReplicantTestUtil.disableNames();

    assertEquals( action.toString(),
                  "replicant.MessageResponse@" + Integer.toHexString( System.identityHashCode( action ) ) );
  }

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

    final SafeProcedure completionAction = mock( SafeProcedure.class );
    final String rawJsonData = ValueUtil.randomString();
    final MessageResponse action = new MessageResponse( rawJsonData );

    final String requestKey = ValueUtil.randomString();
    final RequestEntry request = new RequestEntry( requestId, requestKey, null );

    //Ensure the initial state is as expected
    assertEquals( action.getRawJsonData(), rawJsonData );
    assertThrows( action::getChangeSet );

    assertEquals( action.needsChannelActionsProcessed(), false );
    assertEquals( action.areChangesPending(), false );
    assertEquals( action.areEntityLinksPending(), false );
    assertEquals( action.hasWorldBeenNotified(), false );

    assertNull( action.getCompletionAction() );

    request.setCompletionAction( completionAction );

    action.recordChangeSet( changeSet, request );

    assertEquals( request.getCompletionAction(), completionAction );
    assertEquals( action.getChangeSet(), changeSet );
    assertEquals( action.getRequest(), request );
    assertEquals( action.getRawJsonData(), null );

    assertEquals( action.needsChannelActionsProcessed(), false );
    assertEquals( action.areChangesPending(), true );
    assertEquals( action.areEntityLinksPending(), false );
    assertEquals( action.hasWorldBeenNotified(), false );

    // Process entity changes
    {
      assertEquals( action.nextChange(), entityChanges[ 0 ] );
      action.changeProcessed( entities[ 0 ] );
      action.incEntityUpdateCount();

      assertEquals( action.areChangesPending(), true );

      assertEquals( action.nextChange(), entityChanges[ 1 ] );
      action.incEntityRemoveCount();

      assertEquals( action.areChangesPending(), true );

      assertEquals( action.nextChange(), entityChanges[ 2 ] );
      action.incEntityUpdateCount();
      action.changeProcessed( entities[ 2 ] );

      assertEquals( action.areChangesPending(), false );

      assertEquals( action.nextChange(), null );

      assertEquals( action.areChangesPending(), false );

      assertEquals( action.getEntityUpdateCount(), 2 );
      assertEquals( action.getEntityRemoveCount(), 1 );
    }

    assertEquals( action.needsChannelActionsProcessed(), false );
    assertEquals( action.areChangesPending(), false );
    assertEquals( action.areEntityLinksPending(), true );
    assertEquals( action.hasWorldBeenNotified(), false );

    // process links
    {
      assertEquals( action.nextEntityToLink(), entities[ 0 ] );
      action.incEntityLinkCount();
      assertEquals( action.nextEntityToLink(), null );
      assertEquals( action.getEntityLinkCount(), 1 );
    }

    assertEquals( action.areEntityLinksPending(), false );

    assertEquals( action.areChangesPending(), false );
    assertEquals( action.areEntityLinksPending(), false );
    assertEquals( action.hasWorldBeenNotified(), false );

    action.markWorldAsNotified();

    assertEquals( action.areChangesPending(), false );
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

    final SafeProcedure completionAction = mock( SafeProcedure.class );
    final String rawJsonData = ValueUtil.randomString();
    final MessageResponse action = new MessageResponse( rawJsonData );

    final String requestKey = ValueUtil.randomString();
    final RequestEntry request = new RequestEntry( requestId, requestKey, null );

    //Ensure the initial state is as expected
    assertEquals( action.getRawJsonData(), rawJsonData );
    assertThrows( action::getChangeSet );

    assertEquals( action.needsChannelActionsProcessed(), false );
    assertEquals( action.areChangesPending(), false );
    assertEquals( action.areEntityLinksPending(), false );
    assertEquals( action.hasWorldBeenNotified(), false );

    assertNull( action.getCompletionAction() );

    request.setCompletionAction( completionAction );

    action.recordChangeSet( changeSet, request );

    assertEquals( request.getCompletionAction(), completionAction );
    assertEquals( action.getChangeSet(), changeSet );
    assertEquals( action.getRequest(), request );
    assertEquals( action.getRawJsonData(), null );

    assertEquals( action.needsChannelActionsProcessed(), true );
    assertEquals( action.areChangesPending(), false );
    assertEquals( action.areEntityLinksPending(), false );
    assertEquals( action.hasWorldBeenNotified(), false );

    // processed as single block in caller
    action.markChannelActionsProcessed();

    assertEquals( action.needsChannelActionsProcessed(), false );
    assertEquals( action.areChangesPending(), false );
    assertEquals( action.areEntityLinksPending(), false );
    assertEquals( action.hasWorldBeenNotified(), false );

    action.markWorldAsNotified();

    assertEquals( action.needsChannelActionsProcessed(), false );
    assertEquals( action.areChangesPending(), false );
    assertEquals( action.areEntityLinksPending(), false );
    assertEquals( action.hasWorldBeenNotified(), true );
  }

  @Test
  public void lifeCycleWithOOBMessage()
  {
    // ChangeSet details
    final int sequence = 1;
    final String requestId = ValueUtil.randomString();

    // Channel updates
    final ChannelChange[] channelChanges = new ChannelChange[ 0 ];

    final EntityChange[] entityChanges = new EntityChange[ 0 ];

    final ChangeSet changeSet = ChangeSet.create( sequence, requestId, null, channelChanges, entityChanges );

    final SafeProcedure oobCompletionAction = mock( SafeProcedure.class );
    final String rawJsonData = ValueUtil.randomString();
    final MessageResponse action = new MessageResponse( rawJsonData, oobCompletionAction );

    //Ensure the initial state is as expected
    assertEquals( action.getCompletionAction(), oobCompletionAction );
    assertEquals( action.getRawJsonData(), rawJsonData );
    assertThrows( action::getChangeSet );

    assertEquals( action.needsChannelActionsProcessed(), false );
    assertEquals( action.areChangesPending(), false );
    assertEquals( action.areEntityLinksPending(), false );
    assertEquals( action.hasWorldBeenNotified(), false );

    action.recordChangeSet( changeSet, null );

    assertEquals( action.getChangeSet(), changeSet );
    assertEquals( action.getRequest(), null );
    assertEquals( action.getRawJsonData(), null );

    assertEquals( action.needsChannelActionsProcessed(), false );
    assertEquals( action.areChangesPending(), false );
    assertEquals( action.areEntityLinksPending(), false );
    assertEquals( action.hasWorldBeenNotified(), false );

    action.markWorldAsNotified();

    assertEquals( action.needsChannelActionsProcessed(), false );
    assertEquals( action.areChangesPending(), false );
    assertEquals( action.areEntityLinksPending(), false );
    assertEquals( action.hasWorldBeenNotified(), true );
  }

  @Test
  public void setChangeSet_includingRequestWithOOBMessage()
  {
    final MessageResponse action = new MessageResponse( ValueUtil.randomString(), mock( SafeProcedure.class ) );

    final ChangeSet changeSet =
      ChangeSet.create( ValueUtil.randomInt(),
                        "X1234",
                        null,
                        new ChannelChange[ 0 ],
                        new EntityChange[ 0 ] );

    final RequestEntry request = new RequestEntry( "X1234", "DoMagic", null );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> action.recordChangeSet( changeSet, request ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0010: Incorrectly associating a request named 'DoMagic' with requestId 'X1234' with an out-of-band message." );
  }

  @Test
  public void setChangeSet_mismatchedRequestId()
  {
    final MessageResponse action = new MessageResponse( ValueUtil.randomString() );

    final ChangeSet changeSet =
      ChangeSet.create( ValueUtil.randomInt(),
                        "X1234",
                        null,
                        new ChannelChange[ 0 ],
                        new EntityChange[ 0 ] );

    final RequestEntry request = new RequestEntry( "Y5678", ValueUtil.randomString(), null );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> action.recordChangeSet( changeSet, request ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0011: ChangeSet specified requestId 'X1234' but request with requestId 'Y5678' has been passed to recordChangeSet." );
  }
}