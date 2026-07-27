package replicant.spy;

import static org.testng.Assert.*;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ValueUtil;

public final class ExecStartedEventTest extends AbstractReplicantTest {
    @Test
    public void basicOperation() {
        final String command = ValueUtil.randomString();
        final int systemSchemaId = ValueUtil.randomInt();
        final String systemSchemaName = ValueUtil.randomString();
        final int requestId = ValueUtil.randomInt();
        final ExecStartedEvent event = new ExecStartedEvent(systemSchemaId, systemSchemaName, command, requestId);

        assertEquals(event.getSystemSchemaId(), systemSchemaId);
        assertEquals(event.getSystemSchemaName(), systemSchemaName);
        assertEquals(event.getCommand(), command);
        assertEquals(event.getRequestId(), requestId);

        final HashMap<String, Object> data = new HashMap<>();
        event.toMap(data);

        assertEquals(data.get("type"), "Connector.ExecStarted");
        assertEquals(data.get("systemSchema.id"), systemSchemaId);
        assertEquals(data.get("systemSchema.name"), systemSchemaName);
        assertEquals(data.get("command"), command);
        assertEquals(data.get("requestId"), requestId);

        assertEquals(data.size(), 5);
    }
}
