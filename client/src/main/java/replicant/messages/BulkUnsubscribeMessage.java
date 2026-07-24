package replicant.messages;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import replicant.shared.Messages;

@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
@SuppressWarnings( { "NotNullFieldNotInitialized", "NullAway.Init", "unused" } )
public final class BulkUnsubscribeMessage
  extends ClientToServerMessage
{
  @JsOverlay
  public static final String TYPE = Messages.C2S_Type.BULK_UNSUB;
  @NonNull
  private String[] channels;

  @JsOverlay
  @NonNull
  public static BulkUnsubscribeMessage create( final int req, @NonNull final String[] channels )
  {
    final BulkUnsubscribeMessage message = new BulkUnsubscribeMessage();
    message.type = TYPE;
    message.requestId = req;
    message.channels = Objects.requireNonNull( channels );
    return message;
  }
}
