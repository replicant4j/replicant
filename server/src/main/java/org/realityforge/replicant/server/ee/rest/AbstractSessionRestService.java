package org.realityforge.replicant.server.ee.rest;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.realityforge.replicant.server.ChannelAddress;
import org.realityforge.replicant.server.transport.ChannelMetaData;
import org.realityforge.replicant.server.transport.ReplicantSession;
import org.realityforge.replicant.server.transport.ReplicantSessionManager;
import org.realityforge.replicant.server.transport.SubscriptionEntry;
import org.realityforge.replicant.server.transport.SystemMetaData;
import org.realityforge.replicant.shared.SharedConstants;

/**
 * The session management rest resource.
 *
 * It is expected that this endpoint has already had security applied.
 *
 * Extend this class and provider a SessionManager as required. ie.
 *
 * <pre>
 * \@Path( ReplicantContext.CONNECTION_URL_FRAGMENT )
 * \@Produces( MediaType.APPLICATION_JSON )
 * \@ApplicationScoped
 * public class CalendarSessionRestService
 *   extends AbstractSessionRestService
 * {
 *   \@Inject
 *   private MySessionManager _sessionManager;
 *
 *   \@Override
 *   protected ReplicantSessionManager getSessionManager()
 *   {
 *     return _sessionManager;
 *   }
 *
 *   \@Override
 *   \@PostConstruct
 *   public void postConstruct()
 *   {
 *     super.postConstruct();
 *   }
 * }
 * </pre>
 */
@SuppressWarnings( "RSReferenceInspection" )
public abstract class AbstractSessionRestService
{
  private JsonGeneratorFactory _factory;

  @SuppressWarnings( "WeakerAccess" )
  @Nonnull
  protected abstract ReplicantSessionManager getSessionManager();

  @PostConstruct
  protected void postConstruct()
  {
    final HashMap<String, Object> config = new HashMap<>();
    config.put( JsonGenerator.PRETTY_PRINTING, true );
    _factory = Json.createGeneratorFactory( config );
  }

  @GET
  public Response listSessions( @Context @Nonnull final UriInfo uri )
  {
    return doListSessions( uri );
  }

  @Path( "{sessionId}" )
  @GET
  public Response getSession( @PathParam( "sessionId" ) @NotNull final String sessionId,
                              @Context @Nonnull final UriInfo uri )
  {
    return doGetSession( sessionId, uri );
  }

  @Path( "{sessionId}" + SharedConstants.CHANNEL_URL_FRAGMENT )
  @GET
  public Response getChannels( @PathParam( "sessionId" ) @NotNull final String sessionId,
                               @Context @Nonnull final UriInfo uri )
  {
    return doGetChannels( sessionId, uri );
  }

  @Path( "{sessionId}" + SharedConstants.CHANNEL_URL_FRAGMENT + "/{channelId:\\d+}" )
  @GET
  public Response getChannel( @PathParam( "sessionId" ) @NotNull final String sessionId,
                              @PathParam( "channelId" ) @NotNull final int channelId,
                              @Context @Nonnull final UriInfo uri )
  {
    final ChannelMetaData channelMetaData = getChannelMetaData( channelId );
    if ( channelMetaData.isTypeGraph() )
    {
      return doGetChannel( sessionId, toChannelDescriptor( channelId ), uri );
    }
    else
    {
      return doGetInstanceChannels( sessionId, channelId, uri );
    }
  }

  @Path( "{sessionId}" + SharedConstants.CHANNEL_URL_FRAGMENT + "/{channelId:\\d+}.{subChannelId:\\d+}" )
  @GET
  public Response getChannel( @PathParam( "sessionId" ) @NotNull final String sessionId,
                              @PathParam( "channelId" ) @NotNull final int channelId,
                              @PathParam( "subChannelId" ) @NotNull final int subChannelId,
                              @Context @Nonnull final UriInfo uri )
  {
    final ChannelMetaData channelMetaData = getChannelMetaData( channelId );
    if ( channelMetaData.isTypeGraph() )
    {
      final Response response =
        standardResponse( Response.Status.BAD_REQUEST, "Supplied subChannelIds to type graph" );
      throw new WebApplicationException( response );
    }
    return doGetChannel( sessionId, new ChannelAddress( channelId, subChannelId ), uri );
  }

  @Nonnull
  protected Response doGetChannel( @Nonnull final String sessionId,
                                   @Nonnull final ChannelAddress address,
                                   @Nonnull final UriInfo uri )
  {
    final ReplicantSession session = ensureSession( sessionId );
    final SubscriptionEntry entry = session.findSubscriptionEntry( address );
    if ( null == entry )
    {
      return standardResponse( Response.Status.NOT_FOUND, "No such channel" );
    }
    else
    {
      final String content =
        json( g -> Encoder.emitChannel( getSystemMetaData(), session, g, entry, uri ) );
      return buildResponse( Response.ok(), content );
    }
  }

  @Nonnull
  protected Response doGetChannels( @Nonnull final String sessionId,
                                    @Nonnull final UriInfo uri )
  {
    final ReplicantSession session = ensureSession( sessionId );
    final String content =
      json( g -> Encoder.emitChannelsList( getSystemMetaData(), session, g, uri ) );
    return buildResponse( Response.ok(), content );
  }

  @Nonnull
  protected Response doGetInstanceChannels( @Nonnull final String sessionId,
                                            final int channelId,
                                            @Nonnull final UriInfo uri )
  {
    final SystemMetaData systemMetaData = getSystemMetaData();
    final ReplicantSession session = ensureSession( sessionId );
    final String content =
      json( g -> Encoder.emitInstanceChannelList( systemMetaData, channelId, session, g, uri ) );
    return buildResponse( Response.ok(), content );
  }

  @Nonnull
  protected Response doListSessions( @Nonnull final UriInfo uri )
  {
    final String content = json( g -> emitSessionsList( g, uri ) );
    return buildResponse( Response.ok(), content );
  }

  private void emitSessionsList( @Nonnull final JsonGenerator g, @Nonnull final UriInfo uri )
  {
    final Set<String> sessionIds = getSessionManager().getSessionIDs();
    g.writeStartArray();
    for ( final String sessionId : sessionIds )
    {
      final ReplicantSession session = getSessionManager().getSession( sessionId );
      if ( null != session )
      {
        Encoder.emitSession( getSystemMetaData(), session, g, uri, false );
      }
    }
    g.writeEnd();
  }

  @Nonnull
  protected Response doGetSession( @Nonnull final String sessionId,
                                   @Nonnull final UriInfo uri )
  {
    final ReplicantSession session = ensureSession( sessionId );
    final String content =
      json( g -> Encoder.emitSession( getSystemMetaData(), session, g, uri, true ) );
    return buildResponse( Response.ok(), content );
  }

  @Nonnull
  private ChannelAddress toChannelDescriptor( final int channelId )
  {
    if ( getChannelMetaData( channelId ).isInstanceGraph() )
    {
      final Response response =
        standardResponse( Response.Status.BAD_REQUEST, "Failed to supply subChannelId to instance graph" );
      throw new WebApplicationException( response );
    }
    return new ChannelAddress( channelId );
  }

  @Nonnull
  private ChannelMetaData getChannelMetaData( final int channelId )
  {
    return getSystemMetaData().getChannelMetaData( channelId );
  }

  @Nonnull
  private ReplicantSession ensureSession( @Nonnull final String sessionId )
  {
    final ReplicantSession session = getSessionManager().getSession( sessionId );
    if ( null == session )
    {
      throw new WebApplicationException( standardResponse( Response.Status.NOT_FOUND, "No such session." ) );
    }
    return session;
  }

  @Nonnull
  Response standardResponse( @Nonnull final Response.Status status, @Nonnull final String message )
  {
    final String content =
      json( g ->
            {
              g.writeStartObject();
              g.write( "code", status.getStatusCode() );
              g.write( "description", message );
              g.writeEnd();
              g.close();
            } );

    final Response.ResponseBuilder builder = Response.status( status );
    return buildResponse( builder, content );
  }

  @Nonnull
  private String json( @Nonnull final Consumer<JsonGenerator> consumer )
  {
    final StringWriter writer = new StringWriter();
    final JsonGenerator g = _factory.createGenerator( writer );
    consumer.accept( g );
    g.close();
    return writer.toString();
  }

  @Nonnull
  private SystemMetaData getSystemMetaData()
  {
    return getSessionManager().getSystemMetaData();
  }

  @Nonnull
  private Response buildResponse( @Nonnull final Response.ResponseBuilder builder, @Nonnull final String content )
  {
    CacheUtil.configureNoCacheHeaders( builder );
    return builder.entity( content ).build();
  }
}
