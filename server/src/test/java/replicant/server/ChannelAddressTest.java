package replicant.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChannelAddressTest
{
  @Test
  public void basicOperation()
  {
    final ChannelAddress cd1 = new ChannelAddress( 1, 22, "a" );
    final ChannelAddress cd2 = new ChannelAddress( 1, 22, "a" );
    final ChannelAddress cd3 = new ChannelAddress( 1, 23, "a" );
    final ChannelAddress cd4 = new ChannelAddress( 2 );
    final ChannelAddress cd5 = new ChannelAddress( 3 );
    final ChannelAddress cd6 = new ChannelAddress( 2 );
    final ChannelAddress cd7 = new ChannelAddress( 1, 22, "b" );

    assertEquals( cd1.channelId(), 1 );
    assertEquals( cd1.rootId(), (Integer) 22 );
    assertTrue( cd1.hasRootId() );
    assertEquals( cd1.toString(), "1.22#a" );
    assertEquals( cd1, cd1 );
    assertEquals( cd2, cd1 );
    assertNotEquals( cd3, cd1 );
    assertNotEquals( cd4, cd1 );
    assertNotEquals( cd7, cd1 );

    assertEquals( cd4.channelId(), 2 );
    assertNull( cd4.rootId() );
    assertFalse( cd4.hasRootId() );
    assertEquals( cd4.toString(), "2" );
    assertEquals( cd4, cd4 );
    assertEquals( cd6, cd4 );
    assertNotEquals( cd3, cd4 );
    assertNotEquals( cd5, cd4 );

    final List<ChannelAddress> list = new ArrayList<>( Arrays.asList( cd6, cd5, cd4, cd3, cd2, cd1, cd6, cd7 ) );

    Collections.sort( list );

    final ChannelAddress[] expected = { cd1, cd2, cd7, cd3, cd4, cd6, cd6, cd5 };
    assertEquals( list.toArray( new ChannelAddress[ 0 ] ), expected );
  }

  @Test
  public void parse()
  {
    final ChannelAddress address1 = ChannelAddress.parse( "1.22" );
    assertEquals( address1.channelId(), 1 );
    assertEquals( address1.rootId(), (Integer) 22 );
    assertNull( address1.filterInstanceId() );
    final ChannelAddress address2 = ChannelAddress.parse( "0" );
    assertEquals( address2.channelId(), 0 );
    assertEquals( address2.rootId(), null );
    assertNull( address2.filterInstanceId() );
  }

  @Test
  public void parseWithInstanceId()
  {
    final ChannelAddress address1 = ChannelAddress.parse( "1.22#alpha" );
    assertEquals( address1.channelId(), 1 );
    assertEquals( address1.rootId(), (Integer) 22 );
    assertEquals( address1.filterInstanceId(), "alpha" );
    final ChannelAddress address2 = ChannelAddress.parse( "0#alpha" );
    assertEquals( address2.channelId(), 0 );
    assertEquals( address2.rootId(), null );
    assertEquals( address2.filterInstanceId(), "alpha" );
  }
}
