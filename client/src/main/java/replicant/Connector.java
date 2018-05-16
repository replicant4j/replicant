package replicant;

import arez.ArezContext;
import arez.Disposable;
import arez.annotations.Action;
import arez.annotations.ContextRef;
import arez.annotations.Observable;
import arez.annotations.PreDispose;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.anodoc.TestOnly;
import org.realityforge.braincheck.Guards;
import replicant.spy.ConnectFailureEvent;
import replicant.spy.ConnectedEvent;
import replicant.spy.DataLoadStatus;
import replicant.spy.DisconnectFailureEvent;
import replicant.spy.DisconnectedEvent;
import replicant.spy.MessageProcessFailureEvent;
import replicant.spy.MessageProcessedEvent;
import replicant.spy.MessageReadFailureEvent;
import replicant.spy.RestartEvent;
import replicant.spy.SubscribeCompletedEvent;
import replicant.spy.SubscribeFailedEvent;
import replicant.spy.SubscribeRequestQueuedEvent;
import replicant.spy.SubscribeStartedEvent;
import replicant.spy.SubscriptionUpdateCompletedEvent;
import replicant.spy.SubscriptionUpdateFailedEvent;
import replicant.spy.SubscriptionUpdateRequestQueuedEvent;
import replicant.spy.SubscriptionUpdateStartedEvent;
import replicant.spy.UnsubscribeCompletedEvent;
import replicant.spy.UnsubscribeFailedEvent;
import replicant.spy.UnsubscribeRequestQueuedEvent;
import replicant.spy.UnsubscribeStartedEvent;

/**
 * The Connector is responsible for managing a Connection to a backend datasource.
 */
public abstract class Connector
  extends ReplicantService
{
  @Nonnull
  private final Class<?> _systemType;
  @Nonnull
  private ConnectorState _state = ConnectorState.DISCONNECTED;
  /**
   * The current connection managed by the connector, if any.
   */
  @Nullable
  private Connection _connection;
  /**
   * Flag indicating that the Connectors internal scheduler is actively progressing
   * requests and responses. A scheduler should only be active if there is a connection present.
   */
  private boolean _schedulerActive;
  /**
   * This lock is acquired by the Connector when it begins processing messages from the network.
   * Once the processor is idle the lock should be released to allow Arez to reflect all the changes.
   */
  @Nullable
  private Disposable _schedulerLock;

  protected Connector( @Nullable final ReplicantContext context, @Nonnull final Class<?> systemType )
  {
    super( context );
    _systemType = Objects.requireNonNull( systemType );
    getReplicantRuntime().registerConnector( this );
  }

  @PreDispose
  protected void preDispose()
  {
    getReplicantRuntime().deregisterConnector( this );
  }

  /**
   * Connect to the underlying data source.
   */
  public void connect()
  {
    final ConnectorState state = getState();
    if ( ConnectorState.CONNECTING != state && ConnectorState.CONNECTED != state )
    {
      ConnectorState newState = ConnectorState.ERROR;
      try
      {
        doConnect( this::onConnected );
        newState = ConnectorState.CONNECTING;
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
    final ConnectorState state = getState();
    if ( ConnectorState.DISCONNECTING != state && ConnectorState.DISCONNECTED != state )
    {
      ConnectorState newState = ConnectorState.ERROR;
      try
      {
        doDisconnect( this::onDisconnected );
        newState = ConnectorState.DISCONNECTING;
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
  public final Class<?> getSystemType()
  {
    return _systemType;
  }

  protected final void setConnection( @Nullable final Connection connection )
  {
    _connection = connection;
    purgeSubscriptions();
  }

  @Nullable
  protected final Connection getConnection()
  {
    return _connection;
  }

  @Nonnull
  protected final Connection ensureConnection()
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      Guards.invariant( () -> null != _connection,
                        () -> "Replicant-0031: Connector.ensureConnection() when no connection is present." );
    }
    assert null != _connection;
    return _connection;
  }

  @Action
  protected void purgeSubscriptions()
  {
    Stream.concat( getReplicantContext().getTypeSubscriptions().stream(),
                   getReplicantContext().getInstanceSubscriptions().stream() )
      // Only purge subscriptions for current system
      .filter( s -> s.getAddress().getSystem().equals( getSystemType() ) )
      // Purge in reverse order. First instance subscriptions then type subscriptions
      .sorted( Comparator.reverseOrder() )
      .forEachOrdered( Disposable::dispose );
  }

  /**
   * Return true if an area of interest action with specified parameters is pending or being processed.
   * When the action parameter is DELETE the filter parameter is ignored.
   */
  public final boolean isAreaOfInterestRequestPending( @Nonnull final AreaOfInterestRequest.Type action,
                                                       @Nonnull final ChannelAddress address,
                                                       @Nullable final Object filter )
  {
    final Connection connection = getConnection();
    return null != connection && connection.isAreaOfInterestRequestPending( action, address, filter );
  }

  /**
   * Return the index of last matching Type in pending aoi actions list.
   */
  public final int lastIndexOfPendingAreaOfInterestRequest( @Nonnull final AreaOfInterestRequest.Type action,
                                                            @Nonnull final ChannelAddress address,
                                                            @Nullable final Object filter )
  {
    final Connection connection = getConnection();
    return null == connection ? -1 : connection.lastIndexOfPendingAreaOfInterestRequest( action, address, filter );
  }

  public final void requestSubscribe( @Nonnull final ChannelAddress address, @Nullable final Object filter )
  {
    ensureConnection().requestSubscribe( address, filter );
    triggerScheduler();
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new SubscribeRequestQueuedEvent( address, filter ) );
    }
  }

  public final void requestSubscriptionUpdate( @Nonnull final ChannelAddress address,
                                               @Nullable final Object filter )
  {
    ensureConnection().requestSubscriptionUpdate( address, filter );
    triggerScheduler();
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new SubscriptionUpdateRequestQueuedEvent( address, filter ) );
    }
  }

  public final void requestUnsubscribe( @Nonnull final ChannelAddress address )
  {
    ensureConnection().requestUnsubscribe( address );
    triggerScheduler();
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new UnsubscribeRequestQueuedEvent( address ) );
    }
  }

  final boolean isSchedulerActive()
  {
    return _schedulerActive;
  }

  /**
   * Schedule request and response processing.
   * This method should be invoked when requests are queued or responses are received.
   */
  protected final void triggerScheduler()
  {
    if ( !_schedulerActive )
    {
      _schedulerActive = true;

      activateScheduler();
    }
  }

  /**
   * Perform a single step progressing requests and responses.
   * This is invoked from the scheduler and will continue to be
   * invoked until it returns false.
   *
   * @return true if more work is to be done.
   */
  protected final boolean scheduleTick()
  {
    if ( null == _schedulerLock )
    {
      _schedulerLock = context().pauseScheduler();
    }
    try
    {
      final boolean step1 = progressAreaOfInterestRequestProcessing();
      final boolean step2 = progressResponseProcessing();
      _schedulerActive = step1 || step2;
    }
    catch ( final Throwable e )
    {
      onMessageProcessFailure( e );
      _schedulerActive = false;
      return false;
    }
    finally
    {
      if ( !_schedulerActive )
      {
        _schedulerLock.dispose();
        _schedulerLock = null;
      }
    }
    return _schedulerActive;
  }

  /**
   * Activate the scheduler.
   * This involves creating a scheduler that will invoke {@link #scheduleTick()} until
   * that method returns false.
   */
  protected abstract void activateScheduler();

  /**
   * Perform a single step in sending one (or a batch) or requests to the server.
   *
   * @return true if more work is to be done.
   */
  protected abstract boolean progressAreaOfInterestRequestProcessing();

  /**
   * Perform a single step processing messages received from the server.
   *
   * @return true if more work is to be done.
   */
  protected abstract boolean progressResponseProcessing();

  /**
   * {@inheritDoc}
   */
  @Nonnull
  @Observable
  public ConnectorState getState()
  {
    return _state;
  }

  protected void setState( @Nonnull final ConnectorState state )
  {
    _state = Objects.requireNonNull( state );
  }

  /**
   * Build a ChannelAddress from a ChannelChange value.
   *
   * @param channelChange the change.
   * @return the address.
   */
  @Nonnull
  protected final ChannelAddress toAddress( @Nonnull final ChannelChange channelChange )
  {
    final int channelId = channelChange.getChannelId();
    final Integer subChannelId = channelChange.hasSubChannelId() ? channelChange.getSubChannelId() : null;
    final Enum channelType = (Enum) getSystemType().getEnumConstants()[ channelId ];
    return new ChannelAddress( channelType, subChannelId );
  }

  /**
   * Method invoked when a filter has updated and the Connector needs to delink any entities
   * that are no longer part of the subscription now that the filter has changed.
   *
   * @param subscription the subscription that was updated.
   */
  protected final void updateSubscriptionForFilteredEntities( @Nonnull final Subscription subscription )
  {
    for ( final Class<?> entityType : new ArrayList<>( subscription.findAllEntityTypes() ) )
    {
      final List<Entity> entities = subscription.findAllEntitiesByType( entityType );
      if ( !entities.isEmpty() )
      {
        final SubscriptionUpdateEntityFilter entityFilter = getSubscriptionUpdateFilter();
        final ChannelAddress address = subscription.getAddress();
        final Object filter = subscription.getFilter();
        for ( final Entity entity : entities )
        {
          if ( !entityFilter.doesEntityMatchFilter( address, filter, entity ) )
          {
            entity.delinkFromSubscription( subscription );
          }
        }
      }
    }
  }

  @Nonnull
  protected abstract SubscriptionUpdateEntityFilter getSubscriptionUpdateFilter();

  /**
   * Invoked to fire an event when disconnect has completed.
   */
  @Action
  protected void onConnected()
  {
    setState( ConnectorState.CONNECTED );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new ConnectedEvent( getSystemType() ) );
    }
  }

  /**
   * Invoked to fire an event when failed to connect.
   */
  @Action
  protected void onConnectFailure( @Nonnull final Throwable error )
  {
    setState( ConnectorState.ERROR );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new ConnectFailureEvent( getSystemType(), error ) );
    }
  }

  /**
   * Invoked to fire an event when disconnect has completed.
   */
  @Action
  protected void onDisconnected()
  {
    setState( ConnectorState.DISCONNECTED );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new DisconnectedEvent( getSystemType() ) );
    }
  }

  /**
   * Invoked to fire an event when failed to connect.
   */
  @Action
  protected void onDisconnectFailure( @Nonnull final Throwable error )
  {
    setState( ConnectorState.ERROR );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new DisconnectFailureEvent( getSystemType(), error ) );
    }
  }

  /**
   * Invoked when a change set has been completely processed.
   *
   * @param status the status describing the results of data load.
   */
  protected void onMessageProcessed( @Nonnull final DataLoadStatus status )
  {
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new MessageProcessedEvent( getSystemType(), status ) );
    }
  }

  /**
   * Called when a data load has resulted in a failure.
   */
  @Action
  protected void onMessageProcessFailure( @Nonnull final Throwable error )
  {
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new MessageProcessFailureEvent( getSystemType(), error ) );
    }
    disconnectIfPossible( error );
  }

  /**
   * Attempted to retrieve data from backend and failed.
   */
  @Action
  protected void onMessageReadFailure( @Nonnull final Throwable error )
  {
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new MessageReadFailureEvent( getSystemType(), error ) );
    }
    disconnectIfPossible( error );
  }

  final void disconnectIfPossible( @Nonnull final Throwable cause )
  {
    if ( !ConnectorState.isTransitionState( getState() ) )
    {
      if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
      {
        getReplicantContext().getSpy().reportSpyEvent( new RestartEvent( getSystemType(), cause ) );
      }
      disconnect();
    }
  }

  @Action
  protected void onSubscribeStarted( @Nonnull final ChannelAddress address )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.LOADING, null );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new SubscribeStartedEvent( getSystemType(), address ) );
    }
  }

  @Action
  protected void onSubscribeCompleted( @Nonnull final ChannelAddress address )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.LOADED, null );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new SubscribeCompletedEvent( getSystemType(), address ) );
    }
  }

  @Action
  protected void onSubscribeFailed( @Nonnull final ChannelAddress address, @Nonnull final Throwable error )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.LOAD_FAILED, error );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new SubscribeFailedEvent( getSystemType(), address, error ) );
    }
  }

  @Action
  protected void onUnsubscribeStarted( @Nonnull final ChannelAddress address )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.UNLOADING, null );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new UnsubscribeStartedEvent( getSystemType(), address ) );
    }
  }

  @Action
  protected void onUnsubscribeCompleted( @Nonnull final ChannelAddress address )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.UNLOADED, null );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new UnsubscribeCompletedEvent( getSystemType(), address ) );
    }
  }

  @Action
  protected void onUnsubscribeFailed( @Nonnull final ChannelAddress address, @Nonnull final Throwable error )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.UNLOADED, null );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new UnsubscribeFailedEvent( getSystemType(), address, error ) );
    }
  }

  @Action
  protected void onSubscriptionUpdateStarted( @Nonnull final ChannelAddress address )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.UPDATING, null );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new SubscriptionUpdateStartedEvent( getSystemType(), address ) );
    }
  }

  @Action
  protected void onSubscriptionUpdateCompleted( @Nonnull final ChannelAddress address )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.UPDATED, null );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new SubscriptionUpdateCompletedEvent( getSystemType(), address ) );
    }
  }

  @Action
  protected void onSubscriptionUpdateFailed( @Nonnull final ChannelAddress address,
                                             @Nonnull final Throwable error )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.UPDATE_FAILED, error );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext()
        .getSpy()
        .reportSpyEvent( new SubscriptionUpdateFailedEvent( getSystemType(), address, error ) );
    }
  }

  private void updateAreaOfInterest( @Nonnull final ChannelAddress address,
                                     @Nonnull final AreaOfInterest.Status status,
                                     @Nullable final Throwable error )
  {
    final AreaOfInterest areaOfInterest = getReplicantContext().findAreaOfInterestByAddress( address );
    if ( null != areaOfInterest )
    {
      areaOfInterest.updateAreaOfInterest( status, error );
    }
  }

  @Nonnull
  protected final ReplicantRuntime getReplicantRuntime()
  {
    return getReplicantContext().getRuntime();
  }

  @ContextRef
  @Nonnull
  protected abstract ArezContext context();

  @TestOnly
  @Nullable
  final Disposable getSchedulerLock()
  {
    return _schedulerLock;
  }
}
