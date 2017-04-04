package org.realityforge.replicant.client.transport;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
    _listeners = Collections.unmodifiableList( Arrays.asList( listeners ) );
  }

  public List<DataLoaderListener<G>> getListeners()
  {
    return _listeners;
  }

  @Override
  public void onDisconnect( @Nonnull final DataLoaderService<G> service )
  {
    getListeners().forEach( e -> e.onDisconnect( service ) );
  }

  @Override
  public void onInvalidDisconnect( @Nonnull final DataLoaderService<G> service, @Nonnull final Throwable throwable )
  {
    getListeners().forEach( e -> e.onInvalidDisconnect( service, throwable ) );
  }

  @Override
  public void onConnect( @Nonnull final DataLoaderService<G> service )
  {
    getListeners().forEach( e -> e.onConnect( service ) );
  }

  @Override
  public void onInvalidConnect( @Nonnull final DataLoaderService<G> service, @Nonnull final Throwable throwable )
  {
    getListeners().forEach( e -> e.onInvalidConnect( service, throwable ) );
  }

  @Override
  public void onDataLoadComplete( @Nonnull final DataLoaderService<G> service, @Nonnull final DataLoadStatus status )
  {
    getListeners().forEach( e -> e.onDataLoadComplete( service, status ) );
  }

  @Override
  public void onDataLoadFailure( @Nonnull final DataLoaderService<G> service, @Nonnull final Throwable throwable )
  {
    getListeners().forEach( e -> e.onDataLoadFailure( service, throwable ) );
  }

  @Override
  public void onPollFailure( @Nonnull final DataLoaderService<G> service, @Nonnull final Throwable throwable )
  {
    getListeners().forEach( e -> e.onPollFailure( service, throwable ) );
  }

  @Override
  public void onSubscribeStarted( @Nonnull final DataLoaderService<G> service,
                                  @Nonnull final G graph,
                                  @Nullable final Object id )
  {
    getListeners().forEach( e -> e.onSubscribeStarted( service, graph, id ) );
  }

  @Override
  public void onSubscribeCompleted( @Nonnull final DataLoaderService<G> service,
                                    @Nonnull final G graph,
                                    @Nullable final Object id )
  {
    getListeners().forEach( e -> e.onSubscribeCompleted( service, graph, id ) );
  }

  @Override
  public void onSubscribeFailed( @Nonnull final DataLoaderService<G> service,
                                 @Nonnull final G graph,
                                 @Nullable final Object id,
                                 @Nonnull final Throwable throwable )
  {
    getListeners().forEach( e -> e.onSubscribeFailed( service, graph, id, throwable ) );
  }

  @Override
  public void onUnsubscribeStarted( @Nonnull final DataLoaderService<G> service,
                                    @Nonnull final G graph,
                                    @Nullable final Object id )
  {
    getListeners().forEach( e -> e.onUnsubscribeStarted( service, graph, id ) );
  }

  @Override
  public void onUnsubscribeCompleted( @Nonnull final DataLoaderService<G> service,
                                      @Nonnull final G graph,
                                      @Nullable final Object id )
  {
    getListeners().forEach( e -> e.onUnsubscribeCompleted( service, graph, id ) );
  }

  @Override
  public void onUnsubscribeFailed( @Nonnull final DataLoaderService<G> service,
                                   @Nonnull final G graph,
                                   @Nullable final Object id,
                                   @Nonnull final Throwable throwable )
  {
    getListeners().forEach( e -> e.onUnsubscribeFailed( service, graph, id, throwable ) );
  }

  @Override
  public void onSubscriptionUpdateStarted( @Nonnull final DataLoaderService<G> service,
                                           @Nonnull final G graph,
                                           @Nullable final Object id )
  {
    getListeners().forEach( e -> e.onSubscriptionUpdateStarted( service, graph, id ) );
  }

  @Override
  public void onSubscriptionUpdateCompleted( @Nonnull final DataLoaderService<G> service,
                                             @Nonnull final G graph,
                                             @Nullable final Object id )
  {
    getListeners().forEach( e -> e.onSubscriptionUpdateCompleted( service, graph, id ) );
  }

  @Override
  public void onSubscriptionUpdateFailed( @Nonnull final DataLoaderService<G> service,
                                          @Nonnull final G graph,
                                          @Nullable final Object id,
                                          @Nonnull final Throwable throwable )
  {
    getListeners().forEach( e -> e.onSubscriptionUpdateFailed( service, graph, id, throwable ) );
  }
}
