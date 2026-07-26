package replicant.spy;

import static org.testng.Assert.*;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.DatasetAddress;

public class SubscribeStartedEventTest extends AbstractReplicantTest {
    @Test
    public void basicOperation() {
        final DatasetAddress datasetAddress = new DatasetAddress(1, 2);
        final SubscribeStartedEvent event = new SubscribeStartedEvent(23, "Rose", datasetAddress);

        assertEquals(event.getSchemaId(), 23);
        assertEquals(event.getSchemaName(), "Rose");
        assertEquals(event.getDatasetAddress(), datasetAddress);

        final HashMap<String, Object> data = new HashMap<>();
        event.toMap(data);

        assertEquals(data.get("type"), "Connector.SubscribeStarted");
        assertEquals(data.get("schema.id"), 23);
        assertEquals(data.get("schema.name"), "Rose");
        assertEquals(data.get("datasetAddress.schemaId"), 1);
        assertEquals(data.get("datasetAddress.datasetId"), 2);
        assertEquals(data.get("datasetAddress.datasetRootId"), datasetAddress.datasetRootId());
        assertEquals(data.size(), 6);
    }
}
