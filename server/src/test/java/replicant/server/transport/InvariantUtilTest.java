package replicant.server.transport;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import replicant.server.DatasetAddress;
import replicant.server.DatasetAddressTemplate;
import replicant.server.SubscriptionDependencyCandidate;

public final class InvariantUtilTest {
    @Test
    public void assertAddressMatchesDatasetMetadata_allowsConcreteKeyedAddress() {
        final var unfiltered = new DatasetMetadata(
                0,
                "Source",
                null,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var keyed = new DatasetMetadata(
                1,
                "Target",
                7,
                DatasetMetadata.FilterMode.PARAMETER_FILTERED,
                DatasetMetadata.FilterParameterMode.FIXED,
                true,
                DatasetMetadata.CacheType.NONE,
                true);
        final var schema = new SchemaMetaData("Test", unfiltered, keyed);

        InvariantUtil.assertDatasetAddressMatchesDatasetMetadata(schema, DatasetAddress.of(1, 2, "fi"));
    }

    @Test
    public void assertAddress_rejectsConcreteKeyedDatasetAddressWithoutDatasetKey() {
        final var keyed = new DatasetMetadata(
                0,
                "Target",
                7,
                DatasetMetadata.FilterMode.PARAMETER_FILTERED,
                DatasetMetadata.FilterParameterMode.FIXED,
                true,
                DatasetMetadata.CacheType.NONE,
                true);
        final var schema = new SchemaMetaData("Test", keyed);

        expectThrows(
                AssertionError.class,
                () -> InvariantUtil.assertDatasetAddressMatchesDatasetMetadata(schema, DatasetAddress.of(0, 2)));
    }

    @Test
    public void assertSubscriptionDependencyCandidate_rejectsDatasetAddressTemplateForNonKeyedDataset() {
        final var source = new DatasetMetadata(
                0,
                "Source",
                null,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var target = new DatasetMetadata(
                1,
                "Target",
                null,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var schema = new SchemaMetaData("Test", source, target);

        expectThrows(
                AssertionError.class,
                () -> InvariantUtil.assertSubscriptionDependencyCandidate(
                        schema,
                        new SubscriptionDependencyCandidate(DatasetAddress.of(0), DatasetAddressTemplate.of(1))));
    }

    @Test
    public void assertSubscriptionDependencyCandidate_allowsTemplateWithMissingTargetFilterParameter() {
        final var source = new DatasetMetadata(
                0,
                "Source",
                null,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var target = new DatasetMetadata(
                1,
                "Target",
                1,
                DatasetMetadata.FilterMode.PARAMETER_FILTERED,
                DatasetMetadata.FilterParameterMode.FIXED,
                true,
                DatasetMetadata.CacheType.NONE,
                true);
        final var schema = new SchemaMetaData("Test", source, target);

        InvariantUtil.assertSubscriptionDependencyCandidate(
                schema, new SubscriptionDependencyCandidate(DatasetAddress.of(0), DatasetAddressTemplate.of(1, 7)));
    }
}
