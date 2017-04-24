package org.realityforge.replicant.client.runtime;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.ChannelDescriptor;
import org.realityforge.replicant.client.EntityRepository;
import org.realityforge.replicant.client.FilterUtil;

/**
 * Base interface which other runtime extensions extend.
 */
public interface BaseRuntimeExtension
{
  @Nonnull
  AreaOfInterestService getAreaOfInterestService();

  @Nonnull
  ContextConverger getContextConverger();

  @Nonnull
  EntityRepository getRepository();

  /**
   * Create or update a Subscription
   */
  @Nonnull
  default SubscriptionReference subscribe( @Nonnull final Scope scope,
                                           @Nonnull final ChannelDescriptor channel,
                                           @Nullable final Object filter )
  {
    final Subscription subscription = getAreaOfInterestService().findSubscription( channel );
    if ( null == subscription )
    {
      getAreaOfInterestService().createSubscription( channel, filter );
    }
    else if ( !FilterUtil.filtersEqual( subscription.getFilter(), filter ) )
    {
      getAreaOfInterestService().updateSubscription( subscription, filter );
    }
    return getAreaOfInterestService().createSubscriptionReference( scope, channel );
  }

  /**
   * Convert an instance subscription to stream based on function.
   * If the instance subscription is not yet present then return an empty stream.
   * This is typically used by the function passed into the convergeCrossDataSourceSubscriptions()
   * method.
   */
  @Nonnull
  default <T> Stream<Object> instanceSubscriptionToValues( @Nonnull final Subscription subscription,
                                                           @Nonnull final Class<T> type,
                                                           @Nonnull final Function<T, Stream<Object>> rootToStream )
  {
    final Object id = subscription.getDescriptor().getID();
    assert null != id;
    final T root = getRepository().findByID( type, id );
    return null != root ? rootToStream.apply( root ) : Stream.empty();
  }

  /**
   * Converge subscriptions across data sources.
   * All instances of the subscriptions to the source graph within the scope are collected.
   * The supplied function is used to generate a stream ofexpected subscriptions to the target graph
   * that are reachable from the source graphs. If an expected subscription is missing it is added,
   * if an additional subscription is present then it is released.
   */
  default void convergeCrossDataSourceSubscriptions( @Nonnull final Scope scope,
                                                     @Nonnull final Enum sourceGraph,
                                                     @Nonnull final Enum targetGraph,
                                                     @Nullable final Object filter,
                                                     @Nonnull final Function<Subscription, Stream<Object>> sourceSubscriptionToTargetIDs )
  {
    getContextConverger().pauseAndRun( () -> doConvergeCrossDataSourceSubscriptions( scope,
                                                                                     sourceGraph,
                                                                                     targetGraph,
                                                                                     filter,
                                                                                     sourceSubscriptionToTargetIDs ) );
  }

  /**
   * This is worker method for convergeCrossDataSourceSubscriptions. Do not use, do not override.
   */
  default void doConvergeCrossDataSourceSubscriptions( @Nonnull final Scope scope,
                                                       @Nonnull final Enum sourceGraph,
                                                       @Nonnull final Enum targetGraph,
                                                       @Nullable final Object filter,
                                                       @Nonnull final Function<Subscription, Stream<Object>> sourceSubscriptionToTargetIDs )
  {
    final Map<Object, Subscription> existing =
      scope.getRequiredSubscriptionsByGraph( targetGraph ).stream().
        collect( Collectors.toMap( s -> s.getDescriptor().getID(), Function.identity() ) );

    //noinspection ConstantConditions
    scope.getRequiredSubscriptions().stream().
      filter( s -> s.getDescriptor().getGraph() == sourceGraph ).
      flatMap( sourceSubscriptionToTargetIDs ).
      filter( Objects::nonNull ).
      filter( id -> null == existing.remove( id ) ).
      forEach( id -> subscribe( scope, new ChannelDescriptor( targetGraph, id ), filter ) );

    existing.values().forEach( Subscription::release );
  }
}
