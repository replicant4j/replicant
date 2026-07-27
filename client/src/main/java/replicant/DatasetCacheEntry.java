package replicant;

import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * A Dataset Cache Entry containing the serialized Change Set for one concrete Dataset Address.
 *
 * <p>The Dataset Cache Version is opaque and must only be compared for equality with the server's current version for
 * the same Dataset Address.
 */
public final class DatasetCacheEntry {
    @NonNull
    private final DatasetAddress _datasetAddress;

    @NonNull
    private final String _datasetCacheVersion;

    @NonNull
    private final String _changeSet;

    /**
     * Create a Dataset Cache Entry.
     *
     * @param datasetAddress      the concrete Dataset Address that owns the representation.
     * @param datasetCacheVersion the opaque Dataset Cache Version supplied by the server.
     * @param changeSet           the serialized Change Set.
     */
    public DatasetCacheEntry(
            @NonNull final DatasetAddress datasetAddress,
            @NonNull final String datasetCacheVersion,
            @NonNull final String changeSet) {
        _datasetAddress = Objects.requireNonNull(datasetAddress);
        _datasetCacheVersion = Objects.requireNonNull(datasetCacheVersion);
        _changeSet = Objects.requireNonNull(changeSet);
    }

    /**
     * Return the concrete Dataset Address that owns this entry.
     *
     * @return the Dataset Address.
     */
    @NonNull
    public DatasetAddress getDatasetAddress() {
        return _datasetAddress;
    }

    /**
     * Return the opaque Dataset Cache Version.
     *
     * @return the Dataset Cache Version.
     */
    @NonNull
    public String getDatasetCacheVersion() {
        return _datasetCacheVersion;
    }

    /**
     * Return the serialized Change Set.
     *
     * @return the serialized Change Set.
     */
    @NonNull
    public String getChangeSet() {
        return _changeSet;
    }
}
