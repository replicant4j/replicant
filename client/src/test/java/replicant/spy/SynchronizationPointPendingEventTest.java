package replicant.spy;

import static org.testng.Assert.*;

import java.util.HashMap;
import org.jspecify.annotations.Nullable;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;

public class SynchronizationPointPendingEventTest extends AbstractReplicantTest {
    @Test
    public void basicOperation() {
        final SynchronizationPointPendingEvent event = new SynchronizationPointPendingEvent(23);

        assertEquals(event.getSystemSchemaId(), 23);

        final HashMap<String, @Nullable Object> data = new HashMap<>();
        event.toMap(data);

        assertEquals(data.get("type"), "Connector.SynchronizationPointPending");
        assertEquals(data.get("systemSchema.id"), 23);
        assertEquals(data.size(), 2);
    }
}
