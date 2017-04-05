package org.realityforge.replicant.client.transport;

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

  void setListener( @Nullable DataLoaderListener listener );

  /**
   * A symbolic key for describing system.
   */
  @Nonnull
  String getKey();

  /**
   * Return the class of graphs that this loader processes.
   */
  @Nonnull
  Class getGraphType();

  boolean isSubscribed( @Nonnull Enum graph );

  boolean isSubscribed( @Nonnull Enum graph, @Nonnull Object id );

  @SuppressWarnings( "unchecked" )
  default boolean isSubscribed( @Nonnull ChannelDescriptor descriptor )
  {
    if ( null == descriptor.getID() )
    {
      return isSubscribed( descriptor.getGraph() );
    }
    else
    {
      return isSubscribed( descriptor.getGraph(), descriptor.getID() );
    }
  }

  boolean isAreaOfInterestActionPending( @Nonnull AreaOfInterestAction action, @Nonnull ChannelDescriptor descriptor );
}
