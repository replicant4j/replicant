package replicant;

import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Service for caching raw data downloaded from the server.
 */
public interface CacheService {
    @NonNull
    Set<DatasetAddress> keySet(int schemaId);

    /**
     * Lookup etag for specified Dataset Address.
     *
     * @param datasetAddress the Dataset Address.
     * @return the etag or null if not cached.
     */
    @Nullable
    String lookupEtag(@NonNull DatasetAddress datasetAddress);

    /**
     * Lookup cached content for the specified Dataset Address.
     *
     * @param datasetAddress the Dataset Address.
     * @return the cached resource or null if not cached.
     */
    @Nullable
    CacheEntry lookup(@NonNull DatasetAddress datasetAddress);

    /**
     * Store content in cache.
     *
     * @param datasetAddress the Dataset Address under which to store resource.
     * @param eTag    the pseudo eTag for resource.
     * @param content the content of resource.
     * @return true if successfully cached, false otherwise.
     */
    boolean store(@NonNull DatasetAddress datasetAddress, @NonNull String eTag, @NonNull Object content);

    /**
     * Remove and invalidate cached resource.
     *
     * @param datasetAddress the Dataset Address.
     * @return if resource has been removed from cache, false if resource was not cached.
     */
    boolean invalidate(@NonNull DatasetAddress datasetAddress);
}
