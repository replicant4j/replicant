package replicant.spy;

import static org.testng.Assert.*;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;

public class MessageReadFailureEventTest extends AbstractReplicantTest {
    @Test
    public void basicOperation() {
        final MessageReadFailureEvent event = new MessageReadFailureEvent(23, "Rose");

        assertEquals(event.getSystemSchemaId(), 23);
        assertEquals(event.getSystemSchemaName(), "Rose");

        final HashMap<String, Object> data = new HashMap<>();
        event.toMap(data);

        assertEquals(data.get("type"), "Connector.MessageReadFailure");
        assertEquals(data.get("systemSchema.id"), 23);
        assertEquals(data.get("systemSchema.name"), "Rose");
        assertEquals(data.size(), 3);
    }
}
