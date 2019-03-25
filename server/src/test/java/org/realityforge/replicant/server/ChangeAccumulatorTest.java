package org.realityforge.replicant.server;

import java.io.IOException;
import java.util.Collections;
import javax.annotation.Nonnull;
import javax.json.Json;
import javax.json.JsonObject;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.server.ChannelAction.Action;
import org.realityforge.replicant.server.transport.ReplicantSession;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ChangeAccumulatorTest
{
  @Test
  public void basicOperation()
    throws IOException
  {
    final Session webSocketSession = createSession();

    final ReplicantSession c = new ReplicantSession( null, webSocketSession );
    final ChangeAccumulator accumulator = new ChangeAccumulator();

    final int id = 17;
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );

    final int channelId = 1;
    final Integer subChannelId = 2;

    accumulator.addChange( c, new Change( message, channelId, subChannelId ) );
    final boolean impactsInitiator = accumulator.complete( c, 1 );

    assertTrue( impactsInitiator );

    final RemoteEndpoint.Basic remote = webSocketSession.getBasicRemote();
    verify( remote ).sendText(
      "{\"type\":\"update\",\"requestId\":1,\"changes\":[{\"id\":\"42.17\",\"channels\":[\"1.2\"],\"data\":{\"ATTR_KEY2\":\"a2\",\"ATTR_KEY1\":\"a1\"}}]}" );
    reset( remote );
    accumulator.complete( null, null );
    verify( remote, never() ).sendText( anyString() );
  }

  @Test
  public void addEntityMessages()
    throws IOException
  {
    final Session webSocketSession = createSession();
    final ReplicantSession c = new ReplicantSession( null, webSocketSession );
    final ChangeAccumulator accumulator = new ChangeAccumulator();

    final int id = 17;
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );

    accumulator.addChanges( c, Collections.singletonList( new Change( message, 1, 0 ) ) );

    assertEquals( accumulator.getChangeSet( c ).getChanges().size(), 1 );

    final boolean impactsInitiator = accumulator.complete( c, 1 );

    assertEquals( accumulator.getChangeSet( c ).getChanges().size(), 0 );

    assertTrue( impactsInitiator );

    final RemoteEndpoint.Basic remote = webSocketSession.getBasicRemote();
    verify( remote ).sendText(
      "{\"type\":\"update\",\"requestId\":1,\"changes\":[{\"id\":\"42.17\",\"channels\":[\"1.0\"],\"data\":{\"ATTR_KEY2\":\"a2\",\"ATTR_KEY1\":\"a1\"}}]}" );
    reset( remote );

    accumulator.complete( null, null );

    verify( remote, never() ).sendText( anyString() );
  }

  @Test
  public void addActions()
    throws IOException
  {
    final Session webSocketSession = createSession();

    final ReplicantSession c = new ReplicantSession( null, webSocketSession );
    final ChangeAccumulator accumulator = new ChangeAccumulator();

    final JsonObject filter = Json.createBuilderFactory( null ).createObjectBuilder().build();
    accumulator.addActions( c,
                            Collections.singletonList( new ChannelAction( new ChannelAddress( 1, 2 ),
                                                                          Action.ADD,
                                                                          filter ) ) );

    assertEquals( accumulator.getChangeSet( c ).getChannelActions().size(), 1 );

    final boolean impactsInitiator = accumulator.complete( c, 1 );

    assertEquals( accumulator.getChangeSet( c ).getChannelActions().size(), 0 );

    assertTrue( impactsInitiator );

    final RemoteEndpoint.Basic remote = webSocketSession.getBasicRemote();
    verify( remote ).sendText(
      "{\"type\":\"update\",\"requestId\":1,\"fchannels\":[{\"channel\":\"+1.2\",\"filter\":{}}]}" );
    reset( remote );

    accumulator.complete( null, null );

    verify( remote, never() ).sendText( anyString() );
  }

  @Test
  public void basicOperation_whereSessionIDDifferent()
    throws IOException
  {
    final Session webSocketSession = createSession();

    final ReplicantSession c = new ReplicantSession( null, webSocketSession );
    final ChangeAccumulator accumulator = new ChangeAccumulator();

    final EntityMessage message = MessageTestUtil.createMessage( 17, 42, 0, "r1", "r2", "a1", "a2" );

    accumulator.addChange( c, new Change( message, 1, 0 ) );
    final boolean impactsInitiator = accumulator.complete( new ReplicantSession( null, createSession() ), 1 );

    assertFalse( impactsInitiator );

    final RemoteEndpoint.Basic remote = webSocketSession.getBasicRemote();
    verify( remote ).sendText(
      "{\"type\":\"update\",\"changes\":[{\"id\":\"42.17\",\"channels\":[\"1.0\"],\"data\":{\"ATTR_KEY2\":\"a2\",\"ATTR_KEY1\":\"a1\"}}]}" );
    reset( remote );

    accumulator.complete( null, null );

    verify( remote, never() ).sendText( anyString() );
  }

  @Test
  public void basicOperation_whereNoMessagesSentToInitiator()
    throws IOException
  {
    final Session webSocketSession = createSession();

    final ReplicantSession c = new ReplicantSession( null, webSocketSession );
    final ChangeAccumulator accumulator = new ChangeAccumulator();

    accumulator.getChangeSet( c );
    final boolean impactsInitiator = accumulator.complete( c, 1 );

    assertFalse( impactsInitiator );

    verify( webSocketSession.getBasicRemote(), never() ).sendText( anyString() );
  }

  @Nonnull
  private Session createSession()
  {
    final Session webSocketSession = mock( Session.class );
    when( webSocketSession.getId() ).thenReturn( ValueUtil.randomString() );
    when( webSocketSession.isOpen() ).thenReturn( true );
    final RemoteEndpoint.Basic remote = mock( RemoteEndpoint.Basic.class );
    when( webSocketSession.getBasicRemote() ).thenReturn( remote );
    return webSocketSession;
  }
}
