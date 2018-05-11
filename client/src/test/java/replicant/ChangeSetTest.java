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

    final int sequence = 1;
    final String requestId = ValueUtil.randomString();
    final String eTag = null;

    final ChangeSet changeSet =
      ChangeSet.create( sequence, requestId, eTag, channelChanges, entityChanges );

    assertEquals( changeSet.getSequence(), sequence );
    assertEquals( changeSet.getRequestID(), requestId );
    assertEquals( changeSet.getETag(), eTag );
    assertEquals( changeSet.getEntityChanges(), entityChanges );
    assertEquals( changeSet.getChannelChanges(), channelChanges );
  }
}
