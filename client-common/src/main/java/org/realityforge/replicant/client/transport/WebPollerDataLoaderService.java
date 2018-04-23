package org.realityforge.replicant.client.transport;

import arez.Disposable;
import java.io.Serializable;
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
import org.realityforge.replicant.client.ChannelAddress;
import org.realityforge.replicant.shared.transport.ReplicantContext;

public abstract class WebPollerDataLoaderService
  extends AbstractDataLoaderService
{
  protected static final int HTTP_STATUS_CODE_OK = 200;
  private WebPoller _webPoller;

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
                                  @Nullable Serializable subChannelID )
  {
    return getSessionURL() + ReplicantContext.CHANNEL_URL_FRAGMENT +
           "/" + channel + ( null == subChannelID ? "" : "." + subChannelID );
  }

  /**
   * Return URL to the specified channel, for the set of subChannelIDs for this session.
   */
  @Nonnull
  protected String getChannelURL( final int channel,
                                  @Nonnull List<Serializable> subChannelIDs )
  {
    final String queryParam = ReplicantContext.SUB_CHANNEL_ID_PARAM + "=" +
                              subChannelIDs.stream().map( Object::toString ).collect( Collectors.joining( "," ) );
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
    handleInvalidDisconnect( t );
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
        handleInvalidDisconnect( new InvalidHttpResponseException( statusCode, statusText ) );
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
      handleInvalidConnect( new InvalidHttpResponseException( statusCode, statusText ) );
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
  protected void onDataLoadComplete( @Nonnull final DataLoadStatus status )
  {
    resumeWebPoller();
    super.onDataLoadComplete( status );
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
  protected void requestSubscribeToGraph( @Nonnull final ChannelAddress descriptor,
                                          @Nullable final Object filterParameter,
                                          @Nullable final String cacheKey,
                                          @Nullable final String eTag,
                                          @Nullable final Consumer<Runnable> cacheAction,
                                          @Nonnull final Consumer<Runnable> completionAction,
                                          @Nonnull final Consumer<Runnable> failAction )
  {
    //If eTag passed then cache action is expected.
    assert null == eTag || null != cacheAction;
    if ( isGraphValid( descriptor ) )
    {
      getListener().onSubscribeStarted( this, descriptor );
      final Runnable onSuccess =
        () -> completionAction.accept( () -> getListener().onSubscribeCompleted( this, descriptor ) );
      final Runnable onCacheValid =
        null != cacheAction ?
        () -> cacheAction.accept( () -> getListener().onSubscribeCompleted( this, descriptor ) ) :
        null;
      final Consumer<Throwable> onError =
        throwable -> failAction.accept( () -> getListener().onSubscribeFailed( this, descriptor, throwable ) );
      performSubscribe( descriptor.getGraph().ordinal(),
                        (Serializable) descriptor.getId(),
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

  private boolean isGraphValid( @Nonnull final ChannelAddress descriptor )
  {
    return getGraphType() == descriptor.getGraph().getClass();
  }

  protected void performSubscribe( final int channel,
                                   @Nullable Serializable subChannelID,
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
                   getChannelURL( channel, subChannelID ),
                   eTag,
                   onSuccess,
                   onCacheValid,
                   onError ) );
  }

  @Override
  protected void requestBulkSubscribeToGraph( @Nonnull final List<ChannelAddress> descriptors,
                                              @Nullable final Object filterParameter,
                                              @Nonnull final Consumer<Runnable> completionAction,
                                              @Nonnull final Consumer<Runnable> failAction )
  {
    final ChannelAddress descriptor = descriptors.get( 0 );
    if ( isGraphValid( descriptor ) )
    {
      descriptors.forEach( x -> getListener().onSubscribeStarted( this, x ) );
      final Runnable onSuccess =
        () -> completionAction.accept( () -> descriptors.forEach( x -> getListener().onSubscribeCompleted( this,
                                                                                                           x ) ) );
      final Consumer<Throwable> onError =
        throwable -> failAction.accept( () -> descriptors.forEach( x -> getListener().onSubscribeFailed( this,
                                                                                                         descriptor,
                                                                                                         throwable ) ) );
      performBulkSubscribe( descriptor.getGraph().ordinal(),
                            descriptors.stream().map( x -> (Serializable) x.getId() ).collect( Collectors.toList() ),
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
                                       @Nonnull List<Serializable> subChannelIDs,
                                       @Nullable final Object filterParameter,
                                       @Nonnull final Runnable onSuccess,
                                       @Nonnull final Consumer<Throwable> onError )
  {
    getSessionContext().request( toRequestKey( "BulkSubscribe", channel ), null, ( session, request ) ->
      doSubscribe( session,
                   request,
                   filterParameter,
                   getChannelURL( channel, subChannelIDs ),
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
    if ( isGraphValid( descriptor ) )
    {
      getListener().onSubscriptionUpdateStarted( this, descriptor );
      final Runnable onSuccess =
        () -> completionAction.accept( () -> getListener().onSubscriptionUpdateCompleted( this, descriptor ) );
      final Consumer<Throwable> onError =
        throwable -> failAction.accept( () -> getListener().onSubscriptionUpdateFailed( this, descriptor, throwable ) );
      performUpdateSubscription( descriptor.getGraph().ordinal(),
                                 (Serializable) descriptor.getId(),
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
                                            @Nullable Serializable subChannelID,
                                            @Nullable final Object filterParameter,
                                            @Nonnull final Runnable onSuccess,
                                            @Nonnull final Consumer<Throwable> onError )
  {
    getSessionContext().request( toRequestKey( "SubscriptionUpdate", channel ), null, ( session, request ) ->
      doSubscribe( session,
                   request,
                   filterParameter,
                   getChannelURL( channel, subChannelID ),
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
    final ChannelAddress descriptor = descriptors.get( 0 );
    if ( isGraphValid( descriptor ) )
    {
      descriptors.forEach( x -> getListener().onSubscriptionUpdateStarted( this, x ) );
      final Runnable onSuccess = () -> completionAction.accept( () -> descriptors.forEach(
        x -> getListener().onSubscriptionUpdateCompleted( this, descriptor ) ) );
      final Consumer<Throwable> onError = throwable -> failAction.accept( () -> descriptors.forEach(
        x -> getListener().onSubscriptionUpdateFailed( this, descriptor, throwable ) ) );
      performBulkUpdateSubscription( descriptor.getGraph().ordinal(),
                                     descriptors.stream().map(
                                       x -> (Serializable) x.getId() ).collect( Collectors.toList() ),
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
                                                @Nonnull List<Serializable> subChannelIDs,
                                                @Nullable final Object filterParameter,
                                                @Nonnull final Runnable onSuccess,
                                                @Nonnull final Consumer<Throwable> onError )
  {
    getSessionContext().request( toRequestKey( "BulkSubscriptionUpdate", channel ), null, ( session, request ) ->
      doSubscribe( session,
                   request,
                   filterParameter,
                   getChannelURL( channel, subChannelIDs ),
                   null,
                   onSuccess,
                   null,
                   onError ) );
  }

  @Override
  protected void requestUnsubscribeFromGraph( @Nonnull final ChannelAddress descriptor,
                                              @Nonnull final Consumer<Runnable> completionAction,
                                              @Nonnull final Consumer<Runnable> failAction )
  {
    if ( isGraphValid( descriptor ) )
    {
      getListener().onUnsubscribeStarted( this, descriptor );
      final Consumer<Throwable> onError =
        throwable -> failAction.accept( () -> getListener().onUnsubscribeFailed( this, descriptor, throwable ) );
      final Runnable onSuccess =
        () -> completionAction.accept( () -> getListener().onUnsubscribeCompleted( this, descriptor ) );
      performUnsubscribe( descriptor.getGraph().ordinal(), (Serializable) descriptor.getId(), onSuccess, onError );
    }
    else
    {
      throw new IllegalStateException();
    }
  }

  protected void performUnsubscribe( final int channel,
                                     @Nullable Serializable subChannelID,
                                     @Nonnull final Runnable onSuccess,
                                     @Nonnull final Consumer<Throwable> onError )
  {
    getSessionContext().request( toRequestKey( "Unsubscribe", channel ), null, ( session, request ) ->
      doUnsubscribe( session, request, getChannelURL( channel, subChannelID ), onSuccess, onError ) );
  }

  @Override
  protected void requestBulkUnsubscribeFromGraph( @Nonnull final List<ChannelAddress> descriptors,
                                                  @Nonnull final Consumer<Runnable> completionAction,
                                                  @Nonnull final Consumer<Runnable> failAction )
  {
    final ChannelAddress descriptor = descriptors.get( 0 );
    if ( isGraphValid( descriptor ) )
    {
      descriptors.forEach( x -> getListener().onUnsubscribeStarted( this, x ) );
      final Runnable onSuccess =
        () -> completionAction.accept( () -> descriptors.forEach( x -> getListener().onUnsubscribeCompleted( this,
                                                                                                             x ) ) );
      final Consumer<Throwable> onError =
        throwable -> failAction.accept( () -> descriptors.forEach( x -> getListener().onUnsubscribeFailed( this,
                                                                                                           descriptor,
                                                                                                           throwable ) ) );
      performBulkUnsubscribe( descriptor.getGraph().ordinal(),
                              descriptors.stream().map( x -> (Serializable) x.getId() ).collect( Collectors.toList() ),
                              onSuccess,
                              onError );
    }
    else
    {
      throw new IllegalStateException();
    }
  }

  protected void performBulkUnsubscribe( final int channel,
                                         @Nonnull List<Serializable> subChannelIDs,
                                         @Nonnull final Runnable onSuccess,
                                         @Nonnull final Consumer<Throwable> onError )
  {
    getSessionContext().request( toRequestKey( "BulkUnsubscribe", channel ), null, ( session, request ) ->
      doUnsubscribe( session,
                     request,
                     getChannelURL( channel, subChannelIDs ),
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
    return config().shouldRecordRequestKey() ?
           requestType + ":" + getGraphType().getEnumConstants()[ channel ] :
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
    public void onError( @Nonnull final WebPoller webPoller, @Nonnull final Throwable exception )
    {
      getListener().onPollFailure( WebPollerDataLoaderService.this, exception );
    }

    @Override
    public void onStop( @Nonnull final WebPoller webPoller )
    {
      handleWebPollerStop();
    }
  }
}
