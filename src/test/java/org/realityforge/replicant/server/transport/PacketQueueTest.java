package org.realityforge.replicant.server.transport;

import java.util.ArrayList;
import org.realityforge.replicant.server.EntityMessage;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class PacketQueueTest
{
  @Test
  public void x()
  {
    final PacketQueue queue = new PacketQueue();
    assertEquals( queue.getLastSequenceAcked(), 0 );
    assertEquals( queue.size(), 0 );

    queue.addPacket( null, newChanges() );
    queue.addPacket( null, newChanges() );
    queue.addPacket( null, newChanges() );
    queue.addPacket( null, newChanges() );

    assertEquals( queue.size(), 4 );
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

  private ArrayList<EntityMessage> newChanges()
  {
    return new ArrayList<>();
  }
}
