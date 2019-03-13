package org.realityforge.replicant.server.transport;

import javax.annotation.Nonnull;
import org.realityforge.replicant.server.ChangeSet;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class PacketQueueTest
{
  @Test
  public void basicFlow()
  {
    final PacketQueue queue = new PacketQueue();
    assertEquals( queue.getLastSequenceAcked(), 0 );
    assertEquals( queue.size(), 0 );

    queue.addPacket( 21, null, new ChangeSet() );
    queue.addPacket( 22, null, new ChangeSet() );
    queue.addPacket( 23, null, new ChangeSet() );
    queue.addPacket( 24, null, new ChangeSet() );

    assertEquals( queue.size(), 4 );

    assertEquals( getPacketBySequence( queue, 1 ).getRequestId(), (Integer) 21 );
    assertEquals( getPacketBySequence( queue, 2 ).getRequestId(), (Integer) 22 );
    assertEquals( getPacketBySequence( queue, 3 ).getRequestId(), (Integer) 23 );
    assertEquals( getPacketBySequence( queue, 4 ).getRequestId(), (Integer) 24 );

    assertEquals( queue.nextPacketToProcess(), getPacketBySequence( queue, 1 ) );

    queue.ack( 0 );

    assertEquals( queue.nextPacketToProcess(), getPacketBySequence( queue, 1 ) );

    queue.ack( 1 );
    assertEquals( queue.size(), 3 );
    assertEquals( queue.nextPacketToProcess(), getPacketBySequence( queue, 2 ) );

    assertEquals( getPacketBySequence( queue, 3 ).getSequence(), 3 );
    assertEquals( getPacketBySequence( queue, 4 ).getSequence(), 4 );
  }

  @Nonnull
  private Packet getPacketBySequence( @Nonnull final PacketQueue queue, final int sequence )
  {
    final Packet packet = queue.findPacketBySequence( sequence );
    assertNotNull( packet );
    return packet;
  }

  @Test
  public void ack()
  {
    final PacketQueue queue = new PacketQueue();
    assertEquals( queue.getLastSequenceAcked(), 0 );
    assertEquals( queue.size(), 0 );

    queue.addPacket( null, null, new ChangeSet() );
    queue.addPacket( null, null, new ChangeSet() );
    queue.addPacket( null, null, new ChangeSet() );
    queue.addPacket( null, null, new ChangeSet() );

    assertEquals( queue.size(), 4 );

    queue.ack( 0 );
    assertEquals( queue.getLastSequenceAcked(), 0 );
    queue.ack( 1 );
    assertEquals( queue.getLastSequenceAcked(), 1 );
    assertEquals( queue.size(), 3 );
    queue.ack( 0 );
    assertEquals( queue.getLastSequenceAcked(), 1 );
    assertEquals( queue.size(), 3 );
    queue.ack( 4 );
    assertEquals( queue.getLastSequenceAcked(), 4 );
    assertEquals( queue.size(), 0 );

    try
    {
      queue.ack( 5 );
      fail( "Attempted to ack future sequence" );
    }
    catch ( final IllegalStateException ise )
    {
      //Ignored
    }
  }
}
