package replicant;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChannelAddressTest
  extends AbstractReplicantTest
{
  @Test
  public void construct()
  {
    final ChannelAddress address = new ChannelAddress( 2, 4, 1 );

    assertEquals( address.getSchemaId(), 2 );
    assertEquals( address.getChannelId(), 4 );
    assertEquals( address.getId(), (Integer) 1 );
  }

  @Test
  public void parseWithSubChannel()
  {
    final ChannelAddress address = ChannelAddress.parse( 2, "4.1" );

    assertEquals( address.getSchemaId(), 2 );
    assertEquals( address.getChannelId(), 4 );
    assertEquals( address.getId(), (Integer) 1 );
  }

  @Test
  public void parse()
  {
    final ChannelAddress address = ChannelAddress.parse( 4, "77" );

    assertEquals( address.getSchemaId(), 4 );
    assertEquals( address.getChannelId(), 77 );
    assertEquals( address.getId(), null );
  }

  @Test
  public void getCacheKey()
  {
    final ChannelAddress address = new ChannelAddress( 2, 4, 1 );
    assertEquals( address.getCacheKey(), "RC-2.4.1" );
  }

  @SuppressWarnings( "EqualsWithItself" )
  @Test
  public void testEquals()
  {
    final ChannelAddress address = new ChannelAddress( 1, 2, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 2, 2 );
    final ChannelAddress address3 = new ChannelAddress( 1, 1 );

    assertTrue( address.equals( address ) );
    assertFalse( address.equals( new Object() ) );
    assertFalse( address.equals( address2 ) );
    assertFalse( address.equals( address3 ) );
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
  }

  @SuppressWarnings( "EqualsWithItself" )
  @Test
  public void compareTo()
  {
    final ChannelAddress address1 = new ChannelAddress( 1, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 2, 1 );

    assertEquals( address1.compareTo( address1 ), 0 );
    assertEquals( address1.compareTo( address2 ), -1 );
    assertEquals( address2.compareTo( address1 ), 1 );
    assertEquals( address2.compareTo( address2 ), 0 );
  }
}
