package org.realityforge.replicant.client;

import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * Class from which to extend to implement a service that loads data from a change set.
 * Data can be loaded by bulk or incrementally and the load can be broken up into several
 * steps to avoid locking a thread such as in GWT.
 */
public abstract class AbstractDataLoaderService
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

  private int _lastKnownChangeSet = Integer.MIN_VALUE;

  private final LinkedList<DataLoadAction> _dataLoadActions = new LinkedList<>();
  private DataLoadAction _currentAction;
  private int _updateCount;
  private int _removeCount;
  private int _linkCount;
  private int _changesToProcessPerTick = DEFAULT_CHANGES_TO_PROCESS_PER_TICK;
  private int _linksToProcessPerTick = DEFAULT_LINKS_TO_PROCESS_PER_TICK;

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

  protected final void enqueueDataLoad( final boolean isBulkLoad,
                                        final String rawJsonData,
                                        @Nullable final Runnable runnable )
  {
    _dataLoadActions.add( new DataLoadAction( isBulkLoad, rawJsonData, runnable ) );
    scheduleDataLoad();
  }

  protected final boolean progressDataLoad()
  {
    if ( null == _currentAction && _dataLoadActions.isEmpty() )
    {
      if ( LOG.isLoggable( getLogLevel() ) )
      {
        LOG.log( getLogLevel(), "No data to load. Terminating incremental load process." );
      }
      return false;
    }

    //Step: Retrieve the action from the queue and notify the broker
    if ( null == _currentAction )
    {
      _currentAction = _dataLoadActions.remove();
      if ( LOG.isLoggable( getLogLevel() ) )
      {
        LOG.log( getLogLevel(), "Action Selected: " + _currentAction );
      }
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
      _currentAction.setChangeSet( changeSet );
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
      if ( _currentAction.isBulkLoad() )
      {
        _changeBroker.enable();
        onBulkLoadComplete();
        if ( shouldValidateOnLoad() )
        {
          validateRepository();
        }
      }
      else
      {
        _lastKnownChangeSet = set.getSequence();
        _changeBroker.resume();
        onIncrementalLoadComplete();
        if ( shouldValidateOnLoad() )
        {
          validateRepository();
        }
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
    _currentAction = null;
    return true;
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
