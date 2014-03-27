package org.realityforge.replicant.server;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class EntityMessageTest
{
  @Test
  public void mergeElementsOverrideExisting()
  {
    final String id = "myID";
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );

    assertEquals( message.getID(), id );
    assertEquals( message.getTypeID(), typeID );
    assertEquals( message.getTimestamp(), 0 );
    assertNull( message.getLinks() );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY1, "a1" );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY2, "a2" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY1, "r1" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY2, "r2" );

    final EntityMessage message2 =
      MessageTestUtil.createMessage( id,
                                     typeID,
                                     2,
                                     new ChannelLink( new ChannelDescriptor( 47, 66 ) ),
                                     "r3",
                                     null,
                                     "a3",
                                     null );

    message.merge( message2 );

    assertEquals( message.getID(), id );
    assertEquals( message.getTypeID(), typeID );
    assertEquals( message.getTimestamp(), 2 );
    assertNotNull( message.getLinks() );
    assertEquals( message.getLinks().size(), 1 );
    assertEquals( message.getLinks().iterator().next().getTargetChannel().getChannelID(), 47 );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY1, "a3" );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY2, "a2" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY1, "r3" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY2, "r2" );

    final EntityMessage message3 = MessageTestUtil.createMessage( id, typeID, 1, null, null, null, "a4" );

    message.merge( message3 );
    assertEquals( message.getID(), id );
    assertEquals( message.getTypeID(), typeID );
    assertEquals( message.getTimestamp(), 2, "Timestamp merge rule is to take the latest value" );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY1, "a3" );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY2, "a4" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY1, "r3" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY2, "r2" );
  }

  @Test
  public void mergeDeletedEnsuresDeleted()
  {
    final String id = "myID";
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );

    assertTrue( message.isUpdate() );
    assertFalse( message.isDelete() );

    message.merge( MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", null, null ) );

    assertFalse( message.isUpdate() );
    assertTrue( message.isDelete() );
  }

  @Test
  public void mergeUpdateRevivesDeleted()
  {
    final String id = "myID";
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", null, null );

    assertFalse( message.isUpdate() );
    assertTrue( message.isDelete() );

    message.merge( MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" ) );

    assertTrue( message.isUpdate() );
    assertFalse( message.isDelete() );
  }

  @Test
  public void toStringIncludesDataWhenDataPresent()
  {
    final EntityMessage message = MessageTestUtil.createMessage( "myID", 42, 0, "r1", "r2", "a1", "a2" );
    assertTrue( message.toString().matches( ".*Data=.*" ) );
  }

  @Test
  public void toIsDeleteFlagIsCorrect()
  {
    final EntityMessage deleteMessage = MessageTestUtil.createMessage( "myID", 42, 0, "r1", "r2", null, null );
    assertFalse( deleteMessage.toString().matches( ".*Data=.*" ) );
    assertFalse( deleteMessage.isUpdate() );
    assertTrue( deleteMessage.isDelete() );
  }
}
