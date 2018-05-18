package org.realityforge.replicant.client.transport;

import arez.Disposable;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.gwt.webpoller.client.RequestFactory;
import org.realityforge.gwt.webpoller.client.WebPoller;
import org.realityforge.gwt.webpoller.client.WebPollerListener;
import org.realityforge.gwt.webpoller.client.WebPollerListenerAdapter;
import org.realityforge.replicant.shared.SharedConstants;
import replicant.ChannelAddress;
import replicant.Connection;
import replicant.Replicant;
import replicant.RequestEntry;
import replicant.SafeProcedure;
import replicant.SystemSchema;
import replicant.spy.DataLoadStatus;

public abstract class WebPollerDataLoaderService
  extends AbstractDataLoaderService
{
  protected static final int HTTP_STATUS_CODE_OK = 200;
  private WebPoller _webPoller;

  protected WebPollerDataLoaderService( @Nullable final replicant.ReplicantContext context,
                                        @Nonnull final SystemSchema schema,
                                        @Nonnull final SessionContext sessionContext )
  {
    super( context, schema, sessionContext );
  }

  @Nonnull
  protected WebPoller createWebPoller()
  {
    final WebPoller webpoller = newWebPoller();
    webpoller.setLogLevel( getWebPollerLogLevel() );
    webpoller.setRequestFactory( newRequestFactory() );
    webpoller.setInterRequestDuration( 0 );
    webpoller.setListener( newWebPollerListener() );
    return webpoller;
  }

  @Nonnull
  protected WebPollerListener newWebPollerListener()
  {
    return new ReplicantWebPollerListener();
  }

  @Nonnull
  protected Level getWebPollerLogLevel()
  {
    return Level.FINEST;
  }

  @Nonnull
  protected abstract WebPoller newWebPoller();

  @Nonnull
  protected abstract String getEndpointOffset();

  @Override
  protected void onConnection( @Nonnull final String connectionId, @Nonnull final SafeProcedure action )
  {
    super.onConnection( connectionId, action );
    startPolling();
  }

  @Override
  public void disconnect()
  {
    stopPolling();
    super.disconnect();
  }

  /**
   * Return the base url at which the replicant jaxrs resource is anchored.
   */
  @Nonnull
  protected String getBaseURL()
  {
    return getSessionContext().getBaseURL() + getEndpointOffset();
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
           SharedConstants.RECEIVE_SEQUENCE_PARAM + "=" + ensureConnection().getLastRxSequence();
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
    return getBaseConnectionURL() + "/" + ensureConnection().getConnectionId();
  }

  /**
   * Return URL to the specified channel for this connection.
   */
  @Nonnull
  protected String getChannelURL( @Nonnull final ChannelAddress address )
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

  /**
   * Return the underlying Web Poller used by service.
   */
  @Nonnull
  protected WebPoller getWebPoller()
  {
    if ( null == _webPoller )
    {
      throw new NullPointerException( "_webPoller" );
    }
    return _webPoller;
  }

  protected void onDisconnectError( @Nonnull final Throwable t, @Nonnull final SafeProcedure action )
  {
    super.onDisconnection( action );
    onDisconnectFailure( t );
  }

  protected void onDisconnectResponse( final int statusCode,
                                       @Nonnull final String statusText,
                                       @Nonnull final SafeProcedure action )
  {
    final Disposable lock = context().pauseScheduler();
    try
    {
      if ( HTTP_STATUS_CODE_OK == statusCode )
      {
        super.onDisconnection( action );
      }
      else
      {
        super.onDisconnection( action );
        onDisconnectFailure( new InvalidHttpResponseException( statusCode, statusText ) );
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
                                    @Nonnull final SafeProcedure action )
  {
    if ( HTTP_STATUS_CODE_OK == statusCode )
    {
      onConnection( content.get(), action );
    }
    else
    {
      onConnectFailure( new InvalidHttpResponseException( statusCode, statusText ) );
    }
  }

  protected void handleWebPollerStop()
  {
    disconnect();
  }

  private void onPollMessage( @Nullable final String rawJsonData )
  {
    if ( null != rawJsonData )
    {
      ensureConnection().enqueueResponse( rawJsonData );
      triggerScheduler();
      pauseWebPoller();
    }
  }

  @Override
  protected void onMessageProcessed( @Nonnull final DataLoadStatus status )
  {
    resumeWebPoller();
    super.onMessageProcessed( status );
  }

  protected void startPolling()
  {
    stopPolling();
    _webPoller = createWebPoller();
    _webPoller.start();
  }

  @Nonnull
  protected abstract RequestFactory newRequestFactory();

  @Override
  protected void doSetConnection( @Nullable final Connection connection, @Nonnull final SafeProcedure action )
  {
    if ( null == connection )
    {
      stopPolling();
    }
    super.doSetConnection( connection, action );
  }

  protected void stopPolling()
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

  protected void pauseWebPoller()
  {
    if ( null != _webPoller && _webPoller.isActive() && !_webPoller.isPaused() )
    {
      _webPoller.pause();
    }
  }

  protected void resumeWebPoller()
  {
    if ( null != _webPoller && _webPoller.isActive() && _webPoller.isPaused() )
    {
      _webPoller.resume();
    }
  }

  @Override
  protected void requestSubscribeToChannel( @Nonnull final ChannelAddress address,
                                            @Nullable final Object filter,
                                            @Nullable final String cacheKey,
                                            @Nullable final String eTag,
                                            @Nullable final SafeProcedure onCacheValid,
                                            @Nonnull final SafeProcedure onSuccess,
                                            @Nonnull final Consumer<Throwable> onError )
  {
    //If eTag passed then cache action is expected.
    assert null == eTag || null != onCacheValid;
    getSessionContext().request( toRequestKey( "Subscribe", address ), cacheKey, ( connection, request ) ->
      doSubscribe( connection, request, filter, getChannelURL( address ), eTag, onSuccess, onCacheValid, onError ) );
  }

  @Override
  protected void requestBulkSubscribeToChannel( @Nonnull final List<ChannelAddress> addresses,
                                                @Nullable final Object filter,
                                                @Nonnull final SafeProcedure onSuccess,
                                                @Nonnull final Consumer<Throwable> onError )
  {
    final ChannelAddress address = addresses.get( 0 );
    final ChannelAddress template = new ChannelAddress( address.getSystemId(), address.getChannelId() );
    getSessionContext().request( toRequestKey( "BulkSubscribe", template ), null, ( connection, request ) ->
      doSubscribe( connection, request, filter, getChannelURL( addresses ), null, onSuccess, null, onError ) );
  }

  @Override
  protected void requestUpdateSubscription( @Nonnull final ChannelAddress address,
                                            @Nonnull final Object filter,
                                            @Nonnull final SafeProcedure onSuccess,
                                            @Nonnull final Consumer<Throwable> onError )
  {
    getSessionContext().request( toRequestKey( "SubscriptionUpdate", address ), null, ( connection, request ) ->
      doSubscribe( connection,
                   request,
                   filter,
                   getChannelURL( address ),
                   null,
                   onSuccess,
                   null,
                   onError ) );
  }

  @Override
  protected void requestBulkUpdateSubscription( @Nonnull final List<ChannelAddress> addresses,
                                                @Nonnull final Object filter,
                                                @Nonnull final SafeProcedure onSuccess,
                                                @Nonnull final Consumer<Throwable> onError )
  {
    final ChannelAddress address = addresses.get( 0 );
    final ChannelAddress template = new ChannelAddress( address.getSystemId(), address.getChannelId() );
    getSessionContext().request( toRequestKey( "BulkSubscriptionUpdate", template ), null, ( connection, request ) ->
      doSubscribe( connection, request, filter, getChannelURL( addresses ), null, onSuccess, null, onError ) );
  }

  @Override
  protected void requestUnsubscribeFromChannel( @Nonnull final ChannelAddress address,
                                                @Nonnull final SafeProcedure onSuccess,
                                                @Nonnull final Consumer<Throwable> onError )
  {
    getSessionContext().request( toRequestKey( "Unsubscribe", address ), null, ( connection, request ) ->
      doUnsubscribe( connection, request, getChannelURL( address ), onSuccess, onError ) );
  }

  @Override
  protected void requestBulkUnsubscribeFromChannel( @Nonnull final List<ChannelAddress> addresses,
                                                    @Nonnull final SafeProcedure onSuccess,
                                                    @Nonnull final Consumer<Throwable> onError )
  {
    final ChannelAddress address = addresses.get( 0 );
    final ChannelAddress template = new ChannelAddress( address.getSystemId(), address.getChannelId() );
    getSessionContext().request( toRequestKey( "BulkUnsubscribe", template ), null, ( connection, request ) ->
      doUnsubscribe( connection, request, getChannelURL( addresses ), onSuccess, onError ) );
  }

  protected abstract void doSubscribe( @Nullable Connection connection,
                                       @Nullable RequestEntry request,
                                       @Nullable Object filter,
                                       @Nonnull String channelURL,
                                       @Nullable String eTag,
                                       @Nonnull SafeProcedure onSuccess,
                                       @Nullable SafeProcedure onCacheValid,
                                       @Nonnull Consumer<Throwable> onError );

  protected abstract void doUnsubscribe( @Nullable Connection connection,
                                         @Nullable RequestEntry request,
                                         @Nonnull String channelURL,
                                         @Nonnull SafeProcedure onSuccess,
                                         @Nonnull Consumer<Throwable> onError );

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
      onMessageReadFailure( error );
    }

    @Override
    public void onStop( @Nonnull final WebPoller webPoller )
    {
      handleWebPollerStop();
    }
  }
}
