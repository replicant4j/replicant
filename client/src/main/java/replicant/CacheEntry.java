package replicant;

import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Record of data stored in the local cache.
 */
public final class CacheEntry {
    @NonNull
    private final DatasetAddress _datasetAddress;

    @NonNull
    private final String _eTag;

    @NonNull
    private final String _content;

    public CacheEntry(
            @NonNull final DatasetAddress datasetAddress, @NonNull final String eTag, @NonNull final String content) {
        _datasetAddress = Objects.requireNonNull(datasetAddress);
        _eTag = Objects.requireNonNull(eTag);
        _content = Objects.requireNonNull(content);
    }

    @NonNull
    public DatasetAddress getDatasetAddress() {
        return _datasetAddress;
    }

    @NonNull
    public String getETag() {
        return _eTag;
    }

    @NonNull
    public String getContent() {
        return _content;
    }
}
