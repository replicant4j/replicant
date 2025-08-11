package replicant;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;
import replicant.messages.AuthTokenMessage;
import replicant.messages.BulkSubscribeMessage;
import replicant.messages.BulkUnsubscribeMessage;
import replicant.messages.EtagsMessage;
import replicant.messages.ExecMessage;
import replicant.messages.PingMessage;
import replicant.messages.ServerToClientMessage;
import replicant.messages.SubscribeMessage;
import replicant.messages.UnsubscribeMessage;

public abstract class AbstractTransport
  implements Transport
{
  @Nullable
  private TransportContext _transportContext;

  @Override
  public final void unbind()
  {
    doDisconnect();
    _transportContext = null;
  }

  @Override
  public final void requestSync()
  {
    assert null != _transportContext;
    final int requestId = newRequestId( "Sync", true, null );
    sendRemoteMessage( PingMessage.create( requestId ) );
  }

  @Override
  public final void updateAuthToken( @Nullable final String authToken )
  {
    final int requestId = newRequestId( "Auth", true, null );
    sendRemoteMessage( AuthTokenMessage.create( requestId, authToken ) );
  }

  @Override
  public final void updateEtagsSync( @Nonnull final Map<String, String> channelToEtagMap )
  {
    final JsPropertyMap<Object> map = JsPropertyMap.of();
    channelToEtagMap.forEach( map::set );
    final int requestId = newRequestId( "Sync", true, null );
    sendRemoteMessage( EtagsMessage.create( requestId, Js.uncheckedCast( map ) ) );
  }

  @Override
  public void requestExec( @Nonnull final String command,
                           @Nullable final Object payload,
                           @Nullable final ResponseHandler responseHandler )
  {
    final int requestId = newRequestId( "Exec-" + command, responseHandler );
    sendRemoteMessage( ExecMessage.create( requestId, command, payload ) );
  }

  @Override
  public final void requestSubscribe( @Nonnull final ChannelAddress address, @Nullable final Object filter )
  {
    final int requestId = newRequestId( toRequestKey( "Subscribe", address ), null );
    sendRemoteMessage( SubscribeMessage.create( requestId, address.asChannelDescriptor(), filter ) );
  }

  @Override
  public final void requestUnsubscribe( @Nonnull final ChannelAddress address )
  {
    final int requestId = newRequestId( toRequestKey( "Unsubscribe", address ), null );
    sendRemoteMessage( UnsubscribeMessage.create( requestId, address.asChannelDescriptor() ) );
  }

  @Override
  public final void requestBulkSubscribe( @Nonnull final List<ChannelAddress> addresses,
                                          @Nullable final Object filter )
  {
    final int requestId = newRequestId( toRequestKey( "BulkSubscribe", addresses ), null );
    final String[] channels = addresses.stream().map( ChannelAddress::asChannelDescriptor ).toArray( String[]::new );
    sendRemoteMessage( BulkSubscribeMessage.create( requestId, channels, filter ) );
  }

  @Override
  public final void requestBulkUnsubscribe( @Nonnull final List<ChannelAddress> addresses )
  {
    final int requestId = newRequestId( toRequestKey( "BulkUnsubscribe", addresses ), null );
    final String[] channels = addresses.stream().map( ChannelAddress::asChannelDescriptor ).toArray( String[]::new );
    sendRemoteMessage( BulkUnsubscribeMessage.create( requestId, channels ) );
  }

  @Override
  public final void requestConnect( @Nonnull final TransportContext context )
  {
    _transportContext = Objects.requireNonNull( context );
    doConnect();
  }

  @Override
  public final void requestDisconnect()
  {
    doDisconnect();
    _transportContext = null;
  }

  protected final void onMessageReceived( @Nonnull final ServerToClientMessage message )
  {
    // if connection has been disconnected whilst poller request was in flight then ignore response
    if ( null != _transportContext )
    {
      _transportContext.onMessageReceived( message );
    }
  }

  protected final void onError()
  {
    // if connection has been disconnected whilst poller request was in flight then ignore response
    if ( null != _transportContext )
    {
      _transportContext.onError();
    }
  }

  protected final void onDisconnect()
  {
    // if connection has been disconnected then ignore disconnect
    if ( null != _transportContext )
    {
      _transportContext.onDisconnect();
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

  private int newRequestId( @Nullable final String name, @Nullable final ResponseHandler responseHandler )
  {
    return newRequestId( name, false, responseHandler );
  }

  private int newRequestId( @Nullable final String name,
                            final boolean syncRequest,
                            @Nullable final ResponseHandler responseHandler )
  {
    assert null != _transportContext;
    return _transportContext.newRequestId( name, syncRequest, responseHandler );
  }

  protected abstract void doConnect();

  protected abstract void sendRemoteMessage( @Nonnull Object message );

  protected abstract void doDisconnect();
}
