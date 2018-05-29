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
  public enum Action
  {
    ADD, REMOVE, UPDATE
  }

  private int cid;
  @Nullable
  private Double scid;
  private String action;
  @Nullable
  private Object filter;

  /**
   * Create a ChannelChange.
   *
   * @return the new ChannelChange.
   */
  @JsOverlay
  public static ChannelChange create( final int cid,
                                      @Nonnull final Action action,
                                      @Nullable final Object filter )
  {
    final ChannelChange channel = new ChannelChange();
    channel.cid = cid;
    channel.action = action.name().toLowerCase();
    channel.filter = filter;
    return channel;
  }

  private ChannelChange()
  {
  }

  /**
   * Create a ChannelChange.
   *
   * @return the new ChannelChange.
   */
  @JsOverlay
  public static ChannelChange create( final int cid,
                                      final int scid,
                                      @Nonnull final Action action,
                                      @Nullable final Object filter )
  {
    assert Action.REMOVE != action || null == filter;
    final ChannelChange channel = create( cid, action, filter );
    channel.scid = (double) scid;
    return channel;
  }

  /**
   * @return the id of the Channel.
   */
  @JsOverlay
  public final int getChannelId()
  {
    return cid;
  }

  /**
   * @return true if the reference has a SubChannel id present.
   */
  @JsOverlay
  public final boolean hasSubChannelId()
  {
    return null != scid;
  }

  /**
   * Return the sub-channel id.
   * This method should not be invoked unless {@link #hasSubChannelId()} returns true.
   *
   * @return the sub-channel id.
   */
  @JsOverlay
  public final int getSubChannelId()
  {
    assert null != scid;
    return scid.intValue();
  }

  @JsOverlay
  @Nonnull
  public final Action getAction()
  {
    return Action.valueOf( action.toUpperCase() );
  }

  @Nullable
  @JsOverlay
  public final Object getChannelFilter()
  {
    return filter;
  }
}
