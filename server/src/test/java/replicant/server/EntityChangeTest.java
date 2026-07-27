package replicant.server;

import static org.testng.Assert.*;

import java.util.Objects;
import org.testng.annotations.Test;

public class EntityChangeTest {
    @Test
    public void basicOperation() {
        final var id = 17;
        final var typeID = 42;

        final var candidate =
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(id, typeID, 0, "r1", "r2", "a1", "a2");
        final var change = new EntityChange(candidate);

        assertEquals(change.getKey(), "42#17");
        assertEquals(change.getEntityChangeCandidate(), candidate);
        assertEquals(change.getDatasetAddresses().size(), 0);
    }

    @Test
    public void duplicate() {
        final var id = 17;
        final var typeID = 42;

        final var candidate =
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(id, typeID, 0, "r1", "r2", "a1", "a2");
        final var change = new EntityChange(candidate);
        change.getDatasetAddresses().add(DatasetAddress.of(1, 1));
        change.getDatasetAddresses().add(DatasetAddress.of(2, 3));

        final var duplicate = change.duplicate();
        assertEquals(duplicate.getKey(), change.getKey());
        assertEquals(
                duplicate.getEntityChangeCandidate().getId(),
                change.getEntityChangeCandidate().getId());
        assertNotSame(duplicate.getEntityChangeCandidate(), change.getEntityChangeCandidate());
        assertEquals(duplicate.getDatasetAddresses(), change.getDatasetAddresses());
        //noinspection SimplifiableAssertion
        assertFalse(duplicate.getDatasetAddresses() == change.getDatasetAddresses());
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void merge_combinesDatasetAddresses() {
        final var id = 17;
        final var typeID = 42;

        final var candidate1 =
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(id, typeID, 0, "r1", "r2", "a1", "a2");
        final var candidate2 =
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(id, typeID, 0, "r1", "r3", "aZ", "a2");

        final var change1 = new EntityChange(candidate1);
        final var change2 = new EntityChange(candidate2);

        change1.getDatasetAddresses().add(DatasetAddress.of(1, 1));
        change2.getDatasetAddresses().add(DatasetAddress.of(2, 3));

        assertEquals(change1.getDatasetAddresses().size(), 1);
        assertFalse(change1.getDatasetAddresses().contains(DatasetAddress.of(2, 3)));
        assertEquals(
                Objects.requireNonNull(change1.getEntityChangeCandidate().getAttributeValues())
                        .get(EntityChangeCandidateTestUtil.ATTR_KEY1),
                "a1");
        assertEquals(
                change1.getEntityChangeCandidate().getRoutingKeys().get(EntityChangeCandidateTestUtil.ROUTING_KEY2),
                "r2");

        change1.merge(change2);

        assertEquals(change1.getDatasetAddresses().size(), 2);
        assertTrue(change1.getDatasetAddresses().contains(DatasetAddress.of(2, 3)));
        assertEquals(
                Objects.requireNonNull(change1.getEntityChangeCandidate().getAttributeValues())
                        .get(EntityChangeCandidateTestUtil.ATTR_KEY1),
                "aZ");
        assertEquals(
                change1.getEntityChangeCandidate().getRoutingKeys().get(EntityChangeCandidateTestUtil.ROUTING_KEY2),
                "r3");
    }

    @Test
    public void constructor_includesDatasetKey() {
        final var id = 3;
        final var typeID = 4;

        final var candidate =
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(id, typeID, 0, "r1", "r2", "a1", "a2");
        final var datasetAddress = DatasetAddress.of(7, 12, "instance-a");
        final var change = new EntityChange(candidate, datasetAddress);

        assertEquals(change.getDatasetAddresses().size(), 1);
        assertTrue(change.getDatasetAddresses().contains(datasetAddress));
    }

    @Test
    public void merge_preservesDistinctDatasetKeys() {
        final var id = 8;
        final var typeID = 9;

        final var candidate1 =
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(id, typeID, 0, "r1", "r2", "a1", "a2");
        final var candidate2 =
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(id, typeID, 0, "r1", "r3", "aZ", "a2");

        final var change1 = new EntityChange(candidate1);
        final var change2 = new EntityChange(candidate2);

        final var datasetAddressA = DatasetAddress.of(5, 11, "inst-1");
        final var datasetAddressADuplicate = DatasetAddress.of(5, 11, "inst-1");
        final var datasetAddressB = DatasetAddress.of(5, 11, "inst-2");

        change1.getDatasetAddresses().add(datasetAddressA);
        change2.getDatasetAddresses().add(datasetAddressADuplicate);
        change2.getDatasetAddresses().add(datasetAddressB);

        change1.merge(change2);

        assertEquals(change1.getDatasetAddresses().size(), 2);
        assertTrue(change1.getDatasetAddresses().contains(datasetAddressA));
        assertTrue(change1.getDatasetAddresses().contains(datasetAddressB));
    }
}
