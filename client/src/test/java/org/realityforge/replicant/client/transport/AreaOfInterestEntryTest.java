package org.realityforge.replicant.client.transport;

import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ChannelAddress;
import static org.testng.Assert.*;

public class AreaOfInterestEntryTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final ChannelAddress address = new ChannelAddress( TestSystem.A );
    final Object filterParameter = null;
    final AreaOfInterestAction action = AreaOfInterestAction.ADD;

    final AreaOfInterestEntry entry = new AreaOfInterestEntry( address, action, filterParameter );

    assertEquals( entry.getAddress(), address );
    assertEquals( entry.getAction(), action );
    assertEquals( entry.getCacheKey(), "TestSystem:TestSystem.A" );
    assertEquals( entry.getFilterParameter(), filterParameter );
    assertEquals( entry.match( action, address, filterParameter ), true );
    assertEquals( entry.match( action, address, "OtherFilter" ), false );
    assertEquals( entry.match( AreaOfInterestAction.REMOVE, address, filterParameter ), false );
    assertEquals( entry.match( action, new ChannelAddress( TestSystem.B, "X" ), filterParameter ), false );
  }

  @Test
  public void removeActionIgnoredFilterDuringMatch()
  {
    final ChannelAddress address = new ChannelAddress( TestSystem.A );
    final AreaOfInterestAction action = AreaOfInterestAction.REMOVE;

    final AreaOfInterestEntry entry = new AreaOfInterestEntry( address, action, null );

    assertEquals( entry.match( action, address, null ), true );
    assertEquals( entry.match( action, address, "OtherFilter" ), true );
    assertEquals( entry.match( AreaOfInterestAction.ADD, address, null ), false );
  }
}
