package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.ChannelAddress;
import replicant.ConnectorState;

/**
 * Basic interface for interacting with data loader services.
 */
public interface DataLoaderService
{
  /**
   * Return the state of the service.
   */
  @Nonnull
  ConnectorState getState();

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
   * Return true if an area of interest action with specified parameters is pending or being processed.
   * When the action parameter is DELETE the filter parameter is ignored.
   */
  boolean isAreaOfInterestActionPending( @Nonnull AreaOfInterestAction action,
                                         @Nonnull ChannelAddress address,
                                         @Nullable Object filter );

  /**
   * Return the index of last matching AreaOfInterestAction in pending aoi actions list.
   */
  int indexOfPendingAreaOfInterestAction( @Nonnull AreaOfInterestAction action,
                                          @Nonnull ChannelAddress address,
                                          @Nullable Object filter );

  void requestSubscribe( @Nonnull ChannelAddress address, @Nullable Object filterParameter );

  void requestSubscriptionUpdate( @Nonnull ChannelAddress address, @Nullable Object filterParameter );

  void requestUnsubscribe( @Nonnull ChannelAddress address );
}
