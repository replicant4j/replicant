package org.realityforge.replicant.server;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import org.realityforge.replicant.server.ChannelAction.Action;
import org.realityforge.replicant.server.transport.Packet;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChangeAccumulatorTest
{
  @Test
  public void basicOperation()
  {
    final TestSession c = new TestSession( "s1" );
    final ChangeAccumulator accumulator = new ChangeAccumulator();

    final String id = "myID";
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );

    final int channelID = 1;
    final String subChannelID = "2";

    accumulator.addChange( c, new Change( message, channelID, subChannelID ) );
    final boolean impactsInitiator = accumulator.complete( "s1", "j1" );

    assertTrue( impactsInitiator );
    assertEquals( c.getQueue().size(), 1 );
    final Packet packet = c.getQueue().nextPacketToProcess();
    final Change change = packet.getChangeSet().getChanges().iterator().next();
    assertEquals( change.getID(), "42#myID" );
    assertEquals( change.getEntityMessage().getID(), id );
    assertEquals( change.getEntityMessage().getTypeID(), typeID );
    assertEquals( packet.getRequestID(), "j1" );
    final Map<Integer, Serializable> channels = change.getChannels();
    assertEquals( channels.size(), 1 );
    assertEquals( channels.get( channelID ), subChannelID );

    accumulator.complete( null, null );
    assertEquals( c.getQueue().size(), 1 );
  }

  @Test
  public void addEntityMessages()
  {
    final TestSession c = new TestSession( "s1" );
    final ChangeAccumulator accumulator = new ChangeAccumulator();

    final String id = "myID";
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );

    accumulator.addChanges( c, Arrays.asList( new Change( message, 1, 0 ) ) );
    final boolean impactsInitiator = accumulator.complete( "s1", "j1" );

    assertTrue( impactsInitiator );
    assertEquals( c.getQueue().size(), 1 );
    final Packet packet = c.getQueue().nextPacketToProcess();
    assertEquals( packet.getChangeSet().getChanges().iterator().next().getEntityMessage().getID(), id );
    assertEquals( packet.getRequestID(), "j1" );

    accumulator.complete( null, null );
    assertEquals( c.getQueue().size(), 1 );
  }

  @Test
  public void addActions()
  {
    final TestSession c = new TestSession( "s1" );
    final ChangeAccumulator accumulator = new ChangeAccumulator();

    accumulator.addActions( c, Arrays.asList( new ChannelAction( new ChannelDescriptor( 1, 2 ), Action.ADD ) ) );
    final boolean impactsInitiator = accumulator.complete( "s1", "j1" );

    assertTrue( impactsInitiator );
    assertEquals( c.getQueue().size(), 1 );
    final Packet packet = c.getQueue().nextPacketToProcess();
    final ChannelAction action = packet.getChangeSet().getChannelActions().iterator().next();
    assertEquals( action.getChannelDescriptor().getChannelID(), 1 );
    assertEquals( action.getAction(), Action.ADD );
    assertEquals( packet.getRequestID(), "j1" );

    accumulator.complete( null, null );
    assertEquals( c.getQueue().size(), 1 );
  }

  @Test
  public void basicOperation_whereSessionIDDifferent()
  {
    final TestSession c = new TestSession( "s1" );
    final ChangeAccumulator accumulator = new ChangeAccumulator();

    final EntityMessage message = MessageTestUtil.createMessage( "myID", 42, 0, "r1", "r2", "a1", "a2" );

    accumulator.addChange( c, new Change( message, 1, 0 ) );
    final boolean impactsInitiator = accumulator.complete( "s2", "j1" );

    assertFalse( impactsInitiator );

    assertEquals( c.getQueue().size(), 1 );
    assertNull( c.getQueue().nextPacketToProcess().getRequestID() );
  }
}
