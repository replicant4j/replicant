package replicant;

import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.messages.ChangeSetMessage;

/**
 * Optional client service for storing Dataset Cache Entries by Dataset Address.
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
     * Store a Change Set as a Dataset Cache Entry.
     *
     * @param datasetAddress      the concrete Dataset Address owning the representation.
     * @param datasetCacheVersion the opaque Dataset Cache Version supplied by the server.
     * @param changeSet           the Change Set to store.
     * @return true if the Change Set was stored, false otherwise.
     */
    boolean store(
            @NonNull DatasetAddress datasetAddress,
            @NonNull String datasetCacheVersion,
            @NonNull ChangeSetMessage changeSet);

    /**
     * Remove the cached representation for the specified Dataset Address.
     *
     * @param datasetAddress the Dataset Address.
     * @return true if an entry was removed, false if no entry was stored.
     */
    boolean invalidate(@NonNull DatasetAddress datasetAddress);
}
