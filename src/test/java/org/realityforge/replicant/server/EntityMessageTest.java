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

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, "r1", "r2", "a1", "a2" );

    assertEquals( message.getID(), id );
    assertEquals( message.getTypeID(), typeID );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY1, "a1" );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY2, "a2" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY1, "r1" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY2, "r2" );

    final EntityMessage message2 = MessageTestUtil.createMessage( id, typeID, "r3", null, "a3", null );

    message.merge( message2 );

    assertEquals( message.getID(), id );
    assertEquals( message.getTypeID(), typeID );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY1, "a3" );
    MessageTestUtil.assertAttributeValue( message, MessageTestUtil.ATTR_KEY2, "a2" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY1, "r3" );
    MessageTestUtil.assertRouteValue( message, MessageTestUtil.ROUTING_KEY2, "r2" );
  }

  @Test
  public void mergeDeletedEnsuresDeleted()
  {
    final String id = "myID";
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, "r1", "r2", "a1", "a2" );

    assertTrue( message.isUpdate() );
    assertFalse( message.isDelete() );

    message.merge( MessageTestUtil.createMessage( id, typeID, "r1", "r2", null, null ) );

    assertFalse( message.isUpdate() );
    assertTrue( message.isDelete() );
  }

  @Test
  public void mergeUpdateRevivesDeleted()
  {
    final String id = "myID";
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, "r1", "r2", null, null );

    assertFalse( message.isUpdate() );
    assertTrue( message.isDelete() );

    message.merge( MessageTestUtil.createMessage( id, typeID, "r1", "r2", "a1", "a2" ) );

    assertTrue( message.isUpdate() );
    assertFalse( message.isDelete() );
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
