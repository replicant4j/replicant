package replicant.react4j;

import arez.annotations.Action;
import arez.annotations.ComponentDependency;
import arez.annotations.Memoize;
import arez.annotations.Observable;
import arez.annotations.PreDispose;
import arez.annotations.SuppressArezWarnings;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import react4j.ReactNode;
import react4j.annotations.PostMountOrUpdate;
import react4j.annotations.Render;
import replicant.AreaOfInterest;
import replicant.ChannelAddress;
import replicant.Replicant;
import replicant.Subscription;

/**
 * An abstract React4j component that manages subscription to channels.
 */
@SuppressWarnings( { "WeakerAccess", "unused" } )
public abstract class ReplicantSubscription<T>
{
  //The warning is suppressed as reference is managed on method.
  // We can not convert this field into abstract observable because of some surgery do to work between
  // React/Arez component models.
  @SuppressArezWarnings( "Arez:UnmanagedComponentReference" )
  @Nullable
  private AreaOfInterest _areaOfInterest;

  @FunctionalInterface
  public interface NoResultCallback
  {
    @Nullable
    ReactNode render();
  }

  @FunctionalInterface
  public interface TypeResultCallback
  {
    @Nullable
    ReactNode render( @Nonnull Subscription subscription );
  }

  @FunctionalInterface
  public interface InstanceResultCallback<T>
  {
    @Nullable
    ReactNode render( @Nonnull Subscription subscription, @Nonnull T rootEntity );
  }

  @FunctionalInterface
  public interface ErrorCallback
  {
    @Nullable
    ReactNode render( @Nonnull Throwable error );
  }

  @FunctionalInterface
  public interface TypeUpdateErrorCallback
  {
    @Nullable
    ReactNode render( @Nonnull Subscription subscription, @Nonnull Throwable error );
  }

  @FunctionalInterface
  public interface InstanceUpdateErrorCallback<T>
  {
    @Nullable
    ReactNode render( @Nonnull Subscription subscription, @Nonnull T rootEntity, @Nonnull Throwable error );
  }

  @Nullable
  protected Object getFilter()
  {
    return null;
  }

  @Nonnull
  protected abstract NoResultCallback getOnLoading();

  @Nonnull
  protected abstract ErrorCallback getOnLoadFailed();

  @Nonnull
  protected TypeResultCallback getOnTypeSubscriptionLoaded()
  {
    throw new IllegalStateException();
  }

  @Nonnull
  protected InstanceResultCallback<T> getOnInstanceSubscriptionLoaded()
  {
    throw new IllegalStateException();
  }

  @Nonnull
  protected TypeResultCallback getOnTypeSubscriptionUpdating()
  {
    throw new IllegalStateException();
  }

  @Nonnull
  protected InstanceResultCallback<T> getOnInstanceSubscriptionUpdating()
  {
    throw new IllegalStateException();
  }

  @Nonnull
  protected TypeUpdateErrorCallback getOnTypeSubscriptionUpdateFailed()
  {
    throw new IllegalStateException();
  }

  @Nonnull
  protected InstanceUpdateErrorCallback<T> getOnInstanceSubscriptionUpdateFailed()
  {
    throw new IllegalStateException();
  }

  @Nonnull
  protected TypeResultCallback getOnTypeSubscriptionUpdated()
  {
    throw new IllegalStateException();
  }

  @Nonnull
  protected InstanceResultCallback<T> getOnInstanceSubscriptionUpdated()
  {
    throw new IllegalStateException();
  }

  @Nonnull
  protected TypeResultCallback getOnTypeSubscriptionUnloading()
  {
    throw new IllegalStateException();
  }

  @Nonnull
  protected InstanceResultCallback<T> getOnInstanceSubscriptionUnloading()
  {
    throw new IllegalStateException();
  }

  @Nonnull
  protected abstract NoResultCallback getOnUnloaded();

  @Nonnull
  protected abstract NoResultCallback getOnDeleted();

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
    final AreaOfInterest newAreaOfInterest =
      Replicant.context().createOrUpdateAreaOfInterest( getAddress(), getFilter() );
    if ( null == _areaOfInterest )
    {
      newAreaOfInterest.incRefCount();
      setAreaOfInterest( newAreaOfInterest );
    }
  }

  @Nonnull
  protected abstract ChannelAddress getAddress();

  @Render
  @Nullable
  protected ReactNode render()
  {
    final AreaOfInterest areaOfInterest = getAreaOfInterest();
    if ( null == areaOfInterest )
    {
      return null;
    }
    else
    {
      final AreaOfInterest.Status status = getStatus();

      final ChannelAddress address = areaOfInterest.getAddress();
      final boolean isInstanceChannel =
        Replicant.context()
          .getSchemaById( address.getSchemaId() )
          .getChannel( address.getChannelId() )
          .isInstanceChannel();

      if ( AreaOfInterest.Status.NOT_ASKED == status || AreaOfInterest.Status.LOADING == status )
      {
        return getOnLoading().render();
      }
      else if ( AreaOfInterest.Status.LOADED == status )
      {
        final Subscription subscription = Objects.requireNonNull( areaOfInterest.getSubscription() );
        return isInstanceChannel ?
               getOnInstanceSubscriptionLoaded().render( subscription, getInstanceRoot( subscription ) ) :
               getOnTypeSubscriptionLoaded().render( subscription );
      }
      else if ( AreaOfInterest.Status.LOAD_FAILED == status )
      {
        return getOnLoadFailed().render( Objects.requireNonNull( areaOfInterest.getError() ) );
      }
      else if ( AreaOfInterest.Status.UPDATING == status )
      {
        final Subscription subscription = Objects.requireNonNull( areaOfInterest.getSubscription() );
        return isInstanceChannel ?
               getOnInstanceSubscriptionUpdating().render( subscription, getInstanceRoot( subscription ) ) :
               getOnTypeSubscriptionUpdating().render( subscription );
      }
      else if ( AreaOfInterest.Status.UPDATED == status )
      {
        final Subscription subscription = Objects.requireNonNull( areaOfInterest.getSubscription() );
        return isInstanceChannel ?
               getOnInstanceSubscriptionUpdated().render( subscription, getInstanceRoot( subscription ) ) :
               getOnTypeSubscriptionUpdated().render( subscription );
      }
      else if ( AreaOfInterest.Status.UPDATE_FAILED == status )
      {
        final Subscription subscription = Objects.requireNonNull( areaOfInterest.getSubscription() );
        final Throwable error = Objects.requireNonNull( areaOfInterest.getError() );
        return isInstanceChannel ?
               getOnInstanceSubscriptionUpdateFailed().render( subscription,
                                                               getInstanceRoot( subscription ),
                                                               error ) :
               getOnTypeSubscriptionUpdateFailed().render( subscription, error );
      }
      else if ( AreaOfInterest.Status.UNLOADING == status )
      {
        final Subscription subscription = Objects.requireNonNull( areaOfInterest.getSubscription() );
        return isInstanceChannel ?
               getOnInstanceSubscriptionUnloading().render( subscription, getInstanceRoot( subscription ) ) :
               getOnTypeSubscriptionUnloading().render( subscription );
      }
      else if ( AreaOfInterest.Status.UNLOADED == status )
      {
        return getOnUnloaded().render();
      }
      else
      {
        assert AreaOfInterest.Status.DELETED == status;
        return getOnDeleted().render();
      }
    }
  }

  @SuppressWarnings( "unchecked" )
  @Nonnull
  private T getInstanceRoot( @Nonnull final Subscription subscription )
  {
    return (T) subscription.getInstanceRoot();
  }

  @Memoize
  @Nonnull
  protected AreaOfInterest.Status getStatus()
  {
    final AreaOfInterest areaOfInterest = getAreaOfInterest();
    if ( null == areaOfInterest )
    {
      // This can happen when the AreaOfInterest has been removed but
      // the react4j view has not been removed from view tree
      return AreaOfInterest.Status.NOT_ASKED;
    }
    else
    {
      final AreaOfInterest.Status status = areaOfInterest.getStatus();
      final Subscription subscription = areaOfInterest.getSubscription();

      // Update the status field to pretend that subscription is at an earlier stage if
      // subscription data has not arrived
      if ( null == subscription )
      {
        if ( status.shouldDataBePresent() )
        {
          return AreaOfInterest.Status.LOADING;
        }
        else if ( AreaOfInterest.Status.UPDATE_FAILED == status )
        {
          return AreaOfInterest.Status.LOAD_FAILED;
        }
      }
      return status;
    }
  }
}
