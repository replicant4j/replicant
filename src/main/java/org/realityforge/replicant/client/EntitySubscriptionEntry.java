package org.realityforge.replicant.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * Representation of a subscriptions that impact entity.
 */
public class EntitySubscriptionEntry
{
  private final Map<ChannelDescriptor, ChannelSubscriptionEntry> _graphSubscriptions = new HashMap<>();
  private final Map<ChannelDescriptor, ChannelSubscriptionEntry> _roGraphSubscriptions =
    Collections.unmodifiableMap( _graphSubscriptions );
  private final Class<?> _type;
  private final Object _id;

  public EntitySubscriptionEntry( @Nonnull final Class<?> type, @Nonnull final Object id )
  {
    _type = type;
    _id = id;
  }

  @Nonnull
  public Class<?> getType()
  {
    return _type;
  }

  @Nonnull
  public Object getID()
  {
    return _id;
  }

  public Map<ChannelDescriptor, ChannelSubscriptionEntry> getGraphSubscriptions()
  {
    return _roGraphSubscriptions;
  }

  final Map<ChannelDescriptor, ChannelSubscriptionEntry> getRwGraphSubscriptions()
  {
    return _graphSubscriptions;
  }
}
