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
            @NonNull final SystemSchema systemSchema, @NonNull final DatasetAddress datasetAddress) {
        if (isInvariantCheckingEnabled()) {
            assertDatasetAddressMatchesDataset(systemSchema, datasetAddress);
        }
    }

    @VisibleForTesting
    static void assertDatasetAddressMatchesDataset(
            @NonNull final SystemSchema systemSchema, @NonNull final DatasetAddress datasetAddress) {
        if (isInvariantCheckingEnabled()) {
            assertDatasetAddressMatchesDataset(systemSchema.getDataset(datasetAddress.datasetId()), datasetAddress);
        }
    }

    private static void assertDatasetAddressMatchesDataset(
            @NonNull final Dataset dataset, @NonNull final DatasetAddressCandidate datasetAddressCandidate) {
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
            @NonNull final SystemSchema systemSchema,
            @NonNull final SubscriptionDependencyCandidate subscriptionDependencyCandidate) {
        if (isInvariantCheckingEnabled()) {
            final var sourceDatasetAddressCandidate = subscriptionDependencyCandidate.sourceDatasetAddressCandidate();
            assertDatasetAddressMatchesDataset(
                    systemSchema.getDataset(sourceDatasetAddressCandidate.datasetId()), sourceDatasetAddressCandidate);
            final var targetDatasetAddressCandidate = subscriptionDependencyCandidate.targetDatasetAddressCandidate();
            assertDatasetAddressMatchesDataset(
                    systemSchema.getDataset(targetDatasetAddressCandidate.datasetId()), targetDatasetAddressCandidate);
        }
    }
}
