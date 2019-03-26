package replicant;

import arez.component.Linkable;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.messages.ChangeSetMessage;
import replicant.messages.ChannelChange;
import replicant.messages.EntityChange;
import replicant.messages.EntityChangeDataImpl;
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
    assertFalse( action.isOob() );

    assertFalse( action.areEntityLinksPending() );
    assertFalse( action.areEntityChangesPending() );
    assertFalse( action.hasWorldBeenValidated() );

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

    final ChangeSetMessage changeSet1 = ChangeSetMessage.create( null, null, null, new ChannelChange[ 0 ], new EntityChange[ 0 ] );
    final ChangeSetMessage changeSet2 = ChangeSetMessage.create( null, null, null, new ChannelChange[ 0 ], new EntityChange[ 0 ] );
    final ChangeSetMessage changeSet3 = ChangeSetMessage.create( null, null, null, new ChannelChange[ 0 ], new EntityChange[ 0 ] );
    final ChangeSetMessage changeSet4 = ChangeSetMessage.create( null, null, null, new ChannelChange[ 0 ], new EntityChange[ 0 ] );

    action1.recordChangeSet( changeSet1, null );
    action2.recordChangeSet( changeSet2, null );
    action3.recordChangeSet( changeSet3, null );
    action4.recordChangeSet( changeSet4, null );

    assertEquals( action1.compareTo( action1 ), 0 );
    assertEquals( action1.compareTo( action2 ), 0 );
    assertEquals( action1.compareTo( action3 ), 1 );
    assertEquals( action1.compareTo( action4 ), 1 );
    assertEquals( action2.compareTo( action1 ), 0 );
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
    final int requestId = ValueUtil.randomInt();
    final ChangeSetMessage changeSet =
      ChangeSetMessage.create( requestId, null, null, new ChannelChange[ 0 ], new EntityChange[ 0 ] );

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

    assertEquals( status.getRequestId(), (Integer) requestId );
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

    final int requestId = 767576;
    final ChangeSetMessage changeSet =
      ChangeSetMessage.create( requestId, null, null, new ChannelChange[ 0 ], new EntityChange[ 0 ] );
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
    final int requestId = ValueUtil.randomInt();

    // Channel updates
    final ChannelChange[] channelChanges = new ChannelChange[ 0 ];

    // Entity Updates
    final int channelId = 22;

    // Entity update
    final EntityChange change1 =
      EntityChange.create( 100, 50,
                           new String[]{ String.valueOf( channelId ) },
                           new EntityChangeDataImpl() );
    // Entity Remove
    final EntityChange change2 =
      EntityChange.create( 100, 51,
                           new String[]{ String.valueOf( channelId ) } );
    // Entity update - non linkable
    final EntityChange change3 =
      EntityChange.create( 100, 52,
                           new String[]{ String.valueOf( channelId ) },
                           new EntityChangeDataImpl() );
    final EntityChange[] entityChanges = new EntityChange[]{ change1, change2, change3 };

    final Object[] entities = new Object[]{ mock( Linkable.class ), new Object(), new Object() };

    final ChangeSetMessage changeSet =
      ChangeSetMessage.create( requestId, null, null, channelChanges, entityChanges );

    final SafeProcedure completionAction = mock( SafeProcedure.class );
    final String rawJsonData = ValueUtil.randomString();
    final MessageResponse action = new MessageResponse( rawJsonData );

    final String requestKey = ValueUtil.randomString();
    final RequestEntry request = new RequestEntry( requestId, requestKey );

    //Ensure the initial state is as expected
    assertEquals( action.getRawJsonData(), rawJsonData );
    assertThrows( action::getChangeSet );

    assertFalse( action.needsChannelChangesProcessed() );
    assertFalse( action.areEntityChangesPending() );
    assertFalse( action.areEntityLinksPending() );
    assertFalse( action.hasWorldBeenValidated() );

    assertNull( action.getCompletionAction() );

    request.setCompletionAction( completionAction );

    action.recordChangeSet( changeSet, request );
    request.setNormalCompletion( true );

    assertEquals( action.getCompletionAction(), completionAction );
    assertEquals( request.getCompletionAction(), completionAction );
    assertEquals( action.getChangeSet(), changeSet );
    assertEquals( action.getRequest(), request );
    assertNull( action.getRawJsonData() );

    assertFalse( action.needsChannelChangesProcessed() );
    assertTrue( action.areEntityChangesPending() );
    assertFalse( action.areEntityLinksPending() );
    assertFalse( action.hasWorldBeenValidated() );

    // Process entity changes
    {
      assertEquals( action.nextEntityChange(), entityChanges[ 0 ] );
      action.changeProcessed( entities[ 0 ] );
      action.incEntityUpdateCount();

      assertTrue( action.areEntityChangesPending() );

      assertEquals( action.nextEntityChange(), entityChanges[ 1 ] );
      action.incEntityRemoveCount();

      assertTrue( action.areEntityChangesPending() );

      assertEquals( action.nextEntityChange(), entityChanges[ 2 ] );
      action.incEntityUpdateCount();
      action.changeProcessed( entities[ 2 ] );

      assertFalse( action.areEntityChangesPending() );

      assertNull( action.nextEntityChange() );

      assertFalse( action.areEntityChangesPending() );

      assertEquals( action.getEntityUpdateCount(), 2 );
      assertEquals( action.getEntityRemoveCount(), 1 );
    }

    assertFalse( action.needsChannelChangesProcessed() );
    assertFalse( action.areEntityChangesPending() );
    assertTrue( action.areEntityLinksPending() );
    assertFalse( action.hasWorldBeenValidated() );

    // process links
    {
      assertEquals( action.nextEntityToLink(), entities[ 0 ] );
      action.incEntityLinkCount();
      assertNull( action.nextEntityToLink() );
      assertEquals( action.getEntityLinkCount(), 1 );
    }

    assertFalse( action.areEntityLinksPending() );

    assertFalse( action.areEntityChangesPending() );
    assertFalse( action.areEntityLinksPending() );
    assertFalse( action.hasWorldBeenValidated() );

    action.markWorldAsValidated();

    assertFalse( action.areEntityChangesPending() );
    assertFalse( action.areEntityLinksPending() );
    assertTrue( action.hasWorldBeenValidated() );
  }

  @Test
  public void lifeCycleWithChannelUpdates()
  {
    // ChangeSet details
    final int requestId = ValueUtil.randomInt();

    // Channel updates
    final String filter1 = ValueUtil.randomString();
    final String filter2 = ValueUtil.randomString();
    final ChannelChange channelChange1 = ChannelChange.create( "+42", filter1 );
    final ChannelChange channelChange2 = ChannelChange.create( "=43.1", filter2 );
    final ChannelChange[] channelChanges = new ChannelChange[]{ channelChange1, channelChange2 };

    final EntityChange[] entityChanges = new EntityChange[ 0 ];

    final ChangeSetMessage changeSet =
      ChangeSetMessage.create( requestId, null, new String[]{ "-43.2" }, channelChanges, entityChanges );

    final SafeProcedure completionAction = mock( SafeProcedure.class );
    final String rawJsonData = ValueUtil.randomString();
    final MessageResponse action = new MessageResponse( rawJsonData );

    final String requestKey = ValueUtil.randomString();
    final RequestEntry request = new RequestEntry( requestId, requestKey );

    //Ensure the initial state is as expected
    assertEquals( action.getRawJsonData(), rawJsonData );
    assertThrows( action::getChangeSet );

    assertFalse( action.needsChannelChangesProcessed() );
    assertFalse( action.areEntityChangesPending() );
    assertFalse( action.areEntityLinksPending() );
    assertFalse( action.hasWorldBeenValidated() );

    assertNull( action.getCompletionAction() );

    request.setCompletionAction( completionAction );

    action.recordChangeSet( changeSet, request );

    assertEquals( request.getCompletionAction(), completionAction );
    assertEquals( action.getChangeSet(), changeSet );
    assertEquals( action.getRequest(), request );
    assertNull( action.getRawJsonData() );

    assertTrue( action.needsChannelChangesProcessed() );
    assertFalse( action.areEntityChangesPending() );
    assertFalse( action.areEntityLinksPending() );
    assertFalse( action.hasWorldBeenValidated() );

    // processed as single block in caller
    action.markChannelActionsProcessed();

    assertFalse( action.needsChannelChangesProcessed() );
    assertFalse( action.areEntityChangesPending() );
    assertFalse( action.areEntityLinksPending() );
    assertFalse( action.hasWorldBeenValidated() );

    action.markWorldAsValidated();

    assertFalse( action.needsChannelChangesProcessed() );
    assertFalse( action.areEntityChangesPending() );
    assertFalse( action.areEntityLinksPending() );
    assertTrue( action.hasWorldBeenValidated() );
  }

  @Test
  public void lifeCycleWithOOBMessage()
  {
    // ChangeSet details
    final int requestId = ValueUtil.randomInt();

    final ChangeSetMessage changeSet =
      ChangeSetMessage.create( requestId, null, new String[ 0 ], new ChannelChange[ 0 ], new EntityChange[ 0 ] );

    final SafeProcedure oobCompletionAction = mock( SafeProcedure.class );
    final String rawJsonData = ValueUtil.randomString();
    final MessageResponse action = new MessageResponse( rawJsonData, oobCompletionAction );

    //Ensure the initial state is as expected
    assertEquals( action.getCompletionAction(), oobCompletionAction );
    assertEquals( action.getRawJsonData(), rawJsonData );
    assertThrows( action::getChangeSet );

    assertFalse( action.needsChannelChangesProcessed() );
    assertFalse( action.areEntityChangesPending() );
    assertFalse( action.areEntityLinksPending() );
    assertFalse( action.hasWorldBeenValidated() );

    action.recordChangeSet( changeSet, null );

    assertEquals( action.getChangeSet(), changeSet );
    assertNull( action.getRequest() );
    assertNull( action.getRawJsonData() );

    assertFalse( action.needsChannelChangesProcessed() );
    assertFalse( action.areEntityChangesPending() );
    assertFalse( action.areEntityLinksPending() );
    assertFalse( action.hasWorldBeenValidated() );

    action.markWorldAsValidated();

    assertFalse( action.needsChannelChangesProcessed() );
    assertFalse( action.areEntityChangesPending() );
    assertFalse( action.areEntityLinksPending() );
    assertTrue( action.hasWorldBeenValidated() );
  }

  @Test
  public void setChangeSet_includingRequestWithOOBMessage()
  {
    final MessageResponse action = new MessageResponse( ValueUtil.randomString(), mock( SafeProcedure.class ) );

    final ChangeSetMessage changeSet =
      ChangeSetMessage.create( 1234, null, null, null, null );

    final RequestEntry request = new RequestEntry( 1234, "DoMagic" );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> action.recordChangeSet( changeSet, request ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0010: Incorrectly associating a request named 'DoMagic' with requestId '1234' with an out-of-band message." );
  }

  @Test
  public void setChangeSet_mismatchedRequestId()
  {
    final MessageResponse action = new MessageResponse( ValueUtil.randomString() );

    final ChangeSetMessage changeSet =
      ChangeSetMessage.create( 1234, null, null, null, null );

    final RequestEntry request = new RequestEntry( 5678, ValueUtil.randomString() );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> action.recordChangeSet( changeSet, request ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0011: ChangeSet specified requestId '1234' but request with requestId '5678' has been passed to recordChangeSet." );
  }
}
