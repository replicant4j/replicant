package replicant.server.json;

import static org.testng.Assert.*;

import java.io.Serializable;
import java.io.StringReader;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Objects;
import javax.json.Json;
import javax.json.JsonObject;
import org.testng.annotations.Test;
import replicant.server.ChangeSet;
import replicant.server.DatasetAddress;
import replicant.server.EntityChange;
import replicant.server.EntityChangeCandidate;
import replicant.server.MessageTestUtil;
import replicant.server.SubscriptionChange;
import replicant.server.ValueUtil;
import replicant.shared.Messages;

/**
 * Utility class used when encoding EntityChangeCandidate into JSON payload.
 */
public final class JsonEncoderTest {
    @Test
    public void encodeAllData() {
        final var id = 17;
        final var typeID = 42;
        final var calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2001);
        calendar.set(Calendar.MONTH, Calendar.JULY);
        calendar.set(Calendar.DAY_OF_MONTH, 5);
        calendar.set(Calendar.AM_PM, Calendar.AM);
        calendar.set(Calendar.HOUR_OF_DAY, 5);
        calendar.set(Calendar.MINUTE, 8);
        calendar.set(Calendar.SECOND, 56);
        calendar.set(Calendar.MILLISECOND, 0);

        final var date = calendar.getTime();

        final var message = MessageTestUtil.createMessage(id, typeID, 0, "r1", "r2", "a1", "a2");
        final var values = message.getAttributeValues();
        assertNotNull(values);
        Objects.requireNonNull(values).put("key3", date);

        final var requestId = 1;
        final var response = Json.createArrayBuilder().add(17).add(42).build();

        final var datasetCacheVersion = "#1";
        final var filterParameter = Json.createBuilderFactory(null)
                .createObjectBuilder()
                .add("a", "b")
                .build();

        final var change = new EntityChange(message);
        change.getDatasetAddresses().add(DatasetAddress.of(1, null));
        change.getDatasetAddresses().add(DatasetAddress.of(2, 42));
        change.getDatasetAddresses().add(DatasetAddress.of(3, 73));
        final var cs = new ChangeSet();
        cs.merge(change);
        cs.mergeSubscriptionChange(
                SubscriptionChange.of(DatasetAddress.of(45, 77), SubscriptionChange.Type.UPDATE, filterParameter));
        final var encoded = JsonEncoder.encodeChangeSet(requestId, response, datasetCacheVersion, cs);
        final var changeSet = toJsonObject(encoded);

        assertNotNull(changeSet);

        assertEquals(changeSet.getInt(Messages.Common.REQUEST_ID), requestId);
        final var jsonResponse = changeSet.getJsonArray(Messages.Update.RESPONSE);
        assertEquals(jsonResponse.size(), 2);
        assertEquals(jsonResponse.getInt(0), 17);
        assertEquals(jsonResponse.getInt(1), 42);
        assertEquals(changeSet.getString(Messages.S2C_Common.DATASET_CACHE_VERSION), datasetCacheVersion);

        final var action = changeSet
                .getJsonArray(Messages.Update.FILTER_PARAMETER_SUBSCRIPTION_CHANGES)
                .getJsonObject(0);
        assertEquals(action.getString(Messages.Update.SUBSCRIPTION_CHANGE), "=45.77");
        assertEquals(action.getJsonObject(Messages.Update.FILTER_PARAMETER).toString(), filterParameter.toString());

        final var object = changeSet.getJsonArray(Messages.Update.CHANGES).getJsonObject(0);

        assertEquals(object.getString(Messages.Update.ENTITY_ID), "42.17");

        final var data = object.getJsonObject(Messages.Update.DATA);
        assertNotNull(data);
        assertEquals(data.getString(MessageTestUtil.ATTR_KEY1), "a1");
        assertEquals(data.getString(MessageTestUtil.ATTR_KEY2), "a2");
        assertTrue(data.getString("key3").startsWith("2001-07-05T05:08:56.000"));

        final var datasetAddresses = object.getJsonArray(Messages.Update.DATASET_ADDRESSES);
        assertNotNull(datasetAddresses);
        assertEquals(datasetAddresses.size(), 3);
        final var datasetAddress1 = datasetAddresses.getString(0);
        assertNotNull(datasetAddress1);
        final var datasetAddress2 = datasetAddresses.getString(1);
        assertNotNull(datasetAddress2);
        final var datasetAddress3 = datasetAddresses.getString(2);
        assertNotNull(datasetAddress3);

        assertEquals(datasetAddress1, "1");
        assertEquals(datasetAddress2, "2.42");
        assertEquals(datasetAddress3, "3.73");
    }

    @Test
    public void encodeChangeSetFromEntityChangeCandidates_deleteMessage() {
        final var id = 17;
        final var typeID = 42;

        final var message = MessageTestUtil.createMessage(id, typeID, 0, "r1", "r2", null, null);

        final var cs = new ChangeSet();
        cs.merge(new EntityChange(message));
        final var encoded = JsonEncoder.encodeChangeSet(null, null, null, cs);
        final var changeSet = toJsonObject(encoded);

        assertNotNull(changeSet);

        final var object = changeSet.getJsonArray(Messages.Update.CHANGES).getJsonObject(0);

        assertEquals(object.getString(Messages.Update.ENTITY_ID), "42.17");

        assertFalse(object.containsKey(Messages.Update.DATA));
    }

    @Test
    public void encodeChangeSet_empty() {
        final var cs = new ChangeSet();
        final var changeSet = toJsonObject(JsonEncoder.encodeChangeSet(null, null, null, cs));

        assertNotNull(changeSet);
        assertEquals(changeSet.getString(Messages.Common.TYPE), Messages.S2C_Type.UPDATE);
        assertFalse(changeSet.containsKey(Messages.Common.REQUEST_ID));
        assertFalse(changeSet.containsKey(Messages.Update.RESPONSE));
        assertFalse(changeSet.containsKey(Messages.S2C_Common.DATASET_CACHE_VERSION));
        assertFalse(changeSet.containsKey(Messages.Update.SUBSCRIPTION_CHANGES));
        assertFalse(changeSet.containsKey(Messages.Update.FILTER_PARAMETER_SUBSCRIPTION_CHANGES));
        assertFalse(changeSet.containsKey(Messages.Update.CHANGES));
    }

    private JsonObject toJsonObject(final String encoded) {
        return Json.createReader(new StringReader(encoded)).readObject();
    }

    @Test
    public void action_WithNoFilter() {
        final var cs = new ChangeSet();
        cs.mergeSubscriptionChange(
                SubscriptionChange.of(DatasetAddress.of(45, null), SubscriptionChange.Type.SUBSCRIBE));
        final var changeSet = toJsonObject(JsonEncoder.encodeChangeSet(null, null, null, cs));
        assertNotNull(changeSet);

        assertEquals(
                changeSet.getJsonArray(Messages.Update.SUBSCRIPTION_CHANGES).getString(0), "+45");
    }

    @Test
    public void subscriptionChange_DELETE() {
        final var cs = new ChangeSet();
        cs.mergeSubscriptionChange(
                SubscriptionChange.of(DatasetAddress.of(45, null), SubscriptionChange.Type.INVALIDATE_DATASET_ADDRESS));
        final var changeSet = toJsonObject(JsonEncoder.encodeChangeSet(null, null, null, cs));
        assertNotNull(changeSet);

        assertEquals(
                changeSet.getJsonArray(Messages.Update.SUBSCRIPTION_CHANGES).getString(0), "!45");
    }

    @Test
    public void mixedSubscriptionChanges() {
        final var cs = new ChangeSet();
        cs.mergeSubscriptionChange(
                SubscriptionChange.of(DatasetAddress.of(1, null), SubscriptionChange.Type.SUBSCRIBE));
        cs.mergeSubscriptionChange(SubscriptionChange.of(DatasetAddress.of(2, 5), SubscriptionChange.Type.UNSUBSCRIBE));
        cs.mergeSubscriptionChange(
                SubscriptionChange.of(DatasetAddress.of(3, 7, "inst"), SubscriptionChange.Type.UPDATE));

        final var filterParameter = Json.createObjectBuilder().add("a", "b").build();
        cs.mergeSubscriptionChange(
                SubscriptionChange.of(DatasetAddress.of(4, 9), SubscriptionChange.Type.SUBSCRIBE, filterParameter));

        final var changeSet = toJsonObject(JsonEncoder.encodeChangeSet(null, null, null, cs));
        assertNotNull(changeSet);

        final var actions = changeSet.getJsonArray(Messages.Update.SUBSCRIPTION_CHANGES);
        assertEquals(actions.size(), 3);
        assertEquals(actions.getString(0), "+1");
        assertEquals(actions.getString(1), "-2.5");
        assertEquals(actions.getString(2), "=3.7#inst");

        final var filteredAction = changeSet
                .getJsonArray(Messages.Update.FILTER_PARAMETER_SUBSCRIPTION_CHANGES)
                .getJsonObject(0);
        assertEquals(filteredAction.getString(Messages.Update.SUBSCRIPTION_CHANGE), "+4.9");
        assertEquals(
                filteredAction.getJsonObject(Messages.Update.FILTER_PARAMETER).toString(), filterParameter.toString());
    }

    @Test
    public void encodeChangeSet_dataTypes() {
        final var calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2001);
        calendar.set(Calendar.MONTH, Calendar.JULY);
        calendar.set(Calendar.DAY_OF_MONTH, 5);
        calendar.set(Calendar.AM_PM, Calendar.AM);
        calendar.set(Calendar.HOUR_OF_DAY, 5);
        calendar.set(Calendar.MINUTE, 8);
        calendar.set(Calendar.SECOND, 56);
        calendar.set(Calendar.MILLISECOND, 0);
        final var date = calendar.getTime();

        final var routingKeys = new HashMap<String, Serializable>();
        final var attributeData = new HashMap<String, Serializable>();
        attributeData.put("s", "text");
        attributeData.put("i", 12);
        attributeData.put("f", 1.5f);
        attributeData.put("b", true);
        attributeData.put("d", date);
        attributeData.put("n", null);

        final var message = new EntityChangeCandidate(1, 2, 0, routingKeys, attributeData, null);
        final var cs = new ChangeSet();
        cs.merge(new EntityChange(message));

        final var changeSet = toJsonObject(JsonEncoder.encodeChangeSet(null, null, null, cs));
        final var change = changeSet.getJsonArray(Messages.Update.CHANGES).getJsonObject(0);
        final var data = change.getJsonObject(Messages.Update.DATA);

        assertEquals(data.getString("s"), "text");
        assertEquals(data.getInt("i"), 12);
        assertEquals(data.getJsonNumber("f").doubleValue(), 1.5, 0.0001);
        assertTrue(data.getBoolean("b"));
        assertTrue(data.getString("d").startsWith("2001-07-05T05:08:56.000"));
        assertFalse(data.containsKey("n"));
        assertFalse(change.containsKey(Messages.Update.DATASET_ADDRESSES));
    }

    @Test
    public void encodeChangeSet_datasetAddressDescriptors_includeDatasetKey() {
        final var routingKeys = new HashMap<String, Serializable>();
        final var attributeData = new HashMap<String, Serializable>();
        attributeData.put("x", "y");
        final var message = new EntityChangeCandidate(1, 2, 0, routingKeys, attributeData, null);
        final var change = new EntityChange(message);
        change.getDatasetAddresses().add(DatasetAddress.of(7, null, "fi"));
        change.getDatasetAddresses().add(DatasetAddress.of(8, 3, "fi-2"));
        final var cs = new ChangeSet();
        cs.merge(change);

        final var changeSet = toJsonObject(JsonEncoder.encodeChangeSet(null, null, null, cs));
        final var datasetAddresses = changeSet
                .getJsonArray(Messages.Update.CHANGES)
                .getJsonObject(0)
                .getJsonArray(Messages.Update.DATASET_ADDRESSES);

        assertEquals(datasetAddresses.size(), 2);
        assertEquals(datasetAddresses.getString(0), "7#fi");
        assertEquals(datasetAddresses.getString(1), "8.3#fi-2");
    }

    @Test
    public void encodeChangeSet_rejectsUnsupportedValues() {
        final var routingKeys = new HashMap<String, Serializable>();
        final var attributeData = new HashMap<String, Serializable>();
        attributeData.put("bad", (byte) 1);
        final var message = new EntityChangeCandidate(1, 2, 0, routingKeys, attributeData, null);
        final var cs = new ChangeSet();
        cs.merge(new EntityChange(message));

        final var exception =
                expectThrows(IllegalStateException.class, () -> JsonEncoder.encodeChangeSet(null, null, null, cs));
        assertTrue(Objects.requireNonNull(exception.getMessage()).startsWith("Unable to encode:"));
    }

    @Test
    public void encodeLong() {
        final var id = ValueUtil.randomInt();
        final var typeID = 42;
        final var routingKeys = new HashMap<String, Serializable>();
        final var attributeData = new HashMap<String, Serializable>();
        attributeData.put("X", 1392061102056L);
        final var message = new EntityChangeCandidate(id, typeID, 0, routingKeys, attributeData, null);
        final var cs = new ChangeSet();
        cs.merge(new EntityChange(message));

        final var encoded = JsonEncoder.encodeChangeSet(null, null, null, cs);

        final var value = toJsonObject(encoded)
                .getJsonArray(Messages.Update.CHANGES)
                .getJsonObject(0)
                .getJsonObject(Messages.Update.DATA)
                .getString("X");
        assertNotNull(value);
        assertEquals(value, "1392061102056");
    }

    @Test
    public void encodeUseCachedDatasetMessage() {
        final var datasetAddress = DatasetAddress.of(1, 2, "inst");
        final var message = toJsonObject(JsonEncoder.encodeUseCachedDatasetMessage(datasetAddress, "e1", 7));

        assertEquals(message.getString(Messages.Common.TYPE), Messages.S2C_Type.USE_CACHED_DATASET);
        assertEquals(message.getString(Messages.Common.DATASET_ADDRESS), "1.2#inst");
        assertEquals(message.getString(Messages.S2C_Common.DATASET_CACHE_VERSION), "e1");
        assertEquals(message.getInt(Messages.Common.REQUEST_ID), 7);
    }

    @Test
    public void encodeUseCachedDatasetMessage_withoutRequestId() {
        final var datasetAddress = DatasetAddress.of(1, 2);
        final var message = toJsonObject(JsonEncoder.encodeUseCachedDatasetMessage(datasetAddress, "e1", null));

        assertEquals(message.getString(Messages.Common.TYPE), Messages.S2C_Type.USE_CACHED_DATASET);
        assertEquals(message.getString(Messages.Common.DATASET_ADDRESS), "1.2");
        assertEquals(message.getString(Messages.S2C_Common.DATASET_CACHE_VERSION), "e1");
        assertFalse(message.containsKey(Messages.Common.REQUEST_ID));
    }

    @Test
    public void encodeSessionCreatedMessage() {
        final var message = toJsonObject(JsonEncoder.encodeSessionCreatedMessage("sid-1"));

        assertEquals(message.getString(Messages.Common.TYPE), Messages.S2C_Type.SESSION_CREATED);
        assertEquals(message.getString(Messages.S2C_Common.SESSION_ID), "sid-1");
    }

    @Test
    public void encodeOkMessage() {
        final var message = toJsonObject(JsonEncoder.encodeOkMessage(4));

        assertEquals(message.getString(Messages.Common.TYPE), Messages.S2C_Type.OK);
        assertEquals(message.getInt(Messages.Common.REQUEST_ID), 4);
    }

    @Test
    public void encodeMalformedMessageMessage() {
        final var message = toJsonObject(JsonEncoder.encodeMalformedMessageMessage("bad"));

        assertEquals(message.getString(Messages.Common.TYPE), Messages.S2C_Type.MALFORMED_MESSAGE);
        assertEquals(message.getString(Messages.S2C_Common.MESSAGE), "bad");
    }

    @Test
    public void encodeUnknownRequestType() {
        final var command = Json.createObjectBuilder().add("t", "x").build();
        final var message = toJsonObject(JsonEncoder.encodeUnknownRequestType(command));

        assertEquals(message.getString(Messages.Common.TYPE), Messages.S2C_Type.UNKNOWN_REQUEST_TYPE);
        assertEquals(message.getJsonObject(Messages.Common.COMMAND).toString(), command.toString());
    }

    @Test
    public void encodeErrorMessage() {
        final var message = toJsonObject(JsonEncoder.encodeErrorMessage("oops"));

        assertEquals(message.getString(Messages.Common.TYPE), Messages.S2C_Type.ERROR);
        assertEquals(message.getString(Messages.S2C_Common.MESSAGE), "oops");
    }
}
