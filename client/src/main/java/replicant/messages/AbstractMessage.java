package replicant.messages;

import org.jspecify.annotations.NonNull;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * The abstract message that messages conform to.
 */
@SuppressWarnings( { "NotNullFieldNotInitialized", "NullAway.Init" } )
@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
public abstract class AbstractMessage
{
  @NonNull
  String type;

  /**
   * Return the type of the message.
   *
   * @return the type of the message.
   */
  @NonNull
  @JsOverlay
  public final String getType()
  {
    return type;
  }
}
