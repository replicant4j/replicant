package org.realityforge.replicant.client.gwt;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window;
import com.google.web.bindery.event.shared.EventBus;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.gwt.webpoller.client.AbstractHttpRequestFactory;
import org.realityforge.gwt.webpoller.client.RequestFactory;
import org.realityforge.gwt.webpoller.client.TimerBasedWebPoller;
import org.realityforge.gwt.webpoller.client.WebPoller;
import org.realityforge.replicant.client.EntitySystem;
import org.realityforge.replicant.client.transport.CacheService;
import org.realityforge.replicant.client.transport.ClientSession;
import org.realityforge.replicant.client.transport.InvalidHttpResponseException;
import org.realityforge.replicant.client.transport.SessionContext;
import org.realityforge.replicant.shared.transport.ReplicantContext;

public abstract class GwtWebPollerDataLoaderService<T extends ClientSession<T, G>, G extends Enum<G>>
  extends GwtDataLoaderService<T, G>
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
                                        @Nonnull final CacheService cacheService,
                                        @Nonnull final EventBus eventBus,
                                        @Nonnull final ReplicantConfig replicantConfig )
  {
    super( sessionContext, eventBus, replicantConfig );
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
    final Window.ClosingHandler handler = event -> disconnect( null );
    Window.addWindowClosingHandler( handler );
  }

  @Override
  protected void doConnect( @Nullable final Runnable runnable )
  {
    final RequestBuilder rb = newRequestBuilder( RequestBuilder.POST, getTokenURL() );
    try
    {
      rb.sendRequest( null, new RequestCallback()
      {
        @Override
        public void onResponseReceived( final Request request, final Response response )
        {
          final int statusCode = response.getStatusCode();
          if ( Response.SC_OK == statusCode )
          {
            onSessionCreated( response.getText(), runnable );
          }
          else
          {
            handleInvalidConnect( new InvalidHttpResponseException( statusCode, response.getStatusText() ) );
          }
        }

        @Override
        public void onError( final Request request, final Throwable exception )
        {
          handleInvalidConnect( exception );
        }
      } );
    }
    catch ( final RequestException e )
    {
      handleInvalidConnect( e );
    }
  }

  @Override
  protected void doDisconnect( @Nonnull final T session, @Nullable final Runnable runnable )
  {
    final RequestBuilder rb =
      newRequestBuilder( RequestBuilder.DELETE, getTokenURL() + "/" + session.getSessionID() );
    try
    {
      rb.sendRequest( null, new RequestCallback()
      {
        @Override
        public void onResponseReceived( final Request request, final Response response )
        {
          final int statusCode = response.getStatusCode();
          if ( Response.SC_OK == statusCode )
          {
            setSession( null, runnable );
          }
          else
          {
            setSession( null, runnable );
            getListener().
              onInvalidDisconnect( GwtWebPollerDataLoaderService.this,
                                   new InvalidHttpResponseException( statusCode, response.getStatusText() ) );
          }
        }

        @Override
        public void onError( final Request request, final Throwable exception )
        {
          setSession( null, runnable );
          getListener().onInvalidDisconnect( GwtWebPollerDataLoaderService.this, exception );
        }
      } );
    }
    catch ( final RequestException e )
    {
      setSession( null, runnable );
      getListener().onInvalidDisconnect( this, e );
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
    rb.setHeader( "Pragma", "no-cache" );
    final String authenticationToken = getSessionContext().getAuthenticationToken();
    if ( null != authenticationToken )
    {
      rb.setHeader( "Authorization", "Bearer " + authenticationToken );
    }
    return rb;
  }
}
