package org.realityforge.replicant.server.transport;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.server.EntityMessage;

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
    removePacketsLessThanOrEqual( sequence );
    _lastSequenceAcked = sequence;
  }

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
   * @param requestID the opaque identifier indicating the request that caused the changes if the owning session initiated the changes.
   * @param etag      the opaque identifier identifying the version. May be null if packet is not cache-able
   * @param messages  the changes to create packet from.
   * @return the packet.
   */
  public synchronized Packet addPacket( @Nullable final String requestID,
                                        @Nullable final String etag,
                                        @Nonnull final List<EntityMessage> messages )
  {
    final Packet packet = new Packet( _nextSequence++, requestID, etag, messages );
    _packets.add( packet );
    Collections.sort( _packets );
    return packet;
  }

  /**
   * Remove packets with a sequence less than or equal to specified sequence.
   *
   * @param sequence the sequence
   */
  final void removePacketsLessThanOrEqual( final int sequence )
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
  public synchronized Packet getPacket( final int sequence )
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
