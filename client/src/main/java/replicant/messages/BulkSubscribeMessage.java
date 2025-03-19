package replicant.messages;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import replicant.shared.Messages;

@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
@SuppressWarnings( { "NotNullFieldNotInitialized", "unused" } )
public final class BulkSubscribeMessage
  extends ClientToServerMessage
{
  @JsOverlay
  public static final String TYPE = Messages.C2S_Type.BULK_SUB;
  @Nonnull
  private String[] channels;
  @Nullable
  private Object filter;

  @JsOverlay
  @Nonnull
  public static BulkSubscribeMessage create( final int req,
                                             @Nonnull final String[] channels,
                                             @Nullable final Object filter )
  {
    final BulkSubscribeMessage message = new BulkSubscribeMessage();
    message.type = TYPE;
    message.requestId = req;
    message.channels = Objects.requireNonNull( channels );
    message.filter = filter;
    return message;
  }
}
