package replicant;

import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Optional client service for storing complete Cacheable Dataset representations by Dataset Address.
 *
 * <p>Each stored representation is identified by an opaque Dataset Cache Version supplied by the server. Callers
 * compare Dataset Cache Versions only for equality and treat absent, unreadable, corrupt, or mismatched entries as
 * recoverable cache misses.
 */
public interface CacheService {
    /**
     * Return the Dataset Addresses with locally stored entries for the System Schema.
     *
     * @param schemaId the System Schema identifier.
     * @return the Dataset Addresses with stored entries.
     */
    @NonNull
    Set<DatasetAddress> keySet(int schemaId);

    /**
     * Return the Dataset Cache Version for the specified Dataset Address.
     *
     * @param datasetAddress the Dataset Address.
     * @return the opaque Dataset Cache Version, or null if no readable version is stored.
     */
    @Nullable
    String lookupDatasetCacheVersion(@NonNull DatasetAddress datasetAddress);

    /**
     * Return the complete cached representation for the specified Dataset Address.
     *
     * @param datasetAddress the Dataset Address.
     * @return the cache entry, or null if no readable entry is stored.
     */
    @Nullable
    CacheEntry lookup(@NonNull DatasetAddress datasetAddress);

    /**
     * Store a complete Dataset representation.
     *
     * @param datasetAddress      the concrete Dataset Address owning the representation.
     * @param datasetCacheVersion the opaque Dataset Cache Version supplied by the server.
     * @param content             the complete representation.
     * @return true if the representation was stored, false otherwise.
     */
    boolean store(@NonNull DatasetAddress datasetAddress, @NonNull String datasetCacheVersion, @NonNull Object content);

    /**
     * Remove the cached representation for the specified Dataset Address.
     *
     * @param datasetAddress the Dataset Address.
     * @return true if an entry was removed, false if no entry was stored.
     */
    boolean invalidate(@NonNull DatasetAddress datasetAddress);
}
