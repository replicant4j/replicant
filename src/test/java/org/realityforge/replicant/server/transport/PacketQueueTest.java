package org.realityforge.replicant.server.transport;

import java.util.ArrayList;
import org.realityforge.replicant.server.Change;
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

    final Packet p1 = queue.addPacket( null, null, newChanges() );
    final Packet p2 = queue.addPacket( null, null, newChanges() );
    final Packet p3 = queue.addPacket( null, null, newChanges() );
    final Packet p4 = queue.addPacket( null, null, newChanges() );

    assertEquals( queue.size(), 4 );

    assertEquals( queue.getPacket( 1 ), p1 );
    assertEquals( queue.getPacket( 2 ), p2 );
    assertEquals( queue.getPacket( 3 ), p3 );
    assertEquals( queue.getPacket( 4 ), p4 );

    assertEquals( queue.nextPacketToProcess().getSequence(), 1 );
    assertEquals( queue.nextPacketToProcess(), queue.getPacket( 1 ) );
    assertEquals( queue.nextPacketToProcess().getSequence(), 1 );

    queue.ack( 0 );
    assertEquals( queue.nextPacketToProcess().getSequence(), 1 );

    queue.ack( 1 );
    assertEquals( queue.size(), 3 );
    assertEquals( queue.nextPacketToProcess().getSequence(), 2 );
    assertEquals( queue.nextPacketToProcess(), queue.getPacket( 2 ) );

    assertEquals( queue.getPacket( 3 ).getSequence(), 3 );
    assertEquals( queue.getPacket( 4 ).getSequence(), 4 );
  }

  @Test
  public void ack()
  {
    final PacketQueue queue = new PacketQueue();
    assertEquals( queue.getLastSequenceAcked(), 0 );
    assertEquals( queue.size(), 0 );

    queue.addPacket( null, null, newChanges() );
    queue.addPacket( null, null, newChanges() );
    queue.addPacket( null, null, newChanges() );
    queue.addPacket( null, null, newChanges() );

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

  private ArrayList<Change> newChanges()
  {
    return new ArrayList<Change>();
  }
}
