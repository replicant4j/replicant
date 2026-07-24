package replicant.messages;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * A message fragment defining an action on a channel.
 */
@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
@SuppressWarnings( "NullAway.Init" )
public class ChannelChange
{
  private String channel;
  @Nullable
  private Object filter;

  /**
   * Create a ChannelChange.
   *
   * @return the new ChannelChange.
   */
  @JsOverlay
  public static ChannelChange create( @NonNull final String channelAction, @Nullable final Object filter )
  {
    final ChannelChange channel = new ChannelChange();
    channel.channel = channelAction;
    channel.filter = filter;
    return channel;
  }

  private ChannelChange()
  {
  }

  /**
   * Return the channel action description.
   *
   * @return the channel action description.
   */
  @JsOverlay
  @NonNull
  public final String getChannel()
  {
    return channel;
  }

  /**
   * @return the id of the Channel.
   */
  @Nullable
  @JsOverlay
  public final Object getFilter()
  {
    return filter;
  }
}
