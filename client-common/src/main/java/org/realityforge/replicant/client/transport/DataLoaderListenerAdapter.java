package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class DataLoaderListenerAdapter<G extends Enum<G>>
  implements DataLoaderListener<G>
{
  /**
   * {@inheritDoc}
   */
  @Override
  public void onDisconnect()
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onInvalidDisconnect( @Nonnull final Throwable throwable )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onConnect()
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onInvalidConnect( @Nonnull final Throwable throwable )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDataLoadComplete( @Nonnull final DataLoadStatus status )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDataLoadFailure( @Nonnull final Throwable throwable )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onPollFailure( @Nonnull final Throwable throwable )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onSubscribeStarted( @Nonnull final G graph, @Nullable final Object id )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onSubscribeCompleted( @Nonnull final G graph, @Nullable final Object id )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onSubscribeFailed( @Nonnull final G graph, @Nullable final Object id, @Nonnull final Throwable throwable )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onUnsubscribeStarted( @Nonnull final G graph, @Nullable final Object id )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onUnsubscribeCompleted( @Nonnull final G graph, @Nullable final Object id )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onUnsubscribeFailed( @Nonnull final G graph,
                                   @Nullable final Object id,
                                   @Nonnull final Throwable throwable )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onSubscriptionUpdateStarted( @Nonnull final G graph, @Nullable final Object id )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onSubscriptionUpdateCompleted( @Nonnull final G graph, @Nullable final Object id )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onSubscriptionUpdateFailed( @Nonnull final G graph,
                                          @Nullable final Object id,
                                          @Nonnull final Throwable throwable )
  {
  }
}
