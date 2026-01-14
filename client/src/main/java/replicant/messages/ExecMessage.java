package replicant.messages;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import replicant.shared.Messages;

@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
@SuppressWarnings( { "NotNullFieldNotInitialized", "unused" } )
public final class ExecMessage
  extends ClientToServerMessage
{
  @JsOverlay
  public static final String TYPE = Messages.C2S_Type.EXEC;
  @Nonnull
  private String command;
  @Nullable
  private Object payload;

  @JsOverlay
  @Nonnull
  public static ExecMessage create( final int req, @Nonnull final String command, @Nullable final Object payload )
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
  @Nonnull
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
