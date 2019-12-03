package org.realityforge.replicant.server;

import java.util.Arrays;
import java.util.HashMap;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class EntityMessageSetTest
{
  @Test
  public void mergeElementsOverrideExisting()
  {
    final String id = "myID";
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );
    final EntityMessage message2 = MessageTestUtil.createMessage( id, typeID, 2, "r3", null, "a3", null );
    final EntityMessage message3 = MessageTestUtil.createMessage( id, typeID, 1, null, "r4", null, "a4" );

    final EntityMessageSet set = new EntityMessageSet();
    set.merge( message );
    assertEquals( set.getEntityMessages().size(), 1 );

    set.merge( message2 );
    assertEquals( set.getEntityMessages().size(), 1 );
    assertEquals( set.getEntityMessages().iterator().next(), message );
    assertEquals( message.getTimestamp(), 2, "Timestamp merge rule is to take the latest value" );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY1, "a3" );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY2, "a2" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY1, "r3" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY2, "r2" );

    set.merge( message3 );
    assertEquals( set.getEntityMessages().size(), 1 );

    assertEquals( set.getEntityMessages().iterator().next(), message );
    assertEquals( message.getTimestamp(), 2, "Timestamp merge rule is to take the latest value" );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY1, "a3" );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY2, "a4" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY1, "r3" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY2, "r4" );
  }

  @Test
  public void mergeReplacesIfCopySpecified()
  {
    final String id = "myID";
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );

    final EntityMessageSet set = new EntityMessageSet();
    set.merge( message, true );
    final EntityMessage inserted = set.getEntityMessages().iterator().next();
    assertNotSame( inserted, message );

    assertEquals( inserted.getTimestamp(), 0 );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY1, "a1" );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY2, "a2" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY1, "r1" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY2, "r2" );
  }

  @Test
  public void mergeMultiple()
  {
    final String id = "myID";
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );
    final EntityMessage message2 = MessageTestUtil.createMessage( id, typeID, 2, "r3", null, "a3", null );
    final EntityMessage message3 = MessageTestUtil.createMessage( id, typeID, 1, null, "r4", null, "a4" );

    final EntityMessageSet set = new EntityMessageSet();
    set.mergeAll( Arrays.asList( message, message2, message3 ) );
    assertEquals( set.getEntityMessages().size(), 1 );
    final EntityMessage inserted = set.getEntityMessages().iterator().next();
    assertSame( inserted, message );

    assertEquals( inserted.getTimestamp(), 2, "Timestamp merge rule is to take the latest value" );
    MessageTestUtil.assertAttributeValue( inserted, MessageTestUtil.ATTR_KEY1, "a3" );
    MessageTestUtil.assertAttributeValue( inserted, MessageTestUtil.ATTR_KEY2, "a4" );
    MessageTestUtil.assertRouteValue( inserted, MessageTestUtil.ROUTING_KEY1, "r3" );
    MessageTestUtil.assertRouteValue( inserted, MessageTestUtil.ROUTING_KEY2, "r4" );
  }

  @Test
  public void mergeMultipleWithCopy()
  {
    final String id = "myID";
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );
    final EntityMessage message2 = MessageTestUtil.createMessage( id, typeID, 2, "r3", null, "a3", null );
    final EntityMessage message3 = MessageTestUtil.createMessage( id, typeID, 1, null, "r4", null, "a4" );

    final EntityMessageSet set = new EntityMessageSet();
    set.mergeAll( Arrays.asList( message, message2, message3 ), true );
    assertEquals( set.getEntityMessages().size(), 1 );

    final EntityMessage inserted = set.getEntityMessages().iterator().next();
    assertNotSame( inserted, message );

    assertEquals( inserted.getTimestamp(), 2, "Timestamp merge rule is to take the latest value" );
    MessageTestUtil.assertAttributeValue( inserted, MessageTestUtil.ATTR_KEY1, "a3" );
    MessageTestUtil.assertAttributeValue( inserted, MessageTestUtil.ATTR_KEY2, "a4" );
    MessageTestUtil.assertRouteValue( inserted, MessageTestUtil.ROUTING_KEY1, "r3" );
    MessageTestUtil.assertRouteValue( inserted, MessageTestUtil.ROUTING_KEY2, "r4" );
  }

  @Test
  public void isMessagePresent()
  {
    final EntityMessage message =
      new EntityMessage( "a",
                         42,
                         0,
                         new HashMap<>(),
                         new HashMap<>(),
                         null );

    final EntityMessageSet set = new EntityMessageSet();

    assertFalse( set.containsEntityMessage( message.getTypeId(), message.getId() ) );
    set.merge( message );
    assertTrue( set.containsEntityMessage( message.getTypeId(), message.getId() ) );
  }
}
