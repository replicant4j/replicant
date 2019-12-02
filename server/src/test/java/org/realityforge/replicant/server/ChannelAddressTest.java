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
    final ChannelAddress cd1 = new ChannelAddress( 1, "X" );
    final ChannelAddress cd2 = new ChannelAddress( 1, "X" );
    final ChannelAddress cd3 = new ChannelAddress( 1, "Y" );
    final ChannelAddress cd4 = new ChannelAddress( 2, null );
    final ChannelAddress cd5 = new ChannelAddress( 3, null );
    final ChannelAddress cd6 = new ChannelAddress( 2, null );

    assertEquals( cd1.getChannelId(), 1 );
    assertEquals( cd1.getSubChannelId(), "X" );
    assertEquals( cd1.toString(), "#1.X#" );
    assertTrue( cd1.equals( cd1 ) );
    assertTrue( cd1.equals( cd2 ) );
    assertFalse( cd1.equals( cd3 ) );
    assertFalse( cd1.equals( cd4 ) );

    assertEquals( cd4.getChannelId(), 2 );
    assertEquals( cd4.getSubChannelId(), null );
    assertEquals( cd4.toString(), "#2#" );
    assertTrue( cd4.equals( cd4 ) );
    assertTrue( cd4.equals( cd6 ) );
    assertFalse( cd4.equals( cd3 ) );
    assertFalse( cd4.equals( cd5 ) );

    final List<ChannelAddress> list = new ArrayList<>( Arrays.asList( cd6, cd5, cd4, cd3, cd2, cd1, cd6 ) );

    Collections.sort( list );

    final ChannelAddress[] expected = { cd1, cd2, cd3, cd4, cd6, cd6, cd5 };
    assertEquals( list.toArray( new ChannelAddress[ 0 ] ), expected );
  }
}
