package org.realityforge.replicant.client.transport;

import elemental2.core.JsDate;
import java.util.Date;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import jsinterop.base.JsPropertyMap;
import replicant.EntityChannel;

/**
 * A change to an entity.
 */
@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
public class Change
{
  private int id;
  private int type;
  private EntityChannel[] channels;
  private JsPropertyMap<Object> data;

  /**
   * Create an EntityChannel with a sub-channel.
   *
   * @return the new EntityChannel.
   */
  @JsOverlay
  public static Change create( final int id, final int type, @Nonnull final EntityChannel[] channels )
  {
    final Change change = new Change();
    change.id = id;
    change.type = type;
    change.channels = channels;
    return change;
  }

  /**
   * Create an EntityChannel with a sub-channel.
   *
   * @return the new EntityChannel.
   */
  @JsOverlay
  public static Change create( final int id,
                               final int type,
                               @Nonnull final EntityChannel[] channels,
                               @Nullable final JsPropertyMap<Object> data )
  {
    final Change change = new Change();
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
  public final boolean containsKey( @Nonnull String key )
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

  /**
   * @return the number of channels on which the change is sent. Must be &gt; 1.
   */
  @JsOverlay
  public final int getChannelCount()
  {
    return channels.length;
  }

  /**
   * Return the channel id at specific index.
   *
   * @param index the index of the channel.
   * @return the channel id.
   */
  @JsOverlay
  public final int getChannelId( final int index )
  {
    return channels[ index ].getId();
  }

  /**
   * @param index the index of the channel.
   * @return the sub-channel id.
   */
  @Nullable
  @JsOverlay
  public final Integer getSubChannelId( final int index )
  {
    final EntityChannel channel = channels[ index ];
    return channel.hasSubChannelId() ? channel.getSubChannelId() : null;
  }
}
