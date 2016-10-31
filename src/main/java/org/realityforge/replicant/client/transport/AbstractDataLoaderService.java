package org.realityforge.replicant.client.transport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.Change;
import org.realityforge.replicant.client.ChangeMapper;
import org.realityforge.replicant.client.ChangeSet;
import org.realityforge.replicant.client.ChannelAction;
import org.realityforge.replicant.client.ChannelDescriptor;
import org.realityforge.replicant.client.ChannelSubscriptionEntry;
import org.realityforge.replicant.client.EntityChangeBroker;
import org.realityforge.replicant.client.EntityRepository;
import org.realityforge.replicant.client.EntityRepositoryDebugger;
import org.realityforge.replicant.client.EntityRepositoryValidator;
import org.realityforge.replicant.client.EntitySubscriptionEntry;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.client.Linkable;
import org.realityforge.replicant.client.transport.AreaOfInterestAction.Action;

/**
 * Class from which to extend to implement a service that loads data from a change set.
 * Data can be loaded by bulk or incrementally and the load can be broken up into several
 * steps to avoid locking a thread such as in GWT.
 */
@SuppressWarnings( { "WeakerAccess", "unused" } )
public abstract class AbstractDataLoaderService<T extends ClientSession<T, G>, G extends Enum>
{
  protected static final Logger LOG = Logger.getLogger( AbstractDataLoaderService.class.getName() );

  private static final int DEFAULT_CHANGES_TO_PROCESS_PER_TICK = 100;
  private static final int DEFAULT_LINKS_TO_PROCESS_PER_TICK = 100;
  private final ChangeMapper _changeMapper;
  private final EntityChangeBroker _changeBroker;
  private final EntityRepository _repository;
  private final CacheService _cacheService;
  private final EntitySubscriptionManager _subscriptionManager;
  private final SessionContext _sessionContext;

  private DataLoadAction _currentAction;
  private AreaOfInterestAction<G> _currentAoiAction;
  private int _changesToProcessPerTick = DEFAULT_CHANGES_TO_PROCESS_PER_TICK;
  private int _linksToProcessPerTick = DEFAULT_LINKS_TO_PROCESS_PER_TICK;
  private boolean _incrementalDataLoadInProgress;

  private T _session;

  protected AbstractDataLoaderService( @Nonnull final SessionContext sessionContext,
                                       @Nonnull final EntityChangeBroker changeBroker,
                                       @Nonnull final EntityRepository repository,
                                       @Nonnull final CacheService cacheService,
                                       @Nonnull final EntitySubscriptionManager subscriptionManager )
  {
    _sessionContext = sessionContext;
    _changeBroker = changeBroker;
    _repository = repository;
    _cacheService = cacheService;
    _subscriptionManager = subscriptionManager;
    _changeMapper = newChangeMapper();
  }

  @Nonnull
  protected abstract ChangeMapper newChangeMapper();

  protected SessionContext getSessionContext()
  {
    return _sessionContext;
  }

  /**
   * Action invoked after current action completes to reset session state.
   */
  private Runnable _resetAction;

  protected boolean shouldPurgeOnSessionChange()
  {
    return true;
  }

  protected void setSession( @Nullable final T session, @Nullable final Runnable postAction )
  {
    final Runnable runnable = new Runnable()
    {
      @Override
      public void run()
      {
        doSetSession( session, postAction );
      }
    };
    if ( null == _currentAction )
    {
      runnable.run();
    }
    else
    {
      _resetAction = runnable;
    }
  }

  private void doSetSession( final T session, final Runnable postAction )
  {
    if ( session != _session )
    {
      _session = session;
      // This should probably be moved elsewhere ... but where?
      _sessionContext.setSession( session );
      if ( shouldPurgeOnSessionChange() )
      {
        final boolean enabled = _changeBroker.isEnabled();
        if ( enabled )
        {
          _changeBroker.disable( getSystemKey() );
        }
        //TODO: else schedule action so that it runs in loop
        // until it can disable broker. This will involve replacing _resetAction
        // with something more like existing action setup.
        purgeSubscriptions();
        if ( enabled )
        {
          _changeBroker.enable( getSystemKey() );
        }
      }
    }
    if ( null != postAction )
    {
      postAction.run();
    }
  }

  @Nonnull
  protected CacheService getCacheService()
  {
    return _cacheService;
  }

  @Nonnull
  protected EntityChangeBroker getChangeBroker()
  {
    return _changeBroker;
  }

  @Nonnull
  protected ChangeMapper getChangeMapper()
  {
    return _changeMapper;
  }

  @Nonnull
  protected EntitySubscriptionManager getSubscriptionManager()
  {
    return _subscriptionManager;
  }

  @Nullable
  public T getSession()
  {
    return _session;
  }

  @Nonnull
  protected abstract T ensureSession();

  /**
   * Return the id of the session associated with the service.
   *
   * @return the id of the session associated with the service.
   * @throws RuntimeException if the service is not currently associated with the session.
   */
  protected String getSessionID()
  {
    return ensureSession().getSessionID();
  }

  protected void purgeSubscriptions()
  {
    /*
     * Ensure that we only purge subscriptions that are managed by this data loader.
     */
    final Class<G> graphClass = getGraphType();
    final EntitySubscriptionManager subscriptionManager = getSubscriptionManager();
    for ( final Enum graph : sortGraphs( subscriptionManager.getInstanceSubscriptionKeys() ) )
    {
      if ( graph.getClass().equals( graphClass ) )
      {
        unsubscribeInstanceGraphs( graph );
      }
    }
    for ( final Enum graph : sortGraphs( subscriptionManager.getTypeSubscriptions() ) )
    {
      if ( graph.getClass().equals( graphClass ) )
      {
        final ChannelSubscriptionEntry entry = subscriptionManager.unsubscribe( graph );
        deregisterUnOwnedEntities( entry );
      }
    }
  }

  protected void unsubscribeInstanceGraphs( final Enum graph )
  {
    final EntitySubscriptionManager subscriptionManager = getSubscriptionManager();
    for ( final Object id : new ArrayList<>( subscriptionManager.getInstanceSubscriptions( graph ) ) )
    {
      final ChannelSubscriptionEntry entry = subscriptionManager.unsubscribe( graph, id );
      deregisterUnOwnedEntities( entry );
    }
  }

  private ArrayList<Enum> sortGraphs( final Set<Enum> enums )
  {
    final ArrayList<Enum> list = new ArrayList<>( enums );
    Collections.sort( list );
    Collections.reverse( list );
    return list;
  }

  /**
   * Ugly hack, should split into two (schedule subscribe, schedule data)
   */
  /**
   * Schedule data loads using incremental scheduler.
   */
  protected void scheduleDataLoad()
  {
    if ( !_incrementalDataLoadInProgress )
    {
      _incrementalDataLoadInProgress = true;

      doScheduleDataLoad();
    }
  }

  /**
   * Perform a single step in incremental data load process.
   *
   * @return true if more work is to be done.
   */
  protected boolean stepDataLoad()
  {
    try
    {
      final boolean aoiActionProgressed = progressAreaOfInterestActions();
      final boolean dataActionProgressed = progressDataLoad();
      _incrementalDataLoadInProgress = aoiActionProgressed || dataActionProgressed;
    }
    catch ( final Exception e )
    {
      progressDataLoadFailure( e );
      _incrementalDataLoadInProgress = false;
      return false;
    }
    return _incrementalDataLoadInProgress;
  }

  /**
   * Actually perform the scheduling of the data load action.
   */
  protected abstract void doScheduleDataLoad();

  protected abstract void progressDataLoadFailure( @Nonnull Exception e );

  protected void setChangesToProcessPerTick( final int changesToProcessPerTick )
  {
    _changesToProcessPerTick = changesToProcessPerTick;
  }

  protected void setLinksToProcessPerTick( final int linksToProcessPerTick )
  {
    _linksToProcessPerTick = linksToProcessPerTick;
  }

  protected abstract ChangeSet parseChangeSet( String rawJsonData );

  protected boolean progressAreaOfInterestActions()
  {
    if ( null == _currentAoiAction )
    {
      final LinkedList<AreaOfInterestAction<G>> actions = ensureSession().getPendingAreaOfInterestActions();
      if ( 0 == actions.size() )
      {
        return false;
      }
      _currentAoiAction = actions.removeFirst();
    }
    if ( _currentAoiAction.isInProgress() )
    {
      return false;
    }
    else
    {
      _currentAoiAction.markAsInProgress();
      final G graph = _currentAoiAction.getGraph();
      final Object id = _currentAoiAction.getId();
      final Object filterParameter = _currentAoiAction.getFilterParameter();
      final String label =
        graph.name() +
        ( null == id ? "" : "(" + id + ")" ) +
        ( null == filterParameter ? "" : "[" + filterParameter + "]" );
      final Runnable userAction = _currentAoiAction.getUserAction();
      final String cacheKey = _currentAoiAction.getCacheKey();
      final Action action = _currentAoiAction.getAction();
      if ( action == Action.ADD )
      {
        final ChannelSubscriptionEntry entry = findSubscription( graph, id );
        //Already subscribed
        if ( null != entry )
        {
          LOG.warning( "Subscription to " + label + " requested but already subscribed." );
          completeAoiAction( userAction );
          return true;
        }
        final Runnable cacheAction;
        final String eTag;
        if ( null != cacheKey )
        {
          final CacheEntry cacheEntry = _cacheService.lookup( cacheKey );
          eTag = null != cacheEntry ? cacheEntry.getETag() : null;
          final String content = null != cacheEntry ? cacheEntry.getContent() : null;
          if ( null != content )
          {
            final String message =
              "Found locally cached data for graph " + label + " with etag " + eTag + ".";
            LOG.info( message );
            cacheAction = new Runnable()
            {
              public void run()
              {
                LOG.info( "Loading cached data for graph " + label + " with etag " + eTag );
                //TODO: Figure out how to make the bulkLoad configurable
                final Runnable runnable = new Runnable()
                {
                  @Override
                  public void run()
                  {
                    completeAoiAction( userAction );
                  }
                };
                ensureSession().enqueueOOB( content, runnable, true );
              }
            };
          }
          else
          {
            cacheAction = null;
          }
        }
        else
        {
          eTag = null;
          cacheAction = null;
        }
        final Runnable runnable = new Runnable()
        {
          @Override
          public void run()
          {
            LOG.info( "Subscription to " + label + " completed." );
            completeAoiAction( userAction );
          }
        };
        LOG.info( "Subscription to " + label + " requested." );
        requestSubscribeToGraph( graph, id, filterParameter, eTag, cacheAction, runnable );
        return true;
      }
      else if ( action == Action.REMOVE )
      {
        final ChannelSubscriptionEntry entry = findSubscription( graph, id );
        //Not subscribed
        if ( null == entry )
        {
          LOG.warning( "Unsubscribe from " + label + " requested but not subscribed." );
          completeAoiAction( userAction );
          return true;
        }

        LOG.info( "Unsubscribe from " + label + " requested." );
        final Runnable runnable = new Runnable()
        {
          @Override
          public void run()
          {
            LOG.info( "Unsubscribe from " + label + " completed." );
            completeAoiAction( userAction );
          }
        };
        requestUnsubscribeFromGraph( graph, id, runnable );
        return true;
      }
      else
      {
        final ChannelSubscriptionEntry entry = findSubscription( graph, id );
        //Not subscribed
        if ( null == entry )
        {
          LOG.warning( "Subscription update of " + label + " requested but not subscribed." );
          completeAoiAction( userAction );
          return true;
        }

        final Runnable runnable = new Runnable()
        {
          @Override
          public void run()
          {
            LOG.warning( "Subscription update of " + label + " completed." );
            completeAoiAction( userAction );
          }
        };
        LOG.warning( "Subscription update of " + label + " requested." );
        assert null != filterParameter;
        requestUpdateSubscription( graph, id, filterParameter, runnable );
        return true;
      }
    }
  }

  private ChannelSubscriptionEntry findSubscription( final G graph, final Object id )
  {
    final ChannelSubscriptionEntry entry;
    if ( null == id )
    {
      entry = _subscriptionManager.findSubscription( graph );
    }
    else
    {
      entry = _subscriptionManager.findSubscription( graph, id );
    }
    return entry;
  }

  private void completeAoiAction( final Runnable userAction )
  {
    scheduleDataLoad();
    if ( null != userAction )
    {
      userAction.run();
    }
    _currentAoiAction.markAsComplete();
    _currentAoiAction = null;
  }

  protected abstract void requestSubscribeToGraph( @Nonnull G graph,
                                                   @Nullable Object id,
                                                   @Nullable Object filterParameter,
                                                   @Nullable String eTag,
                                                   @Nullable Runnable cacheAction,
                                                   @Nonnull Runnable completionAction );

  protected abstract void requestUnsubscribeFromGraph( @Nonnull G graph,
                                                       @Nullable Object id,
                                                       @Nonnull Runnable completionAction );

  protected abstract void requestUpdateSubscription( @Nonnull G graph,
                                                     @Nullable Object id,
                                                     @Nonnull Object filterParameter,
                                                     @Nonnull Runnable completionAction );

  protected boolean isSubscribed( @Nonnull final G graph, @Nonnull final Object id )
  {
    return null != _subscriptionManager.findSubscription( graph, id );
  }

  protected boolean isSubscribed( @Nonnull final G graph )
  {
    return null != _subscriptionManager.findSubscription( graph );
  }

  DataLoadAction getCurrentAction()
  {
    return _currentAction;
  }

  protected boolean progressDataLoad()
  {
    final T session = ensureSession();
    // Step: Retrieve any out of band actions
    final LinkedList<DataLoadAction> oobActions = session.getOobActions();
    if ( null == _currentAction && !oobActions.isEmpty() )
    {
      _currentAction = oobActions.removeFirst();
      return true;
    }

    //Step: Retrieve the action from the parsed queue if it is the next in the sequence
    final LinkedList<DataLoadAction> parsedActions = session.getParsedActions();
    if ( null == _currentAction && !parsedActions.isEmpty() )
    {
      final DataLoadAction action = parsedActions.get( 0 );
      final ChangeSet changeSet = action.getChangeSet();
      assert null != changeSet;
      if ( action.isOob() || session.getLastRxSequence() + 1 == changeSet.getSequence() )
      {
        _currentAction = parsedActions.remove();
        if ( LOG.isLoggable( getLogLevel() ) )
        {
          LOG.log( getLogLevel(), "Parsed Action Selected: " + _currentAction );
        }
        return true;
      }
    }

    // Abort if there is no pending data load actions to take
    final LinkedList<DataLoadAction> pendingActions = session.getPendingActions();
    if ( null == _currentAction && pendingActions.isEmpty() )
    {
      if ( LOG.isLoggable( getLogLevel() ) )
      {
        LOG.log( getLogLevel(), "No data to load. Terminating incremental load process." );
      }
      onTerminatingIncrementalDataLoadProcess();
      return false;
    }

    //Step: Retrieve the action from the un-parsed queue
    if ( null == _currentAction )
    {
      _currentAction = pendingActions.remove();
      if ( LOG.isLoggable( getLogLevel() ) )
      {
        LOG.log( getLogLevel(), "Un-parsed Action Selected: " + _currentAction );
      }
      return true;
    }

    //Step: Parse the json
    final String rawJsonData = _currentAction.getRawJsonData();
    if ( null != rawJsonData )
    {
      if ( LOG.isLoggable( getLogLevel() ) )
      {
        LOG.log( getLogLevel(), "Parsing JSON: " + _currentAction );
      }
      final ChangeSet changeSet = parseChangeSet( _currentAction.getRawJsonData() );
      // OOB messages are not in response to requests as such
      final String requestID = _currentAction.isOob() ? null : changeSet.getRequestID();
      // OOB messages have no etags as from local cache or generated locally
      final String eTag = _currentAction.isOob() ? null : changeSet.getETag();
      final int sequence = _currentAction.isOob() ? 0 : changeSet.getSequence();
      if ( LOG.isLoggable( getLogLevel() ) )
      {
        LOG.log( getLogLevel(),
                 "Parsed ChangeSet:" +
                 " oob=" + _currentAction.isOob() +
                 " seq=" + sequence +
                 " requestID=" + requestID +
                 " eTag=" + eTag +
                 " changeCount=" + changeSet.getChangeCount()
        );
      }
      final RequestEntry request;
      if ( _currentAction.isOob() )
      {
        request = null;
      }
      else
      {
        request = null != requestID ? session.getRequest( requestID ) : null;
        if ( null == request && null != requestID )
        {
          final String message =
            "Unable to locate requestID '" + requestID + "' specified for ChangeSet: seq=" + sequence +
            " Existing Requests: " + session.getRequests();
          if ( LOG.isLoggable( Level.WARNING ) )
          {
            LOG.warning( message );
          }
          throw new IllegalStateException( message );
        }
        else if ( null != request )
        {
          final String cacheKey = request.getCacheKey();
          if ( null != eTag && null != cacheKey )
          {
            if ( LOG.isLoggable( getLogLevel() ) )
            {
              LOG.log( getLogLevel(), "Caching ChangeSet: seq=" + sequence + " cacheKey=" + cacheKey );
            }
            getCacheService().store( cacheKey, eTag, rawJsonData );
          }
        }
      }

      _currentAction.setChangeSet( changeSet, request );
      parsedActions.add( _currentAction );
      Collections.sort( parsedActions );
      _currentAction = null;
      return true;
    }

    //Step: Setup the change recording state
    if ( _currentAction.needsBrokerPause() )
    {
      if ( getChangeBroker().isInTransaction() )
      {
        // Another DataLoaderService has temporarily paused/disabled the broker. So we will
        // just spin waiting for it to be released.
        return true;
      }
      _currentAction.markBrokerPaused();
      if ( _currentAction.isBulkLoad() )
      {
        getChangeBroker().disable( getSystemKey() );
      }
      else
      {
        getChangeBroker().pause( getSystemKey() );
      }
      return true;
    }

    if ( _currentAction.needsChannelActionsProcessed() )
    {
      _currentAction.markChannelActionsProcessed();
      final ChangeSet changeSet = _currentAction.getChangeSet();
      assert null != changeSet;
      final int channelActionCount = changeSet.getChannelActionCount();
      for ( int i = 0; i < channelActionCount; i++ )
      {
        final ChannelAction action = changeSet.getChannelAction( i );
        final int channel = action.getChannelID();
        final Object subChannelID = action.getSubChannelID();
        final Object filter = action.getChannelFilter();
        final ChannelAction.Action actionType = action.getAction();
        if ( LOG.isLoggable( getLogLevel() ) )
        {
          final String message =
            "ChannelAction:: " + actionType.name() + " " +
            channel + ( null == subChannelID ? "" : ( "-" + subChannelID ) ) +
            " filter=" + filter;
          LOG.log( getLogLevel(), message );
        }

        final G graph = channelToGraph( channel );
        final ChannelDescriptor descriptor = new ChannelDescriptor( graph, subChannelID );
        if ( ChannelAction.Action.ADD == actionType )
        {
          _currentAction.recordChannelSubscribe( new ChannelChangeStatus( descriptor, filter, 0 ) );
          if ( null == subChannelID )
          {
            _subscriptionManager.subscribe( graph, filter );
          }
          else
          {
            _subscriptionManager.subscribe( graph, subChannelID, filter );
          }
        }
        else if ( ChannelAction.Action.REMOVE == actionType )
        {
          final ChannelSubscriptionEntry entry;
          if ( null == subChannelID )
          {
            entry = _subscriptionManager.unsubscribe( graph );
          }
          else
          {
            entry = _subscriptionManager.unsubscribe( graph, subChannelID );
          }
          final int removedEntities = deregisterUnOwnedEntities( entry );
          _currentAction.recordChannelUnsubscribe( new ChannelChangeStatus( descriptor, filter, removedEntities ) );
        }
        else if ( ChannelAction.Action.UPDATE == actionType )
        {
          final ChannelSubscriptionEntry entry;
          if ( null == subChannelID )
          {
            entry = _subscriptionManager.updateSubscription( graph, filter );
          }
          else
          {
            entry = _subscriptionManager.updateSubscription( graph, subChannelID, filter );
          }
          final int removedEntities = updateSubscriptionForFilteredEntities( entry, filter );
          final ChannelChangeStatus status = new ChannelChangeStatus( descriptor, filter, removedEntities );
          _currentAction.recordChannelSubscriptionUpdate( status );
        }
        else
        {
          throw new IllegalStateException();
        }
      }
      return true;
    }

    //Step: Process a chunk of changes
    if ( _currentAction.areChangesPending() )
    {
      if ( LOG.isLoggable( getLogLevel() ) )
      {
        LOG.log( getLogLevel(), "Processing ChangeSet: " + _currentAction );
      }
      Change change;
      for ( int i = 0; i < _changesToProcessPerTick && null != ( change = _currentAction.nextChange() ); i++ )
      {
        final Object entity = getChangeMapper().applyChange( change );
        if ( LOG.isLoggable( Level.INFO ) )
        {
          if ( change.isUpdate() )
          {
            _currentAction.incUpdateCount();
          }
          else
          {
            _currentAction.incRemoveCount();
          }
        }
        _currentAction.changeProcessed( change.isUpdate(), entity );
      }
      return true;
    }

    //Step: Calculate the entities that need to be linked
    if ( !_currentAction.areEntityLinksCalculated() )
    {
      if ( LOG.isLoggable( getLogLevel() ) )
      {
        LOG.log( getLogLevel(), "Calculating Link list: " + _currentAction );
      }
      _currentAction.calculateEntitiesToLink();
      return true;
    }

    //Step: Process a chunk of links
    if ( _currentAction.areEntityLinksPending() )
    {
      if ( LOG.isLoggable( getLogLevel() ) )
      {
        LOG.log( getLogLevel(), "Linking Entities: " + _currentAction );
      }
      Linkable linkable;
      for ( int i = 0; i < _linksToProcessPerTick && null != ( linkable = _currentAction.nextEntityToLink() ); i++ )
      {
        linkable.link();
        if ( LOG.isLoggable( Level.INFO ) )
        {
          _currentAction.incLinkCount();
        }
      }
      return true;
    }

    final ChangeSet set = _currentAction.getChangeSet();
    assert null != set;

    //Step: Finalize the change set
    if ( !_currentAction.hasWorldBeenNotified() )
    {
      _currentAction.markWorldAsNotified();
      if ( LOG.isLoggable( getLogLevel() ) )
      {
        LOG.log( getLogLevel(), "Finalizing action: " + _currentAction );
      }
      // OOB messages are not sequenced
      if ( !_currentAction.isOob() )
      {
        session.setLastRxSequence( set.getSequence() );
      }
      if ( _currentAction.isBulkLoad() )
      {
        if ( _currentAction.hasBrokerBeenPaused() )
        {
          getChangeBroker().enable( getSystemKey() );
        }
      }
      else
      {
        if ( _currentAction.hasBrokerBeenPaused() )
        {
          getChangeBroker().resume( getSystemKey() );
        }
      }
      if ( repositoryDebugOutputEnabled() )
      {
        outputRepositoryDebug();
      }
      if ( subscriptionsDebugOutputEnabled() )
      {
        outputSubscriptionDebug();
      }
      if ( shouldValidateOnLoad() )
      {
        validateRepository();
      }
      return true;
    }
    final DataLoadStatus status = _currentAction.toStatus( getSystemKey() );
    if ( LOG.isLoggable( Level.INFO ) )
    {
      LOG.info( status.getSystemKey() + ": ChangeSet " + set.getSequence() + " involved " +
                status.getChannelAdds().size() + " subscribes, " +
                status.getChannelUpdates().size() + " subscription updates, " +
                status.getChannelRemoves().size() + " un-subscribes, " +
                status.getEntityUpdateCount() + " updates, " +
                status.getEntityRemoveCount() + " removes and " +
                status.getEntityLinkCount() + " links." );
      for ( final ChannelChangeStatus changeStatus : status.getChannelUpdates() )
      {
        LOG.info( status.getSystemKey() + ": ChangeSet " + set.getSequence() + " subscription update " +
                  changeStatus.getDescriptor() + " caused " +
                  changeStatus.getEntityRemoveCount() + " entity removes." );
      }
      for ( final ChannelChangeStatus changeStatus : status.getChannelRemoves() )
      {
        LOG.info( status.getSystemKey() + ": ChangeSet " + set.getSequence() + " un-subscribe " +
                  changeStatus.getDescriptor() + " caused " +
                  changeStatus.getEntityRemoveCount() + " entity removes." );
      }
    }

    //Step: Run the post actions
    if ( LOG.isLoggable( getLogLevel() ) )
    {
      LOG.log( getLogLevel(), "Running post action and cleaning action: " + _currentAction );
    }
    final RequestEntry request = _currentAction.getRequest();
    if ( null != request )
    {
      request.markResultsAsArrived();
    }
    final Runnable runnable = _currentAction.getRunnable();
    if ( null != runnable )
    {
      runnable.run();
      // OOB messages are not in response to requests as such
      final String requestID = _currentAction.isOob() ? null : _currentAction.getChangeSet().getRequestID();
      if ( null != requestID )
      {
        // We can remove the request because this side ran second and the
        // RPC channel has already returned.

        final boolean removed = session.removeRequest( requestID );
        if ( !removed )
        {
          LOG.severe( "ChangeSet " + set.getSequence() + " expected to complete request '" +
                      requestID + "' but no request was registered with session." );
        }
        if ( requestDebugOutputEnabled() )
        {
          outputRequestDebug();
        }
      }
    }
    onDataLoadComplete( status );
    _currentAction = null;
    if ( null != _resetAction )
    {
      _resetAction.run();
      _resetAction = null;
    }
    return true;
  }

  private int deregisterUnOwnedEntities( @Nonnull final ChannelSubscriptionEntry entry )
  {
    int removedEntities = 0;
    for ( final Entry<Class<?>, Map<Object, EntitySubscriptionEntry>> entitySet : entry.getEntities().entrySet() )
    {
      final Class<?> type = entitySet.getKey();
      for ( final Entry<Object, EntitySubscriptionEntry> entityEntry : entitySet.getValue().entrySet() )
      {
        final Object entityID = entityEntry.getKey();
        final EntitySubscriptionEntry entitySubscription = entityEntry.getValue();
        final ChannelSubscriptionEntry element = entitySubscription.deregisterGraph( entry.getDescriptor() );
        if ( null != element && 0 == entitySubscription.getGraphSubscriptions().size() )
        {
          removedEntities += 1;
          final Object entity = _repository.deregisterEntity( type, entityID );
          getChangeBroker().removeAllChangeListeners( entity );
        }
      }
    }
    return removedEntities;
  }

  @Nonnull
  protected abstract String getSystemKey();

  @Nonnull
  protected abstract Class<G> getGraphType();

  protected abstract int updateSubscriptionForFilteredEntities( @Nonnull ChannelSubscriptionEntry graphEntry,
                                                                @Nonnull Object filter );

  protected int updateSubscriptionForFilteredEntities( @Nonnull final ChannelSubscriptionEntry graphEntry,
                                                       @Nullable final Object filter,
                                                       @Nonnull final Collection<EntitySubscriptionEntry> entities )
  {
    int removedEntities = 0;
    final ChannelDescriptor descriptor = graphEntry.getDescriptor();

    final EntitySubscriptionEntry[] subscriptionEntries =
      entities.toArray( new EntitySubscriptionEntry[ entities.size() ] );
    for ( final EntitySubscriptionEntry entry : subscriptionEntries )
    {
      final Class<?> entityType = entry.getType();
      final Object entityID = entry.getID();

      if ( !doesEntityMatchFilter( descriptor, filter, entityType, entityID ) )
      {
        final EntitySubscriptionEntry entityEntry =
          _subscriptionManager.removeEntityFromGraph( entityType, entityID, descriptor );
        final boolean deregisterEntity = 0 == entityEntry.getGraphSubscriptions().size();
        if ( LOG.isLoggable( getLogLevel() ) )
        {
          LOG.log( getLogLevel(),
                   "Removed entity " + entityType.getSimpleName() + "/" + entityID +
                   " from graph " + descriptor + " resulting in " +
                   entityEntry.getGraphSubscriptions().size() + " subscriptions left for entity." +
                   ( deregisterEntity ? " De-registering entity!" : "" ) );
        }
        // If there is only one subscriber then lets delete it
        if ( deregisterEntity )
        {
          _subscriptionManager.removeEntity( entityType, entityID );
          _repository.deregisterEntity( entityType, entityID );
          removedEntities += 1;
        }
      }
    }
    return removedEntities;
  }

  protected abstract boolean doesEntityMatchFilter( @Nonnull ChannelDescriptor descriptor,
                                                    @Nullable Object filter,
                                                    @Nonnull Class<?> entityType,
                                                    @Nonnull Object entityID );

  /**
   * Return the graph for specified channel.
   *
   * @param channel the channel code.
   * @return the graph enum associated with channel.
   * @throws IllegalArgumentException if no such channel
   */
  @Nonnull
  protected abstract G channelToGraph( int channel )
    throws IllegalArgumentException;

  /**
   * Template method invoked when progressDataLoad() is about to return false and terminate load process.
   */
  protected void onTerminatingIncrementalDataLoadProcess()
  {
  }

  /**
   * Invoked when a change set has been completely processed.
   *
   * @param status the status describing the results of data load.
   */
  protected void onDataLoadComplete( @Nonnull final DataLoadStatus status )
  {
    fireDataLoadCompleteEvent( status );
  }

  /**
   * Invoked to fire an event when data load has completed.
   */
  protected void fireDataLoadCompleteEvent( @Nonnull final DataLoadStatus status )
  {
  }

  protected Level getLogLevel()
  {
    return Level.FINEST;
  }

  protected EntityRepository getRepository()
  {
    return _repository;
  }

  /**
   * @return true if a load action should result in the EntityRepository being validated.
   */
  protected boolean shouldValidateOnLoad()
  {
    return false;
  }

  /**
   * Perform a validation of the EntityRepository.
   */
  protected void validateRepository()
  {
    getEntityRepositoryValidator().validate( getRepository() );
  }

  protected EntityRepositoryValidator getEntityRepositoryValidator()
  {
    return new EntityRepositoryValidator();
  }

  protected boolean requestDebugOutputEnabled()
  {
    return false;
  }

  protected boolean subscriptionsDebugOutputEnabled()
  {
    return false;
  }

  protected boolean repositoryDebugOutputEnabled()
  {
    return false;
  }

  protected void outputRepositoryDebug()
  {
    getEntityRepositoryDebugger().outputRepository( getRepository() );
  }

  protected EntityRepositoryDebugger getEntityRepositoryDebugger()
  {
    return new EntityRepositoryDebugger();
  }

  protected void outputSubscriptionDebug()
  {
    getSubscriptionDebugger().outputSubscriptionManager( getSubscriptionManager() );
  }

  protected EntitySubscriptionDebugger getSubscriptionDebugger()
  {
    return new EntitySubscriptionDebugger();
  }

  protected void outputRequestDebug()
  {
    getRequestDebugger().outputRequests( getSessionContext().getKey() + ":", ensureSession() );
  }

  protected RequestDebugger getRequestDebugger()
  {
    return new RequestDebugger();
  }
}
