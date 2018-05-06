package org.realityforge.replicant.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChannelDescriptorTest
{
  @SuppressWarnings( "EqualsWithItself" )
  @Test
  public void basicOperation()
  {
    final ChannelDescriptor cd1 = new ChannelDescriptor( 1, 22 );
    final ChannelDescriptor cd2 = new ChannelDescriptor( 1, 22 );
    final ChannelDescriptor cd3 = new ChannelDescriptor( 1, 23 );
    final ChannelDescriptor cd4 = new ChannelDescriptor( 2 );
    final ChannelDescriptor cd5 = new ChannelDescriptor( 3 );
    final ChannelDescriptor cd6 = new ChannelDescriptor( 2 );

    assertEquals( cd1.getChannelId(), 1 );
    assertEquals( cd1.getSubChannelId(), (Integer) 22 );
    assertEquals( cd1.toString(), "#1.22#" );
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

    final List<ChannelDescriptor> list = new ArrayList<>( Arrays.asList( cd6, cd5, cd4, cd3, cd2, cd1, cd6 ) );

    Collections.sort( list );

    final ChannelDescriptor[] expected = { cd1, cd2, cd3, cd4, cd6, cd6, cd5 };
    assertEquals( list.toArray( new ChannelDescriptor[ 0 ] ), expected );
  }
}
