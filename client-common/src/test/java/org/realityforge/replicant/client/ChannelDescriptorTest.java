package org.realityforge.replicant.client;

import org.realityforge.replicant.client.transport.TestGraph;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChannelDescriptorTest
{
  @SuppressWarnings( "EqualsWithItself" )
  @Test
  public void basicOperation()
  {
    final ChannelDescriptor descriptor1 = new ChannelDescriptor( TestGraph.A );
    final ChannelDescriptor descriptor2 = new ChannelDescriptor( TestGraph.B, 1 );

    assertEquals( descriptor1.getSystem(), TestGraph.class );
    assertEquals( descriptor1.getGraph(), TestGraph.A );
    assertEquals( descriptor1.getID(), null );
    assertEquals( descriptor1.toString(), "A" );
    assertEquals( descriptor1.equals( descriptor1 ), true );
    assertEquals( descriptor1.equals( descriptor2 ), false );

    assertEquals( descriptor2.getSystem(), TestGraph.class );
    assertEquals( descriptor2.getGraph(), TestGraph.B );
    assertEquals( descriptor2.getID(), 1 );
    assertEquals( descriptor2.toString(), "B:1" );
    assertEquals( descriptor2.equals( descriptor1 ), false );
    assertEquals( descriptor2.equals( descriptor2 ), true );
  }
}
