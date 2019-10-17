package replicant.messages;

import javax.annotation.Nullable;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * The abstract message type that all messages that are sent from server conform to.
 */
@SuppressFBWarnings( "EI_EXPOSE_REP" )
@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
public abstract class ServerToClientMessage
  extends AbstractMessage
{
  @Nullable
  Double requestId;

  /**
   * Return the id of the request that this message is in response to.
   * If a different client generated this message or the message is not in response to a particular
   * request then this is null.
   *
   * @return the id of the clients request that generated message, else null.
   */
  @Nullable
  @JsOverlay
  public final Integer getRequestId()
  {
    return null == requestId ? null : requestId.intValue();
  }

  @JsOverlay
  public final void setRequestId( @Nullable final Integer requestId )
  {
    this.requestId = null == requestId ? null : requestId.doubleValue();
  }
}
