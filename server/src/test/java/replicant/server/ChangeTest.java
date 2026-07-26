package replicant.server;

import static org.testng.Assert.*;

import java.util.Objects;
import org.testng.annotations.Test;

public class ChangeTest {
    @Test
    public void basicOperation() {
        final var id = 17;
        final var typeID = 42;

        final var message = MessageTestUtil.createMessage(id, typeID, 0, "r1", "r2", "a1", "a2");
        final var change = new Change(message);

        assertEquals(change.getKey(), "42#17");
        assertEquals(change.getEntityMessage(), message);
        assertEquals(change.getDatasetAddresses().size(), 0);
    }

    @Test
    public void duplicate() {
        final var id = 17;
        final var typeID = 42;

        final var message = MessageTestUtil.createMessage(id, typeID, 0, "r1", "r2", "a1", "a2");
        final var change = new Change(message);
        change.getDatasetAddresses().add(DatasetAddress.of(1, 1));
        change.getDatasetAddresses().add(DatasetAddress.of(2, 3));

        final var duplicate = change.duplicate();
        assertEquals(duplicate.getKey(), change.getKey());
        assertEquals(
                duplicate.getEntityMessage().getId(), change.getEntityMessage().getId());
        assertNotSame(duplicate.getEntityMessage(), change.getEntityMessage());
        assertEquals(duplicate.getDatasetAddresses(), change.getDatasetAddresses());
        //noinspection SimplifiableAssertion
        assertFalse(duplicate.getDatasetAddresses() == change.getDatasetAddresses());
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void merge_combinesDatasetAddresses() {
        final var id = 17;
        final var typeID = 42;

        final var message1 = MessageTestUtil.createMessage(id, typeID, 0, "r1", "r2", "a1", "a2");
        final var message2 = MessageTestUtil.createMessage(id, typeID, 0, "r1", "r3", "aZ", "a2");

        final var change1 = new Change(message1);
        final var change2 = new Change(message2);

        change1.getDatasetAddresses().add(DatasetAddress.of(1, 1));
        change2.getDatasetAddresses().add(DatasetAddress.of(2, 3));

        assertEquals(change1.getDatasetAddresses().size(), 1);
        assertFalse(change1.getDatasetAddresses().contains(DatasetAddress.of(2, 3)));
        assertEquals(
                Objects.requireNonNull(change1.getEntityMessage().getAttributeValues())
                        .get(MessageTestUtil.ATTR_KEY1),
                "a1");
        assertEquals(change1.getEntityMessage().getRoutingKeys().get(MessageTestUtil.ROUTING_KEY2), "r2");

        change1.merge(change2);

        assertEquals(change1.getDatasetAddresses().size(), 2);
        assertTrue(change1.getDatasetAddresses().contains(DatasetAddress.of(2, 3)));
        assertEquals(
                Objects.requireNonNull(change1.getEntityMessage().getAttributeValues())
                        .get(MessageTestUtil.ATTR_KEY1),
                "aZ");
        assertEquals(change1.getEntityMessage().getRoutingKeys().get(MessageTestUtil.ROUTING_KEY2), "r3");
    }

    @Test
    public void constructor_includesDatasetKey() {
        final var id = 3;
        final var typeID = 4;

        final var message = MessageTestUtil.createMessage(id, typeID, 0, "r1", "r2", "a1", "a2");
        final var datasetAddress = DatasetAddress.of(7, 12, "instance-a");
        final var change = new Change(message, datasetAddress);

        assertEquals(change.getDatasetAddresses().size(), 1);
        assertTrue(change.getDatasetAddresses().contains(datasetAddress));
    }

    @Test
    public void merge_preservesDistinctDatasetKeys() {
        final var id = 8;
        final var typeID = 9;

        final var message1 = MessageTestUtil.createMessage(id, typeID, 0, "r1", "r2", "a1", "a2");
        final var message2 = MessageTestUtil.createMessage(id, typeID, 0, "r1", "r3", "aZ", "a2");

        final var change1 = new Change(message1);
        final var change2 = new Change(message2);

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
