package replicant.server.transport;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import replicant.server.ChannelAddress;
import replicant.server.ChannelLink;

public final class InvariantUtilTest {
    @Test
    public void
            assertAddress_MatchesChannelMetaData_MatchesChannelMetaData_allowsConcreteAndPartialInstancedAddresses() {
        final var unfiltered = new ChannelMetaData(
                0, "Source", null, ChannelMetaData.FilterType.NONE, ChannelMetaData.CacheType.NONE, true);
        final var instanced = new ChannelMetaData(
                1, "Target", 7, ChannelMetaData.FilterType.STATIC_INSTANCED, ChannelMetaData.CacheType.NONE, true);
        final var schema = new SchemaMetaData("Test", unfiltered, instanced);

        InvariantUtil.assertAddressMatchesChannelMetaData(schema, ChannelAddress.of(1, 2, "fi"));
        InvariantUtil.assertAddressMatchesChannelMetaData(schema, ChannelAddress.partial(1, 2));
    }

    @Test
    public void
            assertAddress_rejectsConcreteInstancedAddressMatchesChannelMetaDataMatchesChannelMetaDataWithoutInstanceId() {
        final var instanced = new ChannelMetaData(
                0, "Target", 7, ChannelMetaData.FilterType.STATIC_INSTANCED, ChannelMetaData.CacheType.NONE, true);
        final var schema = new SchemaMetaData("Test", instanced);

        expectThrows(
                AssertionError.class,
                () -> InvariantUtil.assertAddressMatchesChannelMetaData(schema, ChannelAddress.of(0, 2)));
    }

    @Test
    public void
            assertAddress_rejectsPartialAddressMatchesChannelMetaDataMatchesChannelMetaDataForNonInstancedChannel() {
        final var channel = new ChannelMetaData(
                0, "Target", null, ChannelMetaData.FilterType.NONE, ChannelMetaData.CacheType.NONE, true);
        final var schema = new SchemaMetaData("Test", channel);

        expectThrows(
                AssertionError.class,
                () -> InvariantUtil.assertAddressMatchesChannelMetaData(schema, ChannelAddress.partial(0)));
    }

    @Test
    public void channelLink_constructorRejectsConcreteLinkWithPartialAddress() {
        expectThrows(
                AssertionError.class, () -> new ChannelLink(ChannelAddress.partial(0), ChannelAddress.of(1, 7, "fi")));
    }

    @Test
    public void assertLink_allowsPartialLinkWithMissingTargetFilter() {
        final var source = new ChannelMetaData(
                0, "Source", null, ChannelMetaData.FilterType.NONE, ChannelMetaData.CacheType.NONE, true);
        final var target = new ChannelMetaData(
                1, "Target", 1, ChannelMetaData.FilterType.STATIC, ChannelMetaData.CacheType.NONE, true);
        final var schema = new SchemaMetaData("Test", source, target);

        InvariantUtil.assertLink(schema, new ChannelLink(ChannelAddress.of(0), ChannelAddress.of(1, 7), null, true));
    }

    @Test
    public void assertLink_rejectsConcreteFilteredLinkWithoutTargetFilter() {
        final var source = new ChannelMetaData(
                0, "Source", null, ChannelMetaData.FilterType.NONE, ChannelMetaData.CacheType.NONE, true);
        final var target = new ChannelMetaData(
                1, "Target", 1, ChannelMetaData.FilterType.STATIC, ChannelMetaData.CacheType.NONE, true);
        final var schema = new SchemaMetaData("Test", source, target);

        expectThrows(
                AssertionError.class,
                () -> InvariantUtil.assertLink(schema, new ChannelLink(ChannelAddress.of(0), ChannelAddress.of(1, 7))));
    }
}
