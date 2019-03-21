package replicant.messages;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import static org.testng.Assert.*;

public class ChangeSetTest
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

    final ChangeSet changeSet =
      ChangeSet.create( requestId, eTag, channelChanges, fchannels, entityChanges );

    assertEquals( changeSet.getRequestId(), (Integer) requestId );
    assertEquals( changeSet.getETag(), eTag );
    assertEquals( changeSet.getEntityChanges(), entityChanges );
    assertTrue( changeSet.hasEntityChanges() );
    assertTrue( changeSet.hasChannels() );
    assertTrue( changeSet.hasFilteredChannels() );
    assertEquals( changeSet.getChannels(), channelChanges );
    assertEquals( changeSet.getFilteredChannels(), fchannels );

    changeSet.validate();
  }

  @Test
  public void construct_NoChanges()
  {
    final ChangeSet changeSet =
      ChangeSet.create( null, null, null, null, null );

    assertNull( changeSet.getRequestId() );
    assertNull( changeSet.getETag() );
    assertFalse( changeSet.hasEntityChanges() );
    assertFalse( changeSet.hasChannels() );
    assertFalse( changeSet.hasFilteredChannels() );

    changeSet.validate();
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

    final ChangeSet changeSet =
      ChangeSet.create( null, null, channelChanges, null, entityChanges );

    changeSet.validate();
  }

  @Test
  public void validate_duplicateChannelActions_typeChannel()
  {
    final String[] channelChanges = new String[]{ "+1", "+2.50", "+3.50", "+4.23", "+1" };

    final ChangeSet changeSet =
      ChangeSet.create( null, null, channelChanges, null, null );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, changeSet::validate );
    assertEquals( exception.getMessage(),
                  "Replicant-0022: ChangeSet contains multiple ChannelChange messages for the channel 1." );
  }

  @Test
  public void validate_duplicateChannelActions_instanceChannel()
  {
    final ChannelChange[] channelChanges = new ChannelChange[]{
      ChannelChange.create( "+2.50", "XX" ),
      ChannelChange.create( "=2.50", "XY" )
    };

    final ChangeSet changeSet =
      ChangeSet.create( null, null, new String[]{ "+1", "+4.23" }, channelChanges, null );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, changeSet::validate );
    assertEquals( exception.getMessage(),
                  "Replicant-0028: ChangeSet contains multiple ChannelChange messages for the channel 2.50." );
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

    final ChangeSet changeSet =
      ChangeSet.create( null, null, null, null, entityChanges );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, changeSet::validate );
    assertEquals( exception.getMessage(),
                  "Replicant-0014: ChangeSet contains multiple EntityChange messages with the id '1.1'." );
  }

  @Test
  public void getChannels_WhenNone()
  {
    final ChangeSet changeSet = ChangeSet.create( null, null, null, null, null );

    final IllegalStateException exception = expectThrows( IllegalStateException.class, changeSet::getChannels );
    assertEquals( exception.getMessage(),
                  "Replicant-0013: ChangeSet.getChannels() invoked when no changes are present. Should guard call with ChangeSet.hasChannels()." );
  }

  @Test
  public void getFilteredChannels_WhenNone()
  {
    final ChangeSet changeSet = ChangeSet.create( null, null, null, null, null );

    final IllegalStateException exception = expectThrows( IllegalStateException.class, changeSet::getFilteredChannels );
    assertEquals( exception.getMessage(),
                  "Replicant-0030: ChangeSet.getFilteredChannels() invoked when no changes are present. Should guard call with ChangeSet.hasFilteredChannels()." );
  }

  @Test
  public void getEntityChanges_WhenNone()
  {
    final ChangeSet changeSet = ChangeSet.create( null, null, null, null, null );

    final IllegalStateException exception = expectThrows( IllegalStateException.class, changeSet::getEntityChanges );
    assertEquals( exception.getMessage(),
                  "Replicant-0012: ChangeSet.getEntityChanges() invoked when no changes are present. Should guard call with ChangeSet.hasEntityChanges()." );
  }
}
