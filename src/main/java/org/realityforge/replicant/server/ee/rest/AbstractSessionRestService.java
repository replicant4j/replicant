package org.realityforge.replicant.server.ee.rest;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import org.realityforge.replicant.server.transport.Packet;
import org.realityforge.replicant.server.transport.PacketQueue;
import org.realityforge.replicant.server.transport.ReplicantSession;
import org.realityforge.replicant.shared.transport.ReplicantContext;
import org.realityforge.rest.field_filter.FieldFilter;
import org.realityforge.ssf.SessionInfo;
import org.realityforge.ssf.SessionManager;

/**
 * The session management rest resource.
 *
 * It is expected that this endpoint has already had security applied.
 *
 * Extend this class and provider a SessionManager as required. ie.
 *
 * <pre>
 * @Path( ReplicantContext.SESSION_URL_FRAGMENT )
 * @Produces( MediaType.APPLICATION_JSON )
 * @ApplicationScoped
 * public class CalendarSessionRestService
 *   extends AbstractSessionRestService
 * {
 *   @Inject
 *   private SessionManager<MySession> _sessionManager;
 *
 *   @Override
 *   protected SessionManager getSessionManager()
 *   {
 *     return _sessionManager;
 *   }
 *
 *   @Override
 *   @PostConstruct
 *   public void postConstruct()
 *   {
 *     super.postConstruct();
 *   }
 * }
 * </pre>
 */
public abstract class AbstractSessionRestService<T extends ReplicantSession>
{
  private DatatypeFactory _datatypeFactory;
  private JsonGeneratorFactory _factory;

  protected abstract SessionManager<T> getSessionManager();

  public void postConstruct()
  {
    final HashMap<String, Object> config = new HashMap<>();
    config.put( JsonGenerator.PRETTY_PRINTING, true );
    _factory = Json.createGeneratorFactory( config );

    try
    {
      _datatypeFactory = DatatypeFactory.newInstance();
    }
    catch ( final DatatypeConfigurationException dtce )
    {
      throw new IllegalStateException( "Unable to initialize DatatypeFactory", dtce );
    }
  }

  @POST
  @Produces( MediaType.TEXT_PLAIN )
  public Response createSession()
  {
    final Response.ResponseBuilder builder = Response.ok();
    CacheUtil.configureNoCacheHeaders( builder );
    return builder.entity( getSessionManager().createSession().getSessionID() ).build();
  }

  @Path( "{sessionID}" )
  @DELETE
  public Response deleteSession( @PathParam( "sessionID" ) @NotNull final String sessionID )
  {
    final StringWriter writer = new StringWriter();
    final JsonGenerator g = _factory.createGenerator( writer );
    g.writeStartObject();
    g.write( "code", Response.Status.OK.getStatusCode() );
    g.write( "description", "Session removed." );
    g.writeEnd();
    g.close();

    final Response.ResponseBuilder builder = Response.ok();
    CacheUtil.configureNoCacheHeaders( builder );
    getSessionManager().invalidateSession( sessionID );
    return builder.entity( writer.toString() ).build();
  }

  @GET
  public Response listSessions( @QueryParam( "fields" ) @DefaultValue( "url" ) @Nonnull final FieldFilter filter,
                                @Context @Nonnull final UriInfo uri )
  {
    final Response.ResponseBuilder builder = Response.ok();
    CacheUtil.configureNoCacheHeaders( builder );

    final StringWriter writer = new StringWriter();
    final JsonGenerator g = _factory.createGenerator( writer );
    final Set<String> sessionIDs = getSessionManager().getSessionIDs();
    g.writeStartArray();
    for ( final String sessionID : sessionIDs )
    {
      final ReplicantSession session = (ReplicantSession) getSessionManager().getSession( sessionID );
      if ( null != session )
      {
        emitSession( session, g, filter, uri );
      }
    }
    g.writeEnd();
    g.close();

    return builder.entity( writer.toString() ).build();
  }

  @Path( "{sessionID}" )
  @GET
  public Response getSession( @PathParam( "sessionID" ) @NotNull final String sessionID,
                              @QueryParam( "fields" ) @DefaultValue( "" ) @Nonnull final FieldFilter filter,
                              @Context @Nonnull final UriInfo uri )
  {
    final StringWriter writer = new StringWriter();
    final JsonGenerator g = _factory.createGenerator( writer );

    final ReplicantSession session = getSessionManager().getSession( sessionID );
    final Response.ResponseBuilder builder;
    if ( null == session )
    {
      builder = Response.status( Response.Status.NOT_FOUND );
      CacheUtil.configureNoCacheHeaders( builder );

      g.writeStartObject();
      g.write( "code", Response.Status.NOT_FOUND.getStatusCode() );
      g.write( "description", "No such session." );
      g.writeEnd();
    }
    else
    {
      builder = Response.ok();
      emitSession( session, g, filter, uri );
    }

    CacheUtil.configureNoCacheHeaders( builder );
    g.close();
    return builder.entity( writer.toString() ).build();
  }

  void emitSession( @Nonnull final ReplicantSession session,
                    @Nonnull final JsonGenerator g,
                    @Nonnull final FieldFilter filter,
                    @Nonnull final UriInfo uri )
  {
    g.writeStartObject();
    if ( filter.allow( "id" ) )
    {
      g.write( "id", session.getSessionID() );
    }
    if ( filter.allow( "url" ) )
    {
      g.write( "url", getSessionURL( session, uri ) );
    }
    if ( filter.allow( "createdAt" ) )
    {
      g.write( "createdAt", asDateTimeString( session.getCreatedAt() ) );
    }
    if ( filter.allow( "lastAccessedAt" ) )
    {
      g.write( "lastAccessedAt", asDateTimeString( session.getLastAccessedAt() ) );
    }
    if ( filter.allow( "attributes" ) )
    {
      final FieldFilter subFilter = filter.subFilter( "attributes" );
      g.writeStartObject( "attributes" );

      for ( final String attributeKey : session.getAttributeKeys() )
      {
        if ( subFilter.allow( attributeKey ) )
        {
          final Serializable attribute = session.getAttribute( attributeKey );
          if ( null == attribute )
          {
            g.writeNull( attributeKey );
          }
          else
          {
            g.write( attributeKey, String.valueOf( attribute ) );
          }
        }
      }
      g.writeEnd();
    }
    if ( filter.allow( "net" ) )
    {
      final FieldFilter subFilter = filter.subFilter( "net" );
      g.writeStartObject( "net" );
      final PacketQueue queue = session.getQueue();
      if ( subFilter.allow( "queueSize" ) )
      {
        g.write( "queueSize", queue.size() );
      }
      if ( subFilter.allow( "lastSequenceAcked" ) )
      {
        g.write( "lastSequenceAcked", queue.getLastSequenceAcked() );
      }
      if ( subFilter.allow( "nextPacketSequence" ) )
      {
        final Packet nextPacket = queue.nextPacketToProcess();
        if ( null != nextPacket )
        {
          g.write( "nextPacketSequence", nextPacket.getSequence() );
        }
        else
        {
          g.writeNull( "nextPacketSequence" );
        }
      }
      g.writeEnd();
    }

    if ( filter.allow( "status" ) )
    {
      final FieldFilter subFilter = filter.subFilter( "status" );
      g.writeStartObject( "status" );
      session.emitStatus( g, subFilter );
      g.writeEnd();
    }
    g.writeEnd();
  }

  private String getSessionURL( @Nonnull final SessionInfo session, @Nonnull final UriInfo uri )
  {
    return uri.getBaseUri() + ReplicantContext.SESSION_URL_FRAGMENT.substring( 1 ) + "/" + session.getSessionID();
  }

  @Nonnull
  private String asDateTimeString( final long timeInMillis )
  {
    final GregorianCalendar calendar = new GregorianCalendar();
    calendar.setTimeInMillis( timeInMillis );
    return _datatypeFactory.newXMLGregorianCalendar( calendar ).toXMLFormat();
  }
}
