package replicant.server;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A representation indicating that an entity message will cause another channel to be subscribed.
 */
public record ChannelLink(@Nonnull ChannelAddress source, @Nonnull ChannelAddress target, @Nullable Object targetFilter,
                          boolean partial)
{
  public ChannelLink
  {
    assert partial || ( !source.partial() && !target.partial() );
    assert !partial || source.partial() || target.partial() || null == targetFilter;
  }

  public boolean hasTargetFilter()
  {
    return null != targetFilter;
  }

  @Nonnull
  @Override
  public String toString()
  {
    return "[" +
           source + "=>" + target +
           ( hasTargetFilter() ? ( "~<" + targetFilter + ">" ) : "" ) +
           ( partial ? "?" : "" ) +
           "]";
  }
}
