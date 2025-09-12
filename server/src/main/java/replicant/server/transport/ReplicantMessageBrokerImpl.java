package replicant.server.transport;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.JsonObject;
import javax.json.JsonValue;
import replicant.server.ChangeSet;
import replicant.server.EntityMessage;

public abstract class ReplicantMessageBrokerImpl
  implements ReplicantMessageBroker
{
  @Nonnull
  private static final Logger LOG = Logger.getLogger( ReplicantSessionManagerImpl.class.getName() );
  private static final long QUEUE_TIMEOUT = 10L;
  @Nonnull
  private final BlockingQueue<ReplicantSession> _queue = new LinkedBlockingDeque<>();
  /**
   * In progress is used because a single session with a long running load can queue up other requests
   * and will eventually consume up all the active processors. If we guarantee that there is at most
   * one thread per session then we can allow sessions to keep making progress.
   */
  @Nonnull
  private final ConcurrentHashMap<String, ReplicantSession> _inProgress = new ConcurrentHashMap<>();

  @Nonnull
  protected abstract ReplicantSessionManager getReplicantSessionManager();

  @Override
  public void queueChangeMessage( @Nonnull final ReplicantSession session,
                                  final boolean altersExplicitSubscriptions,
                                  @Nullable final Integer requestId,
                                  @Nullable final JsonValue response,
                                  @Nullable final String etag,
                                  @Nonnull final Collection<EntityMessage> messages,
                                  @Nonnull final ChangeSet changeSet )
  {
    session.queuePacket( new Packet( altersExplicitSubscriptions, requestId, response, etag, messages, changeSet ) );
    _queue.add( session );
  }

  @Override
  public void processPendingSessions()
  {
    try
    {
      ReplicantSession session;
      while ( null != ( session = _queue.poll( QUEUE_TIMEOUT, TimeUnit.MILLISECONDS ) ) )
      {
        processPendingSession( session );
      }
    }
    catch ( final InterruptedException ignored )
    {
    }
  }

  private void processPendingSession( @Nonnull final ReplicantSession session )
  {
    final String id = session.getId();
    LOG.log( Level.FINEST, () -> "Processing pending ChangeSets for session " + session.getId() );
    if ( session.isOpen() && !_inProgress.containsKey( id ) )
    {
      final ReentrantLock lock = session.getLock();
      try
      {
        lock.lockInterruptibly();
        _inProgress.put( id, session );
        Packet packet;
        while ( null != ( packet = session.popPendingPacket() ) )
        {
          getReplicantSessionManager()
            .sendChangeMessage( session,
                                packet.requestId(),
                                packet.response(),
                                packet.etag(),
                                packet.messages(),
                                packet.changeSet() );
        }
      }
      catch ( final InterruptedException ignored )
      {
        LOG.log( Level.FINEST, () -> "Error completing send of packet " + session.getId() );

        session.closeDueToInterrupt();
      }
      finally
      {
        //noinspection resource
        _inProgress.remove( id );
        lock.unlock();
      }
    }
  }
}
