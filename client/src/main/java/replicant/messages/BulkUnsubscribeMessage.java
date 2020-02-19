package replicant.messages;

import java.util.Objects;
import javax.annotation.Nonnull;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
@SuppressWarnings( { "NotNullFieldNotInitialized", "unused" } )
public final class BulkUnsubscribeMessage
  extends ClientToServerMessage
{
  @Nonnull
  private String[] channels;

  @JsOverlay
  @Nonnull
  public static BulkUnsubscribeMessage create( final int req, @Nonnull final String[] channels )
  {
    final BulkUnsubscribeMessage message = new BulkUnsubscribeMessage();
    message.type = "bulk-unsub";
    message.requestId = req;
    message.channels = Objects.requireNonNull( channels );
    return message;
  }
}
