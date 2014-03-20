package org.realityforge.replicant.server.transport;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.replicant.server.Change;
import org.realityforge.replicant.server.EntityMessageEndpoint;
import org.realityforge.replicant.server.ee.ReplicantContextHolder;
import org.realityforge.replicant.shared.transport.ReplicantContext;
import org.realityforge.ssf.InMemorySessionManager;

/**
 * Base class for session managers.
 */
public abstract class ReplicantSessionManager<T extends ReplicantSession>
  extends InMemorySessionManager<T>
  implements EntityMessageEndpoint
{
  @Resource
  private TransactionSynchronizationRegistry _registry;

  /**
   * Send messages to the specified session.
   * The requesting service must NOT have made any other changes that will be sent to the
   * client, otherwise this message will be discarded.
   *
   * @param session  the session.
   * @param etag     the etag for message if any.
   * @param changes the messages to be sent along to the client.
   * @return the packet created.
   */
  protected final Packet sendPacket( final T session,
                                     @Nullable final String etag,
                                     @Nonnull final List<Change> changes )
  {
    final String requestID = (String) getRegistry().getResource( ReplicantContext.REQUEST_ID_KEY );
    ReplicantContextHolder.put( ReplicantContext.REQUEST_COMPLETE_KEY, "0" );
    return session.getQueue().addPacket( requestID, etag, changes );
  }

  /**
   * @return the transaction synchronization registry.
   */
  protected final TransactionSynchronizationRegistry getRegistry()
  {
    return _registry;
  }

  /**
   * Return the next packet to send to the client.
   * The packet is only returned if the client has acked the previous message.
   *
   * @param session           the session.
   * @param lastSequenceAcked the sequence that the client last ack'ed.
   * @return the packet or null if no packet is ready.
   */
  @Nullable
  protected final Packet poll( @Nonnull final T session, final int lastSequenceAcked )
  {
    final PacketQueue queue = session.getQueue();
    queue.ack( lastSequenceAcked );
    return queue.nextPacketToProcess();
  }

  /**
   * Return the session associated with the specified id.
   *
   * @param sessionID the session id.
   * @return the associated session.
   * @throws BadSessionException if unable to locate session with specified id.
   */
  @Nonnull
  protected final T ensureSession( @Nonnull final String sessionID )
    throws BadSessionException
  {
    final T session = getSession( sessionID );
    if ( null == session )
    {
      throw new BadSessionException();
    }
    return session;
  }
}
