package org.realityforge.replicant.client.transport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
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
import org.realityforge.replicant.client.EntitySystem;
import org.realityforge.replicant.client.Linkable;

/**
 * Class from which to extend to implement a service that loads data from a change set.
 * Data can be loaded by bulk or incrementally and the load can be broken up into several
 * steps to avoid locking a thread such as in GWT.
 */
@SuppressWarnings( { "WeakerAccess", "unused" } )
public abstract class AbstractDataLoaderService
  implements DataLoaderService
{
  protected static final Logger LOG = Logger.getLogger( AbstractDataLoaderService.class.getName() );

  private static final int DEFAULT_CHANGES_TO_PROCESS_PER_TICK = 100;
  private static final int DEFAULT_LINKS_TO_PROCESS_PER_TICK = 100;

  private DataLoadAction _currentAction;
  private AreaOfInterestEntry _currentAoiAction;
  private int _changesToProcessPerTick = DEFAULT_CHANGES_TO_PROCESS_PER_TICK;
  private int _linksToProcessPerTick = DEFAULT_LINKS_TO_PROCESS_PER_TICK;
  private boolean _incrementalDataLoadInProgress;
  private final DataLoaderListenerSupport _listenerSupport = new DataLoaderListenerSupport();
  /**
   * Action invoked after current action completes to reset session state.
   */
  private Runnable _resetAction;
  @Nonnull
  private State _state = State.DISCONNECTED;

  private ClientSession _session;

  @Nonnull
  @Override
  public String getKey()
  {
    return getSessionContext().getKey();
  }

  @Nonnull
  @Override
  public State getState()
  {
    return _state;
  }

  @SuppressWarnings( "ConstantConditions" )
  protected void setState( @Nonnull final State state )
  {
    assert null != state;
    _state = state;
  }

  @Nonnull
  protected abstract DataLoaderServiceConfig config();

  @Nonnull
  protected DataLoaderListener getListener()
  {
    return _listenerSupport;
  }

  @Override
  public boolean addDataLoaderListener( @Nonnull final DataLoaderListener listener )
  {
    return _listenerSupport.addListener( listener );
  }

  @Override
  public boolean removeDataLoaderListener( @Nonnull final DataLoaderListener listener )
  {
    return _listenerSupport.removeListener( listener );
  }

  @Nonnull
  protected abstract SessionContext getSessionContext();

  @Nonnull
  protected abstract CacheService getCacheService();

  @Nonnull
  protected EntityChangeBroker getChangeBroker()
  {
    return getEntitySystem().getChangeBroker();
  }

  @Nonnull
  protected abstract ChangeMapper getChangeMapper();

  @Nonnull
  protected EntitySubscriptionManager getSubscriptionManager()
  {
    return getEntitySystem().getSubscriptionManager();
  }

  @Nonnull
  protected EntityRepository getRepository()
  {
    return getEntitySystem().getRepository();
  }

  protected abstract EntitySystem getEntitySystem();

  protected boolean shouldPurgeOnSessionChange()
  {
    return true;
  }

  protected void setSession( @Nullable final ClientSession session, @Nullable final Runnable postAction )
  {
    final Runnable runnable = () -> doSetSession( session, postAction );
    if ( null == _currentAction )
    {
      runnable.run();
    }
    else
    {
      _resetAction = runnable;
    }
  }

  protected void doSetSession( @Nullable final ClientSession session, @Nullable final Runnable postAction )
  {
    if ( session != _session )
    {
      _session = session;
      // This should probably be moved elsewhere ... but where?
      getSessionContext().setSession( session );
      if ( shouldPurgeOnSessionChange() )
      {
        final boolean enabled = getChangeBroker().isEnabled();
        if ( enabled )
        {
          getChangeBroker().disable( getChangeBrokerKey() );
        }
        //TODO: else schedule action so that it runs in loop
        // until it can disable broker. This will involve replacing _resetAction
        // with something more like existing action setup.
        purgeSubscriptions();
        if ( enabled )
        {
          getChangeBroker().enable( getChangeBrokerKey() );
        }
      }
    }
    if ( null != postAction )
    {
      postAction.run();
    }
  }

  @Nonnull
  protected String getChangeBrokerKey()
  {
    return getGraphType().getSimpleName();
  }

  /**
   * {@inheritDoc}
   */
  @Nullable
  @Override
  public ClientSession getSession()
  {
    return _session;
  }

  /**
   * {@inheritDoc}
   */
  @Nonnull
  public abstract ClientSession ensureSession();

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
    final Class graphClass = getGraphType();
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
        final ChannelSubscriptionEntry entry = subscriptionManager.removeSubscription( new ChannelDescriptor( graph ) );
        deregisterUnOwnedEntities( entry );
      }
    }
  }

  protected void unsubscribeInstanceGraphs( @Nonnull final Enum graph )
  {
    final EntitySubscriptionManager subscriptionManager = getSubscriptionManager();
    for ( final Object id : new ArrayList<>( subscriptionManager.getInstanceSubscriptions( graph ) ) )
    {
      deregisterUnOwnedEntities( subscriptionManager.removeSubscription( new ChannelDescriptor( graph, id ) ) );
    }
  }

  @SuppressWarnings( "unchecked" )
  @Nonnull
  private ArrayList<Enum> sortGraphs( @Nonnull final Set<Enum> enums )
  {
    final ArrayList<Enum> list = new ArrayList<>( enums );
    Collections.sort( list );
    Collections.reverse( list );
    return list;
  }

  /**
   * Schedule data loads using incremental scheduler.
   */
  @Override
  public void scheduleDataLoad()
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
      getListener().onDataLoadFailure( this, e );
      _incrementalDataLoadInProgress = false;
      return false;
    }
    return _incrementalDataLoadInProgress;
  }

  /**
   * Actually perform the scheduling of the data load action.
   */
  protected abstract void doScheduleDataLoad();

  protected void setChangesToProcessPerTick( final int changesToProcessPerTick )
  {
    _changesToProcessPerTick = changesToProcessPerTick;
  }

  protected void setLinksToProcessPerTick( final int linksToProcessPerTick )
  {
    _linksToProcessPerTick = linksToProcessPerTick;
  }

  @Nonnull
  protected abstract ChangeSet parseChangeSet( @Nonnull String rawJsonData );

  protected boolean progressAreaOfInterestActions()
  {
    if ( null == _currentAoiAction )
    {
      final LinkedList<AreaOfInterestEntry> actions = ensureSession().getPendingAreaOfInterestActions();
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
      final ChannelDescriptor descriptor = _currentAoiAction.getDescriptor();
      final Object filterParameter = _currentAoiAction.getFilterParameter();

      final String label = getKey() + ":" + descriptor + ( null == filterParameter ? "" : "[" + filterParameter + "]" );
      final String cacheKey = _currentAoiAction.getCacheKey();
      final AreaOfInterestAction action = _currentAoiAction.getAction();
      if ( action == AreaOfInterestAction.ADD )
      {
        final ChannelSubscriptionEntry entry = getSubscriptionManager().findSubscription( descriptor );
        //Already subscribed
        if ( null != entry )
        {
          if ( entry.isExplicitSubscription() )
          {
            LOG.warning( "Subscription to " + label + " requested but already subscribed." );
            completeAoiAction();
            return true;
          }
          else
          {
            LOG.warning( "Existing subscription to " + label + " converted to a explicit subscription." );
            entry.setExplicitSubscription( true );
            completeAoiAction();
            return true;
          }
        }
        final CacheEntry cacheEntry = getCacheService().lookup( cacheKey );
        final String eTag;
        final Consumer<Runnable> cacheAction;
        if ( null != cacheEntry )
        {
          eTag = cacheEntry.getETag();
          LOG.info( "Found locally cached data for graph " + label + " with etag " + eTag + "." );
          cacheAction = a ->
          {
            LOG.info( "Loading cached data for graph " + label + " with etag " + eTag );
            final Runnable completeAoiAction = () ->
            {
              LOG.info( "Completed load of cached data for graph " + label + " with etag " + eTag + "." );
              completeAoiAction();
              a.run();
            };
            //TODO: Figure out how to make the bulkLoad configurable
            ensureSession().enqueueOOB( cacheEntry.getContent(), completeAoiAction, true );
          };
        }
        else
        {
          eTag = null;
          cacheAction = null;
        }
        final Consumer<Runnable> completionAction = a ->
        {
          LOG.info( "Subscription to " + label + " completed." );
          completeAoiAction();
          a.run();
        };
        final Consumer<Runnable> failAction = a ->
        {
          LOG.info( "Subscription to " + label + " failed." );
          completeAoiAction();
          a.run();
        };
        LOG.info( "Subscription to " + label + " requested." );
        requestSubscribeToGraph( descriptor, filterParameter, eTag, cacheAction, completionAction, failAction );
        return true;
      }
      else if ( action == AreaOfInterestAction.REMOVE )
      {
        final ChannelSubscriptionEntry entry = getSubscriptionManager().findSubscription( descriptor );
        //Not subscribed
        if ( null == entry )
        {
          LOG.warning( "Unsubscribe from " + label + " requested but not subscribed." );
          completeAoiAction();
          return true;
        }
        else if ( !entry.isExplicitSubscription() )
        {
          LOG.warning( "Unsubscribe from " + label + " requested but not explicitly subscribed." );
          completeAoiAction();
          return true;
        }

        LOG.info( "Unsubscribe from " + label + " requested." );
        final Consumer<Runnable> completionAction = a ->
        {
          LOG.info( "Unsubscribe from " + label + " completed." );
          entry.setExplicitSubscription( false );
          completeAoiAction();
          a.run();
        };
        final Consumer<Runnable> failAction = a ->
        {
          LOG.info( "Unsubscribe from " + label + " failed." );
          entry.setExplicitSubscription( false );
          completeAoiAction();
          a.run();
        };
        requestUnsubscribeFromGraph( descriptor, completionAction, failAction );
        return true;
      }
      else
      {
        final ChannelSubscriptionEntry entry = getSubscriptionManager().findSubscription( descriptor );
        //Not subscribed
        if ( null == entry )
        {
          LOG.warning( "Subscription update of " + label + " requested but not subscribed." );
          completeAoiAction();
          return true;
        }

        final Consumer<Runnable> completionAction = a ->
        {
          LOG.warning( "Subscription update of " + label + " completed." );
          completeAoiAction();
          a.run();
        };
        final Consumer<Runnable> failAction = a ->
        {
          LOG.warning( "Subscription update of " + label + " failed." );
          completeAoiAction();
          a.run();
        };
        LOG.warning( "Subscription update of " + label + " requested." );
        assert null != filterParameter;
        requestUpdateSubscription( descriptor, filterParameter, completionAction, failAction );
        return true;
      }
    }
  }

  private void completeAoiAction()
  {
    scheduleDataLoad();
    _currentAoiAction.markAsComplete();
    _currentAoiAction = null;
  }

  protected abstract void requestSubscribeToGraph( @Nonnull ChannelDescriptor descriptor,
                                                   @Nullable Object filterParameter,
                                                   @Nullable String eTag,
                                                   @Nullable Consumer<Runnable> cacheAction,
                                                   @Nonnull Consumer<Runnable> completionAction,
                                                   @Nonnull Consumer<Runnable> failAction );

  protected abstract void requestUnsubscribeFromGraph( @Nonnull ChannelDescriptor descriptor,
                                                       @Nonnull Consumer<Runnable> completionAction,
                                                       @Nonnull Consumer<Runnable> failAction );

  protected abstract void requestUpdateSubscription( @Nonnull ChannelDescriptor descriptor,
                                                     @Nonnull Object filterParameter,
                                                     @Nonnull Consumer<Runnable> completionAction,
                                                     @Nonnull Consumer<Runnable> failAction );

  @Override
  public boolean isSubscribed( @Nonnull final ChannelDescriptor descriptor )
  {
    return null != getSubscriptionManager().findSubscription( descriptor );
  }

  @Override
  public boolean isAreaOfInterestActionPending( @Nonnull final AreaOfInterestAction action,
                                                @Nonnull final ChannelDescriptor descriptor,
                                                @Nullable final Object filter )
  {
    final ClientSession session = getSession();
    return
      null != _currentAoiAction && _currentAoiAction.match( action, descriptor, filter ) ||
      (
        null != session &&
        session.getPendingAreaOfInterestActions().stream().
          anyMatch( a -> a.match( action, descriptor, filter ) )
      );
  }

  @Override
  public int indexOfPendingAreaOfInterestAction( @Nonnull final AreaOfInterestAction action,
                                                 @Nonnull final ChannelDescriptor descriptor,
                                                 @Nullable final Object filter )
  {
    final ClientSession session = getSession();
    if ( null != _currentAoiAction && _currentAoiAction.match( action, descriptor, filter ) )
    {
      return 0;
    }
    else if ( null != session )
    {
      final LinkedList<AreaOfInterestEntry> actions = session.getPendingAreaOfInterestActions();
      int index = actions.size();

      final Iterator<AreaOfInterestEntry> iterator = actions.descendingIterator();
      while ( iterator.hasNext() )
      {
        final AreaOfInterestEntry entry = iterator.next();
        if ( entry.match( action, descriptor, filter ) )
        {
          return index;
        }
        index -= 1;
      }

    }
    return -1;
  }

  // Method only used in tests
  DataLoadAction getCurrentAction()
  {
    return _currentAction;
  }

  protected boolean progressDataLoad()
  {
    final ClientSession session = ensureSession();
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
        getChangeBroker().disable( getChangeBrokerKey() );
      }
      else
      {
        getChangeBroker().pause( getChangeBrokerKey() );
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
        final ChannelDescriptor descriptor = toChannelDescriptor( action );
        final Object filter = action.getChannelFilter();
        final ChannelAction.Action actionType = action.getAction();
        if ( LOG.isLoggable( getLogLevel() ) )
        {
          final String message = "ChannelAction:: " + actionType.name() + " " + descriptor + " filter=" + filter;
          LOG.log( getLogLevel(), message );
        }

        if ( ChannelAction.Action.ADD == actionType )
        {
          _currentAction.recordChannelSubscribe( new ChannelChangeStatus( descriptor, filter, 0 ) );
          boolean explicitSubscribe = false;
          if ( null != _currentAoiAction &&
               _currentAoiAction.isInProgress() &&
               _currentAoiAction.getDescriptor().equals( descriptor ) )
          {
            if ( LOG.isLoggable( getLogLevel() ) )
            {
              LOG.log( getLogLevel(), "Recording explicit subscription for " + descriptor );
            }
            explicitSubscribe = true;
          }
          getSubscriptionManager().recordSubscription( descriptor, filter, explicitSubscribe );
        }
        else if ( ChannelAction.Action.REMOVE == actionType )
        {
          final ChannelSubscriptionEntry entry = getSubscriptionManager().removeSubscription( descriptor );
          final int removedEntities = deregisterUnOwnedEntities( entry );
          _currentAction.recordChannelUnsubscribe( new ChannelChangeStatus( descriptor, filter, removedEntities ) );
        }
        else if ( ChannelAction.Action.UPDATE == actionType )
        {
          final ChannelSubscriptionEntry entry = getSubscriptionManager().updateSubscription( descriptor, filter );
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
          getChangeBroker().enable( getChangeBrokerKey() );
        }
      }
      else
      {
        if ( _currentAction.hasBrokerBeenPaused() )
        {
          getChangeBroker().resume( getChangeBrokerKey() );
        }
      }
      if ( config().repositoryDebugOutputEnabled() )
      {
        outputRepositoryDebug();
      }
      if ( config().subscriptionsDebugOutputEnabled() )
      {
        outputSubscriptionDebug();
      }
      if ( config().shouldValidateRepositoryOnLoad() )
      {
        validateRepository();
      }
      return true;
    }
    final DataLoadStatus status = _currentAction.toStatus( getKey() );
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
        if ( config().requestDebugOutputEnabled() )
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

  @Nonnull
  private ChannelDescriptor toChannelDescriptor( final ChannelAction action )
  {
    final int channel = action.getChannelID();
    final Object subChannelID = action.getSubChannelID();
    final Enum graph = channelToGraph( channel );
    return new ChannelDescriptor( graph, subChannelID );
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
          final Object entity = getRepository().deregisterEntity( type, entityID );
          getChangeBroker().removeAllChangeListeners( entity );
        }
      }
    }
    return removedEntities;
  }

  @Nonnull
  public abstract Class<? extends Enum> getGraphType();

  protected abstract int updateSubscriptionForFilteredEntities( @Nonnull ChannelSubscriptionEntry graphEntry,
                                                                @Nullable Object filter );

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
          getSubscriptionManager().removeEntityFromGraph( entityType, entityID, descriptor );
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
          getSubscriptionManager().removeEntity( entityType, entityID );
          getRepository().deregisterEntity( entityType, entityID );
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

  @Override
  public void connect()
  {
    if ( null == getSession() )
    {
      performConnect();
    }
  }

  private void performConnect()
  {
    State state = State.ERROR;
    try
    {
      doConnect( this::completeConnect );
      state = State.CONNECTING;
    }
    finally
    {
      setState( state );
    }
  }

  private void completeConnect()
  {
    setState( State.CONNECTED );
    getListener().onConnect( this );
  }

  protected abstract void doConnect( @Nullable Runnable runnable );

  protected void handleInvalidConnect( @Nonnull final Throwable exception )
  {
    setState( State.ERROR );
    getListener().onInvalidConnect( this, exception );
  }

  protected void handleInvalidDisconnect( @Nonnull final Throwable exception )
  {
    setState( State.ERROR );
    getListener().onInvalidDisconnect( this, exception );
  }

  @Override
  public void disconnect()
  {
    final ClientSession session = getSession();
    if ( null != session && State.DISCONNECTING != getState() )
    {
      setState( State.DISCONNECTING );
      doDisconnect( session, this::onDisconnect );
    }
  }

  private void onDisconnect()
  {
    setState( State.DISCONNECTED );
    getListener().onDisconnect( this );
  }

  protected abstract void doDisconnect( @Nonnull ClientSession session, @Nullable Runnable runnable );

  /**
   * Return the graph for specified channel.
   *
   * @param channel the channel code.
   * @return the graph enum associated with channel.
   * @throws IllegalArgumentException if no such channel
   */
  @Nonnull
  protected Enum channelToGraph( final int channel )
    throws IllegalArgumentException
  {
    assert getGraphType().isEnum();
    return getGraphType().getEnumConstants()[ channel ];
  }

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
    getListener().onDataLoadComplete( this, status );
  }

  @Nonnull
  protected Level getLogLevel()
  {
    return Level.FINEST;
  }

  /**
   * Perform a validation of the EntityRepository.
   */
  protected void validateRepository()
  {
    getEntityRepositoryValidator().validate( getRepository() );
  }

  @Nonnull
  protected EntityRepositoryValidator getEntityRepositoryValidator()
  {
    return new EntityRepositoryValidator();
  }

  protected void outputRepositoryDebug()
  {
    getEntityRepositoryDebugger().outputRepository( getRepository() );
  }

  @Nonnull
  protected EntityRepositoryDebugger getEntityRepositoryDebugger()
  {
    return new EntityRepositoryDebugger();
  }

  protected void outputSubscriptionDebug()
  {
    getSubscriptionDebugger().outputSubscriptionManager( getSubscriptionManager() );
  }

  @Nonnull
  protected EntitySubscriptionDebugger getSubscriptionDebugger()
  {
    return new EntitySubscriptionDebugger();
  }

  protected void outputRequestDebug()
  {
    getRequestDebugger().outputRequests( getKey() + ":", ensureSession() );
  }

  @Nonnull
  protected RequestDebugger getRequestDebugger()
  {
    return new RequestDebugger();
  }
}
