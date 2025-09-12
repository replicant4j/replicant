package replicant;

import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Service for caching raw data downloaded from the server.
 */
public interface CacheService
{
  @Nonnull
  Set<ChannelAddress> keySet( int schemaId );

  /**
   * Lookup etag for specified address.
   *
   * @param address the address.
   * @return the etag or null if not cached.
   */
  @Nullable
  String lookupEtag( @Nonnull ChannelAddress address );

  /**
   * Lookup cached content for the specified address.
   *
   * @param address the address.
   * @return the cached resource or null if not cached.
   */
  @Nullable
  CacheEntry lookup( @Nonnull ChannelAddress address );

  /**
   * Store content in cache.
   *
   * @param address the address under which to store resource.
   * @param eTag    the pseudo eTag for resource.
   * @param content the content of resource.
   * @return true if successfully cached, false otherwise.
   */
  boolean store( @Nonnull ChannelAddress address, @Nonnull String eTag, @Nonnull Object content );

  /**
   * Remove and invalidate cached resource.
   *
   * @param address the address.
   * @return if resource has been removed from cache, false if resource was not cached.
   */
  boolean invalidate( @Nonnull ChannelAddress address );
}
