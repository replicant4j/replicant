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
  void onInvalidDisconnect( @Nonnull DataLoaderService service, @Nonnull Throwable throwable );

  /**
   * Invoked to fire an event when disconnect has completed.
   */
  void onConnect( @Nonnull DataLoaderService service );

  /**
   * Invoked to fire an event when failed to connect.
   */
  void onInvalidConnect( @Nonnull DataLoaderService service, @Nonnull Throwable throwable );

  /**
   * Called when a data load has completed.
   */
  void onDataLoadComplete( @Nonnull DataLoaderService service, @Nonnull DataLoadStatus status );

  /**
   * Called when a data load has resulted in a failure.
   */
  void onDataLoadFailure( @Nonnull DataLoaderService service, @Nonnull Throwable throwable );

  /**
   * Attempted to retrieve data from backend and failed.
   */
  void onPollFailure( @Nonnull DataLoaderService service, @Nonnull Throwable throwable );

  void onSubscribeStarted( @Nonnull DataLoaderService service, @Nonnull ChannelAddress descriptor );

  void onSubscribeCompleted( @Nonnull DataLoaderService service, @Nonnull ChannelAddress descriptor );

  void onSubscribeFailed( @Nonnull DataLoaderService service,
                          @Nonnull ChannelAddress descriptor,
                          @Nonnull Throwable throwable );

  void onUnsubscribeStarted( @Nonnull DataLoaderService service, @Nonnull ChannelAddress descriptor );

  void onUnsubscribeCompleted( @Nonnull DataLoaderService service, @Nonnull ChannelAddress descriptor );

  void onUnsubscribeFailed( @Nonnull DataLoaderService service,
                            @Nonnull ChannelAddress descriptor,
                            @Nonnull Throwable throwable );

  void onSubscriptionUpdateStarted( @Nonnull DataLoaderService service, @Nonnull ChannelAddress descriptor );

  void onSubscriptionUpdateCompleted( @Nonnull DataLoaderService service, @Nonnull ChannelAddress descriptor );

  void onSubscriptionUpdateFailed( @Nonnull DataLoaderService service,
                                   @Nonnull ChannelAddress descriptor,
                                   @Nonnull Throwable throwable );
}
