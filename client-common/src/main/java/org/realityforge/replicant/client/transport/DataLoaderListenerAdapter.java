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
  public void onDisconnect( @Nonnull final DataLoaderService<G> service )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onInvalidDisconnect( @Nonnull final DataLoaderService<G> service, @Nonnull final Throwable throwable )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onConnect( @Nonnull final DataLoaderService<G> service )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onInvalidConnect( @Nonnull final DataLoaderService<G> service, @Nonnull final Throwable throwable )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDataLoadComplete( @Nonnull final DataLoaderService<G> service, @Nonnull final DataLoadStatus status )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDataLoadFailure( @Nonnull final DataLoaderService<G> service, @Nonnull final Throwable throwable )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onPollFailure( @Nonnull final DataLoaderService<G> service, @Nonnull final Throwable throwable )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onSubscribeStarted( @Nonnull final DataLoaderService<G> service,
                                  @Nonnull final G graph,
                                  @Nullable final Object id )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onSubscribeCompleted( @Nonnull final DataLoaderService<G> service,
                                    @Nonnull final G graph,
                                    @Nullable final Object id )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onSubscribeFailed( @Nonnull final DataLoaderService<G> service,
                                 @Nonnull final G graph,
                                 @Nullable final Object id,
                                 @Nonnull final Throwable throwable )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onUnsubscribeStarted( @Nonnull final DataLoaderService<G> service,
                                    @Nonnull final G graph,
                                    @Nullable final Object id )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onUnsubscribeCompleted( @Nonnull final DataLoaderService<G> service,
                                      @Nonnull final G graph,
                                      @Nullable final Object id )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onUnsubscribeFailed( @Nonnull final DataLoaderService<G> service,
                                   @Nonnull final G graph,
                                   @Nullable final Object id,
                                   @Nonnull final Throwable throwable )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onSubscriptionUpdateStarted( @Nonnull final DataLoaderService<G> service,
                                           @Nonnull final G graph,
                                           @Nullable final Object id )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onSubscriptionUpdateCompleted( @Nonnull final DataLoaderService<G> service,
                                             @Nonnull final G graph,
                                             @Nullable final Object id )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onSubscriptionUpdateFailed( @Nonnull final DataLoaderService<G> service,
                                          @Nonnull final G graph,
                                          @Nullable final Object id,
                                          @Nonnull final Throwable throwable )
  {
  }
}
