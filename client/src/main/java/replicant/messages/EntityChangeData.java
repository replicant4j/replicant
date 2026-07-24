package replicant.messages;

import org.jspecify.annotations.NonNull;
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
  default boolean containsKey( @NonNull final String key )
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
  default boolean isNull( @NonNull final String key )
  {
    return null == Js.asPropertyMap( this ).getAsAny( key );
  }

  /**
   * Return the attribute true if data for the attribute identified by the key is null.
   *
   * @param key the attribute key.
   * @return true if the data is null.
   */
  @JsOverlay
  default int getIntegerValue( @NonNull final String key )
  {
    return Js.asPropertyMap( this ).getAsAny( key ).asInt();
  }

  @NonNull
  @JsOverlay
  default String getStringValue( @NonNull final String key )
  {
    return Js.asPropertyMap( this ).getAsAny( key ).asString();
  }

  @JsOverlay
  default boolean getBooleanValue( @NonNull final String key )
  {
    return Js.asPropertyMap( this ).getAsAny( key ).asBoolean();
  }
}
