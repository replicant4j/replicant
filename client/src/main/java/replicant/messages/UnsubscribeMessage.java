package replicant.messages;

import javax.annotation.Nonnull;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import replicant.shared.Messages;

@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
@SuppressWarnings( { "NotNullFieldNotInitialized", "unused" } )
public final class UnsubscribeMessage
  extends ClientToServerMessage
{
  @JsOverlay
  public static final String TYPE = Messages.C2S_Type.UNSUB;
  @Nonnull
  private String channel;

  @JsOverlay
  @Nonnull
  public static UnsubscribeMessage create( final int req, @Nonnull final String ch )
  {
    final UnsubscribeMessage message = new UnsubscribeMessage();
    message.type = TYPE;
    message.requestId = req;
    message.channel = ch;
    return message;
  }
}
