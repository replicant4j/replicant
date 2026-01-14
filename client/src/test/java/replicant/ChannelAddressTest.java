package replicant;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public final class ChannelAddressTest
  extends AbstractReplicantTest
{
  @Test
  void construct()
  {
    final ChannelAddress address = new ChannelAddress( 2, 4, 1, "a" );

    assertEquals( address.schemaId(), 2 );
    assertEquals( address.channelId(), 4 );
    assertEquals( address.rootId(), (Integer) 1 );
    assertEquals( address.filterInstanceId(), "a" );
  }

  @Test
  void constructTypeChannel()
  {
    final ChannelAddress address = new ChannelAddress( 2, 4 );

    assertEquals( address.schemaId(), 2 );
    assertEquals( address.channelId(), 4 );
    assertNull( address.rootId() );
    assertNull( address.filterInstanceId() );
  }

  @Test
  void parseWithSubChannel()
  {
    final ChannelAddress address = ChannelAddress.parse( 2, "4.1" );

    assertEquals( address.schemaId(), 2 );
    assertEquals( address.channelId(), 4 );
    assertEquals( address.rootId(), (Integer) 1 );
  }

  @Test
  void parse()
  {
    final ChannelAddress address = ChannelAddress.parse( 4, "77" );

    assertEquals( address.schemaId(), 4 );
    assertEquals( address.channelId(), 77 );
    assertNull( address.rootId() );
  }

  @Test
  void parseWithInstanceId()
  {
    final ChannelAddress address = ChannelAddress.parse( 4, "77#alpha" );

    assertEquals( address.schemaId(), 4 );
    assertEquals( address.channelId(), 77 );
    assertNull( address.rootId() );
    assertEquals( address.filterInstanceId(), "alpha" );
  }

  @Test
  void parseWithRootAndInstanceId()
  {
    final ChannelAddress address = ChannelAddress.parse( 4, "77.5#alpha" );

    assertEquals( address.schemaId(), 4 );
    assertEquals( address.channelId(), 77 );
    assertEquals( address.rootId(), (Integer) 5 );
    assertEquals( address.filterInstanceId(), "alpha" );
  }

  @Test
  void getCacheKey()
  {
    final ChannelAddress address = new ChannelAddress( 2, 4, 1, "a" );
    assertEquals( address.getCacheKey(), "RC-2.4.1#a" );
  }

  @SuppressWarnings( { "EqualsWithItself", "SimplifiableAssertion", "ConstantValue" } )
  @Test
  void testEquals()
  {
    final ChannelAddress address1 = new ChannelAddress( 1, 2, 1, "a" );
    final ChannelAddress address2 = new ChannelAddress( 1, 2, 1, "a" );
    final ChannelAddress address3 = new ChannelAddress( 1, 2, 2, "a" );
    final ChannelAddress address4 = new ChannelAddress( 1, 1, 1 );
    final ChannelAddress address5 = new ChannelAddress( 2, 2, 1 );
    final ChannelAddress address6 = new ChannelAddress( 1, 2 );
    final ChannelAddress address7 = new ChannelAddress( 1, 2, 1, "b" );

    assertTrue( address1.equals( address1 ) );
    assertTrue( address1.equals( address2 ) );
    assertFalse( address1.equals( new Object() ) );
    assertFalse( address1.equals( null ) );
    assertFalse( address1.equals( address3 ) );
    assertFalse( address1.equals( address4 ) );
    assertFalse( address1.equals( address5 ) );
    assertFalse( address1.equals( address6 ) );
    assertFalse( address1.equals( address7 ) );
  }

  @Test
  void testHashCode()
  {
    final ChannelAddress address1 = new ChannelAddress( 1, 2, 1, "a" );
    final ChannelAddress address2 = new ChannelAddress( 1, 2, 1, "a" );
    final ChannelAddress address3 = new ChannelAddress( 1, 2, 2, "a" );

    assertEquals( address1.hashCode(), address2.hashCode() );
    assertNotEquals( address1.hashCode(), address3.hashCode() );
  }

  @Test
  void toStringTest()
  {
    final ChannelAddress address = new ChannelAddress( 1, 2, 1, "a" );
    assertEquals( address.toString(), "1.2.1#a" );
  }

  @Test
  void toStringTest_NamingDisabled()
  {
    ReplicantTestUtil.disableNames();
    final ChannelAddress address =
      new ChannelAddress( ValueUtil.randomInt(), ValueUtil.randomInt(), ValueUtil.randomInt() );
    assertEquals( address.toString(), "replicant.ChannelAddress@" + Integer.toHexString( address.hashCode() ) );
  }

  @Test
  void getName_NamingDisabled()
  {
    ReplicantTestUtil.disableNames();
    assertEquals( new ChannelAddress( 1, 3, 5, "a" ).getName(), "1.3.5#a" );
  }

  @Test
  void asChannelDescriptor()
  {
    assertEquals( new ChannelAddress( 1, 3, 5, "a" ).asChannelDescriptor(), "3.5#a" );
    assertEquals( new ChannelAddress( 1, 3 ).asChannelDescriptor(), "3" );
  }

  @SuppressWarnings( "EqualsWithItself" )
  @Test
  void compareTo()
  {
    final ChannelAddress address1 = new ChannelAddress( 1, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 2, 3 );
    final ChannelAddress address3 = new ChannelAddress( 1, 2, 2 );
    final ChannelAddress address4 = new ChannelAddress( 2, 1 );
    final ChannelAddress address5 = new ChannelAddress( 1, 2 );
    final ChannelAddress address6 = new ChannelAddress( 1, 2, null, "a" );

    // Different schema
    assertEquals( address1.compareTo( address4 ), -1 );
    assertEquals( address4.compareTo( address1 ), 1 );

    // Same schema, different channel
    assertEquals( address1.compareTo( address2 ), -1 );
    assertEquals( address2.compareTo( address1 ), 1 );

    // Same schema, same channel, different root (val vs val)
    assertEquals( address2.compareTo( address3 ), 1 );
    assertEquals( address3.compareTo( address2 ), -1 );

    // Same schema, same channel, different root (null vs val)
    assertEquals( address5.compareTo( address2 ), -1 );
    assertEquals( address2.compareTo( address5 ), 1 );

    // Same schema, same channel, different root (null vs null)
    assertEquals( address1.compareTo( address1 ), 0 );
    assertEquals( address5.compareTo( address5 ), 0 );

    // Same schema, same channel, same root, different instance id
    assertEquals( address5.compareTo( address6 ), -1 );
    assertEquals( address6.compareTo( address5 ), 1 );
  }
}
