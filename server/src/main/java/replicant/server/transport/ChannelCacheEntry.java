package replicant.server.transport;

import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.server.ChangeSet;
import replicant.server.ChannelAddress;

final class ChannelCacheEntry
{
  @NonNull
  private final ReadWriteLock _lock = new ReentrantReadWriteLock();
  @NonNull
  private final ChannelAddress _descriptor;
  @Nullable
  private String _cacheKey;
  @Nullable
  private ChangeSet _changeSet;

  ChannelCacheEntry( @NonNull final ChannelAddress address )
  {
    assert address.concrete();
    _descriptor = Objects.requireNonNull( address );
  }

  @NonNull
  ReadWriteLock getLock()
  {
    return _lock;
  }

  @NonNull
  ChannelAddress getDescriptor()
  {
    return _descriptor;
  }

  void init( @NonNull final String cacheKey, @NonNull final ChangeSet changeSet )
  {
    _cacheKey = Objects.requireNonNull( cacheKey );
    _changeSet = Objects.requireNonNull( changeSet );
  }

  boolean isInitialized()
  {
    return null != _cacheKey;
  }

  @NonNull
  String getCacheKey()
  {
    return Objects.requireNonNull( _cacheKey );
  }

  @NonNull
  ChangeSet getChangeSet()
  {
    return Objects.requireNonNull( _changeSet );
  }
}
