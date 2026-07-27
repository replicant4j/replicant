package replicant.server.transport;

import static org.testng.Assert.*;

import java.util.ArrayList;
import javax.json.Json;
import org.testng.annotations.Test;
import replicant.server.ChangeSet;
import replicant.server.DatasetAddress;
import replicant.server.EntityChangeCandidate;
import replicant.server.ValueUtil;

public class PacketTest {
    @Test
    public void packetFromInitiator() {
        final var requestId = ValueUtil.randomInt();
        final var response = Json.createArrayBuilder().build();
        final var datasetCacheVersion = ValueUtil.randomString();
        final var messages = new ArrayList<EntityChangeCandidate>();
        final var changeSet = new ChangeSet();

        final var packet = new Packet(true, requestId, response, datasetCacheVersion, messages, changeSet);

        assertTrue(packet.fromSubscriptionRequest());
        assertEquals(packet.requestId(), (Integer) requestId);
        assertEquals(packet.response(), response);
        assertEquals(packet.datasetCacheVersion(), datasetCacheVersion);
        assertSame(packet.messages(), messages);
        assertSame(packet.changeSet(), changeSet);
    }

    @Test
    public void packetNotFromInitiator() {
        final var messages = new ArrayList<EntityChangeCandidate>();
        final var changeSet = new ChangeSet();

        final var packet = new Packet(false, null, null, null, messages, changeSet);

        assertFalse(packet.fromSubscriptionRequest());
        assertNull(packet.requestId());
        assertNull(packet.datasetCacheVersion());
        assertSame(packet.messages(), messages);
        assertSame(packet.changeSet(), changeSet);
    }

    @Test
    public void cachedDatasetReference() {
        final var requestId = ValueUtil.randomInt();
        final var datasetAddress = DatasetAddress.of(1);
        final var datasetCacheVersion = ValueUtil.randomString();

        final var packet = Packet.cachedDatasetReference(requestId, datasetAddress, datasetCacheVersion);

        assertTrue(packet.fromSubscriptionRequest());
        assertEquals(packet.requestId(), (Integer) requestId);
        assertEquals(packet.datasetCacheVersion(), datasetCacheVersion);
        assertEquals(packet.cachedDatasetAddress(), datasetAddress);
        assertTrue(packet.messages().isEmpty());
        assertFalse(packet.changeSet().hasContent());
    }
}
