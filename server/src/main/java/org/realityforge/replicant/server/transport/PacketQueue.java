package org.realityforge.replicant.server.transport;

import java.util.Iterator;
import java.util.LinkedList;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.server.ChangeSet;

/**
 * A queue of packets for session.
 */
public class PacketQueue
{
  /**
   * List of packets associated with session.
   */
  private final LinkedList<Packet> _packets = new LinkedList<>();
  /**
   * Sequence of last packet removed from queue.
   */
  private int _lastSequenceAcked;
  /**
   * Sequence of next packet to be added to the queue.
   */
  private int _nextSequence = 1;

  /**
   * @return the number of packets in queue.
   */
  public synchronized int size()
  {
    return _packets.size();
  }

  /**
   * Acknowledge that the remote side has received packet with specified sequence.
   *
   * @param sequence the sequence.
   */
  public synchronized void ack( final int sequence )
  {
    if ( sequence >= _nextSequence )
    {
      final String message = "Attempting to ack sequence " + sequence + " when next sequence is " + _nextSequence;
      throw new IllegalStateException( message );
    }
    else if ( _lastSequenceAcked < sequence )
    {
      removePacketsLessThanOrEqual( sequence );
      _lastSequenceAcked = sequence;
    }
  }

  @Nullable
  public synchronized Packet nextPacketToProcess()
  {
    if ( 0 == _packets.size() )
    {
      return null;
    }
    else
    {
      final Packet packet = _packets.getFirst();
      if ( packet.isPrevious( _lastSequenceAcked ) )
      {
        return packet;
      }
      else
      {
        return null;
      }
    }
  }

  public synchronized int getLastSequenceAcked()
  {
    return _lastSequenceAcked;
  }

  /**
   * Add packet to queue.
   *
   * @param requestId the opaque identifier indicating the request that caused the changes if the owning session initiated the changes.
   * @param etag      the opaque identifier identifying the version. May be null if packet is not cache-able
   * @param changeSet the changeSet to create packet from.
   * @return the packet.
   */
  public synchronized Packet addPacket( @Nullable final Integer requestId,
                                        @Nullable final String etag,
                                        @Nonnull final ChangeSet changeSet )
  {
    final Packet packet = new Packet( _nextSequence++, requestId, etag, changeSet );
    _packets.add( packet );
    _packets.sort( null );
    return packet;
  }

  /**
   * Remove packets with a sequence less than or equal to specified sequence.
   *
   * @param sequence the sequence
   */
  private void removePacketsLessThanOrEqual( final int sequence )
  {
    final Iterator<Packet> iterator = _packets.iterator();
    while ( iterator.hasNext() )
    {
      final Packet packet = iterator.next();
      final int seq = packet.getSequence();

      if ( packet.isLessThanOrEqual( sequence ) )
      {
        iterator.remove();
        if ( seq == sequence )
        {
          break;
        }
      }
    }
  }

  /**
   * Return the packet with specified sequence.
   *
   * @param sequence the sequence.
   * @return the packet with sequence or null if no such packet.
   */
  @Nullable
  public synchronized Packet findPacketBySequence( final int sequence )
  {
    for ( final Packet packet : _packets )
    {
      final int seq = packet.getSequence();
      if ( seq == sequence )
      {
        return packet;
      }
    }
    return null;
  }

  public String toString()
  {
    return "PacketQueue[" + _packets + "]";
  }
}
