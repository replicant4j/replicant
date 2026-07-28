package replicant.spy;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ReplicantTestUtil;

public class MessageProcessingSummaryTest extends AbstractReplicantTest {
    @Test
    public void basicOperation() {
        final int requestId = 44;
        final int subscriptionSubscribeCount = 4;
        final int subscriptionUpdateCount = 2;
        final int subscriptionUnsubscribeCount = 1;
        final int entityUpdateCount = 123;
        final int entityRemoveCount = 3;
        final int entityLinkCount = 126;
        final MessageProcessingSummary summary = new MessageProcessingSummary(
                requestId,
                subscriptionSubscribeCount,
                subscriptionUpdateCount,
                subscriptionUnsubscribeCount,
                entityUpdateCount,
                entityRemoveCount,
                entityLinkCount);

        assertEquals(summary.getRequestId(), (Integer) requestId);
        assertEquals(summary.getSubscriptionSubscribeCount(), subscriptionSubscribeCount);
        assertEquals(summary.getSubscriptionUpdateCount(), subscriptionUpdateCount);
        assertEquals(summary.getSubscriptionUnsubscribeCount(), subscriptionUnsubscribeCount);
        assertEquals(summary.getEntityUpdateCount(), entityUpdateCount);
        assertEquals(summary.getEntityRemoveCount(), entityRemoveCount);
        assertEquals(summary.getEntityLinkCount(), entityLinkCount);

        assertEquals(
                summary.toString(),
                "[Message for request 44 involved 4 subscribes, 2 subscription updates, 1 unsubscribes, 123 updates,"
                        + " 3 removes and 126 links]");

        ReplicantTestUtil.disableNames();

        assertEquals(
                summary.toString(),
                "replicant.spy.MessageProcessingSummary@" + Integer.toHexString(summary.hashCode()));
    }
}
