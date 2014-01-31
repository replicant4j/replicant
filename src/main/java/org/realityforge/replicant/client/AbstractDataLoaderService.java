package org.realityforge.replicant.client;

import java.util.Collections;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.realityforge.replicant.client.transport.ClientSession;
import org.realityforge.replicant.client.transport.RequestEntry;
import org.realityforge.replicant.client.transport.RequestManager;

/**
 * Class from which to extend to implement a service that loads data from a change set.
 * Data can be loaded by bulk or incrementally and the load can be broken up into several
 * steps to avoid locking a thread such as in GWT.
 */
public abstract class AbstractDataLoaderService<T extends ClientSession>
{
  private static final int DEFAULT_CHANGES_TO_PROCESS_PER_TICK = 100;
  private static final int DEFAULT_LINKS_TO_PROCESS_PER_TICK = 100;
  protected static final Logger LOG = Logger.getLogger( AbstractDataLoaderService.class.getName() );
  @Inject
  private ChangeMapper _changeMapper;
  @Inject
  private EntityChangeBroker _changeBroker;
  @Inject
  private EntityRepository _repository;

  private int _lastKnownChangeSet;

  /**
   * The set of data load actions that still need to have the json parsed.
   */
  private final LinkedList<DataLoadAction> _pendingActions = new LinkedList<>();
  /**
   * The set of data load actions that have their json parsed. They are inserted into
   * this list according to their sequence.
   */
  private final LinkedList<DataLoadAction> _parsedActions = new LinkedList<>();
  private DataLoadAction _currentAction;
  private int _updateCount;
  private int _removeCount;
  private int _linkCount;
  private int _changesToProcessPerTick = DEFAULT_CHANGES_TO_PROCESS_PER_TICK;
  private int _linksToProcessPerTick = DEFAULT_LINKS_TO_PROCESS_PER_TICK;

  private T _session;

  protected void setSession( final T session )
  {
    _session = session;
  }

  protected final T getSession()
  {
    return _session;
  }

  protected void onBulkLoadComplete()
  {
  }

  protected void onIncrementalLoadComplete()
  {
  }

  protected abstract void scheduleDataLoad();

  protected final void setChangesToProcessPerTick( final int changesToProcessPerTick )
  {
    _changesToProcessPerTick = changesToProcessPerTick;
  }

  protected final void setLinksToProcessPerTick( final int linksToProcessPerTick )
  {
    _linksToProcessPerTick = linksToProcessPerTick;
  }

  protected final int getLastKnownChangeSet()
  {
    return _lastKnownChangeSet;
  }

  protected abstract ChangeSet parseChangeSet( String rawJsonData );

  @SuppressWarnings( "ConstantConditions" )
  protected final void enqueueDataLoad( @Nonnull final String rawJsonData )
  {
    if( null == rawJsonData )
    {
      throw new IllegalStateException( "null == rawJsonData" );
    }
    _pendingActions.add( new DataLoadAction( rawJsonData ) );
    scheduleDataLoad();
  }

  protected final boolean progressDataLoad()
  {
    //Step: Retrieve the action from the parsed queue if it is the next in the sequence
    if ( null == _currentAction && !_parsedActions.isEmpty() )
    {
      final ChangeSet changeSet = _parsedActions.get( 0 ).getChangeSet();
      assert null != changeSet;
      if ( _lastKnownChangeSet + 1 == changeSet.getSequence() )
      {
        _currentAction = _parsedActions.remove();
        if ( LOG.isLoggable( getLogLevel() ) )
        {
          LOG.log( getLogLevel(), "Parsed Action Selected: " + _currentAction );
        }
        return true;
      }
    }

    // Abort if there is no pending data load actions to take
    if ( null == _currentAction && _pendingActions.isEmpty() )
    {
      if ( LOG.isLoggable( getLogLevel() ) )
      {
        LOG.log( getLogLevel(), "No data to load. Terminating incremental load process." );
      }
      return false;
    }

    //Step: Retrieve the action from the un-parsed queue
    if ( null == _currentAction )
    {
      _currentAction = _pendingActions.remove();
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
      final RequestManager requestManager = getSession().getRequestManager();
      final RequestEntry request = requestManager.getRequest( changeSet.getRequestID() );
      _currentAction.setChangeSet( changeSet, request );
      _parsedActions.add( _currentAction );
      Collections.sort( _parsedActions );
      _currentAction = null;
      return true;
    }

    //Step: Setup the change recording state
    if ( _currentAction.needsBrokerPause() )
    {
      _currentAction.markBrokerPaused();
      if ( _currentAction.isBulkLoad() )
      {
        _changeBroker.disable();
      }
      else
      {
        _changeBroker.pause();
      }
      if ( LOG.isLoggable( Level.INFO ) )
      {
        _updateCount = 0;
        _removeCount = 0;
        _linkCount = 0;
      }
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
        final Object entity = _changeMapper.applyChange( change );
        if ( LOG.isLoggable( Level.INFO ) )
        {
          if ( change.isUpdate() )
          {
            _updateCount++;
          }
          else
          {
            _removeCount++;
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
          _linkCount++;
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
      _lastKnownChangeSet = set.getSequence();
      if ( _currentAction.isBulkLoad() )
      {
        if ( _currentAction.hasBrokerBeenPaused() )
        {
          _changeBroker.enable();
        }
        onBulkLoadComplete();
      }
      else
      {
        if ( _currentAction.hasBrokerBeenPaused() )
        {
          _changeBroker.resume();
        }
        onIncrementalLoadComplete();
      }
      if ( shouldValidateOnLoad() )
      {
        validateRepository();
      }
      return true;
    }
    if ( LOG.isLoggable( Level.INFO ) )
    {
      LOG.info( "ChangeSet " + set.getSequence() + " involved " + _updateCount + " updates, " + _removeCount +
                " removes and " + _linkCount + " links." );
    }

    //Step: Run the post actions
    if ( LOG.isLoggable( getLogLevel() ) )
    {
      LOG.log( getLogLevel(), "Running post action and cleaning action: " + _currentAction );
    }
    final Runnable runnable = _currentAction.getRunnable();
    if ( null != runnable )
    {
      runnable.run();
    }
    onDataLoadComplete( _currentAction.isBulkLoad(), set.getRequestID() );
    _currentAction = null;
    return true;
  }

  /**
   * Invoked when a change set has been completely processed.
   *
   * @param bulkLoad true if the change set was processed as a bulk load, false otherwise.
   * @param requestID the local request id that initiated the changes.
   */
  protected void onDataLoadComplete( final boolean bulkLoad, @Nullable final String requestID )
  {
  }

  protected Level getLogLevel()
  {
    return Level.FINEST;
  }

  protected final EntityRepository getRepository()
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
  protected final void validateRepository()
  {
    try
    {
      _repository.validate();
    }
    catch ( final Exception e )
    {
      throw new IllegalStateException( e.getMessage(), e );
    }
  }
}
