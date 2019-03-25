package replicant.messages;

import javax.annotation.Nonnull;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
@SuppressWarnings( { "NullableProblems", "unused" } )
public final class UnsubscribeMessage
  extends ClientToServerMessage
{
  @Nonnull
  private String channel;

  @JsOverlay
  @Nonnull
  public static UnsubscribeMessage create( final int req, @Nonnull final String ch )
  {
    final UnsubscribeMessage message = new UnsubscribeMessage();
    message.type = "unsub";
    message.req = req;
    message.channel = ch;
    return message;
  }
}
