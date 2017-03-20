package org.realityforge.replicant.client.transport;

import java.util.Map;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.gwt.webpoller.client.RequestFactory;
import org.realityforge.gwt.webpoller.client.WebPoller;
import org.realityforge.gwt.webpoller.client.WebPollerListenerAdapter;
import org.realityforge.replicant.shared.transport.ReplicantContext;

public abstract class WebPollerDataLoaderService<T extends ClientSession<T, G>, G extends Enum>
  extends AbstractDataLoaderService<T, G>
{
  private WebPoller _webPoller;

  @Nonnull
  protected WebPoller createWebPoller()
  {
    final WebPoller webpoller = newWebPoller();
    webpoller.setLogLevel( getWebPollerLogLevel() );
    webpoller.setRequestFactory( newRequestFactory() );
    webpoller.setInterRequestDuration( 0 );
    webpoller.setListener( new WebPollerListenerAdapter()
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
        handlePollFailure( exception );
      }

      @Override
      public void onStop( @Nonnull final WebPoller webPoller )
      {
        handleWebPollerStop();
      }
    } );
    return webpoller;
  }

  @Nonnull
  protected Level getWebPollerLogLevel()
  {
    return Level.INFO;
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

  public void disconnect( @Nullable final Runnable runnable )
  {
    stopPolling();
    super.disconnect( runnable );
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

  private void handlePollFailure( @Nonnull final Throwable exception )
  {
    handleSystemFailure( exception, "Failed to poll" );
    firePollFailure( exception );
  }

  protected void handleWebPollerStop()
  {
    disconnect( null );
  }

  /**
   * Invoked to fire an event when poll failed.
   */
  protected void firePollFailure( @Nonnull final Throwable exception )
  {
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
  protected void doSetSession( @Nullable final T session, @Nullable final Runnable postAction )
  {
    if( null == session )
    {
      stopPolling();
    }
    super.doSetSession( session, postAction );
  }

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

  protected void pauseWebPoller()
  {
    if ( !getWebPoller().isPaused() )
    {
      getWebPoller().pause();
    }
  }

  protected void resumeWebPoller()
  {
    final WebPoller webPoller = getWebPoller();
    if ( webPoller.isPaused() )
    {
      webPoller.resume();
    }
  }
}
