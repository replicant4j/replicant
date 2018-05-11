package replicant;

import elemental2.core.JsDate;
import java.util.Date;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import jsinterop.base.JsPropertyMap;

/**
 * A change to an entity.
 */
@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
public class EntityChange
{
  private int id;
  private int type;
  private EntityChannel[] channels;
  private JsPropertyMap<Object> data;

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
                                     @Nullable final JsPropertyMap<Object> data )
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
   * Return true if data for the attribute identified by the key is present in the change.
   *
   * @param key the attribute key.
   * @return true if the data is present.
   */
  @JsOverlay
  public final boolean containsKey( @Nonnull final String key )
  {
    assert null != data;
    return data.has( key );
  }

  /**
   * Return true if data for the attribute identified by the key is null.
   *
   * @param key the attribute key.
   * @return true if the data is null.
   */
  @JsOverlay
  public final boolean isNull( @Nonnull final String key )
  {
    assert null != data;
    return null == data.get( key );
  }

  @JsOverlay
  public final int getIntegerValue( @Nonnull final String key )
  {
    assert null != data;
    return data.getAny( key ).asInt();
  }

  @Nonnull
  @JsOverlay
  @SuppressWarnings( "deprecation" )
  public final Date getDateValue( @Nonnull String key )
  {
    // This will have to be extracted out and replaced at compile time
    final JsDate d = new JsDate( getStringValue( key ) );
    return new Date( d.getFullYear(),
                     d.getMonth(),
                     d.getDate(),
                     d.getHours(),
                     d.getMinutes(),
                     d.getSeconds() );
  }

  @Nonnull
  @JsOverlay
  public final String getStringValue( @Nonnull final String key )
  {
    assert null != data;
    return data.getAny( key ).asString();
  }

  @JsOverlay
  public final boolean getBooleanValue( @Nonnull final String key )
  {
    assert null != data;
    return data.getAny( key ).asBoolean();
  }
}
