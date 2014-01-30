package org.realityforge.replicant.server.transport;

import java.util.ArrayList;
import org.realityforge.replicant.server.EntityMessage;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class PacketTest
{
  @Test
  public void basicOperation()
  {
    final ArrayList<EntityMessage> changes = new ArrayList<>();
    final Packet packet = new Packet( 2, "r1", changes );
    final Packet other = new Packet( 3, null, changes );

    assertEquals( packet.getSequence(), 2 );
    assertEquals( packet.getJobID(), "r1" );
    assertEquals( packet.getChanges(), changes );

    assertTrue( packet.equals( packet ) );
    assertFalse( packet.equals( other ) );
    assertFalse( other.equals( packet ) );

    assertEquals( packet.compareTo( packet ), 0 );
    assertEquals( packet.compareTo( other ), -1 );
    assertEquals( other.compareTo( packet ), 1 );

    assertTrue( packet.isLessThanOrEqual( 2 ) );
    assertTrue( packet.isLessThanOrEqual( 3 ) );
    assertFalse( packet.isLessThanOrEqual( 1 ) );

    assertTrue( packet.isLessThan( 3 ) );
    assertFalse( packet.isLessThan( 2 ) );

    assertTrue( packet.isNext( 3 ) );
    assertFalse( packet.isNext( 4 ) );
    assertFalse( packet.isNext( 2 ) );

    assertTrue( packet.isPrevious( 1 ) );
    assertFalse( packet.isPrevious( 0 ) );
    assertFalse( packet.isPrevious( 2 ) );

    assertFalse( packet.isLessThanOrEqual( Integer.MIN_VALUE ) );
  }
}
