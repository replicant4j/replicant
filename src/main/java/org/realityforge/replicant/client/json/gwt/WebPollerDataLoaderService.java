package org.realityforge.replicant.client.json.gwt;

import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.user.client.Window;
import java.util.Map;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.realityforge.gwt.webpoller.client.AbstractHttpRequestFactory;
import org.realityforge.gwt.webpoller.client.WebPoller;
import org.realityforge.gwt.webpoller.client.WebPollerListenerAdapter;
import org.realityforge.replicant.client.ChangeMapper;
import org.realityforge.replicant.client.EntityChangeBroker;
import org.realityforge.replicant.client.EntityRepository;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.client.transport.CacheService;
import org.realityforge.replicant.client.transport.ClientSession;
import org.realityforge.replicant.client.transport.DataLoadStatus;
import org.realityforge.replicant.shared.transport.ReplicantContext;

public abstract class WebPollerDataLoaderService<T extends ClientSession<T,G>, G extends Enum>
  extends GwtDataLoaderService<T, G>
{
  private final WebPoller _webPoller = WebPoller.newWebPoller();

  class ReplicantRequestFactory
    extends AbstractHttpRequestFactory
  {
    @Override
    protected RequestBuilder getRequestBuilder()
    {
      final RequestBuilder rb = new RequestBuilder( RequestBuilder.GET, getPollURL() );
      rb.setHeader( ReplicantContext.SESSION_ID_HEADER, getSessionID() );
      return rb;
    }
  }

  public WebPollerDataLoaderService( final ChangeMapper changeMapper,
                                     final EntityChangeBroker changeBroker,
                                     final EntityRepository repository,
                                     final CacheService cacheService,
                                     final EntitySubscriptionManager subscriptionManager,
                                     final ReplicantConfig replicantConfig )
  {
    super( changeMapper, changeBroker, repository, cacheService, subscriptionManager, replicantConfig );
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
    setupCloseHandler();
  }

  protected void setupCloseHandler()
  {
    final Window.ClosingHandler handler = new Window.ClosingHandler()
    {
      @Override
      public void onWindowClosing( final Window.ClosingEvent event )
      {
        disconnect();
      }
    };
    Window.addWindowClosingHandler( handler );
  }

  @Nonnull
  protected abstract T createSession( @Nonnull String sessionID );

  protected final void onSessionCreated( @Nonnull final String sessionID )
  {
    setSession( createSession( sessionID ), new Runnable()
    {
                  @Override
                  public void run()
                  {
                    onSessionConnected();
                  }
                } );
    scheduleDataLoad();
    startPolling();
  }

  protected abstract void onSessionConnected();

  public void disconnect()
  {
    stopPolling();
    setSession( null, null );
  }

  @Nonnull
  protected abstract String getPollURL();

  final void handlePollSuccess( final String rawJsonData )
  {
    if ( null != rawJsonData )
    {
      logResponse( rawJsonData );
      getSession().enqueueDataLoad( rawJsonData );
      if ( !_webPoller.isPaused() )
      {
        _webPoller.pause();
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
      LOG.info( "Received data from poll: " + messageData );
    }
  }

  protected int getThresholdForResponseLogging()
  {
    return 300;
  }

  @Override
  protected void onDataLoadComplete( @Nonnull final DataLoadStatus status )
  {
    if ( _webPoller.isPaused() )
    {
      _webPoller.resume();
    }
  }

  protected abstract void handleSystemFailure( @Nonnull Throwable caught, @Nonnull String message );

  private void startPolling()
  {
    stopPolling();
    _webPoller.setRequestFactory( new ReplicantRequestFactory() );
    _webPoller.setInterRequestDuration( 0 );
    _webPoller.start();
  }

  private void stopPolling()
  {
    if ( isConnected() )
    {
      _webPoller.stop();
    }
  }

  public boolean isConnected()
  {
    return _webPoller.isActive();
  }
}
