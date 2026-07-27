package replicant.spy;

import static org.testng.Assert.*;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;

public class InSyncEventTest extends AbstractReplicantTest {
    @Test
    public void basicOperation() {
        final InSyncEvent event = new InSyncEvent(23);

        assertEquals(event.getSystemSchemaId(), 23);

        final HashMap<String, Object> data = new HashMap<>();
        event.toMap(data);

        assertEquals(data.get("type"), "Connector.InSync");
        assertEquals(data.get("systemSchema.id"), 23);
        assertEquals(data.size(), 2);
    }
}
