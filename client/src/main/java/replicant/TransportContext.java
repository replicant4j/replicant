package replicant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.messages.ServerToClientMessage;

interface TransportContext
{
  /**
   * Create a new request abstraction.
   *
   * @param name        the name of the request. This should be null if {@link Replicant#areNamesEnabled()} returns false, otherwise it should be non-null.
   * @param syncRequest is request a sync request.
   */
  @Nonnull
  RequestEntry newRequest( @Nullable String name, final boolean syncRequest );

  /**
   * Notify the Connector that a message was received.
   *
   * @param message the message.
   */
  void onMessageReceived( @Nonnull ServerToClientMessage message );

  /**
   * Notify the Connector that there was an error from the Transport.
   *
   * @param error the error.
   */
  void onError( @Nonnull Throwable error );

  /**
   * Notify the Connector that the Transport has disconnected.
   */
  void onDisconnect();
}
