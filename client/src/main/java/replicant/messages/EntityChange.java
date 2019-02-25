package replicant.messages;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * A change to an entity.
 */
@SuppressFBWarnings( "EI_EXPOSE_REP" )
@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
public class EntityChange
{
  private String id;
  private String[] channels;
  private EntityChangeData data;

  /**
   * Create a "remove" EntityChange message.
   *
   * @return the new EntityChange.
   */
  @JsOverlay
  public static EntityChange create( final int type, final int id, @Nonnull final String[] channels )
  {
    final EntityChange change = new EntityChange();
    change.id = type + "." + id;
    change.channels = channels;
    return change;
  }

  /**
   * Create an "update" EntityChange message.
   *
   * @return the new EntityChange.
   */
  @JsOverlay
  public static EntityChange create( final int type,
                                     final int id,
                                     @Nonnull final String[] channels,
                                     @Nullable final EntityChangeData data )
  {
    final EntityChange change = new EntityChange();
    change.id = type + "." + id;
    change.channels = channels;
    change.data = data;
    return change;
  }

  private EntityChange()
  {
  }

  /**
   * @return the id of the entity.
   */
  @JsOverlay
  public final String getId()
  {
    return id;
  }

  /**
   * Return the channels that the entity is associated with.
   *
   * @return the channels that the entity is associated with.
   */
  @JsOverlay
  public final String[] getChannels()
  {
    return channels;
  }

  /**
   * @return true if the change is an update, false if it is a remove.
   */
  @JsOverlay
  public final boolean isUpdate()
  {
    return null != data;
  }

  /**
   * @return true if the change is a remove, false if it is an update.
   */
  @JsOverlay
  public final boolean isRemove()
  {
    return !isUpdate();
  }

  /**
   * Return data to update.
   *
   * @return true if the data is present.
   */
  @Nonnull
  @JsOverlay
  public final EntityChangeData getData()
  {
    assert null != data;
    return data;
  }
}
