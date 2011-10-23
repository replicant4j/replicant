package org.realityforge.replicant.server;

import java.io.Serializable;
import java.util.HashMap;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class EntityMessageTest
{
  @Test
  public void mergeElementsOverrideExisting()
  {
    final String id = "myID";
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, "r1", "r2", "a1", "a2" );

    assertEquals( message.getID(), id );
    assertEquals( message.getTypeID(), typeID );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY1, "a1" );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY2, "a2" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY1, "r1" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY2, "r2" );

    final HashMap<String, Serializable> routingKeys2 = new HashMap<String, Serializable>();
    routingKeys2.put( MessageTestUtil.ROUTING_KEY1, "r3" );

    final HashMap<String, Serializable> attributeValues2 = new HashMap<String, Serializable>();
    attributeValues2.put( MessageTestUtil.ATTR_KEY1, "a3" );

    final EntityMessage message2 =
        new EntityMessage( id, typeID, routingKeys2, attributeValues2 );

    message.merge( message2 );

    assertEquals( message.getID(), id );
    assertEquals( message.getTypeID(), typeID );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY1, "a3" );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY2, "a2" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY1, "r3" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY2, "r2" );

    assertTrue( message.toString().matches( ".*Data=.*" ) );
  }

  @Test
  public void toStringIncludesDataWhenDataPresent()
  {
    final EntityMessage message = MessageTestUtil.createMessage( "myID", 42, "r1", "r2", "a1", "a2" );
    assertTrue( message.toString().matches( ".*Data=.*" ) );
  }

  @Test
  public void toIsDeleteFlagIsCorrect()
  {
    final EntityMessage deleteMessage = MessageTestUtil.createMessage( "myID", 42, "r1", "r2", null, null );
    assertFalse( deleteMessage.toString().matches( ".*Data=.*" ) );
    assertFalse( deleteMessage.isUpdate() );
    assertTrue( deleteMessage.isDelete() );
  }
}
