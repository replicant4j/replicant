package replicant.server;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import javax.json.JsonObject;

/**
 * A representation indicating that an entity message will cause another channel to be subscribed.
 */
public record ChannelLink(@NonNull ChannelAddress source, @NonNull ChannelAddress target,
                          @Nullable JsonObject targetFilter,
                          boolean partial)
{
  public ChannelLink( @NonNull final ChannelAddress source, @NonNull final ChannelAddress target )
  {
    this( source, target, null );
  }

  public ChannelLink( @NonNull final ChannelAddress source,
                      @NonNull final ChannelAddress target,
                      @Nullable final JsonObject targetFilter )
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

  @NonNull
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
