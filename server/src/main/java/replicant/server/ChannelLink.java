package replicant.server;

import javax.annotation.Nonnull;

/**
 * A representation indicating that an entity message will cause another channel to be subscribed.
 */
public record ChannelLink(@Nonnull ChannelAddress source, @Nonnull ChannelAddress target)
{
  @Nonnull
  @Override
  public String toString()
  {
    return "[" + source + "=>" + target + "]";
  }
}
