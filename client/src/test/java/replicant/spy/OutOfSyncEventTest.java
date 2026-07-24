package replicant.spy;

import static org.testng.Assert.*;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;

public class OutOfSyncEventTest extends AbstractReplicantTest {
    @Test
    public void basicOperation() {
        final OutOfSyncEvent event = new OutOfSyncEvent(23);

        assertEquals(event.getSchemaId(), 23);

        final HashMap<String, Object> data = new HashMap<>();
        event.toMap(data);

        assertEquals(data.get("type"), "Connector.OutOfSync");
        assertEquals(data.get("schema.id"), 23);
        assertEquals(data.size(), 2);
    }
}
