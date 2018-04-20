package org.realityforge.replicant.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Representation of a subscription to a graph.
 */
public final class ChannelSubscriptionEntry
{
  private final ChannelAddress _address;
  @Nullable
  private Object _filter;
  private boolean _explicitSubscription;

  private final Map<Class<?>, Map<Object, EntitySubscriptionEntry>> _entities =
    new HashMap<>();
  private final Map<Class<?>, Map<Object, EntitySubscriptionEntry>> _roEntities =
    Collections.unmodifiableMap( _entities );

  public ChannelSubscriptionEntry( @Nonnull final ChannelAddress address,
                                   @Nullable final Object filter,
                                   final boolean explicitSubscription )
  {
    _address = Objects.requireNonNull( address );
    _filter = filter;
    _explicitSubscription = explicitSubscription;
  }

  public ChannelAddress getAddress()
  {
    return _address;
  }

  void setFilter( @Nullable final Object filter )
  {
    _filter = filter;
  }

  @Nullable
  public Object getFilter()
  {
    return _filter;
  }

  public boolean isExplicitSubscription()
  {
    return _explicitSubscription;
  }

  public void setExplicitSubscription( final boolean explicitSubscription )
  {
    _explicitSubscription = explicitSubscription;
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
