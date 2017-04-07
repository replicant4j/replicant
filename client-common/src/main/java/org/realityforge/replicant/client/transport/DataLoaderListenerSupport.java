package org.realityforge.replicant.client.transport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.ChannelDescriptor;

public final class DataLoaderListenerSupport
  implements DataLoaderListener
{
  private final ArrayList<DataLoaderListener> _listeners = new ArrayList<>();
  private final List<DataLoaderListener> _roListeners = Collections.unmodifiableList( _listeners );

  public boolean addListener( @Nonnull final DataLoaderListener listener )
  {
    Objects.requireNonNull( listener );
    if ( !_listeners.contains( listener ) )
    {
      _listeners.add( listener );
      return true;
    }
    else
    {
      return false;
    }
  }

  public boolean removeListener( @Nonnull final DataLoaderListener listener )
  {
    Objects.requireNonNull( listener );
    return _listeners.remove( listener );
  }

  public List<DataLoaderListener> getListeners()
  {
    return _roListeners;
  }

  @Override
  public void onDisconnect( @Nonnull final DataLoaderService service )
  {
    _listeners.forEach( l -> l.onDisconnect( service ) );
  }

  @Override
  public void onInvalidDisconnect( @Nonnull final DataLoaderService service, @Nonnull final Throwable throwable )
  {
    _listeners.forEach( l -> l.onInvalidDisconnect( service, throwable ) );
  }

  @Override
  public void onConnect( @Nonnull final DataLoaderService service )
  {
    _listeners.forEach( l -> l.onConnect( service ) );
  }

  @Override
  public void onInvalidConnect( @Nonnull final DataLoaderService service, @Nonnull final Throwable throwable )
  {
    _listeners.forEach( l -> l.onInvalidConnect( service, throwable ) );
  }

  @Override
  public void onDataLoadComplete( @Nonnull final DataLoaderService service, @Nonnull final DataLoadStatus status )
  {
    _listeners.forEach( l -> l.onDataLoadComplete( service, status ) );
  }

  @Override
  public void onDataLoadFailure( @Nonnull final DataLoaderService service, @Nonnull final Throwable throwable )
  {
    _listeners.forEach( l -> l.onDataLoadFailure( service, throwable ) );
  }

  @Override
  public void onPollFailure( @Nonnull final DataLoaderService service, @Nonnull final Throwable throwable )
  {
    _listeners.forEach( l -> l.onPollFailure( service, throwable ) );
  }

  @Override
  public void onSubscribeStarted( @Nonnull final DataLoaderService service,
                                  @Nonnull final ChannelDescriptor descriptor )
  {
    _listeners.forEach( l -> l.onSubscribeStarted( service, descriptor ) );
  }

  @Override
  public void onSubscribeCompleted( @Nonnull final DataLoaderService service,
                                    @Nonnull final ChannelDescriptor descriptor )
  {
    _listeners.forEach( l -> l.onSubscribeCompleted( service, descriptor ) );
  }

  @Override
  public void onSubscribeFailed( @Nonnull final DataLoaderService service,
                                 @Nonnull final ChannelDescriptor descriptor,
                                 @Nonnull final Throwable throwable )
  {
    _listeners.forEach( l -> l.onSubscribeFailed( service, descriptor, throwable ) );
  }

  @Override
  public void onUnsubscribeStarted( @Nonnull final DataLoaderService service,
                                    @Nonnull final ChannelDescriptor descriptor )
  {
    _listeners.forEach( l -> l.onUnsubscribeStarted( service, descriptor ) );
  }

  @Override
  public void onUnsubscribeCompleted( @Nonnull final DataLoaderService service,
                                      @Nonnull final ChannelDescriptor descriptor )
  {
    _listeners.forEach( l -> l.onUnsubscribeCompleted( service, descriptor ) );
  }

  @Override
  public void onUnsubscribeFailed( @Nonnull final DataLoaderService service,
                                   @Nonnull final ChannelDescriptor descriptor,
                                   @Nonnull final Throwable throwable )
  {
    _listeners.forEach( l -> l.onUnsubscribeFailed( service, descriptor, throwable ) );
  }

  @Override
  public void onSubscriptionUpdateStarted( @Nonnull final DataLoaderService service,
                                           @Nonnull final ChannelDescriptor descriptor )
  {
    _listeners.forEach( l -> l.onSubscriptionUpdateStarted( service, descriptor ) );
  }

  @Override
  public void onSubscriptionUpdateCompleted( @Nonnull final DataLoaderService service,
                                             @Nonnull final ChannelDescriptor descriptor )
  {
    _listeners.forEach( l -> l.onSubscriptionUpdateCompleted( service, descriptor ) );
  }

  @Override
  public void onSubscriptionUpdateFailed( @Nonnull final DataLoaderService service,
                                          @Nonnull final ChannelDescriptor descriptor,
                                          @Nonnull final Throwable throwable )
  {
    _listeners.forEach( l -> l.onSubscriptionUpdateFailed( service, descriptor, throwable ) );
  }
}
