package org.realityforge.replicant.client.ee;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.transport.CacheEntry;
import org.realityforge.replicant.client.transport.CacheService;

/**
 * An implementation of the CacheService that uses an in-memory map for caching.
 */
public class InMemoryCacheService
  implements CacheService
{
  private Map<String, CacheEntry> _cache = new HashMap<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public CacheEntry lookup( @Nonnull final String key )
  {
    return _cache.get( key );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean store( @Nonnull final String key, @Nonnull final String eTag, @Nonnull final String content )
  {
    _cache.put( key, new CacheEntry( key, eTag, content ) );
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean invalidate( @Nonnull final String key )
  {
    return null != _cache.remove( key );
  }
}
