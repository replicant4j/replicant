package org.realityforge.replicant.client.subscription;

import arez.Arez;
import arez.Disposable;
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
  private final Map<ChannelAddress, Subscription> _subscriptions = new HashMap<>();
  private final Class<?> _type;
  private final Object _id;
  private Object _userObject;

  static Entity create( @Nonnull final Class<?> type, @Nonnull final Object id )
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
   * Return the collection of subscriptions for the entity.
   *
   * @return the subscriptions.
   */
  @Nonnull
  @Observable( expectSetter = false )
  public Collection<Subscription> getSubscriptions()
  {
    // This return result is already immutable as it is part of map so no need to convert to immutable
    return _subscriptions.values();
  }

  @ObservableRef
  protected abstract arez.Observable getSubscriptionsObservable();

  /**
   * Link to subscription if it does not exist.
   *
   * @param subscription the subscription.
   */
  final void linkToSubscription( @Nonnull final Subscription subscription )
  {
    linkEntityToSubscription( subscription );
    subscription.linkSubscriptionToEntity( this );
  }

  private void linkEntityToSubscription( @Nonnull final Subscription subscription )
  {
    getSubscriptionsObservable().preReportChanged();
    final ChannelAddress address = subscription.getChannel().getAddress();
    if ( !_subscriptions.containsKey( address ) )
    {
      _subscriptions.put( address, subscription );
      getSubscriptionsObservable().reportChanged();
    }
  }

  /**
   * Remove the specified subscription.
   *
   * @param subscription the subscription.
   */
  public final void delinkFromSubscription( @Nonnull final Subscription subscription )
  {
    subscription.delinkEntityFromSubscription( this );
    delinkSubscriptionFromEntity( subscription );
  }

  /**
   * Delink the specified subscription from this entity.
   * This method does not delink entity from subscription and it is assumed this is achieved through
   * other means such as {@link Subscription#delinkEntityFromSubscription(Entity)}.
   *
   * @param subscription the subscription.
   */
  final void delinkSubscriptionFromEntity( @Nonnull final Subscription subscription )
  {
    getSubscriptionsObservable().preReportChanged();
    final ChannelAddress address = subscription.getChannel().getAddress();
    final Subscription candidate = _subscriptions.remove( address );
    getSubscriptionsObservable().reportChanged();
    if ( BrainCheckConfig.checkApiInvariants() )
    {
      apiInvariant( () -> null != candidate,
                    () -> "Unable to locate subscription for channel " + address + " on entity " + this );
    }
    if ( _subscriptions.isEmpty() )
    {
      Disposable.dispose( this );
    }
  }

  @PreDispose
  final void preDispose()
  {
    delinkEntityFromAllSubscriptions();
    if ( null != _userObject )
    {
      Disposable.dispose( _userObject );
    }
  }

  private void delinkEntityFromAllSubscriptions()
  {
    _subscriptions.values().forEach( subscription -> subscription.delinkEntityFromSubscription( this ) );
  }

  @Override
  public final String toString()
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
  final Map<ChannelAddress, Subscription> subscriptions()
  {
    return _subscriptions;
  }
}
