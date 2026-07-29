package replicant.spy;

import static org.testng.Assert.*;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ValueUtil;

public final class CommandQueuedEventTest extends AbstractReplicantTest {
    @Test
    public void basicOperation() {
        final String commandName = ValueUtil.randomString();
        final int systemSchemaId = ValueUtil.randomInt();
        final String systemSchemaName = ValueUtil.randomString();
        final CommandQueuedEvent event = new CommandQueuedEvent(systemSchemaId, systemSchemaName, commandName);

        assertEquals(event.getSystemSchemaId(), systemSchemaId);
        assertEquals(event.getSystemSchemaName(), systemSchemaName);
        assertEquals(event.getCommandName(), commandName);

        final HashMap<String, Object> data = new HashMap<>();
        event.toMap(data);

        assertEquals(data.get("type"), "Connector.CommandQueued");
        assertEquals(data.get("systemSchema.id"), systemSchemaId);
        assertEquals(data.get("systemSchema.name"), systemSchemaName);
        assertEquals(data.get("command.name"), commandName);

        assertEquals(data.size(), 4);
    }
}
