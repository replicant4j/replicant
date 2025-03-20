package replicant;

import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The transport is responsible for communicating with the backend system.
 */
public interface Transport
{
  /**
   * Perform the connection.
   */
  void requestConnect( @Nonnull TransportContext context );

  /**
   * This method is invoked by the Connector when the connection
   * disconnects or there is a fatal error. This method disassociates the connection context bound to the transport
   * via the {@link #requestConnect(TransportContext)} method.
   */
  void unbind();

  /**
   * Request disconnection.
   */
  void requestDisconnect();

  /**
   * Request a synchronization point.
   * This method talks to the back-end and pings it. If the reply returns and there has been no
   * intermediate requests then the connection is considered synchronized to that point. If there
   * has been requests in the meantime (i.e. the sequence number of sync is not 1 behind) then
   * there is still processing queued on the server (or client).
   */
  void requestSync();

  void updateAuthToken( @Nullable String authToken );

  void updateEtagsSync( @Nonnull Map<String, String> channelToEtagMap );

  void requestExec( @Nonnull String command, @Nullable Object payload );

  void requestSubscribe( @Nonnull ChannelAddress address, @Nullable Object filter );

  void requestUnsubscribe( @Nonnull ChannelAddress address );

  void requestBulkSubscribe( @Nonnull List<ChannelAddress> addresses, @Nullable Object filter );

  void requestBulkUnsubscribe( @Nonnull List<ChannelAddress> addresses );
}
