package org.realityforge.replicant.client.transport;

import java.util.Map;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.gwt.webpoller.client.RequestFactory;
import org.realityforge.gwt.webpoller.client.WebPoller;
import org.realityforge.gwt.webpoller.client.WebPollerListenerAdapter;
import org.realityforge.replicant.client.EntityChangeBroker;
import org.realityforge.replicant.client.EntityRepository;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.shared.transport.ReplicantContext;

public abstract class WebPollerDataLoaderService<T extends ClientSession<T, G>, G extends Enum>
  extends AbstractDataLoaderService<T, G>
{
  private WebPoller _webPoller;

  public WebPollerDataLoaderService( @Nonnull final SessionContext sessionContext,
                                     @Nonnull final EntityChangeBroker changeBroker,
                                     @Nonnull final EntityRepository repository,
                                     @Nonnull final CacheService cacheService,
                                     @Nonnull final EntitySubscriptionManager subscriptionManager )
  {
    super( sessionContext, changeBroker, repository, cacheService, subscriptionManager );
  }

  @Nonnull
  protected WebPoller createWebPoller()
  {
    WebPoller _webPoller;
    _webPoller = newWebPoller();
    _webPoller.setLogLevel( getWebPollerLogLevel() );
    _webPoller.setRequestFactory( newRequestFactory() );
    _webPoller.setInterRequestDuration( 0 );
    _webPoller.setListener( new WebPollerListenerAdapter()
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
        handleSystemFailure( exception, "Failed to poll" );
      }
    } );
    return _webPoller;
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

  @Nonnull
  protected abstract T createSession( @Nonnull String sessionID );

  protected void onSessionCreated( @Nonnull final String sessionID, @Nullable final Runnable runnable )
  {
    setSession( createSession( sessionID ), runnable );
    scheduleDataLoad();
    startPolling();
  }

  public void connect( @Nullable final Runnable runnable )
  {
    disconnect( null );
    doConnect( runnable );
  }

  protected abstract void doConnect( @Nullable Runnable runnable );

  protected void handleInvalidConnect( @Nullable final Throwable exception )
  {
    handleSystemFailure( exception, "Failed to generate session token" );
  }

  public void disconnect( @Nullable final Runnable runnable )
  {
    stopPolling();
    final T session = getSession();
    if ( null != session )
    {
      doDisconnect( session, runnable );
    }
  }

  protected abstract void doDisconnect( @Nonnull T session, @Nullable Runnable runnable );

  protected void handleInvalidDisconnect( @Nullable final Throwable exception )
  {
    handleSystemFailure( exception, "Failed to disconnect session" );
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
   * Return the url to generate session token.
   *
   * The implementation derives the url from getBaseURL().
   */
  @Nonnull
  protected String getTokenURL()
  {
    return getBaseURL() + ReplicantContext.SESSION_URL_FRAGMENT;
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

  private void handlePollSuccess( final String rawJsonData )
  {
    if ( null != rawJsonData )
    {
      logResponse( rawJsonData );
      ensureSession().enqueueDataLoad( rawJsonData );
      if ( !getWebPoller().isPaused() )
      {
        getWebPoller().pause();
      }
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
      LOG.info( getSessionContext().getKey() + ".Poll - Received data: " + messageData );
    }
  }

  protected int getThresholdForResponseLogging()
  {
    return 300;
  }

  @Override
  protected void onDataLoadComplete( @Nonnull final DataLoadStatus status )
  {
    final WebPoller webPoller = getWebPoller();
    if ( webPoller.isPaused() )
    {
      webPoller.resume();
    }
    super.onDataLoadComplete( status );
  }

  @Override
  protected void progressDataLoadFailure( @Nonnull final Exception e )
  {
    handleSystemFailure( e, "Failed to progress data load" );
  }

  protected void handleSystemFailure( @Nullable final Throwable caught, @Nonnull final String message )
  {
    LOG.log( Level.SEVERE, "System Failure: " + message, caught );
  }

  protected void startPolling()
  {
    stopPolling();
    _webPoller = createWebPoller();
    _webPoller.start();
  }

  @Nonnull
  protected abstract RequestFactory newRequestFactory();

  protected void stopPolling()
  {
    if ( isConnected() )
    {
      _webPoller.stop();
      _webPoller = null;
    }
  }

  public boolean isConnected()
  {
    return null != _webPoller && getWebPoller().isActive();
  }
}
