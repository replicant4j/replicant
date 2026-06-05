package replicant.server.transport;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.json.JsonValue;
import javax.transaction.Transactional;
import javax.websocket.CloseReason;
import org.jetbrains.annotations.VisibleForTesting;
import replicant.server.ChangeSet;
import replicant.server.EntityMessage;
import replicant.server.ee.ReplicantSystem;

@ApplicationScoped
@Transactional( Transactional.TxType.NOT_SUPPORTED )
@Typed( ReplicantMessageBroker.class )
public class ReplicantMessageBrokerImpl
  implements ReplicantMessageBroker
{
  @Nonnull
  private static final Logger LOG = Logger.getLogger( ReplicantMessageBrokerImpl.class.getName() );
  private static final long RETRY_DELAY = 20L;
  @Nonnull
  private final BlockingQueue<ReplicantSession> _queue = new LinkedBlockingQueue<>();
  @Nonnull
  private final ConcurrentHashMap<String, WorkState> _workStates = new ConcurrentHashMap<>();
  @Nonnull
  private final AtomicInteger _activeDrainTasks = new AtomicInteger();
  @Nonnull
  private final AtomicBoolean _retryScheduled = new AtomicBoolean();
  @VisibleForTesting
  @Inject
  ReplicantSessionManager _sessionManager;
  @Inject
  @ReplicantSystem( "ScheduledExecutorService" )
  private ScheduledExecutorService _scheduledExecutorService;
  @Inject
  @ReplicantSystem( "ExecutorService" )
  private ExecutorService _executorService;
  @Inject
  @ReplicantSystem( "broker/maxConcurrentDrainTasks" )
  private Integer _maxConcurrentDrainTasks;
  @Inject
  @ReplicantSystem( "broker/maxPacketsPerRun" )
  private Integer _maxPacketsPerRun;
  @Inject
  @ReplicantSystem( "broker/maxSessionsPerDrainTask" )
  private Integer _maxSessionsPerDrainTask;
  private volatile boolean _stopping;

  @PreDestroy
  void preDestroy()
  {
    _stopping = true;
  }

  @Nonnull
  @Override
  public Packet queueChangeMessage( @Nonnull final ReplicantSession session,
                                    final boolean altersExplicitSubscriptions,
                                    @Nullable final Integer requestId,
                                    @Nullable final JsonValue response,
                                    @Nullable final String etag,
                                    @Nonnull final Collection<EntityMessage> messages,
                                    @Nonnull final ChangeSet changeSet )
  {
    final var packet = new Packet( altersExplicitSubscriptions, requestId, response, etag, messages, changeSet );
    session.queuePacket( packet );
    enqueueSessionIfRequired( session );
    scheduleDrainTasks();
    if ( LOG.isLoggable( Level.FINEST ) )
    {
      LOG.log( Level.FINEST, () -> "queueChangeMessage() queue.size=" + _queue.size() + " packet=" + packet );
    }
    return packet;
  }

  private void enqueueSessionIfRequired( @Nonnull final ReplicantSession session )
  {
    if ( null == _workStates.putIfAbsent( session.getId(), WorkState.QUEUED ) )
    {
      _queue.add( session );
    }
  }

  private void scheduleDrainTasks()
  {
    if ( !_stopping )
    {
      while ( !_queue.isEmpty() )
      {
        if ( reserveDrainTask() )
        {
          try
          {
            submitDrainTask( this::runDrainTask );
          }
          catch ( final RuntimeException e )
          {
            _activeDrainTasks.decrementAndGet();
            LOG.log( Level.SEVERE,
                     e,
                     () -> "Unable to submit Replicant drain task. queue.size=" + _queue.size() +
                           " activeDrainTasks=" + _activeDrainTasks.get() );
            scheduleDelayedRetry();
            return;
          }
        }
        else
        {
          return;
        }
      }
    }
  }

  private boolean reserveDrainTask()
  {
    while ( true )
    {
      final var activeDrainTasks = _activeDrainTasks.get();
      if ( activeDrainTasks >= _maxConcurrentDrainTasks || activeDrainTasks >= _workStates.size() )
      {
        return false;
      }
      if ( _activeDrainTasks.compareAndSet( activeDrainTasks, activeDrainTasks + 1 ) )
      {
        return true;
      }
    }
  }

  private void runDrainTask()
  {
    var madeProgress = false;
    try
    {
      madeProgress = drainQueuedSessions();
    }
    catch ( final Throwable t )
    {
      LOG.log( Level.SEVERE, t, () -> "Error in Replicant drain task" );
    }
    finally
    {
      _activeDrainTasks.decrementAndGet();
      if ( !_stopping && !_queue.isEmpty() )
      {
        if ( madeProgress )
        {
          scheduleDrainTasks();
        }
        else
        {
          scheduleDelayedRetry();
        }
      }
    }
  }

  private boolean drainQueuedSessions()
  {
    var madeProgress = false;
    final var processedSessionIds = new HashSet<String>();
    for ( var i = 0; i < _maxSessionsPerDrainTask; i++ )
    {
      final var session = _queue.poll();
      if ( null == session )
      {
        break;
      }
      if ( !processedSessionIds.add( session.getId() ) )
      {
        _queue.add( session );
        break;
      }
      if ( processPendingSession( session ) )
      {
        madeProgress = true;
      }
    }
    return madeProgress;
  }

  private boolean processPendingSession( @Nonnull final ReplicantSession session )
  {
    final var id = session.getId();
    LOG.log( Level.FINEST, () -> "Processing pending ChangeSets for session " + id );
    if ( !_workStates.replace( id, WorkState.QUEUED, WorkState.RUNNING ) )
    {
      return false;
    }
    if ( !session.isOpen() )
    {
      _workStates.remove( id, WorkState.RUNNING );
      return true;
    }
    final var lock = session.getLock();
    if ( !lock.tryLock() )
    {
      requeueRunningSession( session );
      return false;
    }
    var processedPacket = false;
    var closeSession = false;
    try
    {
      for ( var i = 0; i < _maxPacketsPerRun; i++ )
      {
        final var packet = session.popPendingPacket();
        if ( null == packet )
        {
          break;
        }
        processedPacket = true;
        _sessionManager.sendChangeMessage( session, packet );
        if ( !session.isOpen() )
        {
          closeSession = true;
          break;
        }
      }
    }
    catch ( final Throwable t )
    {
      LOG.log( Level.SEVERE, t, () -> "Error completing send of packet for session " + id );
      session.close( new CloseReason( CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Packet processing failed" ) );
      closeSession = true;
    }
    finally
    {
      lock.unlock();
    }
    _workStates.remove( id, WorkState.RUNNING );
    if ( closeSession || !session.isOpen() )
    {
      return true;
    }
    else
    {
      if ( session.hasPendingPackets() )
      {
        enqueueSessionIfRequired( session );
      }
      return processedPacket;
    }
  }

  private void requeueRunningSession( @Nonnull final ReplicantSession session )
  {
    if ( _workStates.replace( session.getId(), WorkState.RUNNING, WorkState.QUEUED ) )
    {
      _queue.add( session );
    }
  }

  private void scheduleDelayedRetry()
  {
    if ( _stopping || _queue.isEmpty() || !_retryScheduled.compareAndSet( false, true ) )
    {
      return;
    }
    try
    {
      scheduleRetryTask( this::runDelayedRetry );
    }
    catch ( final RuntimeException e )
    {
      _retryScheduled.set( false );
      LOG.log( Level.SEVERE,
               e,
               () -> "Unable to schedule Replicant drain retry. queue.size=" + _queue.size() +
                     " activeDrainTasks=" + _activeDrainTasks.get() );
    }
  }

  @VisibleForTesting
  void runDelayedRetry()
  {
    _retryScheduled.set( false );
    if ( !_stopping && !_queue.isEmpty() )
    {
      scheduleDrainTasks();
    }
  }

  @VisibleForTesting
  void submitDrainTask( @Nonnull final Runnable task )
  {
    _executorService.execute( task );
  }

  @VisibleForTesting
  void scheduleRetryTask( @Nonnull final Runnable task )
  {
    _scheduledExecutorService.schedule( task, RETRY_DELAY, TimeUnit.MILLISECONDS );
  }

  @VisibleForTesting
  int getActiveDrainTaskCount()
  {
    return _activeDrainTasks.get();
  }

  @VisibleForTesting
  int getQueuedSessionCount()
  {
    return _queue.size();
  }

  @VisibleForTesting
  int getWorkStateCount()
  {
    return _workStates.size();
  }

  @VisibleForTesting
  boolean isRetryScheduled()
  {
    return _retryScheduled.get();
  }

  @VisibleForTesting
  int getMaxPacketsPerRun()
  {
    return _maxPacketsPerRun;
  }

  @VisibleForTesting
  void setMaxConcurrentDrainTasks( final int maxConcurrentDrainTasks )
  {
    _maxConcurrentDrainTasks = maxConcurrentDrainTasks;
  }

  @SuppressWarnings( "SameParameterValue" )
  @VisibleForTesting
  void setMaxPacketsPerRun( final int maxPacketsPerRun )
  {
    _maxPacketsPerRun = maxPacketsPerRun;
  }

  @SuppressWarnings( "SameParameterValue" )
  @VisibleForTesting
  void setMaxSessionsPerDrainTask( final int maxSessionsPerDrainTask )
  {
    _maxSessionsPerDrainTask = maxSessionsPerDrainTask;
  }

  private enum WorkState
  {
    QUEUED,
    RUNNING
  }
}
