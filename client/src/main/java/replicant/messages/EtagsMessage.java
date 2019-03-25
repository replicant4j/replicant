package replicant.messages;

import javax.annotation.Nonnull;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
@SuppressWarnings( { "NullableProblems", "unused" } )
public final class EtagsMessage
  extends ClientToServerMessage
{
  @Nonnull
  private EtagsData etags;

  @JsOverlay
  @Nonnull
  public static EtagsMessage create( final int req, @Nonnull final EtagsData etags )
  {
    final EtagsMessage message = new EtagsMessage();
    message.type = "etags";
    message.req = req;
    message.etags = etags;
    return message;
  }
}
