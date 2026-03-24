package replicant.server.transport;

import java.util.ArrayList;
import java.util.List;
import javax.json.Json;
import javax.json.JsonValue;
import replicant.server.ValueUtil;
import org.testng.annotations.Test;
import replicant.server.ChangeSet;
import replicant.server.EntityMessage;
import static org.testng.Assert.*;

public class PacketTest
{
  @Test
  public void packetFromInitiator()
  {
    final var requestId = ValueUtil.randomInt();
    final var response = Json.createArrayBuilder().build();
    final var etag = ValueUtil.randomString();
    final var messages = new ArrayList<EntityMessage>();
    final var changeSet = new ChangeSet();

    final var packet = new Packet( true, requestId, response, etag, messages, changeSet );

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
    final var messages = new ArrayList<EntityMessage>();
    final var changeSet = new ChangeSet();

    final var packet = new Packet( false, null, null, null, messages, changeSet );

    assertFalse( packet.altersExplicitSubscriptions() );
    assertNull( packet.requestId() );
    assertNull( packet.etag() );
    assertSame( packet.messages(), messages );
    assertSame( packet.changeSet(), changeSet );
  }
}
