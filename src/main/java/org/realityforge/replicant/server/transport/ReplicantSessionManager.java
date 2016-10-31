package org.realityforge.replicant.server.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.EntityMessageEndpoint;
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
   * @param session   the session.
   * @param etag      the etag for message if any.
   * @param changeSet the messages to be sent along to the client.
   * @return the packet created.
   */
  protected Packet sendPacket( final T session, @Nullable final String etag, @Nonnull final ChangeSet changeSet )
  {
    final String requestID = (String) getRegistry().getResource( ReplicantContext.REQUEST_ID_KEY );
    getRegistry().putResource( ReplicantContext.REQUEST_COMPLETE_KEY, Boolean.FALSE );
    return session.getQueue().addPacket( requestID, etag, changeSet );
  }

  /**
   * @return the transaction synchronization registry.
   */
  @Nonnull
  protected TransactionSynchronizationRegistry getRegistry()
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
  protected Packet pollPacket( @Nonnull final T session, final int lastSequenceAcked )
  {
    final PacketQueue queue = session.getQueue();
    queue.ack( lastSequenceAcked );
    return queue.nextPacketToProcess();
  }
}
