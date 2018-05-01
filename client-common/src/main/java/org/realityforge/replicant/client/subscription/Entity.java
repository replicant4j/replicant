package org.realityforge.replicant.client.subscription;

import arez.Arez;
import arez.annotations.ArezComponent;
import arez.annotations.Observable;
import arez.annotations.ObservableRef;
import arez.annotations.PreDispose;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.anodoc.TestOnly;
import org.realityforge.braincheck.BrainCheckConfig;
import org.realityforge.replicant.client.ChannelAddress;
import static org.realityforge.braincheck.Guards.*;

/**
 * A representation of an entity within the replicant system.
 */
@ArezComponent
public abstract class Entity
{
  private final Map<ChannelAddress, ChannelSubscriptionEntry> _channelSubscriptions = new HashMap<>();
  private final Class<?> _type;
  private final Object _id;
  private Object _userObject;

  public static Entity create( @Nonnull final Class<?> type, @Nonnull final Object id )
  {
    return new Arez_Entity( type, id );
  }

  Entity( @Nonnull final Class<?> type, @Nonnull final Object id )
  {
    _type = type;
    _id = id;
  }

  @Nonnull
  public final Class<?> getType()
  {
    return _type;
  }

  @Nonnull
  public final Object getId()
  {
    return _id;
  }

  @Observable
  @Nullable
  public Object getUserObject()
  {
    return _userObject;
  }

  public void setUserObject( @Nullable final Object userObject )
  {
    if ( BrainCheckConfig.checkApiInvariants() )
    {
      if ( null != userObject )
      {
        //noinspection NonJREEmulationClassesInClientCode
        apiInvariant( () -> getType().isInstance( userObject ),
                      () -> "Entity " + this + " specified non-null userObject of type " +
                            userObject.getClass().getName() + " but the entity expected type " +
                            _type.getName() );
      }
    }
    _userObject = userObject;
  }

  /**
   * Return the collection of channel subscriptions for the entity.
   *
   * @return the channel subscriptions.
   */
  @Nonnull
  @Observable( expectSetter = false )
  public Collection<ChannelSubscriptionEntry> getChannelSubscriptions()
  {
    // This return result is already immutable as it is part of map so no need to convert to immutable
    return _channelSubscriptions.values();
  }

  @ObservableRef
  protected abstract arez.Observable getChannelSubscriptionsObservable();

  /**
   * Add channel subscription if it does not exist.
   *
   * @param subscription the channel subscription.
   */
  void addChannelSubscription( @Nonnull final ChannelSubscriptionEntry subscription )
  {
    linkEntityToChannel( subscription );
    linkChannelToEntity( subscription );
  }

  private void linkChannelToEntity( @Nonnull final ChannelSubscriptionEntry subscription )
  {
    subscription.getRwEntities().computeIfAbsent( getType(), k -> new HashMap<>() ).put( getId(), this );
  }

  private void linkEntityToChannel( @Nonnull final ChannelSubscriptionEntry subscription )
  {
    getChannelSubscriptionsObservable().preReportChanged();
    final ChannelAddress address = subscription.getChannel().getAddress();
    if ( !_channelSubscriptions.containsKey( address ) )
    {
      _channelSubscriptions.put( address, subscription );
      getChannelSubscriptionsObservable().reportChanged();
    }
  }

  /**
   * Remove specified channel subscription.
   *
   * @param subscription the channel subscription.
   */
  void removeChannelSubscription( @Nonnull final ChannelSubscriptionEntry subscription )
  {
    delinkChannelFromEntity( subscription );
    delinkEntityFromChannel( subscription );
  }

  /**
   * De-register the specified channel subscription from this entity.
   * This method does not deregister entity from channel and it is assumed this is achieved through
   * other means such as {@link #delinkEntityFromChannel(ChannelSubscriptionEntry)}.
   *
   * @param subscription the channel subscription.
   */
  void delinkChannelFromEntity( @Nonnull final ChannelSubscriptionEntry subscription )
  {
    getChannelSubscriptionsObservable().preReportChanged();
    final ChannelAddress address = subscription.getChannel().getAddress();
    final ChannelSubscriptionEntry candidate = _channelSubscriptions.remove( address );
    getChannelSubscriptionsObservable().reportChanged();
    if ( BrainCheckConfig.checkApiInvariants() )
    {
      apiInvariant( () -> null != candidate,
                    () -> "Unable to locate subscription for channel " + address + " on entity " + this );
    }
  }

  /**
   * Deregister this entity from the specified channel subscription.
   * This method does not deregister channel from entity and it is assumed this is achieved through
   * other means such as {@link #delinkChannelFromEntity(ChannelSubscriptionEntry)}.
   *
   * @param subscription the channel subscription.
   */
  final void delinkEntityFromChannel( @Nonnull final ChannelSubscriptionEntry subscription )
  {
    final Map<Class<?>, Map<Object, Entity>> map = subscription.getRwEntities();
    final Map<Object, Entity> typeMap = map.get( _type );
    final ChannelAddress address = subscription.getChannel().getAddress();
    if ( BrainCheckConfig.checkInvariants() )
    {
      invariant( () -> null != typeMap,
                 () -> "Entity type " + _type.getSimpleName() + " not present in channel " + address );
    }
    assert null != typeMap;
    final Entity entity = typeMap.remove( _id );
    if ( BrainCheckConfig.checkInvariants() )
    {
      invariant( () -> null != entity,
                 () -> "Entity instance " + this + " not present in channel " + address );
    }
    if ( typeMap.isEmpty() )
    {
      map.remove( _type );
    }
  }

  @PreDispose
  final void preDispose()
  {
    removeFromAllChannels();
  }

  private void removeFromAllChannels()
  {
    _channelSubscriptions.values().forEach( this::delinkEntityFromChannel );
  }

  @Override
  public String toString()
  {
    if ( Arez.areNamesEnabled() )
    {
      return _type.getSimpleName() + "/" + _id;
    }
    else
    {
      return super.toString();
    }
  }

  @TestOnly
  final Map<ChannelAddress, ChannelSubscriptionEntry> channelSubscriptions()
  {
    return _channelSubscriptions;
  }
}
