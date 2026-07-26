package replicant.server;

import static org.testng.Assert.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import org.testng.annotations.Test;

public final class EntityMessageTest {
    @Test
    public void constructor_withoutSubscriptionDependencies_setsSubscriptionDependenciesToNull() {
        final var routingKeys = new HashMap<String, Serializable>();
        routingKeys.put("R", "v");
        final var attributes = new HashMap<String, Serializable>();
        attributes.put("A", "x");

        final var message = new EntityMessage(11, 22, 33L, routingKeys, attributes);

        assertEquals(message.getId(), 11);
        assertEquals(message.getTypeId(), 22);
        assertEquals(message.getTimestamp(), 33L);
        assertEquals(message.getRoutingKeys(), routingKeys);
        assertEquals(message.getAttributeValues(), attributes);
        assertNull(message.getSubscriptionDependencies());
        assertTrue(message.isUpdate());
    }

    @Test
    public void constructor_rejectsDeleteWithSubscriptionDependencies() {
        final var routingKeys = new HashMap<String, Serializable>();
        final var subscriptionDependency = new SubscriptionDependency(DatasetAddress.of(1, 2), DatasetAddress.of(3, 4));

        expectThrows(
                AssertionError.class,
                () -> new EntityMessage(11, 22, 33L, routingKeys, null, Set.of(subscriptionDependency)));
    }

    @Test
    public void mergeElementsOverrideExisting() {
        final var id = 17;
        final var typeID = 42;

        final var message = MessageTestUtil.createMessage(id, typeID, 0, "r1", "r2", "a1", "a2");

        assertEquals(message.getId(), id);
        assertEquals(message.getTypeId(), typeID);
        assertEquals(message.getTimestamp(), 0);
        assertNull(message.getSubscriptionDependencies());
        MessageTestUtil.assertAttributeValue(message, MessageTestUtil.ATTR_KEY1, "a1");
        MessageTestUtil.assertAttributeValue(message, MessageTestUtil.ATTR_KEY2, "a2");
        MessageTestUtil.assertRouteValue(message, MessageTestUtil.ROUTING_KEY1, "r1");
        MessageTestUtil.assertRouteValue(message, MessageTestUtil.ROUTING_KEY2, "r2");

        final var message2 = MessageTestUtil.createMessage(
                id,
                typeID,
                2,
                new SubscriptionDependency(DatasetAddress.of(1, 2), DatasetAddress.of(47, 66)),
                "r3",
                null,
                "a3",
                null);

        message.merge(message2);

        assertEquals(message.getId(), id);
        assertEquals(message.getTypeId(), typeID);
        assertEquals(message.getTimestamp(), 2);
        assertNotNull(message.getSubscriptionDependencies());
        final var subscriptionDependencies = Objects.requireNonNull(message.getSubscriptionDependencies());
        assertEquals(subscriptionDependencies.size(), 1);
        final var subscriptionDependency = subscriptionDependencies.iterator().next();
        assertEquals(subscriptionDependency.sourceDatasetAddress().datasetId(), 1);
        assertEquals(subscriptionDependency.sourceDatasetAddress().datasetRootId(), (Integer) 2);
        assertEquals(subscriptionDependency.targetDatasetAddress().datasetId(), 47);
        MessageTestUtil.assertAttributeValue(message, MessageTestUtil.ATTR_KEY1, "a3");
        MessageTestUtil.assertAttributeValue(message, MessageTestUtil.ATTR_KEY2, "a2");
        MessageTestUtil.assertRouteValue(message, MessageTestUtil.ROUTING_KEY1, "r3");
        MessageTestUtil.assertRouteValue(message, MessageTestUtil.ROUTING_KEY2, "r2");

        final var message3 = MessageTestUtil.createMessage(id, typeID, 1, null, null, null, "a4");

        message.merge(message3);
        assertEquals(message.getId(), id);
        assertEquals(message.getTypeId(), typeID);
        assertEquals(message.getTimestamp(), 2, "Timestamp merge rule is to take the latest value");
        MessageTestUtil.assertAttributeValue(message, MessageTestUtil.ATTR_KEY1, "a3");
        MessageTestUtil.assertAttributeValue(message, MessageTestUtil.ATTR_KEY2, "a4");
        MessageTestUtil.assertRouteValue(message, MessageTestUtil.ROUTING_KEY1, "r3");
        MessageTestUtil.assertRouteValue(message, MessageTestUtil.ROUTING_KEY2, "r2");
    }

    @Test
    public void mergeDeletedEnsuresDeleted() {
        final var id = 17;
        final var typeID = 42;

        final var message = MessageTestUtil.createMessage(
                id,
                typeID,
                0,
                new SubscriptionDependency(DatasetAddress.of(1, 2), DatasetAddress.of(47, 66)),
                "r1",
                "r2",
                "a1",
                "a2");

        assertTrue(message.isUpdate());
        assertFalse(message.isDelete());
        assertNotNull(message.getSubscriptionDependencies());

        message.merge(MessageTestUtil.createMessage(id, typeID, 0, "r1", "r2", null, null));

        assertFalse(message.isUpdate());
        assertTrue(message.isDelete());
        assertNull(message.getSubscriptionDependencies());
    }

    @Test
    public void mergeUpdateRevivesDeleted() {
        final var id = 17;
        final var typeID = 42;

        final var message = MessageTestUtil.createMessage(id, typeID, 0, "r1", "r2", null, null);

        assertFalse(message.isUpdate());
        assertTrue(message.isDelete());

        message.merge(MessageTestUtil.createMessage(id, typeID, 0, "r1", "r2", "a1", "a2"));

        assertTrue(message.isUpdate());
        assertFalse(message.isDelete());
    }

    @Test
    public void toStringIncludesDataWhenDataPresent() {
        final var message = MessageTestUtil.createMessage(17, 42, 0, "r1", "r2", "a1", "a2");
        assertTrue(message.toString().matches(".*Data=.*"));
    }

    @Test
    public void toIsDeleteFlagIsCorrect() {
        final var deleteMessage = MessageTestUtil.createMessage(17, 42, 0, "r1", "r2", null, null);
        assertFalse(deleteMessage.toString().matches(".*Data=.*"));
        assertFalse(deleteMessage.isUpdate());
        assertTrue(deleteMessage.isDelete());
    }

    @Test
    public void toDelete() {
        final var id = ValueUtil.randomInt();
        final var typeId = ValueUtil.randomInt();
        final var timestamp = ValueUtil.randomInt();
        final var message = MessageTestUtil.createMessage(
                id,
                typeId,
                timestamp,
                new SubscriptionDependency(DatasetAddress.of(1, 2), DatasetAddress.of(47, 66)),
                "r1",
                "r2",
                "a1",
                "a2");

        assertEquals(message.getId(), id);
        assertEquals(message.getTypeId(), typeId);
        assertEquals(message.getTimestamp(), timestamp);
        assertNotNull(message.getSubscriptionDependencies());
        assertTrue(message.isUpdate());
        assertFalse(message.isDelete());
        MessageTestUtil.assertAttributeValue(message, MessageTestUtil.ATTR_KEY1, "a1");
        MessageTestUtil.assertAttributeValue(message, MessageTestUtil.ATTR_KEY2, "a2");
        MessageTestUtil.assertRouteValue(message, MessageTestUtil.ROUTING_KEY1, "r1");
        MessageTestUtil.assertRouteValue(message, MessageTestUtil.ROUTING_KEY2, "r2");

        final var message2 = message.toDelete();

        assertEquals(message2.getId(), id);
        assertEquals(message2.getTypeId(), typeId);
        assertEquals(message2.getTimestamp(), timestamp);
        assertNull(message2.getSubscriptionDependencies());
        assertNull(message2.getAttributeValues());
        assertFalse(message2.isUpdate());
        assertTrue(message2.isDelete());
        MessageTestUtil.assertRouteValue(message2, MessageTestUtil.ROUTING_KEY1, "r1");
        MessageTestUtil.assertRouteValue(message2, MessageTestUtil.ROUTING_KEY2, "r2");
    }
}
