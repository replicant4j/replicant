package replicant.messages;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
@SuppressWarnings( { "NullableProblems", "unused" } )
public final class SubscribeMessage
  extends ClientToServerMessage
{
  @Nonnull
  private String channel;
  @Nullable
  private Object filter;

  @JsOverlay
  @Nonnull
  public static SubscribeMessage create( final int req, @Nonnull final String ch, @Nullable final Object filter )
  {
    final SubscribeMessage message = new SubscribeMessage();
    message.type = "sub";
    message.requestId = req;
    message.channel = ch;
    if ( null != filter )
    {
      message.filter = filter;
    }
    return message;
  }
}
