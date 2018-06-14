package org.realityforge.replicant.client.react4j;

import arez.Disposable;
import arez.annotations.Action;
import arez.annotations.Dependency;
import arez.annotations.Observable;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.base.JsPropertyMap;
import react4j.annotations.Prop;
import react4j.arez.ReactArezComponent;
import react4j.core.ReactNode;
import replicant.AreaOfInterest;
import replicant.ChannelAddress;
import replicant.FilterUtil;
import replicant.Replicant;
import replicant.Subscription;

/**
 * An abstract React4j component that manages subscription to channels.
 */
public abstract class ReplicantSubscription<T>
  extends ReactArezComponent
{
  @FunctionalInterface
  public interface BasicCallback
  {
    @Nullable
    ReactNode render();
  }

  @FunctionalInterface
  public interface EntryCallback<T>
  {
    @Nullable
    ReactNode render( @Nonnull SubscriptionResult<T> entry );
  }

  @FunctionalInterface
  public interface ErrorCallback
  {
    @Nullable
    ReactNode render( @Nonnull Throwable error );
  }

  @FunctionalInterface
  public interface UpdateErrorCallback
  {
    @Nullable
    ReactNode render( @Nonnull Subscription entry, @Nonnull Throwable error );
  }

  protected abstract int getSystemId();

  protected abstract int getChannelId();

  @Nullable
  protected Integer getId()
  {
    return null;
  }

  @Nullable
  protected Class getInstanceType()
  {
    return null;
  }

  @Nullable
  protected Object getFilter()
  {
    return null;
  }

  protected boolean expectFilter()
  {
    return false;
  }

  protected boolean expectFilterUpdates()
  {
    return false;
  }

  @Prop
  @Nullable
  protected abstract BasicCallback onNotAsked();

  @Prop
  @Nullable
  protected abstract BasicCallback onLoading();

  @Prop
  @Nullable
  protected abstract ErrorCallback onLoadFailed();

  @Prop
  @Nullable
  protected abstract EntryCallback<T> onLoaded();

  @Prop
  @Nullable
  protected abstract EntryCallback<T> onUpdating();

  @Prop
  @Nullable
  protected abstract UpdateErrorCallback onUpdateFailed();

  @Prop
  @Nullable
  protected abstract EntryCallback<T> onUpdated();

  @Prop
  @Nullable
  protected abstract EntryCallback<T> onUnloading();

  @Prop
  @Nullable
  protected abstract BasicCallback onUnloaded();

  @Dependency( action = Dependency.Action.SET_NULL )
  @Observable
  @Nullable
  protected abstract AreaOfInterest getAreaOfInterest();

  protected abstract void setAreaOfInterest( @Nullable AreaOfInterest areaOfInterest );

  @Action
  @Override
  protected void componentDidMount()
  {
    super.componentDidMount();
    updateAreaOfInterest();
  }

  @Action
  @Override
  protected void componentDidUpdate( @Nullable final JsPropertyMap<Object> prevProps,
                                     @Nullable final JsPropertyMap<Object> prevState )
  {
    super.componentDidUpdate( prevProps, prevState );
    final Object lastId = null != prevProps ? prevProps.get( "id" ) : null;
    if ( !Objects.equals( getId(), lastId ) )
    {
      clearAreaOfInterest();
    }
    else if ( expectFilter() )
    {
      final Object filter = null != prevProps ? prevProps.get( "filter" ) : null;
      final Object newFilter = getFilter();
      if ( !FilterUtil.filtersEqual( newFilter, filter ) )
      {
        final AreaOfInterest areaOfInterest = getAreaOfInterest();
        if ( null != areaOfInterest && expectFilterUpdates() )
        {
          Replicant.context().createOrUpdateAreaOfInterest( areaOfInterest.getAddress(), newFilter );
        }
        else
        {
          clearAreaOfInterest();
        }
      }
    }
    updateAreaOfInterest();
  }

  @Action
  @Override
  protected void componentWillUnmount()
  {
    clearAreaOfInterest();
  }

  private void clearAreaOfInterest()
  {
    final AreaOfInterest areaOfInterest = getAreaOfInterest();
    if ( null != areaOfInterest )
    {
      Disposable.dispose( areaOfInterest );
    }
  }

  private void updateAreaOfInterest()
  {
    final ChannelAddress address = new ChannelAddress( getSystemId(), getChannelId(), getId() );
    final AreaOfInterest newAreaOfInterest = Replicant.context().createOrUpdateAreaOfInterest( address, getFilter() );
    if ( getAreaOfInterest() != newAreaOfInterest )
    {
      clearAreaOfInterest();
      setAreaOfInterest( newAreaOfInterest );
    }
  }

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
      final AreaOfInterest.Status status = areaOfInterest.getStatus();
      final Subscription subscription = areaOfInterest.getSubscription();
      /*
       * TODO: Need to figure out how we handle subscriptions that not present
       * when AOI updates.
       */
      if ( AreaOfInterest.Status.NOT_ASKED == status )
      {
        final BasicCallback callback = onNotAsked();
        return null != callback ? callback.render() : null;
      }
      else if ( AreaOfInterest.Status.LOADING == status )
      {
        final BasicCallback callback = onLoading();
        return null != callback ? callback.render() : null;
      }
      else if ( AreaOfInterest.Status.LOADED == status && null != subscription )
      {
        final EntryCallback<T> callback = onLoaded();
        return null != callback ? callback.render( asResult( areaOfInterest ) ) : null;
      }
      else if ( AreaOfInterest.Status.LOAD_FAILED == status )
      {
        final ErrorCallback callback = onLoadFailed();
        return null != callback ? callback.render( Objects.requireNonNull( areaOfInterest.getError() ) ) : null;
      }
      else if ( AreaOfInterest.Status.UPDATING == status && null != subscription )
      {
        final EntryCallback<T> callback = onUpdating();
        return null != callback ? callback.render( asResult( areaOfInterest ) ) : null;
      }
      else if ( AreaOfInterest.Status.UPDATED == status && null != subscription )
      {
        final EntryCallback<T> callback = onUpdated();
        return null != callback ? callback.render( asResult( areaOfInterest ) ) : null;
      }
      else if ( AreaOfInterest.Status.UPDATE_FAILED == status )
      {
        final UpdateErrorCallback callback = onUpdateFailed();
        return null != callback ?
               callback.render( Objects.requireNonNull( areaOfInterest.getSubscription() ),
                                Objects.requireNonNull( areaOfInterest.getError() ) ) :
               null;
      }
      else if ( AreaOfInterest.Status.UNLOADING == status && null != subscription )
      {
        final EntryCallback<T> callback = onUnloading();
        return null != callback ? callback.render( asResult( areaOfInterest ) ) : null;
      }
      else
      {
        //TODO: assert AreaOfInterest.Status.UNLOADED == status;
        final BasicCallback callback = onUnloaded();
        return null != callback ? callback.render() : null;
      }
    }
  }

  @SuppressWarnings( "unchecked" )
  @Nonnull
  private SubscriptionResult<T> asResult( @Nonnull final AreaOfInterest areaOfInterest )
  {
    final Subscription subscription = areaOfInterest.getSubscription();
    assert null != subscription;
    final T instanceRoot =
      subscription.getChannelSchema().isInstanceChannel() ? (T) subscription.getInstanceRoot() : null;
    return new SubscriptionResult<>( subscription, instanceRoot );
  }
}
