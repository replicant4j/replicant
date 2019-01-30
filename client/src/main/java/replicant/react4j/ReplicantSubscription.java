package replicant.react4j;

import arez.Disposable;
import arez.annotations.Action;
import arez.annotations.ComponentDependency;
import arez.annotations.Memoize;
import arez.annotations.Observable;
import arez.annotations.PreDispose;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import react4j.Component;
import react4j.ReactNode;
import react4j.annotations.PostRender;
import replicant.AreaOfInterest;
import replicant.ChannelAddress;
import replicant.ChannelSchema;
import replicant.Replicant;
import replicant.Subscription;

/**
 * An abstract React4j component that manages subscription to channels.
 */
public abstract class ReplicantSubscription<T>
  extends Component
{
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

  protected abstract int getSystemId();

  protected abstract int getChannelId();

  protected int getId()
  {
    return 0;
  }

  protected boolean hasId()
  {
    return false;
  }

  @Nullable
  protected Object getFilter()
  {
    return null;
  }

  @Nullable
  protected abstract NoResultCallback getOnNotAsked();

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

  @ComponentDependency( action = ComponentDependency.Action.SET_NULL )
  @Observable
  @Nullable
  protected abstract AreaOfInterest getAreaOfInterest();

  protected abstract void setAreaOfInterest( @Nullable AreaOfInterest areaOfInterest );

  @PostRender
  protected final void postRender()
  {
    updateAreaOfInterest();
  }

  protected void updateAreaOfInterestOnIdChange()
  {
    clearAreaOfInterest();
  }

  @Action( reportParameters = false )
  protected void updateAreaOfInterestOnFilterChange( final Object newFilter )
  {
    final AreaOfInterest areaOfInterest = getAreaOfInterest();
    if ( null != areaOfInterest )
    {
      Replicant.context().createOrUpdateAreaOfInterest( areaOfInterest.getAddress(), newFilter );
    }
  }

  @PreDispose
  protected void preDispose()
  {
    clearAreaOfInterest();
  }

  @Action
  protected void clearAreaOfInterest()
  {
    final AreaOfInterest areaOfInterest = getAreaOfInterest();
    if ( null != areaOfInterest )
    {
      Disposable.dispose( areaOfInterest );
    }
  }

  @Action
  protected void updateAreaOfInterest()
  {
    final ChannelAddress address = new ChannelAddress( getSystemId(), getChannelId(), hasId() ? getId() : null );
    final AreaOfInterest newAreaOfInterest = Replicant.context().createOrUpdateAreaOfInterest( address, getFilter() );
    if ( getAreaOfInterest() != newAreaOfInterest )
    {
      clearAreaOfInterest();
      setAreaOfInterest( newAreaOfInterest );
    }
  }

  @SuppressWarnings( "unchecked" )
  @Nullable
  @Override
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
      final boolean isInstanceChannel = isInstanceChannel();

      if ( AreaOfInterest.Status.NOT_ASKED == status )
      {
        final NoResultCallback callback = getOnNotAsked();
        return null != callback ? callback.render() : null;
      }
      else if ( AreaOfInterest.Status.LOADING == status )
      {
        return getOnLoading().render();
      }
      else if ( AreaOfInterest.Status.LOADED == status )
      {
        final Subscription subscription = Objects.requireNonNull( areaOfInterest.getSubscription() );
        return isInstanceChannel ?
               getOnInstanceSubscriptionLoaded().render( subscription, (T) subscription.getInstanceRoot() ) :
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
               getOnInstanceSubscriptionUpdating().render( subscription, (T) subscription.getInstanceRoot() ) :
               getOnTypeSubscriptionUpdating().render( subscription );
      }
      else if ( AreaOfInterest.Status.UPDATED == status )
      {
        final Subscription subscription = Objects.requireNonNull( areaOfInterest.getSubscription() );
        return isInstanceChannel ?
               getOnInstanceSubscriptionUpdated().render( subscription, (T) subscription.getInstanceRoot() ) :
               getOnTypeSubscriptionUpdated().render( subscription );
      }
      else if ( AreaOfInterest.Status.UPDATE_FAILED == status )
      {
        final Subscription subscription = Objects.requireNonNull( areaOfInterest.getSubscription() );
        final Throwable error = Objects.requireNonNull( areaOfInterest.getError() );
        return isInstanceChannel ?
               getOnInstanceSubscriptionUpdateFailed().render( subscription,
                                                               (T) subscription.getInstanceRoot(),
                                                               error ) :
               getOnTypeSubscriptionUpdateFailed().render( subscription, error );
      }
      else if ( AreaOfInterest.Status.UNLOADING == status )
      {
        final Subscription subscription = Objects.requireNonNull( areaOfInterest.getSubscription() );
        return isInstanceChannel ?
               getOnInstanceSubscriptionUnloading().render( subscription, (T) subscription.getInstanceRoot() ) :
               getOnTypeSubscriptionUnloading().render( subscription );
      }
      else
      {
        assert AreaOfInterest.Status.UNLOADED == status;
        return getOnUnloaded().render();
      }
    }
  }

  @Memoize
  @Nullable
  protected AreaOfInterest.Status getStatus()
  {
    final AreaOfInterest areaOfInterest = getAreaOfInterest();
    assert null != areaOfInterest;
    final AreaOfInterest.Status status = areaOfInterest.getStatus();
    final Subscription subscription = areaOfInterest.getSubscription();

    // Update the status field to pretend that subscription is at an earlier stage if
    // subscription data has not arrived
    if ( null == subscription )
    {
      if ( AreaOfInterest.Status.LOADED == status ||
           AreaOfInterest.Status.UPDATING == status ||
           AreaOfInterest.Status.UPDATED == status ||
           AreaOfInterest.Status.UNLOADING == status )
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

  private boolean isInstanceChannel()
  {
    return getChannelSchema().isInstanceChannel();
  }

  @Nonnull
  private ChannelSchema getChannelSchema()
  {
    return Replicant.context().getSchemaById( getSystemId() ).getChannel( getChannelId() );
  }
}
