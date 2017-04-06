package org.realityforge.replicant.client.transport;

import org.realityforge.replicant.client.ChannelDescriptor;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class AreaOfInterestEntryTest
{
  @Test
  public void basicOperation()
  {
    final String systemKey = "Foo";
    final ChannelDescriptor descriptor = new ChannelDescriptor( TestGraph.A, null );
    final Object filterParameter = null;
    final AreaOfInterestAction action = AreaOfInterestAction.ADD;

    final AreaOfInterestEntry entry =
      new AreaOfInterestEntry( systemKey, descriptor, action, filterParameter );

    assertEquals( entry.getSystemKey(), systemKey );
    assertEquals( entry.getDescriptor(), descriptor );
    assertEquals( entry.getAction(), action );
    assertEquals( entry.getCacheKey(), "Foo:A" );
    assertEquals( entry.getFilterParameter(), filterParameter );
    assertEquals( entry.match( descriptor, action ), true );
    assertEquals( entry.match( descriptor, AreaOfInterestAction.REMOVE ), false );
    assertEquals( entry.match( new ChannelDescriptor( TestGraph.B, "X" ), action ), false );
  }
}
