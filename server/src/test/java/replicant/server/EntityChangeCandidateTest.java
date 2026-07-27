package replicant.server;

import static org.testng.Assert.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import org.testng.annotations.Test;

public final class EntityChangeCandidateTest {
    @Test
    public void constructor_withoutSubscriptionDependencies_setsSubscriptionDependenciesToNull() {
        final var routingKeys = new HashMap<String, Serializable>();
        routingKeys.put("R", "v");
        final var attributes = new HashMap<String, Serializable>();
        attributes.put("A", "x");

        final var candidate = new EntityChangeCandidate(11, 22, 33L, routingKeys, attributes);

        assertEquals(candidate.getId(), 11);
        assertEquals(candidate.getTypeId(), 22);
        assertEquals(candidate.getTimestamp(), 33L);
        assertEquals(candidate.getRoutingKeys(), routingKeys);
        assertEquals(candidate.getAttributeValues(), attributes);
        assertNull(candidate.getSubscriptionDependencyCandidates());
        assertTrue(candidate.isUpdate());
    }

    @Test
    public void constructor_rejectsDeleteWithSubscriptionDependencies() {
        final var routingKeys = new HashMap<String, Serializable>();
        final var subscriptionDependency =
                new SubscriptionDependencyCandidate(DatasetAddress.of(1, 2), DatasetAddress.of(3, 4));

        expectThrows(
                AssertionError.class,
                () -> new EntityChangeCandidate(11, 22, 33L, routingKeys, null, Set.of(subscriptionDependency)));
    }

    @Test
    public void mergeElementsOverrideExisting() {
        final var id = 17;
        final var typeID = 42;

        final var candidate =
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(id, typeID, 0, "r1", "r2", "a1", "a2");

        assertEquals(candidate.getId(), id);
        assertEquals(candidate.getTypeId(), typeID);
        assertEquals(candidate.getTimestamp(), 0);
        assertNull(candidate.getSubscriptionDependencyCandidates());
        EntityChangeCandidateTestUtil.assertAttributeValue(candidate, EntityChangeCandidateTestUtil.ATTR_KEY1, "a1");
        EntityChangeCandidateTestUtil.assertAttributeValue(candidate, EntityChangeCandidateTestUtil.ATTR_KEY2, "a2");
        EntityChangeCandidateTestUtil.assertRoutingKeyValue(
                candidate, EntityChangeCandidateTestUtil.ROUTING_KEY1, "r1");
        EntityChangeCandidateTestUtil.assertRoutingKeyValue(
                candidate, EntityChangeCandidateTestUtil.ROUTING_KEY2, "r2");

        final var candidate2 = EntityChangeCandidateTestUtil.createEntityChangeCandidate(
                id,
                typeID,
                2,
                new SubscriptionDependencyCandidate(DatasetAddress.of(1, 2), DatasetAddress.of(47, 66)),
                "r3",
                null,
                "a3",
                null);

        candidate.merge(candidate2);

        assertEquals(candidate.getId(), id);
        assertEquals(candidate.getTypeId(), typeID);
        assertEquals(candidate.getTimestamp(), 2);
        assertNotNull(candidate.getSubscriptionDependencyCandidates());
        final var subscriptionDependencyCandidates =
                Objects.requireNonNull(candidate.getSubscriptionDependencyCandidates());
        assertEquals(subscriptionDependencyCandidates.size(), 1);
        final var subscriptionDependency =
                subscriptionDependencyCandidates.iterator().next();
        assertEquals(subscriptionDependency.sourceDatasetAddressCandidate().datasetId(), 1);
        assertEquals(subscriptionDependency.sourceDatasetAddressCandidate().datasetRootId(), (Integer) 2);
        assertEquals(subscriptionDependency.targetDatasetAddressCandidate().datasetId(), 47);
        EntityChangeCandidateTestUtil.assertAttributeValue(candidate, EntityChangeCandidateTestUtil.ATTR_KEY1, "a3");
        EntityChangeCandidateTestUtil.assertAttributeValue(candidate, EntityChangeCandidateTestUtil.ATTR_KEY2, "a2");
        EntityChangeCandidateTestUtil.assertRoutingKeyValue(
                candidate, EntityChangeCandidateTestUtil.ROUTING_KEY1, "r3");
        EntityChangeCandidateTestUtil.assertRoutingKeyValue(
                candidate, EntityChangeCandidateTestUtil.ROUTING_KEY2, "r2");

        final var candidate3 =
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(id, typeID, 1, null, null, null, "a4");

        candidate.merge(candidate3);
        assertEquals(candidate.getId(), id);
        assertEquals(candidate.getTypeId(), typeID);
        assertEquals(candidate.getTimestamp(), 2, "Timestamp merge rule is to take the latest value");
        EntityChangeCandidateTestUtil.assertAttributeValue(candidate, EntityChangeCandidateTestUtil.ATTR_KEY1, "a3");
        EntityChangeCandidateTestUtil.assertAttributeValue(candidate, EntityChangeCandidateTestUtil.ATTR_KEY2, "a4");
        EntityChangeCandidateTestUtil.assertRoutingKeyValue(
                candidate, EntityChangeCandidateTestUtil.ROUTING_KEY1, "r3");
        EntityChangeCandidateTestUtil.assertRoutingKeyValue(
                candidate, EntityChangeCandidateTestUtil.ROUTING_KEY2, "r2");
    }

    @Test
    public void mergeDeletedEnsuresDeleted() {
        final var id = 17;
        final var typeID = 42;

        final var candidate = EntityChangeCandidateTestUtil.createEntityChangeCandidate(
                id,
                typeID,
                0,
                new SubscriptionDependencyCandidate(DatasetAddress.of(1, 2), DatasetAddress.of(47, 66)),
                "r1",
                "r2",
                "a1",
                "a2");

        assertTrue(candidate.isUpdate());
        assertFalse(candidate.isDelete());
        assertNotNull(candidate.getSubscriptionDependencyCandidates());

        candidate.merge(
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(id, typeID, 0, "r1", "r2", null, null));

        assertFalse(candidate.isUpdate());
        assertTrue(candidate.isDelete());
        assertNull(candidate.getSubscriptionDependencyCandidates());
    }

    @Test
    public void mergeUpdateRevivesDeleted() {
        final var id = 17;
        final var typeID = 42;

        final var candidate =
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(id, typeID, 0, "r1", "r2", null, null);

        assertFalse(candidate.isUpdate());
        assertTrue(candidate.isDelete());

        candidate.merge(
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(id, typeID, 0, "r1", "r2", "a1", "a2"));

        assertTrue(candidate.isUpdate());
        assertFalse(candidate.isDelete());
    }

    @Test
    public void toStringIncludesDataWhenDataPresent() {
        final var candidate =
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(17, 42, 0, "r1", "r2", "a1", "a2");
        assertTrue(candidate.toString().matches(".*Data=.*"));
    }

    @Test
    public void toIsDeleteFlagIsCorrect() {
        final var deletionCandidate =
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(17, 42, 0, "r1", "r2", null, null);
        assertFalse(deletionCandidate.toString().matches(".*Data=.*"));
        assertFalse(deletionCandidate.isUpdate());
        assertTrue(deletionCandidate.isDelete());
    }

    @Test
    public void toReplicaRemoval() {
        final var id = ValueUtil.randomInt();
        final var typeId = ValueUtil.randomInt();
        final var timestamp = ValueUtil.randomInt();
        final var candidate = EntityChangeCandidateTestUtil.createEntityChangeCandidate(
                id,
                typeId,
                timestamp,
                new SubscriptionDependencyCandidate(DatasetAddress.of(1, 2), DatasetAddress.of(47, 66)),
                "r1",
                "r2",
                "a1",
                "a2");

        assertEquals(candidate.getId(), id);
        assertEquals(candidate.getTypeId(), typeId);
        assertEquals(candidate.getTimestamp(), timestamp);
        assertNotNull(candidate.getSubscriptionDependencyCandidates());
        assertTrue(candidate.isUpdate());
        assertFalse(candidate.isDelete());
        EntityChangeCandidateTestUtil.assertAttributeValue(candidate, EntityChangeCandidateTestUtil.ATTR_KEY1, "a1");
        EntityChangeCandidateTestUtil.assertAttributeValue(candidate, EntityChangeCandidateTestUtil.ATTR_KEY2, "a2");
        EntityChangeCandidateTestUtil.assertRoutingKeyValue(
                candidate, EntityChangeCandidateTestUtil.ROUTING_KEY1, "r1");
        EntityChangeCandidateTestUtil.assertRoutingKeyValue(
                candidate, EntityChangeCandidateTestUtil.ROUTING_KEY2, "r2");

        final var candidate2 = candidate.toReplicaRemoval();

        assertEquals(candidate2.getId(), id);
        assertEquals(candidate2.getTypeId(), typeId);
        assertEquals(candidate2.getTimestamp(), timestamp);
        assertNull(candidate2.getSubscriptionDependencyCandidates());
        assertNull(candidate2.getAttributeValues());
        assertFalse(candidate2.isUpdate());
        assertTrue(candidate2.isDelete());
        EntityChangeCandidateTestUtil.assertRoutingKeyValue(
                candidate2, EntityChangeCandidateTestUtil.ROUTING_KEY1, "r1");
        EntityChangeCandidateTestUtil.assertRoutingKeyValue(
                candidate2, EntityChangeCandidateTestUtil.ROUTING_KEY2, "r2");
    }
}
