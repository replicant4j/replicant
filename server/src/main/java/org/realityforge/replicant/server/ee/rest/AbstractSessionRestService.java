package org.realityforge.replicant.server.ee.rest;

import java.io.StringWriter;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.json.stream.JsonGenerator;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.realityforge.replicant.server.ChannelDescriptor;
import org.realityforge.replicant.server.ee.EntityMessageCacheUtil;
import org.realityforge.replicant.server.ee.ReplicantContextHolder;
import org.realityforge.replicant.server.ee.Replicate;
import org.realityforge.replicant.server.transport.ReplicantSession;
import org.realityforge.replicant.server.transport.ReplicantSessionManager;
import org.realityforge.replicant.server.transport.SubscriptionEntry;
import org.realityforge.replicant.shared.ee.rest.AbstractReplicantRestService;
import org.realityforge.replicant.shared.transport.ReplicantContext;
import org.realityforge.rest.field_filter.FieldFilter;

/**
 * The session management rest resource.
 *
 * It is expected that this endpoint has already had security applied.
 *
 * Extend this class and provider a SessionManager as required. ie.
 *
 * <pre>
 * \@Path( ReplicantContext.SESSION_URL_FRAGMENT )
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
  extends AbstractReplicantRestService
{
  @Nonnull
  protected abstract ReplicantSessionManager getSessionManager();

  @Nonnull
  protected abstract TransactionSynchronizationRegistry getRegistry();

  @POST
  @Produces( MediaType.TEXT_PLAIN )
  public Response createSession()
  {
    return doCreateSession();
  }

  @Path( "{sessionID}" )
  @DELETE
  public Response deleteSession( @PathParam( "sessionID" ) @NotNull final String sessionID )
  {
    return doDeleteSession( sessionID );
  }

  @GET
  public Response listSessions( @QueryParam( "fields" ) @DefaultValue( "url" ) @NotNull final FieldFilter filter,
                                @Context @Nonnull final UriInfo uri )
  {
    return doListSessions( filter, uri );
  }

  @Path( "{sessionID}" )
  @GET
  public Response getSession( @PathParam( "sessionID" ) @NotNull final String sessionID,
                              @QueryParam( "fields" ) @DefaultValue( "" ) @Nonnull final FieldFilter filter,
                              @Context @Nonnull final UriInfo uri )
  {
    return doGetSession( sessionID, filter, uri );
  }

  @Path( "{sessionID}" + ReplicantContext.CHANNEL_URL_FRAGMENT )
  @GET
  public Response getChannels( @PathParam( "sessionID" ) @NotNull final String sessionID,
                               @QueryParam( "fields" ) @DefaultValue( "" ) @Nonnull final FieldFilter filter,
                               @Context @Nonnull final UriInfo uri )
  {
    return doGetChannels( sessionID, filter, uri );
  }

  @Path( "{sessionID}" + ReplicantContext.CHANNEL_URL_FRAGMENT + "/{channelID:\\d+}" )
  @GET
  public Response getTypeChannel( @PathParam( "sessionID" ) @NotNull final String sessionID,
                                  @PathParam( "channelID" ) @NotNull final int channelID,
                                  @QueryParam( "fields" ) @DefaultValue( "" ) @Nonnull final FieldFilter filter,
                                  @Context @Nonnull final UriInfo uri )
  {
    return doGetChannel( sessionID, new ChannelDescriptor( channelID ), filter, uri );
  }

  @Path( "{sessionID}" + ReplicantContext.CHANNEL_URL_FRAGMENT + "/{channelID:\\d+}.{subChannelID:\\d+}" )
  @GET
  public Response getInstanceChannel( @PathParam( "sessionID" ) @NotNull final String sessionID,
                                      @PathParam( "channelID" ) @NotNull final int channelID,
                                      @PathParam( "subChannelID" ) @NotNull final int subChannelID,
                                      @QueryParam( "fields" ) @DefaultValue( "" ) @Nonnull final FieldFilter filter,
                                      @Context @Nonnull final UriInfo uri )
  {
    return doGetChannel( sessionID, new ChannelDescriptor( channelID, subChannelID ), filter, uri );
  }

  @Path( "{sessionID}" + ReplicantContext.CHANNEL_URL_FRAGMENT + "/{channelID:\\d+}.{subChannelID}" )
  @GET
  public Response getInstanceChannel( @PathParam( "sessionID" ) @NotNull final String sessionID,
                                      @PathParam( "channelID" ) @NotNull final int channelID,
                                      @PathParam( "subChannelID" ) @NotNull final String subChannelID,
                                      @QueryParam( "fields" ) @DefaultValue( "" ) @Nonnull final FieldFilter filter,
                                      @Context @Nonnull final UriInfo uri )
  {
    return doGetChannel( sessionID, new ChannelDescriptor( channelID, subChannelID ), filter, uri );
  }

  @Replicate
  @Path( "{sessionID}" + ReplicantContext.CHANNEL_URL_FRAGMENT + "/{channelID:\\d+}" )
  @DELETE
  public Response unsubscribeTypeChannel( @PathParam( "sessionID" ) @NotNull final String sessionID,
                                          @PathParam( "channelID" ) @NotNull final int channelID )
  {
    return doUnsubscribeChannel( sessionID, new ChannelDescriptor( channelID ) );
  }

  @Replicate
  @Path( "{sessionID}" + ReplicantContext.CHANNEL_URL_FRAGMENT + "/{channelID:\\d+}.{subChannelID:\\d+}" )
  @DELETE
  public Response unsubscribeInstanceChannel( @PathParam( "sessionID" ) @NotNull final String sessionID,
                                              @PathParam( "channelID" ) @NotNull final int channelID,
                                              @PathParam( "subChannelID" ) @NotNull final int subChannelID )
  {
    return doUnsubscribeChannel( sessionID, new ChannelDescriptor( channelID, subChannelID ) );
  }

  @Replicate
  @Path( "{sessionID}" + ReplicantContext.CHANNEL_URL_FRAGMENT + "/{channelID:\\d+}.{subChannelID}" )
  @DELETE
  public Response unsubscribeInstanceChannel( @PathParam( "sessionID" ) @NotNull final String sessionID,
                                              @PathParam( "channelID" ) @NotNull final int channelID,
                                              @PathParam( "subChannelID" ) @NotNull final String subChannelID )
  {
    return doUnsubscribeChannel( sessionID, new ChannelDescriptor( channelID, subChannelID ) );
  }

  @Nonnull
  protected Response doUnsubscribeChannel( @Nonnull final String sessionID,
                                           @Nonnull final ChannelDescriptor descriptor )
  {
    final ReplicantSession session = ensureSession( sessionID );
    getSessionManager().unsubscribe( session,
                                     descriptor,
                                     true,
                                     EntityMessageCacheUtil.getSessionChanges() );
    return standardResponse( Response.Status.OK, "Channel subscription removed." );
  }

  @Nonnull
  protected Response doGetChannel( @Nonnull final String sessionID,
                                   @Nonnull final ChannelDescriptor descriptor,
                                   @Nonnull final FieldFilter filter,
                                   @Nonnull final UriInfo uri )
  {
    final ReplicantSession session = ensureSession( sessionID );
    final SubscriptionEntry entry = session.findSubscriptionEntry( descriptor );
    if ( null == entry )
    {
      return standardResponse( Response.Status.NOT_FOUND, "No such channel" );
    }
    else
    {
      final String content = json( g -> Encoder.emitChannel( getSessionManager(), session, g, entry, filter, uri ) );
      return buildResponse( Response.ok(), content );
    }
  }

  @Nonnull
  protected Response doGetChannels( @Nonnull final String sessionID,
                                    @Nonnull final FieldFilter filter,
                                    @Nonnull final UriInfo uri )
  {
    final ReplicantSession session = ensureSession( sessionID );
    final String content = json( g -> Encoder.emitChannelsList( getSessionManager(), session, g, filter, uri ) );
    return buildResponse( Response.ok(), content );
  }

  @Nonnull
  protected Response doCreateSession()
  {
    return buildResponse( Response.ok(), getSessionManager().createSession().getSessionID() );
  }

  @Nonnull
  protected Response doDeleteSession( @Nonnull final String sessionID )
  {
    getSessionManager().invalidateSession( sessionID );
    return standardResponse( Response.Status.OK, "Session removed." );
  }

  @Nonnull
  protected Response doListSessions( @Nonnull final FieldFilter filter,
                                     @Nonnull final UriInfo uri )
  {
    final String content = json( g -> emitSessionsList( g, filter, uri ) );
    return buildResponse( Response.ok(), content );
  }

  private void emitSessionsList( @Nonnull final JsonGenerator g,
                                 @Nonnull final FieldFilter filter,
                                 @Nonnull final UriInfo uri )
  {
    final Set<String> sessionIDs = getSessionManager().getSessionIDs();
    g.writeStartArray();
    for ( final String sessionID : sessionIDs )
    {
      final ReplicantSession session = getSessionManager().getSession( sessionID );
      if ( null != session )
      {
        Encoder.emitSession( getSessionManager(), session, g, filter, uri );
      }
    }
    g.writeEnd();
  }

  @Nonnull
  protected Response doGetSession( @Nonnull final String sessionID,
                                   @Nonnull final FieldFilter filter,
                                   @Nonnull final UriInfo uri )
  {
    final ReplicantSession session = ensureSession( sessionID );
    final String content = json( g -> Encoder.emitSession( getSessionManager(), session, g, filter, uri ) );
    return buildResponse( Response.ok(), content );
  }

  @Nonnull
  private ReplicantSession ensureSession( @Nonnull final String sessionID )
  {
    final ReplicantSession session = getSessionManager().getSession( sessionID );
    if ( null == session )
    {
      throw new WebApplicationException( standardResponse( Response.Status.NOT_FOUND, "No such session." ) );
    }
    getRegistry().putResource( ReplicantContext.SESSION_ID_KEY, sessionID );
    return session;
  }

  @Nonnull
  protected Response standardResponse( @Nonnull final Response.Status status, @Nonnull final String message )
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
    configureCompletionHeader( builder );
    return buildResponse( builder, content );
  }

  private void configureCompletionHeader( @Nonnull final Response.ResponseBuilder builder )
  {
    final String complete = (String) ReplicantContextHolder.remove( ReplicantContext.REQUEST_COMPLETE_KEY );
    if ( null != complete )
    {
      builder.header( ReplicantContext.REQUEST_COMPLETE_HEADER, complete );
    }
  }

  @Nonnull
  private String json( @Nonnull final Consumer<JsonGenerator> consumer )
  {
    final StringWriter writer = new StringWriter();
    final JsonGenerator g = factory().createGenerator( writer );
    consumer.accept( g );
    g.close();
    return writer.toString();
  }
}
