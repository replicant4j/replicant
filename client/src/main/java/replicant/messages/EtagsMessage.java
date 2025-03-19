package replicant.messages;

import javax.annotation.Nonnull;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import replicant.shared.Messages;

@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
@SuppressWarnings( { "NotNullFieldNotInitialized", "unused" } )
public final class EtagsMessage
  extends ClientToServerMessage
{
  @JsOverlay
  public static final String TYPE = Messages.C2S_Type.ETAGS;

  @Nonnull
  private EtagsData etags;

  @JsOverlay
  @Nonnull
  public static EtagsMessage create( final int req, @Nonnull final EtagsData etags )
  {
    final EtagsMessage message = new EtagsMessage();
    message.type = TYPE;
    message.requestId = req;
    message.etags = etags;
    return message;
  }
}
