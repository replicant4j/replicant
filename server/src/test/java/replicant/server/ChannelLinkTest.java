package replicant.server;

import static org.testng.Assert.*;

import org.jspecify.annotations.NonNull;
import org.testng.annotations.Test;

public final class ChannelLinkTest {
    @Test
    public void basicOperation() {
        final var link = new ChannelLink(ChannelAddress.of(22, 44), ChannelAddress.of(1, 2));
        assertEquals(link.source().channelId(), 22);
        assertEquals(link.source().rootId(), (Integer) 44);
        assertEquals(link.target().channelId(), 1);
        assertEquals(link.target().rootId(), (Integer) 2);
        assertFalse(link.partial());
        assertEquals(link.toString(), "[22.44=>1.2]");
    }

    @Test
    public void partialOperation() {
        final var link = new ChannelLink(ChannelAddress.partial(22, 44), ChannelAddress.of(1, 2), null, true);

        assertTrue(link.partial());
        assertEquals(link.toString(), "[22.44?=>1.2?]");
    }

    @Test
    public void hashcodeAndEquals() {
        final var link1 = new ChannelLink(ChannelAddress.of(22, 44), ChannelAddress.of(1, 2));
        final var link2 = new ChannelLink(ChannelAddress.of(22, 44), ChannelAddress.of(1, 3));
        final var link3 = new ChannelLink(ChannelAddress.of(22, 77), ChannelAddress.of(1, 2));
        final var link4 = new ChannelLink(ChannelAddress.of(27), ChannelAddress.of(1, 2));
        final var link5 = new ChannelLink(ChannelAddress.of(27), ChannelAddress.of(1, 3));
        final var link6 = new ChannelLink(ChannelAddress.partial(22, 44), ChannelAddress.of(1, 2), null, true);

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
