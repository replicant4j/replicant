package replicant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * An action on a channel.
 */
@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
public class ChannelAction
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
   * Create a ChannelAction.
   *
   * @return the new ChannelAction.
   */
  @JsOverlay
  public static ChannelAction create( final int cid,
                                      final int scid,
                                      @Nonnull final Action action,
                                      @Nullable final Object filter )
  {
    final ChannelAction channel = new ChannelAction();
    channel.cid = cid;
    channel.scid = (double) scid;
    channel.action = action.name().toLowerCase();
    channel.filter = filter;
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
