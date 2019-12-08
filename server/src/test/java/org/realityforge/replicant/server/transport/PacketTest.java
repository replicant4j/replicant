package org.realityforge.replicant.server.transport;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.server.ChangeSet;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class PacketTest
{
  @Test
  public void basicOperation()
  {
    final ChangeSet changeSet = new ChangeSet();
    final Integer requestId = ValueUtil.randomInt();
    final Packet packet = new Packet( 2, requestId, "e1", changeSet );
    final Packet other = new Packet( 3, null, null, changeSet );

    assertEquals( packet.getSequence(), 2 );
    assertEquals( packet.getRequestId(), requestId );
    assertEquals( packet.getETag(), "e1" );
    assertEquals( packet.getChangeSet(), changeSet );

    assertEquals( packet, packet );
    assertNotEquals( other, packet );
    assertNotEquals( packet, other );

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
