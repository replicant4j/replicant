package replicant.server.transport;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import replicant.server.DatasetAddress;
import replicant.server.DatasetAddressTemplate;
import replicant.server.SubscriptionDependencyCandidate;

public final class InvariantUtilTest {
    @Test
    public void assertAddressMatchesDataset_allowsConcreteKeyedAddress() {
        final var unfiltered = new Dataset(
                0, "Source", null, Dataset.FilterMode.UNFILTERED, null, false, Dataset.CacheType.NONE, true);
        final var keyed = new Dataset(
                1,
                "Target",
                7,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.FIXED,
                true,
                Dataset.CacheType.NONE,
                true);
        final var systemSchema = new SystemSchema("Test", unfiltered, keyed);

        InvariantUtil.assertDatasetAddressMatchesDataset(systemSchema, DatasetAddress.of(1, 2, "fi"));
    }

    @Test
    public void assertAddress_rejectsConcreteKeyedDatasetAddressWithoutDatasetKey() {
        final var keyed = new Dataset(
                0,
                "Target",
                7,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.FIXED,
                true,
                Dataset.CacheType.NONE,
                true);
        final var systemSchema = new SystemSchema("Test", keyed);

        expectThrows(
                AssertionError.class,
                () -> InvariantUtil.assertDatasetAddressMatchesDataset(systemSchema, DatasetAddress.of(0, 2)));
    }

    @Test
    public void assertSubscriptionDependencyCandidate_rejectsDatasetAddressTemplateForNonKeyedDataset() {
        final var source = new Dataset(
                0, "Source", null, Dataset.FilterMode.UNFILTERED, null, false, Dataset.CacheType.NONE, true);
        final var target = new Dataset(
                1, "Target", null, Dataset.FilterMode.UNFILTERED, null, false, Dataset.CacheType.NONE, true);
        final var systemSchema = new SystemSchema("Test", source, target);

        expectThrows(
                AssertionError.class,
                () -> InvariantUtil.assertSubscriptionDependencyCandidate(
                        systemSchema,
                        new SubscriptionDependencyCandidate(DatasetAddress.of(0), DatasetAddressTemplate.of(1))));
    }

    @Test
    public void assertSubscriptionDependencyCandidate_allowsTemplateWithMissingTargetFilterParameter() {
        final var source = new Dataset(
                0, "Source", null, Dataset.FilterMode.UNFILTERED, null, false, Dataset.CacheType.NONE, true);
        final var target = new Dataset(
                1,
                "Target",
                1,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.FIXED,
                true,
                Dataset.CacheType.NONE,
                true);
        final var systemSchema = new SystemSchema("Test", source, target);

        InvariantUtil.assertSubscriptionDependencyCandidate(
                systemSchema,
                new SubscriptionDependencyCandidate(DatasetAddress.of(0), DatasetAddressTemplate.of(1, 7)));
    }
}
