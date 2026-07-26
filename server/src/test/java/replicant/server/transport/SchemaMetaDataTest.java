package replicant.server.transport;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import replicant.server.ValueUtil;

public class SchemaMetaDataTest {
    @Test
    public void basicOperation() {
        final var dataset0 = new DatasetMetadata(
                0,
                ValueUtil.randomString(),
                2,
                DatasetMetadata.FilterType.NONE,
                false,
                DatasetMetadata.CacheType.NONE,
                false);
        final var dataset1 = new DatasetMetadata(
                1,
                ValueUtil.randomString(),
                null,
                DatasetMetadata.FilterType.NONE,
                false,
                DatasetMetadata.CacheType.NONE,
                false);
        final var dataset2 = new DatasetMetadata(
                2,
                ValueUtil.randomString(),
                54,
                DatasetMetadata.FilterType.NONE,
                false,
                DatasetMetadata.CacheType.NONE,
                false);
        final var name = ValueUtil.randomString();

        final var schemaMetaData = new SchemaMetaData(name, dataset0, dataset1, dataset2);

        assertEquals(schemaMetaData.getName(), name);
        assertEquals(schemaMetaData.getDatasetMetadata(0), dataset0);
        assertEquals(schemaMetaData.getDatasetMetadata(1), dataset1);
        assertEquals(schemaMetaData.getDatasetMetadata(2), dataset2);
        assertEquals(schemaMetaData.getDatasetCount(), 3);
        assertEquals(schemaMetaData.getInstanceDatasetCount(), 2);
        assertEquals(schemaMetaData.getInstanceDatasetCount(), 2);
        assertEquals(schemaMetaData.getInstanceDatasetByIndex(0), dataset0);
        assertEquals(schemaMetaData.getInstanceDatasetByIndex(1), dataset2);
    }
}
