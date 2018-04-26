package org.realityforge.replicant.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Representation of a subscriptions that impact entity.
 */
public class EntitySubscriptionEntry
{
  private final Map<ChannelAddress, ChannelSubscriptionEntry> _graphSubscriptions = new HashMap<>();
  private final Map<ChannelAddress, ChannelSubscriptionEntry> _roGraphSubscriptions =
    Collections.unmodifiableMap( _graphSubscriptions );
  private final Class<?> _type;
  private final Object _id;
  private Object _entity;

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

  @Nullable
  public Object getEntity()
  {
    return _entity;
  }

  public void setEntity( @Nonnull final Object entity )
  {
    _entity = Objects.requireNonNull( entity );
  }

  public Map<ChannelAddress, ChannelSubscriptionEntry> getGraphSubscriptions()
  {
    return _roGraphSubscriptions;
  }

  public Map<ChannelAddress, ChannelSubscriptionEntry> getRwGraphSubscriptions()
  {
    return _graphSubscriptions;
  }

  @Nullable
  public ChannelSubscriptionEntry deregisterGraph( @Nonnull final ChannelAddress descriptor )
  {
    return getRwGraphSubscriptions().remove( descriptor );
  }
}
