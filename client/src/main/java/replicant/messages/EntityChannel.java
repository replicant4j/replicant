package replicant.messages;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import replicant.ChannelAddress;

/**
 * A reference to a channel.
 */
@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
public class EntityChannel
{
  private int cid;
  @Nullable
  private Double scid;

  /**
   * Create an EntityChannel with no sub-channel.
   *
   * @return the new EntityChannel.
   */
  @JsOverlay
  public static EntityChannel create( final int cid )
  {
    final EntityChannel channel = new EntityChannel();
    channel.cid = cid;
    return channel;
  }

  /**
   * Create an EntityChannel with a sub-channel.
   *
   * @return the new EntityChannel.
   */
  @JsOverlay
  public static EntityChannel create( final int cid, final int scid )
  {
    final EntityChannel channel = new EntityChannel();
    channel.cid = cid;
    channel.scid = (double) scid;
    return channel;
  }

  private EntityChannel()
  {
  }

  /**
   * @return the id of the Channel.
   */
  @JsOverlay
  public final int getId()
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
  public final ChannelAddress toAddress( final int schemaId )
  {
    final Integer scid = hasSubChannelId() ? getSubChannelId() : null;
    return new ChannelAddress( schemaId, getId(), scid );
  }
}
