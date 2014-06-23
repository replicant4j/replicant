package org.realityforge.replicant.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Representation of a subscription to a graph.
 */
public final class GraphSubscriptionEntry
{
  private final GraphDescriptor _descriptor;
  @Nullable
  private Object _filter;

  private final Map<Class<?>, Map<Object, EntitySubscriptionEntry>> _entities =
    new HashMap<Class<?>, Map<Object, EntitySubscriptionEntry>>();
  private final Map<Class<?>, Map<Object, EntitySubscriptionEntry>> _roEntities =
    Collections.unmodifiableMap( _entities );

  public GraphSubscriptionEntry( @Nonnull final GraphDescriptor descriptor,
                                 @Nullable final Object filter )
  {
    _descriptor = descriptor;
    _filter = filter;
  }

  public GraphDescriptor getDescriptor()
  {
    return _descriptor;
  }

  void setFilter( final Object filter )
  {
    _filter = filter;
  }

  public Object getFilter()
  {
    return _filter;
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
