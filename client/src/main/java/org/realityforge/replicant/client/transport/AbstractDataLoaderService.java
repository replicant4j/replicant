package org.realityforge.replicant.client.transport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.Change;
import org.realityforge.replicant.client.ChangeMapper;
import org.realityforge.replicant.client.ChangeSet;
import org.realityforge.replicant.client.ChannelAction;
import org.realityforge.replicant.client.ChannelAddress;
import org.realityforge.replicant.client.ChannelSubscriptionEntry;
import org.realityforge.replicant.client.EntityChangeBroker;
import org.realityforge.replicant.client.EntityRepository;
import org.realityforge.replicant.client.EntityRepositoryDebugger;
import org.realityforge.replicant.client.EntityRepositoryValidator;
import org.realityforge.replicant.client.EntitySubscriptionEntry;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.client.EntitySystem;
import org.realityforge.replicant.client.FilterUtil;
import org.realityforge.replicant.client.Linkable;

/**
 * Class from which to extend to implement a service that loads data from a change set.
 * Data is loaded incrementally and the load can be broken up into several
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
  private List<AreaOfInterestEntry> _currentAoiActions = new ArrayList<>();
  private int _changesToProcessPerTick = DEFAULT_CHANGES_TO_PROCESS_PER_TICK;
  private int _linksToProcessPerTick = DEFAULT_LINKS_TO_PROCESS_PER_TICK;
  private boolean _incrementalDataLoadInProgress;
  private final DataLoaderListenerSupport _listenerSupport = new DataLoaderListenerSupport();
  /**
   * The list of entities that no longer part of any graph. This set is used to collect
   * entities so that their listeners can be purged at the end of the cycle.
   */
  private final HashSet<Object> _disownedEntities = new LinkedHashSet<>();
  /**
   * Action invoked after current action completes to reset session state.
   */
  private Runnable _resetAction;
  @Nonnull
  private State _state = State.DISCONNECTED;

  private ClientSession _session;

  /*
   * Timing of state changes
   */
  private Date _connectingAt;
  private Date _connectedAt;
  private Date _disconnectedAt;

  /**
   * The last error that was received during connection establishment.
   * Nulled at the time of disconnection
   */
  private Throwable _lastErrorDuringConnection;

  /**
   * The last error that caused whilst connected, probably caused connection to drop.
   * Never nulled.
   */
  private Throwable _lastError;
  private Date _lastErrorAt;

  @Nonnull
  @Override
  public String getKey()
  {
    return getSessionContext().getKey();
  }

  /**
   * {@inheritDoc}
   */
  @Nonnull
  @Override
  public State getState()
  {
    return _state;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Nullable
  public Date getConnectingAt()
  {
    return _connectingAt;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Nullable
  public Date getConnectedAt()
  {
    return _connectedAt;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Nullable
  public Date getDisconnectedAt()
  {
    return _disconnectedAt;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Nullable
  public Throwable getLastErrorDuringConnection()
  {
    return _lastErrorDuringConnection;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Nullable
  public Throwable getLastError()
  {
    return _lastError;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Nullable
  public Date getLastErrorAt()
  {
    return _lastErrorAt;
  }

  @Override
  public void onCommunicationError( @Nonnull final Throwable error )
  {
    _lastError = error;
    _lastErrorAt = new Date();
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
    final Class<?> graphClass = getGraphType();
    final EntitySubscriptionManager subscriptionManager = getSubscriptionManager();
    for ( final Enum<?> graph : sortGraphs( subscriptionManager.getInstanceSubscriptionKeys() ) )
    {
      if ( graph.getClass().equals( graphClass ) )
      {
        unsubscribeInstanceGraphs( graph );
      }
    }
    for ( final Enum<?> graph : sortGraphs( subscriptionManager.getTypeSubscriptions() ) )
    {
      if ( graph.getClass().equals( graphClass ) )
      {
        final ChannelSubscriptionEntry entry = subscriptionManager.removeSubscription( new ChannelAddress( graph ) );
        deregisterUnOwnedEntities( entry );
      }
    }

    purgeDisownedEntities();
  }

  protected void unsubscribeInstanceGraphs( @Nonnull final Enum graph )
  {
    final EntitySubscriptionManager subscriptionManager = getSubscriptionManager();
    for ( final Integer id : new ArrayList<>( subscriptionManager.getInstanceSubscriptions( graph ) ) )
    {
      deregisterUnOwnedEntities( subscriptionManager.removeSubscription( new ChannelAddress( graph, id ) ) );
    }
  }

  @SuppressWarnings( { "unchecked", "rawtypes" } )
  @Nonnull
  private List<Enum<?>> sortGraphs( @Nonnull final Set<Enum<?>> enums )
  {
    final List<Enum<?>> list = new ArrayList<>( enums );
    Collections.sort( (List) list );
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

  /**
   * Perform a single step in sending one (or a batch) or requests to the server.
   *
   * @return true if more work is to be done.
   */
  protected boolean progressAreaOfInterestActions()
  {
    if ( _currentAoiActions.isEmpty() )
    {
      final LinkedList<AreaOfInterestEntry> actions = ensureSession().getPendingAreaOfInterestActions();
      if ( 0 == actions.size() )
      {
        return false;
      }
      final AreaOfInterestEntry first = actions.removeFirst();
      _currentAoiActions.add( first );
      while ( actions.size() > 0 && isCompatibleForBulkChange( first, actions.get( 0 ) ) )
      {
        _currentAoiActions.add( actions.removeFirst() );
      }
    }

    if ( _currentAoiActions.get( 0 ).isInProgress() )
    {
      return false;
    }
    else
    {
      _currentAoiActions.forEach( AreaOfInterestEntry::markAsInProgress );
      final AreaOfInterestAction action = _currentAoiActions.get( 0 ).getAction();
      if ( action == AreaOfInterestAction.ADD )
      {
        return progressBulkAOIAddActions();
      }
      else if ( action == AreaOfInterestAction.REMOVE )
      {
        return progressBulkAOIRemoveActions();
      }
      else
      {
        return progressBulkAOIUpdateActions();
      }
    }
  }

  private String label( AreaOfInterestEntry entry )
  {
    final ChannelAddress descriptor = entry.getDescriptor();
    final Object filterParameter = entry.getFilterParameter();
    return getKey() + ":" + descriptor +
           ( null == filterParameter ? "" : "[" + filterToString( filterParameter ) + "]" );
  }

  private String label( List<AreaOfInterestEntry> entries )
  {
    if ( entries.size() == 0 )
    {
      return "";
    }
    final Object filterParameter = entries.get( 0 ).getFilterParameter();
    return getKey() +
           ":" +
           entries.stream().map( e -> e.getDescriptor().toString() ).collect( Collectors.joining( "/" ) ) +
           ( null == filterParameter ? "" : "[" + filterToString( filterParameter ) + "]" );
  }

  private boolean progressBulkAOIUpdateActions()
  {
    _currentAoiActions.removeIf( a -> {
      final ChannelSubscriptionEntry entry = getSubscriptionManager().findSubscription( a.getDescriptor() );
      if ( null == entry )
      {
        LOG.warning( () -> "Subscription update of " + label( a ) + " requested but not subscribed." );
        a.markAsComplete();
        return true;
      }
      return false;
    } );

    if ( 0 == _currentAoiActions.size() )
    {
      completeAoiAction();
      return true;
    }

    final Consumer<Runnable> completionAction = a ->
    {
      LOG.warning( () -> "Subscription update of " + label( _currentAoiActions ) + " completed." );
      completeAoiAction();
      a.run();
    };
    final Consumer<Runnable> failAction = a ->
    {
      LOG.warning( () -> "Subscription update of " + label( _currentAoiActions ) + " failed." );
      completeAoiAction();
      a.run();
    };
    LOG.warning( () -> "Subscription update of " + label( _currentAoiActions ) + " requested." );

    final AreaOfInterestEntry aoiEntry = _currentAoiActions.get( 0 );
    assert null != aoiEntry.getFilterParameter();
    if ( _currentAoiActions.size() > 1 )
    {
      final List<ChannelAddress> ids =
        _currentAoiActions.stream().map( AreaOfInterestEntry::getDescriptor ).collect( Collectors.toList() );
      requestBulkUpdateSubscription( ids, aoiEntry.getFilterParameter(), completionAction, failAction );
    }
    else
    {
      final ChannelAddress descriptor = aoiEntry.getDescriptor();
      requestUpdateSubscription( descriptor, aoiEntry.getFilterParameter(), completionAction, failAction );
    }
    return true;
  }

  private boolean progressBulkAOIRemoveActions()
  {
    _currentAoiActions.removeIf( a -> {
      final ChannelSubscriptionEntry entry = getSubscriptionManager().findSubscription( a.getDescriptor() );
      if ( null == entry )
      {
        LOG.warning( () -> "Unsubscribe from " + label( a ) + " requested but not subscribed." );
        a.markAsComplete();
        return true;
      }
      else if ( !entry.isExplicitSubscription() )
      {
        LOG.warning( () -> "Unsubscribe from " + label( a ) + " requested but not explicitly subscribed." );
        a.markAsComplete();
        return true;
      }
      return false;
    } );

    if ( 0 == _currentAoiActions.size() )
    {
      completeAoiAction();
      return true;
    }

    LOG.info( () -> "Unsubscribe from " + label( _currentAoiActions ) + " requested." );
    final Consumer<Runnable> completionAction = postAction ->
    {
      LOG.info( () -> "Unsubscribe from " + label( _currentAoiActions ) + " completed." );
      _currentAoiActions.forEach( a -> {
        final ChannelSubscriptionEntry entry = getSubscriptionManager().findSubscription( a.getDescriptor() );
        if ( null != entry )
        {
          entry.setExplicitSubscription( false );
        }
      } );
      completeAoiAction();
      postAction.run();
    };

    final Consumer<Runnable> failAction = postAction ->
    {
      LOG.info( "Unsubscribe from " + label( _currentAoiActions ) + " failed." );
      _currentAoiActions.forEach( a -> {
        final ChannelSubscriptionEntry entry = getSubscriptionManager().findSubscription( a.getDescriptor() );
        if ( null != entry )
        {
          entry.setExplicitSubscription( false );
        }
      } );
      completeAoiAction();
      postAction.run();
    };

    final AreaOfInterestEntry aoiEntry = _currentAoiActions.get( 0 );
    if ( _currentAoiActions.size() > 1 )
    {
      final List<ChannelAddress> ids =
        _currentAoiActions.stream().map( AreaOfInterestEntry::getDescriptor ).collect( Collectors.toList() );
      requestBulkUnsubscribeFromGraph( ids, completionAction, failAction );
    }
    else
    {
      final ChannelAddress descriptor = aoiEntry.getDescriptor();
      requestUnsubscribeFromGraph( descriptor, completionAction, failAction );
    }
    return true;
  }

  private boolean progressBulkAOIAddActions()
  {
    _currentAoiActions.removeIf( a -> {
      final ChannelSubscriptionEntry entry = getSubscriptionManager().findSubscription( a.getDescriptor() );
      if ( null != entry )
      {
        if ( entry.isExplicitSubscription() )
        {
          LOG.warning( "Subscription to " + label( a ) + " requested but already subscribed." );
        }
        else
        {
          LOG.warning( () -> "Existing subscription to " + label( a ) + " converted to a explicit subscription." );
          entry.setExplicitSubscription( true );
        }
        a.markAsComplete();
        return true;
      }
      return false;
    } );

    if ( 0 == _currentAoiActions.size() )
    {
      completeAoiAction();
      return true;
    }

    final Consumer<Runnable> completionAction = a ->
    {
      LOG.info( () -> "Subscription to " + label( _currentAoiActions ) + " completed." );
      completeAoiAction();
      a.run();
    };
    final Consumer<Runnable> failAction = a ->
    {
      LOG.info( () -> "Subscription to " + label( _currentAoiActions ) + " failed." );
      completeAoiAction();
      a.run();
    };

    final AreaOfInterestEntry aoiEntry = _currentAoiActions.get( 0 );
    if ( _currentAoiActions.size() == 1 )
    {
      final String cacheKey = aoiEntry.getCacheKey();
      final CacheEntry cacheEntry = getCacheService().lookup( cacheKey );
      final String eTag;
      final Consumer<Runnable> cacheAction;
      if ( null != cacheEntry )
      {
        eTag = cacheEntry.getETag();
        LOG.info( () -> "Found locally cached data for graph " + label( aoiEntry ) + " with etag " + eTag + "." );
        cacheAction = a ->
        {
          LOG.info( () -> "Loading cached data for graph " + label( aoiEntry ) + " with etag " + eTag );
          final Runnable completeAoiAction = () ->
          {
            LOG.info( () -> "Completed load of cached data for graph " +
                            label( aoiEntry ) +
                            " with etag " +
                            eTag +
                            "." );
            completeAoiAction();
            a.run();
          };
          ensureSession().enqueueOOB( cacheEntry.getContent(), completeAoiAction );
        };
      }
      else
      {
        eTag = null;
        cacheAction = null;
      }
      LOG.info( () -> "Subscription to " + label( aoiEntry ) + " with eTag " + cacheKey + "=" + eTag + " requested" );
      requestSubscribeToGraph( aoiEntry.getDescriptor(),
                               aoiEntry.getFilterParameter(),
                               cacheKey,
                               eTag,
                               cacheAction,
                               completionAction,
                               failAction );
    }
    else
    {
      // don't support bulk loading of anything that is already cached
      final List<ChannelAddress> ids =
        _currentAoiActions.stream().map( AreaOfInterestEntry::getDescriptor ).collect( Collectors.toList() );
      requestBulkSubscribeToGraph( ids, aoiEntry.getFilterParameter(), completionAction, failAction );
    }
    return true;
  }

  private boolean isCompatibleForBulkChange( final AreaOfInterestEntry template,
                                             final AreaOfInterestEntry match )
  {
    final AreaOfInterestAction action = match.getAction();
    return null == getCacheService().lookup( template.getCacheKey() ) &&
           null == getCacheService().lookup( match.getCacheKey() ) &&
           template.getAction().equals( action ) &&
           template.getDescriptor().getGraph().equals( match.getDescriptor().getGraph() ) &&
           ( AreaOfInterestAction.REMOVE == action ||
             FilterUtil.filtersEqual( match.getFilterParameter(), template.getFilterParameter() ) );
  }

  private void completeAoiAction()
  {
    scheduleDataLoad();
    _currentAoiActions.forEach( AreaOfInterestEntry::markAsComplete );
    _currentAoiActions.clear();
  }

  protected abstract void requestSubscribeToGraph( @Nonnull ChannelAddress descriptor,
                                                   @Nullable Object filterParameter,
                                                   @Nullable String cacheKey,
                                                   @Nullable String eTag,
                                                   @Nullable Consumer<Runnable> cacheAction,
                                                   @Nonnull Consumer<Runnable> completionAction,
                                                   @Nonnull Consumer<Runnable> failAction );

  protected abstract void requestUnsubscribeFromGraph( @Nonnull ChannelAddress descriptor,
                                                       @Nonnull Consumer<Runnable> completionAction,
                                                       @Nonnull Consumer<Runnable> failAction );

  protected abstract void requestUpdateSubscription( @Nonnull ChannelAddress descriptor,
                                                     @Nonnull Object filterParameter,
                                                     @Nonnull Consumer<Runnable> completionAction,
                                                     @Nonnull Consumer<Runnable> failAction );

  protected abstract void requestBulkSubscribeToGraph( @Nonnull List<ChannelAddress> descriptor,
                                                       @Nullable Object filterParameter,
                                                       @Nonnull Consumer<Runnable> completionAction,
                                                       @Nonnull Consumer<Runnable> failAction );

  protected abstract void requestBulkUnsubscribeFromGraph( @Nonnull List<ChannelAddress> descriptors,
                                                           @Nonnull Consumer<Runnable> completionAction,
                                                           @Nonnull Consumer<Runnable> failAction );

  protected abstract void requestBulkUpdateSubscription( @Nonnull List<ChannelAddress> descriptors,
                                                         @Nonnull Object filterParameter,
                                                         @Nonnull Consumer<Runnable> completionAction,
                                                         @Nonnull Consumer<Runnable> failAction );

  @Override
  public boolean isIdle()
  {
    return _currentAoiActions.isEmpty() && _currentAction == null &&
    _session.getPendingActions().isEmpty() && _session.getPendingAreaOfInterestActions().isEmpty();
  }

  @Override
  public boolean isSubscribed( @Nonnull final ChannelAddress descriptor )
  {
    return null != getSubscriptionManager().findSubscription( descriptor );
  }

  @Override
  public boolean isAreaOfInterestActionPending( @Nonnull final AreaOfInterestAction action,
                                                @Nonnull final ChannelAddress descriptor,
                                                @Nullable final Object filter )
  {
    final ClientSession session = getSession();
    return
      null != _currentAoiActions &&
      _currentAoiActions.stream().anyMatch( a -> a.match( action, descriptor, filter ) ) ||
      (
        null != session &&
        session.getPendingAreaOfInterestActions().stream().
          anyMatch( a -> a.match( action, descriptor, filter ) )
      );
  }

  @Override
  public int indexOfPendingAreaOfInterestAction( @Nonnull final AreaOfInterestAction action,
                                                 @Nonnull final ChannelAddress descriptor,
                                                 @Nullable final Object filter )
  {
    final ClientSession session = getSession();
    if ( null != _currentAoiActions &&
         _currentAoiActions.stream().anyMatch( a -> a.match( action, descriptor, filter ) ) )
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

  // Method only used in tests
  List<AreaOfInterestEntry> getCurrentAOIActions()
  {
    return _currentAoiActions;
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
      final String requestId = _currentAction.isOob() ? null : changeSet.getRequestId();
      // OOB messages have no etags as from local cache or generated locally
      final String eTag = _currentAction.isOob() ? null : changeSet.getETag();
      final int sequence = _currentAction.isOob() ? 0 : changeSet.getSequence();
      if ( LOG.isLoggable( getLogLevel() ) )
      {
        LOG.log( getLogLevel(),
                 "Parsed ChangeSet:" +
                 " oob=" + _currentAction.isOob() +
                 " seq=" + sequence +
                 " requestId=" + requestId +
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
        request = null != requestId ? session.getRequest( requestId ) : null;
        if ( null == request && null != requestId )
        {
          final String message =
            "Unable to locate requestId '" + requestId + "' specified for ChangeSet: seq=" + sequence +
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
          else
          {
            if ( LOG.isLoggable( getLogLevel() ) )
            {
              LOG.log( getLogLevel(), "Not caching ChangeSet: seq=" + sequence + " cacheKey=" + cacheKey );
            }
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
      getChangeBroker().pause( getChangeBrokerKey() );
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
        final ChannelAddress descriptor = toChannelDescriptor( action );
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
          if ( _currentAoiActions.stream().anyMatch( a -> a.isInProgress() && a.getDescriptor().equals( descriptor ) ) )
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
      if ( _currentAction.hasBrokerBeenPaused() )
      {
        getChangeBroker().resume( getChangeBrokerKey() );
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

      purgeDisownedEntities();
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
      final String requestId = _currentAction.isOob() ? null : _currentAction.getChangeSet().getRequestId();
      if ( null != requestId )
      {
        // We can remove the request because this side ran second and the
        // RPC channel has already returned.

        final boolean removed = session.removeRequest( requestId );
        if ( !removed )
        {
          LOG.severe( "ChangeSet " + set.getSequence() + " expected to complete request '" +
                      requestId + "' but no request was registered with session." );
        }
        if ( config().requestDebugOutputEnabled() )
        {
          outputRequestDebug();
        }
      }
    }
    _currentAction = null;
    onDataLoadComplete( status );
    if ( null != _resetAction )
    {
      _resetAction.run();
      _resetAction = null;
    }
    return true;
  }

  private void purgeDisownedEntities()
  {
    for ( final Object entity : _disownedEntities )
    {
      getChangeBroker().removeAllChangeListeners( entity );
    }
    _disownedEntities.clear();
  }

  @Nonnull
  private ChannelAddress toChannelDescriptor( final ChannelAction action )
  {
    final int channel = action.getChannelId();
    final Integer subChannelId = action.getSubChannelId();
    final Enum graph = channelToGraph( channel );
    return new ChannelAddress( graph, subChannelId );
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

          /*
            We need to queue up entities to have their change listeners purged on next cycle.
            We can not do it immediately as otherwise listener will not be notified
           */
          _disownedEntities.add( entity );
        }
      }
    }
    return removedEntities;
  }

  @Nonnull
  public abstract Class<? extends Enum<?>> getGraphType();

  protected abstract int updateSubscriptionForFilteredEntities( @Nonnull ChannelSubscriptionEntry graphEntry,
                                                                @Nullable Object filter );

  protected int updateSubscriptionForFilteredEntities( @Nonnull final ChannelSubscriptionEntry graphEntry,
                                                       @Nullable final Object filter,
                                                       @Nonnull final Collection<EntitySubscriptionEntry> entities )
  {
    int removedEntities = 0;
    final ChannelAddress descriptor = graphEntry.getDescriptor();

    final EntitySubscriptionEntry[] subscriptionEntries = entities.toArray( new EntitySubscriptionEntry[ 0 ] );
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

  protected abstract boolean doesEntityMatchFilter( @Nonnull ChannelAddress descriptor,
                                                    @Nullable Object filter,
                                                    @Nonnull Class<?> entityType,
                                                    @Nonnull Object entityID );

  @Override
  public void connect()
  {
    if ( null == getSession() && State.CONNECTING != getState() )
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
      _connectingAt = new Date();
      _connectedAt = null;
      _disconnectedAt = null;
      state = State.CONNECTING;
    }
    finally
    {
      setState( state );
    }
  }

  private void completeConnect()
  {
    _connectedAt = new Date();
    setState( State.CONNECTED );
    getListener().onConnect( this );
  }

  protected abstract void doConnect( @Nullable Runnable runnable );

  protected void handleInvalidConnect( @Nonnull final Throwable exception )
  {
    _lastErrorDuringConnection = exception;
    setState( State.ERROR );
    getListener().onInvalidConnect( this, exception );
  }

  protected void handleInvalidDisconnect( @Nonnull final Throwable exception )
  {
    _lastErrorDuringConnection = null;
    _disconnectedAt = new Date();
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
      doDisconnect( this::onDisconnect );
    }
  }

  private void onDisconnect()
  {
    setState( State.DISCONNECTED );
    getListener().onDisconnect( this );
  }

  protected abstract void doDisconnect( @Nullable Runnable runnable );

  /**
   * Return the graph for specified channel.
   *
   * @param channel the channel code.
   * @return the graph enum associated with channel.
   * @throws IllegalArgumentException if no such channel
   */
  @Nonnull
  protected Enum<?> channelToGraph( final int channel )
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
  protected String filterToString( @Nullable final Object filterParameter )
  {
    if ( null == filterParameter )
    {
      return "";
    }
    else
    {
      return doFilterToString( filterParameter );
    }
  }

  @Nonnull
  protected abstract String doFilterToString( @Nonnull Object filterParameter );

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
