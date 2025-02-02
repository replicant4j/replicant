package org.realityforge.replicant.server.transport;

import java.util.ArrayList;
import java.util.List;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.EntityMessage;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class PacketTest
{
  @Test
  public void packetFromInitiator()
  {
    final int requestId = ValueUtil.randomInt();
    final String response = "[]";
    final String etag = ValueUtil.randomString();
    final List<EntityMessage> messages = new ArrayList<>();
    final ChangeSet changeSet = new ChangeSet();

    final Packet packet = new Packet( true, requestId, response, etag, messages, changeSet );

    assertTrue( packet.altersExplicitSubscriptions() );
    assertEquals( packet.getRequestId(), (Integer) requestId );
    assertEquals( packet.getResponse(), response );
    assertEquals( packet.getEtag(), etag );
    assertSame( packet.getMessages(), messages );
    assertSame( packet.getChangeSet(), changeSet );
  }

  @Test
  public void packetNotFromInitiator()
  {
    final List<EntityMessage> messages = new ArrayList<>();
    final ChangeSet changeSet = new ChangeSet();

    final Packet packet = new Packet( false, null, null, null, messages, changeSet );

    assertFalse( packet.altersExplicitSubscriptions() );
    assertNull( packet.getRequestId() );
    assertNull( packet.getEtag() );
    assertSame( packet.getMessages(), messages );
    assertSame( packet.getChangeSet(), changeSet );
  }
}
