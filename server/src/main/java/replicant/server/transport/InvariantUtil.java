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
            assertDatasetAddressMatchesChannelMetaData(schema, datasetAddress);
        }
    }

    @VisibleForTesting
    static void assertDatasetAddressMatchesChannelMetaData(
            @NonNull final SchemaMetaData schema, @NonNull final DatasetAddress datasetAddress) {
        if (isInvariantCheckingEnabled()) {
            assertDatasetAddressMatchesChannelMetaData(
                    schema.getChannelMetaData(datasetAddress.datasetId()), datasetAddress);
        }
    }

    private static void assertDatasetAddressMatchesChannelMetaData(
            @NonNull final ChannelMetaData channel, @NonNull final DatasetAddress datasetAddress) {
        if (channel.isTypeGraph()) {
            assert !datasetAddress.hasDatasetRootId();
        } else {
            assert datasetAddress.hasDatasetRootId();
        }

        if (datasetAddress.partial()) {
            assert channel.requiresDatasetKey();
            assert null == datasetAddress.datasetKey();
        } else if (channel.requiresDatasetKey()) {
            assert null != datasetAddress.datasetKey();
        } else {
            assert null == datasetAddress.datasetKey();
        }
    }

    static void assertLink(@NonNull final SchemaMetaData schema, @NonNull final ChannelLink link) {
        if (isInvariantCheckingEnabled()) {
            assertDatasetAddressMatchesChannelMetaData(schema, link.sourceDatasetAddress());
            final var targetChannel =
                    schema.getChannelMetaData(link.targetDatasetAddress().datasetId());
            assertDatasetAddressMatchesChannelMetaData(targetChannel, link.targetDatasetAddress());

            if (link.partial()) {
                assert link.sourceDatasetAddress().partial()
                        || link.targetDatasetAddress().partial()
                        || (targetChannel.requiresFilterParameter() && null == link.targetFilter());
            } else {
                assert link.sourceDatasetAddress().concrete();
                assert link.targetDatasetAddress().concrete();
                assert !targetChannel.requiresFilterParameter() || null != link.targetFilter();
            }
        }
    }
}
