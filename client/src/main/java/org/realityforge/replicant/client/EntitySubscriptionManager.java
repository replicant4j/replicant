package org.realityforge.replicant.client;

import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface EntitySubscriptionManager
{
  /**
   * Return the collection of current type subscriptions.
   * These keys can be directly used to unsubscribe from the graph.
   */
  @Nonnull
  Set<Enum> getTypeSubscriptions();

  /**
   * Return the collection of enums that represent instance subscriptions.
   * These can be used to further interrogate the EntitySubscriptionManager
   * to retrieve the set of instance subscriptions.
   */
  @Nonnull
  Set<Enum> getInstanceSubscriptionKeys();


  /**
   * Return the collection of instance subscriptions for graph.
   */
  @Nonnull
  Set<Object> getInstanceSubscriptions( @Nonnull Enum graph );

  /**
   * Record a subscription for specified graph.
   *
   * @param graph the graph.
   * @param filter the filter if subscription is filterable.
   * @param explicitSubscription if subscription was explicitly requested by the client.
   * @return the subscription entry.
   * @throws IllegalStateException if graph already subscribed to.
   */
  @Nonnull
  ChannelSubscriptionEntry recordSubscription( @Nonnull ChannelAddress graph,
                                               @Nullable Object filter,
                                               boolean explicitSubscription )
    throws IllegalStateException;

  /**
   * Update subscription details for the specified graph.
   *
   * @param graph the graph.
   * @param filter the filter being updated.
   * @return the subscription entry.
   * @throws IllegalStateException if graph already subscribed to.
   */
  @Nonnull
  ChannelSubscriptionEntry updateSubscription( @Nonnull ChannelAddress graph, @Nullable Object filter )
    throws IllegalStateException;

  /**
   * Return the subscription details for the specified graph.
   *
   * @param graph the graph.
   * @return the subscription entry.
   * @throws IllegalArgumentException if no such subscription
   */
  @Nonnull
  ChannelSubscriptionEntry getSubscription( @Nonnull ChannelAddress graph )
    throws IllegalArgumentException;

  /**
   * Return the subscription details for the specified graph if a subscription is recorded.
   *
   * @param graph the graph.
   * @return the subscription entry if it exists, null otherwise.
   */
  @Nullable
  ChannelSubscriptionEntry findSubscription( @Nonnull ChannelAddress graph );

  /**
   * Remove subscription details for specified graph.
   *
   * @param graph the graph.
   * @return the subscription entry.
   * @throws IllegalStateException if graph not subscribed to.
   */
  @Nonnull
  ChannelSubscriptionEntry removeSubscription( @Nonnull ChannelAddress graph )
    throws IllegalStateException;

  /**
   * Return the subscription details for entity.
   *
   * @param type the type of the entity.
   * @param id   the id of the entity.
   * @return the subscription entry.
   * @throws IllegalArgumentException if no such subscription
   */
  @Nonnull
  EntitySubscriptionEntry getSubscription( @Nonnull Class<?> type, @Nonnull Object id );

  /**
   * Find the subscription details for entity.
   *
   * @param type the type of the entity.
   * @param id   the id of the entity.
   * @return the subscription entry if it exists, null otherwise.
   */
  @Nullable
  EntitySubscriptionEntry findSubscription( @Nonnull Class<?> type, @Nonnull Object id );

  /**
   * Register specified entity as being part of specified graphs.
   *
   * Note: It is assumed that if an entity is part of a graph, they are always part of the graph.
   * This may not be true with filters but we can assume it for all other scenarios.
   *
   * @param type   the type of the entity.
   * @param id     the id of the entity.
   * @param graphs the graphs that the entity is part of.
   */
  void updateEntity( @Nonnull Class<?> type, @Nonnull Object id, @Nonnull ChannelAddress[] graphs );

  /**
   * Disassociate entity from specified graph.
   *
   * Note: It is assumed that the caller will remove the entity from the subscription manager and
   * repository if there are no more subscriptions.
   *
   * @param type  the type of the entity.
   * @param id    the id of the entity.
   * @param graph the graph that the entity is to be disassociated from.
   * @return the entry representing entities subscription state.
   * @throws IllegalStateException if no such entity or the entity is not associated with the graph.
   */
  @Nonnull
  EntitySubscriptionEntry removeEntityFromGraph( @Nonnull Class<?> type,
                                                 @Nonnull Object id,
                                                 @Nonnull ChannelAddress graph )
    throws IllegalStateException;

  /**
   * Remove entity and all associated subscriptions.
   *
   * @param type the type of the entity.
   * @param id   the id of the entity.
   */
  void removeEntity( @Nonnull Class<?> type, @Nonnull Object id );
}
