package replicant.server.ee;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.event.Event;
import javax.json.Json;
import javax.json.JsonObject;
import javax.websocket.CloseReason;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import org.testng.annotations.Test;
import replicant.server.ChannelAddress;
import replicant.server.transport.ChannelMetaData;
import replicant.server.transport.ReplicantSession;
import replicant.server.transport.ReplicantSessionManager;
import replicant.server.transport.SchemaMetaData;
import replicant.shared.Messages;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public final class ReplicantEndpointTest
{
  @Test
  public void onOpen_createsSessionAndSendsSessionCreated()
  {
    final var fixture = newFixture();

    fixture.endpoint.onOpen( fixture.session );

    verify( fixture.sessionManager ).createSession( fixture.session );
    verify( fixture.addedEvent ).fire( new ReplicantSessionAdded( fixture.sessionId ) );

    final var response = getLastSentMessage( fixture );
    assertEquals( response.getString( Messages.Common.TYPE ), Messages.S2C_Type.SESSION_CREATED );
    assertEquals( response.getString( Messages.S2C_Common.SESSION_ID ), fixture.sessionId );
  }

  @Test
  public void onClose_withSession()
  {
    final var fixture = newFixture();

    fixture.endpoint.onClose( fixture.session );

    verify( fixture.sessionManager ).invalidateSession( fixture.replicantSession );
    verify( fixture.removedEvent ).fire( new ReplicantSessionRemoved( fixture.sessionId ) );
  }

  @Test
  public void onClose_withoutSession()
  {
    final var fixture = newFixture();
    when( fixture.sessionManager.getSession( fixture.sessionId ) ).thenReturn( null );

    fixture.endpoint.onClose( fixture.session );

    verify( fixture.sessionManager, never() ).invalidateSession( fixture.replicantSession );
  }

  @Test
  public void onError_sendsErrorAndCloses()
    throws Exception
  {
    final var fixture = newFixture();

    fixture.endpoint.onError( fixture.session, new RuntimeException( "Boom" ) );

    verify( fixture.session ).close( any( CloseReason.class ) );
    verify( fixture.sessionManager ).invalidateSession( fixture.replicantSession );
    verify( fixture.removedEvent ).fire( new ReplicantSessionRemoved( fixture.sessionId ) );

    final var response = getLastSentMessage( fixture );
    assertEquals( response.getString( Messages.Common.TYPE ), Messages.S2C_Type.ERROR );
    assertTrue( response.getString( Messages.S2C_Common.MESSAGE ).contains( "Boom" ) );
    verify( fixture.updatedEvent, never() ).fire( any() );
  }

  @Test
  public void command_unknownSession()
    throws Exception
  {
    final var fixture = newFixture();
    when( fixture.sessionManager.getSession( fixture.sessionId ) ).thenThrow( new IllegalStateException() );

    fixture.endpoint.command( fixture.session, "{\"type\":\"ping\",\"requestId\":1}" );

    verify( fixture.session ).close( any( CloseReason.class ) );
    verify( fixture.sessionManager, never() ).invalidateSession( any() );
    final var response = getLastSentMessage( fixture );
    assertEquals( response.getString( Messages.Common.TYPE ), Messages.S2C_Type.ERROR );
    assertEquals( response.getString( Messages.S2C_Common.MESSAGE ), "Unable to locate associated replicant session" );
  }

  @Test
  public void command_malformedMessage()
    throws Exception
  {
    final var fixture = newFixture();

    fixture.endpoint.command( fixture.session, "not-json" );

    verify( fixture.session ).close( any( CloseReason.class ) );
    verify( fixture.sessionManager ).invalidateSession( fixture.replicantSession );
    verify( fixture.removedEvent ).fire( new ReplicantSessionRemoved( fixture.sessionId ) );
    verify( fixture.updatedEvent, never() ).fire( any() );

    final var response = getLastSentMessage( fixture );
    assertEquals( response.getString( Messages.Common.TYPE ), Messages.S2C_Type.MALFORMED_MESSAGE );
    assertEquals( response.getString( Messages.S2C_Common.MESSAGE ), "not-json" );
  }

  @Test
  public void command_unauthorized()
    throws Exception
  {
    final var fixture = newFixture();
    when( fixture.sessionManager.isAuthorized( fixture.replicantSession ) ).thenReturn( false );

    fixture.endpoint.command( fixture.session, createPingCommand( 1 ) );

    verify( fixture.session ).close( any( CloseReason.class ) );
    verify( fixture.sessionManager ).invalidateSession( fixture.replicantSession );
    verify( fixture.removedEvent ).fire( new ReplicantSessionRemoved( fixture.sessionId ) );
    verify( fixture.updatedEvent, never() ).fire( any() );

    final var response = getLastSentMessage( fixture );
    assertEquals( response.getString( Messages.Common.TYPE ), Messages.S2C_Type.ERROR );
    assertEquals( response.getString( Messages.S2C_Common.MESSAGE ), "Replicant session not authorized" );
  }

  @Test
  public void command_auth()
    throws Exception
  {
    final var fixture = newFixture();
    final var command = Json.createObjectBuilder()
      .add( Messages.Common.TYPE, Messages.C2S_Type.AUTH )
      .add( Messages.Common.REQUEST_ID, 12 )
      .add( Messages.Auth.TOKEN, "token" )
      .build();

    fixture.endpoint.command( fixture.session, command.toString() );

    assertEquals( fixture.replicantSession.getAuthToken(), "token" );
    verify( fixture.updatedEvent ).fire( new ReplicantSessionUpdated( fixture.sessionId ) );

    final var response = getLastSentMessage( fixture );
    assertEquals( response.getString( Messages.Common.TYPE ), Messages.S2C_Type.OK );
    assertEquals( response.getInt( Messages.Common.REQUEST_ID ), 12 );
  }

  @Test
  public void command_exec_withPayload()
    throws Exception
  {
    final var fixture = newFixture();
    final var payload = Json.createObjectBuilder().add( "a", "b" ).build();
    final var command = Json.createObjectBuilder()
      .add( Messages.Common.TYPE, Messages.C2S_Type.EXEC )
      .add( Messages.Common.REQUEST_ID, 4 )
      .add( Messages.Common.COMMAND, "cmd" )
      .add( Messages.Exec.PAYLOAD, payload )
      .build();

    fixture.endpoint.command( fixture.session, command.toString() );

    verify( fixture.sessionManager ).execCommand( fixture.replicantSession, "cmd", 4, payload );
    verify( fixture.updatedEvent ).fire( new ReplicantSessionUpdated( fixture.sessionId ) );
  }

  @Test
  public void command_exec_withoutPayload()
    throws Exception
  {
    final var fixture = newFixture();
    final var command = Json.createObjectBuilder()
      .add( Messages.Common.TYPE, Messages.C2S_Type.EXEC )
      .add( Messages.Common.REQUEST_ID, 5 )
      .add( Messages.Common.COMMAND, "cmd" )
      .build();

    fixture.endpoint.command( fixture.session, command.toString() );

    verify( fixture.sessionManager ).execCommand( fixture.replicantSession, "cmd", 5, null );
    verify( fixture.updatedEvent ).fire( new ReplicantSessionUpdated( fixture.sessionId ) );
  }

  @Test
  public void command_etags()
    throws Exception
  {
    final var fixture = newFixture();
    final var etags = Json.createObjectBuilder().add( "1", "e1" ).add( "2.3#fi", "e2" ).build();
    final var command = Json.createObjectBuilder()
      .add( Messages.Common.TYPE, Messages.C2S_Type.ETAGS )
      .add( Messages.Common.REQUEST_ID, 9 )
      .add( Messages.Etags.ETAGS, etags )
      .build();

    fixture.endpoint.command( fixture.session, command.toString() );

    @SuppressWarnings( "unchecked" )
    final var captor =
      (org.mockito.ArgumentCaptor<Map<ChannelAddress, String>>) (Object) org.mockito.ArgumentCaptor.forClass( Map.class );
    verify( fixture.sessionManager ).setETags( eq( fixture.replicantSession ), captor.capture() );
    final Map<ChannelAddress, String> captured = captor.getValue();
    assertEquals( captured.get( new ChannelAddress( 1 ) ), "e1" );
    assertEquals( captured.get( new ChannelAddress( 2, 3, "fi" ) ), "e2" );

    final var response = getLastSentMessage( fixture );
    assertEquals( response.getString( Messages.Common.TYPE ), Messages.S2C_Type.OK );
    assertEquals( response.getInt( Messages.Common.REQUEST_ID ), 9 );
    verify( fixture.updatedEvent ).fire( new ReplicantSessionUpdated( fixture.sessionId ) );
  }

  @Test
  public void command_ping()
    throws Exception
  {
    final var fixture = newFixture();

    fixture.endpoint.command( fixture.session, createPingCommand( 7 ) );

    final var response = getLastSentMessage( fixture );
    assertEquals( response.getString( Messages.Common.TYPE ), Messages.S2C_Type.OK );
    assertEquals( response.getInt( Messages.Common.REQUEST_ID ), 7 );
    verify( fixture.updatedEvent ).fire( new ReplicantSessionUpdated( fixture.sessionId ) );
  }

  @Test
  public void command_subscribe_typeChannel()
    throws Exception
  {
    final var fixture = newFixture();
    final var command = createSubscribeCommand( "0", 1, null );

    fixture.endpoint.command( fixture.session, command.toString() );

    verify( fixture.sessionManager ).bulkSubscribe( fixture.replicantSession,
                                                    1,
                                                    Collections.singletonList( new ChannelAddress( 0 ) ),
                                                    null );
    verify( fixture.updatedEvent ).fire( new ReplicantSessionUpdated( fixture.sessionId ) );
  }

  @Test
  public void command_subscribe_withFilter()
    throws Exception
  {
    final var fixture = newFixture();
    final var filter = Json.createObjectBuilder().add( "k", "v" ).build();
    final var command = createSubscribeCommand( "1.5", 2, filter );

    fixture.endpoint.command( fixture.session, command.toString() );

    verify( fixture.sessionManager ).bulkSubscribe( fixture.replicantSession,
                                                    2,
                                                    Collections.singletonList( new ChannelAddress( 1, 5 ) ),
                                                    filter );
    verify( fixture.updatedEvent ).fire( new ReplicantSessionUpdated( fixture.sessionId ) );
  }

  @Test
  public void command_subscribe_ignoresFilterWhenNotSupported()
    throws Exception
  {
    final var fixture = newFixture();
    final var filter = Json.createObjectBuilder().add( "k", "v" ).build();
    final var command = createSubscribeCommand( "0", 3, filter );

    fixture.endpoint.command( fixture.session, command.toString() );

    verify( fixture.sessionManager ).bulkSubscribe( fixture.replicantSession,
                                                    3,
                                                    Collections.singletonList( new ChannelAddress( 0 ) ),
                                                    null );
  }

  @Test
  public void command_subscribe_internalChannel()
    throws Exception
  {
    assertInvalidSubscribe( "3", "Attempted to subscribe to internal-only channel" );
  }

  @Test
  public void command_subscribe_typeWithRoot()
    throws Exception
  {
    assertInvalidSubscribe( "0.1", "Attempted to subscribe to type channel with instance data" );
  }

  @Test
  public void command_subscribe_instanceWithoutRoot()
    throws Exception
  {
    assertInvalidSubscribe( "1", "Attempted to subscribe to instance channel without instance data" );
  }

  @Test
  public void command_subscribe_instancedWithoutFilterInstanceId()
    throws Exception
  {
    assertInvalidSubscribe( "2.7", "Attempted to use instanced channel without filter instance id" );
  }

  @Test
  public void command_subscribe_staticInstancedWithoutFilterInstanceId()
    throws Exception
  {
    assertInvalidSubscribe( "4.7", "Attempted to use instanced channel without filter instance id" );
  }

  @Test
  public void command_subscribe_nonInstancedWithFilterInstanceId()
    throws Exception
  {
    assertInvalidSubscribe( "1.5#fi", "Attempted to use non-instanced channel with filter instance id" );
  }

  @Test
  public void command_subscribe_staticInstancedWithFilterInstanceId()
    throws Exception
  {
    final var fixture = newFixture();
    final var filter = Json.createObjectBuilder().add( "k", "v" ).build();
    final var command = createSubscribeCommand( "4.7#fi", 5, filter );

    fixture.endpoint.command( fixture.session, command.toString() );

    verify( fixture.sessionManager ).bulkSubscribe( fixture.replicantSession,
                                                    5,
                                                    Collections.singletonList( new ChannelAddress( 4, 7, "fi" ) ),
                                                    filter );
  }

  @Test
  public void command_bulkSubscribe_success()
    throws Exception
  {
    final var fixture = newFixture();
    final var filter = Json.createObjectBuilder().add( "x", "y" ).build();
    final var command = Json.createObjectBuilder()
      .add( Messages.Common.TYPE, Messages.C2S_Type.BULK_SUB )
      .add( Messages.Common.REQUEST_ID, 4 )
      .add( Messages.Update.CHANNELS, Json.createArrayBuilder().add( "2.7#fi" ).add( "2.8#fi2" ) )
      .add( Messages.Update.FILTER, filter )
      .build();

    fixture.endpoint.command( fixture.session, command.toString() );

    final var expected = Arrays.asList( new ChannelAddress( 2, 7, "fi" ), new ChannelAddress( 2, 8, "fi2" ) );
    verify( fixture.sessionManager ).bulkSubscribe( fixture.replicantSession, 4, expected, filter );
    verify( fixture.updatedEvent ).fire( new ReplicantSessionUpdated( fixture.sessionId ) );
  }

  @Test
  public void command_bulkSubscribe_empty()
    throws Exception
  {
    final var fixture = newFixture();
    final var command = Json.createObjectBuilder()
      .add( Messages.Common.TYPE, Messages.C2S_Type.BULK_SUB )
      .add( Messages.Common.REQUEST_ID, 4 )
      .add( Messages.Update.CHANNELS, Json.createArrayBuilder() )
      .build();

    fixture.endpoint.command( fixture.session, command.toString() );

    verify( fixture.sessionManager, never() ).bulkSubscribe( any(), anyInt(), anyList(), any() );
    verify( fixture.updatedEvent ).fire( new ReplicantSessionUpdated( fixture.sessionId ) );
  }

  @Test
  public void command_bulkSubscribe_mixedChannels()
    throws Exception
  {
    final var fixture = newFixture();
    final var command = Json.createObjectBuilder()
      .add( Messages.Common.TYPE, Messages.C2S_Type.BULK_SUB )
      .add( Messages.Common.REQUEST_ID, 4 )
      .add( Messages.Update.CHANNELS, Json.createArrayBuilder().add( "2.7#fi" ).add( "1.5#fi2" ) )
      .build();

    fixture.endpoint.command( fixture.session, command.toString() );

    verify( fixture.sessionManager, never() ).bulkSubscribe( any(), anyInt(), anyList(), any() );
    verify( fixture.sessionManager ).invalidateSession( fixture.replicantSession );
    verify( fixture.removedEvent ).fire( new ReplicantSessionRemoved( fixture.sessionId ) );
    verify( fixture.updatedEvent ).fire( new ReplicantSessionUpdated( fixture.sessionId ) );

    final var response = getLastSentMessage( fixture );
    assertEquals( response.getString( Messages.Common.TYPE ), Messages.S2C_Type.ERROR );
    assertEquals( response.getString( Messages.S2C_Common.MESSAGE ),
                  "Bulk channel subscribe included addresses from multiple channels" );
  }

  @Test
  public void command_unsubscribe_success()
    throws Exception
  {
    final var fixture = newFixture();
    final var command = Json.createObjectBuilder()
      .add( Messages.Common.TYPE, Messages.C2S_Type.UNSUB )
      .add( Messages.Common.REQUEST_ID, 7 )
      .add( Messages.Common.CHANNEL, "1.5" )
      .build();

    fixture.endpoint.command( fixture.session, command.toString() );

    verify( fixture.sessionManager ).bulkUnsubscribe( fixture.replicantSession,
                                                      7,
                                                      Collections.singletonList( new ChannelAddress( 1, 5 ) ) );
    verify( fixture.updatedEvent ).fire( new ReplicantSessionUpdated( fixture.sessionId ) );
  }

  @Test
  public void command_unsubscribe_nonInstancedWithFilterInstanceId()
    throws Exception
  {
    final var fixture = newFixture();
    final var command = Json.createObjectBuilder()
      .add( Messages.Common.TYPE, Messages.C2S_Type.UNSUB )
      .add( Messages.Common.REQUEST_ID, 7 )
      .add( Messages.Common.CHANNEL, "1.5#fi" )
      .build();

    fixture.endpoint.command( fixture.session, command.toString() );

    verify( fixture.sessionManager, never() ).bulkUnsubscribe( any(), anyInt(), anyList() );
    verify( fixture.sessionManager ).invalidateSession( fixture.replicantSession );
    verify( fixture.removedEvent ).fire( new ReplicantSessionRemoved( fixture.sessionId ) );
    verify( fixture.updatedEvent ).fire( new ReplicantSessionUpdated( fixture.sessionId ) );
  }

  @Test
  public void command_bulkUnsubscribe_success()
    throws Exception
  {
    final var fixture = newFixture();
    final var command = Json.createObjectBuilder().add( Messages.Common.TYPE, Messages.C2S_Type.BULK_UNSUB ).add(
      Messages.Common.REQUEST_ID,
      8 ).add( Messages.Update.CHANNELS, Json.createArrayBuilder().add( "1.1" ).add( "1.2" ) ).build();

    fixture.endpoint.command( fixture.session, command.toString() );

    final var expected = Arrays.asList( new ChannelAddress( 1, 1 ), new ChannelAddress( 1, 2 ) );
    verify( fixture.sessionManager ).bulkUnsubscribe( fixture.replicantSession, 8, expected );
    verify( fixture.updatedEvent ).fire( new ReplicantSessionUpdated( fixture.sessionId ) );
  }

  @Test
  public void command_bulkUnsubscribe_mixedChannels()
    throws Exception
  {
    final var fixture = newFixture();
    final var command = Json.createObjectBuilder().add( Messages.Common.TYPE, Messages.C2S_Type.BULK_UNSUB ).add(
      Messages.Common.REQUEST_ID,
      8 ).add( Messages.Update.CHANNELS, Json.createArrayBuilder().add( "2.7#fi" ).add( "1.5#fi2" ) ).build();

    fixture.endpoint.command( fixture.session, command.toString() );

    verify( fixture.sessionManager, never() ).bulkUnsubscribe( any(), anyInt(), anyList() );
    verify( fixture.sessionManager ).invalidateSession( fixture.replicantSession );
    verify( fixture.removedEvent ).fire( new ReplicantSessionRemoved( fixture.sessionId ) );
    verify( fixture.updatedEvent ).fire( new ReplicantSessionUpdated( fixture.sessionId ) );

    final var response = getLastSentMessage( fixture );
    assertEquals( response.getString( Messages.Common.TYPE ), Messages.S2C_Type.ERROR );
    assertEquals( response.getString( Messages.S2C_Common.MESSAGE ),
                  "Bulk channel unsubscribe included addresses from multiple channels" );
  }

  @Test
  public void command_unknownType()
    throws Exception
  {
    final var fixture = newFixture();
    final var command = Json.createObjectBuilder().add( Messages.Common.TYPE, "wut" ).add( Messages.Common.REQUEST_ID,
                                                                                           9 ).build();

    fixture.endpoint.command( fixture.session, command.toString() );

    verify( fixture.sessionManager ).invalidateSession( fixture.replicantSession );
    verify( fixture.removedEvent ).fire( new ReplicantSessionRemoved( fixture.sessionId ) );
    verify( fixture.updatedEvent ).fire( new ReplicantSessionUpdated( fixture.sessionId ) );

    final var response = getLastSentMessage( fixture );
    assertEquals( response.getString( Messages.Common.TYPE ), Messages.S2C_Type.UNKNOWN_REQUEST_TYPE );
  }

  @Test
  public void command_securityException()
    throws Exception
  {
    final var fixture = newFixture( new SecuredEndpoint() );
    final var command = Json.createObjectBuilder()
      .add( Messages.Common.TYPE, Messages.C2S_Type.PING )
      .add( Messages.Common.REQUEST_ID, 3 )
      .build();

    fixture.endpoint.command( fixture.session, command.toString() );

    verify( fixture.sessionManager ).invalidateSession( fixture.replicantSession );
    verify( fixture.removedEvent ).fire( new ReplicantSessionRemoved( fixture.sessionId ) );
    verify( fixture.updatedEvent, never() ).fire( any() );

    final var response = getLastSentMessage( fixture );
    assertEquals( response.getString( Messages.Common.TYPE ), Messages.S2C_Type.ERROR );
    assertEquals( response.getString( Messages.S2C_Common.MESSAGE ), "Security constraints violated" );
  }

  private void assertInvalidSubscribe( @Nonnull final String address, @Nonnull final String message )
    throws Exception
  {
    final var fixture = newFixture();
    final var command = createSubscribeCommand( address, 2, null );

    fixture.endpoint.command( fixture.session, command.toString() );

    verify( fixture.sessionManager, never() ).bulkSubscribe( any(), anyInt(), anyList(), any() );
    verify( fixture.sessionManager ).invalidateSession( fixture.replicantSession );
    verify( fixture.removedEvent ).fire( new ReplicantSessionRemoved( fixture.sessionId ) );
    verify( fixture.updatedEvent ).fire( new ReplicantSessionUpdated( fixture.sessionId ) );

    final var response = getLastSentMessage( fixture );
    assertEquals( response.getString( Messages.Common.TYPE ), Messages.S2C_Type.ERROR );
    assertEquals( response.getString( Messages.S2C_Common.MESSAGE ), message );
  }

  @Nonnull
  private String createPingCommand( final int requestId )
  {
    return Json.createObjectBuilder()
      .add( Messages.Common.TYPE, Messages.C2S_Type.PING )
      .add( Messages.Common.REQUEST_ID, requestId )
      .build()
      .toString();
  }

  @Nonnull
  private JsonObject createSubscribeCommand( @Nonnull final String channel,
                                             final int requestId,
                                             @Nullable final JsonObject filter )
  {
    final var builder = Json.createObjectBuilder()
      .add( Messages.Common.TYPE, Messages.C2S_Type.SUB )
      .add( Messages.Common.REQUEST_ID, requestId )
      .add( Messages.Common.CHANNEL, channel );
    if ( null != filter )
    {
      builder.add( Messages.Update.FILTER, filter );
    }
    return builder.build();
  }

  @Nonnull
  private JsonObject getLastSentMessage( @Nonnull final EndpointFixture fixture )
  {
    final var captor = org.mockito.ArgumentCaptor.forClass( String.class );
    try
    {
      verify( fixture.remote, atLeastOnce() ).sendText( captor.capture() );
    }
    catch ( final IOException ioe )
    {
      throw new AssertionError( ioe );
    }
    final var message = captor.getValue();
    return Json.createReader( new StringReader( message ) ).readObject();
  }

  @Nonnull
  private EndpointFixture newFixture()
  {
    return newFixture( new ReplicantEndpoint() );
  }

  @Nonnull
  private EndpointFixture newFixture( @Nonnull final ReplicantEndpoint endpoint )
  {
    final var sessionManager = mock( ReplicantSessionManager.class );
    final Event<ReplicantSessionAdded> addedEvent = mockEvent();
    final Event<ReplicantSessionUpdated> updatedEvent = mockEvent();
    final Event<ReplicantSessionRemoved> removedEvent = mockEvent();
    setField( endpoint, "_sessionManager", sessionManager );
    setField( endpoint, "_replicantSessionAddedEventEvent", addedEvent );
    setField( endpoint, "_replicantSessionUpdatedEvent", updatedEvent );
    setField( endpoint, "_replicantSessionRemovedEvent", removedEvent );

    final var session = mock( Session.class );
    final var remote = mock( RemoteEndpoint.Basic.class );
    final var sessionId = "session-1";
    when( session.getId() ).thenReturn( sessionId );
    when( session.isOpen() ).thenReturn( true );
    when( session.getBasicRemote() ).thenReturn( remote );

    final ReplicantSession replicantSession = new ReplicantSession( session );
    when( sessionManager.createSession( session ) ).thenReturn( replicantSession );
    when( sessionManager.getSession( sessionId ) ).thenReturn( replicantSession );
    when( sessionManager.isAuthorized( replicantSession ) ).thenReturn( true );
    when( sessionManager.getSchemaMetaData() ).thenReturn( newSchemaMetaData() );

    return new EndpointFixture( endpoint,
                                sessionManager,
                                addedEvent,
                                updatedEvent,
                                removedEvent,
                                session,
                                remote,
                                sessionId,
                                replicantSession );
  }

  @Nonnull
  private SchemaMetaData newSchemaMetaData()
  {
    final ChannelMetaData typeChannel = new ChannelMetaData( 0,
                                                             "type",
                                                             null,
                                                             ChannelMetaData.FilterType.NONE,
                                                             null,
                                                             ChannelMetaData.CacheType.NONE,
                                                             true );
    final ChannelMetaData dynamicChannel = new ChannelMetaData( 1,
                                                                "dynamic",
                                                                1,
                                                                ChannelMetaData.FilterType.DYNAMIC,
                                                                json -> json,
                                                                ChannelMetaData.CacheType.NONE,
                                                                true );
    final ChannelMetaData instancedChannel = new ChannelMetaData( 2,
                                                                  "instanced",
                                                                  2,
                                                                  ChannelMetaData.FilterType.DYNAMIC_INSTANCED,
                                                                  json -> json,
                                                                  ChannelMetaData.CacheType.NONE,
                                                                  true );
    final ChannelMetaData staticInstancedChannel = new ChannelMetaData( 4,
                                                                        "staticInstanced",
                                                                        4,
                                                                        ChannelMetaData.FilterType.STATIC_INSTANCED,
                                                                        json -> json,
                                                                        ChannelMetaData.CacheType.NONE,
                                                                        true );
    final ChannelMetaData internalChannel = new ChannelMetaData( 3,
                                                                 "internal",
                                                                 null,
                                                                 ChannelMetaData.FilterType.NONE,
                                                                 null,
                                                                 ChannelMetaData.CacheType.NONE,
                                                                 false );
    return new SchemaMetaData( "Test",
                               typeChannel,
                               dynamicChannel,
                               instancedChannel,
                               internalChannel,
                               staticInstancedChannel );
  }

  private void setField( @Nonnull final Object target, @Nonnull final String name, @Nullable final Object value )
  {
    try
    {
      final Field field = ReplicantEndpoint.class.getDeclaredField( name );
      field.setAccessible( true );
      field.set( target, value );
    }
    catch ( final Exception e )
    {
      throw new AssertionError( e );
    }
  }

  private record EndpointFixture(@Nonnull ReplicantEndpoint endpoint, @Nonnull ReplicantSessionManager sessionManager,
                                 @Nonnull Event<ReplicantSessionAdded> addedEvent,
                                 @Nonnull Event<ReplicantSessionUpdated> updatedEvent,
                                 @Nonnull Event<ReplicantSessionRemoved> removedEvent, @Nonnull Session session,
                                 @Nonnull RemoteEndpoint.Basic remote, @Nonnull String sessionId,
                                 @Nonnull ReplicantSession replicantSession)
  {
  }

  private static final class SecuredEndpoint
    extends ReplicantEndpoint
  {
    @Override
    protected void beforeCommand( @Nonnull final ReplicantSession session,
                                  @Nonnull final String type,
                                  @Nonnull final JsonObject command )
    {
      throw new SecurityException( "Denied" );
    }
  }

  @SuppressWarnings( "unchecked" )
  private static <T> Event<T> mockEvent()
  {
    return (Event<T>) mock( Event.class );
  }
}
