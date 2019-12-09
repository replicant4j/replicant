package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;
import org.realityforge.replicant.client.ChannelAddress;

public abstract class DataLoaderListenerAdapter
  implements DataLoaderListener
{
  @Override
  public void onDisconnect( @Nonnull final DataLoaderService service )
  {
  }

  @Override
  public void onInvalidDisconnect( @Nonnull final DataLoaderService service, @Nonnull final Throwable throwable )
  {
  }

  @Override
  public void onConnect( @Nonnull final DataLoaderService service )
  {
  }

  @Override
  public void onInvalidConnect( @Nonnull final DataLoaderService service, @Nonnull final Throwable throwable )
  {
  }

  @Override
  public void onDataLoadComplete( @Nonnull final DataLoaderService service, @Nonnull final DataLoadStatus status )
  {
  }

  @Override
  public void onDataLoadFailure( @Nonnull final DataLoaderService service, @Nonnull final Throwable throwable )
  {
  }

  @Override
  public void onPollFailure( @Nonnull final DataLoaderService service, @Nonnull final Throwable throwable )
  {
  }

  @Override
  public void onSubscribeStarted( @Nonnull final DataLoaderService service,
                                  @Nonnull final ChannelAddress descriptor )
  {
  }

  @Override
  public void onSubscribeCompleted( @Nonnull final DataLoaderService service,
                                    @Nonnull final ChannelAddress descriptor )
  {
  }

  @Override
  public void onSubscribeFailed( @Nonnull final DataLoaderService service,
                                 @Nonnull final ChannelAddress descriptor,
                                 @Nonnull final Throwable throwable )
  {
  }

  @Override
  public void onUnsubscribeStarted( @Nonnull final DataLoaderService service,
                                    @Nonnull final ChannelAddress descriptor )
  {
  }

  @Override
  public void onUnsubscribeCompleted( @Nonnull final DataLoaderService service,
                                      @Nonnull final ChannelAddress descriptor )
  {
  }

  @Override
  public void onUnsubscribeFailed( @Nonnull final DataLoaderService service,
                                   @Nonnull final ChannelAddress descriptor,
                                   @Nonnull final Throwable throwable )
  {
  }

  @Override
  public void onSubscriptionUpdateStarted( @Nonnull final DataLoaderService service,
                                           @Nonnull final ChannelAddress descriptor )
  {
  }

  @Override
  public void onSubscriptionUpdateCompleted( @Nonnull final DataLoaderService service,
                                             @Nonnull final ChannelAddress descriptor )
  {
  }

  @Override
  public void onSubscriptionUpdateFailed( @Nonnull final DataLoaderService service,
                                          @Nonnull final ChannelAddress descriptor,
                                          @Nonnull final Throwable throwable )
  {
  }
}
