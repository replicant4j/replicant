package org.realityforge.replicant.server.transport;

import java.util.Arrays;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.ChannelDescriptor;
import org.realityforge.replicant.server.EntityMessageEndpoint;
import org.realityforge.replicant.shared.transport.ReplicantContext;
import org.realityforge.ssf.InMemorySessionManager;

/**
 * Base class for session managers.
 */
public abstract class ReplicantSessionManagerImpl
  extends InMemorySessionManager<ReplicantSession>
  implements EntityMessageEndpoint, ReplicantSessionManager
{
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
  protected Packet sendPacket( @Nonnull final ReplicantSession session,
                               @Nullable final String etag,
                               @Nonnull final ChangeSet changeSet )
  {
    final String requestID = (String) getRegistry().getResource( ReplicantContext.REQUEST_ID_KEY );
    getRegistry().putResource( ReplicantContext.REQUEST_COMPLETE_KEY, Boolean.FALSE );
    return session.getQueue().addPacket( requestID, etag, changeSet );
  }

  /**
   * @return the transaction synchronization registry.
   */
  @Nonnull
  protected abstract TransactionSynchronizationRegistry getRegistry();

  /**
   * @return the metadata for all the channels as an array.
   */
  @Nonnull
  protected abstract ChannelMetaData[] getChannelMetaData();

  @Nonnull
  @Override
  protected ReplicantSession newSessionInfo()
  {
    return new ReplicantSession( UUID.randomUUID().toString() );
  }

  /**
   * @return the channel metadata.
   */
  @Nonnull
  public ChannelMetaData getChannelMetaData( @Nonnull final ChannelDescriptor descriptor )
  {
    final ChannelMetaData[] channelMetaData = getChannelMetaData();
    if ( descriptor.getChannelID() >= channelMetaData.length )
    {
      final String message =
        "Descriptor " + descriptor + " not part of declared metadata: " + Arrays.asList( channelMetaData );
      throw new IllegalStateException( message );
    }
    return channelMetaData[ descriptor.getChannelID() ];
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
  protected Packet pollPacket( @Nonnull final ReplicantSession session, final int lastSequenceAcked )
  {
    final PacketQueue queue = session.getQueue();
    queue.ack( lastSequenceAcked );
    return queue.nextPacketToProcess();
  }

  /**
   * Return session associated with specified ID.
   *
   * @throws RuntimeException if no such session is available.
   */
  @Nonnull
  protected ReplicantSession ensureSession( @Nonnull final String sessionID )
  {
    final ReplicantSession session = getSession( sessionID );
    if ( null == session )
    {
      throw newBadSessionException( sessionID );
    }
    return session;
  }

  @Nonnull
  protected abstract RuntimeException newBadSessionException( @Nonnull String sessionID );


  /**
   * Configure the SubscriptionEntries to reflect an auto graph link between the source and target graph.
   */
  protected void linkSubscriptionEntries( @Nonnull final ReplicantSession session,
                                          @Nonnull final ChannelDescriptor source,
                                          @Nonnull final ChannelDescriptor target )
  {
    session.getSubscriptionEntry( source ).registerOutwardSubscriptions( target );
    session.getSubscriptionEntry( target ).registerInwardSubscriptions( source );


    if ( getChannelMetaData( target ).getFilterType() != ChannelMetaData.FilterType.NONE )
    {
      targetEntry.setFilter( sourceEntry.getFilter() );
    }
  }
}
