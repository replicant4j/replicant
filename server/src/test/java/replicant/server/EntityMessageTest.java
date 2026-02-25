package replicant.server;

import java.io.Serializable;
import java.util.HashMap;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public final class EntityMessageTest
{
  @Test
  public void constructor_withoutLinks_setsLinksToNull()
  {
    final var routingKeys = new HashMap<String, Serializable>();
    routingKeys.put( "R", "v" );
    final var attributes = new HashMap<String, Serializable>();
    attributes.put( "A", "x" );

    final var message = new EntityMessage( 11, 22, 33L, routingKeys, attributes );

    assertEquals( message.getId(), 11 );
    assertEquals( message.getTypeId(), 22 );
    assertEquals( message.getTimestamp(), 33L );
    assertEquals( message.getRoutingKeys(), routingKeys );
    assertEquals( message.getAttributeValues(), attributes );
    assertNull( message.getLinks() );
    assertTrue( message.isUpdate() );
  }

  @Test
  public void mergeElementsOverrideExisting()
  {
    final var id = 17;
    final var typeID = 42;

    final var message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );

    assertEquals( message.getId(), id );
    assertEquals( message.getTypeId(), typeID );
    assertEquals( message.getTimestamp(), 0 );
    assertNull( message.getLinks() );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY1, "a1" );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY2, "a2" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY1, "r1" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY2, "r2" );

    final var message2 =
      MessageTestUtil.createMessage( id,
                                     typeID,
                                     2,
                                     new ChannelLink( new ChannelAddress( 1, 2 ), new ChannelAddress( 47, 66 ), null ),
                                     "r3",
                                     null,
                                     "a3",
                                     null );

    message.merge( message2 );

    assertEquals( message.getId(), id );
    assertEquals( message.getTypeId(), typeID );
    assertEquals( message.getTimestamp(), 2 );
    assertNotNull( message.getLinks() );
    assertEquals( message.getLinks().size(), 1 );
    final var channelLink = message.getLinks().iterator().next();
    assertEquals( channelLink.source().channelId(), 1 );
    assertEquals( channelLink.source().rootId(), (Integer) 2 );
    assertEquals( channelLink.target().channelId(), 47 );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY1, "a3" );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY2, "a2" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY1, "r3" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY2, "r2" );

    final var message3 = MessageTestUtil.createMessage( id, typeID, 1, null, null, null, "a4" );

    message.merge( message3 );
    assertEquals( message.getId(), id );
    assertEquals( message.getTypeId(), typeID );
    assertEquals( message.getTimestamp(), 2, "Timestamp merge rule is to take the latest value" );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY1, "a3" );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY2, "a4" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY1, "r3" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY2, "r2" );
  }

  @Test
  public void mergeDeletedEnsuresDeleted()
  {
    final var id = 17;
    final var typeID = 42;

    final var message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );

    assertTrue( message.isUpdate() );
    assertFalse( message.isDelete() );

    message.merge( MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", null, null ) );

    assertFalse( message.isUpdate() );
    assertTrue( message.isDelete() );
  }

  @Test
  public void mergeUpdateRevivesDeleted()
  {
    final var id = 17;
    final var typeID = 42;

    final var message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", null, null );

    assertFalse( message.isUpdate() );
    assertTrue( message.isDelete() );

    message.merge( MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" ) );

    assertTrue( message.isUpdate() );
    assertFalse( message.isDelete() );
  }

  @Test
  public void toStringIncludesDataWhenDataPresent()
  {
    final var message = MessageTestUtil.createMessage( 17, 42, 0, "r1", "r2", "a1", "a2" );
    assertTrue( message.toString().matches( ".*Data=.*" ) );
  }

  @Test
  public void toIsDeleteFlagIsCorrect()
  {
    final var deleteMessage = MessageTestUtil.createMessage( 17, 42, 0, "r1", "r2", null, null );
    assertFalse( deleteMessage.toString().matches( ".*Data=.*" ) );
    assertFalse( deleteMessage.isUpdate() );
    assertTrue( deleteMessage.isDelete() );
  }

  @Test
  public void toDelete()
  {
    final var id = ValueUtil.randomInt();
    final var typeId = ValueUtil.randomInt();
    final var timestamp = ValueUtil.randomInt();
    final var message = MessageTestUtil.createMessage( id, typeId, timestamp, "r1", "r2", "a1", "a2" );

    assertEquals( message.getId(), id );
    assertEquals( message.getTypeId(), typeId );
    assertEquals( message.getTimestamp(), timestamp );
    assertNull( message.getLinks() );
    assertTrue( message.isUpdate() );
    assertFalse( message.isDelete() );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY1, "a1" );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY2, "a2" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY1, "r1" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY2, "r2" );

    final var message2 = message.toDelete();

    assertEquals( message2.getId(), id );
    assertEquals( message2.getTypeId(), typeId );
    assertEquals( message2.getTimestamp(), timestamp );
    assertNull( message2.getLinks() );
    assertNull( message2.getAttributeValues() );
    assertFalse( message2.isUpdate() );
    assertTrue( message2.isDelete() );
    MessageTestUtil.assertRouteValue( message2, MessageTestUtil.ROUTING_KEY1, "r1" );
    MessageTestUtil.assertRouteValue( message2, MessageTestUtil.ROUTING_KEY2, "r2" );
  }
}
