package org.realityforge.replicant.client.transport;

import org.realityforge.replicant.client.ChannelAddress;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class AreaOfInterestEntryTest
{
  @Test
  public void basicOperation()
  {
    final String systemKey = "Foo";
    final ChannelAddress descriptor = new ChannelAddress( TestGraph.A );
    final Object filterParameter = null;
    final AreaOfInterestAction action = AreaOfInterestAction.ADD;

    final AreaOfInterestEntry entry =
      new AreaOfInterestEntry( systemKey, descriptor, action, filterParameter );

    assertEquals( entry.getSystemKey(), systemKey );
    assertEquals( entry.getDescriptor(), descriptor );
    assertEquals( entry.getAction(), action );
    assertEquals( entry.getCacheKey(), "Foo:TestGraph.A" );
    assertEquals( entry.getFilterParameter(), filterParameter );
    assertTrue( entry.match( action, descriptor, filterParameter ) );
    assertFalse( entry.match( action, descriptor, "OtherFilter" ) );
    assertFalse( entry.match( AreaOfInterestAction.REMOVE, descriptor, filterParameter ) );
    assertFalse( entry.match( action, new ChannelAddress( TestGraph.B, "X" ), filterParameter ) );
  }

   @Test
  public void removeActionIgnoredFilterDuringMatch()
  {
    final ChannelAddress descriptor = new ChannelAddress( TestGraph.A );
    final AreaOfInterestAction action = AreaOfInterestAction.REMOVE;

    final AreaOfInterestEntry entry =
      new AreaOfInterestEntry( "Foo", descriptor, action, null );

    assertTrue( entry.match( action, descriptor, null ) );
    assertTrue( entry.match( action, descriptor, "OtherFilter" ) );
    assertFalse( entry.match( AreaOfInterestAction.ADD, descriptor, null ) );
  }
}
