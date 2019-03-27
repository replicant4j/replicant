package replicant.messages;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
@SuppressWarnings( { "NullableProblems", "unused" } )
public final class OkMessage
  extends ServerToClientMessage
{
  @JsOverlay
  public static final String TYPE = "ok";

  @GwtIncompatible
  @Nonnull
  public static OkMessage create( final int requestId )
  {
    final OkMessage message = new OkMessage();
    message.type = TYPE;
    message.requestId = (double) requestId;
    return message;
  }
}
