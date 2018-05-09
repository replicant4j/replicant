package replicant;

import arez.ArezContext;
import arez.annotations.Action;
import arez.annotations.ContextRef;
import arez.annotations.Observable;
import arez.annotations.PreDispose;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.transport.AreaOfInterestAction;
import org.realityforge.replicant.client.transport.DataLoaderService;
import replicant.spy.ConnectFailureEvent;
import replicant.spy.ConnectedEvent;
import replicant.spy.DataLoadStatus;
import replicant.spy.DisconnectFailureEvent;
import replicant.spy.DisconnectedEvent;
import replicant.spy.MessageProcessFailureEvent;
import replicant.spy.MessageProcessedEvent;
import replicant.spy.MessageReadFailureEvent;
import replicant.spy.SubscribeCompletedEvent;
import replicant.spy.SubscribeFailedEvent;
import replicant.spy.SubscribeStartedEvent;
import replicant.spy.SubscriptionUpdateCompletedEvent;
import replicant.spy.SubscriptionUpdateFailedEvent;
import replicant.spy.SubscriptionUpdateStartedEvent;
import replicant.spy.UnsubscribeCompletedEvent;
import replicant.spy.UnsubscribeFailedEvent;
import replicant.spy.UnsubscribeStartedEvent;

/**
 * Base class responsible for managing data loading from a particular source.
 *
 * TODO: This class exists as separate base class to make it easier to incrementally test code but should
 * eventually be migrated into AbstractDataLoaderService.
 */
public abstract class Connector
  implements DataLoaderService
{
  @Nonnull
  private final Class<?> _systemType;
  @Nonnull
  private final ReplicantRuntime _replicantRuntime;
  @Nonnull
  private State _state = State.DISCONNECTED;

  protected Connector( @Nonnull final Class<?> systemType, @Nonnull final ReplicantRuntime replicantRuntime )
  {
    _systemType = Objects.requireNonNull( systemType );
    _replicantRuntime = Objects.requireNonNull( replicantRuntime );
    _replicantRuntime.registerConnector( this );
  }

  @PreDispose
  protected void preDispose()
  {
    _replicantRuntime.deregisterConnector( this );
  }

  /**
   * Connect to the underlying data source.
   */
  public void connect()
  {
    final State state = getState();
    if ( State.CONNECTING != state && State.CONNECTED != state )
    {
      State newState = State.ERROR;
      try
      {
        doConnect( this::onConnected );
        newState = State.CONNECTING;
      }
      finally
      {
        setState( newState );
      }
    }
  }

  /**
   * Perform the connection, invoking the action when connection has completed.
   *
   * @param action the action to invoke once connect has completed.
   */
  protected abstract void doConnect( @Nonnull SafeProcedure action );

  /**
   * Disconnect from underlying data source.
   */
  public void disconnect()
  {
    final State state = getState();
    if ( State.DISCONNECTING != state && State.DISCONNECTED != state )
    {
      State newState = State.ERROR;
      try
      {
        doDisconnect( this::onDisconnected );
        newState = State.DISCONNECTING;
      }
      finally
      {
        setState( newState );
      }
    }
  }

  /**
   * Perform the disconnection, invoking the action when disconnection has completed.
   *
   * @param action the action to invoke once disconnect has completed.
   */
  protected abstract void doDisconnect( @Nonnull SafeProcedure action );

  /**
   * Return the class of channels that this loader processes.
   */
  @Nonnull
  @Override
  public final Class<?> getSystemType()
  {
    return _systemType;
  }

  /**
   * Return true if an area of interest action with specified parameters is pending or being processed.
   * When the action parameter is DELETE the filter parameter is ignored.
   */
  public abstract boolean isAreaOfInterestActionPending( @Nonnull AreaOfInterestAction action,
                                                         @Nonnull ChannelAddress address,
                                                         @Nullable Object filter );

  /**
   * Return the index of last matching AreaOfInterestAction in pending aoi actions list.
   */
  public abstract int indexOfPendingAreaOfInterestAction( @Nonnull AreaOfInterestAction action,
                                                          @Nonnull ChannelAddress address,
                                                          @Nullable Object filter );

  public abstract void requestSubscribe( @Nonnull ChannelAddress address, @Nullable Object filterParameter );

  public abstract void requestSubscriptionUpdate( @Nonnull ChannelAddress address, @Nullable Object filterParameter );

  public abstract void requestUnsubscribe( @Nonnull ChannelAddress address );

  /**
   * {@inheritDoc}
   */
  @Nonnull
  @Override
  @Observable
  public State getState()
  {
    return _state;
  }

  protected void setState( @Nonnull final State state )
  {
    _state = Objects.requireNonNull( state );
  }

  /**
   * Invoked to fire an event when disconnect has completed.
   */
  @Action
  protected void onConnected()
  {
    setState( State.CONNECTED );
    if ( Replicant.areSpiesEnabled() && Replicant.context().getSpy().willPropagateSpyEvents() )
    {
      Replicant.context().getSpy().reportSpyEvent( new ConnectedEvent( getSystemType() ) );
    }
  }

  /**
   * Invoked to fire an event when failed to connect.
   */
  @Action
  protected void onConnectFailure( @Nonnull final Throwable error )
  {
    setState( State.ERROR );
    if ( Replicant.areSpiesEnabled() && Replicant.context().getSpy().willPropagateSpyEvents() )
    {
      Replicant.context().getSpy().reportSpyEvent( new ConnectFailureEvent( getSystemType(), error ) );
    }
  }

  /**
   * Invoked to fire an event when disconnect has completed.
   */
  @Action
  protected void onDisconnected()
  {
    setState( State.DISCONNECTED );
    if ( Replicant.areSpiesEnabled() && Replicant.context().getSpy().willPropagateSpyEvents() )
    {
      Replicant.context().getSpy().reportSpyEvent( new DisconnectedEvent( getSystemType() ) );
    }
  }

  /**
   * Invoked to fire an event when failed to connect.
   */
  @Action
  protected void onDisconnectFailure( @Nonnull final Throwable error )
  {
    setState( State.ERROR );
    if ( Replicant.areSpiesEnabled() && Replicant.context().getSpy().willPropagateSpyEvents() )
    {
      Replicant.context().getSpy().reportSpyEvent( new DisconnectFailureEvent( getSystemType(), error ) );
    }
  }

  /**
   * Invoked when a change set has been completely processed.
   *
   * @param status the status describing the results of data load.
   */
  protected void onMessageProcessed( @Nonnull final DataLoadStatus status )
  {
    if ( Replicant.areSpiesEnabled() && Replicant.context().getSpy().willPropagateSpyEvents() )
    {
      Replicant.context().getSpy().reportSpyEvent( new MessageProcessedEvent( getSystemType(), status ) );
    }
  }

  /**
   * Called when a data load has resulted in a failure.
   */
  @Action
  protected void onMessageProcessFailure( @Nonnull final Throwable error )
  {
    _replicantRuntime.disconnectIfPossible( this, error );
    if ( Replicant.areSpiesEnabled() && Replicant.context().getSpy().willPropagateSpyEvents() )
    {
      Replicant.context().getSpy().reportSpyEvent( new MessageProcessFailureEvent( getSystemType(), error ) );
    }
  }

  /**
   * Attempted to retrieve data from backend and failed.
   */
  @Action
  protected void onMessageReadFailure( @Nonnull final Throwable error )
  {
    _replicantRuntime.disconnectIfPossible( this, error );
    if ( Replicant.areSpiesEnabled() && Replicant.context().getSpy().willPropagateSpyEvents() )
    {
      Replicant.context().getSpy().reportSpyEvent( new MessageReadFailureEvent( getSystemType(), error ) );
    }
  }

  @Action
  protected void onSubscribeStarted( @Nonnull final ChannelAddress address )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.LOADING, null );
    if ( Replicant.areSpiesEnabled() && Replicant.context().getSpy().willPropagateSpyEvents() )
    {
      Replicant.context().getSpy().reportSpyEvent( new SubscribeStartedEvent( getSystemType(), address ) );
    }
  }

  @Action
  protected void onSubscribeCompleted( @Nonnull final ChannelAddress address )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.LOADED, null );
    if ( Replicant.areSpiesEnabled() && Replicant.context().getSpy().willPropagateSpyEvents() )
    {
      Replicant.context().getSpy().reportSpyEvent( new SubscribeCompletedEvent( getSystemType(), address ) );
    }
  }

  @Action
  protected void onSubscribeFailed( @Nonnull final ChannelAddress address, @Nonnull final Throwable error )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.LOAD_FAILED, error );
    if ( Replicant.areSpiesEnabled() && Replicant.context().getSpy().willPropagateSpyEvents() )
    {
      Replicant.context().getSpy().reportSpyEvent( new SubscribeFailedEvent( getSystemType(), address, error ) );
    }
  }

  @Action
  protected void onUnsubscribeStarted( @Nonnull final ChannelAddress address )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.UNLOADING, null );
    if ( Replicant.areSpiesEnabled() && Replicant.context().getSpy().willPropagateSpyEvents() )
    {
      Replicant.context().getSpy().reportSpyEvent( new UnsubscribeStartedEvent( getSystemType(), address ) );
    }
  }

  @Action
  protected void onUnsubscribeCompleted( @Nonnull final ChannelAddress address )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.UNLOADED, null );
    if ( Replicant.areSpiesEnabled() && Replicant.context().getSpy().willPropagateSpyEvents() )
    {
      Replicant.context().getSpy().reportSpyEvent( new UnsubscribeCompletedEvent( getSystemType(), address ) );
    }
  }

  @Action
  protected void onUnsubscribeFailed( @Nonnull final ChannelAddress address, @Nonnull final Throwable error )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.UNLOADED, null );
    if ( Replicant.areSpiesEnabled() && Replicant.context().getSpy().willPropagateSpyEvents() )
    {
      Replicant.context().getSpy().reportSpyEvent( new UnsubscribeFailedEvent( getSystemType(), address, error ) );
    }
  }

  @Action
  protected void onSubscriptionUpdateStarted( @Nonnull final ChannelAddress address )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.UPDATING, null );
    if ( Replicant.areSpiesEnabled() && Replicant.context().getSpy().willPropagateSpyEvents() )
    {
      Replicant.context().getSpy().reportSpyEvent( new SubscriptionUpdateStartedEvent( getSystemType(), address ) );
    }
  }

  @Action
  protected void onSubscriptionUpdateCompleted( @Nonnull final ChannelAddress address )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.UPDATED, null );
    if ( Replicant.areSpiesEnabled() && Replicant.context().getSpy().willPropagateSpyEvents() )
    {
      Replicant.context().getSpy().reportSpyEvent( new SubscriptionUpdateCompletedEvent( getSystemType(), address ) );
    }
  }

  @Action
  protected void onSubscriptionUpdateFailed( @Nonnull final ChannelAddress address,
                                             @Nonnull final Throwable error )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.UPDATE_FAILED, error );
    if ( Replicant.areSpiesEnabled() && Replicant.context().getSpy().willPropagateSpyEvents() )
    {
      Replicant.context()
        .getSpy()
        .reportSpyEvent( new SubscriptionUpdateFailedEvent( getSystemType(), address, error ) );
    }
  }

  private void updateAreaOfInterest( @Nonnull final ChannelAddress address,
                                     @Nonnull final AreaOfInterest.Status status,
                                     @Nullable final Throwable error )
  {
    final AreaOfInterest areaOfInterest = Replicant.context().findAreaOfInterestByAddress( address );
    if ( null != areaOfInterest )
    {
      areaOfInterest.updateAreaOfInterest( status, error );
    }
  }

  @Nonnull
  protected final ReplicantRuntime getReplicantRuntime()
  {
    return _replicantRuntime;
  }

  @ContextRef
  @Nonnull
  protected abstract ArezContext context();
}
