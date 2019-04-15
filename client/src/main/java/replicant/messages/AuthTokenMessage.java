package replicant.messages;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
@SuppressWarnings( { "NullableProblems", "unused" } )
public final class AuthTokenMessage
  extends ClientToServerMessage
{
  @Nullable
  private String token;

  @JsOverlay
  @Nonnull
  public static AuthTokenMessage create( final int req, @Nullable final String token )
  {
    final AuthTokenMessage message = new AuthTokenMessage();
    message.type = "auth";
    message.requestId = req;
    message.token = token;
    return message;
  }
}
