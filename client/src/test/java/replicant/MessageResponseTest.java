package replicant;

import arez.component.Linkable;
import org.testng.annotations.Test;
import replicant.messages.ChannelChange;
import replicant.messages.EntityChange;
import replicant.messages.EntityChangeDataImpl;
import replicant.messages.UpdateMessage;
import replicant.spy.DataLoadStatus;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class MessageResponseTest
  extends AbstractReplicantTest
{
  @Test
  public void construct()
  {
    final var action =
      new MessageResponse( 1, UpdateMessage.create( null, null, null, null, null, null ), null );

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
    final var changeSet =
      UpdateMessage.create( null, null, null, new ChannelChange[ 0 ], new EntityChange[ 0 ], null );

    final var action = new MessageResponse( 1, changeSet, null );

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

    final var status = action.toStatus();

    assertNull( status.getRequestId() );
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

    final var action = new MessageResponse( 1, new UpdateMessage(), null );

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
    final var changeSet =
      UpdateMessage.create( null, null, null, new ChannelChange[ 0 ], new EntityChange[ 0 ], null );
    final var action = new MessageResponse( 1, changeSet, null );
    assertEquals( action.toString(),
                  "MessageResponse[Type=update,RequestId=null,ChangeIndex=0,EntitiesToLink.size=0]" );

    // Null out Entities
    action.nextEntityToLink();

    assertEquals( action.toString(),
                  "MessageResponse[Type=update,RequestId=null,ChangeIndex=0,EntitiesToLink.size=0]" );

    ReplicantTestUtil.disableNames();

    assertEquals( action.toString(),
                  "replicant.MessageResponse@" + Integer.toHexString( System.identityHashCode( action ) ) );
  }

  @Test
  public void lifeCycleWithNormallyCompletedRequest()
  {
    // ChangeSet details
    final var requestId = ValueUtil.randomInt();

    // Channel updates
    final var channelChanges = new ChannelChange[ 0 ];

    // Entity Updates
    final var channelId = 22;

    // Entity update
    final var change1 =
      EntityChange.create( 100, 50,
                           new String[]{ String.valueOf( channelId ) },
                           new EntityChangeDataImpl() );
    // Entity Remove
    final var change2 =
      EntityChange.create( 100, 51,
                           new String[]{ String.valueOf( channelId ) } );
    // Entity update - non linkable
    final var change3 =
      EntityChange.create( 100, 52,
                           new String[]{ String.valueOf( channelId ) },
                           new EntityChangeDataImpl() );
    final var entityChanges = new EntityChange[]{ change1, change2, change3 };

    final var entities = new Object[]{ mock( Linkable.class ), new Object(), new Object() };

    final var changeSet =
      UpdateMessage.create( requestId, null, null, channelChanges, entityChanges, null );

    final var requestKey = ValueUtil.randomString();
    final var request = new RequestEntry( requestId, requestKey, false, null );

    final var action = new MessageResponse( 1, changeSet, request );

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
    final var requestId = ValueUtil.randomInt();

    // Channel updates
    final var filter1 = ValueUtil.randomString();
    final var filter2 = ValueUtil.randomString();
    final var channelChange1 = ChannelChange.create( "+42", filter1 );
    final var channelChange2 = ChannelChange.create( "=43.1", filter2 );
    final var channelChanges = new ChannelChange[]{ channelChange1, channelChange2 };

    final var entityChanges = new EntityChange[ 0 ];

    final var changeSet =
      UpdateMessage.create( requestId, null, new String[]{ "-43.2" }, channelChanges, entityChanges, null );
    final var requestKey = ValueUtil.randomString();
    final var request = new RequestEntry( requestId, requestKey, false, null );

    final var action = new MessageResponse( 1, changeSet, request );

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
    final var changeSet =
      UpdateMessage.create( 1234, null, null, null, null, null );
    final var request = new RequestEntry( 5678, ValueUtil.randomString(), false, null );

    final var exception =
      expectThrows( IllegalStateException.class, () -> new MessageResponse( 1, changeSet, request ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0011: Response message specified requestId '1234' but request specified requestId '5678'." );
  }
}
