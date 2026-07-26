package replicant.server.transport;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import replicant.server.ChannelLink;
import replicant.server.DatasetAddress;

public final class InvariantUtilTest {
    @Test
    public void assertAddress_MatchesChannelMetaData_MatchesChannelMetaData_allowsConcreteAndPartialKeyedAddresses() {
        final var unfiltered = new ChannelMetaData(
                0, "Source", null, ChannelMetaData.FilterType.NONE, false, ChannelMetaData.CacheType.NONE, true);
        final var keyed = new ChannelMetaData(
                1, "Target", 7, ChannelMetaData.FilterType.STATIC, true, ChannelMetaData.CacheType.NONE, true);
        final var schema = new SchemaMetaData("Test", unfiltered, keyed);

        InvariantUtil.assertDatasetAddressMatchesChannelMetaData(schema, DatasetAddress.of(1, 2, "fi"));
        InvariantUtil.assertDatasetAddressMatchesChannelMetaData(schema, DatasetAddress.partial(1, 2));
    }

    @Test
    public void
            assertAddress_rejectsConcreteKeyedAddressMatchesChannelMetaDataMatchesChannelMetaDataWithoutDatasetKey() {
        final var keyed = new ChannelMetaData(
                0, "Target", 7, ChannelMetaData.FilterType.STATIC, true, ChannelMetaData.CacheType.NONE, true);
        final var schema = new SchemaMetaData("Test", keyed);

        expectThrows(
                AssertionError.class,
                () -> InvariantUtil.assertDatasetAddressMatchesChannelMetaData(schema, DatasetAddress.of(0, 2)));
    }

    @Test
    public void assertAddress_rejectsPartialAddressMatchesChannelMetaDataMatchesChannelMetaDataForNonKeyedChannel() {
        final var channel = new ChannelMetaData(
                0, "Target", null, ChannelMetaData.FilterType.NONE, false, ChannelMetaData.CacheType.NONE, true);
        final var schema = new SchemaMetaData("Test", channel);

        expectThrows(
                AssertionError.class,
                () -> InvariantUtil.assertDatasetAddressMatchesChannelMetaData(schema, DatasetAddress.partial(0)));
    }

    @Test
    public void channelLink_constructorRejectsConcreteLinkWithPartialAddress() {
        expectThrows(
                AssertionError.class, () -> new ChannelLink(DatasetAddress.partial(0), DatasetAddress.of(1, 7, "fi")));
    }

    @Test
    public void assertLink_allowsPartialLinkWithMissingTargetFilter() {
        final var source = new ChannelMetaData(
                0, "Source", null, ChannelMetaData.FilterType.NONE, false, ChannelMetaData.CacheType.NONE, true);
        final var target = new ChannelMetaData(
                1, "Target", 1, ChannelMetaData.FilterType.STATIC, false, ChannelMetaData.CacheType.NONE, true);
        final var schema = new SchemaMetaData("Test", source, target);

        InvariantUtil.assertLink(schema, new ChannelLink(DatasetAddress.of(0), DatasetAddress.of(1, 7), null, true));
    }

    @Test
    public void assertLink_rejectsConcreteFilteredLinkWithoutTargetFilter() {
        final var source = new ChannelMetaData(
                0, "Source", null, ChannelMetaData.FilterType.NONE, false, ChannelMetaData.CacheType.NONE, true);
        final var target = new ChannelMetaData(
                1, "Target", 1, ChannelMetaData.FilterType.STATIC, false, ChannelMetaData.CacheType.NONE, true);
        final var schema = new SchemaMetaData("Test", source, target);

        expectThrows(
                AssertionError.class,
                () -> InvariantUtil.assertLink(schema, new ChannelLink(DatasetAddress.of(0), DatasetAddress.of(1, 7))));
    }
}
