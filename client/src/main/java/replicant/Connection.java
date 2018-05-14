package replicant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Connection state used by the connector to manage connection to backend system.
 * This includes a list of pending requests, pending messages that needs to be applied
 * to the local state etc.
 */
public final class Connection
{
  //TODO: Make this package access after all classes migrated to replicant package

  private static final Logger LOG = Logger.getLogger( Connection.class.getName() );
  private static final Level LOG_LEVEL = Level.INFO;

  private final String _connectionId;
  private final Map<String, RequestEntry> _requests = new HashMap<>();
  private int _requestID;

  /**
   * Pending actions that will change the area of interest.
   */
  private final LinkedList<AreaOfInterestEntry> _pendingAreaOfInterestActions = new LinkedList<>();
  /**
   * The set of data load actions that still need to have the json parsed.
   */
  private final LinkedList<DataLoadAction> _pendingActions = new LinkedList<>();
  /**
   * The set of data load actions that have their json parsed. They are inserted into
   * this list according to their sequence.
   */
  private final LinkedList<DataLoadAction> _parsedActions = new LinkedList<>();
  /**
   * Sometimes a data load action occurs that is not initiated by the server. These do not
   * typically need to be sequenced and are prioritized above other actions.
   */
  private final LinkedList<DataLoadAction> _oobActions = new LinkedList<>();

  private int _lastRxSequence;

  /**
   * The current message action being processed.
   */
  @Nullable
  private DataLoadAction _currentAction;
  @Nonnull
  private List<AreaOfInterestEntry> _currentAoiActions = new ArrayList<>();

  public Connection( @Nonnull final String connectionId )
  {
    _connectionId = Objects.requireNonNull( connectionId );
  }

  @Nonnull
  public String getConnectionId()
  {
    return _connectionId;
  }

  public void enqueueAoiAction( @Nonnull final ChannelAddress descriptor,
                                @Nonnull final AreaOfInterestAction action,
                                @Nullable final Object filterParameter )
  {
    getPendingAreaOfInterestActions().add( new AreaOfInterestEntry( descriptor, action, filterParameter ) );
  }

  public void enqueueDataLoad( @Nonnull final String rawJsonData )
  {
    getPendingActions().add( new DataLoadAction( rawJsonData ) );
  }

  public void enqueueOOB( @Nonnull final String rawJsonData, @Nonnull final SafeProcedure oobCompletionAction )
  {
    getOobActions().add( new DataLoadAction( rawJsonData, oobCompletionAction ) );
  }

  public LinkedList<AreaOfInterestEntry> getPendingAreaOfInterestActions()
  {
    return _pendingAreaOfInterestActions;
  }

  public LinkedList<DataLoadAction> getPendingActions()
  {
    return _pendingActions;
  }

  public LinkedList<DataLoadAction> getParsedActions()
  {
    return _parsedActions;
  }

  public LinkedList<DataLoadAction> getOobActions()
  {
    return _oobActions;
  }

  public int getLastRxSequence()
  {
    return _lastRxSequence;
  }

  public void setLastRxSequence( final int lastRxSequence )
  {
    _lastRxSequence = lastRxSequence;
  }

  @Nonnull
  public final RequestEntry newRequest( @Nullable final String name, @Nullable final String cacheKey )
  {
    final RequestEntry entry = new RequestEntry( nextRequestId(), name, cacheKey );
    _requests.put( entry.getRequestId(), entry );
    if ( LOG.isLoggable( LOG_LEVEL ) )
    {
      LOG.log( LOG_LEVEL, "Created request " + entry );
    }
    return entry;
  }

  public final void completeNormalRequest( @Nonnull final RequestEntry request,
                                           @Nonnull final SafeProcedure completionAction )
  {
    if ( request.isExpectingResults() && !request.haveResultsArrived() )
    {
      request.setNormalCompletionAction( completionAction );
      if ( LOG.isLoggable( LOG_LEVEL ) )
      {
        LOG.log( LOG_LEVEL, "Request " + request + " completed normally. Change set has not arrived." );
      }
    }
    else
    {
      completionAction.call();
      removeRequest( request.getRequestId() );
      if ( LOG.isLoggable( LOG_LEVEL ) )
      {
        LOG.log( LOG_LEVEL, "Request " + request + " completed normally. No change set or already arrived." );
      }
    }
  }

  public final void completeNonNormalRequest( @Nonnull final RequestEntry request,
                                              @Nonnull final SafeProcedure completionAction )
  {
    if ( request.isExpectingResults() && !request.haveResultsArrived() )
    {
      request.setNonNormalCompletionAction( completionAction );
      if ( LOG.isLoggable( LOG_LEVEL ) )
      {
        LOG.log( LOG_LEVEL, "Request " + request + " completed with exception. Change set has not arrived." );
      }
    }
    else
    {
      completionAction.call();
      removeRequest( request.getRequestId() );
      if ( LOG.isLoggable( LOG_LEVEL ) )
      {
        LOG.log( LOG_LEVEL, "Request " + request + " completed with exception. No change set or already arrived." );
      }
    }
  }

  @Nullable
  public RequestEntry getRequest( @Nonnull final String requestID )
  {
    return _requests.get( requestID );
  }

  public Map<String, RequestEntry> getRequests()
  {
    return _requests;
  }

  public boolean removeRequest( @Nonnull final String requestID )
  {
    return null != _requests.remove( requestID );
  }

  private String nextRequestId()
  {
    return String.valueOf( ++_requestID );
  }

  @Nullable
  public DataLoadAction getCurrentAction()
  {
    return _currentAction;
  }

  public void setCurrentAction( @Nullable final DataLoadAction currentAction )
  {
    _currentAction = currentAction;
  }

  @Nonnull
  public List<AreaOfInterestEntry> getCurrentAoiActions()
  {
    return _currentAoiActions;
  }
}
