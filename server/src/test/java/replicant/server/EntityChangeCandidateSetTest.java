package replicant.server;

import static org.testng.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import org.testng.annotations.Test;

public class EntityChangeCandidateSetTest {
    @Test
    public void mergeElementsOverrideExisting() {
        final var id = 17;
        final var typeID = 42;

        final var candidate =
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(id, typeID, 0, "r1", "r2", "a1", "a2");
        final var candidate2 =
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(id, typeID, 2, "r3", null, "a3", null);
        final var candidate3 =
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(id, typeID, 1, null, "r4", null, "a4");

        final var set = new EntityChangeCandidateSet();
        set.merge(candidate);
        assertEquals(set.getEntityChangeCandidates().size(), 1);

        set.merge(candidate2);
        assertEquals(set.getEntityChangeCandidates().size(), 1);
        assertEquals(set.getEntityChangeCandidates().iterator().next(), candidate);
        assertEquals(candidate.getTimestamp(), 2, "Timestamp merge rule is to take the latest value");
        EntityChangeCandidateTestUtil.assertAttributeValue(candidate, EntityChangeCandidateTestUtil.ATTR_KEY1, "a3");
        EntityChangeCandidateTestUtil.assertAttributeValue(candidate, EntityChangeCandidateTestUtil.ATTR_KEY2, "a2");
        EntityChangeCandidateTestUtil.assertRoutingKeyValue(
                candidate, EntityChangeCandidateTestUtil.ROUTING_KEY1, "r3");
        EntityChangeCandidateTestUtil.assertRoutingKeyValue(
                candidate, EntityChangeCandidateTestUtil.ROUTING_KEY2, "r2");

        set.merge(candidate3);
        assertEquals(set.getEntityChangeCandidates().size(), 1);

        assertEquals(set.getEntityChangeCandidates().iterator().next(), candidate);
        assertEquals(candidate.getTimestamp(), 2, "Timestamp merge rule is to take the latest value");
        EntityChangeCandidateTestUtil.assertAttributeValue(candidate, EntityChangeCandidateTestUtil.ATTR_KEY1, "a3");
        EntityChangeCandidateTestUtil.assertAttributeValue(candidate, EntityChangeCandidateTestUtil.ATTR_KEY2, "a4");
        EntityChangeCandidateTestUtil.assertRoutingKeyValue(
                candidate, EntityChangeCandidateTestUtil.ROUTING_KEY1, "r3");
        EntityChangeCandidateTestUtil.assertRoutingKeyValue(
                candidate, EntityChangeCandidateTestUtil.ROUTING_KEY2, "r4");
    }

    @Test
    public void mergeReplacesIfCopySpecified() {
        final var id = 17;
        final var typeID = 42;

        final var candidate =
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(id, typeID, 0, "r1", "r2", "a1", "a2");

        final var set = new EntityChangeCandidateSet();
        set.merge(candidate, true);
        final var inserted = set.getEntityChangeCandidates().iterator().next();
        assertNotSame(inserted, candidate);

        assertEquals(inserted.getTimestamp(), 0);
        EntityChangeCandidateTestUtil.assertAttributeValue(candidate, EntityChangeCandidateTestUtil.ATTR_KEY1, "a1");
        EntityChangeCandidateTestUtil.assertAttributeValue(candidate, EntityChangeCandidateTestUtil.ATTR_KEY2, "a2");
        EntityChangeCandidateTestUtil.assertRoutingKeyValue(
                candidate, EntityChangeCandidateTestUtil.ROUTING_KEY1, "r1");
        EntityChangeCandidateTestUtil.assertRoutingKeyValue(
                candidate, EntityChangeCandidateTestUtil.ROUTING_KEY2, "r2");
    }

    @Test
    public void mergeMultiple() {
        final var id = 17;
        final var typeID = 42;

        final var candidate =
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(id, typeID, 0, "r1", "r2", "a1", "a2");
        final var candidate2 =
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(id, typeID, 2, "r3", null, "a3", null);
        final var candidate3 =
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(id, typeID, 1, null, "r4", null, "a4");

        final var set = new EntityChangeCandidateSet();
        set.mergeAll(Arrays.asList(candidate, candidate2, candidate3));
        assertEquals(set.getEntityChangeCandidates().size(), 1);
        final var inserted = set.getEntityChangeCandidates().iterator().next();
        assertSame(inserted, candidate);

        assertEquals(inserted.getTimestamp(), 2, "Timestamp merge rule is to take the latest value");
        EntityChangeCandidateTestUtil.assertAttributeValue(inserted, EntityChangeCandidateTestUtil.ATTR_KEY1, "a3");
        EntityChangeCandidateTestUtil.assertAttributeValue(inserted, EntityChangeCandidateTestUtil.ATTR_KEY2, "a4");
        EntityChangeCandidateTestUtil.assertRoutingKeyValue(inserted, EntityChangeCandidateTestUtil.ROUTING_KEY1, "r3");
        EntityChangeCandidateTestUtil.assertRoutingKeyValue(inserted, EntityChangeCandidateTestUtil.ROUTING_KEY2, "r4");
    }

    @Test
    public void mergeMultipleWithCopy() {
        final var id = 17;
        final var typeID = 42;

        final var candidate =
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(id, typeID, 0, "r1", "r2", "a1", "a2");
        final var candidate2 =
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(id, typeID, 2, "r3", null, "a3", null);
        final var candidate3 =
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(id, typeID, 1, null, "r4", null, "a4");

        final var set = new EntityChangeCandidateSet();
        set.mergeAll(Arrays.asList(candidate, candidate2, candidate3), true);
        assertEquals(set.getEntityChangeCandidates().size(), 1);

        final var inserted = set.getEntityChangeCandidates().iterator().next();
        assertNotSame(inserted, candidate);

        assertEquals(inserted.getTimestamp(), 2, "Timestamp merge rule is to take the latest value");
        EntityChangeCandidateTestUtil.assertAttributeValue(inserted, EntityChangeCandidateTestUtil.ATTR_KEY1, "a3");
        EntityChangeCandidateTestUtil.assertAttributeValue(inserted, EntityChangeCandidateTestUtil.ATTR_KEY2, "a4");
        EntityChangeCandidateTestUtil.assertRoutingKeyValue(inserted, EntityChangeCandidateTestUtil.ROUTING_KEY1, "r3");
        EntityChangeCandidateTestUtil.assertRoutingKeyValue(inserted, EntityChangeCandidateTestUtil.ROUTING_KEY2, "r4");
    }

    @Test
    public void containsEntityChangeCandidate() {
        final var candidate = new EntityChangeCandidate(17, 42, 0, new HashMap<>(), new HashMap<>(), null);

        final var set = new EntityChangeCandidateSet();

        assertFalse(set.containsEntityChangeCandidate(candidate.getTypeId(), candidate.getId()));
        set.merge(candidate);
        assertTrue(set.containsEntityChangeCandidate(candidate.getTypeId(), candidate.getId()));
    }
}
