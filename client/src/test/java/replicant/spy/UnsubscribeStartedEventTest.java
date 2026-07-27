package replicant.spy;

import static org.testng.Assert.*;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.DatasetAddress;

public class UnsubscribeStartedEventTest extends AbstractReplicantTest {
    @Test
    public void basicOperation() {
        final DatasetAddress datasetAddress = new DatasetAddress(1, 2);
        final UnsubscribeStartedEvent event = new UnsubscribeStartedEvent(23, "Rose", datasetAddress);

        assertEquals(event.getSystemSchemaId(), 23);
        assertEquals(event.getSystemSchemaName(), "Rose");
        assertEquals(event.getDatasetAddress(), datasetAddress);

        final HashMap<String, Object> data = new HashMap<>();
        event.toMap(data);

        assertEquals(data.get("type"), "Connector.UnsubscribeStarted");
        assertEquals(data.get("systemSchema.id"), 23);
        assertEquals(data.get("systemSchema.name"), "Rose");
        assertEquals(data.get("datasetAddress.systemSchemaId"), 1);
        assertEquals(data.get("datasetAddress.datasetId"), 2);
        assertEquals(data.get("datasetAddress.datasetRootId"), datasetAddress.datasetRootId());
        assertEquals(data.size(), 6);
    }
}
