package replicant.spy;

import static org.testng.Assert.*;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.DatasetAddress;

public class UnsubscribeRequestQueuedEventTest extends AbstractReplicantTest {
    @Test
    public void basicOperation() {
        final DatasetAddress datasetAddress = new DatasetAddress(1, 2);

        final UnsubscribeRequestQueuedEvent event = new UnsubscribeRequestQueuedEvent(datasetAddress);

        assertEquals(event.getDatasetAddress(), datasetAddress);

        final HashMap<String, Object> data = new HashMap<>();
        event.toMap(data);

        assertEquals(data.get("type"), "Connector.UnsubscribeRequestQueued");
        assertEquals(data.get("datasetAddress.systemSchemaId"), 1);
        assertEquals(data.get("datasetAddress.datasetId"), 2);
        assertNull(data.get("datasetAddress.datasetRootId"));
        assertEquals(data.size(), 4);
    }
}
