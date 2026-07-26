package replicant.spy;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ReplicantTestUtil;

public class DataLoadStatusTest extends AbstractReplicantTest {
    @Test
    public void basicOperation() {
        final int requestId = 44;
        final int subscriptionSubscribeCount = 4;
        final int subscriptionUpdateCount = 2;
        final int subscriptionUnsubscribeCount = 1;
        final int entityUpdateCount = 123;
        final int entityRemoveCount = 3;
        final int entityLinkCount = 126;
        final DataLoadStatus status = new DataLoadStatus(
                requestId,
                subscriptionSubscribeCount,
                subscriptionUpdateCount,
                subscriptionUnsubscribeCount,
                entityUpdateCount,
                entityRemoveCount,
                entityLinkCount);

        assertEquals(status.getRequestId(), (Integer) requestId);
        assertEquals(status.getSubscriptionSubscribeCount(), subscriptionSubscribeCount);
        assertEquals(status.getSubscriptionUpdateCount(), subscriptionUpdateCount);
        assertEquals(status.getSubscriptionUnsubscribeCount(), subscriptionUnsubscribeCount);
        assertEquals(status.getEntityUpdateCount(), entityUpdateCount);
        assertEquals(status.getEntityRemoveCount(), entityRemoveCount);
        assertEquals(status.getEntityLinkCount(), entityLinkCount);

        assertEquals(
                status.toString(),
                "[Message for request 44 involved 4 subscribes, 2 subscription updates, 1 unsubscribes, 123 updates,"
                        + " 3 removes and 126 links]");

        ReplicantTestUtil.disableNames();

        assertEquals(status.toString(), "replicant.spy.DataLoadStatus@" + Integer.toHexString(status.hashCode()));
    }
}
