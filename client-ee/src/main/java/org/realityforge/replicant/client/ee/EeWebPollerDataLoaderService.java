package org.realityforge.replicant.client.ee;

import java.io.Serializable;
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
import org.realityforge.replicant.client.ChannelDescriptor;
import org.realityforge.replicant.client.transport.ClientSession;
import org.realityforge.replicant.client.transport.RequestEntry;
import org.realityforge.replicant.shared.ee.JsonUtil;
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
      return newSessionBasedInvocationBuilder( getPollURL(), null );
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
  protected Invocation.Builder newSessionBasedInvocationBuilder( @Nonnull final String target,
                                                                 @Nullable final RequestEntry request )
  {
    final Invocation.Builder builder = newInvocationBuilder( target ).
      header( ReplicantContext.SESSION_ID_HEADER, getSessionID() );
    if ( null != request )
    {
      builder.header( ReplicantContext.REQUEST_ID_HEADER, request.getRequestID() );
    }
    return builder;
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

  private Runnable wrap( @Nonnull final Runnable action )
  {
    return getContextService().createContextualProxy( action, Runnable.class );
  }

  @Override
  protected void doSubscribe( @Nullable final ClientSession session,
                              @Nullable final RequestEntry request,
                              @Nullable final Object filterParameter,
                              @Nonnull final String channelURL,
                              @Nullable final String cacheKey,
                              @Nonnull final Runnable onSuccess,
                              @Nullable final Runnable onCacheValid,
                              @Nonnull final Consumer<Throwable> onError )
  {
    final ActionCallbackAdapter adapter =
      new ActionCallbackAdapter( wrap( onSuccess ),
                                 null != onCacheValid ? wrap( onCacheValid ) : null,
                                 wrap( onError ),
                                 request,
                                 session );
    final Invocation.Builder builder = newSessionBasedInvocationBuilder( channelURL, request );
    builder.async().put( Entity.entity( filterToString( filterParameter ), MediaType.TEXT_PLAIN_TYPE ), adapter );
  }

  @Override
  protected void doUnsubscribe( @Nullable final ClientSession session,
                                @Nullable final RequestEntry request,
                                @Nonnull final String channelURL,
                                @Nonnull final Runnable onSuccess,
                                @Nonnull final Consumer<Throwable> onError )
  {
    final ActionCallbackAdapter adapter =
      new ActionCallbackAdapter( wrap( onSuccess ), null, wrap( onError ), request, session );
    final Invocation.Builder builder = newSessionBasedInvocationBuilder( channelURL, request );
    builder.async().delete( adapter );
  }

  @Override
  protected void requestSubscribeToGraph( @Nonnull final ChannelDescriptor descriptor,
                                          @Nullable final Object filterParameter,
                                          @Nullable final String eTag,
                                          @Nullable final Consumer<Runnable> cacheAction,
                                          @Nonnull final Consumer<Runnable> completionAction,
                                          @Nonnull final Consumer<Runnable> failAction )
  {
    //If eTag passed then cache action is expected.
    assert null == eTag || null != cacheAction;
    if ( getGraphType().isInstance( descriptor.getGraph() ) )
    {
      getListener().onSubscribeStarted( this, descriptor );
      final Runnable onSuccess =
        () -> completionAction.accept( () -> getListener().onSubscribeCompleted( this, descriptor ) );
      final Runnable onCacheValid =
        null != cacheAction ?
        () -> cacheAction.accept( () -> getListener().onSubscribeCompleted( this, descriptor ) ) :
        null;
      final Consumer<Throwable> onError =
        throwable -> failAction.accept( () -> getListener().onSubscribeFailed( this, descriptor, throwable ) );
      performSubscribe( descriptor.getGraph().ordinal(),
                        (Serializable) descriptor.getID(),
                        filterParameter,
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

  @Nonnull
  @Override
  protected String doFilterToString( @Nonnull final Object filterParameter )
  {
    return JsonUtil.toJsonString( filterParameter );
  }

  @Override
  protected void requestUpdateSubscription( @Nonnull final ChannelDescriptor descriptor,
                                            @Nonnull final Object filterParameter,
                                            @Nonnull final Consumer<Runnable> completionAction,
                                            @Nonnull final Consumer<Runnable> failAction )
  {
    if ( getGraphType().isInstance( descriptor.getGraph() ) )
    {
      getListener().onSubscriptionUpdateStarted( this, descriptor );
      final Runnable onSuccess =
        () -> completionAction.accept( () -> getListener().onSubscriptionUpdateCompleted( this, descriptor ) );
      final Consumer<Throwable> onError =
        throwable -> failAction.accept( () -> getListener().onSubscriptionUpdateFailed( this, descriptor, throwable ) );
      performSubscribe( descriptor.getGraph().ordinal(),
                        (Serializable) descriptor.getID(),
                        filterParameter,
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
    if ( getGraphType().isInstance( descriptor.getGraph() ) )
    {
      getListener().onUnsubscribeStarted( this, descriptor );
      final Consumer<Throwable> onError =
        throwable -> failAction.accept( () -> getListener().onUnsubscribeFailed( this, descriptor, throwable ) );
      final Runnable onSuccess =
        () -> completionAction.accept( () -> getListener().onUnsubscribeCompleted( this, descriptor ) );
      performUnsubscribe( descriptor.getGraph().ordinal(), (Serializable) descriptor.getID(), onSuccess, onError );
    }
    else
    {
      throw new IllegalStateException();
    }
  }
}
