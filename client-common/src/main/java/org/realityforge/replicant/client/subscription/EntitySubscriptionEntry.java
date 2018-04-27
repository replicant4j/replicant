package org.realityforge.replicant.client.subscription;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.ChannelAddress;

/**
 * Representation of a subscriptions that impact entity.
 */
public class EntitySubscriptionEntry
{
  private final Map<ChannelAddress, ChannelSubscriptionEntry> _channelSubscriptions = new HashMap<>();
  private final Map<ChannelAddress, ChannelSubscriptionEntry> _roChannelSubscriptions =
    Collections.unmodifiableMap( _channelSubscriptions );
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

  public Map<ChannelAddress, ChannelSubscriptionEntry> getChannelSubscriptions()
  {
    return _roChannelSubscriptions;
  }

  public Map<ChannelAddress, ChannelSubscriptionEntry> getRwChannelSubscriptions()
  {
    return _channelSubscriptions;
  }

  @Nullable
  public ChannelSubscriptionEntry deregisterChannel( @Nonnull final ChannelAddress address )
  {
    return getRwChannelSubscriptions().remove( address );
  }
}
