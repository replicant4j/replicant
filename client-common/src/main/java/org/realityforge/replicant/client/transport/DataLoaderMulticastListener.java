package org.realityforge.replicant.client.transport;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class DataLoaderMulticastListener<G extends Enum<G>>
  implements DataLoaderListener<G>
{
  private final List<DataLoaderListener<G>> _listeners;

  public DataLoaderMulticastListener( @Nonnull final DataLoaderListener<G>... listeners )
  {
    Objects.requireNonNull( listeners );
    assert Arrays.stream( listeners ).allMatch( Objects::nonNull );
    _listeners = Arrays.asList( listeners );
  }

  public void forEach( @Nonnull final Consumer<DataLoaderListener<G>> action )
  {
    _listeners.forEach( action );
  }

  @Override
  public void onDisconnect( @Nonnull final DataLoaderService<G> service )
  {
    forEach( e -> e.onDisconnect( service ) );
  }

  @Override
  public void onInvalidDisconnect( @Nonnull final DataLoaderService<G> service, @Nonnull final Throwable throwable )
  {
    forEach( e -> e.onInvalidDisconnect( service, throwable ) );
  }

  @Override
  public void onConnect( @Nonnull final DataLoaderService<G> service )
  {
    forEach( e -> e.onConnect( service ) );
  }

  @Override
  public void onInvalidConnect( @Nonnull final DataLoaderService<G> service, @Nonnull final Throwable throwable )
  {
    forEach( e -> e.onInvalidConnect( service, throwable ) );
  }

  @Override
  public void onDataLoadComplete( @Nonnull final DataLoaderService<G> service, @Nonnull final DataLoadStatus status )
  {
    forEach( e -> e.onDataLoadComplete( service, status ) );
  }

  @Override
  public void onDataLoadFailure( @Nonnull final DataLoaderService<G> service, @Nonnull final Throwable throwable )
  {
    forEach( e -> e.onDataLoadFailure( service, throwable ) );
  }

  @Override
  public void onPollFailure( @Nonnull final DataLoaderService<G> service, @Nonnull final Throwable throwable )
  {
    forEach( e -> e.onPollFailure( service, throwable ) );
  }

  @Override
  public void onSubscribeStarted( @Nonnull final DataLoaderService<G> service,
                                  @Nonnull final G graph,
                                  @Nullable final Object id )
  {
    forEach( e -> e.onSubscribeStarted( service, graph, id ) );
  }

  @Override
  public void onSubscribeCompleted( @Nonnull final DataLoaderService<G> service,
                                    @Nonnull final G graph,
                                    @Nullable final Object id )
  {
    forEach( e -> e.onSubscribeCompleted( service, graph, id ) );
  }

  @Override
  public void onSubscribeFailed( @Nonnull final DataLoaderService<G> service,
                                 @Nonnull final G graph,
                                 @Nullable final Object id,
                                 @Nonnull final Throwable throwable )
  {
    forEach( e -> e.onSubscribeFailed( service, graph, id, throwable ) );
  }

  @Override
  public void onUnsubscribeStarted( @Nonnull final DataLoaderService<G> service,
                                    @Nonnull final G graph,
                                    @Nullable final Object id )
  {
    forEach( e -> e.onUnsubscribeStarted( service, graph, id ) );
  }

  @Override
  public void onUnsubscribeCompleted( @Nonnull final DataLoaderService<G> service,
                                      @Nonnull final G graph,
                                      @Nullable final Object id )
  {
    forEach( e -> e.onUnsubscribeCompleted( service, graph, id ) );
  }

  @Override
  public void onUnsubscribeFailed( @Nonnull final DataLoaderService<G> service,
                                   @Nonnull final G graph,
                                   @Nullable final Object id,
                                   @Nonnull final Throwable throwable )
  {
    forEach( e -> e.onUnsubscribeFailed( service, graph, id, throwable ) );
  }

  @Override
  public void onSubscriptionUpdateStarted( @Nonnull final DataLoaderService<G> service,
                                           @Nonnull final G graph,
                                           @Nullable final Object id )
  {
    forEach( e -> e.onSubscriptionUpdateStarted( service, graph, id ) );
  }

  @Override
  public void onSubscriptionUpdateCompleted( @Nonnull final DataLoaderService<G> service,
                                             @Nonnull final G graph,
                                             @Nullable final Object id )
  {
    forEach( e -> e.onSubscriptionUpdateCompleted( service, graph, id ) );
  }

  @Override
  public void onSubscriptionUpdateFailed( @Nonnull final DataLoaderService<G> service,
                                          @Nonnull final G graph,
                                          @Nullable final Object id,
                                          @Nonnull final Throwable throwable )
  {
    forEach( e -> e.onSubscriptionUpdateFailed( service, graph, id, throwable ) );
  }
}
