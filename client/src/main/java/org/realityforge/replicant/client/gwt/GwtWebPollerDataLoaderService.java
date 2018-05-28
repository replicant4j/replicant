package org.realityforge.replicant.client.gwt;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window;
import elemental2.core.Global;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.gwt.webpoller.client.AbstractHttpRequestFactory;
import org.realityforge.gwt.webpoller.client.RequestFactory;
import org.realityforge.gwt.webpoller.client.TimerBasedWebPoller;
import org.realityforge.gwt.webpoller.client.WebPoller;
import org.realityforge.replicant.client.transport.SessionContext;
import org.realityforge.replicant.client.transport.WebPollerDataLoaderService;
import org.realityforge.replicant.shared.SharedConstants;
import replicant.Connection;
import replicant.RequestEntry;
import replicant.SafeProcedure;
import replicant.SystemSchema;

public abstract class GwtWebPollerDataLoaderService
  extends WebPollerDataLoaderService
{
  protected class ReplicantRequestFactory
    extends AbstractHttpRequestFactory
  {
    @Override
    protected RequestBuilder getRequestBuilder()
    {
      return newSessionBasedInvocationBuilder( RequestBuilder.GET, getPollURL() );
    }
  }

  public GwtWebPollerDataLoaderService( @Nullable final replicant.ReplicantContext context,
                                        @Nonnull final SystemSchema schema,
                                        @Nonnull final SessionContext sessionContext )
  {
    super( context, schema, sessionContext );
    createWebPoller();
    setupCloseHandler();
  }

  protected void activateScheduler()
  {
    Scheduler.get().scheduleIncremental( this::scheduleTick );
  }

  @Nonnull
  @Override
  protected WebPoller newWebPoller()
  {
    return new TimerBasedWebPoller();
  }

  protected void setupCloseHandler()
  {
    //TODO: This should be bound to ReplicantContext.deactivate and should route throgh Elemental2
    final Window.ClosingHandler handler = event -> disconnect();
    Window.addWindowClosingHandler( handler );
  }

  @Override
  protected void doConnect( @Nonnull final SafeProcedure action )
  {
    final Consumer<Response> onResponse =
      r -> onConnectResponse( r.getStatusCode(), r.getStatusText(), r::getText, action );
    sendRequest( RequestBuilder.POST, getBaseConnectionURL(), onResponse, this::onConnectFailure );
  }

  @Override
  protected void doDisconnect( @Nonnull final SafeProcedure action )
  {
    final Consumer<Response> onResponse = r -> onDisconnectResponse( r.getStatusCode(), r.getStatusText(), action );
    final Consumer<Throwable> onError = t -> onDisconnectError( t, action );
    sendRequest( RequestBuilder.DELETE, getConnectionURL(), onResponse, onError );
  }

  @Override
  protected void doSubscribe( @Nullable final Connection connection,
                              @Nullable final RequestEntry request,
                              @Nullable final Object filter,
                              @Nonnull final String channelURL,
                              @Nullable final String eTag,
                              @Nonnull final SafeProcedure onSuccess,
                              @Nullable final SafeProcedure onCacheValid,
                              @Nonnull final Consumer<Throwable> onError )
  {
    httpRequest( connection,
                 request,
                 RequestBuilder.PUT,
                 channelURL,
                 eTag,
                 null == filter ? "" : Global.JSON.stringify( filter ),
                 onSuccess,
                 onCacheValid,
                 onError );
  }

  @Override
  protected void doUnsubscribe( @Nullable final Connection connection,
                                @Nullable final RequestEntry request,
                                @Nonnull final String channelURL,
                                @Nonnull final SafeProcedure onSuccess,
                                @Nonnull final Consumer<Throwable> onError )
  {
    httpRequest( connection, request, RequestBuilder.DELETE, channelURL, null, null, onSuccess, null, onError );
  }

  private void httpRequest( @Nullable final Connection connection,
                            @Nullable final RequestEntry request,
                            @Nonnull final RequestBuilder.Method method,
                            @Nonnull final String url,
                            @Nullable final String eTag,
                            @Nullable final String requestData,
                            @Nonnull final SafeProcedure onSuccess,
                            @Nullable final SafeProcedure onCacheValid,
                            @Nonnull final Consumer<Throwable> onError )
  {
    final ActionCallbackAdapter adapter =
      new ActionCallbackAdapter( onSuccess, onCacheValid, onError, request, connection );
    final Integer requestId = null != request ? request.getRequestId() : null;
    final RequestBuilder rb = newRequestBuilder( method, url );
    if ( null != requestId )
    {
      rb.setHeader( SharedConstants.REQUEST_ID_HEADER, String.valueOf( requestId ) );
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
    rb.setHeader( SharedConstants.CONNECTION_ID_HEADER, ensureConnection().getConnectionId() );
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
}
