package replicant.server;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A representation indicating that an entity message will cause another channel to be subscribed.
 */
public record ChannelLink(@Nonnull ChannelAddress source, @Nonnull ChannelAddress target, @Nullable Object targetFilter,
                          boolean partial)
{
  public ChannelLink( @Nonnull final ChannelAddress source, @Nonnull final ChannelAddress target )
  {
    this( source, target, null );
  }

  public ChannelLink( @Nonnull final ChannelAddress source,
                      @Nonnull final ChannelAddress target,
                      @Nullable final Object targetFilter )
  {
    this( source, target, targetFilter, false );
  }

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
