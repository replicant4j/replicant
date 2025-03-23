package replicant.messages;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import static org.testng.Assert.*;

public class UpdateMessageTest
  extends AbstractReplicantTest
{
  @Test
  public void construct()
  {
    final EntityChange[] entityChanges = {};
    final String[] channelChanges = {};
    final ChannelChange[] fchannels = new ChannelChange[ 0 ];

    final int requestId = ValueUtil.randomInt();
    final String eTag = ValueUtil.randomString();

    final UpdateMessage updateMessage =
      UpdateMessage.create( requestId, eTag, channelChanges, fchannels, entityChanges );

    assertEquals( updateMessage.getRequestId(), (Integer) requestId );
    assertEquals( updateMessage.getETag(), eTag );
    assertEquals( updateMessage.getEntityChanges(), entityChanges );
    assertTrue( updateMessage.hasEntityChanges() );
    assertTrue( updateMessage.hasChannels() );
    assertTrue( updateMessage.hasFilteredChannels() );
    assertEquals( updateMessage.getChannels(), channelChanges );
    assertEquals( updateMessage.getFilteredChannels(), fchannels );

    updateMessage.validate();
  }

  @Test
  public void construct_NoChanges()
  {
    final UpdateMessage updateMessage =
      UpdateMessage.create( null, null, null, null, null );

    assertNull( updateMessage.getRequestId() );
    assertNull( updateMessage.getETag() );
    assertFalse( updateMessage.hasEntityChanges() );
    assertFalse( updateMessage.hasChannels() );
    assertFalse( updateMessage.hasFilteredChannels() );

    updateMessage.validate();
  }

  @Test
  public void validate_whereAllOK()
  {
    final String[] channelChanges = new String[]{ "+1", "+2.50", "+3.50", "+4.23", "+4.24", "+4.25", "+5.1" };
    final EntityChange[] entityChanges = new EntityChange[]{
      EntityChange.create( 1, 1, new String[ 0 ] ),
      EntityChange.create( 1, 2, new String[ 0 ], new EntityChangeDataImpl() ),
      EntityChange.create( 1, 3, new String[ 0 ], new EntityChangeDataImpl() ),
      EntityChange.create( 1, 4, new String[ 0 ] ),
      EntityChange.create( 2, 33, new String[ 0 ], new EntityChangeDataImpl() ),
      EntityChange.create( 3, 34, new String[ 0 ], new EntityChangeDataImpl() ),
      EntityChange.create( 4, 1, new String[ 0 ] )
    };

    final UpdateMessage updateMessage =
      UpdateMessage.create( null, null, channelChanges, null, entityChanges );

    updateMessage.validate();
  }

  @Test
  public void validate_duplicateChannelActions_typeChannel()
  {
    final String[] channelChanges = new String[]{ "+1", "+2.50", "+3.50", "+4.23", "+1" };

    final UpdateMessage updateMessage =
      UpdateMessage.create( null, null, channelChanges, null, null );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, updateMessage::validate );
    assertEquals( exception.getMessage(),
                  "Replicant-0022: UpdateMessage contains multiple ChannelChange messages for the channel 1." );
  }

  @Test
  public void validate_duplicateChannelActions_instanceChannel()
  {
    final ChannelChange[] channelChanges = new ChannelChange[]{
      ChannelChange.create( "+2.50", "XX" ),
      ChannelChange.create( "=2.50", "XY" )
    };

    final UpdateMessage updateMessage =
      UpdateMessage.create( null, null, new String[]{ "+1", "+4.23" }, channelChanges, null );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, updateMessage::validate );
    assertEquals( exception.getMessage(),
                  "Replicant-0028: UpdateMessage contains multiple ChannelChange messages for the channel 2.50." );
  }

  @Test
  public void validate_duplicateEntityChanges()
  {
    final EntityChange[] entityChanges = new EntityChange[]{
      EntityChange.create( 1, 1, new String[ 0 ], new EntityChangeDataImpl() ),
      EntityChange.create( 1, 2, new String[ 0 ], new EntityChangeDataImpl() ),
      EntityChange.create( 1, 3, new String[ 0 ], new EntityChangeDataImpl() ),
      EntityChange.create( 1, 4, new String[ 0 ] ),
      EntityChange.create( 2, 33, new String[ 0 ], new EntityChangeDataImpl() ),
      EntityChange.create( 3, 34, new String[ 0 ], new EntityChangeDataImpl() ),
      EntityChange.create( 1, 1, new String[ 0 ] )
    };

    final UpdateMessage updateMessage =
      UpdateMessage.create( null, null, null, null, entityChanges );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, updateMessage::validate );
    assertEquals( exception.getMessage(),
                  "Replicant-0014: UpdateMessage contains multiple EntityChange messages with the id '1.1'." );
  }

  @Test
  public void getChannels_WhenNone()
  {
    final UpdateMessage updateMessage = UpdateMessage.create( null, null, null, null, null );

    final IllegalStateException exception = expectThrows( IllegalStateException.class, updateMessage::getChannels );
    assertEquals( exception.getMessage(),
                  "Replicant-0013: UpdateMessage.getChannels() invoked when no changes are present. Should guard call with UpdateMessage.hasChannels()." );
  }

  @Test
  public void getFilteredChannels_WhenNone()
  {
    final UpdateMessage updateMessage = UpdateMessage.create( null, null, null, null, null );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, updateMessage::getFilteredChannels );
    assertEquals( exception.getMessage(),
                  "Replicant-0030: UpdateMessage.getFilteredChannels() invoked when no changes are present. Should guard call with UpdateMessage.hasFilteredChannels()." );
  }

  @Test
  public void getEntityChanges_WhenNone()
  {
    final UpdateMessage updateMessage = UpdateMessage.create( null, null, null, null, null );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, updateMessage::getEntityChanges );
    assertEquals( exception.getMessage(),
                  "Replicant-0012: UpdateMessage.getEntityChanges() invoked when no changes are present. Should guard call with UpdateMessage.hasEntityChanges()." );
  }
}
