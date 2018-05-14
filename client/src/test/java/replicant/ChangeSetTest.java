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
