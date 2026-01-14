package replicant.messages;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import replicant.shared.Messages;

@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
@SuppressWarnings( { "unused" } )
public final class AuthTokenMessage
  extends ClientToServerMessage
{
  @JsOverlay
  public static final String TYPE = Messages.C2S_Type.AUTH;
  @Nullable
  private String token;

  @JsOverlay
  @Nonnull
  public static AuthTokenMessage create( final int req, @Nullable final String token )
  {
    final AuthTokenMessage message = new AuthTokenMessage();
    message.type = TYPE;
    message.requestId = req;
    message.token = token;
    return message;
  }
}
