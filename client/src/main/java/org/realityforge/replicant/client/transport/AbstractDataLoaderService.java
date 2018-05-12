package org.realityforge.replicant.client.transport;

import arez.Disposable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.Verifiable;
import replicant.AreaOfInterestAction;
import replicant.AreaOfInterestEntry;
import replicant.ChangeSet;
import replicant.ChannelAddress;
import replicant.ChannelChange;
import replicant.Connector;
import replicant.DataLoadAction;
import replicant.Entity;
import replicant.EntityChange;
import replicant.FilterUtil;
import replicant.Linkable;
import replicant.Replicant;
import replicant.ReplicantContext;
import replicant.RequestEntry;
import replicant.SafeProcedure;
import replicant.Subscription;
import replicant.spy.DataLoadStatus;
import static org.realityforge.braincheck.Guards.*;

/**
 * Class from which to extend to implement a service that loads data from a change set.
 * Data is loaded incrementally and the load can be broken up into several
 * steps to avoid locking a thread such as in GWT.
 */
@SuppressWarnings( { "WeakerAccess", "unused" } )
public abstract class AbstractDataLoaderService
  extends Connector
{
  protected static final Logger LOG = Logger.getLogger( AbstractDataLoaderService.class.getName() );

  private static final int DEFAULT_CHANGES_TO_PROCESS_PER_TICK = 100;
  private static final int DEFAULT_LINKS_TO_PROCESS_PER_TICK = 100;

  private final CacheService _cacheService;

  private DataLoadAction _currentAction;
  private List<AreaOfInterestEntry> _currentAoiActions = new ArrayList<>();
  private int _changesToProcessPerTick = DEFAULT_CHANGES_TO_PROCESS_PER_TICK;
  private int _linksToProcessPerTick = DEFAULT_LINKS_TO_PROCESS_PER_TICK;
  private boolean _incrementalDataLoadInProgress;
  /**
   * Action invoked after current action completes to reset session state.
   */
  private Runnable _resetAction;

  private ClientSession _session;
  private Disposable _schedulerLock;

  protected AbstractDataLoaderService( @Nullable final ReplicantContext context,
                                       @Nonnull final Class<?> systemType,
                                       @Nonnull final CacheService cacheService )
  {
    super( context, systemType );
    _cacheService = Objects.requireNonNull( cacheService );
  }

  @Override
  public final void requestSubscribe( @Nonnull final ChannelAddress address, @Nullable final Object filterParameter )
  {
    //TODO: Send spy message ..
    ensureSession().enqueueAoiAction( address, AreaOfInterestAction.ADD, filterParameter );
    scheduleDataLoad();
  }

  @Override
  public final void requestSubscriptionUpdate( @Nonnull final ChannelAddress address,
                                               @Nullable final Object filterParameter )
  {
    //TODO: Send spy message ..
    ensureSession().enqueueAoiAction( address, AreaOfInterestAction.UPDATE, filterParameter );
    scheduleDataLoad();
  }

  @Override
  public final void requestUnsubscribe( @Nonnull final ChannelAddress address )
  {
    //TODO: Send spy message ..
    ensureSession().enqueueAoiAction( address, AreaOfInterestAction.REMOVE, null );
    scheduleDataLoad();
  }

  /**
   * A symbolic key for describing system.
   */
  @Nonnull
  protected String getKey()
  {
    return getSessionContext().getKey();
  }

  @Nonnull
  protected abstract SessionContext getSessionContext();

  @Nonnull
  protected abstract ChangeMapper getChangeMapper();

  protected boolean shouldPurgeOnSessionChange()
  {
    return true;
  }

  protected void setSession( @Nullable final ClientSession session, @Nonnull final SafeProcedure action )
  {
    final Runnable runnable = () -> doSetSession( session, action );
    if ( null == _currentAction )
    {
      runnable.run();
    }
    else
    {
      _resetAction = runnable;
    }
  }

  protected void doSetSession( @Nullable final ClientSession session, @Nonnull final SafeProcedure action )
  {
    if ( session != _session )
    {
      _session = session;
      // This should probably be moved elsewhere ... but where?
      getSessionContext().setSession( session );
      if ( shouldPurgeOnSessionChange() )
      {
        //TODO: else schedule action so that it runs in loop
        // until it can disable broker. This will involve replacing _resetAction
        // with something more like existing action setup.

        context().safeAction( generateName( "purgeSubscriptions" ), this::purgeSubscriptions );
      }
    }
    action.call();
  }

  @Nullable
  protected final ClientSession getSession()
  {
    return _session;
  }

  @Nonnull
  protected abstract ClientSession ensureSession();

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
   * Schedule data loads using incremental scheduler.
   */
  final void scheduleDataLoad()
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
    if ( null == _schedulerLock )
    {
      _schedulerLock = context().pauseScheduler();
    }
    try
    {
      final boolean aoiActionProgressed = progressAreaOfInterestActions();
      final boolean dataActionProgressed = progressDataLoad();
      _incrementalDataLoadInProgress = aoiActionProgressed || dataActionProgressed;
    }
    catch ( final Exception e )
    {
      onMessageProcessFailure( e );
      _incrementalDataLoadInProgress = false;
      return false;
    }
    finally
    {
      if ( !_incrementalDataLoadInProgress )
      {
        _schedulerLock.dispose();
        _schedulerLock = null;
      }
    }
    return _incrementalDataLoadInProgress;
  }

  /**
   * Actually perform the scheduling of the data load action.
   */
  protected abstract void doScheduleDataLoad();

  @SuppressWarnings( "SameParameterValue" )
  protected void setChangesToProcessPerTick( final int changesToProcessPerTick )
  {
    _changesToProcessPerTick = changesToProcessPerTick;
  }

  @SuppressWarnings( "SameParameterValue" )
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
    final ChannelAddress descriptor = entry.getAddress();
    final Object filterParameter = entry.getFilter();
    return getKey() + ":" + descriptor +
           ( null == filterParameter ? "" : "[" + filterToString( filterParameter ) + "]" );
  }

  private String label( List<AreaOfInterestEntry> entries )
  {
    if ( entries.size() == 0 )
    {
      return "";
    }
    final Object filterParameter = entries.get( 0 ).getFilter();
    return getKey() +
           ":" +
           entries.stream().map( e -> e.getAddress().toString() ).collect( Collectors.joining( "/" ) ) +
           ( null == filterParameter ? "" : "[" + filterToString( filterParameter ) + "]" );
  }

  private boolean progressBulkAOIUpdateActions()
  {
    context().safeAction( generateName( "removeUnneededUpdateRequests" ), () -> {
      _currentAoiActions.removeIf( a -> {
        final Subscription subscription = getReplicantContext().findSubscription( a.getAddress() );
        if ( null == subscription )
        {
          LOG.warning( () -> "Subscription update of " + label( a ) + " requested but not subscribed." );
          a.markAsComplete();
          return true;
        }
        return false;
      } );
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
    assert null != aoiEntry.getFilter();
    if ( _currentAoiActions.size() > 1 )
    {
      final List<ChannelAddress> ids =
        _currentAoiActions.stream().map( AreaOfInterestEntry::getAddress ).collect( Collectors.toList() );
      requestBulkUpdateSubscription( ids, aoiEntry.getFilter(), completionAction, failAction );
    }
    else
    {
      final ChannelAddress descriptor = aoiEntry.getAddress();
      requestUpdateSubscription( descriptor, aoiEntry.getFilter(), completionAction, failAction );
    }
    return true;
  }

  private boolean progressBulkAOIRemoveActions()
  {
    context().safeAction( generateName( "removeUnneededRemoveRequests" ), () -> {
      _currentAoiActions.removeIf( a -> {
        final Subscription subscription = getReplicantContext().findSubscription( a.getAddress() );
        if ( null == subscription )
        {
          LOG.warning( () -> "Unsubscribe from " + label( a ) + " requested but not subscribed." );
          a.markAsComplete();
          return true;
        }
        else if ( !subscription.isExplicitSubscription() )
        {
          LOG.warning( () -> "Unsubscribe from " + label( a ) + " requested but not explicitly subscribed." );
          a.markAsComplete();
          return true;
        }
        return false;
      } );
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
      context().safeAction( generateName( "setExplicitSubscription(false)" ), () -> _currentAoiActions.forEach( a -> {
        final Subscription subscription = getReplicantContext().findSubscription( a.getAddress() );
        if ( null != subscription )
        {
          subscription.setExplicitSubscription( false );
        }
      } ) );
      completeAoiAction();
      postAction.run();
    };

    final Consumer<Runnable> failAction = postAction ->
    {
      LOG.info( "Unsubscribe from " + label( _currentAoiActions ) + " failed." );
      context().safeAction( generateName( "setExplicitSubscription(false)" ), () -> _currentAoiActions.forEach( a -> {
        final Subscription subscription = getReplicantContext().findSubscription( a.getAddress() );
        if ( null != subscription )
        {
          subscription.setExplicitSubscription( false );
        }
      } ) );
      completeAoiAction();
      postAction.run();
    };

    final AreaOfInterestEntry aoiEntry = _currentAoiActions.get( 0 );
    if ( _currentAoiActions.size() > 1 )
    {
      final List<ChannelAddress> ids =
        _currentAoiActions.stream().map( AreaOfInterestEntry::getAddress ).collect( Collectors.toList() );
      requestBulkUnsubscribeFromChannel( ids, completionAction, failAction );
    }
    else
    {
      final ChannelAddress descriptor = aoiEntry.getAddress();
      requestUnsubscribeFromChannel( descriptor, completionAction, failAction );
    }
    return true;
  }

  private boolean progressBulkAOIAddActions()
  {
    // Remove all Add Aoi actions that need no action as they are already present locally
    context().safeAction( generateName( "removeUnneededAddRequests" ), () -> {
      _currentAoiActions.removeIf( a -> {
        final Subscription subscription = getReplicantContext().findSubscription( a.getAddress() );
        if ( null != subscription )
        {
          if ( subscription.isExplicitSubscription() )
          {
            LOG.warning( "Subscription to " + label( a ) + " requested but already subscribed." );
          }
          else
          {
            LOG.warning( () -> "Existing subscription to " + label( a ) + " converted to a explicit subscription." );
            subscription.setExplicitSubscription( true );
          }
          a.markAsComplete();
          return true;
        }
        return false;
      } );
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
      final CacheEntry cacheEntry = _cacheService.lookup( cacheKey );
      final String eTag;
      final Consumer<Runnable> cacheAction;
      if ( null != cacheEntry )
      {
        eTag = cacheEntry.getETag();
        LOG.info( () -> "Found locally cached data for channel " + label( aoiEntry ) + " with etag " + eTag + "." );
        cacheAction = a ->
        {
          LOG.info( () -> "Loading cached data for channel " + label( aoiEntry ) + " with etag " + eTag );
          final Runnable completeAoiAction = () ->
          {
            LOG.info( () -> "Completed load of cached data for channel " +
                            label( aoiEntry ) +
                            " with etag " +
                            eTag +
                            "." );
            completeAoiAction();
            a.run();
          };
          ensureSession().enqueueOOB( cacheEntry.getContent(), completeAoiAction );
          scheduleDataLoad();
        };
      }
      else
      {
        eTag = null;
        cacheAction = null;
      }
      LOG.info( () -> "Subscription to " + label( aoiEntry ) + " with eTag " + cacheKey + "=" + eTag + " requested" );
      requestSubscribeToChannel( aoiEntry.getAddress(),
                                 aoiEntry.getFilter(),
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
        _currentAoiActions.stream().map( AreaOfInterestEntry::getAddress ).collect( Collectors.toList() );
      requestBulkSubscribeToChannel( ids, aoiEntry.getFilter(), completionAction, failAction );
    }
    return true;
  }

  private boolean isCompatibleForBulkChange( final AreaOfInterestEntry template,
                                             final AreaOfInterestEntry match )
  {
    final AreaOfInterestAction action = match.getAction();
    return null == _cacheService.lookup( template.getCacheKey() ) &&
           null == _cacheService.lookup( match.getCacheKey() ) &&
           template.getAction().equals( action ) &&
           template.getAddress().getChannelType().equals( match.getAddress().getChannelType() ) &&
           ( AreaOfInterestAction.REMOVE == action ||
             FilterUtil.filtersEqual( match.getFilter(), template.getFilter() ) );
  }

  private void completeAoiAction()
  {
    scheduleDataLoad();
    _currentAoiActions.forEach( AreaOfInterestEntry::markAsComplete );
    _currentAoiActions.clear();
  }

  protected abstract void requestSubscribeToChannel( @Nonnull ChannelAddress descriptor,
                                                     @Nullable Object filterParameter,
                                                     @Nullable String cacheKey,
                                                     @Nullable String eTag,
                                                     @Nullable Consumer<Runnable> cacheAction,
                                                     @Nonnull Consumer<Runnable> completionAction,
                                                     @Nonnull Consumer<Runnable> failAction );

  protected abstract void requestUnsubscribeFromChannel( @Nonnull ChannelAddress descriptor,
                                                         @Nonnull Consumer<Runnable> completionAction,
                                                         @Nonnull Consumer<Runnable> failAction );

  protected abstract void requestUpdateSubscription( @Nonnull ChannelAddress descriptor,
                                                     @Nonnull Object filterParameter,
                                                     @Nonnull Consumer<Runnable> completionAction,
                                                     @Nonnull Consumer<Runnable> failAction );

  protected abstract void requestBulkSubscribeToChannel( @Nonnull List<ChannelAddress> descriptor,
                                                         @Nullable Object filterParameter,
                                                         @Nonnull Consumer<Runnable> completionAction,
                                                         @Nonnull Consumer<Runnable> failAction );

  protected abstract void requestBulkUnsubscribeFromChannel( @Nonnull List<ChannelAddress> descriptors,
                                                             @Nonnull Consumer<Runnable> completionAction,
                                                             @Nonnull Consumer<Runnable> failAction );

  protected abstract void requestBulkUpdateSubscription( @Nonnull List<ChannelAddress> descriptors,
                                                         @Nonnull Object filterParameter,
                                                         @Nonnull Consumer<Runnable> completionAction,
                                                         @Nonnull Consumer<Runnable> failAction );

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isAreaOfInterestActionPending( @Nonnull final AreaOfInterestAction action,
                                                @Nonnull final ChannelAddress address,
                                                @Nullable final Object filter )
  {
    final ClientSession session = getSession();
    return
      null != _currentAoiActions &&
      _currentAoiActions.stream().anyMatch( a -> a.match( action, address, filter ) ) ||
      (
        null != session &&
        session.getPendingAreaOfInterestActions().stream().
          anyMatch( a -> a.match( action, address, filter ) )
      );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int indexOfPendingAreaOfInterestAction( @Nonnull final AreaOfInterestAction action,
                                                 @Nonnull final ChannelAddress address,
                                                 @Nullable final Object filter )
  {
    final ClientSession session = getSession();
    if ( null != _currentAoiActions &&
         _currentAoiActions.stream().anyMatch( a -> a.match( action, address, filter ) ) )
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
        if ( entry.match( action, address, filter ) )
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
                 " changeCount=" + changeSet.getEntityChanges().length
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
            _cacheService.store( cacheKey, eTag, rawJsonData );
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

    if ( _currentAction.needsChannelActionsProcessed() )
    {
      context().safeAction( generateName( "processChannelActions" ), this::processChannelActions );
      return true;
    }

    //Step: Process a chunk of changes
    if ( _currentAction.areChangesPending() )
    {
      if ( LOG.isLoggable( getLogLevel() ) )
      {
        LOG.log( getLogLevel(), "Processing ChangeSet: " + _currentAction );
      }

      final ChangeSet changeSet = _currentAction.getChangeSet();
      context().safeAction( generateName( "applyChange" ), () -> {
        EntityChange change;
        for ( int i = 0; i < _changesToProcessPerTick && null != ( change = _currentAction.nextChange() ); i++ )
        {
          final Object entity = getChangeMapper().applyChange( change );
          if ( LOG.isLoggable( Level.INFO ) )
          {
            if ( change.isUpdate() )
            {
              _currentAction.incEntityUpdateCount();
            }
            else
            {
              _currentAction.incEntityRemoveCount();
            }
          }
          _currentAction.changeProcessed( change.isUpdate(), entity );
        }
      }, changeSet.getSequence(), changeSet.getRequestID() );
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
      final ChangeSet changeSet = _currentAction.getChangeSet();

      context().safeAction( generateName( "link" ), () -> {
        Linkable linkable;
        for ( int i = 0; i < _linksToProcessPerTick && null != ( linkable = _currentAction.nextEntityToLink() ); i++ )
        {
          linkable.link();
          if ( LOG.isLoggable( Level.INFO ) )
          {
            _currentAction.incEntityLinkCount();
          }
        }
      }, changeSet.getSequence(), changeSet.getRequestID() );
      return true;
    }

    final ChangeSet changeSet = _currentAction.getChangeSet();

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
        session.setLastRxSequence( changeSet.getSequence() );
      }
      if ( Replicant.shouldValidateRepositoryOnLoad() )
      {
        // This should never need a transaction ... unless the repository is invalid and there is unlinked data.
        context().safeAction( generateName( "validate" ), this::validateRepository );
      }

      return true;
    }
    final DataLoadStatus status = _currentAction.toStatus();
    if ( LOG.isLoggable( Level.INFO ) )
    {
      LOG.info( status.toString() );
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
          LOG.severe( "ChangeSet " + changeSet.getSequence() + " expected to complete request '" +
                      requestID + "' but no request was registered with session." );
        }
        if ( requestDebugOutputEnabled() )
        {
          outputRequestDebug();
        }
      }
    }
    _currentAction = null;
    onMessageProcessed( status );
    if ( null != _resetAction )
    {
      _resetAction.run();
      _resetAction = null;
    }
    return true;
  }

  private void processChannelActions()
  {
    _currentAction.markChannelActionsProcessed();
    final ChangeSet changeSet = _currentAction.getChangeSet();
    final ChannelChange[] channelChanges = changeSet.getChannelChanges();
    for ( final ChannelChange channelChange : channelChanges )
    {
      final ChannelAddress address = toAddress( channelChange );
      final Object filter = channelChange.getChannelFilter();
      final ChannelChange.Action actionType = channelChange.getAction();
      if ( LOG.isLoggable( getLogLevel() ) )
      {
        final String message = "ChannelChange:: " + actionType.name() + " " + address + " filter=" + filter;
        LOG.log( getLogLevel(), message );
      }

      if ( ChannelChange.Action.ADD == actionType )
      {
        _currentAction.incChannelAddCount();
        boolean explicitSubscribe = false;
        if ( _currentAoiActions.stream().anyMatch( a -> a.isInProgress() && a.getAddress().equals( address ) ) )
        {
          if ( LOG.isLoggable( getLogLevel() ) )
          {
            LOG.log( getLogLevel(), "Recording explicit subscription for " + address );
          }
          explicitSubscribe = true;
        }
        getReplicantContext().createSubscription( address, filter, explicitSubscribe );
      }
      else if ( ChannelChange.Action.REMOVE == actionType )
      {
        final Subscription subscription = getReplicantContext().findSubscription( address );
        assert null != subscription;
        Disposable.dispose( subscription );
        _currentAction.incChannelRemoveCount();
      }
      else if ( ChannelChange.Action.UPDATE == actionType )
      {
        final Subscription subscription = getReplicantContext().findSubscription( address );
        assert null != subscription;
        subscription.setFilter( filter );
        updateSubscriptionForFilteredEntities( subscription, filter );
        _currentAction.incChannelUpdateCount();
      }
      else
      {
        throw new IllegalStateException();
      }
    }
  }

  protected abstract boolean requestDebugOutputEnabled();

  @Nonnull
  private ChannelAddress toAddress( @Nonnull final ChannelChange action )
  {
    final int channelId = action.getChannelId();
    final Integer subChannelId = action.hasSubChannelId() ? action.getSubChannelId() : null;
    final Enum channelType = channelIdToType( channelId );
    return new ChannelAddress( channelType, subChannelId );
  }

  protected abstract void updateSubscriptionForFilteredEntities( @Nonnull Subscription subscription,
                                                                 @Nullable Object filter );

  protected void updateSubscriptionForFilteredEntities( @Nonnull final Subscription subscription,
                                                        @Nullable final Object filter,
                                                        @Nonnull final Collection<Entity> entities )
  {
    if ( !entities.isEmpty() )
    {
      final ChannelAddress address = subscription.getAddress();
      for ( final Entity entity : new ArrayList<>( entities ) )
      {
        if ( !doesEntityMatchFilter( address, filter, entity ) )
        {
          entity.delinkFromSubscription( subscription );
        }
      }
    }
  }

  protected abstract boolean doesEntityMatchFilter( @Nonnull ChannelAddress address,
                                                    @Nullable Object filter,
                                                    @Nonnull Entity entity );

  /**
   * Return the type for specified channel.
   *
   * @param channel the channel code.
   * @return the channelType associated with channelId.
   * @throws IllegalArgumentException if no such channel
   */
  @Nonnull
  protected Enum channelIdToType( final int channel )
    throws IllegalArgumentException
  {
    assert getSystemType().isEnum();
    return (Enum) getSystemType().getEnumConstants()[ channel ];
  }

  /**
   * Template method invoked when progressDataLoad() is about to return false and terminate load process.
   */
  protected void onTerminatingIncrementalDataLoadProcess()
  {
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
   * Check all the entities in the repository and raise an exception if an entity fails to validateRepository.
   *
   * An entity can fail to validateRepository if it is {@link Disposable} and {@link Disposable#isDisposed()} returns
   * true. An entity can also fail to validateRepository if it is {@link Verifiable} and {@link Verifiable#verify()}
   * throws an exception.
   *
   * @throws IllegalStateException if an invalid entity is found in the repository.
   */
  protected void validateRepository()
    throws IllegalStateException
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      for ( final Class<?> entityType : getReplicantContext().findAllEntityTypes() )
      {
        for ( final Object entity : getReplicantContext().findAllEntitiesByType( entityType ) )
        {
          invariant( () -> !Disposable.isDisposed( entity ),
                     () -> "Invalid disposed entity found during validation. Entity: " + entity );
          try
          {
            Verifiable.verify( entity );
          }
          catch ( final Exception e )
          {
            final String message = "Entity failed to verify. Entity = " + entity;
            LOG.log( Level.WARNING, message, e );
            throw new IllegalStateException( message, e );
          }
        }
      }
    }
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

  @Nullable
  protected String generateName( @Nonnull final String name )
  {
    return Replicant.areNamesEnabled() ? toString() + "." + name : null;
  }

  @Override
  public String toString()
  {
    return Replicant.areNamesEnabled() ? "DataLoader[" + getKey() + "]" : super.toString();
  }
}
