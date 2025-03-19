package replicant.messages;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import org.realityforge.replicant.shared.Messages;

@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
@SuppressWarnings( { "NotNullFieldNotInitialized", "unused" } )
public final class SubscribeMessage
  extends ClientToServerMessage
{
  @JsOverlay
  public static final String TYPE = Messages.C2S_Type.SUB;

  @Nonnull
  private String channel;
  @Nullable
  private Object filter;

  @JsOverlay
  @Nonnull
  public static SubscribeMessage create( final int req, @Nonnull final String ch, @Nullable final Object filter )
  {
    final SubscribeMessage message = new SubscribeMessage();
    message.type = TYPE;
    message.requestId = req;
    message.channel = ch;
    if ( null != filter )
    {
      message.filter = filter;
    }
    return message;
  }
}
