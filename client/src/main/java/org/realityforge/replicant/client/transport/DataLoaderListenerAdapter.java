package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;
import replicant.ChannelAddress;

public abstract class DataLoaderListenerAdapter
  implements DataLoaderListener
{
  /**
   * {@inheritDoc}
   */
  @Override
  public void onDisconnect( @Nonnull final DataLoaderService service )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onInvalidDisconnect( @Nonnull final DataLoaderService service, @Nonnull final Throwable throwable )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onConnect( @Nonnull final DataLoaderService service )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onInvalidConnect( @Nonnull final DataLoaderService service, @Nonnull final Throwable throwable )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDataLoadComplete( @Nonnull final DataLoaderService service, @Nonnull final DataLoadStatus status )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDataLoadFailure( @Nonnull final DataLoaderService service, @Nonnull final Throwable throwable )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onPollFailure( @Nonnull final DataLoaderService service, @Nonnull final Throwable throwable )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onSubscribeStarted( @Nonnull final DataLoaderService service,
                                  @Nonnull final ChannelAddress address )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onSubscribeCompleted( @Nonnull final DataLoaderService service,
                                    @Nonnull final ChannelAddress address )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onSubscribeFailed( @Nonnull final DataLoaderService service,
                                 @Nonnull final ChannelAddress address,
                                 @Nonnull final Throwable throwable )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onUnsubscribeStarted( @Nonnull final DataLoaderService service,
                                    @Nonnull final ChannelAddress address )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onUnsubscribeCompleted( @Nonnull final DataLoaderService service,
                                      @Nonnull final ChannelAddress address )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onUnsubscribeFailed( @Nonnull final DataLoaderService service,
                                   @Nonnull final ChannelAddress address,
                                   @Nonnull final Throwable throwable )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onSubscriptionUpdateStarted( @Nonnull final DataLoaderService service,
                                           @Nonnull final ChannelAddress address )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onSubscriptionUpdateCompleted( @Nonnull final DataLoaderService service,
                                             @Nonnull final ChannelAddress address )
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onSubscriptionUpdateFailed( @Nonnull final DataLoaderService service,
                                          @Nonnull final ChannelAddress address,
                                          @Nonnull final Throwable throwable )
  {
  }
}
