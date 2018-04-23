package org.realityforge.replicant.client.ee.rest;

import java.io.StringWriter;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.realityforge.replicant.client.ChannelAddress;
import org.realityforge.replicant.client.ChannelSubscriptionEntry;
import org.realityforge.replicant.client.EntitySubscriptionEntry;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.client.transport.ClientSession;
import org.realityforge.replicant.client.transport.DataLoaderService;
import org.realityforge.replicant.shared.ee.JsonUtil;
import org.realityforge.replicant.shared.ee.rest.AbstractReplicantRestService;

/**
 A base class from which to derive a replicant client rest service. A basic client will look like;

<code>
\@Path( MyDataLoaderServiceRestService.BASE_URL )
\@Produces( MediaType.WILDCARD )
\@ApplicationScoped
\@Transactional( Transactional.TxType.NOT_SUPPORTED )
public class MyDataLoaderServiceRestService
  extends AbstractDataLoaderServiceRestService&lt;MyDataLoaderServiceRestService&gt;
{
  public static final String BASE_URL = CLIENT_URL_PREFIX + "my";

  \@Inject
  private MyEeDataLoaderService _dataLoaderService;
  \@Inject
  private EntitySystem _entitySystem;

  \@Override
  protected Class&lt;MyDataLoaderServiceRestService&gt; getCurrentResource()
  {
    return MyDataLoaderServiceRestService.class;
  }

  \@Override
  \@Nonnull
  protected EntitySystem getEntitySystem()
  {
    return _entitySystem;
  }

  \@Override
  \@Nonnull
  protected DataLoaderService getDataLoaderService()
  {
    return _dataLoaderService;
  }
}

</code>
 */
public abstract class AbstractDataLoaderServiceRestService<T extends AbstractDataLoaderServiceRestService>
  extends AbstractReplicantRestService
{
  public static final String CLIENT_URL_PREFIX = "replicant/client/";

  protected abstract Class<T> getCurrentResource();

  @Nonnull
  protected abstract EntitySubscriptionManager getEntitySubscriptionManager();

  @Nonnull
  protected abstract DataLoaderService getDataLoaderService();

  @PostConstruct
  @Override
  public void postConstruct()
  {
    super.postConstruct();
  }

  @Path( "connect" )
  @GET
  public Response connect( @Context final UriInfo uriInfo )
  {
    final DataLoaderService service = getDataLoaderService();
    service.connect();
    return redirectToStatus( uriInfo );
  }

  @Path( "disconnect" )
  @GET
  public Response disconnect( @Context final UriInfo uriInfo )
  {
    final DataLoaderService service = getDataLoaderService();
    service.disconnect();
    return redirectToStatus( uriInfo );
  }

  @GET
  public Response getStatus()
  {
    return doGetStatus();
  }

  @Nonnull
  protected Response redirectToStatus( final @Context UriInfo uriInfo )
  {
    final UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
    uriBuilder.path( getCurrentResource() );
    final URI resourceBaseUri = uriBuilder.build();
    return Response.seeOther( resourceBaseUri ).build();
  }

  @Nonnull
  protected Response doGetStatus()
  {
    final StringWriter writer = new StringWriter();
    final JsonGenerator g = factory().createGenerator( writer );

    final DataLoaderService service = getDataLoaderService();
    g.writeStartObject();
    g.write( "key", service.getKey() );
    g.write( "state", service.getState().name() );
    emit( g, "connectingAt", service.getConnectingAt() );
    emit( g, "connectedAt", service.getConnectedAt() );
    emit( g, "disconnectedAt", service.getDisconnectedAt() );
    emit( g, "connectionEstablishmentError", service.getLastErrorDuringConnection() );
    emit( g, "lastError", service.getLastError() );
    emit( g, "lastErrorAt", service.getLastErrorAt() );
    service.getStatusProperties().forEach( ( k, v ) -> emit( g, k, v ) );

    final ClientSession session = service.getSession();
    if ( null != session )
    {
      g.writeStartObject( "session" );
      g.write( "sessionID", session.getSessionID() );
      g.write( "lastRxSequence", session.getLastRxSequence() );

      emitChannels( g, service );
      g.writeEnd();
    }
    g.writeEnd();

    g.close();
    return buildResponse( Response.ok(), writer.toString() );
  }

  private void emit( @Nonnull final JsonGenerator g, @Nonnull final String key, @Nullable final Date value )
  {
    if ( null != value )
    {
      emit( g, key, value.toString() );
    }
  }

  private void emit( @Nonnull final JsonGenerator g, @Nonnull final String key, @Nullable final Throwable error )
  {
    if ( null != error )
    {
      if ( null != error.getMessage() )
      {
        emit( g, key, error.getMessage() );
      }
      else if ( null != error.getCause() && null != error.getCause().getMessage() )
      {
        emit( g, key, error.getCause().getMessage() );
      }
      else
      {
        emit( g, key, error.toString() );
      }
    }
  }

  private void emit( @Nonnull final JsonGenerator g, @Nonnull final String key, @Nullable final String value )
  {
    if ( null != value )
    {
      g.write( key, value );
    }
  }

  private void emitChannels( final JsonGenerator g, final DataLoaderService service )
  {
    g.writeStartObject( "channels" );
    final EntitySubscriptionManager subscriptionManager = getEntitySubscriptionManager();
    final Class<? extends Enum> graphType = service.getGraphType();
    final List<Enum> typeSubscriptions =
      subscriptionManager.getTypeSubscriptions().stream().
        filter( e -> e.getClass() == graphType ).
        collect( Collectors.toList() );
    for ( final Enum e : typeSubscriptions )
    {
      emitSubscription( g, subscriptionManager.getSubscription( new ChannelAddress( e ) ) );
    }
    g.writeEnd();
  }

  private void emitSubscription( @Nonnull final JsonGenerator g, @Nonnull final ChannelSubscriptionEntry subscription )
  {
    final ChannelAddress descriptor = subscription.getDescriptor();
    g.writeStartObject( descriptor.getGraph().name() );

    emitID( g, descriptor );
    final Object filter = subscription.getFilter();
    if ( null != filter )
    {
      g.write( "filter", JsonUtil.toJsonObject( filter ) );
    }
    g.write( "explicitSubscription", subscription.isExplicitSubscription() );

    emitEntities( g, subscription );

    g.writeEnd();
  }

  private void emitEntities( @Nonnull final JsonGenerator g, @Nonnull final ChannelSubscriptionEntry subscription )
  {
    g.writeStartObject( "entities" );
    final Map<Class<?>, Map<Object, EntitySubscriptionEntry>> entities = subscription.getEntities();
    for ( final Map.Entry<Class<?>, Map<Object, EntitySubscriptionEntry>> entry : entities.entrySet() )
    {
      g.write( entry.getKey().getName(), entry.getValue().size() );
    }
    g.writeEnd();
  }

  private void emitID( final @Nonnull JsonGenerator g, final ChannelAddress descriptor )
  {
    final Object id = descriptor.getId();
    if ( id instanceof String )
    {
      g.write( "id", (String) id );
    }
    else if ( id instanceof Integer )
    {
      g.write( "id", (Integer) id );
    }
  }
}
