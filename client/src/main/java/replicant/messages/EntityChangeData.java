package replicant.messages;

import javax.annotation.Nonnull;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import jsinterop.base.Js;

/**
 * A simple abstraction for getting data off a javascript object.
 */
@JsType( isNative = true, name = "Object", namespace = JsPackage.GLOBAL )
public interface EntityChangeData
{
  /**
   * Return true if data for the attribute identified by the key is present in the change.
   *
   * @param key the attribute key.
   * @return true if the data is present.
   */
  @JsOverlay
  default boolean containsKey( @Nonnull final String key )
  {
    return Js.asPropertyMap( this ).has( key );
  }

  /**
   * Return true if data for the attribute identified by the key is null.
   *
   * @param key the attribute key.
   * @return true if the data is null.
   */
  @JsOverlay
  default boolean isNull( @Nonnull final String key )
  {
    return null == Js.asPropertyMap( this ).getAny( key );
  }

  /**
   * Return the attribute true if data for the attribute identified by the key is null.
   *
   * @param key the attribute key.
   * @return true if the data is null.
   */
  @JsOverlay
  default int getIntegerValue( @Nonnull final String key )
  {
    return Js.asPropertyMap( this ).getAny( key ).asInt();
  }

  @Nonnull
  @JsOverlay
  default String getStringValue( @Nonnull final String key )
  {
    return Js.asPropertyMap( this ).getAny( key ).asString();
  }

  @JsOverlay
  default boolean getBooleanValue( @Nonnull final String key )
  {
    return Js.asPropertyMap( this ).getAny( key ).asBoolean();
  }
}
