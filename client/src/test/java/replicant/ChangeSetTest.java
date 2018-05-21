package replicant;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChangeSetTest
  extends AbstractReplicantTest
{
  @Test
  public void construct()
  {
    final EntityChange[] entityChanges = {};
    final ChannelChange[] channelChanges = {};

    final int sequence = ValueUtil.randomInt();
    final String requestId = ValueUtil.randomString();
    final String eTag = ValueUtil.randomString();

    final ChangeSet changeSet =
      ChangeSet.create( sequence, requestId, eTag, channelChanges, entityChanges );

    assertEquals( changeSet.getSequence(), sequence );
    assertEquals( changeSet.getRequestId(), requestId );
    assertEquals( changeSet.getETag(), eTag );
    assertEquals( changeSet.getEntityChanges(), entityChanges );
    assertEquals( changeSet.hasEntityChanges(), true );
    assertEquals( changeSet.getChannelChanges(), channelChanges );
    assertEquals( changeSet.hasChannelChanges(), true );

    changeSet.validate();
  }

  @Test
  public void construct_NoChanges()
  {
    final int sequence = ValueUtil.randomInt();

    final ChangeSet changeSet =
      ChangeSet.create( sequence, null, null, null, null );

    assertEquals( changeSet.getSequence(), sequence );
    assertEquals( changeSet.getRequestId(), null );
    assertEquals( changeSet.getETag(), null );
    assertEquals( changeSet.hasEntityChanges(), false );
    assertEquals( changeSet.hasChannelChanges(), false );

    changeSet.validate();
  }

  @Test
  public void validate_whereAllOK()
  {
    final ChannelChange[] channelChanges = new ChannelChange[]{
      ChannelChange.create( 1, ChannelChange.Action.ADD, null ),
      ChannelChange.create( 2, 50, ChannelChange.Action.ADD, null ),
      ChannelChange.create( 3, 50, ChannelChange.Action.ADD, null ),
      ChannelChange.create( 4, 23, ChannelChange.Action.ADD, null ),
      ChannelChange.create( 4, 24, ChannelChange.Action.ADD, null ),
      ChannelChange.create( 4, 25, ChannelChange.Action.ADD, null ),
      ChannelChange.create( 5, 1, ChannelChange.Action.ADD, null )
    };
    final EntityChange[] entityChanges = new EntityChange[]{
      EntityChange.create( 1, 1, new EntityChannel[ 0 ] ),
      EntityChange.create( 2, 1, new EntityChannel[ 0 ], new EntityChangeDataImpl() ),
      EntityChange.create( 3, 1, new EntityChannel[ 0 ], new EntityChangeDataImpl() ),
      EntityChange.create( 4, 1, new EntityChannel[ 0 ] ),
      EntityChange.create( 33, 2, new EntityChannel[ 0 ], new EntityChangeDataImpl() ),
      EntityChange.create( 34, 3, new EntityChannel[ 0 ], new EntityChangeDataImpl() ),
      EntityChange.create( 1, 4, new EntityChannel[ 0 ] )
    };

    final ChangeSet changeSet =
      ChangeSet.create( ValueUtil.randomInt(), null, null, channelChanges, entityChanges );

    changeSet.validate();
  }

  @Test
  public void validate_duplicateChannelActions_typeChannel()
  {
    final ChannelChange[] channelChanges = new ChannelChange[]{
      ChannelChange.create( 1, ChannelChange.Action.ADD, null ),
      ChannelChange.create( 2, 50, ChannelChange.Action.ADD, null ),
      ChannelChange.create( 3, 50, ChannelChange.Action.ADD, null ),
      ChannelChange.create( 4, 23, ChannelChange.Action.ADD, null ),
      ChannelChange.create( 1, ChannelChange.Action.REMOVE, null )
    };

    final ChangeSet changeSet =
      ChangeSet.create( 77, null, null, channelChanges, null );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, changeSet::validate );
    assertEquals( exception.getMessage(),
                  "Replicant-0022: ChangeSet 77 contains multiple ChannelChange messages for the channel with id 1." );
  }

  @Test
  public void validate_duplicateChannelActions_instanceChannel()
  {
    final ChannelChange[] channelChanges = new ChannelChange[]{
      ChannelChange.create( 1, ChannelChange.Action.ADD, null ),
      ChannelChange.create( 2, 50, ChannelChange.Action.ADD, "XX" ),
      ChannelChange.create( 2, 50, ChannelChange.Action.UPDATE, "XY" ),
      ChannelChange.create( 4, 23, ChannelChange.Action.ADD, null ),
      };

    final ChangeSet changeSet =
      ChangeSet.create( 77, null, null, channelChanges, null );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, changeSet::validate );
    assertEquals( exception.getMessage(),
                  "Replicant-0022: ChangeSet 77 contains multiple ChannelChange messages for the channel with id 2 and the subChannel with id 50." );
  }

  @Test
  public void validate_duplicateEntityChanges()
  {
    final EntityChange[] entityChanges = new EntityChange[]{
      EntityChange.create( 1, 1, new EntityChannel[ 0 ], new EntityChangeDataImpl() ),
      EntityChange.create( 2, 1, new EntityChannel[ 0 ], new EntityChangeDataImpl() ),
      EntityChange.create( 3, 1, new EntityChannel[ 0 ], new EntityChangeDataImpl() ),
      EntityChange.create( 4, 1, new EntityChannel[ 0 ] ),
      EntityChange.create( 33, 2, new EntityChannel[ 0 ], new EntityChangeDataImpl() ),
      EntityChange.create( 34, 3, new EntityChannel[ 0 ], new EntityChangeDataImpl() ),
      EntityChange.create( 1, 1, new EntityChannel[ 0 ] )
    };

    final ChangeSet changeSet =
      ChangeSet.create( 77, null, null, null, entityChanges );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, changeSet::validate );
    assertEquals( exception.getMessage(),
                  "Replicant-0014: ChangeSet 77 contains multiple EntityChange messages for the entity of type 1 and id 1." );
  }

  @Test
  public void getChannelChanges_WhenNone()
  {
    final ChangeSet changeSet =
      ChangeSet.create( ValueUtil.randomInt(), null, null, null, null );

    final IllegalStateException exception = expectThrows( IllegalStateException.class, changeSet::getChannelChanges );
    assertEquals( exception.getMessage(),
                  "Replicant-0013: ChangeSet.getChannelChanges() invoked when no changes are present. Should guard call with ChangeSet.hasChannelChanges()." );
  }

  @Test
  public void getEntityChanges_WhenNone()
  {
    final ChangeSet changeSet =
      ChangeSet.create( ValueUtil.randomInt(), null, null, null, null );

    final IllegalStateException exception = expectThrows( IllegalStateException.class, changeSet::getEntityChanges );
    assertEquals( exception.getMessage(),
                  "Replicant-0012: ChangeSet.getEntityChanges() invoked when no changes are present. Should guard call with ChangeSet.hasEntityChanges()." );
  }
}
