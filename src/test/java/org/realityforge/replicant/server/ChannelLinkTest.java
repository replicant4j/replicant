package org.realityforge.replicant.server;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChannelLinkTest
{
  @Test
  public void basicOperation()
  {
    final ChannelLink link = new ChannelLink( new ChannelDescriptor( 22, 44 ), new ChannelDescriptor( 1, "2" ) );
    assertEquals( link.getSourceChannel().getChannelID(), 22 );
    assertEquals( link.getSourceChannel().getSubChannelID(), 44 );
    assertEquals( link.getTargetChannel().getChannelID(), 1 );
    assertEquals( link.getTargetChannel().getSubChannelID(), "2" );
  }

  @Test( expectedExceptions = IllegalArgumentException.class )
  public void badTargetDescriptor()
  {
    new ChannelLink( new ChannelDescriptor( 22, 44 ), new ChannelDescriptor( 1, null ) );
  }

  @Test( expectedExceptions = IllegalArgumentException.class )
  public void badSourceDescriptor()
  {
    new ChannelLink( new ChannelDescriptor( 22, null ), new ChannelDescriptor( 1, "2" ) );
  }
}
