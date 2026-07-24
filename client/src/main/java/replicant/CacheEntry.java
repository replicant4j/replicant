package replicant;

import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Record of data stored in the local cache.
 */
public final class CacheEntry
{
  @NonNull
  private final ChannelAddress _address;
  @NonNull
  private final String _eTag;
  @NonNull
  private final String _content;

  public CacheEntry( @NonNull final ChannelAddress address, @NonNull final String eTag, @NonNull final String content )
  {
    _address = Objects.requireNonNull( address );
    _eTag = Objects.requireNonNull( eTag );
    _content = Objects.requireNonNull( content );
  }

  @NonNull
  public ChannelAddress getAddress()
  {
    return _address;
  }

  @NonNull
  public String getETag()
  {
    return _eTag;
  }

  @NonNull
  public String getContent()
  {
    return _content;
  }
}
