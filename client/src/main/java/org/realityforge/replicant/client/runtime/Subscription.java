package org.realityforge.replicant.client.runtime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.ChannelAddress;
import org.realityforge.replicant.client.FilterUtil;

/**
 * Represents a subscription that a client explicitly subscribes to.
 */
public final class Subscription
{
  private final AreaOfInterestService _areaOfInterestService;
  @Nonnull
  private final ChannelAddress _descriptor;
  @Nullable
  private Object _filter;
  private final Set<SubscriptionReference> _outwardReferences = new HashSet<>();
  private final Set<SubscriptionReference> _incomingReferences = new HashSet<>();
  private boolean _active;

  public Subscription( @Nonnull final AreaOfInterestService areaOfInterestService,
                       @Nonnull final ChannelAddress descriptor )
  {
    _areaOfInterestService = areaOfInterestService;
    _descriptor = descriptor;
    _active = true;
    //This next line is required as otherwise GWT will leave it as undefined
    // which means we can not JSON.stringify the value and match a null value
    _filter = null;
  }

  public boolean isActive()
  {
    return _active;
  }

  public boolean hasBeenReleased()
  {
    return !isActive();
  }

  @Nonnull
  public ChannelAddress getDescriptor()
  {
    return _descriptor;
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

  public int getReferenceCount()
  {
    return _incomingReferences.size();
  }

  @Nonnull
  public SubscriptionReference createReference()
  {
    ensureActive();
    final SubscriptionReference reference = new SubscriptionReference( this );
    _incomingReferences.add( reference );
    return reference;
  }

  @Nonnull
  public List<Subscription> getRequiredSubscriptions()
  {
    return _outwardReferences.stream().
      filter( SubscriptionReference::isActive ).
      map( SubscriptionReference::getSubscription ).
      collect( Collectors.toList() );
  }

  public void release()
  {
    new ArrayList<>( _incomingReferences ).forEach( SubscriptionReference::release );

    //This next line is required if release is called prior to creating a reference to a subscription
    releaseUnreferenced();
  }

  void release( @Nonnull final SubscriptionReference subscriptionReference )
  {
    _incomingReferences.remove( subscriptionReference );
    releaseUnreferenced();
  }

  @Nonnull
  public SubscriptionReference requireSubscription( @Nonnull final Subscription subscription )
  {
    ensureActive();
    assert subscription != this;
    assert subscription.isActive();
    if ( isSubscriptionRequired( subscription ) )
    {
      throw new SubscriptionAlreadyRequiredException( subscription );
    }
    else
    {
      final SubscriptionReference reference = subscription.createReference();
      _outwardReferences.add( reference );
      return reference;
    }
  }

  public boolean isSubscriptionRequired( @Nonnull final Subscription subscription )
  {
    return _outwardReferences.stream().anyMatch( r -> r.getSubscription() == subscription );
  }

  /**
   * If there is no references to this subscription and no other subscription relies upon it
   * then it can be unsubscribed.
   */
  private void releaseUnreferenced()
  {
    if ( isActive() && 0 == _incomingReferences.size() )
    {
      _areaOfInterestService.destroySubscription( this );
    }
  }

  void delete()
  {
    new ArrayList<>( _outwardReferences ).forEach( SubscriptionReference::release );
    _active = false;
  }

  private void ensureActive()
  {
    if ( !_active )
    {
      throw new SubscriptionInactiveException( this );
    }
  }

  @Override
  public String toString()
  {
    return "Subscription[" +
           _descriptor +
           " :: Filter=" + FilterUtil.filterToString( _filter  ) +
           ", Active=" + _active +
           ", OutRefCount=" + _outwardReferences.size() +
           ", InRefCount=" + _incomingReferences.size() +
           "]";
  }
}
