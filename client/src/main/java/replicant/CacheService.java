package replicant;

import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Service for caching raw data downloaded from the server.
 */
public interface CacheService {
    @NonNull
    Set<ChannelAddress> keySet(int schemaId);

    /**
     * Lookup etag for specified address.
     *
     * @param address the address.
     * @return the etag or null if not cached.
     */
    @Nullable
    String lookupEtag(@NonNull ChannelAddress address);

    /**
     * Lookup cached content for the specified address.
     *
     * @param address the address.
     * @return the cached resource or null if not cached.
     */
    @Nullable
    CacheEntry lookup(@NonNull ChannelAddress address);

    /**
     * Store content in cache.
     *
     * @param address the address under which to store resource.
     * @param eTag    the pseudo eTag for resource.
     * @param content the content of resource.
     * @return true if successfully cached, false otherwise.
     */
    boolean store(@NonNull ChannelAddress address, @NonNull String eTag, @NonNull Object content);

    /**
     * Remove and invalidate cached resource.
     *
     * @param address the address.
     * @return if resource has been removed from cache, false if resource was not cached.
     */
    boolean invalidate(@NonNull ChannelAddress address);
}
