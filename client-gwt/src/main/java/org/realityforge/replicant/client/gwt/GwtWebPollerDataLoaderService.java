package org.realityforge.replicant.client.gwt;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.Window;
import java.io.Serializable;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.gwt.webpoller.client.AbstractHttpRequestFactory;
import org.realityforge.gwt.webpoller.client.RequestFactory;
import org.realityforge.gwt.webpoller.client.TimerBasedWebPoller;
import org.realityforge.gwt.webpoller.client.WebPoller;
import org.realityforge.replicant.client.ChannelDescriptor;
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
      rb.setHeader( ReplicantContext.REQUEST_ID_HEADER, requestID );
    }
    if ( null != eTag )
    {
      rb.setHeader( ReplicantContext.CACHE_KEY_HEADER, eTag );
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

  @Nonnull
  @Override
  protected String doFilterToString( @Nonnull final Object filterParameter )
  {
    return new JSONObject( (JavaScriptObject) filterParameter ).toString();
  }

  @Override
  protected void requestSubscribeToGraph( @Nonnull final ChannelDescriptor descriptor,
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
      final Runnable onSuccess =
        () -> completionAction.accept( () -> getListener().onSubscribeCompleted( this, descriptor ) );
      final Runnable onCacheValid =
        null == cacheAction ?
        null :
        () -> cacheAction.accept( () -> getListener().onSubscribeCompleted( this, descriptor ) );
      final Consumer<Throwable> onError =
        throwable -> failAction.accept( () -> getListener().onSubscribeFailed( this, descriptor, throwable ) );

      getListener().onSubscribeStarted( this, descriptor );
      performSubscribe( descriptor.getGraph().ordinal(),
                        (Serializable) descriptor.getID(),
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

  @Override
  protected void requestUpdateSubscription( @Nonnull final ChannelDescriptor descriptor,
                                            @Nonnull final Object filterParameter,
                                            @Nonnull final Consumer<Runnable> completionAction,
                                            @Nonnull final Consumer<Runnable> failAction )
  {
    if ( isGraphValid( descriptor ) )
    {
      final Runnable onSuccess =
        () -> completionAction.accept( () -> getListener().onSubscriptionUpdateCompleted( this, descriptor ) );
      final Consumer<Throwable> onError =
        throwable -> failAction.accept( () -> getListener().onSubscriptionUpdateFailed( this, descriptor, throwable ) );

      getListener().onSubscriptionUpdateStarted( this, descriptor );
      performSubscribe( descriptor.getGraph().ordinal(),
                        (Serializable) descriptor.getID(),
                        filterParameter,
                        null,
                        null,
                        onSuccess,
                        null,
                        onError );
    }
    else
    {
      throw new IllegalStateException();
    }
  }

  @Override
  protected void requestUnsubscribeFromGraph( @Nonnull final ChannelDescriptor descriptor,
                                              @Nonnull final Consumer<Runnable> completionAction,
                                              @Nonnull final Consumer<Runnable> failAction )
  {
    if ( isGraphValid( descriptor ) )
    {
      final Runnable callback =
        () -> completionAction.accept( () -> getListener().onUnsubscribeCompleted( this, descriptor ) );
      final Consumer<Throwable> errorCallback =
        throwable -> failAction.accept( () -> getListener().onUnsubscribeFailed( this, descriptor, throwable ) );
      getListener().onUnsubscribeStarted( this, descriptor );
      performUnsubscribe( descriptor.getGraph().ordinal(), (Serializable) descriptor.getID(), callback, errorCallback );
    }
    else
    {
      throw new IllegalStateException();
    }
  }

  private boolean isGraphValid( @Nonnull final ChannelDescriptor descriptor )
  {
    return getGraphType() == descriptor.getGraph().getClass();
  }
}
