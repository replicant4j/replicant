package replicant;

import arez.ArezContext;
import arez.Disposable;
import arez.annotations.Action;
import arez.annotations.ContextRef;
import arez.annotations.Observable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.anodoc.TestOnly;
import org.realityforge.replicant.client.transport.ChangeMapper;
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
import static org.realityforge.braincheck.Guards.*;

/**
 * The Connector is responsible for managing a Connection to a backend datasource.
 */
public abstract class Connector
  extends ReplicantService
{
  private static final int DEFAULT_LINKS_TO_PROCESS_PER_TICK = 100;
  private static final int DEFAULT_CHANGES_TO_PROCESS_PER_TICK = 100;

  /**
   * The schema that defines data-API used to interact with datasource.
   */
  @Nonnull
  private final SystemSchema _schema;
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
  /**
   * Maximum number of entity links to attempt in a single tick of the scheduler. After this many links have
   * been processed then return and any remaining links can occur in a later tick.
   */
  private int _linksToProcessPerTick = DEFAULT_LINKS_TO_PROCESS_PER_TICK;
  /**
   * Maximum number of EntityChange messages processed in a single tick of the scheduler. After this many changes have
   * been processed then return and any remaining change can be processed in a later tick.
   */
  private int _changesToProcessPerTick = DEFAULT_CHANGES_TO_PROCESS_PER_TICK;

  protected Connector( @Nullable final ReplicantContext context,
                       @Nonnull final SystemSchema schema )
  {
    super( context );
    _schema = Objects.requireNonNull( schema );
    getReplicantRuntime().registerConnector( this );
    final SchemaService schemaService = getReplicantContext().getSchemaService();
    if ( !schemaService.contains( schema ) )
    {
      schemaService.registerSchema( schema );
    }
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
   * Return the schema associated with the connector.
   *
   * @return the schema associated with the connector.
   */
  @Nonnull
  public final SystemSchema getSchema()
  {
    return _schema;
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
      invariant( () -> null != _connection,
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
      .filter( s -> s.getAddress().getSystemId() == getSchema().getId() )
      // Purge in reverse order. First instance subscriptions then type subscriptions
      .sorted( Comparator.reverseOrder() )
      .forEachOrdered( Disposable::dispose );
  }

  final void setLinksToProcessPerTick( final int linksToProcessPerTick )
  {
    _linksToProcessPerTick = linksToProcessPerTick;
  }

  final void setChangesToProcessPerTick( final int changesToProcessPerTick )
  {
    _changesToProcessPerTick = changesToProcessPerTick;
  }

  /**
   * Return true if an area of interest action with specified parameters is pending or being processed.
   * When the action parameter is DELETE the filter parameter is ignored.
   */
  final boolean isAreaOfInterestRequestPending( @Nonnull final AreaOfInterestRequest.Type action,
                                                @Nonnull final ChannelAddress address,
                                                @Nullable final Object filter )
  {
    final Connection connection = getConnection();
    return null != connection && connection.isAreaOfInterestRequestPending( action, address, filter );
  }

  /**
   * Return the index of last matching Type in pending aoi actions list.
   */
  final int lastIndexOfPendingAreaOfInterestRequest( @Nonnull final AreaOfInterestRequest.Type action,
                                                     @Nonnull final ChannelAddress address,
                                                     @Nullable final Object filter )
  {
    final Connection connection = getConnection();
    return null == connection ? -1 : connection.lastIndexOfPendingAreaOfInterestRequest( action, address, filter );
  }

  final void requestSubscribe( @Nonnull final ChannelAddress address, @Nullable final Object filter )
  {
    ensureConnection().requestSubscribe( address, filter );
    triggerScheduler();
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new SubscribeRequestQueuedEvent( address, filter ) );
    }
  }

  final void requestSubscriptionUpdate( @Nonnull final ChannelAddress address,
                                        @Nullable final Object filter )
  {
    //TODO: Verify that this address is for an updateable channel
    ensureConnection().requestSubscriptionUpdate( address, filter );
    triggerScheduler();
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new SubscriptionUpdateRequestQueuedEvent( address, filter ) );
    }
  }

  final void requestUnsubscribe( @Nonnull final ChannelAddress address )
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
  final ChannelAddress toAddress( @Nonnull final ChannelChange channelChange )
  {
    final int channelId = channelChange.getChannelId();
    final Integer subChannelId = channelChange.hasSubChannelId() ? channelChange.getSubChannelId() : null;
    return new ChannelAddress( getSchema().getId(), channelId, subChannelId );
  }

  @Action
  protected void processChannelChanges( @Nonnull final MessageResponse response )
  {
    final ChangeSet changeSet = response.getChangeSet();
    final ChannelChange[] channelChanges = changeSet.getChannelChanges();
    for ( final ChannelChange channelChange : channelChanges )
    {
      final ChannelAddress address = toAddress( channelChange );
      final Object filter = channelChange.getChannelFilter();
      final ChannelChange.Action actionType = channelChange.getAction();

      if ( ChannelChange.Action.ADD == actionType )
      {
        response.incChannelAddCount();
        final boolean explicitSubscribe =
          ensureConnection()
            .getCurrentAreaOfInterestRequests()
            .stream()
            .anyMatch( a -> a.isInProgress() && a.getAddress().equals( address ) );
        getReplicantContext().createSubscription( address, filter, explicitSubscribe );
      }
      else if ( ChannelChange.Action.REMOVE == actionType )
      {
        final Subscription subscription = getReplicantContext().findSubscription( address );
        if ( Replicant.shouldCheckInvariants() )
        {
          invariant( () -> null != subscription,
                     () -> "Replicant-0028: Received ChannelChange of type REMOVE for address " + address +
                           " but no such subscription exists." );
          assert null != subscription;
        }
        assert null != subscription;
        Disposable.dispose( subscription );
        response.incChannelRemoveCount();
      }
      else
      {
        assert ChannelChange.Action.UPDATE == actionType;
        final Subscription subscription = getReplicantContext().findSubscription( address );
        if ( Replicant.shouldCheckInvariants() )
        {
          invariant( () -> null != subscription,
                     () -> "Replicant-0033: Received ChannelChange of type UPDATE for address " + address +
                           " but no such subscription exists." );
          assert null != subscription;
          invariant( subscription::isExplicitSubscription,
                     () -> "Replicant-0029: Received ChannelChange of type UPDATE for address " + address +
                           " but subscription is implicitly subscribed." );
        }
        assert null != subscription;
        subscription.setFilter( filter );
        updateSubscriptionForFilteredEntities( subscription );
        response.incChannelUpdateCount();
      }
    }
    response.markChannelActionsProcessed();
  }

  @Action( reportParameters = false )
  protected void processEntityLinks( @Nonnull final MessageResponse currentAction )
  {
    Linkable linkable;
    for ( int i = 0; i < _linksToProcessPerTick && null != ( linkable = currentAction.nextEntityToLink() ); i++ )
    {
      linkable.link();
      currentAction.incEntityLinkCount();
    }
  }

  /**
   * Method invoked when a filter has updated and the Connector needs to delink any entities
   * that are no longer part of the subscription now that the filter has changed.
   *
   * @param subscription the subscription that was updated.
   */
  final void updateSubscriptionForFilteredEntities( @Nonnull final Subscription subscription )
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

  @Action
  protected void removeExplicitSubscriptions( @Nonnull final List<AreaOfInterestRequest> requests )
  {
    requests.forEach( request -> {
      if ( Replicant.shouldCheckInvariants() )
      {
        invariant( () -> AreaOfInterestRequest.Type.REMOVE == request.getType(),
                   () -> "Replicant-0034: Connector.removeExplicitSubscriptions() invoked with request " +
                         "with type that is not REMOVE. Request: " + request );
      }
      final Subscription subscription = getReplicantContext().findSubscription( request.getAddress() );
      if ( null != subscription )
      {
        /*
         * It is unclear whether this code is actually required as should note the response form the server
         * automatically setExplicitSubscription to false?
         */
        subscription.setExplicitSubscription( false );
      }
    } );
  }

  @Action
  protected void removeUnneededAddRequests( @Nonnull final List<AreaOfInterestRequest> requests )
  {
    requests.removeIf( request -> {
      final ChannelAddress address = request.getAddress();
      final Subscription subscription = getReplicantContext().findSubscription( address );
      if ( Replicant.shouldCheckInvariants() )
      {
        invariant( () -> null == subscription || !subscription.isExplicitSubscription(),
                   () -> "Replicant-0030: Request to add channel at address " + address +
                         " but already explicitly subscribed to channel." );
      }
      if ( null != subscription && !subscription.isExplicitSubscription() )
      {
        // Existing subscription converted to an explicit subscription
        subscription.setExplicitSubscription( true );
        request.markAsComplete();
        return true;
      }
      else
      {
        return false;
      }
    } );
  }

  @Action
  protected void removeUnneededUpdateRequests( @Nonnull final List<AreaOfInterestRequest> requests )
  {
    requests.removeIf( a -> {
      final ChannelAddress address = a.getAddress();
      final Subscription subscription = getReplicantContext().findSubscription( address );
      if ( Replicant.shouldCheckInvariants() )
      {
        invariant( () -> null != subscription,
                   () -> "Replicant-0048: Request to update channel at address " + address +
                         " but not subscribed to channel." );
      }
      // The following code can probably be removed but it was present in the previous system
      // and it is unclear if there is any scenarios where it can still happen. The code has
      // been left in until we can verify it is no longer an issue. The above invariants will trigger
      // in development mode to help us track down these scenarios
      if ( null == subscription )
      {
        a.markAsComplete();
        return true;
      }
      else
      {
        return false;
      }
    } );
  }

  @Action
  protected void removeUnneededRemoveRequests( @Nonnull final List<AreaOfInterestRequest> requests )
  {
    requests.removeIf( request -> {
      final ChannelAddress address = request.getAddress();
      final Subscription subscription = getReplicantContext().findSubscription( address );
      if ( Replicant.shouldCheckInvariants() )
      {
        invariant( () -> null != subscription,
                   () -> "Replicant-0046: Request to unsubscribe from channel at address " + address +
                         " but not subscribed to channel." );
        invariant( () -> null == subscription || subscription.isExplicitSubscription(),
                   () -> "Replicant-0047: Request to unsubscribe from channel at address " + address +
                         " but subscription is not an explicit subscription." );
      }
      // The following code can probably be removed but it was present in the previous system
      // and it is unclear if there is any scenarios where it can still happen. The code has
      // been left in until we can verify it is no longer an issue. The above invariants will trigger
      // in development mode to help us track down these scenarios
      if ( null == subscription || !subscription.isExplicitSubscription() )
      {
        request.markAsComplete();
        return true;
      }
      else
      {
        return false;
      }
    } );
  }

  @Action( reportParameters = false )
  protected void processEntityChanges( @Nonnull final MessageResponse response )
  {
    EntityChange change;
    for ( int i = 0; i < _changesToProcessPerTick && null != ( change = response.nextChange() ); i++ )
    {
      //TODO: Inline all or the majority of the ChangeMappers logic here
      final Object entity = getChangeMapper().applyChange( change );
      if ( change.isUpdate() )
      {
        response.incEntityUpdateCount();
        response.changeProcessed( entity );
      }
      else
      {
        response.incEntityRemoveCount();
      }
    }
  }

  @Nonnull
  protected abstract ChangeMapper getChangeMapper();

  @Nonnull
  protected abstract SubscriptionUpdateEntityFilter getSubscriptionUpdateFilter();

  protected void validateWorld( @Nonnull final MessageResponse response )
  {
    response.markWorldAsValidated();
    if ( Replicant.shouldValidateEntitiesOnLoad() )
    {
      getReplicantContext().getValidator().validateEntities();
    }
  }

  /**
   * The AreaOfInterestRequest currently being processed can be completed and
   * trigger scheduler to start next step.
   */
  protected final void completeAreaOfInterestRequest()
  {
    ensureConnection().completeAreaOfInterestRequest();
    triggerScheduler();
  }

  /**
   * Invoked to fire an event when disconnect has completed.
   */
  @Action
  protected void onConnected()
  {
    setState( ConnectorState.CONNECTED );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new ConnectedEvent( getSchema().getId(), getSchema().getName() ) );
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
      getReplicantContext().getSpy()
        .reportSpyEvent( new ConnectFailureEvent( getSchema().getId(), getSchema().getName(), error ) );
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
      getReplicantContext().getSpy()
        .reportSpyEvent( new DisconnectedEvent( getSchema().getId(), getSchema().getName() ) );
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
      getReplicantContext().getSpy()
        .reportSpyEvent( new DisconnectFailureEvent( getSchema().getId(), getSchema().getName(), error ) );
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
      getReplicantContext().getSpy()
        .reportSpyEvent( new MessageProcessedEvent( getSchema().getId(), getSchema().getName(), status ) );
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
      getReplicantContext().getSpy()
        .reportSpyEvent( new MessageProcessFailureEvent( getSchema().getId(), getSchema().getName(), error ) );
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
      getReplicantContext().getSpy()
        .reportSpyEvent( new MessageReadFailureEvent( getSchema().getId(), getSchema().getName(), error ) );
    }
    disconnectIfPossible( error );
  }

  final void disconnectIfPossible( @Nonnull final Throwable cause )
  {
    if ( !ConnectorState.isTransitionState( getState() ) )
    {
      if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
      {
        getReplicantContext().getSpy()
          .reportSpyEvent( new RestartEvent( getSchema().getId(), getSchema().getName(), cause ) );
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
      getReplicantContext().getSpy()
        .reportSpyEvent( new SubscribeStartedEvent( getSchema().getId(), getSchema().getName(), address ) );
    }
  }

  @Action
  protected void onSubscribeCompleted( @Nonnull final ChannelAddress address )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.LOADED, null );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy()
        .reportSpyEvent( new SubscribeCompletedEvent( getSchema().getId(), getSchema().getName(), address ) );
    }
  }

  @Action
  protected void onSubscribeFailed( @Nonnull final ChannelAddress address, @Nonnull final Throwable error )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.LOAD_FAILED, error );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy()
        .reportSpyEvent( new SubscribeFailedEvent( getSchema().getId(), getSchema().getName(), address, error ) );
    }
  }

  @Action
  protected void onUnsubscribeStarted( @Nonnull final ChannelAddress address )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.UNLOADING, null );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy()
        .reportSpyEvent( new UnsubscribeStartedEvent( getSchema().getId(), getSchema().getName(), address ) );
    }
  }

  @Action
  protected void onUnsubscribeCompleted( @Nonnull final ChannelAddress address )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.UNLOADED, null );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy()
        .reportSpyEvent( new UnsubscribeCompletedEvent( getSchema().getId(), getSchema().getName(), address ) );
    }
  }

  @Action
  protected void onUnsubscribeFailed( @Nonnull final ChannelAddress address, @Nonnull final Throwable error )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.UNLOADED, null );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy()
        .reportSpyEvent( new UnsubscribeFailedEvent( getSchema().getId(), getSchema().getName(), address, error ) );
    }
  }

  @Action
  protected void onSubscriptionUpdateStarted( @Nonnull final ChannelAddress address )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.UPDATING, null );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy()
        .reportSpyEvent( new SubscriptionUpdateStartedEvent( getSchema().getId(), getSchema().getName(), address ) );
    }
  }

  @Action
  protected void onSubscriptionUpdateCompleted( @Nonnull final ChannelAddress address )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.UPDATED, null );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy()
        .reportSpyEvent( new SubscriptionUpdateCompletedEvent( getSchema().getId(), getSchema().getName(), address ) );
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
        .reportSpyEvent( new SubscriptionUpdateFailedEvent( getSchema().getId(),
                                                            getSchema().getName(),
                                                            address,
                                                            error ) );
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
  final ReplicantRuntime getReplicantRuntime()
  {
    return getReplicantContext().getRuntime();
  }

  @ContextRef
  @Nonnull
  protected abstract ArezContext context();

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return Replicant.areNamesEnabled() ? "Connector[" + getSchema().getName() + "]" : super.toString();
  }

  @TestOnly
  @Nullable
  final Disposable getSchedulerLock()
  {
    return _schedulerLock;
  }
}
