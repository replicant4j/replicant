package replicant;

import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The transport is responsible for communicating with the backend system.
 */
public interface Transport
{
  interface OnConnect
  {
    void onConnect( @Nonnull String connectionId );
  }

  interface OnError
  {
    void onError( @Nonnull Throwable error );
  }

  interface Context
  {
    /**
     * Return the schemaId that the transport is responsible for transporting.
     *
     * @return the schemaId that the transport is responsible for transporting.
     */
    int getSchemaId();

    /**
     * Return the sequence of the last PDU received.
     *
     * @return the sequence of the last PDU received.
     */
    int getLastRxSequence();

    /**
     * Notify the Connector that a message was received.
     *
     * @param rawJsonData the message.
     */
    void onMessageReceived( @Nonnull String rawJsonData );

    /**
     * Notify the Connector that there was an error reading a message from the Transport.
     *
     * @param error the error.
     */
    void onMessageReadFailure( @Nonnull Throwable error );

    /**
     * Direct the Connector to disconnect the transport.
     */
    void disconnect();
  }

  /**
   * Perform the connection, invoking the action when connection has completed.
   *
   * @param onConnect      the action to invoke once connect has completed.
   * @param onConnectError the action to invoke if connect errors.
   */
  void connect( @Nonnull OnConnect onConnect, @Nonnull OnError onConnectError );

  /**
   * This is invoked by the Connector when the {@link OnConnect#onConnect(String)} method executes.
   * This method is responsible for providing the necessary context information for the Transport to
   * communicate with the back-end. This context is no longer valid after the callbacks of the
   * {@link #disconnect(SafeProcedure, OnError)} method are invoked.
   *
   * @param context the context that provides environmental data to Transport.
   */
  void bind( @Nonnull Context context );

  /**
   * Perform the disconnection, invoking the action when disconnection has completed.
   *
   * @param onDisconnect      the action to invoke once disconnect has completed.
   * @param onDisconnectError the action to invoke if disconnect errors.
   */
  void disconnect( @Nonnull SafeProcedure onDisconnect, @Nonnull OnError onDisconnectError );

  void requestSubscribe( @Nonnull ChannelAddress address,
                         @Nullable Object filter,
                         @Nonnull String eTag,
                         @Nonnull SafeProcedure onCacheValid,
                         @Nonnull SafeProcedure onSuccess,
                         @Nonnull Consumer<Throwable> onError );

  void requestSubscribe( @Nonnull ChannelAddress address,
                         @Nullable Object filter,
                         @Nonnull SafeProcedure onSuccess,
                         @Nonnull Consumer<Throwable> onError );

  void requestUnsubscribe( @Nonnull ChannelAddress address,
                           @Nonnull SafeProcedure onSuccess,
                           @Nonnull Consumer<Throwable> onError );

  void requestSubscriptionUpdate( @Nonnull ChannelAddress address,
                                  @Nonnull Object filter,
                                  @Nonnull SafeProcedure onSuccess,
                                  @Nonnull Consumer<Throwable> onError );

  void requestBulkSubscribe( @Nonnull List<ChannelAddress> addresses,
                             @Nullable Object filter,
                             @Nonnull SafeProcedure onSuccess,
                             @Nonnull Consumer<Throwable> onError );

  void requestBulkUnsubscribe( @Nonnull List<ChannelAddress> addresses,
                               @Nonnull SafeProcedure onSuccess,
                               @Nonnull Consumer<Throwable> onError );

  void requestBulkSubscriptionUpdate( @Nonnull List<ChannelAddress> addresses,
                                      @Nonnull Object filter,
                                      @Nonnull SafeProcedure onSuccess,
                                      @Nonnull Consumer<Throwable> onError );

  /**
   * Notify the Transport when a Connector has completed processing a message.
   * This is used by the Transport to perform primitive form of flow-control
   */
  void onMessageProcessed();
}
