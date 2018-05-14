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
    assertEquals( changeSet.getChannelChanges(), channelChanges );
  }
}
