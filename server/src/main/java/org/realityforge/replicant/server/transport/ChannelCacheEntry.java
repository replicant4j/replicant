package org.realityforge.replicant.server.transport;

import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nonnull;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.ChannelAddress;

final class ChannelCacheEntry
{
  @Nonnull
  private final ReadWriteLock _lock = new ReentrantReadWriteLock();
  @Nonnull
  private final ChannelAddress _descriptor;
  private String _cacheKey;
  private ChangeSet _changeSet;

  ChannelCacheEntry( @Nonnull final ChannelAddress address )
  {
    _descriptor = Objects.requireNonNull( address );
  }

  @Nonnull
  ReadWriteLock getLock()
  {
    return _lock;
  }

  @Nonnull
  ChannelAddress getDescriptor()
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
  String getCacheKey()
  {
    return Objects.requireNonNull( _cacheKey );
  }

  @Nonnull
  ChangeSet getChangeSet()
  {
    return Objects.requireNonNull( _changeSet );
  }
}
