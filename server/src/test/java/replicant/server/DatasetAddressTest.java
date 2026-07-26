package replicant.server;

import static org.testng.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.testng.annotations.Test;

@SuppressWarnings("EqualsWithItself")
public class DatasetAddressTest {
    @Test
    public void basicOperation() {
        final var datasetAddress1 = DatasetAddress.of(1, 22, "a");
        final var datasetAddress2 = DatasetAddress.of(1, 22, "a");
        final var datasetAddress3 = DatasetAddress.of(1, 23, "a");
        final var datasetAddress4 = DatasetAddress.of(2);
        final var datasetAddress5 = DatasetAddress.of(3);
        final var datasetAddress6 = DatasetAddress.of(2);
        final var datasetAddress7 = DatasetAddress.of(1, 22, "b");

        assertEquals(datasetAddress1.datasetId(), 1);
        assertEquals(datasetAddress1.datasetRootId(), (Integer) 22);
        assertFalse(datasetAddress1.partial());
        assertTrue(datasetAddress1.concrete());
        assertTrue(datasetAddress1.hasDatasetRootId());
        assertEquals(datasetAddress1.toString(), "1.22#a");
        assertEquals(datasetAddress1, datasetAddress1);
        assertEquals(datasetAddress2, datasetAddress1);
        assertNotEquals(datasetAddress3, datasetAddress1);
        assertNotEquals(datasetAddress4, datasetAddress1);
        assertNotEquals(datasetAddress7, datasetAddress1);

        assertEquals(datasetAddress4.datasetId(), 2);
        assertNull(datasetAddress4.datasetRootId());
        assertFalse(datasetAddress4.partial());
        assertTrue(datasetAddress4.concrete());
        assertFalse(datasetAddress4.hasDatasetRootId());
        assertEquals(datasetAddress4.toString(), "2");
        assertEquals(datasetAddress4, datasetAddress4);
        assertEquals(datasetAddress6, datasetAddress4);
        assertNotEquals(datasetAddress3, datasetAddress4);
        assertNotEquals(datasetAddress5, datasetAddress4);

        final var list = new ArrayList<>(Arrays.asList(
                datasetAddress6,
                datasetAddress5,
                datasetAddress4,
                datasetAddress3,
                datasetAddress2,
                datasetAddress1,
                datasetAddress6,
                datasetAddress7));

        Collections.sort(list);

        final var expected = new DatasetAddress[] {
            datasetAddress1,
            datasetAddress2,
            datasetAddress7,
            datasetAddress3,
            datasetAddress4,
            datasetAddress6,
            datasetAddress6,
            datasetAddress5
        };
        assertEquals(list.toArray(new DatasetAddress[0]), expected);
    }

    @Test
    public void parse() {
        final var datasetAddress1 = DatasetAddress.parse("1.22");
        assertEquals(datasetAddress1.datasetId(), 1);
        assertEquals(datasetAddress1.datasetRootId(), (Integer) 22);
        assertNull(datasetAddress1.datasetKey());
        assertFalse(datasetAddress1.partial());
        assertTrue(datasetAddress1.concrete());
        final var datasetAddress2 = DatasetAddress.parse("0");
        assertEquals(datasetAddress2.datasetId(), 0);
        assertNull(datasetAddress2.datasetRootId());
        assertNull(datasetAddress2.datasetKey());
        assertFalse(datasetAddress2.partial());
        assertTrue(datasetAddress2.concrete());
    }

    @Test
    public void parseWithDatasetKey() {
        final var datasetAddress1 = DatasetAddress.parse("1.22#alpha");
        assertEquals(datasetAddress1.datasetId(), 1);
        assertEquals(datasetAddress1.datasetRootId(), (Integer) 22);
        assertEquals(datasetAddress1.datasetKey(), "alpha");
        assertFalse(datasetAddress1.partial());
        final var datasetAddress2 = DatasetAddress.parse("0#alpha");
        assertEquals(datasetAddress2.datasetId(), 0);
        assertNull(datasetAddress2.datasetRootId());
        assertEquals(datasetAddress2.datasetKey(), "alpha");
        assertFalse(datasetAddress2.partial());
    }

    @Test
    public void partialDatasetAddress() {
        final var datasetAddress = DatasetAddress.partial(1, 22);

        assertEquals(datasetAddress.datasetId(), 1);
        assertEquals(datasetAddress.datasetRootId(), (Integer) 22);
        assertNull(datasetAddress.datasetKey());
        assertTrue(datasetAddress.partial());
        assertEquals(datasetAddress.toString(), "1.22?");
    }

    @Test
    public void compareTo_distinguishesPartialFromConcrete() {
        final var concrete = DatasetAddress.of(1, 22);
        final var partial = DatasetAddress.partial(1, 22);

        assertTrue(concrete.compareTo(partial) < 0);
        assertTrue(partial.compareTo(concrete) > 0);
    }
}
