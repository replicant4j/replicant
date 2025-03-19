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
import org.realityforge.replicant.server.json.JsonEncoder;
import org.realityforge.replicant.server.transport.ChannelMetaData;
import org.realityforge.replicant.server.transport.ReplicantSession;
import org.realityforge.replicant.server.transport.ReplicantSessionManager;
import org.realityforge.replicant.server.transport.WebSocketUtil;
import replicant.shared.Messages;

public abstract class AbstractReplicantEndpoint
{
  @Nonnull
  protected static final Logger LOG = Logger.getLogger( AbstractReplicantEndpoint.class.getName() );
  @Nonnull
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

    onSessionOpen( newReplicantSession );

    WebSocketUtil.sendText( session, JsonEncoder.encodeSessionCreatedMessage( newReplicantSession.getId() ) );
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
      type = command.getString( JsonEncoder.TYPE );
      requestId = command.getInt( JsonEncoder.REQUEST_ID );
    }
    catch ( final Throwable ignored )
    {
      onMalformedMessage( replicantSession, message );
      return;
    }
    if ( !Messages.C2S_Type.AUTH.equals( type ) && !isAuthorized( replicantSession ) )
    {
      sendErrorAndClose( session, "Replicant session not authorized" );
      return;
    }
    try
    {
      beforeCommand( replicantSession, type, command );
      //noinspection IfCanBeSwitch
      if ( Messages.C2S_Type.EXEC.equals( type ) )
      {
        try
        {
          onExec( replicantSession,
                  command.getString( JsonEncoder.COMMAND ),
                  command.getInt( JsonEncoder.REQUEST_ID ),
                  command.containsKey( JsonEncoder.PAYLOAD ) ? command.getJsonObject( JsonEncoder.PAYLOAD ) : null );
        }
        catch ( final InterruptedException ignored )
        {
          replicantSession.closeDueToInterrupt();
        }
      }
      else if ( Messages.C2S_Type.ETAGS.equals( type ) )
      {
        try
        {
          onETags( replicantSession, command );
        }
        catch ( final InterruptedException ignored )
        {
          replicantSession.closeDueToInterrupt();
        }
      }
      else if ( Messages.C2S_Type.PING.equals( type ) )
      {
        sendOk( session, requestId );
      }
      else if ( Messages.C2S_Type.SUB.equals( type ) )
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
      else if ( Messages.C2S_Type.BULK_SUB.equals( type ) )
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
      else if ( Messages.C2S_Type.UNSUB.equals( type ) )
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
      else if ( Messages.C2S_Type.BULK_UNSUB.equals( type ) )
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
      else if ( Messages.C2S_Type.AUTH.equals( type ) )
      {
        onAuthorize( replicantSession, command );
      }
      else
      {
        onUnknownType( replicantSession, command );
      }
      afterCommand( replicantSession, type, command );
    }
    catch ( final SecurityException ignored )
    {
      sendErrorAndClose( replicantSession, "Security constraints violated" );
    }
  }

  /**
   * A hook method invoked as a session is opened.
   *
   * @param session the session.
   */
  @SuppressWarnings( "unused" )
  protected void onSessionOpen( @Nonnull final ReplicantSession session )
  {
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
    WebSocketUtil.sendText( session, JsonEncoder.encodeOkMessage( requestId ) );
  }

  protected abstract void onExec( @Nonnull final ReplicantSession session,
                                  @Nonnull final String command,
                                  final int requestId,
                                  @Nullable final JsonObject payload )
    throws InterruptedException;

  private void onETags( @Nonnull final ReplicantSession session, @Nonnull final JsonObject command )
    throws InterruptedException
  {
    final Map<ChannelAddress, String> etags = new HashMap<>();
    for ( final Map.Entry<String, JsonValue> entry : command.getJsonObject( JsonEncoder.ETAGS ).entrySet() )
    {
      final ChannelAddress address = ChannelAddress.parse( entry.getKey() );
      final String eTag = ( (JsonString) entry.getValue() ).getString();
      etags.put( address, eTag );
    }
    ReplicationRequestUtil.sessionLockingRequest( getRegistry(),
                                                  getEntityManager(),
                                                  getEndpoint(),
                                                  "setEtags()",
                                                  session,
                                                  null,
                                                  () -> session.setETags( etags ) );

    sendOk( session.getWebSocketSession(), command.getInt( JsonEncoder.REQUEST_ID ) );
  }

  private void onMalformedMessage( @Nonnull final ReplicantSession replicantSession, @Nonnull final String message )
  {
    closeWithError( replicantSession, "Malformed message", JsonEncoder.encodeMalformedMessageMessage( message ) );
  }

  private void onUnknownType( @Nonnull final ReplicantSession replicantSession, @Nonnull final JsonObject command )
  {
    closeWithError( replicantSession, "Unknown request type", JsonEncoder.encodeUnknownRequestType( command ) );
  }

  private void onAuthorize( @Nonnull final ReplicantSession replicantSession, @Nonnull final JsonObject command )
  {
    replicantSession.setAuthToken( command.getString( "token" ) );
    sendOk( replicantSession.getWebSocketSession(), command.getInt( JsonEncoder.REQUEST_ID ) );
  }

  private void onSubscribe( @Nonnull final ReplicantSession replicantSession, @Nonnull final JsonObject command )
    throws IOException, InterruptedException
  {
    final ChannelAddress address = ChannelAddress.parse( command.getString( "channel" ) );
    final ChannelMetaData channelMetaData = getChannelMetaData( address.getChannelId() );
    if ( checkSubscribeRequest( replicantSession, channelMetaData, address ) )
    {
      subscribe( replicantSession,
                 command.getInt( JsonEncoder.REQUEST_ID ),
                 address,
                 extractFilter( channelMetaData, command ) );
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
        sendErrorAndClose( session, "Bulk channel subscribe included addresses channel without sub-channel ids" );
        return;
      }
      else
      {
        subChannelIds.add( address.getSubChannelId() );
      }
    }

    final int requestId = command.getInt( JsonEncoder.REQUEST_ID );
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
                                @Nullable final List<Integer> subChannelIds,
                                @Nullable final Object filter )
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
    final ChannelAddress address = ChannelAddress.parse( command.getString( JsonEncoder.CHANNEL ) );
    final ChannelMetaData channelMetaData = getChannelMetaData( address.getChannelId() );
    if ( checkUnsubscribeRequest( replicantSession, channelMetaData, address ) )
    {
      final int requestId = command.getInt( JsonEncoder.REQUEST_ID );
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

    final int requestId = command.getInt( JsonEncoder.REQUEST_ID );
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

  @Nullable
  private ReplicantSession findReplicantSession( @Nonnull final Session session )
  {
    try
    {
      return getSessionManager().getSession( session.getId() );
    }
    catch ( final Throwable ignored )
    {
      // This is sometimes called from onClose after the application has already been
      // un-deployed but the websockets have not completed closing. In this scenario
      // the toolkit would generate an exception. We just capture the exception and
      // return null to allow normal shutdown to occur without a log storm.
      return null;
    }
  }

  @Nonnull
  private ReplicantSession getReplicantSession( @Nonnull final Session session )
  {
    final ReplicantSession replicantSession = findReplicantSession( session );
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
    if ( session.isOpen() )
    {
      WebSocketUtil.sendText( session, JsonEncoder.encodeErrorMessage( message ) );
      session.close( new CloseReason( CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Unexpected error" ) );
    }
    final ReplicantSession replicantSession = findReplicantSession( session );
    if ( null != replicantSession )
    {
      closeReplicantSession( replicantSession );
    }
  }

  @OnClose
  public void onClose( @Nonnull final Session session )
  {
    final ReplicantSession replicantSession = findReplicantSession( session );
    if ( null == replicantSession )
    {
      LOG.log( Level.FINE,
               () -> "Closing WebSocket Session " + session.getId() +
                     " but no replicant session found. This can occur except during " +
                     "application undeploy or when the session has errored." );
    }
    else
    {
      LOG.log( Level.FINE,
               () -> "Closing WebSocket Session " + session.getId() +
                     " for replicant session " + replicantSession.getId() );
      closeReplicantSession( replicantSession );
    }
  }

  @Nullable
  private Object toFilter( @Nonnull final ChannelMetaData channelMetaData, @Nonnull final JsonObject filterContent )
  {
    return channelMetaData.hasFilterParameter() ? parseFilter( channelMetaData, filterContent ) : null;
  }

  @Nonnull
  private Object parseFilter( @Nonnull final ChannelMetaData channelMetaData, @Nonnull final JsonObject filterContent )
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
                               @Nonnull final String message )
  {
    WebSocketUtil.sendText( replicantSession.getWebSocketSession(), message );
    replicantSession.close( new CloseReason( CloseReason.CloseCodes.UNEXPECTED_CONDITION, reason ) );
    closeReplicantSession( replicantSession );
  }

  private void closeReplicantSession( @Nonnull final ReplicantSession replicantSession )
  {
    onSessionClose( replicantSession );
    getSessionManager().invalidateSession( replicantSession );
  }
}
