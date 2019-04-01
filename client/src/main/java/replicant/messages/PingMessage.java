package replicant.messages;

import javax.annotation.Nonnull;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
@SuppressWarnings( { "NullableProblems", "unused" } )
public final class PingMessage
  extends ClientToServerMessage
{
  @JsOverlay
  @Nonnull
  public static PingMessage create( final int req )
  {
    final PingMessage message = new PingMessage();
    message.type = "ping";
    message.requestId = req;
    return message;
  }
}
