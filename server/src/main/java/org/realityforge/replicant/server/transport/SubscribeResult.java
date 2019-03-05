package org.realityforge.replicant.server.transport;

import javax.annotation.Nullable;

public final class SubscribeResult
{
  private final boolean _channelRootDeleted;
  @Nullable
  private final String _cacheKey;

  public SubscribeResult( final boolean channelRootDeleted, @Nullable final String cacheKey )
  {
    assert null == cacheKey || !channelRootDeleted;
    _channelRootDeleted = channelRootDeleted;
    _cacheKey = cacheKey;
  }

  public boolean isChannelRootDeleted()
  {
    return _channelRootDeleted;
  }

  @Nullable
  public String getCacheKey()
  {
    return _cacheKey;
  }
}
