package replicant.messages;

import javax.annotation.Nullable;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import replicant.shared.Messages;

/**
 * The message that represents a set of changes to subscriptions and entities that should be applied atomically.
 */
@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
public class ErrorMessage
  extends ServerToClientMessage
{
  @JsOverlay
  public static final String TYPE = Messages.S2C_Type.ERROR;
  @Nullable
  private String message;

  @JsOverlay
  @Nullable
  public final String getMessage()
  {
    return message;
  }
}
