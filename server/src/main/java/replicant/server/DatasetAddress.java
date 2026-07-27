package replicant.server;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The immutable identity of a subscribable Dataset selection.
 *
 * @param datasetId the Dataset identifier
 * @param datasetRootId the Dataset Root identifier, when required
 * @param datasetKey the Dataset Key, when required
 */
public record DatasetAddress(
        int datasetId,
        @Nullable Integer datasetRootId,
        @Nullable String datasetKey) implements DatasetAddressCandidate, Comparable<DatasetAddress> {
    @NonNull
    public static DatasetAddress parse(@NonNull final String datasetAddressDescriptor) {
        final var datasetKeyOffset = datasetAddressDescriptor.indexOf('#');
        final var datasetAddressPart = -1 == datasetKeyOffset
                ? datasetAddressDescriptor
                : datasetAddressDescriptor.substring(0, datasetKeyOffset);
        final var datasetKey = -1 == datasetKeyOffset ? null : datasetAddressDescriptor.substring(datasetKeyOffset + 1);
        final var offset = datasetAddressPart.indexOf(".");
        final var datasetId =
                Integer.parseInt(-1 == offset ? datasetAddressPart : datasetAddressPart.substring(0, offset));
        final var datasetRootId = -1 == offset ? null : Integer.parseInt(datasetAddressPart.substring(offset + 1));
        return new DatasetAddress(datasetId, datasetRootId, datasetKey);
    }

    @NonNull
    public static DatasetAddress of(final int datasetId) {
        return new DatasetAddress(datasetId, null, null);
    }

    @NonNull
    public static DatasetAddress of(final int datasetId, @Nullable final Integer datasetRootId) {
        return new DatasetAddress(datasetId, datasetRootId, null);
    }

    @NonNull
    public static DatasetAddress of(
            final int datasetId, @Nullable final Integer datasetRootId, @Nullable final String datasetKey) {
        return new DatasetAddress(datasetId, datasetRootId, datasetKey);
    }

    public boolean hasDatasetRootId() {
        return null != datasetRootId;
    }

    @Override
    public int compareTo(@NonNull final DatasetAddress other) {
        final var datasetDiff = Integer.compare(datasetId(), other.datasetId());
        if (0 != datasetDiff) {
            return datasetDiff;
        } else {
            final var otherDatasetRootId = other.datasetRootId();
            final var datasetRootId = datasetRootId();
            if (null != otherDatasetRootId || null != datasetRootId) {
                if (null == otherDatasetRootId) {
                    return -1;
                } else if (null == datasetRootId) {
                    return 1;
                } else {
                    final var rootDiff = datasetRootId.compareTo(otherDatasetRootId);
                    if (0 != rootDiff) {
                        return rootDiff;
                    }
                }
            }
        }
        final var datasetKey = datasetKey();
        final var otherDatasetKey = other.datasetKey();
        if (null == datasetKey && null == otherDatasetKey) {
            return 0;
        } else if (null == datasetKey) {
            return -1;
        } else if (null == otherDatasetKey) {
            return 1;
        } else {
            return datasetKey.compareTo(otherDatasetKey);
        }
    }

    @NonNull
    @Override
    public String toString() {
        final var base = datasetId + (null == datasetRootId ? "" : "." + datasetRootId);
        return base + (null == datasetKey ? "" : "#" + datasetKey);
    }
}
