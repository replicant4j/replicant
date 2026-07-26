package replicant.server;

import static org.testng.Assert.*;

import org.jspecify.annotations.NonNull;
import org.testng.annotations.Test;

public final class ChannelLinkTest {
    @Test
    public void basicOperation() {
        final var link = new ChannelLink(DatasetAddress.of(22, 44), DatasetAddress.of(1, 2));
        assertEquals(link.sourceDatasetAddress().datasetId(), 22);
        assertEquals(link.sourceDatasetAddress().datasetRootId(), (Integer) 44);
        assertEquals(link.targetDatasetAddress().datasetId(), 1);
        assertEquals(link.targetDatasetAddress().datasetRootId(), (Integer) 2);
        assertFalse(link.partial());
        assertEquals(link.toString(), "[22.44=>1.2]");
    }

    @Test
    public void partialOperation() {
        final var link = new ChannelLink(DatasetAddress.partial(22, 44), DatasetAddress.of(1, 2), null, true);

        assertTrue(link.partial());
        assertEquals(link.toString(), "[22.44?=>1.2?]");
    }

    @Test
    public void hashcodeAndEquals() {
        final var link1 = new ChannelLink(DatasetAddress.of(22, 44), DatasetAddress.of(1, 2));
        final var link2 = new ChannelLink(DatasetAddress.of(22, 44), DatasetAddress.of(1, 3));
        final var link3 = new ChannelLink(DatasetAddress.of(22, 77), DatasetAddress.of(1, 2));
        final var link4 = new ChannelLink(DatasetAddress.of(27), DatasetAddress.of(1, 2));
        final var link5 = new ChannelLink(DatasetAddress.of(27), DatasetAddress.of(1, 3));
        final var link6 = new ChannelLink(DatasetAddress.partial(22, 44), DatasetAddress.of(1, 2), null, true);

        assertLinkEqual(link1, link1);
        assertLinkEqual(link2, link2);
        assertLinkEqual(link3, link3);
        assertLinkEqual(link4, link4);
        assertLinkEqual(link5, link5);
        assertLinkEqual(link6, link6);

        assertLinkNotEqual(link1, link2);
        assertLinkNotEqual(link1, link3);
        assertLinkNotEqual(link1, link4);
        assertLinkNotEqual(link1, link5);
        assertLinkNotEqual(link1, link6);

        assertLinkNotEqual(link2, link3);
        assertLinkNotEqual(link2, link4);
        assertLinkNotEqual(link2, link5);
        assertLinkNotEqual(link2, link6);

        assertLinkNotEqual(link3, link4);
        assertLinkNotEqual(link3, link5);
        assertLinkNotEqual(link3, link6);

        assertLinkNotEqual(link4, link5);
        assertLinkNotEqual(link4, link6);

        assertLinkNotEqual(link5, link6);
    }

    private void assertLinkEqual(@NonNull final ChannelLink link1, @NonNull final ChannelLink link2) {
        assertEquals(link1, link2);
        assertEquals(link1.hashCode(), link2.hashCode());
    }

    private void assertLinkNotEqual(@NonNull final ChannelLink link1, @NonNull final ChannelLink link2) {
        assertNotEquals(link1, link2);
        assertNotEquals(link1.hashCode(), link2.hashCode());
    }
}
