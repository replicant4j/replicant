package replicant.server.transport;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class DatasetMetadataTest {
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void typeGraph() {
        final var metaData = new DatasetMetadata(
                1, "MetaData", null, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, false);
        assertEquals(metaData.getDatasetId(), 1);
        assertEquals(metaData.getName(), "MetaData");
        assertTrue(metaData.isTypeGraph());
        assertFalse(metaData.isInstanceGraph());
        assertEquals(metaData.filterType(), DatasetMetadata.FilterType.NONE);
        assertFalse(metaData.isCacheable());
        assertFalse(metaData.requiresFilterParameter());
        assertFalse(metaData.requiresDatasetKey());
        assertFalse(metaData.isExternal());

        assertThrows(metaData::getInstanceRootEntityTypeId);
    }

    @Test
    public void instanceGraph() {
        final var metaData = new DatasetMetadata(
                1, "MetaData", 23, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, true);
        assertEquals(metaData.getDatasetId(), 1);
        assertEquals(metaData.getName(), "MetaData");
        assertFalse(metaData.isTypeGraph());
        assertTrue(metaData.isInstanceGraph());
        assertEquals(metaData.getInstanceRootEntityTypeId(), (Integer) 23);
        assertEquals(metaData.filterType(), DatasetMetadata.FilterType.NONE);
        assertFalse(metaData.isCacheable());
        assertFalse(metaData.requiresFilterParameter());
        assertFalse(metaData.requiresDatasetKey());
        assertTrue(metaData.isExternal());
    }

    @Test
    public void filteredGraph() {
        final var metaData = new DatasetMetadata(
                1, "MetaData", 22, DatasetMetadata.FilterType.STATIC, false, DatasetMetadata.CacheType.NONE, true);
        assertEquals(metaData.getDatasetId(), 1);
        assertEquals(metaData.getName(), "MetaData");
        assertFalse(metaData.isTypeGraph());
        assertTrue(metaData.isInstanceGraph());
        assertEquals(metaData.filterType(), DatasetMetadata.FilterType.STATIC);
        assertFalse(metaData.isCacheable());
        assertTrue(metaData.requiresFilterParameter());
        assertFalse(metaData.requiresDatasetKey());
        assertTrue(metaData.isExternal());
    }

    @Test
    public void staticKeyedFilteredGraph() {
        final var metaData = new DatasetMetadata(
                2, "MetaData", 22, DatasetMetadata.FilterType.STATIC, true, DatasetMetadata.CacheType.NONE, true);
        assertEquals(metaData.getDatasetId(), 2);
        assertEquals(metaData.getName(), "MetaData");
        assertFalse(metaData.isTypeGraph());
        assertTrue(metaData.isInstanceGraph());
        assertEquals(metaData.filterType(), DatasetMetadata.FilterType.STATIC);
        assertFalse(metaData.isCacheable());
        assertTrue(metaData.requiresFilterParameter());
        assertTrue(metaData.requiresDatasetKey());
        assertTrue(metaData.isExternal());
    }

    @Test
    public void dynamicKeyedFilteredGraph() {
        final var metaData = new DatasetMetadata(
                3, "MetaData", 22, DatasetMetadata.FilterType.DYNAMIC, true, DatasetMetadata.CacheType.NONE, true);
        assertEquals(metaData.getDatasetId(), 3);
        assertEquals(metaData.getName(), "MetaData");
        assertFalse(metaData.isTypeGraph());
        assertTrue(metaData.isInstanceGraph());
        assertEquals(metaData.filterType(), DatasetMetadata.FilterType.DYNAMIC);
        assertFalse(metaData.isCacheable());
        assertTrue(metaData.requiresFilterParameter());
        assertTrue(metaData.requiresDatasetKey());
        assertTrue(metaData.isExternal());
    }
}
