package replicant;

import elemental2.core.Global;
import elemental2.dom.WebSocket;
import java.util.Objects;
import javax.annotation.Nonnull;
import jsinterop.base.Js;

public class WebSocketTransport
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

    _webSocket.onmessage = e -> onMessageReceived( Js.cast( Global.JSON.parse( e.data.asString() ) ) );
    _webSocket.onerror = e -> onError();
    _webSocket.onclose = e -> onDisconnect();
  }

  @Override
  protected void doDisconnect()
  {
    if ( null != _webSocket )
    {
      if ( WebSocket.OPEN == _webSocket.readyState )
      {
        _webSocket.close();
      }
      else if ( WebSocket.CONNECTING == _webSocket.readyState )
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
  protected final void sendRemoteMessage( @Nonnull final Object message )
  {
    _config.remote( () -> {
      // Attempts to perform a send can occur when there is no connection.
      // This typically happens when a previous request fails.
      if ( null != _webSocket && WebSocket.OPEN == _webSocket.readyState )
      {
        _webSocket.send( Global.JSON.stringify( message ) );
      }
    } );
  }
}
