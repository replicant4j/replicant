package replicant;

import akasha.core.JSON;
import arez.Arez;
import arez.ArezContext;
import arez.Disposable;
import arez.annotations.Action;
import arez.annotations.ArezComponent;
import arez.annotations.CascadeDispose;
import arez.annotations.ContextRef;
import arez.annotations.Feature;
import arez.annotations.Memoize;
import arez.annotations.Observable;
import arez.annotations.PostConstruct;
import arez.annotations.PreDispose;
import arez.component.Linkable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.messages.EntityChange;
import replicant.messages.EntityChangeData;
import replicant.messages.ErrorMessage;
import replicant.messages.OkMessage;
import replicant.messages.ServerToClientMessage;
import replicant.messages.UpdateMessage;
import replicant.messages.UseCacheMessage;
import replicant.spy.ConnectFailureEvent;
import replicant.spy.ConnectedEvent;
import replicant.spy.DisconnectFailureEvent;
import replicant.spy.DisconnectedEvent;
import replicant.spy.ExecCompletedEvent;
import replicant.spy.ExecRequestQueuedEvent;
import replicant.spy.ExecStartedEvent;
import replicant.spy.InSyncEvent;
import replicant.spy.MessageProcessFailureEvent;
import replicant.spy.MessageProcessedEvent;
import replicant.spy.MessageReadFailureEvent;
import replicant.spy.OutOfSyncEvent;
import replicant.spy.RestartEvent;
import replicant.spy.SubscribeCompletedEvent;
import replicant.spy.SubscribeRequestQueuedEvent;
import replicant.spy.SubscribeStartedEvent;
import replicant.spy.SubscriptionUpdateCompletedEvent;
import replicant.spy.SubscriptionUpdateRequestQueuedEvent;
import replicant.spy.SubscriptionUpdateStartedEvent;
import replicant.spy.SyncRequestEvent;
import replicant.spy.UnsubscribeCompletedEvent;
import replicant.spy.UnsubscribeRequestQueuedEvent;
import replicant.spy.UnsubscribeStartedEvent;
import static org.realityforge.braincheck.Guards.*;

/**
 * The Connector is responsible for managing a Connection to a backend datasource.
 */
@ArezComponent( observable = Feature.ENABLE, requireId = Feature.DISABLE )
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
  @CascadeDispose
  Connection _connection;
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
  @Nullable
  private TransportContextImpl _context;

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
  void postConstruct()
  {
    getReplicantRuntime().registerConnector( this );
    getReplicantContext().getSchemaService().registerSchema( _schema );
  }

  @PreDispose
  void preDispose()
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
        Disposable.dispose( _context );
        _context = new TransportContextImpl( this );
        _transport.requestConnect( _context );
        newState = ConnectorState.CONNECTING;
      }
      finally
      {
        setState( newState );
      }
    }
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
        _transport.requestDisconnect();
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
  SystemSchema getSchema()
  {
    return _schema;
  }

  void onConnection( @Nonnull final String connectionId )
  {
    final Connection connection = Connection.create( this );
    connection.setConnectionId( connectionId );
    doSetConnection( connection );
    triggerMessageScheduler();
  }

  void onDisconnection()
  {
    doSetConnection( null );
  }

  private void doSetConnection( @Nullable final Connection connection )
  {
    if ( !Objects.equals( connection, _connection ) )
    {
      if ( null == _connection || null == _connection.getCurrentMessageResponse() )
      {
        setConnection( connection );
      }
      else
      {
        setPostMessageResponseAction( () -> setConnection( connection ) );
      }
    }
  }

  void setConnection( @Nullable final Connection connection )
  {
    _connection = connection;
    // Lock arez otherwise the purgeSubscriptions will trigger a converge when
    // _connection is null but State may be CONNECTED
    final Disposable schedulerLock = Arez.context().pauseScheduler();
    purgeSubscriptions();
    // Avoid emitting an event if disconnect resulted in an error
    if ( ConnectorState.ERROR != getState() && ConnectorState.FATAL_ERROR != getState() )
    {
      if ( null != _connection )
      {
        sendAuthTokenIfAny();
        sendEtagsIfAny();
        onConnected();
      }
      else
      {
        onDisconnected();
      }
    }
    schedulerLock.dispose();
  }

  private void sendAuthTokenIfAny()
  {
    final String authToken = getReplicantContext().getAuthToken();
    if ( null != authToken )
    {
      _transport.updateAuthToken( authToken );
    }
  }

  private void sendEtagsIfAny()
  {
    final CacheService cacheService = getReplicantContext().getCacheService();
    if ( null != cacheService )
    {
      final HashMap<String, String> etags = new HashMap<>();
      final Set<ChannelAddress> addresses = cacheService.keySet( getSchema().getId() );
      for ( final ChannelAddress address : addresses )
      {
        final String eTag = cacheService.lookupEtag( address );
        assert null != eTag;
        etags.put( address.asChannelDescriptor(), eTag );
      }
      if ( !etags.isEmpty() )
      {
        _transport.updateEtagsSync( etags );
      }
    }
  }

  @Nonnull
  Connection ensureConnection()
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

  @Action
  void purgeSubscriptions()
  {
    final SubscriptionService subscriptionService = getReplicantContext().getSubscriptionService();
    Stream.concat( subscriptionService.getTypeSubscriptions().stream(),
                   subscriptionService.getInstanceSubscriptions().stream() )
      // Only purge subscriptions for current system
      .filter( s -> s.address().schemaId() == getSchema().getId() )
      // Purge in reverse order. First instance subscriptions then type subscriptions
      .sorted( Comparator.reverseOrder() )
      .forEachOrdered( Disposable::dispose );

    // Purge AreaOfInterest for current system
    getReplicantContext()
      .getAreaOfInterestService()
      .getAreasOfInterest()
      .stream()
      .filter( s -> s.getAddress().schemaId() == getSchema().getId() )
      .forEachOrdered( aoi -> updateAreaOfInterest( aoi.getAddress(), AreaOfInterest.Status.NOT_ASKED ) );
  }

  void setLinksToProcessPerTick( final int linksToProcessPerTick )
  {
    _linksToProcessPerTick = linksToProcessPerTick;
  }

  void setChangesToProcessPerTick( final int changesToProcessPerTick )
  {
    _changesToProcessPerTick = changesToProcessPerTick;
  }

  /**
   * Return true if an area of interest action with specified parameters is pending or being processed.
   * When the action parameter is DELETE the filter parameter is ignored.
   */
  boolean isAreaOfInterestRequestPending( @Nonnull final AreaOfInterestRequest.Type action,
                                          @Nonnull final ChannelAddress address,
                                          @Nullable final Object filter )
  {
    return null != _connection && _connection.isAreaOfInterestRequestPending( action, address, filter );
  }

  /**
   * Return the index of last matching Type in pending aoi actions list.
   */
  int lastIndexOfPendingAreaOfInterestRequest( @Nonnull final AreaOfInterestRequest.Type action,
                                               @Nonnull final ChannelAddress address,
                                               @Nullable final Object filter )
  {
    return null == _connection ? -1 : _connection.lastIndexOfPendingAreaOfInterestRequest( action, address, filter );
  }

  void requestSync()
  {
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new SyncRequestEvent( getSchema().getId() ) );
    }
    _transport.requestSync();
    tryTriggerMessageScheduler();
  }

  void requestExec( @Nonnull final String command,
                    @Nullable final Object payload,
                    @Nullable final ResponseHandler responseHandler )
  {
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext()
        .getSpy()
        .reportSpyEvent( new ExecRequestQueuedEvent( getSchema().getId(), getSchema().getName(), command ) );
    }
    ensureConnection().requestExec( command, payload, responseHandler );
    tryTriggerMessageScheduler();
  }

  void requestSubscribe( @Nonnull final ChannelAddress address, @Nullable final Object filter )
  {
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new SubscribeRequestQueuedEvent( address, filter ) );
    }
    validateFilterInstanceId( address );
    ensureConnection().requestSubscribe( address, filter );
    tryTriggerMessageScheduler();
  }

  void requestSubscriptionUpdate( @Nonnull final ChannelAddress address,
                                  @Nullable final Object filter )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> {
                   final ChannelSchema.FilterType filterType =
                     getSchema().getChannel( address.channelId() ).getFilterType();
                   return ChannelSchema.FilterType.DYNAMIC == filterType ||
                          ChannelSchema.FilterType.DYNAMIC_INSTANCED == filterType;
                 },
                 () -> "Replicant-0082: Connector.requestSubscriptionUpdate invoked for channel " + address +
                       " but channel does not have a dynamic filter." );
    }
    validateFilterInstanceId( address );
    ensureConnection().requestSubscriptionUpdate( address, filter );
    tryTriggerMessageScheduler();
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new SubscriptionUpdateRequestQueuedEvent( address, filter ) );
    }
  }

  void requestUnsubscribe( @Nonnull final ChannelAddress address )
  {
    validateFilterInstanceId( address );
    ensureConnection().requestUnsubscribe( address );
    tryTriggerMessageScheduler();
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new UnsubscribeRequestQueuedEvent( address ) );
    }
  }

  boolean isSchedulerActive()
  {
    return _schedulerActive;
  }

  private void tryTriggerMessageScheduler()
  {
    context().task( this::triggerMessageScheduler );
  }

  private void validateFilterInstanceId( @Nonnull final ChannelAddress address )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      final SystemSchema schema = getSchema();
      if ( schema.hasChannel( address.channelId() ) )
      {
        final ChannelSchema channel = schema.getChannel( address.channelId() );
        if ( ChannelSchema.FilterType.DYNAMIC_INSTANCED == channel.getFilterType() )
        {
          invariant( () -> null != address.filterInstanceId(),
                     () -> "Replicant-0098: Channel " + address +
                           " requires a filter instance id but none was supplied." );
        }
        else
        {
          invariant( () -> null == address.filterInstanceId(),
                     () -> "Replicant-0099: Channel " + address +
                           " does not support filter instance ids but one was supplied." );
        }
      }
    }
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

  boolean isSchedulerPaused()
  {
    return _schedulerPaused;
  }

  void pauseMessageScheduler()
  {
    _schedulerPaused = true;
  }

  void resumeMessageScheduler()
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
  boolean progressMessages()
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
      if ( null != _connection && ConnectorState.DISCONNECTING != _state )
      {
        final boolean step1 = progressAreaOfInterestRequestProcessing();
        final boolean step2 = progressExecRequestProcessing();
        final boolean step3 = progressResponseProcessing();
        _schedulerActive = step1 || step2 || step3;
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
    else if ( response.areEntityUpdateActionsPending() )
    {
      // Process all update actions. The presumption is that they do not do much
      processEntityUpdateActions();
      return true;
    }
    else if ( response.areOrphanSubscriptionsRemoved() )
    {
      // Remove all subscriptions that have been orphaned ... just in case we have some logic that triggers on
      // incoming change and queries the repository and accesses orphaned and potentially invalid entities.
      // This MUST be done prior to validateWorld()
      getReplicantContext().getConverger().removeOrphanSubscriptions();
      response.markOrphanSubscriptionsRemoved();
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

  @Memoize
  boolean isSynchronized()
  {
    return areRequestResponseQueuesEmpty() && ensureConnection().syncComplete();
  }

  boolean shouldRequestSync()
  {
    return areRequestResponseQueuesEmpty() && !ensureConnection().syncComplete();
  }

  private boolean areRequestResponseQueuesEmpty()
  {
    if ( ConnectorState.CONNECTED != getState() )
    {
      return false;
    }
    else
    {
      final Connection connection = ensureConnection();
      return connection.getRequests().isEmpty() &&
             connection.getPendingResponses().isEmpty();
    }
  }

  @Nonnull
  @Observable( readOutsideTransaction = Feature.ENABLE )
  ConnectorState getState()
  {
    return _state;
  }

  void setState( @Nonnull final ConnectorState state )
  {
    _state = Objects.requireNonNull( state );
    if ( ConnectorState.ERROR == _state ||
         ConnectorState.FATAL_ERROR == _state ||
         ConnectorState.DISCONNECTED == _state )
    {
      _transport.unbind();
    }
  }

  @Action
  void processChannelChanges()
  {
    final MessageResponse response = ensureCurrentMessageResponse();

    for ( final ChannelChangeDescriptor channelChange : response.getChannelChanges() )
    {
      final ChannelAddress address = channelChange.getAddress();
      final Object filter = channelChange.getFilter();
      final ChannelChangeDescriptor.Type actionType = channelChange.getType();

      if ( ChannelChangeDescriptor.Type.ADD == actionType )
      {
        response.incChannelAddCount();
        final boolean explicitSubscribe =
          getReplicantContext()
            .getAreasOfInterest()
            .stream()
            .anyMatch( a -> a.getAddress().equals( address ) );
        getReplicantContext().getSubscriptionService().createSubscription( address, filter, explicitSubscribe );
      }
      else if ( ChannelChangeDescriptor.Type.REMOVE == actionType || ChannelChangeDescriptor.Type.DELETE == actionType )
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
        }

        final AreaOfInterest areaOfInterest = getReplicantContext().findAreaOfInterestByAddress( address );
        if ( null != areaOfInterest )
        {
          if ( ChannelChangeDescriptor.Type.DELETE == actionType )
          {
            areaOfInterest.updateAreaOfInterest( AreaOfInterest.Status.DELETED, null );
          }
          else
          {
            // This means it has been deleted on the server side
            // We dispose it locally and assume that whatever component create AreaOfInterest can respond appropriately
            Disposable.dispose( areaOfInterest );
          }
        }
        response.incChannelRemoveCount();
      }
      else
      {
        assert ChannelChangeDescriptor.Type.UPDATE == actionType;
        final Subscription subscription = getReplicantContext().findSubscription( address );
        if ( Replicant.shouldCheckInvariants() )
        {
          invariant( () -> null != subscription,
                     () -> "Replicant-0033: Received ChannelChange of type UPDATE for address " + address +
                           " but no such subscription exists." );
          assert null != subscription;
          if ( Replicant.shouldCheckInvariants() )
          {
            invariant( () -> {
                         final ChannelSchema.FilterType filterType =
                           getSchema().getChannel( address.channelId() ).getFilterType();
                         return ChannelSchema.FilterType.DYNAMIC == filterType ||
                                ChannelSchema.FilterType.DYNAMIC_INSTANCED == filterType;
                       },
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
  void processEntityLinks()
  {
    final MessageResponse response = ensureCurrentMessageResponse();
    Linkable linkable;
    for ( int i = 0; i < _linksToProcessPerTick && null != ( linkable = response.nextEntityToLink() ); i++ )
    {
      linkable.link();
      response.incEntityLinkCount();
    }
  }

  @Action( verifyRequired = false )
  void processEntityUpdateActions()
  {
    final MessageResponse response = ensureCurrentMessageResponse();
    final OnEntityUpdateAction action = getSchema().getOnEntityUpdateAction();
    if ( null != action )
    {
      Object entity;
      while ( null != ( entity = response.nextEntityToPostAction() ) )
      {
        action.onEntityUpdate( getReplicantContext(), entity );
      }
    }
    else
    {
      response.completePostActions();
    }
  }

  /**
   * Method invoked when a filter has updated and the Connector needs to delink any entities
   * that are no longer part of the subscription now that the filter has changed.
   *
   * @param subscription the subscription that was updated.
   */
  @SuppressWarnings( "unchecked" )
  void updateSubscriptionForFilteredEntities( @Nonnull final Subscription subscription )
  {
    final ChannelAddress address = subscription.address();
    final ChannelSchema channel = getSchema().getChannel( address.channelId() );
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> ChannelSchema.FilterType.DYNAMIC == channel.getFilterType() ||
                       ChannelSchema.FilterType.DYNAMIC_INSTANCED == channel.getFilterType(),
                 () -> "Replicant-0079: Connector.updateSubscriptionForFilteredEntities invoked for address " +
                       subscription.address() + " but the channel does not have a DYNAMIC filter." );
    }

    final List<Entity> entitiesToDelink = new ArrayList<>();
    for ( final Class<?> entityType : new ArrayList<>( subscription.findAllEntityTypes() ) )
    {
      final List<Entity> entities = subscription.findAllEntitiesByType( entityType );
      if ( !entities.isEmpty() )
      {
        @SuppressWarnings( "rawtypes" )
        final SubscriptionUpdateEntityFilter updateFilter = channel.getFilter();
        assert null != updateFilter;
        final Object filter = subscription.getFilter();
        for ( final Entity entity : entities )
        {
          if ( !updateFilter.doesEntityMatchFilter( filter, entity ) )
          {
            // We need to collect all the entities into a separate list and delink later.
            // If we delink immediately and the arez userObject is disposed and a subsequent entity
            // calls `doesEntityMatchFilter` and tries to traverse across the already disposed
            // userObject then we get a crash or unexpected behaviour.
            // i.e. Moving days in planner will match the day first and remove it before attempting
            // to match RosterEntry but RosterEntry involves walking from RosterEntry->Day->Shift
            // which will crash
            entitiesToDelink.add( entity );
          }
        }
      }
    }

    for ( final Entity entity : entitiesToDelink )
    {
      entity.delinkFromSubscription( subscription );
    }
  }

  void setPostMessageResponseAction( @Nullable final SafeProcedure postMessageResponseAction )
  {
    _postMessageResponseAction = postMessageResponseAction;
  }

  void completeMessageResponse()
  {
    final Connection connection = ensureConnection();
    final MessageResponse response = connection.ensureCurrentMessageResponse();

    //Step: Run the post actions
    final RequestEntry request = response.getRequest();
    final ServerToClientMessage message = response.getMessage();
    final Integer requestId = message.getRequestId();

    final ExecRequest execRequest =
      null != requestId ? ensureConnection().getActiveExecRequest( requestId ) : null;
    if ( null != execRequest && null != request && message instanceof UpdateMessage )
    {
      @SuppressWarnings( "PatternVariableCanBeUsed" )
      final UpdateMessage updateMessage = (UpdateMessage) message;
      final ResponseHandler responseHandler = request.getResponseHandler();
      if ( null != responseHandler )
      {
        responseHandler.onResponse( updateMessage.getResponse() );
      }
    }

    // We can remove the request because this side ran second and the RPC channel has already returned.
    if ( null != requestId )
    {
      connection.removeRequest( requestId );
    }
    connection.setCurrentMessageResponse( null );
    if ( null != execRequest )
    {
      connection.markExecRequestAsComplete( requestId );
      onExecCompleted( execRequest.getCommand(), requestId );
    }
    onMessageProcessed( response );
    callPostMessageResponseActionIfPresent();

    if ( null != request )
    {
      final List<AreaOfInterestRequest> requests = connection.getActiveAreaOfInterestRequests();
      if ( !requests.isEmpty() )
      {
        if ( requests.get( 0 ).getRequestId() == request.getRequestId() )
        {
          completeAreaOfInterestRequests( requests );
        }
      }
    }
    //noinspection IfCanBeSwitch
    if ( OkMessage.TYPE.equals( message.getType() ) )
    {
      if ( null != requestId && connection.getLastRxSyncRequestId() == requestId )
      {
        if ( connection.syncComplete() )
        {
          onInSync();
          getReplicantContext().getConverger().removeOrphanSubscriptions();
        }
        else
        {
          onOutOfSync();
        }
        triggerMessageScheduler();
      }
    }
    else if ( UpdateMessage.TYPE.equals( message.getType() ) )
    {
      // If message is not a ping response then try to perform sync
      maybeRequestSync();
      final UpdateMessage updateMessage = (UpdateMessage) message;
      if ( null != updateMessage.getETag() )
      {
        cacheMessageIfPossible( response, updateMessage );
      }
    }
    else if ( ErrorMessage.TYPE.equals( message.getType() ) )
    {
      final ErrorMessage errorMessage = (ErrorMessage) message;
      final String m = errorMessage.getMessage();
      final String text = null == m ? "" : m;
      if ( text.startsWith( "java.lang.SecurityException:" ) )
      {
        fatalError();
      }
    }
  }

  @Action
  void fatalError()
  {
    setState( ConnectorState.FATAL_ERROR );
  }

  // This is in an action so that completeAreaOfInterestRequest() is called observers can react to status changes in AreaOfInterest
  @Action( reportParameters = false )
  void completeAreaOfInterestRequests( @Nonnull final List<AreaOfInterestRequest> requests )
  {
    requests.forEach( areaOfInterestRequest -> {
      final ChannelAddress address = areaOfInterestRequest.getAddress();
      final AreaOfInterestRequest.Type type = areaOfInterestRequest.getType();
      if ( AreaOfInterestRequest.Type.ADD == type )
      {
        onSubscribeCompleted( address );
      }
      else if ( AreaOfInterestRequest.Type.REMOVE == type )
      {
        removeExplicitSubscriptions( Collections.singletonList( areaOfInterestRequest ) );
        onUnsubscribeCompleted( address );
      }
      else
      {
        assert AreaOfInterestRequest.Type.UPDATE == type;
        onSubscriptionUpdateCompleted( address );
      }
    } );
    completeAreaOfInterestRequest();
  }

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
  void removeExplicitSubscriptions( @Nonnull final List<AreaOfInterestRequest> requests )
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
        subscription.setExplicitSubscription( false );
      }
    } );
  }

  @Action( verifyRequired = false )
  void removeUnneededUpdateRequests( @Nonnull final List<AreaOfInterestRequest> requests )
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
  void removeUnneededRemoveRequests( @Nonnull final List<AreaOfInterestRequest> requests )
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
        // We were getting here if we had a instant root deleted that sent DELETED to client which
        // explicitly unsubscribes which gets sent back a successful unsubscribe, even though it had already
        // been orphaned/deleted on client
        request.markAsComplete();
        return true;
      }
      else
      {
        return false;
      }
    } );
  }

  private void cacheMessageIfPossible( @Nonnull final MessageResponse response,
                                       @Nonnull final UpdateMessage changeSet )
  {
    final String eTag = changeSet.getETag();
    final CacheService cacheService = getReplicantContext().getCacheService();

    boolean candidate = false;
    if ( null != cacheService && null != eTag && ( changeSet.hasChannels() || changeSet.hasFilteredChannels() ) )
    {
      final List<ChannelChangeDescriptor> channelChanges = response.getChannelChanges();

      if ( 1 == channelChanges.size() &&
           ChannelChangeDescriptor.Type.ADD == channelChanges.get( 0 ).getType() &&
           getSchema().getChannel( channelChanges.get( 0 ).getAddress().channelId() ).isCacheable() )
      {
        final ChannelAddress address = channelChanges.get( 0 ).getAddress();
        cacheService.store( address, eTag, changeSet );
        candidate = true;
      }
    }
    if ( Replicant.shouldCheckApiInvariants() )
    {
      final boolean c = candidate;
      apiInvariant( () -> null == eTag || null == cacheService || c,
                    () -> "Replicant-0072: eTag in reply for ChangeSet but ChangeSet is not a candidate for caching." );
    }
  }

  @SuppressWarnings( "unchecked" )
  @Action
  void processEntityChanges()
  {
    final MessageResponse response = ensureCurrentMessageResponse();
    EntityChange change;
    for ( int i = 0; i < _changesToProcessPerTick && null != ( change = response.nextEntityChange() ); i++ )
    {
      final String id = change.getId();
      final int idSeparator = id.indexOf( "." );
      if ( idSeparator <= 0 || idSeparator >= id.length() - 1 )
      {
        onMessageProcessFailure( new IllegalArgumentException( "Invalid entity id format: '" + id + "'" ) );
        return;
      }
      final int typeId;
      final int entityId;
      try
      {
        typeId = Integer.parseInt( id.substring( 0, idSeparator ) );
        entityId = Integer.parseInt( id.substring( idSeparator + 1 ) );
      }
      catch ( final Throwable t )
      {
        onMessageProcessFailure( t );
        return;
      }
      final EntitySchema entitySchema = getSchema().getEntity( typeId );
      final Class<?> type = entitySchema.getType();
      Entity entity = getReplicantContext().getEntityService().findEntityByTypeAndId( type, entityId );
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
          final String name = Replicant.areNamesEnabled() ? entitySchema.getName() + "/" + entityId : null;
          entity = getReplicantContext().getEntityService().findOrCreateEntity( name, type, entityId );
          final Object userObject = entitySchema.getCreator().createEntity( entityId, data );
          entity.setUserObject( userObject );
        }
        else
        {
          @SuppressWarnings( "rawtypes" )
          final EntitySchema.Updater updater = entitySchema.getUpdater();
          if ( null != updater )
          {
            updater.updateEntity( entity.getUserObject(), data );
          }
        }

        final String[] channels = change.getChannels();
        final int schemaId = getSchema().getId();
        for ( final String channel : channels )
        {
          try
          {
            final ChannelAddress address = ChannelAddress.parse( schemaId, channel );
            final Subscription subscription = getReplicantContext().findSubscription( address );
            if ( Replicant.shouldCheckInvariants() )
            {
              invariant( () -> null != subscription,
                         () -> "Replicant-0069: UpdateMessage contained an EntityChange message referencing channel " +
                               address + " but no such subscription exists locally." );
            }
            if ( null != subscription )
            {
              entity.tryLinkToSubscription( subscription );
            }
            else
            {
              onOutOfSync();
              return;
            }
          }
          catch ( final Throwable t )
          {
            if ( t instanceof IllegalStateException )
            {
              throw (IllegalStateException) t;
            }
            onMessageProcessFailure( t );
            return;
          }
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

  void validateWorld()
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
  boolean progressAreaOfInterestRequestProcessing()
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

  boolean progressExecRequestProcessing()
  {
    final ExecRequest request = ensureConnection().nextExecRequest();
    if ( null == request )
    {
      return false;
    }
    else
    {
      final String command = request.getCommand();

      _transport.requestExec( command, request.getPayload(), request.getResponseHandler() );
      request.markAsInProgress( ensureConnection().getLastTxRequestId() );
      ensureConnection().recordActiveExecRequest( request );

      onExecStarted( command, request.getRequestId() );
      return true;
    }
  }

  void progressAreaOfInterestAddRequests( @Nonnull final List<AreaOfInterestRequest> requests )
  {
    // We very deliberately do not strip out requests even if there is a local subscription.
    // If the local subscription matched exactly the request would not make it to here and
    // if we are converting an implicit subscription to an explicit subscription then we need
    // to let it flow through to backend so that the backend knows that the subscription has
    // been upgraded to explicit.
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

  void progressAreaOfInterestAddRequest( @Nonnull final AreaOfInterestRequest request )
  {
    final ChannelAddress address = request.getAddress();
    onSubscribeStarted( address );

    _transport.requestSubscribe( request.getAddress(), request.getFilter() );
    request.markAsInProgress( ensureConnection().getLastTxRequestId() );
  }

  void progressBulkAreaOfInterestAddRequests( @Nonnull final List<AreaOfInterestRequest> requests )
  {
    final List<ChannelAddress> addresses =
      requests.stream().map( AreaOfInterestRequest::getAddress ).collect( Collectors.toList() );
    addresses.forEach( this::onSubscribeStarted );

    _transport.requestBulkSubscribe( addresses, requests.get( 0 ).getFilter() );
    final int requestId = ensureConnection().getLastTxRequestId();
    requests.forEach( r -> r.markAsInProgress( requestId ) );
  }

  void progressAreaOfInterestUpdateRequests( @Nonnull final List<AreaOfInterestRequest> requests )
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

  void progressAreaOfInterestUpdateRequest( @Nonnull final AreaOfInterestRequest request )
  {
    final ChannelAddress address = request.getAddress();
    onSubscriptionUpdateStarted( address );

    final Object filter = request.getFilter();
    assert null != filter;
    _transport.requestSubscribe( address, filter );
    final int requestId = ensureConnection().getLastTxRequestId();
    request.markAsInProgress( requestId );
  }

  void progressBulkAreaOfInterestUpdateRequests( @Nonnull final List<AreaOfInterestRequest> requests )
  {
    final List<ChannelAddress> addresses =
      requests.stream().map( AreaOfInterestRequest::getAddress ).collect( Collectors.toList() );
    addresses.forEach( this::onSubscriptionUpdateStarted );

    // All filters will be the same if they are grouped
    final Object filter = requests.get( 0 ).getFilter();
    assert null != filter;
    _transport.requestBulkSubscribe( addresses, filter );
    final int requestId = ensureConnection().getLastTxRequestId();
    requests.forEach( r -> r.markAsInProgress( requestId ) );
  }

  void progressAreaOfInterestRemoveRequests( @Nonnull final List<AreaOfInterestRequest> requests )
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

  void progressAreaOfInterestRemoveRequest( @Nonnull final AreaOfInterestRequest request )
  {
    final ChannelAddress address = request.getAddress();
    onUnsubscribeStarted( address );

    _transport.requestUnsubscribe( address );
    request.markAsInProgress( ensureConnection().getLastTxRequestId() );
  }

  void progressBulkAreaOfInterestRemoveRequests( @Nonnull final List<AreaOfInterestRequest> requests )
  {
    final List<ChannelAddress> addresses =
      requests.stream().map( AreaOfInterestRequest::getAddress ).collect( Collectors.toList() );
    addresses.forEach( this::onUnsubscribeStarted );

    _transport.requestBulkUnsubscribe( addresses );
    final int requestId = ensureConnection().getLastTxRequestId();
    requests.forEach( r -> r.markAsInProgress( requestId ) );
  }

  /**
   * The AreaOfInterestRequest currently being processed can be completed and
   * trigger scheduler to start next step.
   */
  void completeAreaOfInterestRequest()
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
  void onConnected()
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
  void onConnectFailure()
  {
    setState( ConnectorState.ERROR );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy()
        .reportSpyEvent( new ConnectFailureEvent( getSchema().getId(), getSchema().getName() ) );
    }
  }

  /**
   * Invoked to fire an event when disconnect has completed.
   */
  @Action
  void onDisconnected()
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
  void onDisconnectFailure()
  {
    setState( ConnectorState.ERROR );
    doSetConnection( null );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy()
        .reportSpyEvent( new DisconnectFailureEvent( getSchema().getId(), getSchema().getName() ) );
    }
  }

  /**
   * Invoked when a transport received a message.
   *
   * @param message the message.
   */
  void onMessageReceived( @Nonnull final ServerToClientMessage message )
  {
    final Connection connection = ensureConnection();

    final Integer requestId = message.getRequestId();
    final RequestEntry request = null != requestId ? connection.getRequest( requestId ) : null;

    final ServerToClientMessage messageToQueue;
    if ( UseCacheMessage.TYPE.equals( message.getType() ) )
    {
      final UseCacheMessage useCacheMessage = (UseCacheMessage) message;
      final String channel = useCacheMessage.getChannel();
      final ChannelAddress address;
      try
      {
        address = ChannelAddress.parse( getSchema().getId(), channel );
      }
      catch ( final Throwable t )
      {
        onMessageProcessFailure( t );
        return;
      }
      final String etag = useCacheMessage.getEtag();

      final CacheService cacheService = getReplicantContext().getCacheService();
      if ( null == cacheService )
      {
        ReplicantLogger.log( "Received a use-cache message for channel " + address +
                             " but no cache service configured.", null );
        onMessageReadFailure();
        return;
      }

      final CacheEntry entry = cacheService.lookup( address );
      if ( null == entry )
      {
        ReplicantLogger.log( "Received a use-cache message for channel " + channel +
                             " but no cache entry present for channel.", null );
        onMessageReadFailure();
        return;
      }
      if ( !Objects.equals( entry.getETag(), etag ) )
      {
        ReplicantLogger.log( "Received a use-cache message for channel " + channel +
                             " with etag '" + etag + "' but cache entry has etag '" + entry.getETag() + "'.",
                             null );
        onMessageReadFailure();
        return;
      }
      try
      {
        messageToQueue = Objects.requireNonNull( JSON.parse( entry.getContent() ) ).cast();
      }
      catch ( final Throwable t )
      {
        onMessageProcessFailure( t );
        return;
      }
      messageToQueue.setRequestId( requestId );
    }
    else
    {
      messageToQueue = message;
    }

    connection.enqueueResponse( messageToQueue, request );
    triggerMessageScheduler();
  }

  /**
   * Invoked when a change set has been completely processed.
   *
   * @param response the message response.
   */
  void onMessageProcessed( @Nonnull final MessageResponse response )
  {
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy()
        .reportSpyEvent( new MessageProcessedEvent( getSchema().getId(), getSchema().getName(), response.toStatus() ) );
    }
  }

  /**
   * Invoked when an exec has been sent to the server.
   *
   * @param command the exec request command.
   */
  void onExecStarted( @Nonnull final String command, final int requestId )
  {
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy()
        .reportSpyEvent( new ExecStartedEvent( getSchema().getId(), getSchema().getName(), command, requestId ) );
    }
  }

  /**
   * Invoked when an exec has been sent to the server.
   *
   * @param command the exec request command.
   */
  void onExecCompleted( @Nonnull final String command, final int requestId )
  {
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy()
        .reportSpyEvent( new ExecCompletedEvent( getSchema().getId(), getSchema().getName(), command, requestId ) );
    }
  }

  /**
   * Called when a data load has resulted in a failure.
   */
  @Action( verifyRequired = false )
  void onMessageProcessFailure( @Nonnull final Throwable error )
  {
    final String message = ReplicantUtil.safeGetString( () -> "Exception processing replicant message." );
    ReplicantLogger.log( message, error );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy()
        .reportSpyEvent( new MessageProcessFailureEvent( getSchema().getId(), getSchema().getName(), error ) );
    }
    disconnectIfPossible();
  }

  /**
   * Attempted to retrieve data from backend and failed.
   */
  @Action( verifyRequired = false )
  void onMessageReadFailure()
  {
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy()
        .reportSpyEvent( new MessageReadFailureEvent( getSchema().getId(), getSchema().getName() ) );
    }
    disconnectIfPossible();
  }

  void disconnectIfPossible()
  {
    if ( !ConnectorState.isTransitionState( getState() ) )
    {
      if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
      {
        getReplicantContext().getSpy()
          .reportSpyEvent( new RestartEvent( getSchema().getId(), getSchema().getName() ) );
      }
      disconnect();
    }
  }

  void onInSync()
  {
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new InSyncEvent( getSchema().getId() ) );
    }
  }

  void onOutOfSync()
  {
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new OutOfSyncEvent( getSchema().getId() ) );
    }
  }

  @Action
  void onSubscribeStarted( @Nonnull final ChannelAddress address )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.LOADING );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy()
        .reportSpyEvent( new SubscribeStartedEvent( getSchema().getId(), getSchema().getName(), address ) );
    }
  }

  @Action
  void onSubscribeCompleted( @Nonnull final ChannelAddress address )
  {
    final AreaOfInterest areaOfInterest = getReplicantContext().findAreaOfInterestByAddress( address );
    if ( null != areaOfInterest && AreaOfInterest.Status.DELETED != areaOfInterest.getStatus() )
    {
      areaOfInterest.updateAreaOfInterest( AreaOfInterest.Status.LOADED, null );
    }
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy()
        .reportSpyEvent( new SubscribeCompletedEvent( getSchema().getId(), getSchema().getName(), address ) );
    }
  }

  @Action
  void onUnsubscribeStarted( @Nonnull final ChannelAddress address )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.UNLOADING );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy()
        .reportSpyEvent( new UnsubscribeStartedEvent( getSchema().getId(), getSchema().getName(), address ) );
    }
  }

  @Action
  void onUnsubscribeCompleted( @Nonnull final ChannelAddress address )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.UNLOADED );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy()
        .reportSpyEvent( new UnsubscribeCompletedEvent( getSchema().getId(), getSchema().getName(), address ) );
    }
  }

  @Action
  void onSubscriptionUpdateStarted( @Nonnull final ChannelAddress address )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.UPDATING );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy()
        .reportSpyEvent( new SubscriptionUpdateStartedEvent( getSchema().getId(), getSchema().getName(), address ) );
    }
  }

  @Action
  void onSubscriptionUpdateCompleted( @Nonnull final ChannelAddress address )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.UPDATED );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy()
        .reportSpyEvent( new SubscriptionUpdateCompletedEvent( getSchema().getId(), getSchema().getName(), address ) );
    }
  }

  private void updateAreaOfInterest( @Nonnull final ChannelAddress address,
                                     @Nonnull final AreaOfInterest.Status status )
  {
    final AreaOfInterest areaOfInterest = getReplicantContext().findAreaOfInterestByAddress( address );
    if ( null != areaOfInterest )
    {
      areaOfInterest.updateAreaOfInterest( status, null );
    }
  }

  @Nonnull
  ReplicantRuntime getReplicantRuntime()
  {
    return getReplicantContext().getRuntime();
  }

  @ContextRef
  @Nonnull
  abstract ArezContext context();

  @Override
  public String toString()
  {
    return Replicant.areNamesEnabled() ? "Connector[" + getSchema().getName() + "]" : super.toString();
  }

  @Nullable
  Disposable getSchedulerLock()
  {
    return _schedulerLock;
  }

  @Nullable
  SafeProcedure getPostMessageResponseAction()
  {
    return _postMessageResponseAction;
  }

  @Nullable
  Connection getConnection()
  {
    return _connection;
  }

  @Nonnull
  Transport getTransport()
  {
    return _transport;
  }
}
