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
import org.realityforge.replicant.shared.transport.ReplicantContext;
import replicant.ChannelAddress;
import replicant.Replicant;
import replicant.spy.DataLoadStatus;

public abstract class WebPollerDataLoaderService
  extends AbstractDataLoaderService
{
  protected static final int HTTP_STATUS_CODE_OK = 200;
  private WebPoller _webPoller;

  protected WebPollerDataLoaderService( @Nonnull final ReplicantClientSystem replicantClientSystem,
                                        @Nonnull final CacheService cacheService )
  {
    super( replicantClientSystem, cacheService );
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

  protected void onSessionCreated( @Nonnull final String sessionID, @Nullable final Runnable runnable )
  {
    setSession( new ClientSession( this, sessionID ), runnable );
    scheduleDataLoad();
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
           ReplicantContext.REPLICANT_URL_FRAGMENT + "?" +
           ReplicantContext.RECEIVE_SEQUENCE_PARAM + "=" + ensureSession().getLastRxSequence();
  }

  /**
   * Return the url to session service.
   *
   * The implementation derives the url from getBaseURL().
   */
  @Nonnull
  protected String getBaseSessionURL()
  {
    return getBaseURL() + ReplicantContext.SESSION_URL_FRAGMENT;
  }

  /**
   * Return the url to the specific resource for specified session
   */
  @Nonnull
  protected String getSessionURL()
  {
    return getBaseSessionURL() + "/" + ensureSession().getSessionID();
  }

  /**
   * Return URL to the specified channel for this session.
   */
  @Nonnull
  protected String getChannelURL( final int channel,
                                  @Nullable Integer subChannelId )
  {
    return getSessionURL() + ReplicantContext.CHANNEL_URL_FRAGMENT +
           "/" + channel + ( null == subChannelId ? "" : "." + subChannelId );
  }

  /**
   * Return URL to the specified channel, for the set of subChannelIds for this session.
   */
  @Nonnull
  protected String getChannelURL( final int channel,
                                  @Nonnull List<Integer> subChannelIds )
  {
    final String queryParam = ReplicantContext.SUB_CHANNEL_ID_PARAM + "=" +
                              subChannelIds.stream().map( Object::toString ).collect( Collectors.joining( "," ) );
    return getSessionURL() + ReplicantContext.CHANNEL_URL_FRAGMENT + "/" + channel + "?" + queryParam;
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

  protected void onDisconnectError( @Nonnull final Throwable t, @Nullable final Runnable runnable )
  {
    setSession( null, runnable );
    onInvalidDisconnect( t );
  }

  protected void onDisconnectResponse( final int statusCode,
                                       @Nonnull final String statusText,
                                       @Nullable final Runnable action )
  {
    final Disposable lock = context().pauseScheduler();
    try
    {
      if ( HTTP_STATUS_CODE_OK == statusCode )
      {
        setSession( null, action );
      }
      else
      {
        setSession( null, action );
        onInvalidDisconnect( new InvalidHttpResponseException( statusCode, statusText ) );
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
                                    @Nullable final Runnable runnable )
  {
    if ( HTTP_STATUS_CODE_OK == statusCode )
    {
      onSessionCreated( content.get(), runnable );
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

  private void handlePollSuccess( final String rawJsonData )
  {
    if ( null != rawJsonData )
    {
      logResponse( rawJsonData );
      ensureSession().enqueueDataLoad( rawJsonData );
      pauseWebPoller();
    }
  }

  private void logResponse( final String rawJsonData )
  {
    if ( LOG.isLoggable( Level.INFO ) )
    {
      final int threshold = getThresholdForResponseLogging();
      final String messageData =
        0 != threshold && rawJsonData.length() > threshold ?
        rawJsonData.substring( 0, threshold ) + "..." :
        rawJsonData;
      LOG.info( getKey() + ".Poll - Received data: " + messageData );
    }
  }

  protected int getThresholdForResponseLogging()
  {
    return 300;
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
  protected void doSetSession( @Nullable final ClientSession session, @Nullable final Runnable postAction )
  {
    if ( null == session )
    {
      stopPolling();
    }
    super.doSetSession( session, postAction );
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
                                            @Nullable final Object filterParameter,
                                            @Nullable final String cacheKey,
                                            @Nullable final String eTag,
                                            @Nullable final Consumer<Runnable> cacheAction,
                                            @Nonnull final Consumer<Runnable> completionAction,
                                            @Nonnull final Consumer<Runnable> failAction )
  {
    //If eTag passed then cache action is expected.
    assert null == eTag || null != cacheAction;
    if ( isChannelTypeValid( address ) )
    {
      onSubscribeStarted( address );
      final Runnable onSuccess = () -> completionAction.accept( () -> onSubscribeCompleted( address ) );
      final Runnable onCacheValid =
        null != cacheAction ? () -> cacheAction.accept( () -> onSubscribeCompleted( address ) ) : null;
      final Consumer<Throwable> onError = error -> failAction.accept( () -> onSubscribeFailed( address, error ) );
      performSubscribe( address.getChannelType().ordinal(),
                        address.getId(),
                        filterParameter,
                        cacheKey,
                        eTag,
                        onSuccess,
                        onCacheValid,
                        onError );
    }
    else
    {
      throw new IllegalStateException();
    }
  }

  private boolean isChannelTypeValid( @Nonnull final ChannelAddress address )
  {
    return getSystemType() == address.getChannelType().getClass();
  }

  protected void performSubscribe( final int channel,
                                   @Nullable Integer subChannelId,
                                   @Nullable final Object filterParameter,
                                   @Nullable String cacheKey,
                                   @Nullable String eTag,
                                   @Nonnull final Runnable onSuccess,
                                   @Nullable final Runnable onCacheValid,
                                   @Nonnull final Consumer<Throwable> onError )
  {
    getSessionContext().request( toRequestKey( "Subscribe", channel ), cacheKey, ( session, request ) ->
      doSubscribe( session,
                   request,
                   filterParameter,
                   getChannelURL( channel, subChannelId ),
                   eTag,
                   onSuccess,
                   onCacheValid,
                   onError ) );
  }

  @Override
  protected void requestBulkSubscribeToChannel( @Nonnull final List<ChannelAddress> addresses,
                                                @Nullable final Object filterParameter,
                                                @Nonnull final Consumer<Runnable> completionAction,
                                                @Nonnull final Consumer<Runnable> failAction )
  {
    final ChannelAddress address = addresses.get( 0 );
    if ( isChannelTypeValid( address ) )
    {
      addresses.forEach( this::onSubscribeStarted );
      final Runnable onSuccess =
        () -> completionAction.accept( () -> addresses.forEach( this::onSubscribeCompleted ) );
      final Consumer<Throwable> onError =
        error -> failAction.accept( () -> addresses.forEach( x -> onSubscribeFailed( address, error ) ) );
      performBulkSubscribe( address.getChannelType().ordinal(),
                            addresses.stream().map( ChannelAddress::getId ).collect( Collectors.toList() ),
                            filterParameter,
                            onSuccess,
                            onError );
    }
    else
    {
      throw new IllegalStateException();
    }
  }

  protected void performBulkSubscribe( final int channel,
                                       @Nonnull List<Integer> subChannelIds,
                                       @Nullable final Object filterParameter,
                                       @Nonnull final Runnable onSuccess,
                                       @Nonnull final Consumer<Throwable> onError )
  {
    getSessionContext().request( toRequestKey( "BulkSubscribe", channel ), null, ( session, request ) ->
      doSubscribe( session,
                   request,
                   filterParameter,
                   getChannelURL( channel, subChannelIds ),
                   null,
                   onSuccess,
                   null,
                   onError ) );
  }

  @Override
  protected void requestUpdateSubscription( @Nonnull final ChannelAddress descriptor,
                                            @Nonnull final Object filterParameter,
                                            @Nonnull final Consumer<Runnable> completionAction,
                                            @Nonnull final Consumer<Runnable> failAction )
  {
    if ( isChannelTypeValid( descriptor ) )
    {
      onSubscriptionUpdateStarted( descriptor );
      final Runnable onSuccess =
        () -> completionAction.accept( () -> onSubscriptionUpdateCompleted( descriptor ) );
      final Consumer<Throwable> onError =
        error -> failAction.accept( () -> onSubscriptionUpdateFailed( descriptor, error ) );
      performUpdateSubscription( descriptor.getChannelType().ordinal(),
                                 descriptor.getId(),
                                 filterParameter,
                                 onSuccess,
                                 onError );
    }
    else
    {
      throw new IllegalStateException();
    }
  }

  protected void performUpdateSubscription( final int channel,
                                            @Nullable Integer subChannelId,
                                            @Nullable final Object filterParameter,
                                            @Nonnull final Runnable onSuccess,
                                            @Nonnull final Consumer<Throwable> onError )
  {
    getSessionContext().request( toRequestKey( "SubscriptionUpdate", channel ), null, ( session, request ) ->
      doSubscribe( session,
                   request,
                   filterParameter,
                   getChannelURL( channel, subChannelId ),
                   null,
                   onSuccess,
                   null,
                   onError ) );
  }

  protected void requestBulkUpdateSubscription( @Nonnull List<ChannelAddress> descriptors,
                                                @Nonnull Object filterParameter,
                                                @Nonnull Consumer<Runnable> completionAction,
                                                @Nonnull Consumer<Runnable> failAction )
  {
    final ChannelAddress address = descriptors.get( 0 );
    if ( isChannelTypeValid( address ) )
    {
      descriptors.forEach( this::onSubscriptionUpdateStarted );
      final Runnable onSuccess =
        () -> completionAction.accept( () -> descriptors.forEach( x -> onSubscriptionUpdateCompleted( address ) ) );
      final Consumer<Throwable> onError =
        error -> failAction.accept( () -> descriptors.forEach( x -> onSubscriptionUpdateFailed( address, error ) ) );
      performBulkUpdateSubscription( address.getChannelType().ordinal(),
                                     descriptors.stream().map( ChannelAddress::getId ).collect( Collectors.toList() ),
                                     filterParameter,
                                     onSuccess,
                                     onError );
    }
    else
    {
      throw new IllegalStateException();
    }
  }

  protected void performBulkUpdateSubscription( final int channel,
                                                @Nonnull List<Integer> subChannelIds,
                                                @Nullable final Object filterParameter,
                                                @Nonnull final Runnable onSuccess,
                                                @Nonnull final Consumer<Throwable> onError )
  {
    getSessionContext().request( toRequestKey( "BulkSubscriptionUpdate", channel ), null, ( session, request ) ->
      doSubscribe( session,
                   request,
                   filterParameter,
                   getChannelURL( channel, subChannelIds ),
                   null,
                   onSuccess,
                   null,
                   onError ) );
  }

  @Override
  protected void requestUnsubscribeFromChannel( @Nonnull final ChannelAddress address,
                                                @Nonnull final Consumer<Runnable> completionAction,
                                                @Nonnull final Consumer<Runnable> failAction )
  {
    if ( isChannelTypeValid( address ) )
    {
      onUnsubscribeStarted( address );
      final Consumer<Throwable> onError =
        error -> failAction.accept( () -> onUnsubscribeFailed( address, error ) );
      final Runnable onSuccess = () -> completionAction.accept( () -> onUnsubscribeCompleted( address ) );
      performUnsubscribe( address.getChannelType().ordinal(), address.getId(), onSuccess, onError );
    }
    else
    {
      throw new IllegalStateException();
    }
  }

  protected void performUnsubscribe( final int channel,
                                     @Nullable Integer subChannelId,
                                     @Nonnull final Runnable onSuccess,
                                     @Nonnull final Consumer<Throwable> onError )
  {
    getSessionContext().request( toRequestKey( "Unsubscribe", channel ), null, ( session, request ) ->
      doUnsubscribe( session, request, getChannelURL( channel, subChannelId ), onSuccess, onError ) );
  }

  @Override
  protected void requestBulkUnsubscribeFromChannel( @Nonnull final List<ChannelAddress> addresses,
                                                    @Nonnull final Consumer<Runnable> completionAction,
                                                    @Nonnull final Consumer<Runnable> failAction )
  {
    final ChannelAddress address = addresses.get( 0 );
    if ( isChannelTypeValid( address ) )
    {
      addresses.forEach( this::onUnsubscribeStarted );
      final Runnable onSuccess =
        () -> completionAction.accept( () -> addresses.forEach( this::onUnsubscribeCompleted ) );
      final Consumer<Throwable> onError =
        error -> failAction.accept( () -> addresses.forEach( x -> onUnsubscribeFailed( address, error ) ) );
      performBulkUnsubscribe( address.getChannelType().ordinal(),
                              addresses.stream().map( ChannelAddress::getId ).collect( Collectors.toList() ),
                              onSuccess,
                              onError );
    }
    else
    {
      throw new IllegalStateException();
    }
  }

  protected void performBulkUnsubscribe( final int channel,
                                         @Nonnull List<Integer> subChannelIds,
                                         @Nonnull final Runnable onSuccess,
                                         @Nonnull final Consumer<Throwable> onError )
  {
    getSessionContext().request( toRequestKey( "BulkUnsubscribe", channel ), null, ( session, request ) ->
      doUnsubscribe( session,
                     request,
                     getChannelURL( channel, subChannelIds ),
                     onSuccess,
                     onError ) );
  }

  protected abstract void doSubscribe( @Nullable ClientSession session,
                                       @Nullable RequestEntry request,
                                       @Nullable Object filterParameter,
                                       @Nonnull String channelURL,
                                       @Nullable String eTag,
                                       @Nonnull Runnable onSuccess,
                                       @Nullable Runnable onCacheValid,
                                       @Nonnull Consumer<Throwable> onError );

  protected abstract void doUnsubscribe( @Nullable ClientSession session,
                                         @Nullable RequestEntry request,
                                         @Nonnull String channelURL,
                                         @Nonnull Runnable onSuccess,
                                         @Nonnull Consumer<Throwable> onError );

  @Nullable
  private String toRequestKey( @Nonnull final String requestType, final int channel )
  {
    return Replicant.shouldRecordRequestKey() ?
           requestType + ":" + getSystemType().getEnumConstants()[ channel ] :
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
      handlePollSuccess( data );
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
