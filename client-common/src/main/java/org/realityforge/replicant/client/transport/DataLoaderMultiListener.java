package org.realityforge.replicant.client.transport;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class DataLoaderMultiListener<G extends Enum<G>>
  implements DataLoaderListener<G>
{
  private final Stream<DataLoaderListener<G>> _listeners;

  public DataLoaderMultiListener( @Nonnull final DataLoaderListener<G>[] listeners )
  {
    Objects.requireNonNull( listeners );
    assert Arrays.stream( listeners ).allMatch( Objects::nonNull );
    _listeners = Stream.of( listeners );
  }

  @Override
  public void onDisconnect()
  {
    _listeners.forEach( DataLoaderListener::onDisconnect );
  }

  @Override
  public void onInvalidDisconnect( @Nonnull final Throwable throwable )
  {
    _listeners.forEach( e -> e.onInvalidDisconnect( throwable ) );
  }

  @Override
  public void onConnect()
  {
    _listeners.forEach( DataLoaderListener::onConnect );
  }

  @Override
  public void onInvalidConnect( @Nonnull final Throwable throwable )
  {
    _listeners.forEach( e -> e.onInvalidConnect( throwable ) );
  }

  @Override
  public void onDataLoadComplete( @Nonnull final DataLoadStatus status )
  {
    _listeners.forEach( e -> e.onDataLoadComplete( status ) );
  }

  @Override
  public void onDataLoadFailure( @Nonnull final Throwable throwable )
  {
    _listeners.forEach( e -> e.onDataLoadFailure( throwable ) );
  }

  @Override
  public void onPollFailure( @Nonnull final Throwable throwable )
  {
    _listeners.forEach( e -> e.onPollFailure( throwable ) );
  }

  @Override
  public void onSubscribeStarted( @Nonnull final G graph, @Nullable final Object id )
  {
    _listeners.forEach( e -> e.onSubscribeStarted( graph, id ) );
  }

  @Override
  public void onSubscribeCompleted( @Nonnull final G graph, @Nullable final Object id )
  {
    _listeners.forEach( e -> e.onSubscribeCompleted( graph, id ) );
  }

  @Override
  public void onSubscribeFailed( @Nonnull final G graph,
                                 @Nullable final Object id,
                                 @Nonnull final Throwable throwable )
  {
    _listeners.forEach( e -> e.onSubscribeFailed( graph, id, throwable ) );
  }

  @Override
  public void onUnsubscribeStarted( @Nonnull final G graph, @Nullable final Object id )
  {
    _listeners.forEach( e -> e.onUnsubscribeStarted( graph, id ) );
  }

  @Override
  public void onUnsubscribeCompleted( @Nonnull final G graph, @Nullable final Object id )
  {
    _listeners.forEach( e -> e.onUnsubscribeCompleted( graph, id ) );
  }

  @Override
  public void onUnsubscribeFailed( @Nonnull final G graph,
                                   @Nullable final Object id,
                                   @Nonnull final Throwable throwable )
  {
    _listeners.forEach( e -> e.onUnsubscribeFailed( graph, id, throwable ) );
  }

  @Override
  public void onSubscriptionUpdateStarted( @Nonnull final G graph, @Nullable final Object id )
  {
    _listeners.forEach( e -> e.onSubscriptionUpdateStarted( graph, id ) );
  }

  @Override
  public void onSubscriptionUpdateCompleted( @Nonnull final G graph, @Nullable final Object id )
  {
    _listeners.forEach( e -> e.onSubscriptionUpdateCompleted( graph, id ) );
  }

  @Override
  public void onSubscriptionUpdateFailed( @Nonnull final G graph,
                                          @Nullable final Object id,
                                          @Nonnull final Throwable throwable )
  {
    _listeners.forEach( e -> e.onSubscriptionUpdateFailed( graph, id, throwable ) );
  }
}
