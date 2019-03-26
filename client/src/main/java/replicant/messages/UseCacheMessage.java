package replicant.messages;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * The message that represents a set of changes to subscriptions and entities that should be applied atomically.
 */
@SuppressWarnings( "NullableProblems" )
@SuppressFBWarnings( "EI_EXPOSE_REP" )
@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
public class UseCacheMessage
  extends ServerToClientMessage
{
  @JsOverlay
  public static final String TYPE = "use-cache";
  @Nonnull
  private String channel;
  @Nonnull
  private String etag;

  @GwtIncompatible
  @Nonnull
  public static UseCacheMessage create( @Nullable final Integer requestId,
                                        @Nonnull final String channel,
                                        @Nonnull final String eTag )
  {
    final UseCacheMessage changeSet = new UseCacheMessage();
    changeSet.type = TYPE;
    changeSet.requestId = null == requestId ? null : requestId.doubleValue();
    changeSet.channel = Objects.requireNonNull( channel );
    changeSet.etag = Objects.requireNonNull( eTag );
    return changeSet;
  }

  @JsOverlay
  @Nonnull
  public final String getChannel()
  {
    return channel;
  }

  @JsOverlay
  @Nonnull
  public final String getEtag()
  {
    return etag;
  }
}
