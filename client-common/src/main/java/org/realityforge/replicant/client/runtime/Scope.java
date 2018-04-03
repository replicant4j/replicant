package org.realityforge.replicant.client.runtime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.anodoc.TestOnly;

public final class Scope
{
  private final AreaOfInterestService _areaOfInterestService;
  private final String _name;
  private final Set<ScopeReference> _outwardReferences = new HashSet<>();
  private final Set<ScopeReference> _incomingReferences = new HashSet<>();
  private final Set<SubscriptionReference> _subscriptionReferences = new HashSet<>();
  private boolean _active;

  Scope( @Nonnull final AreaOfInterestService areaOfInterestService, @Nonnull final String name )
  {
    _areaOfInterestService = areaOfInterestService;
    _name = name;
    _active = true;
  }

  public String getName()
  {
    return _name;
  }

  public boolean isActive()
  {
    return _active;
  }

  public boolean hasBeenReleased()
  {
    return !isActive();
  }

  public void release()
  {
    new ArrayList<>( _incomingReferences ).forEach( ScopeReference::release );

    //This next line is required if release is called prior to creating a reference to a scope
    releaseUnreferenced();
  }

  @Nonnull
  public ScopeReference createReference()
  {
    ensureActive();
    final ScopeReference reference = new ScopeReference( this );
    _incomingReferences.add( reference );
    return reference;
  }

  @Nonnull
  public ScopeReference requireScope( @Nonnull final Scope scope )
  {
    ensureActive();
    assert scope != this;
    assert scope.isActive();
    if ( isScopeRequired( scope ) )
    {
      throw new ScopeAlreadyRequiredException();
    }
    else
    {
      final ScopeReference reference = scope.createReference();
      _outwardReferences.add( reference );
      return reference;
    }
  }

  @Nonnull
  public List<Scope> getRequiredScopes()
  {
    return _outwardReferences.stream().
      filter( ScopeReference::isActive ).
      map( ScopeReference::getScope ).
      collect( Collectors.toList() );
  }

  public boolean isScopeRequired( @Nonnull final Scope scope )
  {
    return _outwardReferences.stream().anyMatch( r -> r.isActive() && r.getScope() == scope );
  }

  @Nonnull
  public SubscriptionReference requireSubscription( @Nonnull final Subscription subscription )
  {
    ensureActive();
    assert subscription.isActive();
    purgeReleasedSubscriptionReferences();
    if ( isSubscriptionRequired( subscription ) )
    {
      throw new SubscriptionAlreadyRequiredException( subscription );
    }
    else
    {
      final SubscriptionReference reference = subscription.createReference();
      _subscriptionReferences.add( reference );
      return reference;
    }
  }

  @Nonnull
  public List<Subscription> getRequiredSubscriptions()
  {
    return _subscriptionReferences.stream().
      filter( SubscriptionReference::isActive ).
      map( SubscriptionReference::getSubscription ).
      collect( Collectors.toList() );
  }

  @Nonnull
  public List<Subscription> getRequiredSubscriptionsByGraph( @Nonnull final Enum graph )
  {
    return getRequiredSubscriptions().stream().
      filter( s -> s.getDescriptor().getGraph() == graph ).
      collect( Collectors.toList() );
  }

  public void purgeReleasedSubscriptionReferences()
  {
    _subscriptionReferences.removeIf( SubscriptionReference::hasBeenReleased );
  }

  public boolean isSubscriptionRequired( @Nonnull final Subscription subscription )
  {
    return _subscriptionReferences.stream().anyMatch( r -> r.isActive() && r.getSubscription() == subscription );
  }

  @Nullable
  public SubscriptionReference getSubscriptionReference( @Nonnull final Subscription subscription )
  {
    return _subscriptionReferences.stream().
      filter( r -> r.isActive() && r.getSubscription() == subscription ).findFirst().orElse( null );
  }

  void release( @Nonnull final ScopeReference ScopeReference )
  {
    if ( _incomingReferences.remove( ScopeReference ) )
    {
      releaseUnreferenced();
    }
  }

  /**
   * If there is no references to this scope and no other scope relies upon it
   * then it can be released.
   */
  private void releaseUnreferenced()
  {
    if ( isActive() && 0 == _incomingReferences.size() )
    {
      _areaOfInterestService.destroyScope( this );
    }
  }

  /**
   * Actually mark scope as deleted.
   */
  void delete()
  {
    _subscriptionReferences.forEach( SubscriptionReference::release );
    _subscriptionReferences.clear();
    new ArrayList<>( _outwardReferences ).forEach( ScopeReference::release );
    _active = false;
  }

  private void ensureActive()
  {
    if ( !_active )
    {
      throw new ScopeInactiveException();
    }
  }

  @Override
  public String toString()
  {
    return "Subscription[" +
           _name +
           " :: Active=" + _active +
           ", OutRefCount=" + _outwardReferences.size() +
           ", InRefCount=" + _incomingReferences.size() +
           ", SubRefCount=" + _subscriptionReferences.size() +
           "]";
  }

  @TestOnly
  @Nonnegative
  int getReferenceCount()
  {
    return _incomingReferences.size();
  }

  @TestOnly
  @Nonnegative
  int getSubscriptionReferenceCount()
  {
    return _subscriptionReferences.size();
  }
}
