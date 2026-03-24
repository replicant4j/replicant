package replicant.server;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChangeTest
{
  @Test
  public void basicOperation()
  {
    final var id = 17;
    final var typeID = 42;

    final var message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );
    final var change = new Change( message );

    assertEquals( change.getKey(), "42#17" );
    assertEquals( change.getEntityMessage(), message );
    assertEquals( change.getChannels().size(), 0 );
  }

  @Test
  public void duplicate()
  {
    final var id = 17;
    final var typeID = 42;

    final var message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );
    final var change = new Change( message );
    change.getChannels().add( new ChannelAddress( 1, 1 ) );
    change.getChannels().add( new ChannelAddress( 2, 3 ) );

    final var duplicate = change.duplicate();
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
    final var id = 17;
    final var typeID = 42;

    final var message1 = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );
    final var message2 = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r3", "aZ", "a2" );

    final var change1 = new Change( message1 );
    final var change2 = new Change( message2 );

    change1.getChannels().add( new ChannelAddress( 1, 1 ) );
    change2.getChannels().add( new ChannelAddress( 2, 3 ) );

    assertEquals( change1.getChannels().size(), 1 );
    assertFalse( change1.getChannels().contains( new ChannelAddress( 2, 3 ) ) );
    assertEquals( change1.getEntityMessage().getAttributeValues().get( MessageTestUtil.ATTR_KEY1 ), "a1" );
    assertEquals( change1.getEntityMessage().getRoutingKeys().get( MessageTestUtil.ROUTING_KEY2 ), "r2" );

    change1.merge( change2 );

    assertEquals( change1.getChannels().size(), 2 );
    assertTrue( change1.getChannels().contains( new ChannelAddress( 2, 3 ) ) );
    assertEquals( change1.getEntityMessage().getAttributeValues().get( MessageTestUtil.ATTR_KEY1 ), "aZ" );
    assertEquals( change1.getEntityMessage().getRoutingKeys().get( MessageTestUtil.ROUTING_KEY2 ), "r3" );
  }

  @Test
  public void constructor_includesFilterInstanceId()
  {
    final var id = 3;
    final var typeID = 4;

    final var message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );
    final var address = new ChannelAddress( 7, 12, "instance-a" );
    final var change = new Change( message, address );

    assertEquals( change.getChannels().size(), 1 );
    assertTrue( change.getChannels().contains( address ) );
  }

  @Test
  public void merge_preservesDistinctFilterInstanceIds()
  {
    final var id = 8;
    final var typeID = 9;

    final var message1 = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );
    final var message2 = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r3", "aZ", "a2" );

    final var change1 = new Change( message1 );
    final var change2 = new Change( message2 );

    final var addressA = new ChannelAddress( 5, 11, "inst-1" );
    final var addressADuplicate = new ChannelAddress( 5, 11, "inst-1" );
    final var addressB = new ChannelAddress( 5, 11, "inst-2" );

    change1.getChannels().add( addressA );
    change2.getChannels().add( addressADuplicate );
    change2.getChannels().add( addressB );

    change1.merge( change2 );

    assertEquals( change1.getChannels().size(), 2 );
    assertTrue( change1.getChannels().contains( addressA ) );
    assertTrue( change1.getChannels().contains( addressB ) );
  }
}
