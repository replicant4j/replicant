package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;

/**
 * Record of data stored in the local cache.
 */
public class CacheEntry
{
  private final String _key;
  private final String _eTag;
  private final String _content;

  public CacheEntry( @Nonnull final String key, @Nonnull final String eTag, @Nonnull final String content )
  {
    _key = key;
    _eTag = eTag;
    _content = content;
  }

  @Nonnull
  public String getKey()
  {
    return _key;
  }

  @Nonnull
  public String getETag()
  {
    return _eTag;
  }

  @Nonnull
  public String getContent()
  {
    return _content;
  }
}
