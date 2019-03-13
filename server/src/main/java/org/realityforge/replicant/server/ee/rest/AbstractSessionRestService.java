package org.realityforge.replicant.server.ee.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
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

  @Path( "{sessionId}" )
  @DELETE
  public Response deleteSession( @PathParam( "sessionId" ) @NotNull final String sessionId )
  {
    return doDeleteSession( sessionId );
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

  @Path( "{sessionId}" + SharedConstants.PING_URL_FRAGMENT )
  @GET
  public Response ping( @PathParam( "sessionId" ) @NotNull final String sessionId,
                        @HeaderParam( SharedConstants.REQUEST_ID_HEADER ) @Nullable final Integer requestId,
                        @Context @Nonnull final UriInfo uri )
  {
    runRequest( "Ping", sessionId, requestId, () -> {
      ensureSession( sessionId, requestId );
      // The next line forces the creation of per session changes which means a message will return to the client?
      EntityMessageCacheUtil.getSessionChanges().setPingResponse( true );
    } );
    return standardResponse( Response.Status.OK, "Pong" );
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

  @Path( "{sessionId}" + SharedConstants.CHANNEL_URL_FRAGMENT + "/{channelId:\\d+}" )
  @DELETE
  public Response unsubscribeFromChannel( @PathParam( "sessionId" ) @NotNull final String sessionId,
                                          @PathParam( "channelId" ) final int channelId,
                                          @HeaderParam( SharedConstants.REQUEST_ID_HEADER ) @Nullable final Integer requestId,
                                          @QueryParam( SharedConstants.SUB_CHANNEL_ID_PARAM ) @Nullable final String subChannelIds )
  {
    final ChannelMetaData channelMetaData = getChannelMetaData( channelId );
    if ( !channelMetaData.isExternal() )
    {
      return standardResponse( Response.Status.BAD_REQUEST, "Attempted to unsubscribe from internal-only channel" );
    }
    if ( null == subChannelIds )
    {
      //Handle type channel
      return doUnsubscribeChannel( sessionId, requestId, toChannelDescriptor( channelId ) );
    }
    else
    {
      if ( channelMetaData.isTypeGraph() )
      {
        final Response response =
          standardResponse( Response.Status.BAD_REQUEST, "Supplied subChannelIds to type graph" );
        throw new WebApplicationException( response );
      }
      //A bulk subscribe to an instance channel
      final ArrayList<Integer> scids = new ArrayList<>();
      for ( final String scid : subChannelIds.split( "," ) )
      {
        assert channelMetaData.isInstanceGraph();
        scids.add( Integer.parseInt( scid ) );
      }
      return doBulkUnsubscribeChannel( sessionId, requestId, channelId, scids );
    }
  }

  @Path( "{sessionId}" + SharedConstants.CHANNEL_URL_FRAGMENT + "/{channelId:\\d+}.{subChannelId:\\d+}" )
  @DELETE
  public Response unsubscribeFromInstanceChannel( @PathParam( "sessionId" ) @NotNull final String sessionId,
                                                  @PathParam( "channelId" ) final int channelId,
                                                  @PathParam( "subChannelId" ) @NotNull final int subChannelId,
                                                  @HeaderParam( SharedConstants.REQUEST_ID_HEADER ) @Nullable final Integer requestId )
  {
    final ChannelMetaData channelMetaData = getChannelMetaData( channelId );
    if ( !channelMetaData.isExternal() )
    {
      return standardResponse( Response.Status.BAD_REQUEST, "Attempted to unsubscribe from internal-only channel" );
    }
    else if ( channelMetaData.isTypeGraph() )
    {
      final Response response =
        standardResponse( Response.Status.BAD_REQUEST, "Supplied subChannelIds to type graph" );
      throw new WebApplicationException( response );
    }

    return doUnsubscribeChannel( sessionId, requestId, new ChannelAddress( channelId, subChannelId ) );
  }

  @Path( "{sessionId}" + SharedConstants.CHANNEL_URL_FRAGMENT + "/{channelId:\\d+}" )
  @PUT
  public Response subscribeToChannel( @PathParam( "sessionId" ) @NotNull final String sessionId,
                                      @PathParam( "channelId" ) @NotNull final int channelId,
                                      @HeaderParam( SharedConstants.REQUEST_ID_HEADER ) @Nullable final Integer requestId,
                                      @HeaderParam( SharedConstants.ETAG_HEADER ) @Nullable final String eTag,
                                      @QueryParam( SharedConstants.SUB_CHANNEL_ID_PARAM ) @Nullable final String subChannelIds,
                                      @Nonnull final String filterContent )
  {
    final ChannelMetaData channelMetaData = getChannelMetaData( channelId );
    if ( !channelMetaData.isExternal() )
    {
      return standardResponse( Response.Status.BAD_REQUEST, "Attempted to subscribe to internal-only channel" );
    }
    if ( null == subChannelIds )
    {
      //Subscription to a type channel
      return doSubscribeChannel( sessionId, requestId, eTag, toChannelDescriptor( channelId ), filterContent );
    }
    else
    {
      if ( channelMetaData.isTypeGraph() )
      {
        final Response response =
          standardResponse( Response.Status.BAD_REQUEST, "Supplied subChannelIds to type graph" );
        throw new WebApplicationException( response );
      }
      //A bulk subscribe to an instance channel
      final ArrayList<Integer> scids = new ArrayList<>();
      for ( final String scid : subChannelIds.split( "," ) )
      {
        assert channelMetaData.isInstanceGraph();
        scids.add( Integer.parseInt( scid ) );
      }
      return doBulkSubscribeChannel( sessionId, requestId, channelId, scids, filterContent );
    }
  }

  @Path( "{sessionId}" + SharedConstants.CHANNEL_URL_FRAGMENT + "/{channelId:\\d+}.{subChannelId:\\d+}" )
  @PUT
  public Response subscribeToInstanceChannel( @PathParam( "sessionId" ) @NotNull final String sessionId,
                                              @PathParam( "channelId" ) @NotNull final int channelId,
                                              @PathParam( "subChannelId" ) @NotNull final int subChannelId,
                                              @HeaderParam( SharedConstants.REQUEST_ID_HEADER ) @Nullable final Integer requestId,
                                              @HeaderParam( SharedConstants.ETAG_HEADER ) @Nullable final String eTag,
                                              @Nonnull final String filterContent )
  {
    final ChannelMetaData channelMetaData = getChannelMetaData( channelId );
    if ( !channelMetaData.isExternal() )
    {
      return standardResponse( Response.Status.NOT_FOUND, "Attempted to subscribe to internal-only channel" );
    }
    else if ( !channelMetaData.isInstanceGraph() )
    {
      return standardResponse( Response.Status.NOT_FOUND, "Attempted to subscribe to type channel with instance data" );
    }

    return doSubscribeChannel( sessionId,
                               requestId,
                               eTag,
                               new ChannelAddress( channelId, subChannelId ),
                               filterContent );
  }

  @Nonnull
  protected Response doSubscribeChannel( @Nonnull final String sessionId,
                                         @Nullable final Integer requestId,
                                         @Nullable final String eTag,
                                         @Nonnull final ChannelAddress address,
                                         @Nonnull final String filterContent )
  {
    final Supplier<ReplicantSessionManager.CacheStatus> action = () ->
    {
      final ReplicantSession session = ensureSession( sessionId, requestId );
      session.setETag( address, eTag );
      return getSessionManager().subscribe( session,
                                            address,
                                            toFilter( getChannelMetaData( address ), filterContent ),
                                            EntityMessageCacheUtil.getSessionChanges() );
    };

    final String invocationKey =
      getInvocationKey( address.getChannelId(), address.getSubChannelId(), "Subscribe" );
    final ReplicantSessionManager.CacheStatus cacheStatus = runRequest( invocationKey, sessionId, requestId, action );
    final Response.Status status =
      cacheStatus == ReplicantSessionManager.CacheStatus.USE ? Response.Status.NO_CONTENT : Response.Status.OK;
    return standardResponse( status, "Channel subscription added." );
  }

  @Nonnull
  protected Response doBulkSubscribeChannel( @Nonnull final String sessionId,
                                             @Nullable final Integer requestId,
                                             final int channelId,
                                             @Nonnull Collection<Integer> subChannelIds,
                                             @Nonnull final String filterContent )
  {
    final Runnable action = () ->
    {
      final ReplicantSession session = ensureSession( sessionId, requestId );
      final Object filter = toFilter( getChannelMetaData( channelId ), filterContent );
      getSessionManager().bulkSubscribe( session,
                                         channelId,
                                         subChannelIds,
                                         filter,
                                         true,
                                         EntityMessageCacheUtil.getSessionChanges() );
    };
    runRequest( getInvocationKey( channelId, null, "BulkSubscribe" ), sessionId, requestId, action );

    return standardResponse( Response.Status.OK, "Channel subscriptions added." );
  }

  @Nonnull
  protected Response doBulkUnsubscribeChannel( @Nonnull final String sessionId,
                                               @Nullable final Integer requestId,
                                               final int channelId,
                                               @Nonnull final Collection<Integer> subChannelIds )
  {
    final Runnable action = () ->
      getSessionManager().bulkUnsubscribe( ensureSession( sessionId, requestId ),
                                           channelId,
                                           subChannelIds,
                                           true,
                                           EntityMessageCacheUtil.getSessionChanges() );
    runRequest( getInvocationKey( channelId, null, "BulkUnsubscribe" ), sessionId, requestId, action );
    return standardResponse( Response.Status.OK, "Channel subscriptions removed." );
  }

  @Nonnull
  protected Response doUnsubscribeChannel( @Nonnull final String sessionId,
                                           @Nullable final Integer requestId,
                                           @Nonnull final ChannelAddress address )
  {
    final Runnable action = () ->
      getSessionManager().unsubscribe( ensureSession( sessionId, requestId ),
                                       address,
                                       true,
                                       EntityMessageCacheUtil.getSessionChanges() );
    runRequest( getInvocationKey( address.getChannelId(), address.getSubChannelId(), "Unsubscribe" ),
                sessionId,
                requestId,
                action );
    return standardResponse( Response.Status.OK, "Channel subscription removed." );
  }

  @Nonnull
  protected Response doGetChannel( @Nonnull final String sessionId,
                                   @Nonnull final ChannelAddress address,
                                   @Nonnull final UriInfo uri )
  {
    final ReplicantSession session = ensureSession( sessionId, null );
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
    final ReplicantSession session = ensureSession( sessionId, null );
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
    final ReplicantSession session = ensureSession( sessionId, null );
    final String content =
      json( g -> Encoder.emitInstanceChannelList( systemMetaData, channelId, session, g, uri ) );
    return buildResponse( Response.ok(), content );
  }

  @Nonnull
  protected Response doCreateSession()
  {
    return buildResponse( Response.ok(), getSessionManager().createSession().getSessionID() );
  }

  @Nonnull
  protected Response doDeleteSession( @Nonnull final String sessionId )
  {
    getSessionManager().invalidateSession( sessionId );
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
    final ReplicantSession session = ensureSession( sessionId, null );
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
  private ChannelMetaData getChannelMetaData( @Nonnull final ChannelAddress address )
  {
    return getSystemMetaData().getChannelMetaData( address );
  }

  @Nullable
  private Object toFilter( @Nonnull final ChannelMetaData channelMetaData, @Nonnull final String filterContent )
  {
    return channelMetaData.hasFilterParameter() ? parseFilter( channelMetaData, filterContent ) : null;
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
  private ReplicantSession ensureSession( @Nonnull final String sessionId, @Nullable final Integer requestId )
  {
    final ReplicantSession session = getSessionManager().getSession( sessionId );
    if ( null == session )
    {
      throw new WebApplicationException( standardResponse( Response.Status.NOT_FOUND, "No such session." ) );
    }
    getRegistry().putResource( ServerConstants.SESSION_ID_KEY, sessionId );
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
                                                          @Nonnull final String sessionId,
                                                          @Nullable final Integer requestId,
                                                          @Nonnull final Supplier<ReplicantSessionManager.CacheStatus> action )
  {
    return ReplicationRequestUtil.runRequest( getRegistry(),
                                              getEntityManager(),
                                              getEntityMessageEndpoint(),
                                              invocationKey,
                                              sessionId,
                                              requestId,
                                              action );
  }

  private void runRequest( @Nonnull final String invocationKey,
                           @Nonnull final String sessionId,
                           @Nullable final Integer requestId,
                           @Nonnull final Runnable action )
  {
    ReplicationRequestUtil.runRequest( getRegistry(),
                                       getEntityManager(),
                                       getEntityMessageEndpoint(),
                                       invocationKey,
                                       sessionId,
                                       requestId,
                                       action );
  }

  @Nonnull
  private String getInvocationKey( final int channelId,
                                   @Nullable final Integer subChannelId,
                                   @Nonnull final String action )
  {
    return getSystemMetaData().getName() + "." + action + getChannelMetaData( channelId ).getName() +
           ( ( null == subChannelId ) ? "" : "." + subChannelId );
  }

  @Nonnull
  private SystemMetaData getSystemMetaData()
  {
    return getSessionManager().getSystemMetaData();
  }
}
