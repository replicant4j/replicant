package replicant.messages;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * A message fragment defining an action on a channel.
 */
@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
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
  public static ChannelChange create( @Nonnull final String channelAction, @Nullable final Object filter )
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
  @Nonnull
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
