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
     * Return the id of current connection.
     *
     * @return the id of current connection.
     */
    @Nullable
    String getConnectionId();

    /**
     * Return the schemaId that the transport is responsible for transporting.
     *
     * @return the schemaId that the transport is responsible for transporting.
     */
    int getSchemaId();

    /**
     * Return the id of the last request transmitted.
     *
     * @return the id of the last request transmitted.
     */
    int getLastTxRequestId();

    /**
     * Record the id of the last sync request transmitted.
     *
     * @param requestId the id of the last sync request transmitted.
     */
    void recordLastSyncTxRequestId( int requestId );

    /**
     * Record the id of the last sync request received.
     *
     * @param requestId the id of the last sync request received.
     */
    void recordLastSyncRxRequestId( int requestId );

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
   * {@link #disconnect(SafeProcedure)} method are invoked.
   *
   * @param context the context that provides environmental data to Transport.
   */
  void bind( @Nonnull Context context, @Nonnull ReplicantContext replicantContext );

  /**
   * This method is invoked by the Connector when the connection
   * disconnects or there is a fatal error. This method disassociates the connection context bound to the transport
   * via the {@link #bind(Context, ReplicantContext)} method.
   */
  void unbind();

  /**
   * Perform the disconnection, invoking the action when disconnection has completed.
   *
   * @param onDisconnect the action to invoke once disconnect has completed.
   */
  void disconnect( @Nonnull SafeProcedure onDisconnect );

  /**
   * Request a synchronization point.
   * This method talks to the back-end and pings it. If the reply returns and there has been no
   * intermediate requests then the connection is considered synchronized to that point. If there
   * has been requests in the meantime (i.e. the sequence number of sync is not 1 behind) then
   * there is still processing queued on the server (or client).
   *
   * @param onInSync    hook invoked when request returns and replicant state is in sync.
   * @param onOutOfSync hook invoked when request returns and replicant state is not in sync.
   * @param onError     hook invoked if there was an error processing sync request.
   */
  void requestSync( @Nonnull SafeProcedure onInSync,
                    @Nonnull SafeProcedure onOutOfSync,
                    @Nonnull Consumer<Throwable> onError );

  void requestSubscribe( @Nonnull ChannelAddress address,
                         @Nullable Object filter,
                         @Nullable String eTag,
                         @Nullable SafeProcedure onCacheValid,
                         @Nonnull SafeProcedure onSuccess,
                         @Nonnull Consumer<Throwable> onError );

  void requestUnsubscribe( @Nonnull ChannelAddress address,
                           @Nonnull SafeProcedure onSuccess,
                           @Nonnull Consumer<Throwable> onError );

  void requestBulkSubscribe( @Nonnull List<ChannelAddress> addresses,
                             @Nullable Object filter,
                             @Nonnull SafeProcedure onSuccess,
                             @Nonnull Consumer<Throwable> onError );

  void requestBulkUnsubscribe( @Nonnull List<ChannelAddress> addresses,
                               @Nonnull SafeProcedure onSuccess,
                               @Nonnull Consumer<Throwable> onError );
}
