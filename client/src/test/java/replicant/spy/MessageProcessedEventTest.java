package replicant.spy;

import static org.testng.Assert.*;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ValueUtil;

public class MessageProcessedEventTest extends AbstractReplicantTest {
    @Test
    public void basicOperation() {
        final int requestId = ValueUtil.randomInt();
        final int subscriptionSubscribeCount = ValueUtil.getRandom().nextInt(10);
        final int subscriptionUpdateCount = ValueUtil.getRandom().nextInt(10);
        final int subscriptionUnsubscribeCount = ValueUtil.getRandom().nextInt(10);
        final int entityUpdateCount = ValueUtil.getRandom().nextInt(100);
        final int entityRemoveCount = ValueUtil.getRandom().nextInt(100);
        final int entityLinkCount = ValueUtil.getRandom().nextInt(10);
        final DataLoadStatus dataLoadStatus = new DataLoadStatus(
                requestId,
                subscriptionSubscribeCount,
                subscriptionUpdateCount,
                subscriptionUnsubscribeCount,
                entityUpdateCount,
                entityRemoveCount,
                entityLinkCount);
        final MessageProcessedEvent event = new MessageProcessedEvent(23, "Rose", dataLoadStatus);

        assertEquals(event.getSystemSchemaId(), 23);
        assertEquals(event.getSystemSchemaName(), "Rose");
        assertEquals(event.getDataLoadStatus(), dataLoadStatus);

        final HashMap<String, Object> data = new HashMap<>();
        event.toMap(data);

        assertEquals(data.get("type"), "Connector.MessageProcess");
        assertEquals(data.get("systemSchema.id"), 23);
        assertEquals(data.get("systemSchema.name"), "Rose");
        assertEquals(data.get("requestId"), requestId);
        assertEquals(data.get("subscriptionSubscribeCount"), subscriptionSubscribeCount);
        assertEquals(data.get("subscriptionUpdateCount"), subscriptionUpdateCount);
        assertEquals(data.get("subscriptionUnsubscribeCount"), subscriptionUnsubscribeCount);
        assertEquals(data.get("entityUpdateCount"), entityUpdateCount);
        assertEquals(data.get("entityRemoveCount"), entityRemoveCount);
        assertEquals(data.get("entityLinkCount"), entityLinkCount);
        assertEquals(data.size(), 10);
    }
}
