package replicant.spy;

import static org.testng.Assert.*;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.DatasetAddress;
import replicant.ValueUtil;

public class SubscribeRequestQueuedEventTest extends AbstractReplicantTest {
    @Test
    public void basicOperation() {
        final DatasetAddress datasetAddress = new DatasetAddress(1, 2);
        final String filterParameter = ValueUtil.randomString();

        final SubscribeRequestQueuedEvent event = new SubscribeRequestQueuedEvent(datasetAddress, filterParameter);

        assertEquals(event.getDatasetAddress(), datasetAddress);
        assertEquals(event.getFilterParameter(), filterParameter);

        final HashMap<String, Object> data = new HashMap<>();
        event.toMap(data);

        assertEquals(data.get("type"), "Connector.SubscribeRequestQueued");
        assertEquals(data.get("datasetAddress.schemaId"), 1);
        assertEquals(data.get("datasetAddress.datasetId"), 2);
        assertNull(data.get("datasetAddress.datasetRootId"));
        assertEquals(data.get("subscription.filterParameter"), filterParameter);
        assertEquals(data.size(), 5);
    }
}
