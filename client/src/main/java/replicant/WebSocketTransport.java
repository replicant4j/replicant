package replicant;

import elemental2.core.Global;
import elemental2.dom.WebSocket;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.base.Js;
import replicant.messages.BulkSubscribeMessage;
import replicant.messages.BulkUnsubscribeMessage;
import replicant.messages.PingMessage;
import replicant.messages.SubscribeMessage;
import replicant.messages.UnsubscribeMessage;

public class WebSocketTransport
  implements Transport
{
  @Nonnull
  private final WebSocketConfig _config;
  private WebSocket _webSocket;
  @Nullable
  private TransportContext _transportContext;

  public WebSocketTransport( @Nonnull final WebSocketConfig config )
  {
    _config = Objects.requireNonNull( config );
  }

  private void remote( @Nonnull final Runnable action )
  {
    _config.remote( () -> {
      /*
       * Attempts to perform a remote request can occur when there is no connection.
       * This typically happens when a previous request fails.
       */
      if ( null != _webSocket && WebSocket.OPEN == _webSocket.readyState )
      {
        action.run();
      }
    } );
  }

  @Override
  public void unbind()
  {
    closeWebSocket();
    _transportContext = null;
  }

  @Override
  public void requestSync()
  {
    assert null != _transportContext;
    sendRemoteMessage( PingMessage.create( _transportContext.newRequest( "Sync", true ).getRequestId() ) );
  }

  @Override
  public void requestSubscribe( @Nonnull final ChannelAddress address,
                                @Nullable final Object filter,
                                @Nullable final SafeProcedure onCacheValid )
  {
    assert null != _transportContext;
    final RequestEntry request = _transportContext.newRequest( toRequestKey( "Subscribe", address ), false );
    sendRemoteMessage( SubscribeMessage.create( request.getRequestId(), address.asChannelDescriptor(), filter ) );
  }

  @Override
  public void requestUnsubscribe( @Nonnull final ChannelAddress address )
  {
    assert null != _transportContext;
    final RequestEntry request = _transportContext.newRequest( toRequestKey( "Unsubscribe", address ), false );
    sendRemoteMessage( UnsubscribeMessage.create( request.getRequestId(), address.asChannelDescriptor() ) );
  }

  @Override
  public void requestBulkSubscribe( @Nonnull final List<ChannelAddress> addresses,
                                    @Nullable final Object filter )
  {
    assert null != _transportContext;
    final RequestEntry request = _transportContext.newRequest( toRequestKey( "BulkSubscribe", addresses ), false );
    final String[] channels = addresses.stream().map( ChannelAddress::asChannelDescriptor ).toArray( String[]::new );
    sendRemoteMessage( BulkSubscribeMessage.create( request.getRequestId(), channels, filter ) );
  }

  @Override
  public void requestBulkUnsubscribe( @Nonnull final List<ChannelAddress> addresses )
  {
    assert null != _transportContext;
    final RequestEntry request = _transportContext.newRequest( toRequestKey( "BulkUnsubscribe", addresses ), false );
    final String[] channels = addresses.stream().map( ChannelAddress::asChannelDescriptor ).toArray( String[]::new );
    sendRemoteMessage( BulkUnsubscribeMessage.create( request.getRequestId(), channels ) );
  }

  @Override
  public void requestConnect( @Nonnull final TransportContext context )
  {
    _transportContext = Objects.requireNonNull( context );
    closeWebSocket();

    _webSocket = new WebSocket( _config.getUrl() );

    _webSocket.onmessage = e -> onWebSocketMessage( (String) e.data );
    //TODO: Fix error handling so propagate error from ... somewhere
    _webSocket.onerror = e -> onWebSocketError( new IllegalStateException() );
    _webSocket.onclose = e -> onWebSocketClose();
  }

  @Override
  public void requestDisconnect()
  {
    closeWebSocket();
    _transportContext = null;
  }

  private void onWebSocketMessage( @Nonnull final String rawJsonData )
  {
    // if connection has been disconnected whilst poller request was in flight then ignore response
    if ( null != _transportContext )
    {
      _transportContext.onMessageReceived( Js.cast( Global.JSON.parse( rawJsonData ) ) );
    }
  }

  private void onWebSocketError( @Nonnull final Throwable error )
  {
    // if connection has been disconnected whilst poller request was in flight then ignore response
    if ( null != _transportContext )
    {
      _transportContext.onError( error );
    }
  }

  private void onWebSocketClose()
  {
    // if connection has been disconnected then ignore disconnect
    if ( null != _transportContext )
    {
      _transportContext.onDisconnect();
    }
  }

  private void closeWebSocket()
  {
    if ( null != _webSocket )
    {
      if ( WebSocket.CLOSED != _webSocket.readyState && WebSocket.CLOSING != _webSocket.readyState )
      {
        _webSocket.close();
      }
      _webSocket = null;
    }
  }

  @Nullable
  private String toRequestKey( @Nonnull final String requestType, @Nonnull final Collection<ChannelAddress> addresses )
  {
    if ( Replicant.areNamesEnabled() )
    {
      final ChannelAddress address = addresses.iterator().next();
      return requestType + ":" + address.getName();
    }
    else
    {
      return null;
    }
  }

  @Nullable
  private String toRequestKey( @Nonnull final String requestType, @Nonnull final ChannelAddress address )
  {
    return Replicant.areNamesEnabled() ?
           requestType + ":" + address :
           null;
  }

  private void sendRemoteMessage( @Nonnull final Object message )
  {
    remote( () -> _webSocket.send( Global.JSON.stringify( message ) ) );
  }
}
