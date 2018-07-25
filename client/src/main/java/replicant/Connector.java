package replicant;

import arez.ArezContext;
import arez.Disposable;
import arez.annotations.Action;
import arez.annotations.ArezComponent;
import arez.annotations.ContextRef;
import arez.annotations.Feature;
import arez.annotations.Observable;
import arez.annotations.PostConstruct;
import arez.annotations.PreDispose;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.messages.ChangeSet;
import replicant.messages.ChannelChange;
import replicant.messages.EntityChange;
import replicant.messages.EntityChangeData;
import replicant.messages.EntityChannel;
import replicant.spy.ConnectFailureEvent;
import replicant.spy.ConnectedEvent;
import replicant.spy.DisconnectFailureEvent;
import replicant.spy.DisconnectedEvent;
import replicant.spy.InSyncEvent;
import replicant.spy.MessageProcessFailureEvent;
import replicant.spy.MessageProcessedEvent;
import replicant.spy.MessageReadFailureEvent;
import replicant.spy.OutOfSyncEvent;
import replicant.spy.RestartEvent;
import replicant.spy.SubscribeCompletedEvent;
import replicant.spy.SubscribeFailedEvent;
import replicant.spy.SubscribeRequestQueuedEvent;
import replicant.spy.SubscribeStartedEvent;
import replicant.spy.SubscriptionUpdateCompletedEvent;
import replicant.spy.SubscriptionUpdateFailedEvent;
import replicant.spy.SubscriptionUpdateRequestQueuedEvent;
import replicant.spy.SubscriptionUpdateStartedEvent;
import replicant.spy.SyncFailureEvent;
import replicant.spy.SyncRequestEvent;
import replicant.spy.UnsubscribeCompletedEvent;
import replicant.spy.UnsubscribeFailedEvent;
import replicant.spy.UnsubscribeRequestQueuedEvent;
import replicant.spy.UnsubscribeStartedEvent;
import static org.realityforge.braincheck.Guards.*;

/**
 * The Connector is responsible for managing a Connection to a backend datasource.
 */
@ArezComponent( observable = Feature.ENABLE )
abstract class Connector
  extends ReplicantService
{
  private static final int DEFAULT_LINKS_TO_PROCESS_PER_TICK = 20;
  private static final int DEFAULT_CHANGES_TO_PROCESS_PER_TICK = 20;
  /**
   * The schema that defines data-API used to interact with datasource.
   */
  @Nonnull
  private final SystemSchema _schema;
  /**
   * The transport that connects to the backend system.
   */
  @Nonnull
  private final Transport _transport;
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
   * Flag when the scheduler has been explicitly paused.
   * When this is true, the {@link #progressMessages()} will terminate the next time
   * it is invoked and the scheduler will not be activated. This is
   */
  private boolean _schedulerPaused;
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
  /**
   * Action invoked after current MessageResponse is processed. This is typically used to update or alter
   * change Connection on message processing complete.
   */
  @Nullable
  private SafeProcedure _postMessageResponseAction;

  @Nonnull
  static Connector create( @Nullable final ReplicantContext context,
                           @Nonnull final SystemSchema schema,
                           @Nonnull final Transport transport )
  {
    return new Arez_Connector( context, schema, transport );
  }

  Connector( @Nullable final ReplicantContext context,
             @Nonnull final SystemSchema schema,
             @Nonnull final Transport transport )
  {
    super( context );
    _schema = Objects.requireNonNull( schema );
    _transport = Objects.requireNonNull( transport );
  }

  @PostConstruct
  final void postConstruct()
  {
    getReplicantRuntime().registerConnector( this );
    getReplicantContext().getSchemaService().registerSchema( _schema );
  }

  @PreDispose
  final void preDispose()
  {
    _schedulerPaused = true;
    _schedulerActive = false;
    releaseSchedulerLock();
    getReplicantContext().getSchemaService().deregisterSchema( _schema );
  }

  /**
   * Connect to the underlying data source.
   */
  void connect()
  {
    final ConnectorState state = getState();
    if ( ConnectorState.CONNECTING != state && ConnectorState.CONNECTED != state )
    {
      ConnectorState newState = ConnectorState.ERROR;
      try
      {
        getTransport().connect( this::onConnection, this::onConnectFailure );
        newState = ConnectorState.CONNECTING;
      }
      finally
      {
        setState( newState );
      }
    }
  }

  /**
   * Transport has disconnected so now we need to trigger a disconnect of Connector.
   */
  @Action
  void transportDisconnect()
  {
    //Wrap the underlying disconnect in an action
    disconnect();
  }

  /**
   * Disconnect from underlying data source.
   */
  void disconnect()
  {
    final ConnectorState state = getState();
    if ( ConnectorState.DISCONNECTING != state && ConnectorState.DISCONNECTED != state )
    {
      ConnectorState newState = ConnectorState.ERROR;
      try
      {
        getTransport().disconnect( this::onDisconnection, this::onDisconnectionError );
        newState = ConnectorState.DISCONNECTING;
      }
      finally
      {
        setState( newState );
      }
    }
  }

  /**
   * Return the schema associated with the connector.
   *
   * @return the schema associated with the connector.
   */
  @Nonnull
  final SystemSchema getSchema()
  {
    return _schema;
  }

  final void onConnection( @Nonnull final String connectionId )
  {
    doSetConnection( new Connection( this, connectionId ) );
    triggerMessageScheduler();
  }

  private void onDisconnectionError( @Nonnull final Throwable error )
  {
    onDisconnection();
    onDisconnectFailure( error );
  }

  final void onDisconnection()
  {
    doSetConnection( null );
  }

  private void doSetConnection( @Nullable final Connection connection )
  {
    final Connection existing = getConnection();
    if ( null != existing )
    {
      Disposable.dispose( existing );
    }
    if ( null == existing || null == existing.getCurrentMessageResponse() )
    {
      setConnection( connection );
    }
    else
    {
      setPostMessageResponseAction( () -> setConnection( connection ) );
    }
  }

  final void setConnection( @Nullable final Connection connection )
  {
    _connection = connection;
    recordLastTxRequestId( 0 );
    recordLastRxRequestId( 0 );
    recordLastSyncTxRequestId( 0 );
    recordLastSyncRxRequestId( 0 );
    recordSyncInFlight( false );
    recordPendingResponseQueueEmpty( true );
    purgeSubscriptions();
    if ( null != _connection )
    {
      onConnected();
    }
    else
    {
      onDisconnected();
    }
  }

  @Nullable
  final Connection getConnection()
  {
    return _connection;
  }

  @Nonnull
  final Connection ensureConnection()
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> null != _connection,
                 () -> "Replicant-0031: Connector.ensureConnection() when no connection is present." );
    }
    assert null != _connection;
    return _connection;
  }

  @Nonnull
  private MessageResponse ensureCurrentMessageResponse()
  {
    return ensureConnection().ensureCurrentMessageResponse();
  }

  @Nonnull
  final Transport getTransport()
  {
    return _transport;
  }

  @Action
  protected void purgeSubscriptions()
  {
    final SubscriptionService subscriptionService = getReplicantContext().getSubscriptionService();
    Stream.concat( subscriptionService.getTypeSubscriptions().stream(),
                   subscriptionService.getInstanceSubscriptions().stream() )
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

  final void requestSync()
  {
    recordSyncInFlight( true );
    getTransport().requestSync( this::onInSync, this::onOutOfSync, this::onSyncError );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new SyncRequestEvent( getSchema().getId() ) );
    }
  }

  final void requestSubscribe( @Nonnull final ChannelAddress address, @Nullable final Object filter )
  {
    ensureConnection().requestSubscribe( address, filter );
    triggerMessageScheduler();
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new SubscribeRequestQueuedEvent( address, filter ) );
    }
  }

  final void requestSubscriptionUpdate( @Nonnull final ChannelAddress address,
                                        @Nullable final Object filter )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> ChannelSchema.FilterType.DYNAMIC ==
                       getSchema().getChannel( address.getChannelId() ).getFilterType(),
                 () -> "Replicant-0082: Connector.requestSubscriptionUpdate invoked for channel " + address +
                       " but channel does not have a dynamic filter." );
    }
    ensureConnection().requestSubscriptionUpdate( address, filter );
    triggerMessageScheduler();
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new SubscriptionUpdateRequestQueuedEvent( address, filter ) );
    }
  }

  final void requestUnsubscribe( @Nonnull final ChannelAddress address )
  {
    ensureConnection().requestUnsubscribe( address );
    triggerMessageScheduler();
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
  private void triggerMessageScheduler()
  {
    if ( !_schedulerActive )
    {
      _schedulerActive = true;

      if ( !_schedulerPaused )
      {
        activateMessageScheduler();
      }
    }
  }

  final boolean isSchedulerPaused()
  {
    return _schedulerPaused;
  }

  final void pauseMessageScheduler()
  {
    _schedulerPaused = true;
  }

  final void resumeMessageScheduler()
  {
    if ( _schedulerPaused )
    {
      _schedulerPaused = false;
      if ( _schedulerActive )
      {
        activateMessageScheduler();
      }
    }
  }

  /**
   * Perform a single step progressing requests and responses.
   * This is invoked from the scheduler and will continue to be
   * invoked until it returns false.
   *
   * @return true if more work is to be done.
   */
  final boolean progressMessages()
  {
    if ( _schedulerPaused )
    {
      return false;
    }
    if ( null == _schedulerLock )
    {
      _schedulerLock = context().pauseScheduler();
    }
    try
    {
      final Connection connection = getConnection();
      if ( null != connection )
      {
        final boolean step1 = progressAreaOfInterestRequestProcessing();
        final boolean step2 = progressResponseProcessing();
        _schedulerActive = step1 || step2;
      }
      else
      {
        /*
         * This can happen when a connection has been disconnected before the timer triggers
         * that invokes progressMessages() - this can happen in a few scenarios but most of
         * them are the result of errors occurring and connection being removed on error
         */
        _schedulerActive = false;
        callPostMessageResponseActionIfPresent();
      }
    }
    catch ( final Throwable e )
    {
      onMessageProcessFailure( e );
      _schedulerActive = false;
      releaseSchedulerLock();
      return false;
    }
    finally
    {
      if ( !_schedulerActive )
      {
        releaseSchedulerLock();
      }
    }
    return _schedulerActive;
  }

  private void releaseSchedulerLock()
  {
    if ( null != _schedulerLock )
    {
      _schedulerLock.dispose();
      _schedulerLock = null;
    }
  }

  /**
   * Activate the scheduler.
   * This involves creating a scheduler that will invoke {@link #progressMessages()} until
   * that method returns false.
   */
  private void activateMessageScheduler()
  {
    Scheduler.schedule( this::progressMessages );
  }

  /**
   * Perform a single step processing messages received from the server.
   *
   * @return true if more work is to be done.
   */
  boolean progressResponseProcessing()
  {
    final Connection connection = ensureConnection();
    final MessageResponse response = connection.getCurrentMessageResponse();
    if ( null == response )
    {
      // Select the MessageResponse if there is none active
      return connection.selectNextMessageResponse();
    }
    else if ( response.needsParsing() )
    {
      // Parse the json
      parseMessageResponse();
      return true;
    }
    else if ( response.needsChannelChangesProcessed() )
    {
      // Process the updates to channels
      processChannelChanges();
      return true;
    }
    else if ( response.areEntityChangesPending() )
    {
      // Process a chunk of entity changes
      processEntityChanges();
      return true;
    }
    else if ( response.areEntityLinksPending() )
    {
      // Process a chunk of links
      processEntityLinks();
      return true;
    }
    else if ( !response.hasWorldBeenValidated() )
    {
      releaseSchedulerLock();
      // Validate the world after the change set has been applied (if feature is enabled)
      validateWorld();
      return true;
    }
    else
    {
      // We have to also release scheduler lock here in scenario where system not configured to validate world
      releaseSchedulerLock();
      completeMessageResponse();
      return true;
    }
  }

  /*
   * The following are Arez observable properties used to expose state from non-arez enabled
   * elements. The other elements explicitly set state into variables which is picked up by
   * interested observers.
   */

  @Observable
  abstract int getLastSyncRxRequestId();

  abstract void setLastSyncRxRequestId( int requestId );

  @Action
  void recordLastSyncRxRequestId( final int requestId )
  {
    setLastSyncRxRequestId( requestId );
  }

  @Observable
  abstract int getLastSyncTxRequestId();

  abstract void setLastSyncTxRequestId( int requestId );

  @Action
  void recordLastSyncTxRequestId( final int requestId )
  {
    setLastSyncTxRequestId( requestId );
  }

  @Observable
  abstract int getLastTxRequestId();

  abstract void setLastTxRequestId( int lastTxRequestId );

  @Action
  void recordLastTxRequestId( final int lastTxRequestId )
  {
    setLastTxRequestId( lastTxRequestId );
  }

  @Observable
  abstract int getLastRxRequestId();

  abstract void setLastRxRequestId( int lastRxRequestId );

  @Action
  void recordLastRxRequestId( final int lastRxRequestId )
  {
    setLastRxRequestId( lastRxRequestId );
  }

  @Observable
  abstract boolean isSyncInFlight();

  abstract void setSyncInFlight( boolean syncInFlight );

  @Action
  void recordSyncInFlight( final boolean syncInFlight )
  {
    setSyncInFlight( syncInFlight );
  }

  @Observable
  abstract boolean isPendingResponseQueueEmpty();

  abstract void setPendingResponseQueueEmpty( boolean isEmpty );

  @Action
  void recordPendingResponseQueueEmpty( final boolean isEmpty )
  {
    setPendingResponseQueueEmpty( isEmpty );
  }

  boolean isSynchronized()
  {
    return getState() == ConnectorState.CONNECTED &&
           // Last request acknowledged is last request responded to
           getLastSyncRxRequestId() == getLastRxRequestId() &&
           // No requests outwards bound
           getLastSyncRxRequestId() == getLastTxRequestId() &&
           // No messages pending processing
           isPendingResponseQueueEmpty();
  }

  boolean shouldRequestSync()
  {
    return getState() == ConnectorState.CONNECTED &&
           !(
             // Last request acknowledged is last request responded to
             getLastSyncRxRequestId() == getLastRxRequestId() &&
             // No requests outwards bound
             getLastSyncRxRequestId() == getLastTxRequestId()
           ) &&
           // No requests in flight
           getLastRxRequestId() == getLastTxRequestId() &&
           // No sync requests are in flight
           !isSyncInFlight() &&
           // No messages pending processing, otherwise can pick up later
           isPendingResponseQueueEmpty();
  }

  /**
   * {@inheritDoc}
   */
  @Nonnull
  @Observable
  ConnectorState getState()
  {
    return _state;
  }

  protected void setState( @Nonnull final ConnectorState state )
  {
    _state = Objects.requireNonNull( state );
    if ( ConnectorState.ERROR == _state || ConnectorState.DISCONNECTED == _state )
    {
      getTransport().unbind();
    }
    else if ( ConnectorState.CONNECTED == _state )
    {
      getTransport().bind( ensureConnection().getTransportContext(), getReplicantContext() );
    }
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
  protected void processChannelChanges()
  {
    final MessageResponse response = ensureCurrentMessageResponse();
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
          getReplicantContext()
            .getAreasOfInterest()
            .stream()
            .anyMatch( a -> a.getAddress().equals( address ) );
        getReplicantContext().getSubscriptionService().createSubscription( address, filter, explicitSubscribe );
      }
      else if ( ChannelChange.Action.REMOVE == actionType )
      {
        final Subscription subscription = getReplicantContext().findSubscription( address );
        /*
         * It is possible for a subscription to no longer be present and still receive a remove action
         * for the subscription. This can occur due to interleaving of messages - i.e. The application
         * initiates an action that deletes a root entity of an instance channel and then removes
         * subscription from the channel. Depending on the order in which the operations complete
         * could result in a channel remove action when not needed.
         */
        if ( null != subscription )
        {
          Disposable.dispose( subscription );
          response.incChannelRemoveCount();
        }
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
          if ( Replicant.shouldCheckInvariants() )
          {
            invariant( () -> ChannelSchema.FilterType.DYNAMIC ==
                             getSchema().getChannel( address.getChannelId() ).getFilterType(),
                       () -> "Replicant-0078: Received ChannelChange of type UPDATE for address " + address +
                             " but the channel does not have a DYNAMIC filter." );
          }
        }
        assert null != subscription;
        subscription.setFilter( filter );
        updateSubscriptionForFilteredEntities( subscription );
        response.incChannelUpdateCount();
      }
    }
    response.markChannelActionsProcessed();
  }

  @Action( verifyRequired = false )
  protected void processEntityLinks()
  {
    final MessageResponse response = ensureCurrentMessageResponse();
    Linkable linkable;
    for ( int i = 0; i < _linksToProcessPerTick && null != ( linkable = response.nextEntityToLink() ); i++ )
    {
      linkable.link();
      response.incEntityLinkCount();
    }
  }

  /**
   * Method invoked when a filter has updated and the Connector needs to delink any entities
   * that are no longer part of the subscription now that the filter has changed.
   *
   * @param subscription the subscription that was updated.
   */
  @SuppressWarnings( "unchecked" )
  final void updateSubscriptionForFilteredEntities( @Nonnull final Subscription subscription )
  {
    final ChannelAddress address = subscription.getAddress();
    final ChannelSchema channel = getSchema().getChannel( address.getChannelId() );
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> ChannelSchema.FilterType.DYNAMIC == channel.getFilterType(),
                 () -> "Replicant-0079: Connector.updateSubscriptionForFilteredEntities invoked for address " +
                       subscription.getAddress() + " but the channel does not have a DYNAMIC filter." );
    }

    for ( final Class<?> entityType : new ArrayList<>( subscription.findAllEntityTypes() ) )
    {
      final List<Entity> entities = subscription.findAllEntitiesByType( entityType );
      if ( !entities.isEmpty() )
      {
        final SubscriptionUpdateEntityFilter updateFilter = channel.getFilter();
        assert null != updateFilter;
        final Object filter = subscription.getFilter();
        for ( final Entity entity : entities )
        {
          if ( !updateFilter.doesEntityMatchFilter( filter, entity ) )
          {
            entity.delinkFromSubscription( subscription );
          }
        }
      }
    }
  }

  final void setPostMessageResponseAction( @Nullable final SafeProcedure postMessageResponseAction )
  {
    _postMessageResponseAction = postMessageResponseAction;
  }

  void completeMessageResponse()
  {
    final Connection connection = ensureConnection();
    final MessageResponse response = connection.ensureCurrentMessageResponse();

    // OOB messages are not sequenced
    if ( !response.isOob() )
    {
      connection.setLastRxSequence( response.getChangeSet().getSequence() );
    }

    //Step: Run the post actions
    final RequestEntry request = response.getRequest();
    if ( null != request )
    {
      request.markResultsAsArrived();
    }
    /*
     * An action will be returned if the message is an OOB message
     * or it is an answer to a response and the rpc invocation has
     * already returned.
     */
    final SafeProcedure action = response.getCompletionAction();
    if ( null != action )
    {
      action.call();
    }
    // OOB messages are not in response to requests (at least not request associated with the current connection)
    if ( !response.isOob() )
    {
      // We can remove the request because this side ran second and the RPC channel has already returned.
      final ChangeSet changeSet = response.getChangeSet();
      final Integer requestId = changeSet.getRequestId();
      if ( null != requestId )
      {
        connection.removeRequest( requestId );
      }
    }
    connection.setCurrentMessageResponse( null );
    onMessageProcessed( response );
    callPostMessageResponseActionIfPresent();

    recordPendingResponseQueueEmpty( connection.getPendingResponses().isEmpty() &&
                                     connection.getUnparsedResponses().isEmpty() );

    final ChangeSet changeSet = response.getChangeSet();
    if ( changeSet.hasChannelChanges() || changeSet.hasEntityChanges() )
    {
      // If message is not a ping response then try to perform sync
      maybeRequestSync();
    }
  }

  @Action
  void maybeRequestSync()
  {
    if ( shouldRequestSync() )
    {
      requestSync();
    }
  }

  private void callPostMessageResponseActionIfPresent()
  {
    if ( null != _postMessageResponseAction )
    {
      _postMessageResponseAction.call();
      _postMessageResponseAction = null;
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
         * It is unclear whether this code is actually required as should note the response from the server
         * automatically setExplicitSubscription to false?
         */
        subscription.setExplicitSubscription( false );
      }
    } );
  }

  @Action( verifyRequired = false )
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

  @Action( verifyRequired = false )
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

  @Action( verifyRequired = false )
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

  /**
   * Parse the json data associated with the current response and then enqueue it.
   */
  void parseMessageResponse()
  {
    final Connection connection = ensureConnection();
    final MessageResponse response = connection.ensureCurrentMessageResponse();
    final String rawJsonData = response.getRawJsonData();
    assert null != rawJsonData;
    final ChangeSet changeSet = ChangeSetParser.parseChangeSet( rawJsonData );
    if ( Replicant.shouldValidateChangeSetOnRead() )
    {
      changeSet.validate();
    }

    final RequestEntry request;
    if ( response.isOob() )
    {
      /*
       * OOB messages are really just cached messages at this stage and they are the
       * same bytes as originally sent down and then cached. So the requestId present
       * in the json blob is for old connection and can be ignored.
       */
      request = null;
    }
    else
    {
      final Integer requestId = changeSet.getRequestId();
      final int sequence = changeSet.getSequence();
      request = null != requestId ? connection.getRequest( requestId ) : null;
      if ( Replicant.shouldCheckApiInvariants() )
      {
        apiInvariant( () -> null != request || null == requestId,
                      () -> "Replicant-0066: Unable to locate request with id '" +
                            requestId + "' specified for ChangeSet with sequence " +
                            sequence + ". Existing Requests: " + connection.getRequests() );
      }
    }

    cacheMessageIfPossible( rawJsonData, changeSet );

    response.recordChangeSet( changeSet, request );
    connection.queueCurrentResponse();
  }

  private void cacheMessageIfPossible( @Nonnull final String rawJsonData, @Nonnull final ChangeSet changeSet )
  {
    final String eTag = changeSet.getETag();
    final CacheService cacheService = getReplicantContext().getCacheService();

    boolean candidate = false;
    if ( null != cacheService &&
         null != eTag &&
         changeSet.hasChannelChanges() )
    {
      final ChannelChange[] channelChanges = changeSet.getChannelChanges();

      if ( 1 == channelChanges.length &&
           ChannelChange.Action.ADD == channelChanges[ 0 ].getAction() &&
           getSchema().getChannel( channelChanges[ 0 ].getChannelId() ).isCacheable() )
      {
        final ChannelChange channelChange = channelChanges[ 0 ];
        final String cacheKey =
          "RC-" +
          getSchema().getId() +
          "." +
          channelChange.getChannelId() +
          ( channelChange.hasSubChannelId() ? "." + channelChange.getSubChannelId() : "" );
        cacheService.store( cacheKey, eTag, rawJsonData );
        candidate = true;
      }
    }
    if ( Replicant.shouldCheckApiInvariants() )
    {
      final boolean c = candidate;
      apiInvariant( () -> null == eTag || null == cacheService || c,
                    () -> "Replicant-0072: eTag in reply for ChangeSet " + changeSet.getSequence() +
                          " but ChangeSet is not a candidate for caching." );
    }
  }

  @SuppressWarnings( "unchecked" )
  @Action
  protected void processEntityChanges()
  {
    final MessageResponse response = ensureCurrentMessageResponse();
    EntityChange change;
    for ( int i = 0; i < _changesToProcessPerTick && null != ( change = response.nextEntityChange() ); i++ )
    {
      final int id = change.getId();
      final int typeId = change.getTypeId();
      final EntitySchema entitySchema = getSchema().getEntity( typeId );
      final Class<?> type = entitySchema.getType();
      Entity entity = getReplicantContext().getEntityService().findEntityByTypeAndId( type, id );
      if ( change.isRemove() )
      {
        /*
         * Sometimes a remove can occur for an entity that is no longer present on the client. The most
         * common cause of this is initiating an action that deletes an entity and then un-subscribing
         * from the channel that contains entity. This can result in an entity that has been removed
         * locally but has a remove message in the queue. Other interleaved async operations can also
         * trigger this scenario.
         */
        if ( null != entity )
        {
          Disposable.dispose( entity );
          response.incEntityRemoveCount();
        }
      }
      else
      {
        final EntityChangeData data = change.getData();
        if ( null == entity )
        {
          final String name = Replicant.areNamesEnabled() ? entitySchema.getName() + "/" + id : null;
          entity = getReplicantContext().getEntityService().findOrCreateEntity( name, type, id );
          final Object userObject = entitySchema.getCreator().createEntity( id, data );
          entity.setUserObject( userObject );

        }
        else
        {
          final EntitySchema.Updater updater = entitySchema.getUpdater();
          if ( null != updater )
          {
            updater.updateEntity( entity.getUserObject(), data );
          }
        }

        final EntityChannel[] changeCount = change.getChannels();
        final int schemaId = getSchema().getId();
        for ( final EntityChannel entityChannel : changeCount )
        {
          final ChannelAddress address = entityChannel.toAddress( schemaId );
          final Subscription subscription = getReplicantContext().findSubscription( address );
          if ( Replicant.shouldCheckInvariants() )
          {
            invariant( () -> null != subscription,
                       () -> "Replicant-0069: ChangeSet " + response.getChangeSet().getSequence() + " contained an " +
                             "EntityChange message referencing channel " + entityChannel.toAddress( schemaId ) +
                             " but no such subscription exists locally." );
          }
          assert null != subscription;
          entity.tryLinkToSubscription( subscription );
        }
        /*
         We could get the existing subscriptions for an entity, and any that are not present
         in the EntityChannel could be removed here. However we assume the code generated in
         subscription change will handle subscription changes and remove subscriptions no longer
         relevant.
         */

        response.incEntityUpdateCount();
        response.changeProcessed( entity.getUserObject() );
      }
    }
  }

  final void validateWorld()
  {
    ensureCurrentMessageResponse().markWorldAsValidated();
    if ( Replicant.shouldValidateEntitiesOnLoad() )
    {
      getReplicantContext().getValidator().validateEntities();
    }
  }

  /**
   * Perform a single step in sending one (or a batch) or requests to the server.
   */
  final boolean progressAreaOfInterestRequestProcessing()
  {
    final List<AreaOfInterestRequest> requests =
      new ArrayList<>( ensureConnection().getCurrentAreaOfInterestRequests() );
    if ( requests.isEmpty() )
    {
      return false;
    }
    else if ( requests.get( 0 ).isInProgress() )
    {
      return false;
    }
    else
    {
      requests.forEach( AreaOfInterestRequest::markAsInProgress );
      final AreaOfInterestRequest.Type type = requests.get( 0 ).getType();
      if ( AreaOfInterestRequest.Type.ADD == type )
      {
        progressAreaOfInterestAddRequests( requests );
      }
      else if ( AreaOfInterestRequest.Type.REMOVE == type )
      {
        progressAreaOfInterestRemoveRequests( requests );
      }
      else
      {
        progressAreaOfInterestUpdateRequests( requests );
      }
      return true;
    }
  }

  final void progressAreaOfInterestAddRequests( @Nonnull final List<AreaOfInterestRequest> requests )
  {
    removeUnneededAddRequests( requests );

    if ( requests.isEmpty() )
    {
      completeAreaOfInterestRequest();
    }
    else if ( 1 == requests.size() )
    {
      progressAreaOfInterestAddRequest( requests.get( 0 ) );
    }
    else
    {
      progressBulkAreaOfInterestAddRequests( requests );
    }
  }

  final void progressAreaOfInterestAddRequest( @Nonnull final AreaOfInterestRequest request )
  {
    final ChannelAddress address = request.getAddress();
    onSubscribeStarted( address );
    final SafeProcedure onSuccess = () -> {
      completeAreaOfInterestRequest();
      onSubscribeCompleted( address );
    };

    final Consumer<Throwable> onError = error ->
    {
      completeAreaOfInterestRequest();
      onSubscribeFailed( address, error );
    };

    final CacheService cacheService = getReplicantContext().getCacheService();
    final boolean cacheable =
      null != cacheService && getSchema().getChannel( request.getAddress().getChannelId() ).isCacheable();
    final CacheEntry cacheEntry = cacheable ? cacheService.lookup( request.getCacheKey() ) : null;
    if ( null != cacheEntry )
    {
      //Found locally cached data
      final String eTag = cacheEntry.getETag();
      final SafeProcedure onCacheValid = () ->
      {
        // Loading cached data
        completeAreaOfInterestRequest();
        ensureConnection().enqueueOutOfBandResponse( cacheEntry.getContent(), onSuccess );
        triggerMessageScheduler();
      };
      getTransport()
        .requestSubscribe( request.getAddress(), request.getFilter(), eTag, onCacheValid, onSuccess, onError );
    }
    else
    {
      getTransport().requestSubscribe( request.getAddress(), request.getFilter(), onSuccess, onError );
    }
  }

  final void progressBulkAreaOfInterestAddRequests( @Nonnull final List<AreaOfInterestRequest> requests )
  {
    final List<ChannelAddress> addresses =
      requests.stream().map( AreaOfInterestRequest::getAddress ).collect( Collectors.toList() );
    addresses.forEach( this::onSubscribeStarted );

    final SafeProcedure onSuccess = () -> {
      completeAreaOfInterestRequest();
      addresses.forEach( this::onSubscribeCompleted );
    };

    final Consumer<Throwable> onError = error -> {
      completeAreaOfInterestRequest();
      addresses.forEach( a -> onSubscribeFailed( a, error ) );
    };

    getTransport().requestBulkSubscribe( addresses, requests.get( 0 ).getFilter(), onSuccess, onError );
  }

  final void progressAreaOfInterestUpdateRequests( @Nonnull final List<AreaOfInterestRequest> requests )
  {
    removeUnneededUpdateRequests( requests );

    if ( requests.isEmpty() )
    {
      completeAreaOfInterestRequest();
    }
    else if ( requests.size() > 1 )
    {
      progressBulkAreaOfInterestUpdateRequests( requests );
    }
    else
    {
      progressAreaOfInterestUpdateRequest( requests.get( 0 ) );
    }
  }

  final void progressAreaOfInterestUpdateRequest( @Nonnull final AreaOfInterestRequest request )
  {
    final ChannelAddress address = request.getAddress();
    onSubscriptionUpdateStarted( address );
    final SafeProcedure onSuccess = () -> {
      completeAreaOfInterestRequest();
      onSubscriptionUpdateCompleted( address );
    };

    final Consumer<Throwable> onError = error ->
    {
      completeAreaOfInterestRequest();
      onSubscriptionUpdateFailed( address, error );
    };

    final Object filter = request.getFilter();
    assert null != filter;
    getTransport().requestSubscriptionUpdate( address, filter, onSuccess, onError );
  }

  final void progressBulkAreaOfInterestUpdateRequests( @Nonnull final List<AreaOfInterestRequest> requests )
  {
    final List<ChannelAddress> addresses =
      requests.stream().map( AreaOfInterestRequest::getAddress ).collect( Collectors.toList() );
    addresses.forEach( this::onSubscriptionUpdateStarted );
    final SafeProcedure onSuccess = () -> {
      completeAreaOfInterestRequest();
      addresses.forEach( this::onSubscriptionUpdateCompleted );
    };

    final Consumer<Throwable> onError = error -> {
      completeAreaOfInterestRequest();
      addresses.forEach( a -> onSubscriptionUpdateFailed( a, error ) );
    };
    // All filters will be the same if they are grouped
    final Object filter = requests.get( 0 ).getFilter();
    assert null != filter;
    getTransport().requestBulkSubscriptionUpdate( addresses, filter, onSuccess, onError );
  }

  final void progressAreaOfInterestRemoveRequests( @Nonnull final List<AreaOfInterestRequest> requests )
  {
    removeUnneededRemoveRequests( requests );

    if ( requests.isEmpty() )
    {
      completeAreaOfInterestRequest();
    }
    else if ( requests.size() > 1 )
    {
      progressBulkAreaOfInterestRemoveRequests( requests );
    }
    else
    {
      progressAreaOfInterestRemoveRequest( requests.get( 0 ) );
    }
  }

  final void progressAreaOfInterestRemoveRequest( @Nonnull final AreaOfInterestRequest request )
  {
    final ChannelAddress address = request.getAddress();
    onUnsubscribeStarted( address );
    final SafeProcedure onSuccess = () -> {
      removeExplicitSubscriptions( Collections.singletonList( request ) );
      completeAreaOfInterestRequest();
      onUnsubscribeCompleted( address );
    };

    final Consumer<Throwable> onError = error ->
    {
      removeExplicitSubscriptions( Collections.singletonList( request ) );
      completeAreaOfInterestRequest();
      onUnsubscribeFailed( address, error );
    };

    getTransport().requestUnsubscribe( address, onSuccess, onError );
  }

  final void progressBulkAreaOfInterestRemoveRequests( @Nonnull final List<AreaOfInterestRequest> requests )
  {
    final List<ChannelAddress> addresses =
      requests.stream().map( AreaOfInterestRequest::getAddress ).collect( Collectors.toList() );
    addresses.forEach( this::onUnsubscribeStarted );

    final SafeProcedure onSuccess = () -> {
      removeExplicitSubscriptions( requests );
      completeAreaOfInterestRequest();
      addresses.forEach( this::onUnsubscribeCompleted );
    };

    final Consumer<Throwable> onError = error -> {
      removeExplicitSubscriptions( requests );
      completeAreaOfInterestRequest();
      addresses.forEach( a -> onUnsubscribeFailed( a, error ) );
    };

    getTransport().requestBulkUnsubscribe( addresses, onSuccess, onError );
  }

  /**
   * The AreaOfInterestRequest currently being processed can be completed and
   * trigger scheduler to start next step.
   */
  final void completeAreaOfInterestRequest()
  {
    /*
     * Sometimes an AreaOfInterestRequest completes during a disconnection or network failure.
     * i.e. This could be called in response to an error as a result of network failure or it could
     * overlap a disconnect request.
     */
    if ( null != _connection )
    {
      _connection.completeAreaOfInterestRequest();
    }
    triggerMessageScheduler();
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
   * Invoked when a transport received a message.
   *
   * @param rawJsonData the message.
   */
  void onMessageReceived( @Nonnull String rawJsonData )
  {
    ensureConnection().enqueueResponse( rawJsonData );
    triggerMessageScheduler();
  }

  /**
   * Invoked when a change set has been completely processed.
   *
   * @param response the message response.
   */
  void onMessageProcessed( @Nonnull final MessageResponse response )
  {
    getTransport().onMessageProcessed();
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy()
        .reportSpyEvent( new MessageProcessedEvent( getSchema().getId(), getSchema().getName(), response.toStatus() ) );
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

  void onInSync()
  {
    recordSyncInFlight( false );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new InSyncEvent( getSchema().getId() ) );
    }
  }

  void onOutOfSync()
  {
    recordSyncInFlight( false );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new OutOfSyncEvent( getSchema().getId() ) );
    }
  }

  void onSyncError( @Nonnull final Throwable error )
  {
    recordSyncInFlight( false );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new SyncFailureEvent( getSchema().getId(), error ) );
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

  @Nullable
  final Disposable getSchedulerLock()
  {
    return _schedulerLock;
  }

  @Nullable
  final SafeProcedure getPostMessageResponseAction()
  {
    return _postMessageResponseAction;
  }
}
