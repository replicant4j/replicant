package org.realityforge.replicant.client.transport;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.ChannelDescriptor;

/**
 * Basic interface for interacting with data loader services.
 */
public interface DataLoaderService
{
  enum State
  {
    /// The service is not yet connected or has been disconnected
    DISCONNECTED,
    /// The service has started connecting but connection has not completed.
    CONNECTING,
    /// The service is connected.
    CONNECTED,
    /// The service has started disconnecting but disconnection has not completed.
    DISCONNECTING,
    /// The service is in error state. This error may occur during connection, disconnection or in normal operation.
    ERROR
  }

  /**
   * Return the state of the service.
   */
  @Nonnull
  State getState();

  /**
   * Return a session if present.
   * The session is present if the service is in CONNECTED state or is transitioning states.
   */
  @Nullable
  ClientSession getSession();

  /**
   * Return a session and raise an exception if no session is present.
   */
  @Nonnull
  ClientSession ensureSession();

  /**
   * Connect to the underlying data source.
   * When connection is complete then execute passed runnable.
   */
  void connect();

  /**
   * Disconnect from underlying data source.
   * When disconnection is complete then execute passed runnable. The runnable
   * will also be invoked if the data loader service is not currently connected.
   */
  void disconnect();

  /**
   * Schedule a data load.
   */
  void scheduleDataLoad();

  /**
   * Add a listener. Return true if actually added, false if listener was already present.
   */
  boolean addDataLoaderListener( @Nonnull DataLoaderListener listener );

  /**
   * Remove a listener. Return true if actually removed, false if listener was not present.
   */
  boolean removeDataLoaderListener( @Nonnull DataLoaderListener listener );

  /**
   * A symbolic key for describing system.
   */
  @Nonnull
  String getKey();

  /**
   * Return the class of graphs that this loader processes.
   */
  @Nonnull
  Class<? extends Enum> getGraphType();

  boolean isSubscribed( @Nonnull ChannelDescriptor descriptor );

  /**
   * Return the time at which the connection establishment started
   */
  @Nullable
  Date getConnectingAt();

  /**
   * Return the time at which the connection establishment completed
   */
  @Nullable
  Date getConnectedAt();

  /**
   * Return the time at which the connection last disconnected in error
   */
  @Nullable
  Date getDisconnectedAt();

  /**
   * Return the latest error that occurred during current connection establishment.
   * Will be null if no error occurred during the establishment of the current connection
   */
  @Nullable
  Throwable getLastErrorDuringConnection();

  /**
   * Return the last error that occurred whilst connected, probably triggered a disconnection.
   * Never nulled
   */
  @Nullable
  Throwable getLastError();

  /**
   * Return the time at which the LastError occurred
   */
  @Nullable
  Date getLastErrorAt();

  /**
   * Return true if an area of interest action with specified parameters is pending or being processed.
   * When the action parameter is DELETE the filter parameter is ignored.
   */
  boolean isAreaOfInterestActionPending( @Nonnull AreaOfInterestAction action,
                                         @Nonnull ChannelDescriptor descriptor,
                                         @Nullable Object filter );

  /**
   * Return the index of last matching AreaOfInterestAction in pending aoi actions list.
   */
  int indexOfPendingAreaOfInterestAction( @Nonnull AreaOfInterestAction action,
                                          @Nonnull ChannelDescriptor descriptor,
                                          @Nullable Object filter );

  default void requestSubscribe( @Nonnull final ChannelDescriptor descriptor, @Nullable final Object filterParameter )
  {
    ensureSession().requestSubscribe( descriptor, filterParameter );
  }

  default void requestSubscriptionUpdate( @Nonnull final ChannelDescriptor descriptor,
                                          @Nullable final Object filterParameter )
  {
    ensureSession().requestSubscriptionUpdate( descriptor, filterParameter );
  }

  default void requestUnsubscribe( @Nonnull final ChannelDescriptor descriptor )
  {
    ensureSession().requestUnsubscribe( descriptor );
  }

  default void onCommunicationError( @Nonnull final Throwable error )
  {
  }

  /**
   * Return a set of properties that can be included in the 'status' report for this DataLoader
   */
  default @Nonnull
  Map<String, String> getStatusProperties()
  {
    return new HashMap<>();
  }
}
