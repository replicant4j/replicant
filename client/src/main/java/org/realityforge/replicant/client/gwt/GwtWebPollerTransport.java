package org.realityforge.replicant.client.gwt;

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
import org.realityforge.replicant.client.transport.WebPollerTransport;
import org.realityforge.replicant.shared.SharedConstants;
import replicant.ReplicantContext;
import replicant.SafeProcedure;

public abstract class GwtWebPollerTransport
  extends WebPollerTransport
{
  public GwtWebPollerTransport( @Nonnull final ReplicantContext replicantContext )
  {
    super( replicantContext );
    setupCloseHandler();
  }

  @Nonnull
  @Override
  protected WebPoller newWebPoller()
  {
    return new TimerBasedWebPoller();
  }

  private void setupCloseHandler()
  {
    //TODO: This should be bound to ReplicantContext.deactivate and should route throgh Elemental2
    final Window.ClosingHandler handler = event -> ensureTransportContext().disconnect();
    Window.addWindowClosingHandler( handler );
  }

  @Override
  public void connect( @Nonnull final OnConnect onConnect, @Nonnull final OnError onConnectError )
  {
    final Consumer<Response> onResponse =
      r -> onConnectResponse( r.getStatusCode(),
                              r.getStatusText(),
                              r::getText,
                              wrapOnConnect( onConnect ),
                              onConnectError );
    sendRequest( RequestBuilder.POST, getBaseConnectionURL(), onResponse, onConnectError::onError );
  }

  @Override
  public void disconnect( @Nonnull final SafeProcedure onDisconnect, @Nonnull final OnError onDisconnectError )
  {
    final Consumer<Response> onResponse =
      r -> onDisconnectResponse( r.getStatusCode(),
                                 r.getStatusText(),
                                 wrapOnDisconnect( onDisconnect ),
                                 wrapOnDisconnectError( onDisconnectError ) );
    sendRequest( RequestBuilder.DELETE, getConnectionURL(), onResponse, onDisconnectError::onError );
  }

  @Override
  protected void doSubscribe( @Nonnull final replicant.Request request,
                              @Nullable final Object filter,
                              @Nonnull final String channelURL,
                              @Nullable final String eTag,
                              @Nonnull final SafeProcedure onSuccess,
                              @Nullable final SafeProcedure onCacheValid,
                              @Nonnull final Consumer<Throwable> onError )
  {
    final RequestBuilder rb = newRequestBuilder( RequestBuilder.PUT, channelURL );
    rb.setHeader( SharedConstants.REQUEST_ID_HEADER, String.valueOf( request.getRequestId() ) );
    if ( null != eTag )
    {
      rb.setHeader( SharedConstants.ETAG_HEADER, eTag );
    }
    try
    {
      rb.sendRequest( null == filter ? "" : Global.JSON.stringify( filter ), new RequestCallback()
      {
        @Override
        public void onResponseReceived( final Request request11, final Response response )
        {
          final int statusCode = response.getStatusCode();
          if ( Response.SC_OK == statusCode )
          {
            onSuccess.call();
          }
          else if ( Response.SC_NO_CONTENT == statusCode )
          {
            assert null != onCacheValid;
            onCacheValid.call();
          }
          else
          {
            onError.accept( new InvalidHttpResponseException( statusCode, response.getStatusText() ) );
          }
        }

        @Override
        public void onError( final Request request11, final Throwable exception )
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

  @Override
  protected void doUnsubscribe( @Nonnull final replicant.Request request,
                                @Nonnull final String channelURL,
                                @Nonnull final SafeProcedure onSuccess,
                                @Nonnull final Consumer<Throwable> onError )
  {
    final RequestBuilder rb = newRequestBuilder( RequestBuilder.DELETE, channelURL );
    rb.setHeader( SharedConstants.REQUEST_ID_HEADER, String.valueOf( (Integer) request.getRequestId() ) );
    try
    {
      rb.sendRequest( null, new RequestCallback()
      {
        @Override
        public void onResponseReceived( final Request request11, final Response response )
        {
          final int statusCode = response.getStatusCode();
          if ( Response.SC_OK == statusCode )
          {
            onSuccess.call();
          }
          else
          {
            onError.accept( new InvalidHttpResponseException( statusCode, response.getStatusText() ) );
          }
        }

        @Override
        public void onError( final Request request11, final Throwable exception )
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
    return new AbstractHttpRequestFactory()
    {
      @Override
      protected RequestBuilder getRequestBuilder()
      {
        final RequestBuilder rb = newRequestBuilder( RequestBuilder.GET, getPollURL() );
        rb.setHeader( SharedConstants.CONNECTION_ID_HEADER, getConnectionId() );
        return rb;
      }
    };
  }

  @Nonnull
  private RequestBuilder newRequestBuilder( @Nonnull final RequestBuilder.Method method,
                                            @Nonnull final String url )
  {
    final RequestBuilder rb = new RequestBuilder( method, url );
    //Timeout 2 seconds after maximum poll
    rb.setTimeoutMillis( ( SharedConstants.MAX_POLL_TIME_IN_SECONDS + 2 ) * 1000 );
    rb.setHeader( "Pragma", "no-cache" );
    final String authenticationToken = getAuthenticationToken();
    if ( null != authenticationToken )
    {
      rb.setHeader( "Authorization", "Bearer " + authenticationToken );
    }
    return rb;
  }
}
