package org.realityforge.replicant.server;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class EntityMessageSetTest
{
  @Test
  public void mergeElementsOverrideExisting()
  {
    final String id = "myID";
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, "r1", "r2", "a1", "a2" );
    final EntityMessage message2 = MessageTestUtil.createMessage( id, typeID, "r3", null, "a3", null );
    final EntityMessage message3 = MessageTestUtil.createMessage( id, typeID, null, "r4", null, "a4" );

    final EntityMessageSet set = new EntityMessageSet();
    set.merge( message );
    assertEquals( set.getEntityMessages().size(), 1 );

    set.merge( message2 );
    assertEquals( set.getEntityMessages().size(), 1 );
    assertEquals( set.getEntityMessages().iterator().next(), message );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY1, "a3" );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY2, "a2" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY1, "r3" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY2, "r2" );

    set.merge( message3 );
    assertEquals( set.getEntityMessages().size(), 1 );

    assertEquals( set.getEntityMessages().iterator().next(), message );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY1, "a3" );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY2, "a4" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY1, "r3" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY2, "r4" );
  }
}
