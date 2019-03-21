package replicant.spy;

import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ReplicantTestUtil;
import static org.testng.Assert.*;

public class DataLoadStatusTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final int requestId = 44;
    final int channelAddCount = 4;
    final int channelUpdateCount = 2;
    final int channelRemoveCount = 1;
    final int entityUpdateCount = 123;
    final int entityRemoveCount = 3;
    final int entityLinkCount = 126;
    final DataLoadStatus status =
      new DataLoadStatus( requestId,
                          channelAddCount,
                          channelUpdateCount,
                          channelRemoveCount,
                          entityUpdateCount,
                          entityRemoveCount,
                          entityLinkCount );

    assertEquals( status.getRequestId(), (Integer) requestId );
    assertEquals( status.getChannelAddCount(), channelAddCount );
    assertEquals( status.getChannelUpdateCount(), channelUpdateCount );
    assertEquals( status.getChannelRemoveCount(), channelRemoveCount );
    assertEquals( status.getEntityUpdateCount(), entityUpdateCount );
    assertEquals( status.getEntityRemoveCount(), entityRemoveCount );
    assertEquals( status.getEntityLinkCount(), entityLinkCount );

    assertEquals( status.toString(),
                  "[Message involved 4 subscribes, 2 subscription updates, 1 un-subscribes, 123 updates, 3 removes and 126 links]" );

    ReplicantTestUtil.disableNames();

    assertEquals( status.toString(), "replicant.spy.DataLoadStatus@" + Integer.toHexString( status.hashCode() ) );
  }
}
