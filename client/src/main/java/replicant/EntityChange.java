package replicant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * A change to an entity.
 */
@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
public class EntityChange
{
  private int id;
  private int type;
  private EntityChannel[] channels;
  private EntityChangeData data;

  /**
   * Create a "remove" EntityChange message.
   *
   * @return the new EntityChange.
   */
  @JsOverlay
  public static EntityChange create( final int id, final int type, @Nonnull final EntityChannel[] channels )
  {
    final EntityChange change = new EntityChange();
    change.id = id;
    change.type = type;
    change.channels = channels;
    return change;
  }

  /**
   * Create an "update" EntityChange message.
   *
   * @return the new EntityChange.
   */
  @JsOverlay
  public static EntityChange create( final int id,
                                     final int type,
                                     @Nonnull final EntityChannel[] channels,
                                     @Nullable final EntityChangeData data )
  {
    final EntityChange change = new EntityChange();
    change.id = id;
    change.type = type;
    change.channels = channels;
    change.data = data;
    return change;
  }

  /**
   * @return the id of the entity.
   */
  @JsOverlay
  public final int getId()
  {
    return id;
  }

  /**
   * @return a code indicating the type of the entity changed.
   */
  @JsOverlay
  public final int getTypeId()
  {
    return type;
  }

  /**
   * Return the channels that the entity is associated with.
   *
   * @return the channels that the entity is associated with.
   */
  @JsOverlay
  public final EntityChannel[] getChannels()
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
