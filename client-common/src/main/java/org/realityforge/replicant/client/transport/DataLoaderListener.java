package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface DataLoaderListener<G extends Enum<G>>
{
  /**
   * Invoked to fire an event when disconnect has completed.
   */
  void onDisconnect();

  /**
   * Invoked to fire an event when failed to connect.
   */
  void onInvalidDisconnect( @Nonnull Throwable exception );

  /**
   * Invoked to fire an event when disconnect has completed.
   */
  void onConnect();

  /**
   * Invoked to fire an event when failed to connect.
   */
  void onInvalidConnect( @Nonnull Throwable throwable );

  /**
   * Called when a data load has completed.
   */
  void onDataLoadComplete( @Nonnull DataLoadStatus status );

  /**
   * Called when a data load has resulted in a failure.
   */
  void onDataLoadFailure( @Nonnull Throwable throwable );

  /**
   * Attempted to retrieve data from backend and failed.
   */
  void onPollFailure( @Nonnull Throwable throwable );

  void onSubscribeStarted( @Nonnull G graph, @Nullable Object id );

  void onSubscribeCompleted( @Nonnull G graph, @Nullable Object id );

  void onSubscribeFailed( @Nonnull G graph, @Nullable Object id, @Nonnull Throwable throwable );

  void onUnsubscribeStarted( @Nonnull G graph, @Nullable Object id );

  void onUnsubscribeCompleted( @Nonnull G graph, @Nullable Object id );

  void onUnsubscribeFailed( @Nonnull G graph, @Nullable Object id, @Nonnull Throwable throwable );

  void onSubscriptionUpdateStarted( @Nonnull G graph, @Nullable Object id );

  void onSubscriptionUpdateCompleted( @Nonnull G graph, @Nullable Object id );

  void onSubscriptionUpdateFailed( @Nonnull G graph, @Nullable Object id, @Nonnull Throwable throwable );
}
