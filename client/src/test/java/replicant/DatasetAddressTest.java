package replicant;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public final class DatasetAddressTest extends AbstractReplicantTest {
    @Test
    void construct() {
        final DatasetAddress datasetAddress = new DatasetAddress(2, 4, 1, "a");

        assertEquals(datasetAddress.schemaId(), 2);
        assertEquals(datasetAddress.datasetId(), 4);
        assertEquals(datasetAddress.datasetRootId(), (Integer) 1);
        assertEquals(datasetAddress.datasetKey(), "a");
    }

    @Test
    void constructTypeDatasetAddress() {
        final DatasetAddress datasetAddress = new DatasetAddress(2, 4);

        assertEquals(datasetAddress.schemaId(), 2);
        assertEquals(datasetAddress.datasetId(), 4);
        assertNull(datasetAddress.datasetRootId());
        assertNull(datasetAddress.datasetKey());
    }

    @Test
    void parseWithDatasetRootId() {
        final DatasetAddress datasetAddress = DatasetAddress.parse(2, "4.1");

        assertEquals(datasetAddress.schemaId(), 2);
        assertEquals(datasetAddress.datasetId(), 4);
        assertEquals(datasetAddress.datasetRootId(), (Integer) 1);
    }

    @Test
    void parse() {
        final DatasetAddress datasetAddress = DatasetAddress.parse(4, "77");

        assertEquals(datasetAddress.schemaId(), 4);
        assertEquals(datasetAddress.datasetId(), 77);
        assertNull(datasetAddress.datasetRootId());
    }

    @Test
    void parseWithDatasetKey() {
        final DatasetAddress datasetAddress = DatasetAddress.parse(4, "77#alpha");

        assertEquals(datasetAddress.schemaId(), 4);
        assertEquals(datasetAddress.datasetId(), 77);
        assertNull(datasetAddress.datasetRootId());
        assertEquals(datasetAddress.datasetKey(), "alpha");
    }

    @Test
    void parseWithRootAndDatasetKey() {
        final DatasetAddress datasetAddress = DatasetAddress.parse(4, "77.5#alpha");

        assertEquals(datasetAddress.schemaId(), 4);
        assertEquals(datasetAddress.datasetId(), 77);
        assertEquals(datasetAddress.datasetRootId(), (Integer) 5);
        assertEquals(datasetAddress.datasetKey(), "alpha");
    }

    @Test
    void getCacheKey() {
        final DatasetAddress datasetAddress = new DatasetAddress(2, 4, 1, "a");
        assertEquals(datasetAddress.getCacheKey(), "RC-2.4.1#a");
    }

    @SuppressWarnings({"EqualsWithItself", "SimplifiableAssertion", "ConstantValue"})
    @Test
    void testEquals() {
        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 2, 1, "a");
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 2, 1, "a");
        final DatasetAddress datasetAddress3 = new DatasetAddress(1, 2, 2, "a");
        final DatasetAddress datasetAddress4 = new DatasetAddress(1, 1, 1);
        final DatasetAddress datasetAddress5 = new DatasetAddress(2, 2, 1);
        final DatasetAddress datasetAddress6 = new DatasetAddress(1, 2);
        final DatasetAddress datasetAddress7 = new DatasetAddress(1, 2, 1, "b");

        assertTrue(datasetAddress1.equals(datasetAddress1));
        assertTrue(datasetAddress1.equals(datasetAddress2));
        assertFalse(datasetAddress1.equals(new Object()));
        assertFalse(datasetAddress1.equals(null));
        assertFalse(datasetAddress1.equals(datasetAddress3));
        assertFalse(datasetAddress1.equals(datasetAddress4));
        assertFalse(datasetAddress1.equals(datasetAddress5));
        assertFalse(datasetAddress1.equals(datasetAddress6));
        assertFalse(datasetAddress1.equals(datasetAddress7));
    }

    @Test
    void testHashCode() {
        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 2, 1, "a");
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 2, 1, "a");
        final DatasetAddress datasetAddress3 = new DatasetAddress(1, 2, 2, "a");

        assertEquals(datasetAddress1.hashCode(), datasetAddress2.hashCode());
        assertNotEquals(datasetAddress1.hashCode(), datasetAddress3.hashCode());
    }

    @Test
    void toStringTest() {
        final DatasetAddress datasetAddress = new DatasetAddress(1, 2, 1, "a");
        assertEquals(datasetAddress.toString(), "1.2.1#a");
    }

    @Test
    void toStringTest_NamingDisabled() {
        ReplicantTestUtil.disableNames();
        final DatasetAddress datasetAddress =
                new DatasetAddress(ValueUtil.randomInt(), ValueUtil.randomInt(), ValueUtil.randomInt());
        assertEquals(
                datasetAddress.toString(),
                "replicant.DatasetAddress@" + Integer.toHexString(datasetAddress.hashCode()));
    }

    @Test
    void getName_NamingDisabled() {
        ReplicantTestUtil.disableNames();
        assertEquals(new DatasetAddress(1, 3, 5, "a").getName(), "1.3.5#a");
    }

    @Test
    void asDatasetAddressDescriptor() {
        assertEquals(new DatasetAddress(1, 3, 5, "a").asDatasetAddressDescriptor(), "3.5#a");
        assertEquals(new DatasetAddress(1, 3).asDatasetAddressDescriptor(), "3");
    }

    @SuppressWarnings({"EqualsWithItself", "SelfComparison"})
    @Test
    void compareTo() {
        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 1);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 2, 3);
        final DatasetAddress datasetAddress3 = new DatasetAddress(1, 2, 2);
        final DatasetAddress datasetAddress4 = new DatasetAddress(2, 1);
        final DatasetAddress datasetAddress5 = new DatasetAddress(1, 2);
        final DatasetAddress datasetAddress6 = new DatasetAddress(1, 2, null, "a");

        // Different schema
        assertEquals(datasetAddress1.compareTo(datasetAddress4), -1);
        assertEquals(datasetAddress4.compareTo(datasetAddress1), 1);

        // Same System Schema, different Dataset
        assertEquals(datasetAddress1.compareTo(datasetAddress2), -1);
        assertEquals(datasetAddress2.compareTo(datasetAddress1), 1);

        // Same System Schema and Dataset, different Dataset Roots (value vs value)
        assertEquals(datasetAddress2.compareTo(datasetAddress3), 1);
        assertEquals(datasetAddress3.compareTo(datasetAddress2), -1);

        // Same System Schema and Dataset, different Dataset Roots (null vs value)
        assertEquals(datasetAddress5.compareTo(datasetAddress2), -1);
        assertEquals(datasetAddress2.compareTo(datasetAddress5), 1);

        // Same System Schema and Dataset, same absent Dataset Root
        assertEquals(datasetAddress1.compareTo(datasetAddress1), 0);
        assertEquals(datasetAddress5.compareTo(datasetAddress5), 0);

        // Same System Schema, Dataset, and Dataset Root, different Dataset Keys
        assertEquals(datasetAddress5.compareTo(datasetAddress6), -1);
        assertEquals(datasetAddress6.compareTo(datasetAddress5), 1);
    }
}
