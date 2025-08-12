package replicant;

import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Record of data stored in the local cache.
 */
public final class CacheEntry
{
  @Nonnull
  private final ChannelAddress _address;
  @Nonnull
  private final String _eTag;
  @Nonnull
  private final String _content;

  public CacheEntry( @Nonnull final ChannelAddress address, @Nonnull final String eTag, @Nonnull final String content )
  {
    _address = Objects.requireNonNull( address );
    _eTag = Objects.requireNonNull( eTag );
    _content = Objects.requireNonNull( content );
  }

  @Nonnull
  public ChannelAddress getAddress()
  {
    return _address;
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
