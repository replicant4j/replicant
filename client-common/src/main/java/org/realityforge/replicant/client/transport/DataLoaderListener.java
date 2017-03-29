package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface DataLoaderListener<G extends Enum<G>>
{
  /**
   * Invoked to fire an event when disconnect has completed.
   */
  void onDisconnect( @Nonnull DataLoaderService<G> service );

  /**
   * Invoked to fire an event when failed to connect.
   */
  void onInvalidDisconnect( @Nonnull DataLoaderService<G> service, @Nonnull Throwable throwable );

  /**
   * Invoked to fire an event when disconnect has completed.
   */
  void onConnect( @Nonnull DataLoaderService<G> service );

  /**
   * Invoked to fire an event when failed to connect.
   */
  void onInvalidConnect( @Nonnull DataLoaderService<G> service, @Nonnull Throwable throwable );

  /**
   * Called when a data load has completed.
   */
  void onDataLoadComplete( @Nonnull DataLoaderService<G> service, @Nonnull DataLoadStatus status );

  /**
   * Called when a data load has resulted in a failure.
   */
  void onDataLoadFailure( @Nonnull DataLoaderService<G> service, @Nonnull Throwable throwable );

  /**
   * Attempted to retrieve data from backend and failed.
   */
  void onPollFailure( @Nonnull DataLoaderService<G> service, @Nonnull Throwable throwable );

  void onSubscribeStarted( @Nonnull DataLoaderService<G> service, @Nonnull G graph, @Nullable Object id );

  void onSubscribeCompleted( @Nonnull DataLoaderService<G> service, @Nonnull G graph, @Nullable Object id );

  void onSubscribeFailed( @Nonnull DataLoaderService<G> service,
                          @Nonnull G graph,
                          @Nullable Object id,
                          @Nonnull Throwable throwable );

  void onUnsubscribeStarted( @Nonnull DataLoaderService<G> service, @Nonnull G graph, @Nullable Object id );

  void onUnsubscribeCompleted( @Nonnull DataLoaderService<G> service, @Nonnull G graph, @Nullable Object id );

  void onUnsubscribeFailed( @Nonnull DataLoaderService<G> service,
                            @Nonnull G graph,
                            @Nullable Object id,
                            @Nonnull Throwable throwable );

  void onSubscriptionUpdateStarted( @Nonnull DataLoaderService<G> service, @Nonnull G graph, @Nullable Object id );

  void onSubscriptionUpdateCompleted( @Nonnull DataLoaderService<G> service, @Nonnull G graph, @Nullable Object id );

  void onSubscriptionUpdateFailed( @Nonnull DataLoaderService<G> service,
                                   @Nonnull G graph,
                                   @Nullable Object id,
                                   @Nonnull Throwable throwable );
}
