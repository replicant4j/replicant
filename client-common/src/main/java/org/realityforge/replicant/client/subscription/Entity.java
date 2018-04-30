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
public class Entity
{
  private final Map<ChannelAddress, ChannelSubscriptionEntry> _channelSubscriptions = new HashMap<>();
  private final Map<ChannelAddress, ChannelSubscriptionEntry> _roChannelSubscriptions =
    Collections.unmodifiableMap( _channelSubscriptions );
  private final Class<?> _type;
  private final Object _id;
  private Object _userObject;

  public Entity( @Nonnull final Class<?> type, @Nonnull final Object id )
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
  public Object getId()
  {
    return _id;
  }

  @Nullable
  public Object getUserObject()
  {
    return _userObject;
  }

  public void setUserObject( @Nonnull final Object userObject )
  {
    _userObject = Objects.requireNonNull( userObject );
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
