package replicant.server.transport;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import replicant.server.ValueUtil;

public class SystemSchemaTest {
    @Test
    public void reservedDatasetId() {
        final var dataset = new Dataset(
                1, "Present", 54, Dataset.FilterMode.UNFILTERED, null, false, false, Dataset.Visibility.INTERNAL);
        final var schema = new SystemSchema("Sparse", null, dataset);
        assertFalse(schema.hasDataset(0));
        assertThrows(IllegalArgumentException.class, () -> schema.getDataset(0));
        assertSame(schema.getDataset(1), dataset);
        assertEquals(schema.getDatasetCount(), 2);
        assertEquals(schema.getInstanceDatasetCount(), 1);
        assertSame(schema.getInstanceDatasetByIndex(0), dataset);
    }

    @Test
    public void basicOperation() {
        final var dataset0 = new Dataset(
                0,
                ValueUtil.randomString(),
                2,
                Dataset.FilterMode.UNFILTERED,
                null,
                false,
                false,
                Dataset.Visibility.INTERNAL);
        final var dataset1 = new Dataset(
                1,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.UNFILTERED,
                null,
                false,
                false,
                Dataset.Visibility.INTERNAL);
        final var dataset2 = new Dataset(
                2,
                ValueUtil.randomString(),
                54,
                Dataset.FilterMode.UNFILTERED,
                null,
                false,
                false,
                Dataset.Visibility.INTERNAL);
        final var name = ValueUtil.randomString();

        final var systemSchema = new SystemSchema(name, dataset0, dataset1, dataset2);

        assertEquals(systemSchema.getName(), name);
        assertEquals(systemSchema.getDataset(0), dataset0);
        assertEquals(systemSchema.getDataset(1), dataset1);
        assertEquals(systemSchema.getDataset(2), dataset2);
        assertEquals(systemSchema.getDatasetCount(), 3);
        assertEquals(systemSchema.getInstanceDatasetCount(), 2);
        assertEquals(systemSchema.getInstanceDatasetCount(), 2);
        assertEquals(systemSchema.getInstanceDatasetByIndex(0), dataset0);
        assertEquals(systemSchema.getInstanceDatasetByIndex(1), dataset2);
    }
}
