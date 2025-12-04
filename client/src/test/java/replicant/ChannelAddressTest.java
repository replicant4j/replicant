package replicant;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public final class ChannelAddressTest
  extends AbstractReplicantTest
{
  @Test
  public void construct()
  {
    final ChannelAddress address = new ChannelAddress( 2, 4, 1 );

    assertEquals( address.schemaId(), 2 );
    assertEquals( address.channelId(), 4 );
    assertEquals( address.rootId(), (Integer) 1 );
  }

  @Test
  public void constructTypeChannel()
  {
    final ChannelAddress address = new ChannelAddress( 2, 4 );

    assertEquals( address.schemaId(), 2 );
    assertEquals( address.channelId(), 4 );
    assertNull( address.rootId() );
  }

  @Test
  public void parseWithSubChannel()
  {
    final ChannelAddress address = ChannelAddress.parse( 2, "4.1" );

    assertEquals( address.schemaId(), 2 );
    assertEquals( address.channelId(), 4 );
    assertEquals( address.rootId(), (Integer) 1 );
  }

  @Test
  public void parse()
  {
    final ChannelAddress address = ChannelAddress.parse( 4, "77" );

    assertEquals( address.schemaId(), 4 );
    assertEquals( address.channelId(), 77 );
    assertNull( address.rootId() );
  }

  @Test
  public void getCacheKey()
  {
    final ChannelAddress address = new ChannelAddress( 2, 4, 1 );
    assertEquals( address.getCacheKey(), "RC-2.4.1" );
  }

  @SuppressWarnings( { "EqualsWithItself", "SimplifiableAssertion" } )
  @Test
  public void testEquals()
  {
    final ChannelAddress address1 = new ChannelAddress( 1, 2, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 2, 1 );
    final ChannelAddress address3 = new ChannelAddress( 1, 2, 2 );
    final ChannelAddress address4 = new ChannelAddress( 1, 1, 1 );
    final ChannelAddress address5 = new ChannelAddress( 2, 2, 1 );
    final ChannelAddress address6 = new ChannelAddress( 1, 2 );

    assertTrue( address1.equals( address1 ) );
    assertTrue( address1.equals( address2 ) );
    assertFalse( address1.equals( new Object() ) );
    assertFalse( address1.equals( null ) );
    assertFalse( address1.equals( address3 ) );
    assertFalse( address1.equals( address4 ) );
    assertFalse( address1.equals( address5 ) );
    assertFalse( address1.equals( address6 ) );
  }

  @Test
  public void testHashCode()
  {
    final ChannelAddress address1 = new ChannelAddress( 1, 2, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 2, 1 );
    final ChannelAddress address3 = new ChannelAddress( 1, 2, 2 );

    assertEquals( address1.hashCode(), address2.hashCode() );
    assertNotEquals( address1.hashCode(), address3.hashCode() );
  }

  @Test
  public void toStringTest()
  {
    final ChannelAddress address = new ChannelAddress( 1, 2, 1 );
    assertEquals( address.toString(), "1.2.1" );
  }

  @Test
  public void toStringTest_NamingDisabled()
  {
    ReplicantTestUtil.disableNames();
    final ChannelAddress address =
      new ChannelAddress( ValueUtil.randomInt(), ValueUtil.randomInt(), ValueUtil.randomInt() );
    assertEquals( address.toString(), "replicant.ChannelAddress@" + Integer.toHexString( address.hashCode() ) );
  }

  @Test
  public void getName_NamingDisabled()
  {
    ReplicantTestUtil.disableNames();
    assertEquals( new ChannelAddress( 1, 3, 5 ).getName(), "1.3.5" );
  }

  @Test
  public void asChannelDescriptor()
  {
    assertEquals( new ChannelAddress( 1, 3, 5 ).asChannelDescriptor(), "3.5" );
    assertEquals( new ChannelAddress( 1, 3 ).asChannelDescriptor(), "3" );
  }

  @SuppressWarnings( "EqualsWithItself" )
  @Test
  public void compareTo()
  {
    final ChannelAddress address1 = new ChannelAddress( 1, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 2, 3 );
    final ChannelAddress address3 = new ChannelAddress( 1, 2, 2 );
    final ChannelAddress address4 = new ChannelAddress( 2, 1 );
    final ChannelAddress address5 = new ChannelAddress( 1, 2 );

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
  }
}
