package replicant.server.transport;

import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.NonNull;
import replicant.server.ChannelLink;
import replicant.server.DatasetAddress;

final class InvariantUtil {
    private static final boolean ASSERTIONS_ENABLED = InvariantUtil.class.desiredAssertionStatus();

    private InvariantUtil() {}

    static boolean isInvariantCheckingEnabled() {
        return ASSERTIONS_ENABLED;
    }

    static void assertConcreteDatasetAddress(@NonNull final DatasetAddress datasetAddress) {
        assert datasetAddress.concrete();
    }

    static void assertConcreteDatasetAddress(
            @NonNull final SchemaMetaData schema, @NonNull final DatasetAddress datasetAddress) {
        if (isInvariantCheckingEnabled()) {
            assertConcreteDatasetAddress(datasetAddress);
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
            @NonNull final DatasetMetadata dataset, @NonNull final DatasetAddress datasetAddress) {
        if (dataset.isTypeGraph()) {
            assert !datasetAddress.hasDatasetRootId();
        } else {
            assert datasetAddress.hasDatasetRootId();
        }

        if (datasetAddress.partial()) {
            assert dataset.requiresDatasetKey();
            assert null == datasetAddress.datasetKey();
        } else if (dataset.requiresDatasetKey()) {
            assert null != datasetAddress.datasetKey();
        } else {
            assert null == datasetAddress.datasetKey();
        }
    }

    static void assertLink(@NonNull final SchemaMetaData schema, @NonNull final ChannelLink link) {
        if (isInvariantCheckingEnabled()) {
            assertDatasetAddressMatchesDatasetMetadata(schema, link.sourceDatasetAddress());
            final var targetDataset =
                    schema.getDatasetMetadata(link.targetDatasetAddress().datasetId());
            assertDatasetAddressMatchesDatasetMetadata(targetDataset, link.targetDatasetAddress());

            if (link.partial()) {
                assert link.sourceDatasetAddress().partial()
                        || link.targetDatasetAddress().partial()
                        || (targetDataset.requiresFilterParameter() && null == link.targetFilter());
            } else {
                assert link.sourceDatasetAddress().concrete();
                assert link.targetDatasetAddress().concrete();
                assert !targetDataset.requiresFilterParameter() || null != link.targetFilter();
            }
        }
    }
}
