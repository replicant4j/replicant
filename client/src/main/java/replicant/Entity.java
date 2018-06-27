package replicant;

import arez.Disposable;
import arez.annotations.ArezComponent;
import arez.annotations.Feature;
import arez.annotations.Observable;
import arez.annotations.ObservableRef;
import arez.annotations.PreDispose;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static org.realityforge.braincheck.Guards.*;

/**
 * A representation of an entity within the replicant system.
 */
@ArezComponent( observable = Feature.ENABLE )
public abstract class Entity
  extends ReplicantService
{
  private final Map<ChannelAddress, Subscription> _subscriptions = new HashMap<>();
  /**
   * A human consumable name for Entity. It should be non-null if {@link Replicant#areNamesEnabled()} returns
   * true and <tt>null</tt> otherwise.
   */
  @Nullable
  private final String _name;
  @Nonnull
  private final Class<?> _type;
  private final int _id;
  private Object _userObject;

  static Entity create( @Nullable final ReplicantContext context,
                        @Nullable final String name,
                        @Nonnull final Class<?> type,
                        final int id )
  {
    return new Arez_Entity( context, name, type, id );
  }

  Entity( @Nullable final ReplicantContext context,
          @Nullable final String name,
          @Nonnull final Class<?> type,
          final int id )
  {
    super( context );
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> Replicant.areNamesEnabled() || null == name,
                    () -> "Replicant-0032: Entity passed a name '" + name +
                          "' but Replicant.areNamesEnabled() is false" );
    }
    _name = Replicant.areNamesEnabled() ? Objects.requireNonNull( name ) : null;
    _type = Objects.requireNonNull( type );
    _id = id;
  }

  /**
   * Return the name of the Entity.
   * This method should NOT be invoked unless {@link Replicant#areNamesEnabled()} returns true and will throw an
   * exception if invariant checking is enabled.
   *
   * @return the name of the Entity.
   */
  @Nonnull
  public final String getName()
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( Replicant::areNamesEnabled,
                    () -> "Replicant-0009: Entity.getName() invoked when Replicant.areNamesEnabled() is false" );
    }
    assert null != _name;
    return _name;
  }

  @Nonnull
  public final Class<?> getType()
  {
    return _type;
  }

  public final int getId()
  {
    return _id;
  }

  @Nonnull
  public Object getUserObject()
  {
    final Object userObject = maybeUserObject();
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> null != userObject,
                    () -> "Replicant-0071: Entity.getUserObject() invoked when no userObject present" );
    }
    assert null != userObject;
    return userObject;
  }

  @Observable( name = "userObject" )
  @Nullable
  public Object maybeUserObject()
  {
    return _userObject;
  }

  void setUserObject( @Nullable final Object userObject )
  {
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
  abstract arez.Observable getSubscriptionsObservable();

  /**
   * Link to subscription if not already subscribed, ignore otherwise.
   */
  final void tryLinkToSubscription( @Nonnull final Subscription subscription )
  {
    if ( !_subscriptions.containsKey( subscription.getAddress() ) )
    {
      linkToSubscription( subscription );
    }
  }

  /**
   * Link to subscription if it does not exist.
   *
   * @param subscription the subscription.
   */
  final void linkToSubscription( @Nonnull final Subscription subscription )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> null == subscription.findEntityByTypeAndId( getType(), getId() ),
                 () -> "Replicant-0080: Entity.linkToSubscription invoked on Entity " + this +
                       " passing subscription " + subscription.getAddress() + " but entity is " +
                       "already linked to subscription." );
    }
    linkEntityToSubscription( subscription );
    subscription.linkSubscriptionToEntity( this );
  }

  private void linkEntityToSubscription( @Nonnull final Subscription subscription )
  {
    getSubscriptionsObservable().preReportChanged();
    final ChannelAddress address = subscription.getAddress();
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
  final void delinkFromSubscription( @Nonnull final Subscription subscription )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> null != subscription.findEntityByTypeAndId( getType(), getId() ),
                 () -> "Replicant-0081: Entity.delinkFromSubscription invoked on Entity " + this +
                       " passing subscription " + subscription.getAddress() + " but entity is " +
                       "not linked to subscription." );
    }
    delinkSubscriptionFromEntity( subscription, false );
    subscription.delinkEntityFromSubscription( this, false );
    disposeIfNoSubscriptions();
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
    delinkSubscriptionFromEntity( subscription, true );
  }

  private void delinkSubscriptionFromEntity( @Nonnull final Subscription subscription,
                                             final boolean disposeEntityIfNoSubscriptions )
  {
    getSubscriptionsObservable().preReportChanged();
    final ChannelAddress address = subscription.getAddress();
    final Subscription candidate = _subscriptions.remove( address );
    getSubscriptionsObservable().reportChanged();
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> null != candidate,
                    () -> "Unable to locate subscription for channel " + address + " on entity " + this );
    }
    if ( disposeEntityIfNoSubscriptions )
    {
      disposeIfNoSubscriptions();
    }
  }

  final void disposeIfNoSubscriptions()
  {
    if ( _subscriptions.isEmpty() )
    {
      Disposable.dispose( this );
    }
  }

  @PreDispose
  final void preDispose()
  {
    if ( null != _userObject )
    {
      Disposable.dispose( _userObject );
    }
  }

  void delinkEntityFromAllSubscriptions()
  {
    _subscriptions.values().forEach( subscription -> subscription.delinkEntityFromSubscription( this ) );
    _subscriptions.clear();
  }

  @Override
  public final String toString()
  {
    if ( Replicant.areNamesEnabled() )
    {
      return getName();
    }
    else
    {
      return super.toString();
    }
  }

  final Map<ChannelAddress, Subscription> subscriptions()
  {
    return _subscriptions;
  }
}
