package org.realityforge.replicant.client.transport;

import arez.ArezContext;
import arez.Disposable;
import arez.annotations.ContextRef;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.gwt.webpoller.client.RequestFactory;
import org.realityforge.gwt.webpoller.client.WebPoller;
import org.realityforge.gwt.webpoller.client.WebPollerListenerAdapter;
import org.realityforge.replicant.client.gwt.InvalidHttpResponseException;
import org.realityforge.replicant.shared.SharedConstants;
import replicant.ChannelAddress;
import replicant.Replicant;
import replicant.ReplicantContext;
import replicant.Request;
import replicant.SafeProcedure;
import replicant.Transport;
import static org.realityforge.braincheck.Guards.*;

public abstract class WebPollerTransport
  implements Transport
{
  private static final int HTTP_STATUS_CODE_OK = 200;

  @Nonnull
  private final ReplicantContext _replicantContext;
  private WebPoller _webPoller;
  @Nullable
  private String _connectionId;
  @Nullable
  private Context _transportContext;

  public WebPollerTransport( @Nonnull final ReplicantContext replicantContext )
  {
    _replicantContext = Objects.requireNonNull( replicantContext );
  }

  @Nonnull
  protected ReplicantContext getReplicantContext()
  {
    return _replicantContext;
  }

  @ContextRef
  @Nonnull
  protected abstract ArezContext context();

  @Override
  public void bind( @Nonnull final Context context )
  {
    _transportContext = context;
  }

  @Nonnull
  protected final Context ensureTransportContext()
  {
    assert null != _transportContext;
    return _transportContext;
  }

  @Nonnull
  private WebPoller createWebPoller()
  {
    final WebPoller webPoller = WebPoller.newWebPoller();
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> null != webPoller,
                 () -> "Replicant-0084: WebPoller.newWebPoller() returned null. A WebPoller.Factory needs to be registered via a call to WebPoller.register() prior to creating a WebPollerTransport." );
    }
    assert null != webPoller;
    webPoller.setLogLevel( Level.FINEST );
    webPoller.setRequestFactory( newRequestFactory() );
    webPoller.setInterRequestDuration( 0 );
    webPoller.setListener( new ReplicantWebPollerListener() );
    return webPoller;
  }

  @Nonnull
  protected abstract String getEndpointOffset();

  protected abstract String getAppBaseURL();

  @Nullable
  protected String getAuthenticationToken()
  {
    return null;
  }

  @Nonnull
  protected final Transport.OnConnect wrapOnConnect( @Nonnull final OnConnect onConnect )
  {
    return connectionId -> {
      recordConnectionId( connectionId );
      onConnect.onConnect( connectionId );
      startPolling();
    };
  }

  @Nonnull
  protected final SafeProcedure wrapOnDisconnect( @Nonnull final SafeProcedure onDisconnect )
  {
    return () -> {
      stopPolling();
      onDisconnect.call();
      resetConnectionId();
    };
  }

  @Nonnull
  protected final OnError wrapOnDisconnectError( @Nonnull final OnError onDisconnectError )
  {
    return error -> {
      stopPolling();
      onDisconnectError.onError( error );
      resetConnectionId();
    };
  }

  private void recordConnectionId( @Nonnull final String connectionId )
  {
    _connectionId = connectionId;
  }

  private void resetConnectionId()
  {
    _connectionId = null;
  }

  @Nonnull
  protected final String getConnectionId()
  {
    assert null != _connectionId;
    return _connectionId;
  }

  /**
   * Return the base url at which the replicant jaxrs resource is anchored.
   */
  @Nonnull
  private String getBaseURL()
  {
    return getAppBaseURL() + getEndpointOffset();
  }

  /**
   * Return the url to poll for replicant data stream.
   *
   * The implementation derives the url from getBaseURL().
   */
  @Nonnull
  protected String getPollURL()
  {
    return getBaseURL() +
           SharedConstants.REPLICANT_URL_FRAGMENT + "?" +
           SharedConstants.RECEIVE_SEQUENCE_PARAM + "=" + ensureTransportContext().getLastRxSequence();
  }

  /**
   * Return the url to connection service.
   *
   * The implementation derives the url from getBaseURL().
   */
  @Nonnull
  protected String getBaseConnectionURL()
  {
    return getBaseURL() + SharedConstants.CONNECTION_URL_FRAGMENT;
  }

  /**
   * Return the url to the specific resource for specified connection
   */
  @Nonnull
  protected String getConnectionURL()
  {
    return getBaseConnectionURL() + "/" + getConnectionId();
  }

  /**
   * Return URL to the specified channel for this connection.
   */
  @Nonnull
  private String getChannelURL( @Nonnull final ChannelAddress address )
  {
    final int channelId = address.getChannelId();
    final Integer subChannelId = address.getId();
    return getConnectionURL() + SharedConstants.CHANNEL_URL_FRAGMENT +
           "/" + channelId + ( null == subChannelId ? "" : "." + subChannelId );
  }

  @Nonnull
  private String getChannelURL( @Nonnull final List<ChannelAddress> addresses )
  {
    final int channelId = addresses.get( 0 ).getChannelId();
    final String queryParam =
      SharedConstants.SUB_CHANNEL_ID_PARAM + "=" +
      addresses.stream().map( ChannelAddress::getId ).map( Object::toString ).collect( Collectors.joining( "," ) );
    return getConnectionURL() + SharedConstants.CHANNEL_URL_FRAGMENT + "/" + channelId + "?" + queryParam;
  }

  protected void onDisconnectResponse( final int statusCode,
                                       @Nonnull final String statusText,
                                       @Nonnull final SafeProcedure onDisconnect,
                                       @Nonnull final OnError onDisconnectError )
  {
    final Disposable lock = context().pauseScheduler();
    try
    {
      if ( HTTP_STATUS_CODE_OK == statusCode )
      {
        onDisconnect.call();
      }
      else
      {
        onDisconnectError.onError( new InvalidHttpResponseException( statusCode, statusText ) );
      }
    }
    finally
    {
      lock.dispose();
    }
  }

  protected void onConnectResponse( final int statusCode,
                                    @Nonnull final String statusText,
                                    @Nonnull final Supplier<String> content,
                                    @Nonnull final OnConnect onConnect,
                                    @Nonnull final OnError onConnectError )
  {
    if ( HTTP_STATUS_CODE_OK == statusCode )
    {
      onConnect.onConnect( content.get() );
    }
    else
    {
      onConnectError.onError( new InvalidHttpResponseException( statusCode, statusText ) );
    }
  }

  private void handleWebPollerStop()
  {
    ensureTransportContext().disconnect();
  }

  private void onPollMessage( @Nullable final String rawJsonData )
  {
    if ( null != rawJsonData )
    {
      ensureTransportContext().onMessageReceived( rawJsonData );
      pauseWebPoller();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onMessageProcessed()
  {
    resumeWebPoller();
  }

  private void startPolling()
  {
    stopPolling();
    _webPoller = createWebPoller();
    _webPoller.start();
  }

  @Nonnull
  protected abstract RequestFactory newRequestFactory();

  private void stopPolling()
  {
    if ( null != _webPoller )
    {
      if ( _webPoller.isActive() )
      {
        _webPoller.stop();
      }
      _webPoller = null;
    }
  }

  private void pauseWebPoller()
  {
    if ( null != _webPoller && _webPoller.isActive() && !_webPoller.isPaused() )
    {
      _webPoller.pause();
    }
  }

  private void resumeWebPoller()
  {
    if ( null != _webPoller && _webPoller.isActive() && _webPoller.isPaused() )
    {
      _webPoller.resume();
    }
  }

  @Override
  public void requestSubscribe( @Nonnull final ChannelAddress address,
                                @Nullable final Object filter,
                                @Nonnull final SafeProcedure onSuccess,
                                @Nonnull final Consumer<Throwable> onError )
  {
    final Request request = newRequest( toRequestKey( "Subscribe", address ) );
    doSubscribe( request, filter, getChannelURL( address ), null, onSuccess, null, onError );
  }

  @Nonnull
  private Request newRequest( @Nullable final String requestKey )
  {
    return getReplicantContext().newRequest( ensureTransportContext().getSchemaId(), requestKey );
  }

  @Override
  public void requestSubscribe( @Nonnull final ChannelAddress address,
                                @Nullable final Object filter,
                                @Nonnull final String eTag,
                                @Nonnull final SafeProcedure onCacheValid,
                                @Nonnull final SafeProcedure onSuccess,
                                @Nonnull final Consumer<Throwable> onError )
  {
    final Request request = newRequest( toRequestKey( "Subscribe", address ) );
    doSubscribe( request, filter, getChannelURL( address ), eTag, onSuccess, onCacheValid, onError );
  }

  @Override
  public void requestBulkSubscribe( @Nonnull final List<ChannelAddress> addresses,
                                    @Nullable final Object filter,
                                    @Nonnull final SafeProcedure onSuccess,
                                    @Nonnull final Consumer<Throwable> onError )
  {
    final Request request = newRequest( toRequestKey( "BulkSubscribe", addresses ) );
    doSubscribe( request, filter, getChannelURL( addresses ), null, onSuccess, null, onError );
  }

  @Override
  public void requestSubscriptionUpdate( @Nonnull final ChannelAddress address,
                                         @Nonnull final Object filter,
                                         @Nonnull final SafeProcedure onSuccess,
                                         @Nonnull final Consumer<Throwable> onError )
  {
    final Request request =
      newRequest( toRequestKey( "SubscriptionUpdate", address ) );
    doSubscribe( request, filter, getChannelURL( address ), null, onSuccess, null, onError );
  }

  @Override
  public void requestBulkSubscriptionUpdate( @Nonnull final List<ChannelAddress> addresses,
                                             @Nonnull final Object filter,
                                             @Nonnull final SafeProcedure onSuccess,
                                             @Nonnull final Consumer<Throwable> onError )
  {
    final Request request =
      newRequest( toRequestKey( "BulkSubscriptionUpdate", addresses ) );
    doSubscribe( request, filter, getChannelURL( addresses ), null, onSuccess, null, onError );
  }

  @Override
  public void requestUnsubscribe( @Nonnull final ChannelAddress address,
                                  @Nonnull final SafeProcedure onSuccess,
                                  @Nonnull final Consumer<Throwable> onError )
  {
    final Request request = newRequest( toRequestKey( "Unsubscribe", address ) );
    doUnsubscribe( request, getChannelURL( address ), onSuccess, onError );
  }

  @Override
  public void requestBulkUnsubscribe( @Nonnull final List<ChannelAddress> addresses,
                                      @Nonnull final SafeProcedure onSuccess,
                                      @Nonnull final Consumer<Throwable> onError )
  {
    final Request request = newRequest( toRequestKey( "BulkUnsubscribe", addresses ) );
    doUnsubscribe( request, getChannelURL( addresses ), onSuccess, onError );
  }

  protected abstract void doSubscribe( @Nonnull Request request,
                                       @Nullable Object filter,
                                       @Nonnull String channelURL,
                                       @Nullable String eTag,
                                       @Nonnull SafeProcedure onSuccess,
                                       @Nullable SafeProcedure onCacheValid,
                                       @Nonnull Consumer<Throwable> onError );

  protected abstract void doUnsubscribe( @Nonnull Request request,
                                         @Nonnull String channelURL,
                                         @Nonnull SafeProcedure onSuccess,
                                         @Nonnull Consumer<Throwable> onError );

  @Nullable
  private String toRequestKey( @Nonnull final String requestType, @Nonnull final Collection<ChannelAddress> addresses )
  {
    if ( Replicant.areNamesEnabled() )
    {
      final ChannelAddress address = addresses.iterator().next();
      return requestType + ":" + address.getSystemId() + "." + address.getChannelId();
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

  private class ReplicantWebPollerListener
    extends WebPollerListenerAdapter
  {
    @Override
    public void onMessage( @Nonnull final WebPoller webPoller,
                           @Nonnull final Map<String, String> context,
                           @Nonnull final String data )
    {
      onPollMessage( data );
    }

    @Override
    public void onError( @Nonnull final WebPoller webPoller, @Nonnull final Throwable error )
    {
      ensureTransportContext().onMessageReadFailure( error );
    }

    @Override
    public void onStop( @Nonnull final WebPoller webPoller )
    {
      handleWebPollerStop();
    }
  }
}
