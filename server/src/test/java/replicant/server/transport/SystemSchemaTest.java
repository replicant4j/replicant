package replicant.server.transport;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import replicant.server.ValueUtil;

public class SystemSchemaTest {
    @Test
    public void basicOperation() {
        final var dataset0 =
                new Dataset(0, ValueUtil.randomString(), 2, Dataset.FilterMode.UNFILTERED, null, false, false, false);
        final var dataset1 = new Dataset(
                1, ValueUtil.randomString(), null, Dataset.FilterMode.UNFILTERED, null, false, false, false);
        final var dataset2 =
                new Dataset(2, ValueUtil.randomString(), 54, Dataset.FilterMode.UNFILTERED, null, false, false, false);
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
