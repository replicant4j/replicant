package org.realityforge.replicant.client.react4j;

import arez.annotations.Action;
import arez.annotations.Observable;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import jsinterop.base.JsPropertyMap;
import org.realityforge.replicant.client.ChannelDescriptor;
import org.realityforge.replicant.client.ChannelSubscriptionEntry;
import org.realityforge.replicant.client.FilterUtil;
import org.realityforge.replicant.client.runtime.AreaOfInterestListenerAdapter;
import org.realityforge.replicant.client.runtime.AreaOfInterestService;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;
import org.realityforge.replicant.client.runtime.ReplicantConnection;
import org.realityforge.replicant.client.runtime.Subscription;
import org.realityforge.replicant.client.runtime.SubscriptionReference;
import org.realityforge.replicant.client.transport.DataLoaderListenerAdapter;
import org.realityforge.replicant.client.transport.DataLoaderService;
import react4j.annotations.Prop;
import react4j.annotations.ReactComponent;
import react4j.arez.ReactArezComponent;
import react4j.core.ReactNode;

/**
 * A React4j component that manages subscription to graphs.
 */
@SuppressWarnings( "Duplicates" )
@ReactComponent
public abstract class ReplicantSubscription
  extends ReactArezComponent
{
  public enum Status
  {
    NOT_ASKED,
    LOADING,
    FAILURE,
    SUCCESS
  }

  @FunctionalInterface
  public interface OnNotAskedCallback
  {
    /**
     * Callback invoked to render child component when subscription has not been asked.
     */
    @Nullable
    ReactNode render();
  }

  @FunctionalInterface
  public interface OnLoadingCallback
  {
    /**
     * Callback invoked to render child component when subscription is loading.
     */
    @Nullable
    ReactNode render();
  }

  @FunctionalInterface
  public interface OnFailureCallback
  {
    /**
     * Callback invoked to render child component when subscription has failed.
     *
     * @param error the error that occurred subscribing to subscription.
     */
    @Nullable
    ReactNode render( @Nonnull Throwable error );
  }

  @FunctionalInterface
  public interface OnSuccessCallback
  {
    /**
     * Callback invoked to render child component when subscription is successful.
     *
     * @param entry  the entry describing subscription.
     * @param entity the entity subscribed to. This is non-null if the subscription is to an instance graph, otherwise it is null.
     */
    @Nullable
    ReactNode render( @Nonnull ChannelSubscriptionEntry entry,
                      @Nullable Object entity );
  }

  private final AreaOfInterestListenerAdapter _listener = createAreaOfInterestListener();
  private final DataLoaderListenerAdapter _dataLoaderListener = createDataLoaderListener();
  @Nonnull
  private Status _status = Status.NOT_ASKED;
  @Inject
  ReplicantConnection _replicantConnection;

  @Prop
  @Nonnull
  abstract Enum getGraph();

  @Prop
  @Nullable
  abstract Object getId();

  @Prop
  @Nullable
  abstract Object getFilter();

  @Prop
  abstract boolean expectFilter();

  /**
   * If this is non-null then it is expected that it is an instance graph.
   * The type should match the graph
   */
  @Prop
  @Nullable
  abstract Class<?> getInstanceType();

  @Prop
  @Nullable
  abstract OnNotAskedCallback onNotAsked();

  @Prop
  @Nullable
  abstract OnLoadingCallback onLoading();

  @Prop
  @Nullable
  abstract OnFailureCallback onFailure();

  @Prop
  @Nullable
  abstract OnSuccessCallback onSuccess();

  @Observable
  @Nonnull
  Status getStatus()
  {
    return _status;
  }

  void setStatus( @Nonnull final Status status )
  {
    _status = Objects.requireNonNull( status );
  }

  @Observable
  @Nullable
  abstract ChannelSubscriptionEntry getSubscription();

  abstract void setSubscription( @Nullable ChannelSubscriptionEntry subscription );

  @Observable
  @Nullable
  abstract Object getEntity();

  abstract void setEntity( @Nullable Object entity );

  @Observable
  @Nullable
  abstract Throwable getError();

  abstract void setError( @Nullable Throwable error );

  @Observable
  @Nullable
  abstract SubscriptionReference getSubscriptionReference();

  abstract void setSubscriptionReference( @Nullable SubscriptionReference subscriptionReference );

  /**
   * Build channel descriptor from props.
   *
   * @return the channel descriptor.
   */
  @Nonnull
  private ChannelDescriptor buildChannelDescriptor()
  {
    return new ChannelDescriptor( getGraph(), getId() );
  }

  @Action
  @Override
  protected void postConstruct()
  {
    _replicantConnection.getAreaOfInterestService().addAreaOfInterestListener( _listener );
    getReplicantClientSystem().getDataLoaderService( getGraph() ).addDataLoaderListener( _dataLoaderListener );
  }

  @Action
  @Override
  protected void componentDidMount()
  {
    super.componentDidMount();
    updateReferences();
  }

  @Action
  @Override
  protected void componentDidUpdate( @Nullable final JsPropertyMap<Object> prevProps,
                                     @Nullable final JsPropertyMap<Object> prevState )
  {
    super.componentDidUpdate( prevProps, prevState );
    final Enum lastGraph = (Enum) ( null != prevProps ? prevProps.get( "graph" ) : null );
    if ( null != lastGraph && getGraph() != lastGraph )
    {
      // Need to change listener if we move to a different replication graph
      if ( getGraph().getDeclaringClass() != lastGraph.getDeclaringClass() )
      {
        getReplicantClientSystem().getDataLoaderService( lastGraph ).removeDataLoaderListener( _dataLoaderListener );
        getReplicantClientSystem().getDataLoaderService( getGraph() ).addDataLoaderListener( _dataLoaderListener );
      }
      releaseSubscriptionReference();
    }
    else
    {
      final Object lastId = null != prevProps ? prevProps.get( "id" ) : null;
      if ( !Objects.equals( getId(), lastId ) )
      {
        releaseSubscriptionReference();
      }
      else if ( expectFilter() )
      {
        final Object filter = null != prevProps ? prevProps.get( "filter" ) : null;
        final Object newFilter = getFilter();
        if ( !FilterUtil.filtersEqual( newFilter, filter ) )
        {
          final SubscriptionReference reference = getSubscriptionReference();
          if ( null != reference )
          {
            final AreaOfInterestService areaOfInterestService = _replicantConnection.getAreaOfInterestService();
            areaOfInterestService.updateSubscription( reference.getSubscription(), newFilter );
            if ( null != filter || null == newFilter )
            {
              setStatus( Status.UPDATING );
              setError( null );
            }
            return;
          }
        }
      }
    }
    updateReferences();
  }

  @Nonnull
  private ReplicantClientSystem getReplicantClientSystem()
  {
    return _replicantConnection.getReplicantClientSystem();
  }

  private void updateReferences()
  {
    SubscriptionReference subscriptionReference = getSubscriptionReference();
    if ( null == subscriptionReference || subscriptionReference.hasBeenReleased() )
    {
      final ChannelDescriptor channel = buildChannelDescriptor();
      setSubscriptionReference( _replicantConnection.getAreaOfInterestService()
                                  .findOrCreateSubscription( channel, getFilter() )
                                  .createReference() );
      if ( tryLoadSubscription( channel ) )
      {
        setStatus( Status.SUCCESS );
        setError( null );
      }
      else
      {
        setStatus( Status.LOADING );
        setError( null );
      }
    }
  }

  @Action
  @Override
  protected void componentWillUnmount()
  {
    releaseSubscriptionReference();
    _replicantConnection.getAreaOfInterestService().removeAreaOfInterestListener( _listener );
    getReplicantClientSystem().getDataLoaderService( getGraph() ).removeDataLoaderListener( _dataLoaderListener );
    super.componentWillUnmount();
  }

  @Nullable
  @Override
  protected ReactNode render()
  {
    final Status status = getStatus();
    if ( Status.NOT_ASKED == status )
    {
      assert null == getSubscription();
      assert null == getEntity();
      assert null == getError();
      final OnNotAskedCallback callback = onNotAsked();
      if ( null != callback )
      {
        return callback.render();
      }
      else
      {
        return null;
      }
    }
    else if ( Status.LOADING == status )
    {
      assert null == getSubscription();
      assert null == getEntity();
      assert null == getError();
      final OnLoadingCallback callback = onLoading();
      if ( null != callback )
      {
        return callback.render();
      }
      else
      {
        return null;
      }
    }
    else if ( Status.SUCCESS == status )
    {
      assert null != getSubscription();
      assert null == getInstanceType() || null != getEntity();
      assert null == getError();
      final OnSuccessCallback callback = onSuccess();
      if ( null != callback )
      {
        return callback.render( getSubscription(), getEntity() );
      }
      else
      {
        return null;
      }
    }
    else
    {
      assert Status.FAILURE == status;
      assert null == getSubscription();
      assert null == getEntity();
      assert null != getError();

      final Throwable error = getError();
      final OnFailureCallback callback = onFailure();
      if ( null != callback )
      {
        return callback.render( error );
      }
      else
      {
        return null;
      }
    }
  }

  /**
   * Invoked when subscription has been removed from Area of Interest.
   *
   * @param subscription the subscription.
   */
  @Action
  void onSubscriptionDeleted( @Nonnull final Subscription subscription )
  {
    final SubscriptionReference subscriptionReference = getSubscriptionReference();
    if ( null != subscriptionReference )
    {
      if ( subscriptionReference.hasBeenReleased() )
      {
        releaseSubscriptionReference();
      }
      else if ( subscription == subscriptionReference.getSubscription() )
      {
        assert !subscriptionReference.getSubscription().isActive();
        releaseSubscriptionReference();
      }
    }
  }

  /**
   * Called when specified channel has been updated and may cause a re-render.
   *
   * @param descriptor the channel.
   */
  @Action
  @SuppressWarnings( "PMD.CollapsibleIfStatements" )
  void onSubscriptionUpdate( @Nonnull final ChannelDescriptor descriptor )
  {
    final SubscriptionReference subscriptionReference = getSubscriptionReference();
    if ( null != subscriptionReference &&
         !subscriptionReference.hasBeenReleased() &&
         subscriptionReference.getSubscription().getDescriptor().equals( descriptor ) )
    {
      if ( tryLoadSubscription( descriptor ) )
      {
        setStatus( Status.SUCCESS );
        setError( null );
      }
    }
  }

  private boolean tryLoadSubscription( @Nonnull final ChannelDescriptor descriptor )
  {
    final ChannelSubscriptionEntry subscription =
      _replicantConnection.getSubscriptionManager().findSubscription( descriptor );
    setSubscription( subscription );
    if ( null != subscription )
    {
      if ( null != getInstanceType() )
      {
        final Object id = descriptor.getID();
        assert null != id;
        setEntity( _replicantConnection.getEntityLocator().getByID( getInstanceType(), id ) );
      }
      return true;
    }
    else
    {
      return false;
    }
  }

  @Action
  void onSubscriptionError( @Nonnull final ChannelDescriptor descriptor, @Nonnull final Throwable throwable )
  {
    final SubscriptionReference subscriptionReference = getSubscriptionReference();
    if ( null != subscriptionReference &&
         !subscriptionReference.hasBeenReleased() &&
         subscriptionReference.getSubscription().getDescriptor().equals( descriptor ) )
    {
      setStatus( Status.FAILURE );
      setError( throwable );
      setSubscription( null );
      setEntity( null );
    }
  }

  @Action
  void onDataLoaderDisconnect()
  {
    releaseSubscriptionReference();
  }

  /**
   * Called to release and null scope reference if present.
   * This is called when the subscription details change.
   */
  private void releaseSubscriptionReference()
  {
    final SubscriptionReference subscriptionReference = getSubscriptionReference();
    if ( null != subscriptionReference )
    {
      subscriptionReference.release();
      setStatus( Status.NOT_ASKED );
      setSubscriptionReference( null );
      setSubscription( null );
      setError( null );
      setEntity( null );
    }
  }

  @Nonnull
  private AreaOfInterestListenerAdapter createAreaOfInterestListener()
  {
    return new AreaOfInterestListenerAdapter()
    {
      @Override
      public void subscriptionDeleted( @Nonnull final Subscription subscription )
      {
        onSubscriptionDeleted( subscription );
      }
    };
  }

  @Nonnull
  private DataLoaderListenerAdapter createDataLoaderListener()
  {
    return new DataLoaderListenerAdapter()
    {
      @Override
      public void onInvalidDisconnect( @Nonnull final DataLoaderService service, @Nonnull final Throwable throwable )
      {
        onDataLoaderDisconnect();
      }

      @Override
      public void onDisconnect( @Nonnull final DataLoaderService service )
      {
        onDataLoaderDisconnect();
      }

      @Override
      public void onSubscribeCompleted( @Nonnull final DataLoaderService service,
                                        @Nonnull final ChannelDescriptor descriptor )
      {
        onSubscriptionUpdate( descriptor );
      }

      @Override
      public void onSubscribeFailed( @Nonnull final DataLoaderService service,
                                     @Nonnull final ChannelDescriptor descriptor,
                                     @Nonnull final Throwable throwable )
      {
        onSubscriptionError( descriptor, throwable );
      }

      @Override
      public void onSubscriptionUpdateCompleted( @Nonnull final DataLoaderService service,
                                                 @Nonnull final ChannelDescriptor descriptor )
      {
        onSubscriptionUpdate( descriptor );
      }

      @Override
      public void onUnsubscribeCompleted( @Nonnull final DataLoaderService service,
                                          @Nonnull final ChannelDescriptor descriptor )
      {
        onSubscriptionUpdate( descriptor );
      }

      @Override
      public void onSubscriptionUpdateFailed( @Nonnull final DataLoaderService service,
                                              @Nonnull final ChannelDescriptor descriptor,
                                              @Nonnull final Throwable throwable )
      {
        onSubscriptionError( descriptor, throwable );
      }
    };
  }
}
