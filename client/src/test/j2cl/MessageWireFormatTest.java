package replicant;

import static org.junit.Assert.*;

import java.util.Objects;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import replicant.messages.AuthTokenMessage;
import replicant.messages.BulkSubscribeMessage;
import replicant.messages.BulkUnsubscribeMessage;
import replicant.messages.ChangeSetMessage;
import replicant.messages.CommandMessage;
import replicant.messages.DatasetCacheVersionsData;
import replicant.messages.DatasetCacheVersionsMessage;
import replicant.messages.EntityChange;
import replicant.messages.ErrorMessage;
import replicant.messages.PingMessage;
import replicant.messages.SessionCreatedMessage;
import replicant.messages.SubscribeMessage;
import replicant.messages.SubscriptionChangeMessage;
import replicant.messages.UnsubscribeMessage;
import replicant.messages.UseDatasetCacheEntryMessage;

@RunWith(JUnit4.class)
public final class MessageWireFormatTest {
    @Test
    public void serializesClientToServerMessagesWithStablePropertyNames() {
        final JsPropertyMap<Object> versions = JsPropertyMap.of();
        versions.set("fleet", "v7");
        final JsPropertyMap<Object> filter = JsPropertyMap.of();
        filter.set("active", true);
        final JsPropertyMap<Object> payload = JsPropertyMap.of();
        payload.set("priority", 3);

        assertJson("{\"type\":\"ping\",\"requestId\":1}", PingMessage.create(1));
        assertJson("{\"type\":\"auth\",\"requestId\":2,\"token\":\"secret\"}", AuthTokenMessage.create(2, "secret"));
        assertJson(
                "{\"type\":\"dataset-cache-versions\",\"requestId\":3,\"datasetCacheVersions\":{\"fleet\":\"v7\"}}",
                DatasetCacheVersionsMessage.create(3, Js.uncheckedCast(versions)));
        assertJson(
                "{\"type\":\"sub\",\"requestId\":4,\"datasetAddress\":\"fleet\",\"filterParameter\":{\"active\":true}}",
                SubscribeMessage.create(4, "fleet", filter));
        assertJson(
                "{\"type\":\"unsub\",\"requestId\":5,\"datasetAddress\":\"fleet\"}",
                UnsubscribeMessage.create(5, "fleet"));
        assertJson(
                "{\"type\":\"bulk-sub\",\"requestId\":6,\"datasetAddresses\":[\"fleet\",\"crew\"],\"filterParameter\":{\"active\":true}}",
                BulkSubscribeMessage.create(6, new String[] {"fleet", "crew"}, filter));
        assertJson(
                "{\"type\":\"bulk-unsub\",\"requestId\":7,\"datasetAddresses\":[\"fleet\",\"crew\"]}",
                BulkUnsubscribeMessage.create(7, new String[] {"fleet", "crew"}));
        assertJson(
                "{\"type\":\"command\",\"requestId\":8,\"name\":\"dispatch\",\"payload\":{\"priority\":3}}",
                CommandMessage.create(8, "dispatch", payload));
    }

    @Test
    public void parsesServerToClientMessagesWithStablePropertyNames() {
        final ChangeSetMessage changeSet = parse(
                "{\"type\":\"change-set\",\"requestId\":41,\"datasetCacheVersion\":\"cache-v1\","
                        + "\"subscriptionChanges\":[\"+fleet\"],\"filterParameterSubscriptionChanges\":[{"
                        + "\"subscriptionChange\":\"=fleet\",\"filterParameter\":{\"active\":true}}],"
                        + "\"entityChanges\":[{\"entityTypeId\":7,\"entityId\":8,"
                        + "\"datasetAddresses\":[\"fleet\"],\"payload\":{\"callsign\":\"R1\"}}],"
                        + "\"commandResult\":{\"accepted\":true}}",
                ChangeSetMessage.class);

        assertEquals("change-set", changeSet.getType());
        assertEquals(Integer.valueOf(41), changeSet.getRequestId());
        assertEquals("cache-v1", changeSet.getDatasetCacheVersion());
        assertArrayEquals(new String[] {"+fleet"}, changeSet.getSubscriptionChanges());
        final SubscriptionChangeMessage subscriptionChange = changeSet.getFilterParameterSubscriptionChanges()[0];
        assertEquals("=fleet", subscriptionChange.getSubscriptionChange());
        assertTrue(Js.asPropertyMap(subscriptionChange.getFilterParameter())
                .getAsAny("active")
                .asBoolean());
        final EntityChange entityChange = changeSet.getEntityChanges()[0];
        assertEquals(7, entityChange.getEntityTypeId());
        assertEquals(8, entityChange.getEntityId());
        assertArrayEquals(new String[] {"fleet"}, entityChange.getDatasetAddresses());
        assertTrue(entityChange.getPayload().containsKey("callsign"));
        assertEquals("R1", entityChange.getPayload().getStringValue("callsign"));
        assertTrue(Js.asPropertyMap(changeSet.getCommandResult())
                .getAsAny("accepted")
                .asBoolean());

        final ErrorMessage error = parse("{\"type\":\"error\",\"message\":\"denied\"}", ErrorMessage.class);
        assertEquals("error", error.getType());
        assertEquals("denied", error.getMessage());

        final SessionCreatedMessage session = parse(
                "{\"type\":\"session-created\",\"replicantSessionId\":\"session-1\"}", SessionCreatedMessage.class);
        assertEquals("session-1", session.getReplicantSessionId());

        final UseDatasetCacheEntryMessage cacheEntry = parse(
                "{\"type\":\"use-dataset-cache-entry\",\"requestId\":42,"
                        + "\"datasetAddress\":\"fleet\",\"datasetCacheVersion\":\"cache-v2\"}",
                UseDatasetCacheEntryMessage.class);
        assertEquals(Integer.valueOf(42), cacheEntry.getRequestId());
        assertEquals("fleet", cacheEntry.getDatasetAddress());
        assertEquals("cache-v2", cacheEntry.getDatasetCacheVersion());
    }

    @Test
    public void preservesDynamicDatasetCacheVersionKeys() {
        final DatasetCacheVersionsData versions = parse("{\"fleet/active\":\"v9\"}", DatasetCacheVersionsData.class);

        assertTrue(versions.containsDatasetAddress("fleet/active"));
        assertEquals("v9", versions.getDatasetCacheVersion("fleet/active"));
        assertArrayEquals(new String[] {"fleet/active"}, versions.datasetAddresses());
    }

    private static void assertJson(final String expected, final Object message) {
        assertEquals(expected, BrowserJson.stringify(Js.asAny(message)));
    }

    private static <T> T parse(final String json, final Class<T> type) {
        return Js.uncheckedCast(Objects.requireNonNull(BrowserJson.parse(json)));
    }
}
