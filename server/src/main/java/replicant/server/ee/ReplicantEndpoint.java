package replicant.server.ee;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.transaction.Transactional;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import replicant.server.ChannelAddress;
import replicant.server.json.JsonEncoder;
import replicant.server.transport.ChannelMetaData;
import replicant.server.transport.ReplicantSession;
import replicant.server.transport.ReplicantSessionManager;
import replicant.server.transport.WebSocketUtil;
import replicant.shared.Messages;
import replicant.shared.SharedConstants;

@ServerEndpoint( "/api" + SharedConstants.REPLICANT_URL_FRAGMENT )
@ApplicationScoped
@Transactional
public class ReplicantEndpoint
{
  @Nonnull
  protected static final Logger LOG = Logger.getLogger( ReplicantEndpoint.class.getName() );
  @Inject
  private ReplicantSessionManager _sessionManager;
  @Inject
  private Event<ReplicantSessionAdded> _replicantSessionAddedEventEvent;
  @Inject
  private Event<ReplicantSessionUpdated> _replicantSessionUpdatedEvent;
  @Inject
  private Event<ReplicantSessionRemoved> _replicantSessionRemovedEvent;

  @OnOpen
  public void onOpen( @Nonnull final Session session )
  {
    final ReplicantSession newReplicantSession = _sessionManager.createSession( session );
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
      type = command.getString( Messages.Common.TYPE );
      requestId = command.getInt( Messages.Common.REQUEST_ID );
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
        _sessionManager.execCommand( replicantSession,
                                     command.getString( Messages.Common.COMMAND ),
                                     command.getInt( Messages.Common.REQUEST_ID ),
                                     command.containsKey( Messages.Exec.PAYLOAD ) ?
                                     command.getJsonObject( Messages.Exec.PAYLOAD ) :
                                     null );
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
    _replicantSessionAddedEventEvent.fire( new ReplicantSessionAdded( session.getId() ) );
  }

  /**
   * A hook method invoked as a session is closed.
   *
   * @param session the session.
   */
  @SuppressWarnings( "unused" )
  protected void onSessionClose( @Nonnull final ReplicantSession session )
  {
    _replicantSessionRemovedEvent.fire( new ReplicantSessionRemoved( session.getId() ) );
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
    _replicantSessionUpdatedEvent.fire( new ReplicantSessionUpdated( session.getId() ) );
  }

  private boolean isAuthorized( @Nonnull final ReplicantSession session )
  {
    return _sessionManager.isAuthorized( session );
  }

  private void sendOk( @Nonnull final Session session, final int requestId )
  {
    WebSocketUtil.sendText( session, JsonEncoder.encodeOkMessage( requestId ) );
  }

  private void onETags( @Nonnull final ReplicantSession session, @Nonnull final JsonObject command )
    throws InterruptedException
  {
    final var eTags = new HashMap<ChannelAddress, String>();
    for ( final var entry : command.getJsonObject( Messages.Etags.ETAGS ).entrySet() )
    {
      final var address = ChannelAddress.parse( entry.getKey() );
      final var eTag = ( (JsonString) entry.getValue() ).getString();
      eTags.put( address, eTag );
    }
    _sessionManager.setETags( session, eTags );

    sendOk( session.getWebSocketSession(), command.getInt( Messages.Common.REQUEST_ID ) );
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
    replicantSession.setAuthToken( command.getString( Messages.Auth.TOKEN ) );
    sendOk( replicantSession.getWebSocketSession(), command.getInt( Messages.Common.REQUEST_ID ) );
  }

  private void onSubscribe( @Nonnull final ReplicantSession replicantSession, @Nonnull final JsonObject command )
    throws IOException, InterruptedException
  {
    final ChannelAddress address = ChannelAddress.parse( command.getString( Messages.Common.CHANNEL ) );
    final ChannelMetaData channelMetaData = getChannelMetaData( address.channelId() );
    if ( checkSubscribeRequest( replicantSession, channelMetaData, address ) )
    {
      final int requestId = command.getInt( Messages.Common.REQUEST_ID );
      final Object filter = extractFilter( channelMetaData, command );
      _sessionManager.bulkSubscribe( replicantSession, requestId, Collections.singletonList( address ), filter );
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
    else if ( address.hasRootId() && channelMetaData.isTypeGraph() )
    {
      sendErrorAndClose( replicantSession, "Attempted to subscribe to type channel with instance data" );
      return false;
    }
    else if ( !address.hasRootId() && channelMetaData.isInstanceGraph() )
    {
      sendErrorAndClose( replicantSession, "Attempted to subscribe to instance channel without instance data" );
      return false;
    }
    else if ( !validateFilterInstanceId( replicantSession, channelMetaData, address ) )
    {
      return false;
    }
    else
    {
      return true;
    }
  }

  @SuppressWarnings( "DuplicatedCode" )
  private void onBulkSubscribe( @Nonnull final ReplicantSession session, @Nonnull final JsonObject command )
    throws IOException, InterruptedException
  {
    final var addresses = extractChannels( command );
    if ( 0 != addresses.length )
    {
      final var channelId = addresses[ 0 ].channelId();

      final var channelMetaData = getChannelMetaData( channelId );
      for ( final var address : addresses )
      {
        if ( !checkSubscribeRequest( session, channelMetaData, address ) )
        {
          return;
        }
        if ( address.channelId() != channelId )
        {
          sendErrorAndClose( session, "Bulk channel subscribe included addresses from multiple channels" );
          return;
        }
      }

      final var requestId = command.getInt( Messages.Common.REQUEST_ID );
      final var filter = extractFilter( channelMetaData, command );
      _sessionManager.bulkSubscribe( session, requestId, Arrays.asList( addresses ), filter );
    }
  }

  @Nonnull
  private ChannelAddress[] extractChannels( @Nonnull final JsonObject command )
  {
    final var channels = command.getJsonArray( Messages.Update.CHANNELS );
    final var channelCount = channels.size();
    final var addresses = new ChannelAddress[ channelCount ];
    for ( var i = 0; i < channelCount; i++ )
    {
      addresses[ i ] = ChannelAddress.parse( channels.getString( i ) );
    }
    return addresses;
  }

  @Nullable
  private Object extractFilter( @Nonnull final ChannelMetaData channelMetaData, @Nonnull final JsonObject command )
  {
    return channelMetaData.hasFilterParameter() &&
           command.containsKey( Messages.Update.FILTER ) &&
           !command.isNull( Messages.Update.FILTER ) ?
           channelMetaData.getFilterParameterFactory().apply( command.getJsonObject( Messages.Update.FILTER ) ) :
           null;
  }

  private void onUnsubscribe( @Nonnull final ReplicantSession replicantSession, @Nonnull final JsonObject command )
    throws IOException, InterruptedException
  {
    final var address = ChannelAddress.parse( command.getString( Messages.Common.CHANNEL ) );
    final var channelMetaData = getChannelMetaData( address.channelId() );
    if ( checkUnsubscribeRequest( replicantSession, channelMetaData, address ) )
    {
      final var requestId = command.getInt( Messages.Common.REQUEST_ID );
      _sessionManager.bulkUnsubscribe( replicantSession, requestId, Collections.singletonList( address ) );
    }
  }

  @SuppressWarnings( "DuplicatedCode" )
  private void onBulkUnsubscribe( @Nonnull final ReplicantSession session, @Nonnull final JsonObject command )
    throws IOException, InterruptedException
  {
    final ChannelAddress[] addresses = extractChannels( command );
    if ( 0 != addresses.length )
    {
      final var channelId = addresses[ 0 ].channelId();

      final var channelMetaData = getChannelMetaData( channelId );
      for ( final var address : addresses )
      {
        if ( !checkUnsubscribeRequest( session, channelMetaData, address ) )
        {
          return;
        }
        else if ( address.channelId() != channelId )
        {
          sendErrorAndClose( session, "Bulk channel unsubscribe included addresses from multiple channels" );
          return;
        }
      }

      final int requestId = command.getInt( Messages.Common.REQUEST_ID );
      _sessionManager.bulkUnsubscribe( session, requestId, Arrays.asList( addresses ) );
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
    else if ( address.hasRootId() && channelMetaData.isTypeGraph() )
    {
      sendErrorAndClose( replicantSession, "Attempted to unsubscribe from type channel with instance data" );
      return false;
    }
    else if ( !address.hasRootId() && channelMetaData.isInstanceGraph() )
    {
      sendErrorAndClose( replicantSession, "Attempted to unsubscribe from instance channel without instance data" );
      return false;
    }
    else if ( !validateFilterInstanceId( replicantSession, channelMetaData, address ) )
    {
      return false;
    }
    else
    {
      return true;
    }
  }

  private boolean validateFilterInstanceId( @Nonnull final ReplicantSession session,
                                            @Nonnull final ChannelMetaData channelMetaData,
                                            @Nonnull final ChannelAddress address )
    throws IOException
  {
    final boolean hasInstanceId = null != address.filterInstanceId();
    if ( ChannelMetaData.FilterType.DYNAMIC_INSTANCED == channelMetaData.getFilterType() ||
         ChannelMetaData.FilterType.STATIC_INSTANCED == channelMetaData.getFilterType() )
    {
      if ( !hasInstanceId )
      {
        sendErrorAndClose( session, "Attempted to use instanced channel without filter instance id" );
        return false;
      }
      else
      {
        return true;
      }
    }
    else if ( hasInstanceId )
    {
      sendErrorAndClose( session, "Attempted to use non-instanced channel with filter instance id" );
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
      return _sessionManager.getSession( session.getId() );
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

  @Nonnull
  private ChannelMetaData getChannelMetaData( final int channelId )
  {
    return _sessionManager.getSchemaMetaData().getChannelMetaData( channelId );
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
    _sessionManager.invalidateSession( replicantSession );
  }
}
