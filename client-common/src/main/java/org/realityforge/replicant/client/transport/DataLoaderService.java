package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Basic interface for interacting with data loader services.
 */
public interface DataLoaderService<G>
{
  /**
   * @return true if connected to underlying data source.
   */
  boolean isConnected();

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

  boolean isSubscribed( @Nonnull G graph );

  boolean isSubscribed( @Nonnull G graph, @Nonnull Object id );
}
