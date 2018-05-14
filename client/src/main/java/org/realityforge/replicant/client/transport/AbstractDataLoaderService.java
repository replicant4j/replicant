package org.realityforge.replicant.client.transport;

import arez.Disposable;
import arez.annotations.Action;
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
import replicant.Connection;
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

  private int _changesToProcessPerTick = DEFAULT_CHANGES_TO_PROCESS_PER_TICK;
  private int _linksToProcessPerTick = DEFAULT_LINKS_TO_PROCESS_PER_TICK;
  private boolean _incrementalDataLoadInProgress;
  /**
   * Action invoked after current action completes to reset connection state.
   */
  private Runnable _resetAction;

  private Connection _connection;
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
    ensureConnection().enqueueAoiAction( address, AreaOfInterestAction.ADD, filterParameter );
    scheduleDataLoad();
  }

  @Override
  public final void requestSubscriptionUpdate( @Nonnull final ChannelAddress address,
                                               @Nullable final Object filterParameter )
  {
    //TODO: Send spy message ..
    ensureConnection().enqueueAoiAction( address, AreaOfInterestAction.UPDATE, filterParameter );
    scheduleDataLoad();
  }

  @Override
  public final void requestUnsubscribe( @Nonnull final ChannelAddress address )
  {
    //TODO: Send spy message ..
    ensureConnection().enqueueAoiAction( address, AreaOfInterestAction.REMOVE, null );
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

  protected void onConnection( @Nonnull final String connectionId, @Nonnull final SafeProcedure action )
  {
    setConnection( new Connection( connectionId ), action );
    scheduleDataLoad();
  }

  protected void onDisconnection( @Nonnull final SafeProcedure action )
  {
    setConnection( null, action );
  }

  private void setConnection( @Nullable final Connection connection, @Nonnull final SafeProcedure action )
  {
    final Runnable runnable = () -> doSetConnection( connection, action );
    if ( null == connection || null == connection.getCurrentAction() )
    {
      runnable.run();
    }
    else
    {
      _resetAction = runnable;
    }
  }

  protected void doSetConnection( @Nullable final Connection connection, @Nonnull final SafeProcedure action )
  {
    if ( connection != _connection )
    {
      _connection = connection;
      // This should probably be moved elsewhere ... but where?
      getSessionContext().setConnection( connection );
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
  protected final Connection getConnection()
  {
    return _connection;
  }

  @Nonnull
  protected abstract Connection ensureConnection();

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
    final List<AreaOfInterestEntry> currentAOIActions = ensureConnection().getCurrentAoiActions();
    if ( currentAOIActions.isEmpty() )
    {
      final LinkedList<AreaOfInterestEntry> actions = ensureConnection().getPendingAreaOfInterestActions();
      if ( 0 == actions.size() )
      {
        return false;
      }
      final AreaOfInterestEntry first = actions.removeFirst();
      currentAOIActions.add( first );
      while ( actions.size() > 0 && isCompatibleForBulkChange( first, actions.get( 0 ) ) )
      {
        currentAOIActions.add( actions.removeFirst() );
      }
    }

    if ( currentAOIActions.get( 0 ).isInProgress() )
    {
      return false;
    }
    else
    {
      currentAOIActions.forEach( AreaOfInterestEntry::markAsInProgress );
      final AreaOfInterestAction action = currentAOIActions.get( 0 ).getAction();
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
    final List<AreaOfInterestEntry> currentAOIActions = ensureConnection().getCurrentAoiActions();
    context().safeAction( generateName( "removeUnneededUpdateRequests" ), () -> {
      currentAOIActions.removeIf( a -> {
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

    if ( 0 == currentAOIActions.size() )
    {
      completeAoiAction();
      return true;
    }

    final Consumer<SafeProcedure> completionAction = a ->
    {
      LOG.warning( () -> "Subscription update of " + label( currentAOIActions ) + " completed." );
      completeAoiAction();
      a.call();
    };
    final Consumer<SafeProcedure> failAction = a ->
    {
      LOG.warning( () -> "Subscription update of " + label( currentAOIActions ) + " failed." );
      completeAoiAction();
      a.call();
    };
    LOG.warning( () -> "Subscription update of " + label( currentAOIActions ) + " requested." );

    final AreaOfInterestEntry aoiEntry = currentAOIActions.get( 0 );
    assert null != aoiEntry.getFilter();
    if ( currentAOIActions.size() > 1 )
    {
      final List<ChannelAddress> ids =
        currentAOIActions.stream().map( AreaOfInterestEntry::getAddress ).collect( Collectors.toList() );
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
    final List<AreaOfInterestEntry> currentAOIActions = ensureConnection().getCurrentAoiActions();
    context().safeAction( generateName( "removeUnneededRemoveRequests" ), () -> {
      currentAOIActions.removeIf( a -> {
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

    if ( 0 == currentAOIActions.size() )
    {
      completeAoiAction();
      return true;
    }

    LOG.info( () -> "Unsubscribe from " + label( currentAOIActions ) + " requested." );
    final Consumer<SafeProcedure> completionAction = postAction ->
    {
      LOG.info( () -> "Unsubscribe from " + label( currentAOIActions ) + " completed." );
      context().safeAction( generateName( "setExplicitSubscription(false)" ), () -> currentAOIActions.forEach( a -> {
        final Subscription subscription = getReplicantContext().findSubscription( a.getAddress() );
        if ( null != subscription )
        {
          subscription.setExplicitSubscription( false );
        }
      } ) );
      completeAoiAction();
      postAction.call();
    };

    final Consumer<SafeProcedure> failAction = postAction ->
    {
      LOG.info( "Unsubscribe from " + label( currentAOIActions ) + " failed." );
      context().safeAction( generateName( "setExplicitSubscription(false)" ), () -> currentAOIActions.forEach( a -> {
        final Subscription subscription = getReplicantContext().findSubscription( a.getAddress() );
        if ( null != subscription )
        {
          subscription.setExplicitSubscription( false );
        }
      } ) );
      completeAoiAction();
      postAction.call();
    };

    final AreaOfInterestEntry aoiEntry = currentAOIActions.get( 0 );
    if ( currentAOIActions.size() > 1 )
    {
      final List<ChannelAddress> ids =
        currentAOIActions.stream().map( AreaOfInterestEntry::getAddress ).collect( Collectors.toList() );
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
    final List<AreaOfInterestEntry> currentAOIActions = ensureConnection().getCurrentAoiActions();
    // Remove all Add Aoi actions that need no action as they are already present locally
    context().safeAction( generateName( "removeUnneededAddRequests" ), () -> {
      currentAOIActions.removeIf( a -> {
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

    if ( 0 == currentAOIActions.size() )
    {
      completeAoiAction();
      return true;
    }

    final Consumer<SafeProcedure> completionAction = a ->
    {
      LOG.info( () -> "Subscription to " + label( currentAOIActions ) + " completed." );
      completeAoiAction();
      a.call();
    };
    final Consumer<SafeProcedure> failAction = a ->
    {
      LOG.info( () -> "Subscription to " + label( currentAOIActions ) + " failed." );
      completeAoiAction();
      a.call();
    };

    final AreaOfInterestEntry aoiEntry = currentAOIActions.get( 0 );
    if ( currentAOIActions.size() == 1 )
    {
      final String cacheKey = aoiEntry.getCacheKey();
      final CacheEntry cacheEntry = _cacheService.lookup( cacheKey );
      final String eTag;
      final Consumer<SafeProcedure> cacheAction;
      if ( null != cacheEntry )
      {
        eTag = cacheEntry.getETag();
        LOG.info( () -> "Found locally cached data for channel " + label( aoiEntry ) + " with etag " + eTag + "." );
        cacheAction = a ->
        {
          LOG.info( () -> "Loading cached data for channel " + label( aoiEntry ) + " with etag " + eTag );
          final SafeProcedure completeAoiAction = () ->
          {
            LOG.info( () -> "Completed load of cached data for channel " + label( aoiEntry ) +
                            " with etag " + eTag + "." );
            completeAoiAction();
            a.call();
          };
          ensureConnection().enqueueOOB( cacheEntry.getContent(), completeAoiAction );
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
        currentAOIActions.stream().map( AreaOfInterestEntry::getAddress ).collect( Collectors.toList() );
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
    final List<AreaOfInterestEntry> currentAOIActions = ensureConnection().getCurrentAoiActions();
    currentAOIActions.forEach( AreaOfInterestEntry::markAsComplete );
    currentAOIActions.clear();
  }

  protected abstract void requestSubscribeToChannel( @Nonnull ChannelAddress descriptor,
                                                     @Nullable Object filterParameter,
                                                     @Nullable String cacheKey,
                                                     @Nullable String eTag,
                                                     @Nullable Consumer<SafeProcedure> cacheAction,
                                                     @Nonnull Consumer<SafeProcedure> completionAction,
                                                     @Nonnull Consumer<SafeProcedure> failAction );

  protected abstract void requestUnsubscribeFromChannel( @Nonnull ChannelAddress descriptor,
                                                         @Nonnull Consumer<SafeProcedure> completionAction,
                                                         @Nonnull Consumer<SafeProcedure> failAction );

  protected abstract void requestUpdateSubscription( @Nonnull ChannelAddress descriptor,
                                                     @Nonnull Object filterParameter,
                                                     @Nonnull Consumer<SafeProcedure> completionAction,
                                                     @Nonnull Consumer<SafeProcedure> failAction );

  protected abstract void requestBulkSubscribeToChannel( @Nonnull List<ChannelAddress> descriptor,
                                                         @Nullable Object filterParameter,
                                                         @Nonnull Consumer<SafeProcedure> completionAction,
                                                         @Nonnull Consumer<SafeProcedure> failAction );

  protected abstract void requestBulkUnsubscribeFromChannel( @Nonnull List<ChannelAddress> descriptors,
                                                             @Nonnull Consumer<SafeProcedure> completionAction,
                                                             @Nonnull Consumer<SafeProcedure> failAction );

  protected abstract void requestBulkUpdateSubscription( @Nonnull List<ChannelAddress> descriptors,
                                                         @Nonnull Object filterParameter,
                                                         @Nonnull Consumer<SafeProcedure> completionAction,
                                                         @Nonnull Consumer<SafeProcedure> failAction );

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isAreaOfInterestActionPending( @Nonnull final AreaOfInterestAction action,
                                                @Nonnull final ChannelAddress address,
                                                @Nullable final Object filter )
  {
    final Connection connection = getConnection();
    final List<AreaOfInterestEntry> currentAOIActions = null == connection ? null : connection.getCurrentAoiActions();
    return
      null != currentAOIActions &&
      currentAOIActions.stream().anyMatch( a -> a.match( action, address, filter ) ) ||
      (
        null != connection &&
        connection.getPendingAreaOfInterestActions().stream().
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
    final Connection connection = getConnection();
    final List<AreaOfInterestEntry> currentAOIActions = null == connection ? null : connection.getCurrentAoiActions();
    if ( null != currentAOIActions && currentAOIActions.stream().anyMatch( a -> a.match( action, address, filter ) ) )
    {
      return 0;
    }
    else if ( null != connection )
    {
      final LinkedList<AreaOfInterestEntry> actions = connection.getPendingAreaOfInterestActions();
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

  protected boolean progressDataLoad()
  {
    final Connection connection = ensureConnection();
    // Step: Retrieve any out of band actions
    final LinkedList<DataLoadAction> oobActions = connection.getOobActions();
    final DataLoadAction currentAction = connection.getCurrentAction();
    if ( null == currentAction && !oobActions.isEmpty() )
    {
      connection.setCurrentAction( oobActions.removeFirst() );
      return true;
    }

    //Step: Retrieve the action from the parsed queue if it is the next in the sequence
    final LinkedList<DataLoadAction> parsedActions = connection.getParsedActions();
    if ( null == currentAction && !parsedActions.isEmpty() )
    {
      final DataLoadAction action = parsedActions.get( 0 );
      final ChangeSet changeSet = action.getChangeSet();
      if ( action.isOob() || connection.getLastRxSequence() + 1 == changeSet.getSequence() )
      {
        final DataLoadAction candidate = parsedActions.remove();
        connection.setCurrentAction( candidate );
        if ( LOG.isLoggable( getLogLevel() ) )
        {
          LOG.log( getLogLevel(), "Parsed Action Selected: " + candidate );
        }
        return true;
      }
    }

    // Abort if there is no pending data load actions to take
    final LinkedList<DataLoadAction> pendingActions = connection.getPendingActions();
    if ( null == currentAction && pendingActions.isEmpty() )
    {
      if ( LOG.isLoggable( getLogLevel() ) )
      {
        LOG.log( getLogLevel(), "No data to load. Terminating incremental load process." );
      }
      onTerminatingIncrementalDataLoadProcess();
      return false;
    }

    //Step: Retrieve the action from the un-parsed queue
    if ( null == currentAction )
    {
      final DataLoadAction candidate = pendingActions.remove();
      connection.setCurrentAction( candidate );
      if ( LOG.isLoggable( getLogLevel() ) )
      {
        LOG.log( getLogLevel(), "Un-parsed Action Selected: " + candidate );
      }
      return true;
    }

    //Step: Parse the json
    final String rawJsonData = currentAction.getRawJsonData();
    boolean isOutOfBandMessage = currentAction.isOob();
    if ( null != rawJsonData )
    {
      if ( LOG.isLoggable( getLogLevel() ) )
      {
        LOG.log( getLogLevel(), "Parsing JSON: " + currentAction );
      }
      final ChangeSet changeSet = parseChangeSet( rawJsonData );
      if ( Replicant.shouldValidateChangeSetOnRead() )
      {
        changeSet.validate();
      }

      // OOB messages are not in response to requests as such
      final String requestID = isOutOfBandMessage ? null : changeSet.getRequestId();
      // OOB messages have no etags as from local cache or generated locally
      final String eTag = isOutOfBandMessage ? null : changeSet.getETag();
      final int sequence = isOutOfBandMessage ? 0 : changeSet.getSequence();
      if ( LOG.isLoggable( getLogLevel() ) )
      {
        LOG.log( getLogLevel(),
                 "Parsed ChangeSet:" +
                 " oob=" + isOutOfBandMessage +
                 " seq=" + sequence +
                 " requestID=" + requestID +
                 " eTag=" + eTag +
                 " changeCount=" + ( changeSet.hasEntityChanges() ? changeSet.getEntityChanges().length : 0 )
        );
      }
      final RequestEntry request;
      if ( isOutOfBandMessage )
      {
        request = null;
      }
      else
      {
        request = null != requestID ? connection.getRequest( requestID ) : null;
        if ( null == request && null != requestID )
        {
          final String message =
            "Unable to locate requestID '" + requestID + "' specified for ChangeSet: seq=" + sequence +
            " Existing Requests: " + connection.getRequests();
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

      currentAction.recordChangeSet( changeSet, request );
      parsedActions.add( currentAction );
      Collections.sort( parsedActions );
      connection.setCurrentAction( null );
      return true;
    }

    if ( currentAction.needsChannelActionsProcessed() )
    {
      context().safeAction( generateName( "processChannelActions" ), this::processChannelActions );
      return true;
    }

    //Step: Process a chunk of changes
    if ( currentAction.areChangesPending() )
    {
      if ( LOG.isLoggable( getLogLevel() ) )
      {
        LOG.log( getLogLevel(), "Processing ChangeSet: " + currentAction );
      }

      final ChangeSet changeSet = currentAction.getChangeSet();
      context().safeAction( generateName( "applyChange" ), () -> {
        EntityChange change;
        for ( int i = 0; i < _changesToProcessPerTick && null != ( change = currentAction.nextChange() ); i++ )
        {
          final Object entity = getChangeMapper().applyChange( change );
          if ( change.isUpdate() )
          {
            currentAction.incEntityUpdateCount();
          }
          else
          {
            currentAction.incEntityRemoveCount();
          }
          currentAction.changeProcessed( change.isUpdate(), entity );
        }
      }, changeSet.getSequence(), changeSet.getRequestId() );
      return true;
    }

    //Step: Process a chunk of links
    if ( currentAction.areEntityLinksPending() )
    {
      processEntityLinks( currentAction );
      return true;
    }

    final ChangeSet changeSet = currentAction.getChangeSet();

    //Step: Finalize the change set
    if ( !currentAction.hasWorldBeenNotified() )
    {
      currentAction.markWorldAsNotified();
      if ( LOG.isLoggable( getLogLevel() ) )
      {
        LOG.log( getLogLevel(), "Finalizing action: " + currentAction );
      }
      // OOB messages are not sequenced
      if ( !isOutOfBandMessage )
      {
        connection.setLastRxSequence( changeSet.getSequence() );
      }
      if ( Replicant.shouldValidateRepositoryOnLoad() )
      {
        // This should never need a transaction ... unless the repository is invalid and there is unlinked data.
        context().safeAction( generateName( "validate" ), this::validateRepository );
      }

      return true;
    }
    final DataLoadStatus status = currentAction.toStatus();
    if ( LOG.isLoggable( Level.INFO ) )
    {
      LOG.info( status.toString() );
    }

    //Step: Run the post actions
    if ( LOG.isLoggable( getLogLevel() ) )
    {
      LOG.log( getLogLevel(), "Running post action and cleaning action: " + currentAction );
    }
    final RequestEntry request = currentAction.getRequest();
    if ( null != request )
    {
      request.markResultsAsArrived();
    }
    final SafeProcedure runnable = currentAction.getCompletionAction();
    if ( null != runnable )
    {
      runnable.call();
      // OOB messages are not in response to requests as such
      final String requestId = isOutOfBandMessage ? null : currentAction.getChangeSet().getRequestId();
      if ( null != requestId )
      {
        // We can remove the request because this side ran second and the
        // RPC channel has already returned.

        final boolean removed = connection.removeRequest( requestId );
        if ( !removed )
        {
          LOG.severe( "ChangeSet " + changeSet.getSequence() + " expected to complete request '" +
                      requestId + "' but no request was registered with connection." );
        }
        if ( requestDebugOutputEnabled() )
        {
          outputRequestDebug();
        }
      }
    }
    connection.setCurrentAction( null );
    onMessageProcessed( status );
    if ( null != _resetAction )
    {
      _resetAction.run();
      _resetAction = null;
    }
    return true;
  }

  @Action( reportParameters = false )
  protected void processEntityLinks( @Nonnull final DataLoadAction currentAction )
  {
    Linkable linkable;
    for ( int i = 0; i < _linksToProcessPerTick && null != ( linkable = currentAction.nextEntityToLink() ); i++ )
    {
      linkable.link();
      currentAction.incEntityLinkCount();
    }
  }

  private void processChannelActions()
  {
    final DataLoadAction currentAction = ensureConnection().getCurrentAction();
    assert null != currentAction;
    currentAction.markChannelActionsProcessed();
    final ChangeSet changeSet = currentAction.getChangeSet();
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
        currentAction.incChannelAddCount();
        boolean explicitSubscribe = false;
        if ( ensureConnection().getCurrentAoiActions()
          .stream().anyMatch( a -> a.isInProgress() && a.getAddress().equals( address ) ) )
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
        currentAction.incChannelRemoveCount();
      }
      else if ( ChannelChange.Action.UPDATE == actionType )
      {
        final Subscription subscription = getReplicantContext().findSubscription( address );
        assert null != subscription;
        subscription.setFilter( filter );
        updateSubscriptionForFilteredEntities( subscription, filter );
        currentAction.incChannelUpdateCount();
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
    getRequestDebugger().outputRequests( getKey() + ":", ensureConnection() );
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
