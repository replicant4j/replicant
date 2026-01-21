package replicant.react4j;

import arez.annotations.Action;
import arez.annotations.ComponentDependency;
import arez.annotations.Observable;
import arez.annotations.PreDispose;
import arez.annotations.SuppressArezWarnings;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import react4j.annotations.PostMountOrUpdate;
import replicant.AreaOfInterest;
import replicant.ChannelAddress;
import replicant.Replicant;
import replicant.ReplicantContext;

/**
 * An abstract React4j component that manages subscription to channels.
 */
@SuppressWarnings( { "WeakerAccess", "unused" } )
public abstract class ReplicantSubscription
{
  //The warning is suppressed as reference is managed on method.
  // We can not convert this field into abstract observable because of some surgery do to work between
  // React/Arez component models.
  @SuppressArezWarnings( "Arez:UnmanagedComponentReference" )
  @Nullable
  private AreaOfInterest _areaOfInterest;

  @Nullable
  protected Object getFilter()
  {
    return null;
  }

  @ComponentDependency( action = ComponentDependency.Action.SET_NULL )
  @Observable
  @Nullable
  protected AreaOfInterest getAreaOfInterest()
  {
    return _areaOfInterest;
  }

  protected void setAreaOfInterest( @Nullable final AreaOfInterest areaOfInterest )
  {
    _areaOfInterest = areaOfInterest;
  }

  @PreDispose
  protected final void preDispose()
  {
    if ( null != _areaOfInterest )
    {
      _areaOfInterest.decRefCount();
      _areaOfInterest = null;
    }
  }

  @PostMountOrUpdate
  protected final void postMountOrUpdate()
  {
    updateAreaOfInterest();
  }

  protected final void updateAreaOfInterestOnIdChange()
  {
    clearAreaOfInterest();
  }

  protected final void updateAreaOfInterestOnFilterChange( @Nullable final Object newFilter )
  {
    if ( null != _areaOfInterest )
    {
      Replicant.context().createOrUpdateAreaOfInterest( _areaOfInterest.getAddress(), newFilter );
    }
  }

  @Action
  protected void clearAreaOfInterest()
  {
    final AreaOfInterest areaOfInterest = getAreaOfInterest();
    if ( null != areaOfInterest )
    {
      setAreaOfInterest( null );
      areaOfInterest.decRefCount();
    }
  }

  @Action
  protected void updateAreaOfInterest()
  {
    final ReplicantContext context = Replicant.context();
    final AreaOfInterest newAreaOfInterest = context.createOrUpdateAreaOfInterest( getAddress(), getFilter() );
    if ( null == _areaOfInterest )
    {
      newAreaOfInterest.incRefCount();
      setAreaOfInterest( newAreaOfInterest );
    }
  }

  @Nonnull
  protected abstract ChannelAddress getAddress();
}
