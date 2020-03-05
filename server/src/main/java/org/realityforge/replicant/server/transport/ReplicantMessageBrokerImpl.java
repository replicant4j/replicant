package org.realityforge.replicant.server.transport;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.EntityMessage;

public abstract class ReplicantMessageBrokerImpl
  implements ReplicantMessageBroker
{
  @Nonnull
  private static final Logger LOG = Logger.getLogger( ReplicantSessionManagerImpl.class.getName() );
  private static final long QUEUE_TIMEOUT = 10L;
  @Nonnull
  private final Lock _lock = new ReentrantLock();
  @Nonnull
  private final BlockingQueue<ReplicantSession> _queue = new LinkedBlockingDeque<>();

  @Nonnull
  protected abstract ReplicantSessionManager getReplicantSessionManager();

  @Override
  public void queueChangeMessage( @Nonnull final ReplicantSession session,
                                  final boolean altersExplicitSubscriptions,
                                  @Nullable final Integer requestId,
                                  @Nullable final String etag,
                                  @Nonnull final Collection<EntityMessage> messages,
                                  @Nonnull final ChangeSet changeSet )
  {
    session.queuePacket( new Packet( altersExplicitSubscriptions, requestId, etag, messages, changeSet ) );
    _queue.add( session );
  }

  @Override
  public void processPendingSessions()
  {
    if ( _lock.tryLock() )
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
      finally
      {
        _lock.unlock();
      }
    }
  }

  private void processPendingSession( @Nonnull final ReplicantSession session )
  {
    LOG.log( Level.FINEST, () -> "Processing pending ChangeSets for session " + session.getId() );
    if ( session.isOpen() )
    {
      final ReentrantLock lock = session.getLock();
      try
      {
        lock.lockInterruptibly();
        Packet packet;
        while ( null != ( packet = session.popPendingPacket() ) )
        {
          getReplicantSessionManager()
            .sendChangeMessage( session,
                                packet.getRequestId(),
                                packet.getEtag(),
                                packet.getMessages(),
                                packet.getChangeSet() );
        }
      }
      catch ( final InterruptedException ignored )
      {
        LOG.log( Level.FINEST, () -> "Error completing send of packet " + session.getId() );

        session.closeDueToInterrupt();
      }
      finally
      {
        lock.unlock();
      }
    }
  }
}
