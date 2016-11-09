package org.realityforge.replicant.client.gwt;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.InvocationException;
import com.google.web.bindery.event.shared.EventBus;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.gwt.webpoller.client.AbstractHttpRequestFactory;
import org.realityforge.gwt.webpoller.client.RequestFactory;
import org.realityforge.gwt.webpoller.client.TimerBasedWebPoller;
import org.realityforge.gwt.webpoller.client.WebPoller;
import org.realityforge.replicant.client.EntityChangeBroker;
import org.realityforge.replicant.client.EntityRepository;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.client.transport.CacheService;
import org.realityforge.replicant.client.transport.ClientSession;
import org.realityforge.replicant.client.transport.SessionContext;
import org.realityforge.replicant.shared.transport.ReplicantContext;

public abstract class GwtWebPollerDataLoaderService<T extends ClientSession<T, G>, G extends Enum>
  extends GwtDataLoaderService<T, G>
{
  private class ReplicantRequestFactory
    extends AbstractHttpRequestFactory
  {
    @Override
    protected RequestBuilder getRequestBuilder()
    {
      final RequestBuilder rb = new RequestBuilder( RequestBuilder.GET, getPollURL() );
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
                                        @Nonnull final EntityChangeBroker changeBroker,
                                        @Nonnull final EntityRepository repository,
                                        @Nonnull final CacheService cacheService,
                                        @Nonnull final EntitySubscriptionManager subscriptionManager,
                                        @Nonnull final EventBus eventBus,
                                        @Nonnull final ReplicantConfig replicantConfig )
  {
    super( sessionContext,
           changeBroker,
           repository,
           cacheService,
           subscriptionManager,
           eventBus,
           replicantConfig );
    createWebPoller();
    setupCloseHandler();
  }

  @Nonnull
  @Override
  protected WebPoller newWebPoller()
  {
    return new TimerBasedWebPoller();
  }

  protected void setupCloseHandler()
  {
    final Window.ClosingHandler handler = new Window.ClosingHandler()
    {
      @Override
      public void onWindowClosing( final Window.ClosingEvent event )
      {
        disconnect( null );
      }
    };
    Window.addWindowClosingHandler( handler );
  }

  @Override
  protected void doConnect( @Nullable final Runnable runnable )
  {
    final RequestBuilder rb = new RequestBuilder( RequestBuilder.POST, getTokenURL() );
    try
    {
      rb.sendRequest( null, new RequestCallback()
      {
        @Override
        public void onResponseReceived( final Request request, final Response response )
        {
          if ( Response.SC_OK == response.getStatusCode() )
          {
            onSessionCreated( response.getText(), runnable );
          }
          else
          {
            handleInvalidConnect( null );
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
      new RequestBuilder( RequestBuilder.DELETE, getTokenURL() + "/" + session.getSessionID() );
    try
    {
      rb.sendRequest( null, new RequestCallback()
      {
        @Override
        public void onResponseReceived( final Request request, final Response response )
        {
          if ( Response.SC_OK == response.getStatusCode() )
          {
            setSession( null, runnable );
          }
          else
          {
            setSession( null, runnable );
            handleInvalidDisconnect( null );
          }
        }

        @Override
        public void onError( final Request request, final Throwable exception )
        {
          setSession( null, runnable );
          handleInvalidDisconnect( exception );
        }
      } );
    }
    catch ( final RequestException e )
    {
      setSession( null, runnable );
      handleInvalidDisconnect( e );
    }
  }

  protected void handleSystemFailure( @Nullable final Throwable caught, @Nonnull final String message )
  {
    super.handleSystemFailure( caught, message );
    final Throwable cause = ( caught instanceof InvocationException ) ? caught.getCause() : caught;
    getEventBus().fireEvent( new SystemErrorEvent( message, cause ) );
  }

  @Nonnull
  @Override
  protected RequestFactory newRequestFactory()
  {
    return new ReplicantRequestFactory();
  }
}
