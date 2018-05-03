package org.realityforge.replicant.client.react4j;

import arez.annotations.Action;
import arez.annotations.Observable;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import jsinterop.base.JsPropertyMap;
import org.realityforge.replicant.client.ChannelAddress;
import org.realityforge.replicant.client.FilterUtil;
import org.realityforge.replicant.client.aoi.AreaOfInterest;
import org.realityforge.replicant.client.aoi.AreaOfInterestService;
import org.realityforge.replicant.client.subscription.Subscription;
import react4j.annotations.Prop;
import react4j.arez.ReactArezComponent;
import react4j.core.ReactNode;

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

  @Inject
  AreaOfInterestService _areaOfInterestService;

  @Nonnull
  protected abstract Enum getChannelType();

  @Nullable
  protected Object getId()
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

  @Observable
  @Nullable
  protected abstract AreaOfInterest getAreaOfInterest();

  protected abstract void setAreaOfInterest( @Nullable AreaOfInterest areaOfInterest );

  @Action
  @Override
  protected void componentDidMount()
  {
    super.componentDidMount();
    updateChannelSubscription();
  }

  @Action
  @Override
  protected void componentDidUpdate( @Nullable final JsPropertyMap<Object> prevProps,
                                     @Nullable final JsPropertyMap<Object> prevState )
  {
    super.componentDidUpdate( prevProps, prevState );
    final Enum lastChannelType = (Enum) ( null != prevProps ? prevProps.get( "channelType" ) : null );
    if ( null != lastChannelType && getChannelType() != lastChannelType )
    {
      setAreaOfInterest( null );
    }
    else
    {
      final Object lastId = null != prevProps ? prevProps.get( "id" ) : null;
      if ( !Objects.equals( getId(), lastId ) )
      {
        setAreaOfInterest( null );
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
            areaOfInterest.getChannel().setFilter( newFilter );
          }
          else
          {
            setAreaOfInterest( null );
          }
        }
      }
    }
    updateChannelSubscription();
  }

  private void updateChannelSubscription()
  {
    final ChannelAddress address = new ChannelAddress( getChannelType(), getId() );
    setAreaOfInterest( _areaOfInterestService.findOrCreateAreaOfInterest( address, getFilter() ) );
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
      else if ( AreaOfInterest.Status.LOADED == status )
      {
        final EntryCallback<T> callback = onLoaded();
        return null != callback ? callback.render( asResult( areaOfInterest ) ) : null;
      }
      else if ( AreaOfInterest.Status.LOAD_FAILED == status )
      {
        final ErrorCallback callback = onLoadFailed();
        return null != callback ? callback.render( Objects.requireNonNull( areaOfInterest.getError() ) ) : null;
      }
      else if ( AreaOfInterest.Status.UPDATING == status )
      {
        final EntryCallback<T> callback = onUpdating();
        return null != callback ? callback.render( asResult( areaOfInterest ) ) : null;
      }
      else if ( AreaOfInterest.Status.UPDATED == status )
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
      else if ( AreaOfInterest.Status.UNLOADING == status )
      {
        final EntryCallback<T> callback = onUnloading();
        return null != callback ? callback.render( asResult( areaOfInterest ) ) : null;
      }
      else
      {
        assert AreaOfInterest.Status.UNLOADED == status;
        final BasicCallback callback = onUnloaded();
        return null != callback ? callback.render() : null;
      }
    }
  }

  @SuppressWarnings( "unchecked" )
  @Nonnull
  private SubscriptionResult<T> asResult( @Nonnull final AreaOfInterest areaOfInterest )
  {
    final ChannelAddress address = areaOfInterest.getChannel().getAddress();
    final Subscription subscription = areaOfInterest.getSubscription();
    assert null != subscription;
    final Object id = address.getId();
    final T instanceRoot =
      null != id ? (T) subscription.findEntityByTypeAndId( Objects.requireNonNull( getInstanceType() ), id ) : null;
    return new SubscriptionResult<>( Objects.requireNonNull( subscription ), instanceRoot );
  }
}
