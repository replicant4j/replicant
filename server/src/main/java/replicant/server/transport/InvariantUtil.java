package replicant.server.transport;

import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.NonNull;
import replicant.server.DatasetAddress;
import replicant.server.DatasetAddressCandidate;
import replicant.server.DatasetAddressTemplate;
import replicant.server.SubscriptionDependencyCandidate;

final class InvariantUtil {
    private static final boolean ASSERTIONS_ENABLED = InvariantUtil.class.desiredAssertionStatus();

    private InvariantUtil() {}

    static boolean isInvariantCheckingEnabled() {
        return ASSERTIONS_ENABLED;
    }

    static void assertConcreteDatasetAddress(
            @NonNull final SchemaMetaData schema, @NonNull final DatasetAddress datasetAddress) {
        if (isInvariantCheckingEnabled()) {
            assertDatasetAddressMatchesDatasetMetadata(schema, datasetAddress);
        }
    }

    @VisibleForTesting
    static void assertDatasetAddressMatchesDatasetMetadata(
            @NonNull final SchemaMetaData schema, @NonNull final DatasetAddress datasetAddress) {
        if (isInvariantCheckingEnabled()) {
            assertDatasetAddressMatchesDatasetMetadata(
                    schema.getDatasetMetadata(datasetAddress.datasetId()), datasetAddress);
        }
    }

    private static void assertDatasetAddressMatchesDatasetMetadata(
            @NonNull final DatasetMetadata dataset, @NonNull final DatasetAddressCandidate datasetAddressCandidate) {
        if (dataset.isTypeDataset()) {
            assert null == datasetAddressCandidate.datasetRootId();
        } else {
            assert null != datasetAddressCandidate.datasetRootId();
        }

        if (datasetAddressCandidate instanceof DatasetAddressTemplate) {
            assert dataset.isKeyed();
            assert null == datasetAddressCandidate.datasetKey();
        } else if (dataset.isKeyed()) {
            assert null != datasetAddressCandidate.datasetKey();
        } else {
            assert null == datasetAddressCandidate.datasetKey();
        }
    }

    static void assertSubscriptionDependencyCandidate(
            @NonNull final SchemaMetaData schema,
            @NonNull final SubscriptionDependencyCandidate subscriptionDependencyCandidate) {
        if (isInvariantCheckingEnabled()) {
            final var sourceDatasetAddressCandidate = subscriptionDependencyCandidate.sourceDatasetAddressCandidate();
            assertDatasetAddressMatchesDatasetMetadata(
                    schema.getDatasetMetadata(sourceDatasetAddressCandidate.datasetId()),
                    sourceDatasetAddressCandidate);
            final var targetDatasetAddressCandidate = subscriptionDependencyCandidate.targetDatasetAddressCandidate();
            assertDatasetAddressMatchesDatasetMetadata(
                    schema.getDatasetMetadata(targetDatasetAddressCandidate.datasetId()),
                    targetDatasetAddressCandidate);
        }
    }
}
