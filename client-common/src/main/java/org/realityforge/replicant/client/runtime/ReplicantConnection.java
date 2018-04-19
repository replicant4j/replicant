package org.realityforge.replicant.client.runtime;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.ChannelDescriptor;
import org.realityforge.replicant.client.EntityLocator;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.client.FilterUtil;

public interface ReplicantConnection
{
  void connect();

  void disconnect();

  @Nonnull
  AreaOfInterestService getAreaOfInterestService();

  @Nonnull
  ContextConverger getContextConverger();

  @Nonnull
  EntityLocator getEntityLocator();

  @Nonnull
  EntitySubscriptionManager getSubscriptionManager();

  @Nonnull
  ReplicantClientSystem getReplicantClientSystem();

  /**
   * Convert object subscription to root object (specified object Type+ID) to target graphs subscription
   * based on function. If the root object is not yet present then return an empty stream.
   * This is typically used by the function passed into the convergeCrossDataSourceSubscriptions() method.
   */
  @Nonnull
  default <T, O> Stream<O> instanceSubscriptionToValues( @Nonnull final Class<T> type,
                                                         @Nonnull final Object id,
                                                         @Nonnull final Function<T, Stream<O>> rootToStream )
  {
    final T root = getEntityLocator().findByID( type, id );
    return null != root ? rootToStream.apply( root ) : Stream.empty();
  }

  /**
   * Converge subscriptions across data sources.
   * All instances of the subscriptions to the source graph within the scope are collected.
   * The supplied function is used to generate a stream ofexpected subscriptions to the target graph
   * that are reachable from the source graphs. If an expected subscription is missing it is added,
   * if an additional subscription is present then it is released.
   */
  default void convergeCrossDataSourceSubscriptions( @Nonnull final Enum sourceGraph,
                                                     @Nonnull final Enum targetGraph,
                                                     @Nullable final Object filter,
                                                     @Nonnull final Function<Object, Stream<Object>> sourceIDToTargetIDs )
  {
    getContextConverger().pauseAndRun( () -> doConvergeCrossDataSourceSubscriptions( sourceGraph,
                                                                                     targetGraph,
                                                                                     filter,
                                                                                     sourceIDToTargetIDs ) );
  }

  /**
   * This is worker method for convergeCrossDataSourceSubscriptions. Do not use, do not override.
   */
  default void doConvergeCrossDataSourceSubscriptions( @Nonnull final Enum sourceGraph,
                                                       @Nonnull final Enum targetGraph,
                                                       @Nullable final Object filter,
                                                       @Nonnull final Function<Object, Stream<Object>> sourceIDToTargetIDs )
  {
    // Need to check both subscription and filters are identical.
    // If they are not the next step will either update the filters or add subscriptions
    final Map<Object, Subscription> existing =
      getAreaOfInterestService()
        .getSubscriptions()
        .stream()
        .filter( s -> s.getDescriptor().getGraph().equals( targetGraph ) )
        .filter( subscription -> FilterUtil.filtersEqual( subscription.getFilter(), filter ) )
        .collect( Collectors.toMap( s -> s.getDescriptor().getID(), Function.identity() ) );

    //noinspection ConstantConditions
    getAreaOfInterestService()
      .getSubscriptions()
      .stream()
      .filter( s -> s.getDescriptor().getGraph() == sourceGraph )
      .map( s -> s.getDescriptor().getID() )
      .flatMap( sourceIDToTargetIDs )
      .filter( Objects::nonNull )
      .filter( id -> null == existing.remove( id ) )
      .forEach( id -> getAreaOfInterestService().findOrCreateSubscription( new ChannelDescriptor( targetGraph, id ),
                                                                           filter ).createReference() );

    getSubscriptionManager().getInstanceSubscriptions( sourceGraph ).stream().
      flatMap( sourceIDToTargetIDs ).
      filter( Objects::nonNull ).
      filter( id -> null == existing.remove( id ) ).
      forEach( id -> getAreaOfInterestService().findOrCreateSubscription( new ChannelDescriptor( targetGraph, id ),
                                                                          filter ).createReference() );

    existing.values().forEach( Subscription::release );
  }
}
