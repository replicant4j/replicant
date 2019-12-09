package org.realityforge.replicant.server;

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
    final ReplicantSession session = new ReplicantSession( null, ValueUtil.randomString() );
    final ChangeAccumulator accumulator = new ChangeAccumulator();

    final int id = 17;
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );

    final int channelId = 1;
    final Integer subChannelId = 2;
    final Integer requestId = 3;

    accumulator.addChange( session, new Change( message, channelId, subChannelId ) );
    final boolean impactsInitiator = accumulator.complete( session, requestId );

    assertTrue( impactsInitiator );
    assertEquals( session.getQueue().size(), 1 );
    final Packet packet = session.getQueue().nextPacketToProcess();
    final Change change = packet.getChangeSet().getChanges().iterator().next();
    assertEquals( change.getKey(), "42#17" );
    assertEquals( change.getEntityMessage().getId(), id );
    assertEquals( change.getEntityMessage().getTypeId(), typeID );
    assertEquals( packet.getRequestId(), requestId );
    final Map<Integer, Integer> channels = change.getChannels();
    assertEquals( channels.size(), 1 );
    assertEquals( channels.get( channelId ), subChannelId );

    accumulator.complete( null, null );
    assertEquals( session.getQueue().size(), 1 );
  }

  @Test
  public void addEntityMessages()
  {
    final ReplicantSession session = new ReplicantSession( null, ValueUtil.randomString() );
    final ChangeAccumulator accumulator = new ChangeAccumulator();

    final int id = 17;
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );

    final int channelId = 1;
    final Integer subChannelId = 0;
    final Integer requestId = 3;

    accumulator.addChanges( session, Collections.singletonList( new Change( message, channelId, subChannelId ) ) );

    assertEquals( accumulator.getChangeSet( session ).getChanges().size(), 1 );

    final boolean impactsInitiator = accumulator.complete( session, requestId );

    assertEquals( accumulator.getChangeSet( session ).getChanges().size(), 0 );

    assertTrue( impactsInitiator );
    assertEquals( session.getQueue().size(), 1 );
    final Packet packet = session.getQueue().nextPacketToProcess();
    assertEquals( packet.getChangeSet().getChanges().iterator().next().getEntityMessage().getId(), id );
    assertEquals( packet.getRequestId(), requestId );

    accumulator.complete( null, null );
    assertEquals( session.getQueue().size(), 1 );
  }

  @Test
  public void addActions()
  {
    final ReplicantSession session = new ReplicantSession( null, ValueUtil.randomString() );
    final ChangeAccumulator accumulator = new ChangeAccumulator();

    final JsonObject filter = Json.createBuilderFactory( null ).createObjectBuilder().build();
    final int channelId = 1;
    final Integer subChannelId = 2;
    final Integer requestId = 1;

    accumulator.addActions( session,
                            Collections.singletonList( new ChannelAction( new ChannelAddress( channelId, subChannelId ),
                                                                          Action.ADD,
                                                                          filter ) ) );

    assertEquals( accumulator.getChangeSet( session ).getChannelActions().size(), 1 );

    final boolean impactsInitiator = accumulator.complete( session, requestId );

    assertEquals( accumulator.getChangeSet( session ).getChannelActions().size(), 0 );

    assertTrue( impactsInitiator );
    assertEquals( session.getQueue().size(), 1 );
    final Packet packet = session.getQueue().nextPacketToProcess();
    final ChannelAction action = packet.getChangeSet().getChannelActions().iterator().next();
    assertEquals( action.getAddress().getChannelId(), channelId );
    assertEquals( action.getAction(), Action.ADD );
    assertEquals( action.getFilter(), filter );
    assertEquals( packet.getRequestId(), requestId );

    assertEquals( session.getQueue().size(), 1 );
    accumulator.complete( null, null );
    assertEquals( session.getQueue().size(), 1 );
  }

  @Test
  public void basicOperation_whereSessionDifferent()
  {
    final ReplicantSession session = new ReplicantSession( null, ValueUtil.randomString() );
    final ReplicantSession session2 = new ReplicantSession( null, ValueUtil.randomString() );
    final ChangeAccumulator accumulator = new ChangeAccumulator();

    final EntityMessage message = MessageTestUtil.createMessage( 17, 42, 0, "r1", "r2", "a1", "a2" );

    final int channelId = 1;
    final Integer subChannelId = 0;

    accumulator.addChange( session, new Change( message, channelId, subChannelId ) );
    final boolean impactsInitiator = accumulator.complete( session2, 1 );

    assertFalse( impactsInitiator );

    assertEquals( session.getQueue().size(), 1 );
    assertNull( session.getQueue().nextPacketToProcess().getRequestId() );
  }

  @Test
  public void basicOperation_whereNoMessagesSentToInitiator()
  {
    final ReplicantSession session = new ReplicantSession( null, ValueUtil.randomString() );
    final ChangeAccumulator accumulator = new ChangeAccumulator();

    final Integer requestId = 3;

    accumulator.getChangeSet( session );
    final boolean impactsInitiator = accumulator.complete( session, requestId );

    assertFalse( impactsInitiator );

    assertEquals( session.getQueue().size(), 0 );
  }
}
