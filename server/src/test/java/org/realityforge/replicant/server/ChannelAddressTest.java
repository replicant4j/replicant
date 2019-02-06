package org.realityforge.replicant.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChannelAddressTest
{
  @SuppressWarnings( "EqualsWithItself" )
  @Test
  public void basicOperation()
  {
    final ChannelAddress cd1 = new ChannelAddress( 1, 22 );
    final ChannelAddress cd2 = new ChannelAddress( 1, 22 );
    final ChannelAddress cd3 = new ChannelAddress( 1, 23 );
    final ChannelAddress cd4 = new ChannelAddress( 2 );
    final ChannelAddress cd5 = new ChannelAddress( 3 );
    final ChannelAddress cd6 = new ChannelAddress( 2 );

    assertEquals( cd1.getChannelId(), 1 );
    assertEquals( cd1.getSubChannelId(), (Integer) 22 );
    assertEquals( cd1.toString(), "#1.22#" );
    assertEquals( cd1, cd1 );
    assertEquals( cd2, cd1 );
    assertNotEquals( cd3, cd1 );
    assertNotEquals( cd4, cd1 );

    assertEquals( cd4.getChannelId(), 2 );
    assertNull( cd4.getSubChannelId() );
    assertEquals( cd4.toString(), "#2#" );
    assertEquals( cd4, cd4 );
    assertEquals( cd6, cd4 );
    assertNotEquals( cd3, cd4 );
    assertNotEquals( cd5, cd4 );

    final List<ChannelAddress> list = new ArrayList<>( Arrays.asList( cd6, cd5, cd4, cd3, cd2, cd1, cd6 ) );

    Collections.sort( list );

    final ChannelAddress[] expected = { cd1, cd2, cd3, cd4, cd6, cd6, cd5 };
    assertEquals( list.toArray( new ChannelAddress[ 0 ] ), expected );
  }
}
