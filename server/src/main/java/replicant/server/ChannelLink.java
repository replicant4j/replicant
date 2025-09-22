package replicant.server;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A representation indicating that an entity message will cause another channel to be subscribed.
 */
public record ChannelLink(@Nonnull ChannelAddress source, @Nonnull ChannelAddress target, @Nullable Object targetFilter)
{
  public ChannelLink( @Nonnull final ChannelAddress source, @Nonnull final ChannelAddress target )
  {
    this( source, target, null );
  }

  @Nonnull
  @Override
  public String toString()
  {
    return "[" + source + "=>" + target + ( null == targetFilter ? "" : ( "~<" + targetFilter + ">" ) ) + "]";
  }
}
