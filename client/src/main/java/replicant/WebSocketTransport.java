package replicant;

import akasha.MessageEvent;
import akasha.WebSocket;
import akasha.core.JSON;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.base.Any;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;
import replicant.messages.ServerToClientMessage;
import replicant.shared.Messages;

public final class WebSocketTransport
  extends AbstractTransport
{
  @Nonnull
  private final WebSocketConfig _config;
  private WebSocket _webSocket;

  public WebSocketTransport( @Nonnull final WebSocketConfig config )
  {
    _config = Objects.requireNonNull( config );
  }

  @Override
  protected void doConnect()
  {
    _webSocket = new WebSocket( _config.getUrl() );

    _webSocket.onmessage = this::handleMessageEvent;
    _webSocket.onerror = e -> onError();
    _webSocket.onclose = e -> onDisconnect();
  }

  private void handleMessageEvent( @Nonnull final MessageEvent e )
  {
    final Any data = e.data();
    if ( null == data )
    {
      ReplicantLogger.log( "WebSocket message has null data", null );
      onError();
    }
    else
    {
      try
      {
        final ServerToClientMessage message = tryParseMessage( data );
        if ( null == message )
        {
          onError();
        }
        else
        {
          final String type = message.getType();
          if ( isKnownMessageType( type ) )
          {
            onMessageReceived( message );
          }
          else
          {
            ReplicantLogger.log( "Unknown WebSocket message type: " + type, null );
            onError();
          }
        }
      }
      catch ( final Throwable t )
      {
        ReplicantLogger.log( "Failed to parse WebSocket message", t );
        onError();
      }
    }
  }

  private static boolean isKnownMessageType( @Nonnull final String type )
  {
    return Messages.S2C_Type.UPDATE.equals( type ) ||
           Messages.S2C_Type.USE_CACHE.equals( type ) ||
           Messages.S2C_Type.SESSION_CREATED.equals( type ) ||
           Messages.S2C_Type.OK.equals( type ) ||
           Messages.S2C_Type.MALFORMED_MESSAGE.equals( type ) ||
           Messages.S2C_Type.UNKNOWN_REQUEST_TYPE.equals( type ) ||
           Messages.S2C_Type.ERROR.equals( type );
  }

  @Nullable
  private static ServerToClientMessage tryParseMessage( @Nonnull final Any data )
  {
    final String kind = Js.typeof( data );
    Any parsed;
    if ( "string".equals( kind ) )
    {
      parsed = JSON.parse( data.asString() );
    }
    else
    {
      ReplicantLogger.log( "WebSocket message incorrect type: " + kind, null );
      return null;
    }

    if ( null == parsed )
    {
      ReplicantLogger.log( "WebSocket message parsed to null", null );
      return null;
    }
    else
    {
      final JsPropertyMap<?> map = Js.asPropertyMap( parsed );
      if ( null == map || !map.has( "type" ) )
      {
        ReplicantLogger.log( "WebSocket payload missing 'type' property", null );
        return null;
      }
      else
      {
        return parsed.cast();
      }
    }
  }

  @Override
  protected void doDisconnect()
  {
    if ( null != _webSocket )
    {
      final int readyState = _webSocket.readyState();
      if ( WebSocket.OPEN == readyState )
      {
        _webSocket.close();
      }
      else if ( WebSocket.CONNECTING == readyState )
      {
        // It is an error to invoke close() on a socket that is not open, so defer the close until the
        // socket has opened.
        final WebSocket webSocket = _webSocket;
        webSocket.onopen = e -> webSocket.close();
      }
      _webSocket = null;
    }
  }

  @Override
  protected void sendRemoteMessage( @Nonnull final Object message )
  {
    _config.remote( () -> {
      // Attempts to perform a send can occur when there is no connection.
      // This typically happens when a previous request fails.
      if ( null != _webSocket && WebSocket.OPEN == _webSocket.readyState() )
      {
        _webSocket.send( JSON.stringify( message ) );
      }
    } );
  }
}
