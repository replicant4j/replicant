package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;
import replicant.ChannelAddress;

public interface DataLoaderListener
{
  /**
   * Invoked to fire an event when disconnect has completed.
   */
  void onDisconnect( @Nonnull DataLoaderService service );

  /**
   * Invoked to fire an event when failed to connect.
   */
  void onInvalidDisconnect( @Nonnull DataLoaderService service, @Nonnull Throwable error );

  /**
   * Invoked to fire an event when disconnect has completed.
   */
  void onConnect( @Nonnull DataLoaderService service );

  /**
   * Invoked to fire an event when failed to connect.
   */
  void onInvalidConnect( @Nonnull DataLoaderService service, @Nonnull Throwable error );

  /**
   * Called when a data load has completed.
   */
  void onDataLoadComplete( @Nonnull DataLoaderService service, @Nonnull DataLoadStatus status );

  /**
   * Called when a data load has resulted in a failure.
   */
  void onDataLoadFailure( @Nonnull DataLoaderService service, @Nonnull Throwable error );

  /**
   * Attempted to retrieve data from backend and failed.
   */
  void onPollFailure( @Nonnull DataLoaderService service, @Nonnull Throwable error );

  void onSubscribeStarted( @Nonnull DataLoaderService service, @Nonnull ChannelAddress address );

  void onSubscribeCompleted( @Nonnull DataLoaderService service, @Nonnull ChannelAddress address );

  void onSubscribeFailed( @Nonnull DataLoaderService service,
                          @Nonnull ChannelAddress address,
                          @Nonnull Throwable error );

  void onUnsubscribeStarted( @Nonnull DataLoaderService service, @Nonnull ChannelAddress address );

  void onUnsubscribeCompleted( @Nonnull DataLoaderService service, @Nonnull ChannelAddress address );

  void onUnsubscribeFailed( @Nonnull DataLoaderService service,
                            @Nonnull ChannelAddress address,
                            @Nonnull Throwable error );

  void onSubscriptionUpdateStarted( @Nonnull DataLoaderService service, @Nonnull ChannelAddress address );

  void onSubscriptionUpdateCompleted( @Nonnull DataLoaderService service, @Nonnull ChannelAddress address );

  void onSubscriptionUpdateFailed( @Nonnull DataLoaderService service,
                                   @Nonnull ChannelAddress address,
                                   @Nonnull Throwable error );
}
