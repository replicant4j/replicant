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

public class MessageResponseTest
  extends AbstractReplicantTest
{
  @Test
  public void construct()
  {
    final MessageResponse action =
      new MessageResponse( 1, ChangeSetMessage.create( null, null, null, null, null ), null );

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

  @Test
  public void toStatus()
  {
    final ChangeSetMessage changeSet =
      ChangeSetMessage.create( null, null, null, new ChannelChange[ 0 ], new EntityChange[ 0 ] );

    final MessageResponse action = new MessageResponse( 1, changeSet, null );

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

    assertEquals( status.getRequestId(), null );
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

    final MessageResponse action = new MessageResponse( 1, new ChangeSetMessage(), null );

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
    final ChangeSetMessage changeSet =
      ChangeSetMessage.create( null, null, null, new ChannelChange[ 0 ], new EntityChange[ 0 ] );
    final MessageResponse action = new MessageResponse( 1, changeSet, null );
    assertEquals( action.toString(),
                  "MessageResponse[Type=update,RequestId=null,ChangeIndex=0,CompletionAction.null?=true,UpdatedEntities.size=0]" );

    // Null out Entities
    action.nextEntityToLink();

    assertEquals( action.toString(),
                  "MessageResponse[Type=update,RequestId=null,ChangeIndex=0,CompletionAction.null?=true,UpdatedEntities.size=0]" );

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

    final String requestKey = ValueUtil.randomString();
    final RequestEntry request = new RequestEntry( requestId, requestKey, false );

    final SafeProcedure completionAction = mock( SafeProcedure.class );
    final MessageResponse action = new MessageResponse( 1, changeSet, request );

    assertNull( action.getCompletionAction() );

    request.setCompletionAction( completionAction );

    request.setNormalCompletion( true );

    assertEquals( action.getCompletionAction(), completionAction );
    assertEquals( request.getCompletionAction(), completionAction );
    assertEquals( action.getMessage(), changeSet );
    assertEquals( action.getRequest(), request );

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
    final String requestKey = ValueUtil.randomString();
    final RequestEntry request = new RequestEntry( requestId, requestKey, false );

    final MessageResponse action = new MessageResponse( 1, changeSet, request );

    assertEquals( action.getMessage(), changeSet );
    assertEquals( action.getRequest(), request );

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
  public void setChangeSet_mismatchedRequestId()
  {
    final ChangeSetMessage changeSet =
      ChangeSetMessage.create( 1234, null, null, null, null );
    final RequestEntry request = new RequestEntry( 5678, ValueUtil.randomString(), false );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> new MessageResponse( 1, changeSet, request ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0011: Response message specified requestId '1234' but request specified requestId '5678'." );
  }
}
