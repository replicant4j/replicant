package org.realityforge.replicant.client.transport;

import java.util.logging.Logger;
import javax.annotation.Nonnull;
import replicant.ChannelAddress;

/**
 * Debug listener that simply logs state transitions.
 */
public class DebugDataLoaderListener
  implements DataLoaderListener
{
  private static final Logger LOG = Logger.getLogger( DebugDataLoaderListener.class.getName() );

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDisconnect( @Nonnull final DataLoaderService service )
  {
    LOG.warning( "onDisconnect(" + service.getKey() + ")" );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onInvalidDisconnect( @Nonnull final DataLoaderService service, @Nonnull final Throwable throwable )
  {
    LOG.warning( "onInvalidDisconnect(" + service.getKey() + "," + throwable + ")" );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onConnect( @Nonnull final DataLoaderService service )
  {
    LOG.warning( "onConnect(" + service.getKey() + ")" );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onInvalidConnect( @Nonnull final DataLoaderService service, @Nonnull final Throwable throwable )
  {
    LOG.warning( "onInvalidConnect(" + service.getKey() + "," + throwable + ")" );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDataLoadComplete( @Nonnull final DataLoaderService service, @Nonnull final DataLoadStatus status )
  {
    LOG.warning( "onDataLoadComplete(" + service.getKey() + "," + status + ")" );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDataLoadFailure( @Nonnull final DataLoaderService service, @Nonnull final Throwable throwable )
  {
    LOG.warning( "onDataLoadFailure(" + service.getKey() + "," + throwable + ")" );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onPollFailure( @Nonnull final DataLoaderService service, @Nonnull final Throwable throwable )
  {
    LOG.warning( "onPollFailure(" + service.getKey() + "," + throwable + ")" );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onSubscribeStarted( @Nonnull final DataLoaderService service,
                                  @Nonnull final ChannelAddress descriptor )
  {
    LOG.warning( "onSubscribeStarted(" + service.getKey() + "," + descriptor + ")" );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onSubscribeCompleted( @Nonnull final DataLoaderService service,
                                    @Nonnull final ChannelAddress descriptor )
  {
    LOG.warning( "onSubscribeCompleted(" + service.getKey() + "," + descriptor + ")" );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onSubscribeFailed( @Nonnull final DataLoaderService service,
                                 @Nonnull final ChannelAddress descriptor,
                                 @Nonnull final Throwable throwable )
  {
    LOG.warning( "onSubscribeFailed(" + service.getKey() + "," + descriptor + "," + throwable + ")" );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onUnsubscribeStarted( @Nonnull final DataLoaderService service,
                                    @Nonnull final ChannelAddress descriptor )
  {
    LOG.warning( "onUnsubscribeStarted(" + service.getKey() + "," + descriptor + ")" );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onUnsubscribeCompleted( @Nonnull final DataLoaderService service,
                                      @Nonnull final ChannelAddress descriptor )
  {
    LOG.warning( "onUnsubscribeCompleted(" + service.getKey() + "," + descriptor + ")" );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onUnsubscribeFailed( @Nonnull final DataLoaderService service,
                                   @Nonnull final ChannelAddress descriptor,
                                   @Nonnull final Throwable throwable )
  {
    LOG.warning( "onUnsubscribeFailed(" + service.getKey() + "," + descriptor + "," + throwable + ")" );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onSubscriptionUpdateStarted( @Nonnull final DataLoaderService service,
                                           @Nonnull final ChannelAddress descriptor )
  {
    LOG.warning( "onSubscriptionUpdateStarted(" + service.getKey() + "," + descriptor + ")" );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onSubscriptionUpdateCompleted( @Nonnull final DataLoaderService service,
                                             @Nonnull final ChannelAddress descriptor )
  {
    LOG.warning( "onSubscriptionUpdateCompleted(" + service.getKey() + "," + descriptor + ")" );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onSubscriptionUpdateFailed( @Nonnull final DataLoaderService service,
                                          @Nonnull final ChannelAddress descriptor,
                                          @Nonnull final Throwable throwable )
  {
    LOG.warning( "onSubscriptionUpdateFailed(" + service.getKey() + "," + descriptor + "," + throwable + ")" );
  }
}
