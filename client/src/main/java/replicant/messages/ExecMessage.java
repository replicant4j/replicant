package replicant.messages;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import replicant.shared.Messages;

@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
@SuppressWarnings( { "NotNullFieldNotInitialized", "NullAway.Init", "unused" } )
public final class ExecMessage
  extends ClientToServerMessage
{
  @JsOverlay
  public static final String TYPE = Messages.C2S_Type.EXEC;
  @NonNull
  private String command;
  @Nullable
  private Object payload;

  @JsOverlay
  @NonNull
  public static ExecMessage create( final int req, @NonNull final String command, @Nullable final Object payload )
  {
    final ExecMessage message = new ExecMessage();
    message.type = TYPE;
    message.requestId = req;
    message.command = command;
    if ( null != payload )
    {
      message.payload = payload;
    }
    return message;
  }

  @JsOverlay
  @NonNull
  public String getCommand()
  {
    return command;
  }

  @JsOverlay
  @Nullable
  public Object getPayload()
  {
    return payload;
  }
}
