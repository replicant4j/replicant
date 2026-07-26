package replicant.server.transport;

import static org.testng.Assert.*;

import java.util.Set;
import org.testng.annotations.Test;

public class DatasetMetadataTest {
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void typeDataset() {
        final var metaData = new DatasetMetadata(
                1, "MetaData", null, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, false);
        assertEquals(metaData.getDatasetId(), 1);
        assertEquals(metaData.getName(), "MetaData");
        assertTrue(metaData.isTypeDataset());
        assertFalse(metaData.isInstanceDataset());
        assertEquals(metaData.filterType(), DatasetMetadata.FilterType.NONE);
        assertFalse(metaData.isCacheable());
        assertFalse(metaData.requiresFilterParameter());
        assertFalse(metaData.requiresDatasetKey());
        assertFalse(metaData.isExternal());

        assertThrows(metaData::getDatasetRootEntityTypeId);
    }

    @Test
    public void instanceDataset() {
        final var metaData = new DatasetMetadata(
                1, "MetaData", 23, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, true);
        assertEquals(metaData.getDatasetId(), 1);
        assertEquals(metaData.getName(), "MetaData");
        assertFalse(metaData.isTypeDataset());
        assertTrue(metaData.isInstanceDataset());
        assertEquals(metaData.getDatasetRootEntityTypeId(), (Integer) 23);
        assertEquals(metaData.filterType(), DatasetMetadata.FilterType.NONE);
        assertFalse(metaData.isCacheable());
        assertFalse(metaData.requiresFilterParameter());
        assertFalse(metaData.requiresDatasetKey());
        assertTrue(metaData.isExternal());
    }

    @Test
    public void filteredDataset() {
        final var metaData = new DatasetMetadata(
                1, "MetaData", 22, DatasetMetadata.FilterType.STATIC, false, DatasetMetadata.CacheType.NONE, true);
        assertEquals(metaData.getDatasetId(), 1);
        assertEquals(metaData.getName(), "MetaData");
        assertFalse(metaData.isTypeDataset());
        assertTrue(metaData.isInstanceDataset());
        assertEquals(metaData.filterType(), DatasetMetadata.FilterType.STATIC);
        assertFalse(metaData.isCacheable());
        assertTrue(metaData.requiresFilterParameter());
        assertFalse(metaData.requiresDatasetKey());
        assertTrue(metaData.isExternal());
    }

    @Test
    public void staticKeyedFilteredDataset() {
        final var metaData = new DatasetMetadata(
                2, "MetaData", 22, DatasetMetadata.FilterType.STATIC, true, DatasetMetadata.CacheType.NONE, true);
        assertEquals(metaData.getDatasetId(), 2);
        assertEquals(metaData.getName(), "MetaData");
        assertFalse(metaData.isTypeDataset());
        assertTrue(metaData.isInstanceDataset());
        assertEquals(metaData.filterType(), DatasetMetadata.FilterType.STATIC);
        assertFalse(metaData.isCacheable());
        assertTrue(metaData.requiresFilterParameter());
        assertTrue(metaData.requiresDatasetKey());
        assertTrue(metaData.isExternal());
    }

    @Test
    public void dynamicKeyedFilteredDataset() {
        final var metaData = new DatasetMetadata(
                3, "MetaData", 22, DatasetMetadata.FilterType.DYNAMIC, true, DatasetMetadata.CacheType.NONE, true);
        assertEquals(metaData.getDatasetId(), 3);
        assertEquals(metaData.getName(), "MetaData");
        assertFalse(metaData.isTypeDataset());
        assertTrue(metaData.isInstanceDataset());
        assertEquals(metaData.filterType(), DatasetMetadata.FilterType.DYNAMIC);
        assertFalse(metaData.isCacheable());
        assertTrue(metaData.requiresFilterParameter());
        assertTrue(metaData.requiresDatasetKey());
        assertTrue(metaData.isExternal());
    }

    @Test
    public void requiredTypeDatasetsTrackDependencyDirection() {
        final var requiredTypeDataset = new DatasetMetadata(
                1, "MetaData", null, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, false);
        final var requiringDataset = new DatasetMetadata(
                2,
                "Event",
                22,
                DatasetMetadata.FilterType.NONE,
                false,
                DatasetMetadata.CacheType.NONE,
                true,
                requiredTypeDataset);

        assertEquals(requiringDataset.getRequiredTypeDatasets(), new DatasetMetadata[] {requiredTypeDataset});
        assertEquals(requiredTypeDataset.getDependentDatasets(), Set.of(requiringDataset));
        assertTrue(requiringDataset.getDependentDatasets().isEmpty());
        assertEquals(requiredTypeDataset.getRequiredTypeDatasets().length, 0);
    }

    @Test
    public void requiredTypeDatasetMustBeTypeDataset() {
        final var instanceDataset = new DatasetMetadata(
                1, "Event", 22, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, true);

        final var error = expectThrows(
                IllegalArgumentException.class,
                () -> new DatasetMetadata(
                        2,
                        "Requiring",
                        null,
                        DatasetMetadata.FilterType.NONE,
                        false,
                        DatasetMetadata.CacheType.NONE,
                        true,
                        instanceDataset));
        assertEquals(error.getMessage(), "Specified Required Type Dataset Event is not a Type Dataset");
    }
}
