package org.realityforge.replicant.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Representation of a subscriptions that impact entity.
 */
public class EntitySubscriptionEntry
{
  private final Class<?> _type;
  @Nullable
  private final Object _id;

  @Nullable
  private final Map<GraphDescriptor, GraphSubscriptionEntry> _graphSubscriptions =
    new HashMap<GraphDescriptor, GraphSubscriptionEntry>();
  private final Map<GraphDescriptor, GraphSubscriptionEntry> _roGraphSubscriptions =
    Collections.unmodifiableMap( _graphSubscriptions );

  public EntitySubscriptionEntry( final Class<?> type, final Object id )
  {
    _type = type;
    _id = id;
  }

  public Class<?> getType()
  {
    return _type;
  }

  @Nullable
  public Object getID()
  {
    return _id;
  }

  public Map<GraphDescriptor, GraphSubscriptionEntry> getGraphSubscriptions()
  {
    return _roGraphSubscriptions;
  }

  final Map<GraphDescriptor, GraphSubscriptionEntry> getRwGraphSubscriptions()
  {
    return _graphSubscriptions;
  }
}
