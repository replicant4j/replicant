package replicant.server;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChangeTest
{
  @Test
  public void basicOperation()
  {
    final int id = 17;
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );
    final Change change = new Change( message );

    assertEquals( change.getKey(), "42#17" );
    assertEquals( change.getEntityMessage(), message );
    assertEquals( change.getChannels().size(), 0 );
  }

  @Test
  public void duplicate()
  {
    final int id = 17;
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );
    final Change change = new Change( message );
    change.getChannels().put( 1, 1 );
    change.getChannels().put( 2, 3 );

    final Change duplicate = change.duplicate();
    assertEquals( duplicate.getKey(), change.getKey() );
    assertEquals( duplicate.getEntityMessage().getId(), change.getEntityMessage().getId() );
    assertNotSame( duplicate.getEntityMessage(), change.getEntityMessage() );
    assertEquals( duplicate.getChannels(), change.getChannels() );
    //noinspection SimplifiableAssertion
    assertFalse( duplicate.getChannels() == change.getChannels() );
  }

  @SuppressWarnings( "ConstantConditions" )
  @Test
  public void merge_combinesChannels()
  {
    final int id = 17;
    final int typeID = 42;

    final EntityMessage message1 = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );
    final EntityMessage message2 = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r3", "aZ", "a2" );

    final Change change1 = new Change( message1 );
    final Change change2 = new Change( message2 );

    change1.getChannels().put( 1, 1 );
    change2.getChannels().put( 2, 3 );

    assertEquals( change1.getChannels().size(), 1 );
    assertNull( change1.getChannels().get( 2 ) );
    assertEquals( change1.getEntityMessage().getAttributeValues().get( MessageTestUtil.ATTR_KEY1 ), "a1" );
    assertEquals( change1.getEntityMessage().getRoutingKeys().get( MessageTestUtil.ROUTING_KEY2 ), "r2" );

    change1.merge( change2 );

    assertEquals( change1.getChannels().size(), 2 );
    assertEquals( change1.getChannels().get( 2 ), (Integer) 3 );
    assertEquals( change1.getEntityMessage().getAttributeValues().get( MessageTestUtil.ATTR_KEY1 ), "aZ" );
    assertEquals( change1.getEntityMessage().getRoutingKeys().get( MessageTestUtil.ROUTING_KEY2 ), "r3" );
  }
}
