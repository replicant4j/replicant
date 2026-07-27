package replicant.server;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A partially specified Dataset selection used while evaluating a Dataset Link.
 */
public record DatasetAddressTemplate(
        int datasetId, @Nullable Integer datasetRootId) implements DatasetAddressCandidate {
    @NonNull
    public static DatasetAddressTemplate of(final int datasetId) {
        return new DatasetAddressTemplate(datasetId, null);
    }

    @NonNull
    public static DatasetAddressTemplate of(final int datasetId, @Nullable final Integer datasetRootId) {
        return new DatasetAddressTemplate(datasetId, datasetRootId);
    }

    @Override
    @Nullable
    public String datasetKey() {
        return null;
    }

    public boolean matches(@NonNull final DatasetAddress datasetAddress) {
        return datasetId == datasetAddress.datasetId()
                && java.util.Objects.equals(datasetRootId, datasetAddress.datasetRootId());
    }

    @NonNull
    @Override
    public String toString() {
        return datasetId + (null == datasetRootId ? "" : "." + datasetRootId) + "?";
    }
}
