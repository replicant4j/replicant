package replicant.spy;

import static org.testng.Assert.*;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ValueUtil;

public final class CommandStartedEventTest extends AbstractReplicantTest {
    @Test
    public void basicOperation() {
        final String commandName = ValueUtil.randomString();
        final int systemSchemaId = ValueUtil.randomInt();
        final String systemSchemaName = ValueUtil.randomString();
        final int requestId = ValueUtil.randomInt();
        final CommandStartedEvent event =
                new CommandStartedEvent(systemSchemaId, systemSchemaName, commandName, requestId);

        assertEquals(event.getSystemSchemaId(), systemSchemaId);
        assertEquals(event.getSystemSchemaName(), systemSchemaName);
        assertEquals(event.getCommandName(), commandName);
        assertEquals(event.getRequestId(), requestId);

        final HashMap<String, Object> data = new HashMap<>();
        event.toMap(data);

        assertEquals(data.get("type"), "Connector.CommandStarted");
        assertEquals(data.get("systemSchema.id"), systemSchemaId);
        assertEquals(data.get("systemSchema.name"), systemSchemaName);
        assertEquals(data.get("command.name"), commandName);
        assertEquals(data.get("requestId"), requestId);

        assertEquals(data.size(), 5);
    }
}
