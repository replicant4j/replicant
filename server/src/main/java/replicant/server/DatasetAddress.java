package replicant.server;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The immutable identity of a subscribable Dataset selection.
 *
 * @param datasetId the Dataset identifier
 * @param datasetRootId the Dataset Root identifier, when required
 * @param datasetKey the Dataset Key, when required
 * @param partial true when the address is a non-concrete link template
 */
public record DatasetAddress(
        int datasetId,
        @Nullable Integer datasetRootId,
        @Nullable String datasetKey,
        boolean partial) implements Comparable<DatasetAddress> {
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
        return new DatasetAddress(datasetId, datasetRootId, datasetKey, false);
    }

    @NonNull
    public static DatasetAddress partial(final int datasetId) {
        return new DatasetAddress(datasetId, null, null, true);
    }

    @NonNull
    public static DatasetAddress partial(final int datasetId, @Nullable final Integer datasetRootId) {
        return new DatasetAddress(datasetId, datasetRootId, null, true);
    }

    @NonNull
    public static DatasetAddress of(final int datasetId) {
        return new DatasetAddress(datasetId, null, null, false);
    }

    @NonNull
    public static DatasetAddress of(final int datasetId, @Nullable final Integer datasetRootId) {
        return new DatasetAddress(datasetId, datasetRootId, null, false);
    }

    @NonNull
    public static DatasetAddress of(
            final int datasetId, @Nullable final Integer datasetRootId, @Nullable final String datasetKey) {
        return new DatasetAddress(datasetId, datasetRootId, datasetKey, false);
    }

    public DatasetAddress {
        assert !partial || null == datasetKey;
    }

    public boolean concrete() {
        return !partial;
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
        final var f1 = datasetKey();
        final var f2 = other.datasetKey();
        if (null == f1 && null == f2) {
            if (partial() == other.partial()) {
                return 0;
            } else {
                return partial() ? 1 : -1;
            }
        } else if (null == f1) {
            return -1;
        } else if (null == f2) {
            return 1;
        } else if (!f1.equals(f2)) {
            return f1.compareTo(f2);
        } else if (partial() == other.partial()) {
            return 0;
        } else {
            return partial() ? 1 : -1;
        }
    }

    @NonNull
    @Override
    public String toString() {
        final var base = datasetId + (null == datasetRootId ? "" : "." + datasetRootId);
        return base + (null == datasetKey ? "" : "#" + datasetKey) + (partial ? "?" : "");
    }
}
