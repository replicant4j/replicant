package replicant.messages;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
@SuppressWarnings( { "NullableProblems", "unused" } )
public final class BulkSubscribeMessage
  extends ClientToServerMessage
{
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
    message.type = "bulk-sub";
    message.req = req;
    message.channels = Objects.requireNonNull( channels );
    message.filter = filter;
    return message;
  }
}
