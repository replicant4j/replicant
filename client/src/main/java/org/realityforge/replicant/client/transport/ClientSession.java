package org.realityforge.replicant.client.transport;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.AreaOfInterestAction;
import replicant.ChannelAddress;

/**
 * Client-side representation of session.
 * Includes a database of outstanding requests, a subscription database and a
 * collection of outstanding data actions to apply.
 */
public final class ClientSession
{
  private static final Logger LOG = Logger.getLogger( ClientSession.class.getName() );
  private static final Level LOG_LEVEL = Level.INFO;

  private final String _sessionID;
  private final Map<String, RequestEntry> _requests = new HashMap<>();
  private final Map<String, RequestEntry> _roRequests = Collections.unmodifiableMap( _requests );
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

  public ClientSession( @Nonnull final String sessionID )
  {
    _sessionID = Objects.requireNonNull( sessionID );
  }

  @Nonnull
  public String getSessionID()
  {
    return _sessionID;
  }

  void enqueueAoiAction( @Nonnull final ChannelAddress descriptor,
                         @Nonnull final AreaOfInterestAction action,
                         @Nullable final Object filterParameter )
  {
    _pendingAreaOfInterestActions.add( new AreaOfInterestEntry( descriptor, action, filterParameter ) );
  }

  void enqueueDataLoad( @Nonnull final String rawJsonData )
  {
    getPendingActions().add( new DataLoadAction( Objects.requireNonNull( rawJsonData ), false ) );
  }

  void enqueueOOB( @Nonnull final String rawJsonData, @Nullable final Runnable runnable )
  {
    final DataLoadAction action = new DataLoadAction( Objects.requireNonNull( rawJsonData ), true );
    action.setRunnable( runnable );
    getOobActions().add( action );
  }

  LinkedList<AreaOfInterestEntry> getPendingAreaOfInterestActions()
  {
    return _pendingAreaOfInterestActions;
  }

  LinkedList<DataLoadAction> getPendingActions()
  {
    return _pendingActions;
  }

  LinkedList<DataLoadAction> getParsedActions()
  {
    return _parsedActions;
  }

  LinkedList<DataLoadAction> getOobActions()
  {
    return _oobActions;
  }

  int getLastRxSequence()
  {
    return _lastRxSequence;
  }

  void setLastRxSequence( final int lastRxSequence )
  {
    _lastRxSequence = lastRxSequence;
  }

  @Nonnull
  public final RequestEntry newRequest( @Nullable final String requestKey, @Nullable final String cacheKey )
  {
    final RequestEntry entry = new RequestEntry( newRequestID(), requestKey, cacheKey );
    _requests.put( entry.getRequestID(), entry );
    if ( LOG.isLoggable( LOG_LEVEL ) )
    {
      LOG.log( LOG_LEVEL, "Created request " + entry );
    }
    return entry;
  }

  public final void completeNormalRequest( @Nonnull final RequestEntry request,
                                           @Nonnull final Runnable runnable )
  {
    if ( request.isExpectingResults() && !request.haveResultsArrived() )
    {
      request.setNormalCompletionAction( runnable );
      if ( LOG.isLoggable( LOG_LEVEL ) )
      {
        LOG.log( LOG_LEVEL, "Request " + request + " completed normally. Change set has not arrived." );
      }
    }
    else
    {
      runnable.run();
      removeRequest( request.getRequestID() );
      if ( LOG.isLoggable( LOG_LEVEL ) )
      {
        LOG.log( LOG_LEVEL, "Request " + request + " completed normally. No change set or already arrived." );
      }
    }
  }

  public final void completeNonNormalRequest( @Nonnull final RequestEntry request,
                                              @Nonnull final Runnable runnable )
  {
    if ( request.isExpectingResults() && !request.haveResultsArrived() )
    {
      request.setNonNormalCompletionAction( runnable );
      if ( LOG.isLoggable( LOG_LEVEL ) )
      {
        LOG.log( LOG_LEVEL, "Request " + request + " completed with exception. Change set has not arrived." );
      }
    }
    else
    {
      runnable.run();
      removeRequest( request.getRequestID() );
      if ( LOG.isLoggable( LOG_LEVEL ) )
      {
        LOG.log( LOG_LEVEL, "Request " + request + " completed with exception. No change set or already arrived." );
      }
    }
  }

  @Nullable
  RequestEntry getRequest( @Nonnull final String requestID )
  {
    return _requests.get( requestID );
  }

  Map<String, RequestEntry> getRequests()
  {
    return _roRequests;
  }

  boolean removeRequest( @Nonnull final String requestID )
  {
    return null != _requests.remove( requestID );
  }

  private String newRequestID()
  {
    return String.valueOf( ++_requestID );
  }
}
