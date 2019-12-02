package org.realityforge.replicant.client.gwt;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONObject;
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
import org.realityforge.replicant.shared.SharedConstants;

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
      return newSessionBasedInvocationBuilder( RequestBuilder.GET, getPollURL() );
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
    sendRequest( RequestBuilder.POST, getBaseSessionURL(), onResponse, this::handleInvalidConnect );
  }

  @Override
  protected void doDisconnect( @Nullable final Runnable runnable )
  {
    final Consumer<Response> onResponse = r -> onDisconnectResponse( r.getStatusCode(), r.getStatusText(), runnable );
    final Consumer<Throwable> onError = t -> onDisconnectError( t, runnable );
    sendRequest( RequestBuilder.DELETE, getSessionURL(), onResponse, onError );
  }

  @Override
  protected void doSubscribe( @Nullable final ClientSession session,
                              @Nullable final RequestEntry request,
                              @Nullable final Object filterParameter,
                              @Nonnull final String channelURL,
                              @Nullable final String eTag,
                              @Nonnull final Runnable onSuccess,
                              @Nullable final Runnable onCacheValid,
                              @Nonnull final Consumer<Throwable> onError )
  {
    httpRequest( session,
                 request,
                 RequestBuilder.PUT,
                 channelURL,
                 eTag,
                 filterToString( filterParameter ),
                 onSuccess,
                 onCacheValid,
                 onError );
  }

  @Override
  protected void doUnsubscribe( @Nullable final ClientSession session,
                                @Nullable final RequestEntry request,
                                @Nonnull final String channelURL,
                                @Nonnull final Runnable onSuccess,
                                @Nonnull final Consumer<Throwable> onError )
  {
    httpRequest( session, request, RequestBuilder.DELETE, channelURL, null, null, onSuccess, null, onError );
  }

  private void httpRequest( @Nullable final ClientSession session,
                            @Nullable final RequestEntry request,
                            @Nonnull final RequestBuilder.Method method,
                            @Nonnull final String url,
                            @Nullable final String eTag,
                            @Nullable final String requestData,
                            @Nonnull final Runnable onSuccess,
                            @Nullable final Runnable onCacheValid,
                            @Nonnull final Consumer<Throwable> onError )
  {
    final ActionCallbackAdapter adapter =
      new ActionCallbackAdapter( onSuccess, onCacheValid, onError, request, session );
    final String requestID = null != request ? request.getRequestID() : null;
    final RequestBuilder rb = newRequestBuilder( method, url );
    if ( null != requestID )
    {
      rb.setHeader( SharedConstants.REQUEST_ID_HEADER, requestID );
    }
    if ( null != eTag )
    {
      rb.setHeader( SharedConstants.ETAG_HEADER, eTag );
    }
    try
    {
      rb.sendRequest( requestData, adapter );
    }
    catch ( final RequestException e )
    {
      adapter.onError( null, e );
    }
  }

  private void sendRequest( @Nonnull final RequestBuilder.Method method,
                            @Nonnull final String url,
                            @Nonnull final Consumer<Response> onResponse,
                            @Nonnull final Consumer<Throwable> onError )
  {
    final RequestBuilder rb = newRequestBuilder( method, url );
    try
    {
      rb.sendRequest( null, new RequestCallback()
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
  private RequestBuilder newSessionBasedInvocationBuilder( @Nonnull final RequestBuilder.Method method,
                                                           @Nonnull final String url )
  {
    final RequestBuilder rb = newRequestBuilder( method, url );
    rb.setHeader( SharedConstants.SESSION_ID_HEADER, getSessionID() );
    return rb;
  }

  @Nonnull
  protected RequestBuilder newRequestBuilder( @Nonnull final RequestBuilder.Method method,
                                              @Nonnull final String url )
  {
    final RequestBuilder rb = new RequestBuilder( method, url );
    //Timeout 2 seconds after maximum poll
    rb.setTimeoutMillis( ( SharedConstants.MAX_POLL_TIME_IN_SECONDS + 2 ) * 1000 );
    rb.setHeader( "Pragma", "no-cache" );
    final String authenticationToken = getSessionContext().getAuthenticationToken();
    if ( null != authenticationToken )
    {
      rb.setHeader( "Authorization", "Bearer " + authenticationToken );
    }
    return rb;
  }

  @Nonnull
  @Override
  protected String doFilterToString( @Nonnull final Object filterParameter )
  {
    return new JSONObject( (JavaScriptObject) filterParameter ).toString();
  }
}
