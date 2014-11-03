package org.realityforge.replicant.server;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChannelLinkTest
{
  @Test
  public void basicOperation()
  {
    final ChannelLink link = new ChannelLink( new ChannelDescriptor( 1, "2" ) );
    assertEquals( link.getTargetChannel().getChannelID(), 1 );
    assertEquals( link.getTargetChannel().getSubChannelID(), "2" );
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void badDescriptor()
  {
    new ChannelLink( new ChannelDescriptor( 1, null ) );
  }
}
