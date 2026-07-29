package replicant.spy;

import static org.testng.Assert.*;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ValueUtil;

public class RequestCompletedEventTest extends AbstractReplicantTest {
    @Test
    public void basicOperation() {
        final int requestId = ValueUtil.randomInt();
        final String name = ValueUtil.randomString();
        final RequestCompletedEvent event = new RequestCompletedEvent(23, "Rose", requestId, name);

        assertEquals(event.getSystemSchemaId(), 23);
        assertEquals(event.getSystemSchemaName(), "Rose");
        assertEquals(event.getRequestId(), requestId);
        assertEquals(event.getName(), name);

        final HashMap<String, Object> data = new HashMap<>();
        event.toMap(data);

        assertEquals(data.get("type"), "Connector.RequestCompleted");
        assertEquals(data.get("systemSchema.id"), 23);
        assertEquals(data.get("systemSchema.name"), "Rose");
        assertEquals(data.get("requestId"), requestId);
        assertEquals(data.get("name"), name);

        assertEquals(data.size(), 5);
    }
}
