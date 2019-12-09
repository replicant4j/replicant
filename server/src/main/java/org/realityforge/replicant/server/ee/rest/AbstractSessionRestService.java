package org.realityforge.replicant.server.ee.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.stream.JsonGenerator;
import javax.persistence.EntityManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
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
import org.realityforge.replicant.server.ChannelAddress;
import org.realityforge.replicant.server.EntityMessageEndpoint;
import org.realityforge.replicant.server.ServerConstants;
import org.realityforge.replicant.server.ee.EntityMessageCacheUtil;
import org.realityforge.replicant.server.ee.ReplicantContextHolder;
import org.realityforge.replicant.server.ee.ReplicationRequestUtil;
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
public abstract class AbstractSessionRestService
  extends AbstractReplicantRestService
{
  private transient final ObjectMapper _jsonMapper = new ObjectMapper();

  @Nonnull
  protected abstract ReplicantSessionManager getSessionManager();

  @Nonnull
  protected abstract EntityMessageEndpoint getEntityMessageEndpoint();

  @Nonnull
  protected abstract EntityManager getEntityManager();

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

  @Path( "{sessionID}" + SharedConstants.CHANNEL_URL_FRAGMENT )
  @GET
  public Response getChannels( @PathParam( "sessionID" ) @NotNull final String sessionID,
                               @Context @Nonnull final UriInfo uri )
  {
    return doGetChannels( sessionID, uri );
  }

  @Path( "{sessionID}" + SharedConstants.CHANNEL_URL_FRAGMENT + "/{channelId:\\d+}" )
  @GET
  public Response getChannel( @PathParam( "sessionID" ) @NotNull final String sessionID,
                              @PathParam( "channelId" ) @NotNull final int channelId,
                              @Context @Nonnull final UriInfo uri )
  {
    final ChannelMetaData channelMetaData = getChannelMetaData( channelId );
    if ( channelMetaData.isTypeGraph() )
    {
      return doGetChannel( sessionID, toChannelDescriptor( channelId ), uri );
    }
    else
    {
      return doGetInstanceChannels( sessionID, channelId, uri );
    }
  }

  @Path( "{sessionID}" + SharedConstants.CHANNEL_URL_FRAGMENT + "/{channelId:\\d+}.{subChannelId}" )
  @GET
  public Response getChannel( @PathParam( "sessionID" ) @NotNull final String sessionID,
                              @PathParam( "channelId" ) @NotNull final int channelId,
                              @PathParam( "subChannelId" ) @NotNull final String subChannelId,
                              @Context @Nonnull final UriInfo uri )
  {
    return doGetChannel( sessionID, toChannelDescriptor( channelId, subChannelId ), uri );
  }

  @Path( "{sessionID}" + SharedConstants.CHANNEL_URL_FRAGMENT + "/{channelId:\\d+}" )
  @DELETE
  public Response unsubscribeFromChannel( @PathParam( "sessionID" ) @NotNull final String sessionID,
                                          @PathParam( "channelId" ) final int channelId,
                                          @HeaderParam( SharedConstants.REQUEST_ID_HEADER ) @Nullable final Integer requestId,
                                          @QueryParam( "scid" ) @Nullable final String subChannelIds )
  {
    if ( null == subChannelIds )
    {
      //Handle type channel
      return doUnsubscribeChannel( sessionID, requestId, toChannelDescriptor( channelId ) );
    }
    else
    {
      final ChannelMetaData channelMetaData = getChannelMetaData( channelId );
      if ( channelMetaData.isTypeGraph() )
      {
        final Response response =
          standardResponse( Response.Status.BAD_REQUEST, "Supplied subChannelIds to type graph" );
        throw new WebApplicationException( response );
      }
      //A bulk subscribe to an instance channel
      final List<Integer> scids = new ArrayList<>();
      for ( final String scid : subChannelIds.split( "," ) )
      {
        scids.add( Integer.parseInt( scid ) );
      }
      return doBulkUnsubscribeChannel( sessionID, requestId, channelId, scids );
    }
  }

  @Path( "{sessionID}" + SharedConstants.CHANNEL_URL_FRAGMENT + "/{channelId:\\d+}.{subChannelId}" )
  @DELETE
  public Response unsubscribeFromInstanceChannel( @PathParam( "sessionID" ) @NotNull final String sessionID,
                                                  @PathParam( "channelId" ) final int channelId,
                                                  @PathParam( "subChannelId" ) @NotNull final String subChannelText,
                                                  @HeaderParam( SharedConstants.REQUEST_ID_HEADER ) @Nullable final Integer requestId )
  {
    return doUnsubscribeChannel( sessionID, requestId, toChannelDescriptor( channelId, subChannelText ) );
  }

  @Path( "{sessionID}" + SharedConstants.CHANNEL_URL_FRAGMENT + "/{channelId:\\d+}" )
  @PUT
  public Response subscribeToChannel( @PathParam( "sessionID" ) @NotNull final String sessionID,
                                      @PathParam( "channelId" ) @NotNull final int channelId,
                                      @HeaderParam( SharedConstants.REQUEST_ID_HEADER ) @Nullable final Integer requestId,
                                      @HeaderParam( SharedConstants.ETAG_HEADER ) @Nullable final String eTag,
                                      @QueryParam( "scid" ) @Nullable final String subChannelIds,
                                      @Nonnull final String filterContent )
  {
    if ( null == subChannelIds )
    {
      //Subscription to a type channel
      return doSubscribeChannel( sessionID, requestId, eTag, toChannelDescriptor( channelId ), filterContent );
    }
    else
    {
      final ChannelMetaData channelMetaData = getChannelMetaData( channelId );
      if ( channelMetaData.isTypeGraph() )
      {
        final Response response =
          standardResponse( Response.Status.BAD_REQUEST, "Supplied subChannelIds to type graph" );
        throw new WebApplicationException( response );
      }
      //A bulk subscribe to an instance channel
      final List<Integer> scids = new ArrayList<>();
      for ( final String scid : subChannelIds.split( "," ) )
      {
        scids.add( Integer.parseInt( scid ) );
      }
      return doBulkSubscribeChannel( sessionID, requestId, channelId, scids, filterContent );
    }
  }

  @Path( "{sessionID}" + SharedConstants.CHANNEL_URL_FRAGMENT + "/{channelId:\\d+}.{subChannelId}" )
  @PUT
  public Response subscribeToInstanceChannel( @PathParam( "sessionID" ) @NotNull final String sessionID,
                                              @PathParam( "channelId" ) @NotNull final int channelId,
                                              @PathParam( "subChannelId" ) @NotNull final String subChannelText,
                                              @HeaderParam( SharedConstants.REQUEST_ID_HEADER ) @Nullable final Integer requestId,
                                              @HeaderParam( SharedConstants.ETAG_HEADER ) @Nullable final String eTag,
                                              @Nonnull final String filterContent )
  {
    return doSubscribeChannel( sessionID,
                               requestId,
                               eTag,
                               toChannelDescriptor( channelId, subChannelText ),
                               filterContent );
  }

  @Nonnull
  protected Response doSubscribeChannel( @Nonnull final String sessionID,
                                         @Nullable final Integer requestId,
                                         @Nullable final String eTag,
                                         @Nonnull final ChannelAddress descriptor,
                                         @Nonnull final String filterContent )
  {
    final Supplier<ReplicantSessionManager.CacheStatus> action = () ->
    {
      final ReplicantSession session = ensureSession( sessionID, requestId );
      session.setETag( descriptor, eTag );
      return getSessionManager().subscribe( session,
                                            descriptor,
                                            true,
                                            toFilter( getChannelMetaData( descriptor ), filterContent ),
                                            EntityMessageCacheUtil.getSessionChanges() );
    };
    final String invocationKey =
      getInvocationKey( descriptor.getChannelId(), descriptor.getSubChannelId(), "Subscribe" );
    final ReplicantSessionManager.CacheStatus cacheStatus = runRequest( invocationKey, sessionID, requestId, action );
    final Response.Status status =
      cacheStatus == ReplicantSessionManager.CacheStatus.USE ? Response.Status.NO_CONTENT : Response.Status.OK;
    return standardResponse( status, "Channel subscription added." );
  }

  @Nonnull
  protected Response doBulkSubscribeChannel( @Nonnull final String sessionID,
                                             @Nullable final Integer requestId,
                                             final int channelId,
                                             @Nonnull Collection<Integer> subChannelIds,
                                             @Nonnull final String filterContent )
  {
    final Runnable action = () ->
    {
      final ReplicantSession session = ensureSession( sessionID, requestId );
      final Object filter = toFilter( getChannelMetaData( channelId ), filterContent );
      getSessionManager().bulkSubscribe( session,
                                         channelId,
                                         subChannelIds,
                                         filter,
                                         true,
                                         EntityMessageCacheUtil.getSessionChanges() );
    };
    runRequest( getInvocationKey( channelId, null, "BulkSubscribe" ), sessionID, requestId, action );

    return standardResponse( Response.Status.OK, "Channel subscriptions added." );
  }

  @Nonnull
  protected Response doBulkUnsubscribeChannel( @Nonnull final String sessionID,
                                               @Nullable final Integer requestId,
                                               final int channelId,
                                               @Nonnull final Collection<Integer> subChannelIds )
  {
    final Runnable action = () ->
      getSessionManager().bulkUnsubscribe( ensureSession( sessionID, requestId ),
                                           channelId,
                                           subChannelIds,
                                           true,
                                           EntityMessageCacheUtil.getSessionChanges() );
    runRequest( getInvocationKey( channelId, null, "BulkUnsubscribe" ), sessionID, requestId, action );
    return standardResponse( Response.Status.OK, "Channel subscriptions removed." );
  }

  @Nonnull
  protected Response doUnsubscribeChannel( @Nonnull final String sessionID,
                                           @Nullable final Integer requestId,
                                           @Nonnull final ChannelAddress descriptor )
  {
    final Runnable action = () ->
      getSessionManager().unsubscribe( ensureSession( sessionID, requestId ),
                                       descriptor,
                                       true,
                                       EntityMessageCacheUtil.getSessionChanges() );
    runRequest( getInvocationKey( descriptor.getChannelId(), descriptor.getSubChannelId(), "Unsubscribe" ),
                sessionID,
                requestId,
                action );
    return standardResponse( Response.Status.OK, "Channel subscription removed." );
  }

  @Nonnull
  protected Response doGetChannel( @Nonnull final String sessionID,
                                   @Nonnull final ChannelAddress descriptor,
                                   @Nonnull final UriInfo uri )
  {
    final ReplicantSession session = ensureSession( sessionID, null );
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
    final ReplicantSession session = ensureSession( sessionID, null );
    final String content =
      json( g -> Encoder.emitChannelsList( getSessionManager().getSystemMetaData(), session, g, uri ) );
    return buildResponse( Response.ok(), content );
  }

  @Nonnull
  protected Response doGetInstanceChannels( @Nonnull final String sessionID,
                                            final int channelId,
                                            @Nonnull final UriInfo uri )
  {
    final SystemMetaData systemMetaData = getSessionManager().getSystemMetaData();
    final ReplicantSession session = ensureSession( sessionID, null );
    final String content =
      json( g -> Encoder.emitInstanceChannelList( systemMetaData, channelId, session, g, uri ) );
    return buildResponse( Response.ok(), content );
  }

  @Nonnull
  protected Response doCreateSession()
  {
    return buildResponse( Response.ok(), getSessionManager().createSession().getId() );
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
  protected Response doGetSession( @Nonnull final String sessionID,
                                   @Nonnull final UriInfo uri )
  {
    final ReplicantSession session = ensureSession( sessionID, null );
    final String content =
      json( g -> Encoder.emitSession( getSessionManager().getSystemMetaData(), session, g, uri, true ) );
    return buildResponse( Response.ok(), content );
  }

  @Nonnull
  private ChannelAddress toChannelDescriptor( final int channelId, @Nonnull final String subChannelText )
  {
    return new ChannelAddress( channelId, extractSubChannelId( channelId, subChannelText ) );
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
  private Integer extractSubChannelId( final int channelId, @Nonnull final String subChannelText )
  {
    final ChannelMetaData channelMetaData = getChannelMetaData( channelId );
    if ( channelMetaData.isTypeGraph() )
    {
      final Response response =
        standardResponse( Response.Status.BAD_REQUEST, "Attempted to supply subChannelId to type graph" );
      throw new WebApplicationException( response );
    }
    else
    {
      return Integer.parseInt( subChannelText );
    }
  }

  @Nonnull
  private ChannelMetaData getChannelMetaData( final int channelId )
  {
    return getSessionManager().getSystemMetaData().getChannelMetaData( channelId );
  }

  @Nonnull
  private ChannelMetaData getChannelMetaData( @Nonnull final ChannelAddress descriptor )
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
  private ReplicantSession ensureSession( @Nonnull final String sessionID, @Nullable final Integer requestId )
  {
    final ReplicantSession session = getSessionManager().getSession( sessionID );
    if ( null == session )
    {
      throw new WebApplicationException( standardResponse( Response.Status.NOT_FOUND, "No such session." ) );
    }
    getRegistry().putResource( ServerConstants.SESSION_ID_KEY, sessionID );
    getRegistry().putResource( ServerConstants.REQUEST_ID_KEY, requestId );
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
    final String complete = (String) ReplicantContextHolder.remove( ServerConstants.REQUEST_COMPLETE_KEY );
    if ( null != complete )
    {
      builder.header( SharedConstants.REQUEST_COMPLETE_HEADER, complete );
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

  private ReplicantSessionManager.CacheStatus runRequest( @Nonnull final String invocationKey,
                                                          @Nonnull final String sessionID,
                                                          @Nullable final Integer requestId,
                                                          @Nonnull final Supplier<ReplicantSessionManager.CacheStatus> action )
  {
    return ReplicationRequestUtil.runRequest( getRegistry(),
                                              getEntityManager(),
                                              getEntityMessageEndpoint(),
                                              invocationKey,
                                              sessionID,
                                              requestId,
                                              action );
  }

  private void runRequest( @Nonnull final String invocationKey,
                           @Nonnull final ReplicantSession session,
                           @Nullable final Integer requestId,
                           @Nonnull final Runnable action )
  {
    ReplicationRequestUtil.runRequest( getRegistry(),
                                       getEntityManager(),
                                       getEntityMessageEndpoint(),
                                       invocationKey,
                                       session,
                                       requestId,
                                       action );
  }

  @Nonnull
  private String getInvocationKey( final int channelId,
                                   @Nullable final Integer subChannelId,
                                   @Nonnull final String action )
  {
    final SystemMetaData systemMetaData = getSessionManager().getSystemMetaData();
    return systemMetaData.getName() + "." + action + systemMetaData.getChannelMetaData( channelId ).getName() +
           ( ( null == subChannelId ) ? "" : "." + subChannelId );
  }
}
