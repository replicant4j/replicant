package org.realityforge.replicant.client.ee;

import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.concurrent.ContextService;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.realityforge.gwt.webpoller.client.RequestFactory;
import org.realityforge.gwt.webpoller.client.WebPoller;
import org.realityforge.gwt.webpoller.client.WebPollerListener;
import org.realityforge.gwt.webpoller.server.AbstractJaxrsHttpRequestFactory;
import org.realityforge.gwt.webpoller.server.TimerBasedWebPoller;
import org.realityforge.replicant.shared.transport.ReplicantContext;

public abstract class EeWebPollerDataLoaderService
  extends EeDataLoaderService
{
  private static final int DEFAULT_TIMEOUT = 25000;

  private final class ReplicantRequestFactory
    extends AbstractJaxrsHttpRequestFactory
  {
    @Nonnull
    @Override
    protected Invocation.Builder getInvocation()
    {
      return newInvocationBuilder( getPollURL() ).header( ReplicantContext.SESSION_ID_HEADER, getSessionID() );
    }
  }

  @Nonnull
  protected abstract ContextService getContextService();

  @Nonnull
  @Override
  protected WebPoller newWebPoller()
  {
    return new TimerBasedWebPoller( getManagedScheduledExecutorService() );
  }

  @Nonnull
  @Override
  protected RequestFactory newRequestFactory()
  {
    return new ReplicantRequestFactory();
  }

  @Nonnull
  protected Client newClient()
  {
    return ClientBuilder.newClient().
      property( "jersey.config.client.readTimeout", DEFAULT_TIMEOUT ).
      property( "jersey.config.client.connectTimeout", DEFAULT_TIMEOUT );
  }

  @Nonnull
  protected Invocation.Builder newInvocationBuilder( @Nonnull final String target )
  {
    final Invocation.Builder builder = newClient().
      target( target ).
      request( MediaType.APPLICATION_JSON ).
      accept( MediaType.TEXT_PLAIN ).
      header( "Pragma", "no-cache" );
    final String authenticationToken = getSessionContext().getAuthenticationToken();
    if ( null == authenticationToken )
    {
      return builder;
    }
    else
    {
      return builder.header( "Authorization", "Bearer " + authenticationToken );
    }
  }

  @Override
  protected void doConnect( @Nullable final Runnable runnable )
  {
    final Consumer<Response> onCompletion =
      wrap( r -> onConnectResponse( r.getStatus(),
                                    r.getStatusInfo().getReasonPhrase(),
                                    () -> r.readEntity( String.class ),
                                    runnable ) );
    final Consumer<Throwable> onError = wrap( t -> getListener().onInvalidConnect( this, t ) );
    final Invocation.Builder builder = newInvocationBuilder( getBaseSessionURL() );
    builder.async().post( Entity.entity( "", MediaType.TEXT_PLAIN_TYPE ), new InvocationCallback<Response>()
    {
      @Override
      public void completed( final Response response )
      {
        onCompletion.accept( response );
      }

      @Override
      public void failed( final Throwable throwable )
      {
        onError.accept( throwable );
      }
    } );
  }

  protected void doDisconnect( @Nullable final Runnable runnable )
  {
    final Consumer<Response> onCompletion =
      wrap( r -> onDisconnectResponse( r.getStatus(), r.getStatusInfo().getReasonPhrase(), runnable ) );
    final Consumer<Throwable> onError = wrap( t -> onDisconnectError( t, runnable ) );

    final Invocation.Builder builder = newInvocationBuilder( getSessionURL() );
    builder.async().delete( new InvocationCallback<Response>()
    {
      @Override
      public void completed( final Response response )
      {
        onCompletion.accept( response );
      }

      @Override
      public void failed( final Throwable throwable )
      {
        onError.accept( throwable );
      }
    } );
  }

  @Nonnull
  @Override
  protected WebPollerListener newWebPollerListener()
  {
    return getContextService().createContextualProxy( super.newWebPollerListener(), WebPollerListener.class );
  }

  @SuppressWarnings( "unchecked" )
  private <R> Consumer<R> wrap( @Nonnull final Consumer<R> action )
  {
    return getContextService().createContextualProxy( action, Consumer.class );
  }
}
