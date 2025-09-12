package replicant;

import arez.Disposable;
import arez.ObservableValue;
import arez.annotations.ArezComponent;
import arez.annotations.Feature;
import arez.annotations.Observable;
import arez.annotations.ObservableValueRef;
import arez.annotations.PreDispose;
import java.util.ArrayList;
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
@ArezComponent( observable = Feature.ENABLE, requireId = Feature.DISABLE )
public abstract class Entity
  extends ReplicantService
{
  @Nonnull
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
  public String getName()
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
  public Class<?> getType()
  {
    return _type;
  }

  public int getId()
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

  @ObservableValueRef
  abstract ObservableValue<?> getSubscriptionsObservableValue();

  /**
   * Link to subscription if not already subscribed, ignore otherwise.
   */
  void tryLinkToSubscription( @Nonnull final Subscription subscription )
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
  void linkToSubscription( @Nonnull final Subscription subscription )
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
    getSubscriptionsObservableValue().preReportChanged();
    final ChannelAddress address = subscription.getAddress();
    if ( !_subscriptions.containsKey( address ) )
    {
      _subscriptions.put( address, subscription );
      getSubscriptionsObservableValue().reportChanged();
    }
  }

  /**
   * Remove the specified subscription.
   * This is invoked on the client-side by user code when data changes and is now filtered out from the graph.
   * This is only intended to be invoked when changes in data can change a graphs subscription. This occurs when
   * the graph has a mutable routing key or the graph with a filter tpye of INTERNAL has rules that are
   * data dependent. This means that the client has to be responsible for removing subscriptions on the client.
   *
   * @param subscription the subscription.
   */
  public void delinkFromFilteringSubscription( @Nonnull final Subscription subscription )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> ChannelSchema.FilterType.NONE != subscription.getChannelSchema().getFilterType(),
                 () -> "Replicant-0018: Entity.delinkFromFilteringSubscription invoked on Entity " + this +
                       " passing subscription " + subscription.getAddress() + " but subscription is " +
                       "not filtered." );
    }
    delinkFromSubscription( subscription );
  }

  /**
   * Remove the specified subscription.
   *
   * @param subscription the subscription.
   */
  void delinkFromSubscription( @Nonnull final Subscription subscription )
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
   * other means such as {@link Subscription#delinkEntityFromSubscription(Entity, boolean)}.
   *
   * @param subscription the subscription.
   */
  void delinkSubscriptionFromEntity( @Nonnull final Subscription subscription )
  {
    delinkSubscriptionFromEntity( subscription, true );
  }

  private void delinkSubscriptionFromEntity( @Nonnull final Subscription subscription,
                                             final boolean disposeEntityIfNoSubscriptions )
  {
    getSubscriptionsObservableValue().preReportChanged();
    final ChannelAddress address = subscription.getAddress();
    final Subscription candidate = _subscriptions.remove( address );
    getSubscriptionsObservableValue().reportChanged();
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

  void disposeIfNoSubscriptions()
  {
    if ( _subscriptions.isEmpty() )
    {
      Disposable.dispose( this );
    }
  }

  @PreDispose
  void preDispose()
  {
    if ( null != _userObject )
    {
      for ( final Subscription subscription : new ArrayList<>( _subscriptions.values() ) )
      {
        subscription.delinkEntityFromSubscription( this, false );

        final ChannelSchema schema = subscription.getChannelSchema();
        if ( schema.isInstanceChannel() &&
             ( schema.getInstanceType() == getType() ) &&
             ( Objects.equals( subscription.getAddress().rootId(), getId() ) ) )
        {
          // If there is any subscription that this entity is the instance root of, then explicitly dispose it.
          // Historically we used to leave this to removeOrphanedSubscriptions process to clean them up but now
          // downstream code is directly subscribing to subscription objects which may be briefly invalid. when
          // the instance root has been removed but the subscription has not been cleaned up then the Subscription
          // is in a zombie state and accessing methods like getInstanceRoot will cause crashes at best and assertions
          // in development mode. Explicitly unsubscribing from subscriptions will cause any result in almost all that
          // code being skipped as Subscription is no longer observable.
          Disposable.dispose( subscription );
        }
      }
      Disposable.dispose( _userObject );
    }
    if ( Replicant.shouldCheckInvariants() )
    {
      // This is not needed but we do it to make it easier to understand behaviour during debugging
      _subscriptions.clear();
    }
  }

  @Override
  public String toString()
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

  Map<ChannelAddress, Subscription> subscriptions()
  {
    return _subscriptions;
  }
}
