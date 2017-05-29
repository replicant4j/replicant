package org.realityforge.replicant.server.ee.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.stream.JsonGenerator;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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
import org.realityforge.replicant.server.transport.ChannelMetaData;
import org.realityforge.replicant.server.transport.ReplicantSession;
import org.realityforge.replicant.server.transport.ReplicantSessionManager;
import org.realityforge.replicant.server.transport.SubscriptionEntry;
import org.realityforge.replicant.server.transport.SystemMetaData;
import org.realityforge.replicant.shared.ee.rest.AbstractReplicantRestService;
import org.realityforge.replicant.shared.transport.ReplicantContext;

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
  private transient final ObjectMapper _jsonMapper = new ObjectMapper();

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
  public Response listSessions( @Context @Nonnull final UriInfo uri )
  {
    return doListSessions( uri );
  }

  @Path( "{sessionID}" )
  @GET
  public Response getSession( @PathParam( "sessionID" ) @NotNull final String sessionID,
                              @Context @Nonnull final UriInfo uri )
  {
    return doGetSession( sessionID, uri );
  }

  @Path( "{sessionID}" + ReplicantContext.CHANNEL_URL_FRAGMENT )
  @GET
  public Response getChannels( @PathParam( "sessionID" ) @NotNull final String sessionID,
                               @Context @Nonnull final UriInfo uri )
  {
    return doGetChannels( sessionID, uri );
  }

  @Path( "{sessionID}" + ReplicantContext.CHANNEL_URL_FRAGMENT + "/{channelID:\\d+}" )
  @GET
  public Response getChannel( @PathParam( "sessionID" ) @NotNull final String sessionID,
                              @PathParam( "channelID" ) @NotNull final int channelID,
                              @Context @Nonnull final UriInfo uri )
  {
    final ChannelMetaData channelMetaData = getChannelMetaData( channelID );
    if ( channelMetaData.isTypeGraph() )
    {
      return doGetChannel( sessionID, toChannelDescriptor( channelID ), uri );
    }
    else
    {
      return doGetInstanceChannels( sessionID, channelID, uri );
    }
  }

  @Path( "{sessionID}" + ReplicantContext.CHANNEL_URL_FRAGMENT + "/{channelID:\\d+}.{subChannelID}" )
  @GET
  public Response getChannel( @PathParam( "sessionID" ) @NotNull final String sessionID,
                              @PathParam( "channelID" ) @NotNull final int channelID,
                              @PathParam( "subChannelID" ) @NotNull final String subChannelID,
                              @Context @Nonnull final UriInfo uri )
  {
    return doGetChannel( sessionID, toChannelDescriptor( channelID, subChannelID ), uri );
  }

  @Replicate
  @Path( "{sessionID}" + ReplicantContext.CHANNEL_URL_FRAGMENT + "/{channelID:\\d+}" )
  @DELETE
  public Response unsubscribeFromChannel( @PathParam( "sessionID" ) @NotNull final String sessionID,
                                          @PathParam( "channelID" ) final int channelID,
                                          @QueryParam( "scid" ) @Nullable final String subChannelIDs )
  {
    if ( null == subChannelIDs )
    {
      //Handle type channel
      return doUnsubscribeChannel( sessionID, toChannelDescriptor( channelID ) );
    }
    else
    {
      final ChannelMetaData channelMetaData = getChannelMetaData( channelID );
      if ( channelMetaData.isTypeGraph() )
      {
        final Response response =
          standardResponse( Response.Status.BAD_REQUEST, "Supplied subChannelIDs to type graph" );
        throw new WebApplicationException( response );
      }
      //A bulk subscribe to an instance channel
      final ArrayList<Serializable> scids = new ArrayList<>();
      for ( final String scid : subChannelIDs.split( "," ) )
      {
        scids.add( toSubChannelID( channelMetaData, scid ) );
      }
      return doBulkUnsubscribeChannel( sessionID, channelID, scids );
    }
  }

  @Replicate
  @Path( "{sessionID}" + ReplicantContext.CHANNEL_URL_FRAGMENT + "/{channelID:\\d+}.{subChannelID}" )
  @DELETE
  public Response unsubscribeFromInstanceChannel( @PathParam( "sessionID" ) @NotNull final String sessionID,
                                                  @PathParam( "channelID" ) final int channelID,
                                                  @PathParam( "subChannelID" ) @NotNull final String subChannelText )
  {
    return doUnsubscribeChannel( sessionID, toChannelDescriptor( channelID, subChannelText ) );
  }

  @Replicate
  @Path( "{sessionID}" + ReplicantContext.CHANNEL_URL_FRAGMENT + "/{channelID:\\d+}" )
  @PUT
  public Response subscribeToChannel( @PathParam( "sessionID" ) @NotNull final String sessionID,
                                      @PathParam( "channelID" ) @NotNull final int channelID,
                                      @QueryParam( "scid" ) @Nullable final String subChannelIDs,
                                      @Nonnull final String filterContent )
  {
    if ( null == subChannelIDs )
    {
      //Subscription to a type channel
      return doSubscribeChannel( sessionID, toChannelDescriptor( channelID ), filterContent );
    }
    else
    {
      final ChannelMetaData channelMetaData = getChannelMetaData( channelID );
      if ( channelMetaData.isTypeGraph() )
      {
        final Response response =
          standardResponse( Response.Status.BAD_REQUEST, "Supplied subChannelIDs to type graph" );
        throw new WebApplicationException( response );
      }
      //A bulk subscribe to an instance channel
      final ArrayList<Serializable> scids = new ArrayList<>();
      for ( final String scid : subChannelIDs.split( "," ) )
      {
        scids.add( toSubChannelID( channelMetaData, scid ) );
      }
      return doBulkSubscribeChannel( sessionID, channelID, scids, filterContent );
    }
  }

  @Replicate
  @Path( "{sessionID}" + ReplicantContext.CHANNEL_URL_FRAGMENT + "/{channelID:\\d+}.{subChannelID}" )
  @PUT
  public Response subscribeToInstanceChannel( @PathParam( "sessionID" ) @NotNull final String sessionID,
                                              @PathParam( "channelID" ) @NotNull final int channelID,
                                              @PathParam( "subChannelID" ) @NotNull final String subChannelText,
                                              @Nonnull final String filterContent )
  {
    return doSubscribeChannel( sessionID, toChannelDescriptor( channelID, subChannelText ), filterContent );
  }

  @Nonnull
  protected Response doSubscribeChannel( @Nonnull final String sessionID,
                                         @Nonnull final ChannelDescriptor descriptor,
                                         @Nonnull final String filterContent )
  {
    final ReplicantSession session = ensureSession( sessionID );
    getSessionManager().subscribe( session,
                                   descriptor,
                                   true,
                                   toFilter( getChannelMetaData( descriptor ), filterContent ),
                                   EntityMessageCacheUtil.getSessionChanges() );
    return standardResponse( Response.Status.OK, "Channel subscription added." );
  }

  @Nonnull
  protected Response doBulkSubscribeChannel( @Nonnull final String sessionID,
                                             final int channelID,
                                             @Nonnull Collection<Serializable> subChannelIDs,
                                             @Nonnull final String filterContent )
  {
    final ReplicantSession session = ensureSession( sessionID );
    final Object filter = toFilter( getChannelMetaData( channelID ), filterContent );
    getSessionManager().bulkSubscribe( session,
                                       channelID,
                                       subChannelIDs,
                                       filter,
                                       true,
                                       EntityMessageCacheUtil.getSessionChanges() );
    return standardResponse( Response.Status.OK, "Channel subscriptions added." );
  }

  @Nonnull
  protected Response doBulkUnsubscribeChannel( @Nonnull final String sessionID,
                                               final int channelID,
                                               @Nonnull Collection<Serializable> subChannelIDs )
  {
    final ReplicantSession session = ensureSession( sessionID );
    getSessionManager().bulkUnsubscribe( session,
                                         channelID,
                                         subChannelIDs,
                                         true,
                                         EntityMessageCacheUtil.getSessionChanges() );
    return standardResponse( Response.Status.OK, "Channel subscriptions removed." );
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
      final String content =
        json( g -> Encoder.emitChannel( getSessionManager().getSystemMetaData(), session, g, entry, uri ) );
      return buildResponse( Response.ok(), content );
    }
  }

  @Nonnull
  protected Response doGetChannels( @Nonnull final String sessionID,
                                    @Nonnull final UriInfo uri )
  {
    final ReplicantSession session = ensureSession( sessionID );
    final String content =
      json( g -> Encoder.emitChannelsList( getSessionManager().getSystemMetaData(), session, g, uri ) );
    return buildResponse( Response.ok(), content );
  }

  @Nonnull
  protected Response doGetInstanceChannels( @Nonnull final String sessionID,
                                            final int channelID,
                                            @Nonnull final UriInfo uri )
  {
    final SystemMetaData systemMetaData = getSessionManager().getSystemMetaData();
    final ReplicantSession session = ensureSession( sessionID );
    final String content =
      json( g -> Encoder.emitInstanceChannelList( systemMetaData, channelID, session, g, uri ) );
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
  protected Response doListSessions( @Nonnull final UriInfo uri )
  {
    final String content = json( g -> emitSessionsList( g, uri ) );
    return buildResponse( Response.ok(), content );
  }

  private void emitSessionsList( @Nonnull final JsonGenerator g, @Nonnull final UriInfo uri )
  {
    final Set<String> sessionIDs = getSessionManager().getSessionIDs();
    g.writeStartArray();
    for ( final String sessionID : sessionIDs )
    {
      final ReplicantSession session = getSessionManager().getSession( sessionID );
      if ( null != session )
      {
        Encoder.emitSession( getSessionManager().getSystemMetaData(), session, g, uri, false );
      }
    }
    g.writeEnd();
  }

  @Nonnull
  protected Response doGetSession( @Nonnull final String sessionID, @Nonnull final UriInfo uri )
  {
    final ReplicantSession session = ensureSession( sessionID );
    final String content =
      json( g -> Encoder.emitSession( getSessionManager().getSystemMetaData(), session, g, uri, true ) );
    return buildResponse( Response.ok(), content );
  }

  @Nonnull
  private ChannelDescriptor toChannelDescriptor( final int channelID, @Nonnull final String subChannelText )
  {
    return new ChannelDescriptor( channelID, extractSubChannelID( channelID, subChannelText ) );
  }

  @Nonnull
  private ChannelDescriptor toChannelDescriptor( final int channelID )
  {
    if ( getChannelMetaData( channelID ).isTypeGraph() )
    {
      final Response response =
        standardResponse( Response.Status.BAD_REQUEST, "Failed to supply subChannelID to instance graph" );
      throw new WebApplicationException( response );
    }
    return new ChannelDescriptor( channelID );
  }

  @Nonnull
  private Serializable extractSubChannelID( final int channelID, @Nonnull final String subChannelText )
  {
    final ChannelMetaData channelMetaData = getChannelMetaData( channelID );
    if ( channelMetaData.isTypeGraph() )
    {
      final Response response =
        standardResponse( Response.Status.BAD_REQUEST, "Attempted to supply subChannelID to type graph" );
      throw new WebApplicationException( response );
    }
    return toSubChannelID( channelMetaData, subChannelText );
  }

  @Nonnull
  private Serializable toSubChannelID( @Nonnull final ChannelMetaData channelMetaData, @Nonnull final String value )
  {
    final Class subChannelType = channelMetaData.getSubChannelType();
    if ( Integer.class == subChannelType )
    {
      return Integer.parseInt( value );
    }
    else if ( String.class == subChannelType )
    {
      return value;
    }
    else
    {
      final Response response =
        standardResponse( Response.Status.BAD_REQUEST, "Channel has invalid subChannel type" );
      throw new WebApplicationException( response );
    }
  }

  @Nonnull
  private ChannelMetaData getChannelMetaData( final int channelID )
  {
    return getSessionManager().getSystemMetaData().getChannelMetaData( channelID );
  }

  @Nonnull
  private ChannelMetaData getChannelMetaData( @Nonnull final ChannelDescriptor descriptor )
  {
    return getSessionManager().getSystemMetaData().getChannelMetaData( descriptor );
  }

  @Nullable
  private Object toFilter( @Nonnull final ChannelMetaData channelMetaData, @Nonnull final String filterContent )
  {
    return ChannelMetaData.FilterType.NONE == channelMetaData.getFilterType() ?
           null :
           parseFilter( channelMetaData, filterContent );
  }

  @SuppressWarnings( "unchecked" )
  @Nonnull
  private Object parseFilter( @Nonnull final ChannelMetaData channelMetaData,
                              @Nonnull final String filterContent )
  {
    try
    {
      return _jsonMapper.readValue( filterContent, channelMetaData.getFilterParameterType() );
    }
    catch ( final IOException ioe )
    {
      final Response response = standardResponse( Response.Status.BAD_REQUEST, "Invalid or missing filter" );
      throw new WebApplicationException( response );
    }
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
