package org.realityforge.replicant.client.gwt;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.gwt.webpoller.client.AbstractHttpRequestFactory;
import org.realityforge.gwt.webpoller.client.RequestFactory;
import org.realityforge.gwt.webpoller.client.TimerBasedWebPoller;
import org.realityforge.gwt.webpoller.client.WebPoller;
import org.realityforge.replicant.client.EntitySystem;
import org.realityforge.replicant.client.transport.CacheService;
import org.realityforge.replicant.client.transport.ClientSession;
import org.realityforge.replicant.client.transport.RequestEntry;
import org.realityforge.replicant.client.transport.SessionContext;
import org.realityforge.replicant.shared.transport.ReplicantContext;

public abstract class GwtWebPollerDataLoaderService
  extends GwtDataLoaderService
{
  private final CacheService _cacheService;
  private final EntitySystem _entitySystem;

  protected class ReplicantRequestFactory
    extends AbstractHttpRequestFactory
  {
    @Override
    protected RequestBuilder getRequestBuilder()
    {
      final RequestBuilder rb = newRequestBuilder( RequestBuilder.GET, getPollURL() );
      rb.setHeader( ReplicantContext.SESSION_ID_HEADER, getSessionID() );
      rb.setHeader( "Pragma", "no-cache" );
      final String authenticationToken = getSessionContext().getAuthenticationToken();
      if ( null != authenticationToken )
      {
        rb.setHeader( "Authorization", "Bearer " + authenticationToken );
      }
      return rb;
    }
  }

  public GwtWebPollerDataLoaderService( @Nonnull final SessionContext sessionContext,
                                        @Nonnull final EntitySystem entitySystem,
                                        @Nonnull final CacheService cacheService )
  {
    super( sessionContext );
    _entitySystem = entitySystem;
    _cacheService = cacheService;
    createWebPoller();
    setupCloseHandler();
  }

  @Override
  protected EntitySystem getEntitySystem()
  {
    return _entitySystem;
  }

  @Nonnull
  @Override
  protected CacheService getCacheService()
  {
    return _cacheService;
  }

  @Nonnull
  @Override
  protected WebPoller newWebPoller()
  {
    return new TimerBasedWebPoller();
  }

  protected void setupCloseHandler()
  {
    final Window.ClosingHandler handler = event -> disconnect();
    Window.addWindowClosingHandler( handler );
  }

  @Override
  protected void doConnect( @Nullable final Runnable runnable )
  {
    final Consumer<Response> onResponse =
      r -> onConnectResponse( r.getStatusCode(), r.getStatusText(), r::getText, runnable );
    sendRequest( RequestBuilder.POST, getBaseSessionURL(), null, onResponse, this::handleInvalidConnect );
  }

  @Override
  protected void doDisconnect( @Nullable final Runnable runnable )
  {
    final Consumer<Response> onResponse = r -> onDisconnectResponse( r.getStatusCode(), r.getStatusText(), runnable );
    final Consumer<Throwable> onError = t -> onDisconnectError( t, runnable );
    sendRequest( RequestBuilder.DELETE, getSessionURL(), null, onResponse, onError );
  }

  @Override
  protected void doSubscribe( @Nullable final ClientSession session,
                              @Nullable final RequestEntry request,
                              @Nonnull final String channelURL,
                              @Nonnull final Runnable onSuccess,
                              @Nonnull final Consumer<Throwable> onError )
  {
    final Consumer<Response> onResponse = r -> onSuccess.run();
    httpRequest( session,
                 request,
                 RequestBuilder.PUT,
                 channelURL,
                 "",
                 onResponse,
                 onError );
  }

  @Override
  protected void doUnsubscribe( @Nullable final ClientSession session,
                                @Nullable final RequestEntry request,
                                @Nonnull final String channelURL,
                                @Nonnull final Runnable onSuccess,
                                @Nonnull final Consumer<Throwable> onError )
  {
    final Consumer<Response> onResponse = r -> onSuccess.run();
    httpRequest( session,
                 request,
                 RequestBuilder.DELETE,
                 channelURL,
                 null,
                 onResponse,
                 onError );
  }

  private void httpRequest( @Nullable final ClientSession session,
                            @Nullable final RequestEntry request,
                            @Nonnull final RequestBuilder.Method method,
                            @Nonnull final String url,
                            @Nullable final String requestData,
                            @Nonnull final Consumer<Response> onResponse,
                            @Nonnull final Consumer<Throwable> onError )
  {
    final ActionCallbackAdapter adapter = new ActionCallbackAdapter( onResponse, onError, request, session );
    sendRequest( method, url, requestData, adapter::onSuccess, adapter::onFailure );
  }

  protected void sendRequest( @Nonnull final RequestBuilder.Method method,
                              @Nonnull final String url,
                              @Nullable final String requestData,
                              @Nonnull final Consumer<Response> onResponse,
                              @Nonnull final Consumer<Throwable> onError )
  {
    final RequestBuilder rb = newRequestBuilder( method, url );
    try
    {
      rb.sendRequest( requestData, new RequestCallback()
      {
        @Override
        public void onResponseReceived( final Request request, final Response response )
        {
          onResponse.accept( response );
        }

        @Override
        public void onError( final Request request, final Throwable exception )
        {
          onError.accept( exception );
        }
      } );
    }
    catch ( final RequestException e )
    {
      onError.accept( e );
    }
  }

  @Nonnull
  @Override
  protected RequestFactory newRequestFactory()
  {
    return new ReplicantRequestFactory();
  }

  @Nonnull
  protected RequestBuilder newRequestBuilder( @Nonnull final RequestBuilder.Method method,
                                              @Nonnull final String url )
  {
    final RequestBuilder rb = new RequestBuilder( method, url );
    //Timeout 2 seconds after maximum poll
    rb.setTimeoutMillis( ( ReplicantContext.MAX_POLL_TIME_IN_SECONDS + 2 ) * 1000 );
    rb.setHeader( "Pragma", "no-cache" );
    final String authenticationToken = getSessionContext().getAuthenticationToken();
    if ( null != authenticationToken )
    {
      rb.setHeader( "Authorization", "Bearer " + authenticationToken );
    }
    return rb;
  }
}
