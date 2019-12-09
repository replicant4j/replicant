package org.realityforge.replicant.server;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonObject;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.server.ChannelAction.Action;
import org.realityforge.replicant.server.transport.Packet;
import org.realityforge.replicant.server.transport.ReplicantSession;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChangeAccumulatorTest
{
  @Test
  public void basicOperation()
  {
    final ReplicantSession c = new ReplicantSession( null, "s1" );
    final ChangeAccumulator accumulator = new ChangeAccumulator();

    final int id = 17;
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );

    final int channelId = 1;
    final Integer subChannelId = 2;

    accumulator.addChange( c, new Change( message, channelId, subChannelId ) );
    final Integer requestId = ValueUtil.randomInt();
    final boolean impactsInitiator = accumulator.complete( "s1", requestId );

    assertTrue( impactsInitiator );
    assertEquals( c.getQueue().size(), 1 );
    final Packet packet = c.getQueue().nextPacketToProcess();
    final Change change = packet.getChangeSet().getChanges().iterator().next();
    assertEquals( change.getKey(), "42#17" );
    assertEquals( change.getEntityMessage().getId(), id );
    assertEquals( change.getEntityMessage().getTypeId(), typeID );
    assertEquals( packet.getRequestId(), requestId );
    final Map<Integer, Integer> channels = change.getChannels();
    assertEquals( channels.size(), 1 );
    assertEquals( channels.get( channelId ), subChannelId );

    accumulator.complete( null, null );
    assertEquals( c.getQueue().size(), 1 );
  }

  @Test
  public void addEntityMessages()
  {
    final ReplicantSession c = new ReplicantSession( null, "s1" );
    final ChangeAccumulator accumulator = new ChangeAccumulator();

    final int id = 17;
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );

    accumulator.addChanges( c, Collections.singletonList( new Change( message, 1, 0 ) ) );

    assertEquals( accumulator.getChangeSet( c ).getChanges().size(), 1 );

    final Integer requestId = ValueUtil.randomInt();
    final boolean impactsInitiator = accumulator.complete( "s1", requestId );

    assertEquals( accumulator.getChangeSet( c ).getChanges().size(), 0 );

    assertTrue( impactsInitiator );
    assertEquals( c.getQueue().size(), 1 );
    final Packet packet = c.getQueue().nextPacketToProcess();
    assertEquals( packet.getChangeSet().getChanges().iterator().next().getEntityMessage().getId(), id );
    assertEquals( packet.getRequestId(), requestId );

    accumulator.complete( null, null );
    assertEquals( c.getQueue().size(), 1 );
  }

  @Test
  public void addActions()
  {
    final ReplicantSession c = new ReplicantSession( null, "s1" );
    final ChangeAccumulator accumulator = new ChangeAccumulator();

    final JsonObject filter = Json.createBuilderFactory( null ).createObjectBuilder().build();
    accumulator.addActions( c,
                            Collections.singletonList( new ChannelAction( new ChannelAddress( 1, 2 ),
                                                                          Action.ADD,
                                                                          filter ) ) );

    assertEquals( accumulator.getChangeSet( c ).getChannelActions().size(), 1 );

    final Integer requestId = ValueUtil.randomInt();
    final boolean impactsInitiator = accumulator.complete( "s1", requestId );

    assertEquals( accumulator.getChangeSet( c ).getChannelActions().size(), 0 );

    assertTrue( impactsInitiator );
    assertEquals( c.getQueue().size(), 1 );
    final Packet packet = c.getQueue().nextPacketToProcess();
    final ChannelAction action = packet.getChangeSet().getChannelActions().iterator().next();
    assertEquals( action.getAddress().getChannelId(), 1 );
    assertEquals( action.getAction(), Action.ADD );
    assertEquals( action.getFilter(), filter );
    assertEquals( packet.getRequestId(), requestId );

    assertEquals( c.getQueue().size(), 1 );
    accumulator.complete( null, null );
    assertEquals( c.getQueue().size(), 1 );
  }

  @Test
  public void basicOperation_whereSessionIDDifferent()
  {
    final ReplicantSession c = new ReplicantSession( null, "s1" );
    final ChangeAccumulator accumulator = new ChangeAccumulator();

    final EntityMessage message = MessageTestUtil.createMessage( 17, 42, 0, "r1", "r2", "a1", "a2" );

    accumulator.addChange( c, new Change( message, 1, 0 ) );
    final Integer requestId = ValueUtil.randomInt();
    final boolean impactsInitiator = accumulator.complete( "s2", requestId );

    assertFalse( impactsInitiator );

    assertEquals( c.getQueue().size(), 1 );
    assertNull( c.getQueue().nextPacketToProcess().getRequestId() );
  }

  @Test
  public void basicOperation_whereNoMessagesSentToInitiator()
  {
    final ReplicantSession c = new ReplicantSession( null, "s1" );
    final ChangeAccumulator accumulator = new ChangeAccumulator();

    accumulator.getChangeSet( c );
    final Integer requestId = ValueUtil.randomInt();
    final boolean impactsInitiator = accumulator.complete( "s1", requestId );

    assertFalse( impactsInitiator );

    assertEquals( c.getQueue().size(), 0 );
  }
}
