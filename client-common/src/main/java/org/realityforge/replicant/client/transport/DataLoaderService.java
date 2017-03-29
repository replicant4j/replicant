package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Basic interface for interacting with data loader services.
 */
public interface DataLoaderService<G extends Enum<G>>
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
   * Connect to the underlying data source.
   * When connection is complete then execute passed runnable.
   */
  void connect( @Nullable Runnable runnable );

  /**
   * Disconnect from underlying data source.
   * When disconnection is complete then execute passed runnable. The runnable
   * will also be invoked if the data loader service is not currently connected.
   */
  void disconnect( @Nullable Runnable runnable );

  /**
   * Schedule a data load.
   */
  void scheduleDataLoad();

  void setListener( @Nullable DataLoaderListener<G> listener );

  /**
   * Return the class of graphs that this loader processes.
   */
  @Nonnull
  Class<G> getGraphType();

  boolean isSubscribed( @Nonnull G graph );

  boolean isSubscribed( @Nonnull G graph, @Nonnull Object id );
}
