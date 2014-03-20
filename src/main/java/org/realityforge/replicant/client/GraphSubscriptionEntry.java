package org.realityforge.replicant.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Representation of a subscription to a graph.
 */
public class GraphSubscriptionEntry
{
  private final GraphDescriptor _descriptor;

  @Nullable
  private final Map<Class<?>, Map<Object, EntitySubscriptionEntry>> _entities =
    new HashMap<Class<?>, Map<Object, EntitySubscriptionEntry>>();
  private final Map<Class<?>, Map<Object, EntitySubscriptionEntry>> _roEntities =
    Collections.unmodifiableMap( _entities );

  public GraphSubscriptionEntry( final GraphDescriptor descriptor )
  {
    _descriptor = descriptor;
  }

  public GraphDescriptor getDescriptor()
  {
    return _descriptor;
  }

  public Map<Class<?>, Map<Object, EntitySubscriptionEntry>> getEntities()
  {
    return _roEntities;
  }

  final Map<Class<?>, Map<Object, EntitySubscriptionEntry>> getRwEntities()
  {
    return _entities;
  }
}
