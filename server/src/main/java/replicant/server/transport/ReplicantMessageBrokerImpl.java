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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
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
import replicant.server.runtime.ReplicantSystem;

@ApplicationScoped
@Transactional( Transactional.TxType.NOT_SUPPORTED )
@Typed( ReplicantMessageBroker.class )
public class ReplicantMessageBrokerImpl
  implements ReplicantMessageBroker
{
  @NonNull
  private static final Logger LOG = Logger.getLogger( ReplicantMessageBrokerImpl.class.getName() );
  private static final long RETRY_DELAY = 20L;
  @NonNull
  private final BlockingQueue<ReplicantSession> _queue = new LinkedBlockingQueue<>();
  @NonNull
  private final ConcurrentHashMap<String, WorkState> _workStates = new ConcurrentHashMap<>();
  @NonNull
  private final AtomicInteger _activeDrainTasks = new AtomicInteger();
  @NonNull
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
    if ( LOG.isLoggable( Level.INFO ) )
    {
      LOG.log( Level.INFO,
               "event=broker.stop queueSize=" + _queue.size() +
               " activeDrainTasks=" + _activeDrainTasks.get() +
               " workStateCount=" + _workStates.size() +
               " retryScheduled=" + _retryScheduled.get() );
    }
  }

  @NonNull
  @Override
  public Packet queueChangeMessage( @NonNull final ReplicantSession session,
                                    final boolean altersExplicitSubscriptions,
                                    @Nullable final Integer requestId,
                                    @Nullable final JsonValue response,
                                    @Nullable final String etag,
                                    @NonNull final Collection<EntityMessage> messages,
                                    @NonNull final ChangeSet changeSet )
  {
    final var packet = new Packet( altersExplicitSubscriptions, requestId, response, etag, messages, changeSet );
    session.queuePacket( packet );
    final var newlyQueued = enqueueSessionIfRequired( session );
    scheduleDrainTasks();
    if ( LOG.isLoggable( Level.FINE ) )
    {
      LOG.log( Level.FINE,
               "event=broker.packet.queue sessionId=" + session.getId() +
               " requestId=" + requestId +
               " messageCount=" + messages.size() +
               " changeCount=" + changeSet.getChanges().size() +
               " channelActionCount=" + changeSet.getChannelActions().size() +
               " altersExplicitSubscriptions=" + altersExplicitSubscriptions +
               " newlyQueued=" + newlyQueued +
               " queueSize=" + _queue.size() +
               " workStateCount=" + _workStates.size() +
               " activeDrainTasks=" + _activeDrainTasks.get() );
    }
    return packet;
  }

  private boolean enqueueSessionIfRequired( @NonNull final ReplicantSession session )
  {
    if ( null == _workStates.putIfAbsent( session.getId(), WorkState.QUEUED ) )
    {
      _queue.add( session );
      return true;
    }
    return false;
  }

  private void scheduleDrainTasks()
  {
    if ( !_stopping )
    {
      final var initialActiveDrainTasks = _activeDrainTasks.get();
      var submittedDrainTasks = 0;
      while ( !_queue.isEmpty() )
      {
        if ( reserveDrainTask() )
        {
          try
          {
            submitDrainTask( this::runDrainTask );
            submittedDrainTasks++;
          }
          catch ( final RuntimeException e )
          {
            _activeDrainTasks.decrementAndGet();
            if ( LOG.isLoggable( Level.SEVERE ) )
            {
              LOG.log( Level.SEVERE,
                       "event=broker.drain.submit.failed queueSize=" + _queue.size() +
                       " activeDrainTasks=" + _activeDrainTasks.get() +
                       " submittedDrainTasks=" + submittedDrainTasks +
                       " retryRequested=true",
                       e );
            }
            scheduleDelayedRetry();
            return;
          }
        }
        else
        {
          if ( LOG.isLoggable( Level.FINE ) )
          {
            LOG.log( Level.FINE,
                     "event=broker.drain.schedule submittedDrainTasks=" + submittedDrainTasks +
                     " initialActiveDrainTasks=" + initialActiveDrainTasks +
                     " activeDrainTasks=" + _activeDrainTasks.get() +
                     " queueSize=" + _queue.size() +
                     " workStateCount=" + _workStates.size() +
                     " maxConcurrentDrainTasks=" + _maxConcurrentDrainTasks );
          }
          return;
        }
      }
      if ( LOG.isLoggable( Level.FINE ) && submittedDrainTasks > 0 )
      {
        LOG.log( Level.FINE,
                 "event=broker.drain.schedule submittedDrainTasks=" + submittedDrainTasks +
                 " initialActiveDrainTasks=" + initialActiveDrainTasks +
                 " activeDrainTasks=" + _activeDrainTasks.get() +
                 " queueSize=" + _queue.size() +
                 " workStateCount=" + _workStates.size() +
                 " maxConcurrentDrainTasks=" + _maxConcurrentDrainTasks );
      }
    }
    else
    {
      if ( LOG.isLoggable( Level.INFO ) )
      {
        LOG.log( Level.INFO,
                 "event=broker.drain.schedule.skip reason=stopping queueSize=" + _queue.size() +
                 " activeDrainTasks=" + _activeDrainTasks.get() +
                 " workStateCount=" + _workStates.size() );
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
    final var start = System.nanoTime();
    var madeProgress = false;
    var retryRequested = false;
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
          retryRequested = true;
          scheduleDelayedRetry();
        }
      }
      if ( LOG.isLoggable( Level.FINE ) )
      {
        final var durationMs = ( System.nanoTime() - start ) / 1000000L;
        LOG.log( Level.FINE,
                 "event=broker.drain.run durationMs=" + durationMs +
                 " madeProgress=" + madeProgress +
                 " retryRequested=" + retryRequested +
                 " queueSize=" + _queue.size() +
                 " activeDrainTasks=" + _activeDrainTasks.get() +
                 " workStateCount=" + _workStates.size() +
                 " retryScheduled=" + _retryScheduled.get() );
      }
    }
  }

  @SuppressWarnings( { "deprecation", "RedundantSuppression" } )
  private boolean drainQueuedSessions()
  {
    var sessionsPolled = 0;
    var sessionsProcessed = 0;
    var duplicateSessionsSkipped = 0;
    var sessionsSkipped = 0;
    var madeProgress = false;
    final var processedSessionIds = new HashSet<String>();
    for ( var i = 0; i < _maxSessionsPerDrainTask; i++ )
    {
      final var session = _queue.poll();
      if ( null == session )
      {
        break;
      }
      else
      {
        sessionsPolled++;
        if ( !processedSessionIds.add( session.getId() ) )
        {
          duplicateSessionsSkipped++;
          _queue.add( session );
          break;
        }
        else if ( processPendingSession( session ) )
        {
          sessionsProcessed++;
          madeProgress = true;
        }
        else
        {
          sessionsSkipped++;
        }
      }
    }
    if ( LOG.isLoggable( Level.FINE ) )
    {
      LOG.log( Level.FINE,
               "event=broker.drain.sessions threadId=" + Thread.currentThread().getId() +
               " sessionsPolled=" + sessionsPolled +
               " sessionsProcessed=" + sessionsProcessed +
               " duplicateSessionsSkipped=" + duplicateSessionsSkipped +
               " sessionsSkipped=" + sessionsSkipped +
               " queueSize=" + _queue.size() +
               " maxSessionsPerDrainTask=" + _maxSessionsPerDrainTask );
    }
    return madeProgress;
  }

  private boolean processPendingSession( @NonNull final ReplicantSession session )
  {
    final var id = session.getId();
    if ( LOG.isLoggable( Level.FINEST ) )
    {
      LOG.log( Level.FINEST, "event=broker.session.process.start sessionId=" + id );
    }
    if ( !_workStates.replace( id, WorkState.QUEUED, WorkState.RUNNING ) )
    {
      if ( LOG.isLoggable( Level.FINEST ) )
      {
        LOG.log( Level.FINEST,
                 "event=broker.session.process.skip reason=workStateMismatch sessionId=" + id +
                 " queueSize=" + _queue.size() +
                 " workStateCount=" + _workStates.size() );
      }
      return false;
    }
    if ( !session.isOpen() )
    {
      _workStates.remove( id, WorkState.RUNNING );
      if ( LOG.isLoggable( Level.FINEST ) )
      {
        LOG.log( Level.FINEST, "event=broker.session.process.skip reason=sessionClosed sessionId=" + id );
      }
      return true;
    }
    final var lock = session.getLock();
    if ( !lock.tryLock() )
    {
      requeueRunningSession( session );
      if ( LOG.isLoggable( Level.FINEST ) )
      {
        LOG.log( Level.FINEST,
                 "event=broker.session.process.skip reason=lockContention sessionId=" + id +
                 " requeued=true queueSize=" + _queue.size() );
      }
      return false;
    }
    var processedPacket = false;
    var closeSession = false;
    Packet currentPacket = null;
    try
    {
      var packetsProcessed = 0;
      var emptyPacketsSkipped = 0;
      for ( var i = 0; i < _maxPacketsPerRun; i++ )
      {
        final var packet = session.popPendingPacket();
        currentPacket = packet;
        if ( null == packet )
        {
          break;
        }
        else
        {
          processedPacket = true;
          if ( _sessionManager.sendChangeMessage( session, packet ) )
          {
            packetsProcessed++;
          }
          else
          {
            emptyPacketsSkipped++;
          }
          if ( !session.isOpen() )
          {
            closeSession = true;
            break;
          }
        }
      }
      if ( LOG.isLoggable( Level.FINEST ) )
      {
        LOG.log( Level.FINEST,
                 "event=broker.session.process.complete sessionId=" + id +
                 " packetsProcessed=" + packetsProcessed +
                 " emptyPacketsSkipped=" + emptyPacketsSkipped +
                 " closeSession=" + closeSession +
                 " hasPendingPackets=" + session.hasPendingPackets() );
      }
    }
    catch ( final Throwable t )
    {
      if ( LOG.isLoggable( Level.SEVERE ) )
      {
        LOG.log( Level.SEVERE,
                 "event=broker.packet.process.failed sessionId=" + id + " " + describePacket( currentPacket ),
                 t );
      }
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
        if ( LOG.isLoggable( Level.FINEST ) )
        {
          LOG.log( Level.FINEST,
                   "event=broker.session.requeue reason=pendingPackets sessionId=" + id +
                   " queueSize=" + _queue.size() );
        }
        enqueueSessionIfRequired( session );
      }
      return processedPacket;
    }
  }

  private void requeueRunningSession( @NonNull final ReplicantSession session )
  {
    if ( _workStates.replace( session.getId(), WorkState.RUNNING, WorkState.QUEUED ) )
    {
      _queue.add( session );
      if ( LOG.isLoggable( Level.FINEST ) )
      {
        LOG.log( Level.FINEST,
                 "event=broker.session.requeue reason=lockContention sessionId=" + session.getId() +
                 " queueSize=" + _queue.size() );
      }
    }
  }

  private void scheduleDelayedRetry()
  {
    if ( _stopping )
    {
      if ( LOG.isLoggable( Level.FINE ) )
      {
        LOG.log( Level.FINE,
                 "event=broker.retry.schedule.skip reason=stopping queueSize=" + _queue.size() +
                 " activeDrainTasks=" + _activeDrainTasks.get() );
      }
      return;
    }
    if ( _queue.isEmpty() )
    {
      if ( LOG.isLoggable( Level.FINE ) )
      {
        LOG.log( Level.FINE,
                 "event=broker.retry.schedule.skip reason=emptyQueue activeDrainTasks=" +
                 _activeDrainTasks.get() );
      }
      return;
    }
    if ( !_retryScheduled.compareAndSet( false, true ) )
    {
      if ( LOG.isLoggable( Level.FINE ) )
      {
        LOG.log( Level.FINE,
                 "event=broker.retry.schedule.skip reason=alreadyScheduled queueSize=" + _queue.size() +
                 " activeDrainTasks=" + _activeDrainTasks.get() );
      }
      return;
    }
    try
    {
      scheduleRetryTask( this::runDelayedRetry );
      if ( LOG.isLoggable( Level.FINE ) )
      {
        LOG.log( Level.FINE,
                 "event=broker.retry.schedule delayMs=" + RETRY_DELAY +
                 " queueSize=" + _queue.size() +
                 " activeDrainTasks=" + _activeDrainTasks.get() +
                 " workStateCount=" + _workStates.size() );
      }
    }
    catch ( final RuntimeException e )
    {
      _retryScheduled.set( false );
      if ( LOG.isLoggable( Level.SEVERE ) )
      {
        LOG.log( Level.SEVERE,
                 "event=broker.retry.schedule.failed queueSize=" + _queue.size() +
                 " activeDrainTasks=" + _activeDrainTasks.get(),
                 e );
      }
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

  @NonNull
  private String describePacket( @Nullable final Packet packet )
  {
    if ( null == packet )
    {
      return "packetPresent=false";
    }
    return "packetPresent=true requestId=" + packet.requestId() +
           " messageCount=" + packet.messages().size() +
           " changeCount=" + packet.changeSet().getChanges().size() +
           " channelActionCount=" + packet.changeSet().getChannelActions().size() +
           " altersExplicitSubscriptions=" + packet.altersExplicitSubscriptions();
  }

  @VisibleForTesting
  void submitDrainTask( @NonNull final Runnable task )
  {
    _executorService.execute( task );
  }

  @VisibleForTesting
  void scheduleRetryTask( @NonNull final Runnable task )
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
