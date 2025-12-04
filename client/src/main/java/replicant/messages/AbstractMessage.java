package replicant.messages;

import javax.annotation.Nonnull;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * The abstract message that messages conform to.
 */
@SuppressWarnings( "NotNullFieldNotInitialized" )
@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
public abstract class AbstractMessage
{
  @Nonnull
  String type;

  /**
   * Return the type of the message.
   *
   * @return the type of the message.
   */
  @Nonnull
  @JsOverlay
  public final String getType()
  {
    return type;
  }
}
