package org.realityforge.replicant.server.ee;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.persistence.EntityManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.Transactional;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import org.realityforge.replicant.server.ChannelAddress;
import org.realityforge.replicant.server.EntityMessageEndpoint;
import org.realityforge.replicant.server.transport.ChannelMetaData;
import org.realityforge.replicant.server.transport.ReplicantSession;
import org.realityforge.replicant.server.transport.ReplicantSessionManager;
import org.realityforge.replicant.server.transport.WebSocketUtil;

public abstract class AbstractReplicantEndpoint
{
  protected static final Logger LOG = Logger.getLogger( AbstractEeReplicantEndpoint.class.getName() );
  private transient final ObjectMapper _jsonMapper = new ObjectMapper();

  @SuppressWarnings( "WeakerAccess" )
  @Nonnull
  protected abstract ReplicantSessionManager getSessionManager();

  @SuppressWarnings( "WeakerAccess" )
  @Nonnull
  protected abstract TransactionSynchronizationRegistry getRegistry();

  @SuppressWarnings( "WeakerAccess" )
  @Nonnull
  protected abstract EntityManager getEntityManager();

  @SuppressWarnings( "WeakerAccess" )
  @Nonnull
  protected abstract EntityMessageEndpoint getEndpoint();

  @OnOpen
  public void onOpen( @Nonnull final Session session )
  {
    final ReplicantSession newReplicantSession = getSessionManager().createSession( session );
    if ( LOG.isLoggable( Level.FINE ) )
    {
      LOG.log( Level.FINE,
               "Opening WebSocket Session " + session.getId() +
               " for replicant session " + getReplicantSession( session ).getId() );
    }

    final JsonObjectBuilder builder =
      Json.createObjectBuilder()
        .add( "type", "session-created" )
        .add( "sessionId", newReplicantSession.getId() );
    WebSocketUtil.sendJsonObject( session, builder.build() );
  }

  @OnMessage
  @Transactional
  public void command( @Nonnull final Session session, @Nonnull final String message )
    throws IOException
  {
    final ReplicantSession replicantSession;
    try
    {
      replicantSession = getReplicantSession( session );
    }
    catch ( final Throwable ignored )
    {
      sendErrorAndClose( session, "Unable to locate associated replicant session" );
      return;
    }
    if ( LOG.isLoggable( Level.FINE ) )
    {
      LOG.log( Level.FINE,
               "Message on WebSocket Session " + session.getId() +
               " for replicant session " + getReplicantSession( session ).getId() + ". Message:\n" + message );
    }
    final JsonObject command;
    final String type;
    final int requestId;
    try
    {
      command = Json.createReader( new StringReader( message ) ).readObject();
      type = command.getString( "type" );
      requestId = command.getInt( "requestId" );
    }
    catch ( final Throwable ignored )
    {
      onMalformedMessage( replicantSession, message );
      return;
    }
    if ( !"auth".equals( type ) && !isAuthorized( replicantSession ) )
    {
      sendErrorAndClose( session, "Replicant session not authroized" );
      return;
    }
    beforeCommand( replicantSession, type, command );
    //noinspection IfCanBeSwitch
    if ( "etags".equals( type ) )
    {
      onETags( replicantSession, command );
    }
    else if ( "ping".equals( type ) )
    {
      sendOk( session, requestId );
    }
    else if ( "sub".equals( type ) )
    {
      try
      {
        onSubscribe( replicantSession, command );
      }
      catch ( final InterruptedException ignored )
      {
        replicantSession.closeDueToInterrupt();
      }
    }
    else if ( "bulk-sub".equals( type ) )
    {
      try
      {
        onBulkSubscribe( replicantSession, command );
      }
      catch ( final InterruptedException ignored )
      {
        replicantSession.closeDueToInterrupt();
      }
    }
    else if ( "unsub".equals( type ) )
    {
      try
      {
        onUnsubscribe( replicantSession, command );
      }
      catch ( final InterruptedException ignored )
      {
        replicantSession.closeDueToInterrupt();
      }
    }
    else if ( "bulk-unsub".equals( type ) )
    {
      try
      {
        onBulkUnsubscribe( replicantSession, command );
      }
      catch ( final InterruptedException ignored )
      {
        replicantSession.closeDueToInterrupt();
      }
    }
    else if ( "auth".equals( type ) )
    {
      onAuthorize( replicantSession, command );
    }
    else
    {
      onUnknownCommand( replicantSession, command );
    }
    afterCommand( replicantSession, type, command );
  }

  /**
   * A hook method invoked as a session is closed.
   *
   * @param session the session.
   */
  @SuppressWarnings( "unused" )
  protected void onSessionClose( @Nonnull final ReplicantSession session )
  {
  }

  /**
   * A hook method invoked before a command is processed.
   *
   * @param session the session.
   * @param type    the type of the command.
   * @param command the command object
   */
  @SuppressWarnings( "unused" )
  protected void beforeCommand( @Nonnull final ReplicantSession session,
                                @Nonnull final String type,
                                @Nonnull final JsonObject command )
  {
  }

  /**
   * A hook method invoked after a command is processed.
   *
   * @param session the session.
   * @param type    the type of the command.
   * @param command the command object
   */
  @SuppressWarnings( "unused" )
  protected void afterCommand( @Nonnull final ReplicantSession session,
                               @Nonnull final String type,
                               @Nonnull final JsonObject command )
  {
  }

  @SuppressWarnings( { "WeakerAccess", "unused" } )
  protected boolean isAuthorized( @Nonnull final ReplicantSession replicantSession )
  {
    return true;
  }

  private void sendOk( @Nonnull final Session session, final int requestId )
  {
    final JsonObjectBuilder builder =
      Json.createObjectBuilder()
        .add( "type", "ok" )
        .add( "requestId", requestId );
    WebSocketUtil.sendJsonObject( session, builder.build() );
  }

  private void onETags( @Nonnull final ReplicantSession replicantSession, @Nonnull final JsonObject command )
  {
    final Map<ChannelAddress, String> etags = new HashMap<>();
    for ( final Map.Entry<String, JsonValue> entry : command.getJsonObject( "etags" ).entrySet() )
    {
      final ChannelAddress address = ChannelAddress.parse( entry.getKey() );
      final String eTag = ( (JsonString) entry.getValue() ).getString();
      etags.put( address, eTag );
    }
    replicantSession.setETags( etags );
    sendOk( replicantSession.getWebSocketSession(), command.getInt( "requestId" ) );
  }

  private void onMalformedMessage( @Nonnull final ReplicantSession replicantSession, @Nonnull final String message )
  {
    final JsonObjectBuilder builder =
      Json.createObjectBuilder()
        .add( "type", "malformed-message" )
        .add( "message", message );
    closeWithError( replicantSession,
                    "Malformed message",
                    builder.build() );
  }

  private void onUnknownCommand( @Nonnull final ReplicantSession replicantSession, @Nonnull final JsonObject command )
  {
    final JsonObjectBuilder builder =
      Json.createObjectBuilder()
        .add( "type", "unknown-command" )
        .add( "command", command );
    closeWithError( replicantSession, "Unknown command", builder.build() );
  }

  private void onAuthorize( @Nonnull final ReplicantSession replicantSession, @Nonnull final JsonObject command )
  {
    replicantSession.setAuthToken( command.getString( "token" ) );
    sendOk( replicantSession.getWebSocketSession(), command.getInt( "requestId" ) );
  }

  private void onSubscribe( @Nonnull final ReplicantSession replicantSession, @Nonnull final JsonObject command )
    throws IOException, InterruptedException
  {
    final ChannelAddress address = ChannelAddress.parse( command.getString( "channel" ) );
    final ChannelMetaData channelMetaData = getChannelMetaData( address.getChannelId() );
    if ( checkSubscribeRequest( replicantSession, channelMetaData, address ) )
    {
      subscribe( replicantSession, command.getInt( "requestId" ), address, extractFilter( channelMetaData, command ) );
    }
  }

  private boolean checkSubscribeRequest( @Nonnull final ReplicantSession replicantSession,
                                         @Nonnull final ChannelMetaData channelMetaData,
                                         @Nonnull final ChannelAddress address )
    throws IOException
  {
    if ( !channelMetaData.isExternal() )
    {
      sendErrorAndClose( replicantSession, "Attempted to subscribe to internal-only channel" );
      return false;
    }
    else if ( address.hasSubChannelId() && channelMetaData.isTypeGraph() )
    {
      sendErrorAndClose( replicantSession, "Attempted to subscribe to type channel with instance data" );
      return false;
    }
    else if ( !address.hasSubChannelId() && channelMetaData.isInstanceGraph() )
    {
      sendErrorAndClose( replicantSession, "Attempted to subscribe to instance channel without instance data" );
      return false;
    }
    else
    {
      return true;
    }
  }

  private void subscribe( @Nonnull final ReplicantSession session,
                          final int requestId,
                          @Nonnull final ChannelAddress address,
                          @Nullable final Object filter )
    throws InterruptedException
  {
    ReplicationRequestUtil.sessionUpdateRequest( getRegistry(),
                                                 getEntityManager(),
                                                 getEndpoint(),
                                                 "Subscribe(" + address + ")",
                                                 session,
                                                 requestId,
                                                 () -> doSubscribe( session, address, filter ) );
  }

  private void doSubscribe( @Nonnull final ReplicantSession session,
                            @Nonnull final ChannelAddress address,
                            @Nullable final Object filter )
  {
    EntityMessageCacheUtil.getSessionChanges().setRequired( true );
    try
    {
      getSessionManager().subscribe( session, address, filter );
    }
    catch ( final InterruptedException ignored )
    {
      session.closeDueToInterrupt();
    }
  }

  private void onBulkSubscribe( @Nonnull final ReplicantSession session, @Nonnull final JsonObject command )
    throws IOException, InterruptedException
  {
    final ChannelAddress[] addresses = extractChannels( command );
    if ( 0 == addresses.length )
    {
      return;
    }
    final int channelId = addresses[ 0 ].getChannelId();

    final ChannelMetaData channelMetaData = getChannelMetaData( channelId );
    final List<Integer> subChannelIds = new ArrayList<>();
    for ( final ChannelAddress address : addresses )
    {
      if ( !checkSubscribeRequest( session, channelMetaData, address ) )
      {
        return;
      }
      if ( address.getChannelId() != channelId )
      {
        sendErrorAndClose( session, "Bulk channel subscribe included addresses from multiple channels" );
        return;
      }
      else if ( !address.hasSubChannelId() )
      {
        sendErrorAndClose( session,
                           "Bulk channel subscribe included addresses channel without sub-channel ids" );
        return;
      }
      else
      {
        subChannelIds.add( address.getSubChannelId() );
      }
    }

    final int requestId = command.getInt( "requestId" );
    final Object filter = extractFilter( channelMetaData, command );
    if ( 1 == addresses.length )
    {
      subscribe( session, requestId, addresses[ 0 ], filter );
    }
    else
    {
      ReplicationRequestUtil.sessionUpdateRequest( getRegistry(),
                                                   getEntityManager(),
                                                   getEndpoint(),
                                                   "BulkSubscribe(" + channelMetaData.getChannelId() + ")",
                                                   session,
                                                   requestId,
                                                   () -> doBulkSubscribe( session, channelId, subChannelIds, filter ) );
    }
  }

  private void doBulkSubscribe( @Nonnull final ReplicantSession session,
                                final int channelId,
                                final List<Integer> subChannelIds, final Object filter )
  {
    EntityMessageCacheUtil.getSessionChanges().setRequired( true );
    try
    {
      getSessionManager().bulkSubscribe( session, channelId, subChannelIds, filter );
    }
    catch ( final InterruptedException ignored )
    {
      session.closeDueToInterrupt();
    }
  }

  @Nonnull
  private ChannelAddress[] extractChannels( @Nonnull final JsonObject command )
  {
    final JsonArray channels = command.getJsonArray( "channels" );
    final int channelCount = channels.size();
    final ChannelAddress[] addresses = new ChannelAddress[ channelCount ];
    for ( int i = 0; i < channelCount; i++ )
    {
      addresses[ i ] = ChannelAddress.parse( channels.getString( i ) );
    }
    return addresses;
  }

  @Nullable
  private Object extractFilter( final ChannelMetaData channelMetaData, final @Nonnull JsonObject command )
  {
    return command.containsKey( "filter" ) && !command.isNull( "filter" ) ?
           toFilter( channelMetaData, command.getJsonObject( "filter" ) ) :
           null;
  }

  private void onUnsubscribe( @Nonnull final ReplicantSession replicantSession, @Nonnull final JsonObject command )
    throws IOException, InterruptedException
  {
    final ChannelAddress address = ChannelAddress.parse( command.getString( "channel" ) );
    final ChannelMetaData channelMetaData = getChannelMetaData( address.getChannelId() );
    if ( checkUnsubscribeRequest( replicantSession, channelMetaData, address ) )
    {
      final int requestId = command.getInt( "requestId" );
      unsubscribe( replicantSession, requestId, address );
    }
  }

  private void onBulkUnsubscribe( @Nonnull final ReplicantSession session, @Nonnull final JsonObject command )
    throws IOException, InterruptedException
  {
    final ChannelAddress[] addresses = extractChannels( command );
    if ( 0 == addresses.length )
    {
      return;
    }
    final int channelId = addresses[ 0 ].getChannelId();

    final ChannelMetaData channelMetaData = getChannelMetaData( channelId );
    final List<Integer> subChannelIds = new ArrayList<>();
    for ( final ChannelAddress address : addresses )
    {
      if ( !checkUnsubscribeRequest( session, channelMetaData, address ) )
      {
        return;
      }
      if ( address.getChannelId() != channelId )
      {
        sendErrorAndClose( session, "Bulk channel unsubscribe included addresses from multiple channels" );
        return;
      }
      else if ( !address.hasSubChannelId() )
      {
        sendErrorAndClose( session,
                           "Bulk channel unsubscribe included addresses channel without sub-channel ids" );
        return;
      }
      else
      {
        subChannelIds.add( address.getSubChannelId() );
      }
    }

    final int requestId = command.getInt( "requestId" );
    if ( 1 == addresses.length )
    {
      unsubscribe( session, requestId, addresses[ 0 ] );
    }
    else
    {
      ReplicationRequestUtil.sessionUpdateRequest( getRegistry(),
                                                   getEntityManager(),
                                                   getEndpoint(),
                                                   "BulkUnsubscribe(" + channelMetaData.getChannelId() + ")",
                                                   session,
                                                   requestId,
                                                   () -> doBulkUnsubscribe( session, channelId, subChannelIds ) );
    }
  }

  private void doBulkUnsubscribe( @Nonnull final ReplicantSession session,
                                  final int channelId,
                                  final List<Integer> subChannelIds )
  {
    EntityMessageCacheUtil.getSessionChanges().setRequired( true );
    try
    {
      getSessionManager().bulkUnsubscribe( session, channelId, subChannelIds );
    }
    catch ( final InterruptedException ignored )
    {
      session.closeDueToInterrupt();
    }
  }

  private void unsubscribe( @Nonnull final ReplicantSession session,
                            final int requestId,
                            @Nonnull final ChannelAddress address )
    throws InterruptedException
  {
    ReplicationRequestUtil.sessionUpdateRequest( getRegistry(),
                                                 getEntityManager(),
                                                 getEndpoint(),
                                                 "Unsubscribe(" + address + ")",
                                                 session,
                                                 requestId,
                                                 () -> doUnsubscribe( session, address ) );
  }

  private void doUnsubscribe( @Nonnull final ReplicantSession session, @Nonnull final ChannelAddress address )
  {
    EntityMessageCacheUtil.getSessionChanges().setRequired( true );
    try
    {
      getSessionManager().unsubscribe( session, address );
    }
    catch ( final InterruptedException ignored )
    {
      session.closeDueToInterrupt();
    }
  }

  private boolean checkUnsubscribeRequest( @Nonnull final ReplicantSession replicantSession,
                                           @Nonnull final ChannelMetaData channelMetaData,
                                           @Nonnull final ChannelAddress address )
    throws IOException
  {
    if ( !channelMetaData.isExternal() )
    {
      sendErrorAndClose( replicantSession, "Attempted to unsubscribe from internal-only channel" );
      return false;
    }
    else if ( address.hasSubChannelId() && channelMetaData.isTypeGraph() )
    {
      sendErrorAndClose( replicantSession, "Attempted to unsubscribe from type channel with instance data" );
      return false;
    }
    else if ( !address.hasSubChannelId() && channelMetaData.isInstanceGraph() )
    {
      sendErrorAndClose( replicantSession, "Attempted to unsubscribe from instance channel without instance data" );
      return false;
    }
    else
    {
      return true;
    }
  }

  @Nonnull
  private ReplicantSession getReplicantSession( @Nonnull final Session session )
  {
    final ReplicantSession replicantSession = getSessionManager().getSession( session.getId() );
    if ( null != replicantSession )
    {
      return replicantSession;
    }
    else
    {
      throw new IllegalStateException( "Unable to locate ReplicantSession for WebSocket session " + session.getId() );
    }
  }

  @OnError
  public void onError( @Nonnull final Session session, @Nonnull final Throwable error )
    throws IOException
  {
    if ( LOG.isLoggable( Level.INFO ) )
    {
      LOG.log( Level.INFO, "Error on WebSocket Session " + session.getId(), error );
    }

    sendErrorAndClose( session, error.toString() );
  }

  private void sendErrorAndClose( @Nonnull final ReplicantSession session, @Nonnull final String message )
    throws IOException
  {
    sendErrorAndClose( session.getWebSocketSession(), message );
  }

  private void sendErrorAndClose( @Nonnull final Session session, @Nonnull final String message )
    throws IOException
  {
    WebSocketUtil.sendJsonObject( session,
                                  Json
                                    .createObjectBuilder()
                                    .add( "type", "error" )
                                    .add( "message", message )
                                    .build() );
    session.close( new CloseReason( CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Unexpected error" ) );
    final ReplicantSession replicantSession = getSessionManager().getSession( session.getId() );
    if ( null != replicantSession )
    {
      closeSession( replicantSession );
    }
  }

  @OnClose
  public void onClose( @Nonnull final Session session )
  {
    if ( LOG.isLoggable( Level.FINE ) )
    {
      LOG.log( Level.FINE,
               "Closing WebSocket Session " + session.getId() +
               " for replicant session " + getReplicantSession( session ).getId() );
    }
    closeSession( getReplicantSession( session ) );
  }

  @Nullable
  private Object toFilter( @Nonnull final ChannelMetaData channelMetaData, @Nonnull final JsonObject filterContent )
  {
    return channelMetaData.hasFilterParameter() ? parseFilter( channelMetaData, filterContent ) : null;
  }

  @Nonnull
  private Object parseFilter( @Nonnull final ChannelMetaData channelMetaData,
                              @Nonnull final JsonObject filterContent )
  {
    try
    {
      return _jsonMapper.readValue( filterContent.toString(), channelMetaData.getFilterParameterType() );
    }
    catch ( final IOException ioe )
    {
      throw new IllegalArgumentException( "Unable to parse filter: " + filterContent, ioe );
    }
  }

  @Nonnull
  private ChannelMetaData getChannelMetaData( final int channelId )
  {
    return getSessionManager().getSystemMetaData().getChannelMetaData( channelId );
  }

  private void closeWithError( @Nonnull final ReplicantSession replicantSession,
                               @Nonnull final String reason,
                               @Nonnull final JsonObject object )
  {
    WebSocketUtil.sendJsonObject( replicantSession.getWebSocketSession(), object );
    replicantSession.close( new CloseReason( CloseReason.CloseCodes.UNEXPECTED_CONDITION, reason ) );
    closeSession( replicantSession );
  }

  private void closeSession( @Nonnull final ReplicantSession replicantSession )
  {
    onSessionClose( replicantSession );
    getSessionManager().invalidateSession( replicantSession );
  }
}
