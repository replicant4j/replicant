package replicant.server.transport;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class ChannelMetaDataTest {
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void typeGraph() {
        final var metaData = new ChannelMetaData(
                1, "MetaData", null, ChannelMetaData.FilterType.NONE, ChannelMetaData.CacheType.NONE, false);
        assertEquals(metaData.getChannelId(), 1);
        assertEquals(metaData.getName(), "MetaData");
        assertTrue(metaData.isTypeGraph());
        assertFalse(metaData.isInstanceGraph());
        assertEquals(metaData.filterType(), ChannelMetaData.FilterType.NONE);
        assertFalse(metaData.isCacheable());
        assertFalse(metaData.requiresFilterParameter());
        assertFalse(metaData.requiresFilterInstanceId());
        assertFalse(metaData.isExternal());

        assertThrows(metaData::getInstanceRootEntityTypeId);
    }

    @Test
    public void instanceGraph() {
        final var metaData = new ChannelMetaData(
                1, "MetaData", 23, ChannelMetaData.FilterType.NONE, ChannelMetaData.CacheType.NONE, true);
        assertEquals(metaData.getChannelId(), 1);
        assertEquals(metaData.getName(), "MetaData");
        assertFalse(metaData.isTypeGraph());
        assertTrue(metaData.isInstanceGraph());
        assertEquals(metaData.getInstanceRootEntityTypeId(), (Integer) 23);
        assertEquals(metaData.filterType(), ChannelMetaData.FilterType.NONE);
        assertFalse(metaData.isCacheable());
        assertFalse(metaData.requiresFilterParameter());
        assertFalse(metaData.requiresFilterInstanceId());
        assertTrue(metaData.isExternal());
    }

    @Test
    public void filteredGraph() {
        final var metaData = new ChannelMetaData(
                1, "MetaData", 22, ChannelMetaData.FilterType.STATIC, ChannelMetaData.CacheType.NONE, true);
        assertEquals(metaData.getChannelId(), 1);
        assertEquals(metaData.getName(), "MetaData");
        assertFalse(metaData.isTypeGraph());
        assertTrue(metaData.isInstanceGraph());
        assertEquals(metaData.filterType(), ChannelMetaData.FilterType.STATIC);
        assertFalse(metaData.isCacheable());
        assertTrue(metaData.requiresFilterParameter());
        assertFalse(metaData.requiresFilterInstanceId());
        assertTrue(metaData.isExternal());
    }

    @Test
    public void staticInstancedFilteredGraph() {
        final var metaData = new ChannelMetaData(
                2, "MetaData", 22, ChannelMetaData.FilterType.STATIC_INSTANCED, ChannelMetaData.CacheType.NONE, true);
        assertEquals(metaData.getChannelId(), 2);
        assertEquals(metaData.getName(), "MetaData");
        assertFalse(metaData.isTypeGraph());
        assertTrue(metaData.isInstanceGraph());
        assertEquals(metaData.filterType(), ChannelMetaData.FilterType.STATIC_INSTANCED);
        assertFalse(metaData.isCacheable());
        assertTrue(metaData.requiresFilterParameter());
        assertTrue(metaData.requiresFilterInstanceId());
        assertTrue(metaData.isExternal());
    }

    @Test
    public void dynamicInstancedFilteredGraph() {
        final var metaData = new ChannelMetaData(
                3, "MetaData", 22, ChannelMetaData.FilterType.DYNAMIC_INSTANCED, ChannelMetaData.CacheType.NONE, true);
        assertEquals(metaData.getChannelId(), 3);
        assertEquals(metaData.getName(), "MetaData");
        assertFalse(metaData.isTypeGraph());
        assertTrue(metaData.isInstanceGraph());
        assertEquals(metaData.filterType(), ChannelMetaData.FilterType.DYNAMIC_INSTANCED);
        assertFalse(metaData.isCacheable());
        assertTrue(metaData.requiresFilterParameter());
        assertTrue(metaData.requiresFilterInstanceId());
        assertTrue(metaData.isExternal());
    }
}
