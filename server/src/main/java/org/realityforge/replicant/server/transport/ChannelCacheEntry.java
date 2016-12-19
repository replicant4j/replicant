package org.realityforge.replicant.server.transport;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nonnull;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.ChannelDescriptor;

public final class ChannelCacheEntry
{
  private final ReadWriteLock _lock = new ReentrantReadWriteLock();
  private final ChannelDescriptor _descriptor;
  private String _cacheKey;
  private ChangeSet _changeSet;

  ChannelCacheEntry( @Nonnull final ChannelDescriptor descriptor )
  {
    _descriptor = descriptor;
  }

  @Nonnull
  public ReadWriteLock getLock()
  {
    return _lock;
  }

  @Nonnull
  public ChannelDescriptor getDescriptor()
  {
    return _descriptor;
  }

  void init( @Nonnull final String cacheKey, @Nonnull final ChangeSet changeSet )
  {
    _cacheKey = cacheKey;
    _changeSet = changeSet;
  }

  boolean isInitialized()
  {
    return null != _cacheKey;
  }

  @Nonnull
  public String getCacheKey()
  {
    if ( null == _cacheKey )
    {
      throw new NullPointerException( "cacheKey" );
    }
    return _cacheKey;
  }

  @Nonnull
  public ChangeSet getChangeSet()
  {
    if ( null == _changeSet )
    {
      throw new NullPointerException( "changeSet" );
    }
    return _changeSet;
  }
}
