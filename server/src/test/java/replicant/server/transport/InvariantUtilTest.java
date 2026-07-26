package replicant.server.transport;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import replicant.server.DatasetAddress;
import replicant.server.SubscriptionDependency;

public final class InvariantUtilTest {
    @Test
    public void assertAddressMatchesDatasetMetadata_allowsConcreteAndPartialKeyedAddresses() {
        final var unfiltered = new DatasetMetadata(
                0, "Source", null, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, true);
        final var keyed = new DatasetMetadata(
                1, "Target", 7, DatasetMetadata.FilterType.STATIC, true, DatasetMetadata.CacheType.NONE, true);
        final var schema = new SchemaMetaData("Test", unfiltered, keyed);

        InvariantUtil.assertDatasetAddressMatchesDatasetMetadata(schema, DatasetAddress.of(1, 2, "fi"));
        InvariantUtil.assertDatasetAddressMatchesDatasetMetadata(schema, DatasetAddress.partial(1, 2));
    }

    @Test
    public void assertAddress_rejectsConcreteKeyedDatasetAddressWithoutDatasetKey() {
        final var keyed = new DatasetMetadata(
                0, "Target", 7, DatasetMetadata.FilterType.STATIC, true, DatasetMetadata.CacheType.NONE, true);
        final var schema = new SchemaMetaData("Test", keyed);

        expectThrows(
                AssertionError.class,
                () -> InvariantUtil.assertDatasetAddressMatchesDatasetMetadata(schema, DatasetAddress.of(0, 2)));
    }

    @Test
    public void assertAddress_rejectsPartialAddressForNonKeyedDataset() {
        final var dataset = new DatasetMetadata(
                0, "Target", null, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, true);
        final var schema = new SchemaMetaData("Test", dataset);

        expectThrows(
                AssertionError.class,
                () -> InvariantUtil.assertDatasetAddressMatchesDatasetMetadata(schema, DatasetAddress.partial(0)));
    }

    @Test
    public void subscriptionDependency_constructorRejectsConcreteLinkWithPartialAddress() {
        expectThrows(
                AssertionError.class,
                () -> new SubscriptionDependency(DatasetAddress.partial(0), DatasetAddress.of(1, 7, "fi")));
    }

    @Test
    public void assertSubscriptionDependency_allowsPartialLinkWithMissingTargetFilter() {
        final var source = new DatasetMetadata(
                0, "Source", null, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, true);
        final var target = new DatasetMetadata(
                1, "Target", 1, DatasetMetadata.FilterType.STATIC, false, DatasetMetadata.CacheType.NONE, true);
        final var schema = new SchemaMetaData("Test", source, target);

        InvariantUtil.assertSubscriptionDependency(
                schema, new SubscriptionDependency(DatasetAddress.of(0), DatasetAddress.of(1, 7), null, true));
    }

    @Test
    public void assertSubscriptionDependency_rejectsConcreteFilteredLinkWithoutTargetFilter() {
        final var source = new DatasetMetadata(
                0, "Source", null, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, true);
        final var target = new DatasetMetadata(
                1, "Target", 1, DatasetMetadata.FilterType.STATIC, false, DatasetMetadata.CacheType.NONE, true);
        final var schema = new SchemaMetaData("Test", source, target);

        expectThrows(
                AssertionError.class,
                () -> InvariantUtil.assertSubscriptionDependency(
                        schema, new SubscriptionDependency(DatasetAddress.of(0), DatasetAddress.of(1, 7))));
    }
}
