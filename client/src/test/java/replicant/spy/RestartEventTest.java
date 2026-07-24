package replicant.spy;

import static org.testng.Assert.*;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;

public class RestartEventTest extends AbstractReplicantTest {
    @Test
    public void basicOperation() {
        final RestartEvent event = new RestartEvent(23, "Rose");

        assertEquals(event.getSchemaId(), 23);
        assertEquals(event.getSchemaName(), "Rose");

        final HashMap<String, Object> data = new HashMap<>();
        event.toMap(data);

        assertEquals(data.get("type"), "Connector.Restart");
        assertEquals(data.get("schema.id"), 23);
        assertEquals(data.get("schema.name"), "Rose");
        assertEquals(data.size(), 3);
    }
}
