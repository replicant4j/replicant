package replicant.spy;

import static org.testng.Assert.*;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;

public class MessageProcessingFailureEventTest extends AbstractReplicantTest {
    @Test
    public void basicOperation() {
        final MessageProcessingFailureEvent event =
                new MessageProcessingFailureEvent(23, "Rose", new Error("Some ERROR"));

        assertEquals(event.getSystemSchemaId(), 23);
        assertEquals(event.getSystemSchemaName(), "Rose");

        final HashMap<String, Object> data = new HashMap<>();
        event.toMap(data);

        assertEquals(data.get("type"), "Connector.MessageProcessingFailure");
        assertEquals(data.get("systemSchema.id"), 23);
        assertEquals(data.get("systemSchema.name"), "Rose");
        assertEquals(data.get("message"), "Some ERROR");
        assertEquals(data.size(), 4);
    }

    @Test
    public void basicOperation_ThrowableNoMessage() {
        final MessageProcessingFailureEvent event =
                new MessageProcessingFailureEvent(23, "Rose", new NullPointerException());

        assertEquals(event.getSystemSchemaId(), 23);
        assertEquals(event.getSystemSchemaName(), "Rose");

        final HashMap<String, Object> data = new HashMap<>();
        event.toMap(data);

        assertEquals(data.get("type"), "Connector.MessageProcessingFailure");
        assertEquals(data.get("systemSchema.id"), 23);
        assertEquals(data.get("systemSchema.name"), "Rose");
        assertEquals(data.get("message"), "java.lang.NullPointerException");
        assertEquals(data.size(), 4);
    }
}
