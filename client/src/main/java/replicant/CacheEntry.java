package replicant;

import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * A complete cached Dataset representation for one concrete Dataset Address.
 *
 * <p>The Dataset Cache Version is opaque and must only be compared for equality with the server's current version for
 * the same Dataset Address.
 */
public final class CacheEntry {
    @NonNull
    private final DatasetAddress _datasetAddress;

    @NonNull
    private final String _datasetCacheVersion;

    @NonNull
    private final String _content;

    /**
     * Create a cache entry.
     *
     * @param datasetAddress      the concrete Dataset Address that owns the representation.
     * @param datasetCacheVersion the opaque Dataset Cache Version supplied by the server.
     * @param content             the serialized complete Dataset representation.
     */
    public CacheEntry(
            @NonNull final DatasetAddress datasetAddress,
            @NonNull final String datasetCacheVersion,
            @NonNull final String content) {
        _datasetAddress = Objects.requireNonNull(datasetAddress);
        _datasetCacheVersion = Objects.requireNonNull(datasetCacheVersion);
        _content = Objects.requireNonNull(content);
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
     * Return the serialized complete Dataset representation.
     *
     * @return the serialized representation.
     */
    @NonNull
    public String getContent() {
        return _content;
    }
}
