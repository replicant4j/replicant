package replicant.messages;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * The abstract message type that all messages that are sent to the server conform to.
 */
@SuppressWarnings( "NullableProblems" )
@SuppressFBWarnings( "EI_EXPOSE_REP" )
@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
public abstract class ClientToServerMessage
  extends AbstractMessage
{
  int requestId;

  /**
   * Return the request id of the message.
   * Each request should have a unique value so that the client can track which which responses
   * are a result of which request.
   *
   * @return the request id of the message.
   */
  @JsOverlay
  public final int getRequestId()
  {
    return requestId;
  }
}
