package org.realityforge.replicant.server.transport;

import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.server.MessageTestUtil;
import org.realityforge.replicant.server.transport.EntityMessageAccumulator;
import org.realityforge.replicant.server.transport.PacketQueue;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class EntityMessageAccumulatorTest
{
  @Test
  public void basicOperation()
  {
    final PacketQueue c = new PacketQueue();
    final EntityMessageAccumulator accumulator = new EntityMessageAccumulator();

    final String id = "myID";
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );

    accumulator.addEntityMessage( c, message );
    accumulator.complete();

    assertEquals( c.size(), 1 );
    assertEquals( c.nextPacketToProcess().getChanges().get( 0 ).getID(), id );

    accumulator.complete();
    assertEquals( c.size(), 1 );
  }
}
