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
   * Record a subscription to a graph containing types.
   *
   * @param graph the graph to subscribe to.
   * @param filter the filter if subscription is update-able.
   * @return the subscription entry.
   * @throws IllegalStateException if graph already subscribed to.
   */
  @Nonnull
  ChannelSubscriptionEntry subscribe( @Nonnull Enum graph, @Nullable Object filter )
    throws IllegalStateException;

  /**
   * Subscribe to graph rooted at an instance.
   *
   * @param graph the graph to subscribe to.
   * @param id    the id of the root object.
   * @param filter the filter if subscription is update-able.
   * @return the subscription entry.
   * @throws IllegalStateException if graph already subscribed to.
   */
  @Nonnull
  ChannelSubscriptionEntry subscribe( @Nonnull Enum graph, @Nonnull Object id, @Nullable Object filter );

  /**
   * Update subscription to a graph containing types.
   *
   * @param graph the graph to subscribe to.
   * @param filter the filter being updated.
   * @return the subscription entry.
   * @throws IllegalStateException if graph already subscribed to.
   */
  @Nonnull
  ChannelSubscriptionEntry updateSubscription( @Nonnull Enum graph, @Nonnull Object filter )
    throws IllegalStateException;

  /**
   * Update subscription to graph rooted at an instance.
   *
   * @param graph the graph to subscribe to.
   * @param id    the id of the root object.
   * @param filter the filter being updated.
   * @return the subscription entry.
   * @throws IllegalStateException if graph already subscribed to.
   */
  @Nonnull
  ChannelSubscriptionEntry updateSubscription( @Nonnull Enum graph, @Nonnull Object id, @Nonnull Object filter );

  /**
   * Return the subscription for type graph.
   *
   * @param graph the graph.
   * @return the subscription entry.
   * @throws IllegalArgumentException if no such subscription
   */
  @Nonnull
  ChannelSubscriptionEntry getSubscription( @Nonnull Enum graph )
    throws IllegalArgumentException;

  /**
   * Return the subscription for  rooted at an instance.
   *
   * @param graph the graph.
   * @param id    the id of the root object.
   * @return the subscription entry.
   * @throws IllegalArgumentException if no such subscription
   */
  @Nonnull
  ChannelSubscriptionEntry getSubscription( @Nonnull Enum graph, @Nonnull Object id )
    throws IllegalArgumentException;

  /**
   * Find subscription for type graph if it exists.
   *
   * @param graph the graph.
   * @return the subscription entry if it exists, null otherwise.
   */
  @Nullable
  ChannelSubscriptionEntry findSubscription( @Nonnull Enum graph );

  /**
   * Find a graph rooted at an instance.
   *
   * @param graph the graph to look for.
   * @param id    the id of the root object.
   * @return the subscription entry if it exists, null otherwise.
   */
  @Nullable
  ChannelSubscriptionEntry findSubscription( @Nonnull Enum graph, @Nonnull Object id );

  /**
   * Unsubscribe from graph containing types.
   *
   * @param graph the graph to unsubscribe from.
   * @throws IllegalStateException if graph not subscribed to.
   */
  void unsubscribe( @Nonnull Enum graph )
    throws IllegalStateException;

  /**
   * Unsubscribe from graph rooted at an instance.
   *
   * @param graph the graph to unsubscribe from.
   * @param id    the id of the root object.
   * @throws IllegalStateException if graph not subscribed to.
   */
  void unsubscribe( @Nonnull Enum graph, @Nonnull Object id )
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
   * <p/>
   * Note: It is assumed that if an entity is part of a graph, they are always part of the graph.
   * This may not be true with filters but we can assume it for all other scenarios.
   *
   * @param type   the type of the entity.
   * @param id     the id of the entity.
   * @param graphs the graphs that the entity is part of.
   */
  void updateEntity( @Nonnull Class<?> type, @Nonnull Object id, @Nonnull ChannelDescriptor[] graphs );

  /**
   * Disassociate entity from specified graph.
   * <p/>
   * Note: It is assumed that the caller will remove the entity from the subscription manager and
   * repository if there are no more subscriptions.
   *
   * @param type   the type of the entity.
   * @param id     the id of the entity.
   * @param graph the graph that the entity is to be disassociated from.
   * @return the entry representing entities subscription state.
   * @throws IllegalStateException if no such entity or the entity is not associated with the graph.
   */
  @Nonnull
  EntitySubscriptionEntry removeEntityFromGraph( @Nonnull Class<?> type, @Nonnull Object id, @Nonnull ChannelDescriptor graph )
    throws IllegalStateException;

  /**
   * Remove entity and all associated subscriptions.
   *
   * @param type the type of the entity.
   * @param id   the id of the entity.
   */
  void removeEntity( @Nonnull Class<?> type, @Nonnull Object id );
}
