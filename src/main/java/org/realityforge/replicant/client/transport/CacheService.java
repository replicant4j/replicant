package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;

/**
 * Service for caching raw data downloaded from the server.
 */
public interface CacheService
{
  /**
   * Lookup cached resource with specified key.
   *
   * @param key the key.
   * @return the cached resource or null if not cached.
   */
  CacheEntry lookup( @Nonnull String key );

  /**
   * Store resource in cache.
   *
   * @param key     the key under which to store resource.
   * @param eTag    the pseudo eTag for resource.
   * @param content the content of resource.
   * @return true if successfully cached, false otherwise.
   */
  boolean store( @Nonnull String key, @Nonnull String eTag, @Nonnull String content );

  /**
   * Remove and invalidate cached resource.
   *
   * @param key the key.
   * @return if resource has been removed from cache, false if resource was not cached.
   */
  boolean invalidate( @Nonnull String key );
}
