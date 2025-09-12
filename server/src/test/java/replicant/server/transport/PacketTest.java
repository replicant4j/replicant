package replicant.server.transport;

import java.util.ArrayList;
import java.util.List;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.server.ChangeSet;
import replicant.server.EntityMessage;
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
    assertEquals( packet.requestId(), (Integer) requestId );
    assertEquals( packet.response(), response );
    assertEquals( packet.etag(), etag );
    assertSame( packet.messages(), messages );
    assertSame( packet.changeSet(), changeSet );
  }

  @Test
  public void packetNotFromInitiator()
  {
    final List<EntityMessage> messages = new ArrayList<>();
    final ChangeSet changeSet = new ChangeSet();

    final Packet packet = new Packet( false, null, null, null, messages, changeSet );

    assertFalse( packet.altersExplicitSubscriptions() );
    assertNull( packet.requestId() );
    assertNull( packet.etag() );
    assertSame( packet.messages(), messages );
    assertSame( packet.changeSet(), changeSet );
  }
}
