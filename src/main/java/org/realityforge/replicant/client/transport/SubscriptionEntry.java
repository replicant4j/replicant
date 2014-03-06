package org.realityforge.replicant.client.transport;

import javax.annotation.Nullable;

/**
 * An object representing a subscription.
 * The subscription is to a "graph" of objects and any changes to that graph will be replicated
 * to the client from the server via the subscription. The graph is represented by an enum.
 *
 * @param <T> The enum type representing the different graphs.
 */
public class SubscriptionEntry<T extends Enum>
{
  private final T _graph;
  @Nullable
  private final Object _id;
  @Nullable
  private Object _filterParameter;

  /**
   * True if the initial load of data for entity has been downloaded and is local.
   */
  private boolean _present;
  /**
   * True if the subscription is being updated.
   */
  private boolean _subscriptionUpdateInProgress;
  /**
   * True if the subscription is part way through being cancelled.
   */
  private boolean _deregisterInProgress;
  /**
   * True if a deregister has not been completed.
   */
  private boolean _registered;

  public SubscriptionEntry( final T graph, final Object id )
  {
    _graph = graph;
    _id = id;
    _registered = true;
  }

  public void setFilterParameter( @Nullable final Object filterParameter )
  {
    _filterParameter = filterParameter;
  }

  @Nullable
  public Object getFilterParameter()
  {
    return _filterParameter;
  }

  public T getGraph()
  {
    return _graph;
  }

  @Nullable
  public Object getId()
  {
    return _id;
  }

  public boolean isPresent()
  {
    return _present;
  }

  public void markAsPresent()
  {
    _present = true;
  }

  public boolean isSubscriptionUpdateInProgress()
  {
    return _subscriptionUpdateInProgress;
  }

  public void setSubscriptionUpdateInProgress( final boolean subscriptionUpdateInProgress )
  {
    _subscriptionUpdateInProgress = subscriptionUpdateInProgress;
  }

  public boolean isDeregisterInProgress()
  {
    return _deregisterInProgress;
  }

  public void markDeregisterInProgress()
  {
    _deregisterInProgress = true;
  }

  public boolean isRegistered()
  {
    return _registered;
  }

  public void markAsDeregistered()
  {
    _registered = false;
  }
}
