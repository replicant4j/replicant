package org.realityforge.replicant.server.ee.rest;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.json.stream.JsonGenerator;
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
import org.realityforge.replicant.server.ChannelDescriptor;
import org.realityforge.replicant.server.ee.JsonUtil;
import org.realityforge.replicant.server.transport.ChannelMetaData;
import org.realityforge.replicant.server.transport.Packet;
import org.realityforge.replicant.server.transport.PacketQueue;
import org.realityforge.replicant.server.transport.ReplicantSession;
import org.realityforge.replicant.server.transport.ReplicantSessionManager;
import org.realityforge.replicant.server.transport.SubscriptionEntry;
import org.realityforge.replicant.shared.transport.ReplicantContext;
import org.realityforge.rest.field_filter.FieldFilter;
import org.realityforge.ssf.SessionInfo;

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
  protected abstract ReplicantSessionManager getSessionManager();

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

  @Nonnull
  protected Response doCreateSession()
  {
    return buildResponse( Response.ok(), getSessionManager().createSession().getSessionID() );
  }

  @Nonnull
  protected Response doDeleteSession( @Nonnull final String sessionID )
  {
    final StringWriter writer = new StringWriter();
    final JsonGenerator g = factory().createGenerator( writer );
    g.writeStartObject();
    g.write( "code", Response.Status.OK.getStatusCode() );
    g.write( "description", "Session removed." );
    g.writeEnd();
    g.close();

    getSessionManager().invalidateSession( sessionID );
    return buildResponse( Response.ok(), writer.toString() );
  }

  @Nonnull
  protected Response doListSessions( @Nonnull final FieldFilter filter,
                                     @Nonnull final UriInfo uri )
  {
    final StringWriter writer = new StringWriter();
    final JsonGenerator g = factory().createGenerator( writer );
    final Set<String> sessionIDs = getSessionManager().getSessionIDs();
    g.writeStartArray();
    for ( final String sessionID : sessionIDs )
    {
      final ReplicantSession session = getSessionManager().getSession( sessionID );
      if ( null != session )
      {
        emitSession( session, g, filter, uri );
      }
    }
    g.writeEnd();
    g.close();

    return buildResponse( Response.ok(), writer.toString() );
  }

  @Nonnull
  protected Response doGetSession( @Nonnull final String sessionID,
                                   @Nonnull final FieldFilter filter,
                                   @Nonnull final UriInfo uri )
  {
    final StringWriter writer = new StringWriter();
    final JsonGenerator g = factory().createGenerator( writer );

    final ReplicantSession session = getSessionManager().getSession( sessionID );
    final Response.ResponseBuilder builder;
    if ( null == session )
    {
      builder = Response.status( Response.Status.NOT_FOUND );

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

    g.close();

    return buildResponse( builder, writer.toString() );
  }

  private void emitSession( @Nonnull final ReplicantSession session,
                            @Nonnull final JsonGenerator g,
                            @Nonnull final FieldFilter filter,
                            @Nonnull final UriInfo uri )
  {
    g.writeStartObject();
    if ( filter.allow( "id" ) )
    {
      g.write( "id", session.getSessionID() );
    }
    if ( filter.allow( "userID" ) )
    {
      final String userID = session.getUserID();
      if ( null == userID )
      {
        g.writeNull( "userID" );
      }
      else
      {
        g.write( "userID", userID );
      }
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
      emitSubscriptions( g, session.getSubscriptions().values(), subFilter );
      g.writeEnd();
    }
    g.writeEnd();
  }

  @Nonnull
  private String getSessionURL( @Nonnull final SessionInfo session, @Nonnull final UriInfo uri )
  {
    return uri.getBaseUri() + ReplicantContext.SESSION_URL_FRAGMENT.substring( 1 ) + "/" + session.getSessionID();
  }

  @Nonnull
  private String asDateTimeString( final long timeInMillis )
  {
    final GregorianCalendar calendar = new GregorianCalendar();
    calendar.setTimeInMillis( timeInMillis );
    return datatypeFactory().newXMLGregorianCalendar( calendar ).toXMLFormat();
  }


  private void emitSubscriptions( @Nonnull final JsonGenerator g,
                                  @Nonnull final Collection<SubscriptionEntry> subscriptionEntries,
                                  @Nonnull final FieldFilter filter )
  {
    if ( filter.allow( "subscriptions" ) )
    {
      g.writeStartArray( "subscriptions" );
      final FieldFilter subFilter = filter.subFilter( "subscriptions" );

      final ArrayList<SubscriptionEntry> subscriptions = new ArrayList<>( subscriptionEntries );
      Collections.sort( subscriptions );

      for ( final SubscriptionEntry subscription : subscriptions )
      {
        emitSubscriptionEntry( g, subscription, subFilter );
      }
      g.writeEnd();
    }
  }

  private void emitChannelID( @Nonnull final JsonGenerator g,
                              @Nonnull final FieldFilter filter,
                              final int channelID )
  {
    if ( filter.allow( "channelID" ) )
    {
      g.write( "channelID", channelID );
    }
  }

  private void emitSubChannelID( @Nonnull final JsonGenerator g,
                                 @Nonnull final FieldFilter filter,
                                 final Serializable subChannelID )
  {
    if ( null != subChannelID && filter.allow( "instanceID" ) )
    {
      if ( subChannelID instanceof Integer )
      {
        g.write( "instanceID", (Integer) subChannelID );
      }
      else
      {
        g.write( "instanceID", String.valueOf( subChannelID ) );
      }
    }
  }

  private void emitChannelDescriptors( @Nonnull final JsonGenerator g,
                                       @Nonnull final Set<ChannelDescriptor> descriptors,
                                       @Nonnull final FieldFilter filter )
  {
    for ( final ChannelDescriptor descriptor : descriptors )
    {
      g.writeStartObject();
      emitChannelDescriptor( g, filter, descriptor );
      g.writeEnd();
    }
  }

  private void emitSubscriptionEntry( @Nonnull final JsonGenerator g,
                                      @Nonnull final SubscriptionEntry entry,
                                      @Nonnull final FieldFilter filter )
  {
    if ( filter.allow( "subscription" ) )
    {
      final FieldFilter subFilter = filter.subFilter( "subscription" );
      g.writeStartObject();
      final ChannelMetaData channelMetaData = getSessionManager().getChannelMetaData( entry.getDescriptor() );
      if ( subFilter.allow( "name" ) )
      {
        g.write( "name", channelMetaData.getName() );
      }
      emitChannelDescriptor( g, subFilter, entry.getDescriptor() );
      if ( subFilter.allow( "explicitlySubscribed" ) )
      {
        g.write( "explicitlySubscribed", entry.isExplicitlySubscribed() );
      }
      if ( channelMetaData.getFilterType() != ChannelMetaData.FilterType.NONE && subFilter.allow( "filter" ) )
      {
        final Object f = entry.getFilter();
        if ( null == f )
        {
          g.writeNull( "filter" );
        }
        else
        {
          g.write( "filter", JsonUtil.toJsonObject( f ) );
        }
      }

      emitChannelDescriptors( g,
                              "inwardSubscriptions",
                              entry.getInwardSubscriptions(),
                              subFilter );
      emitChannelDescriptors( g,
                              "outwardSubscriptions",
                              entry.getOutwardSubscriptions(),
                              subFilter );
      g.writeEnd();
    }
  }

  private void emitChannelDescriptor( @Nonnull final JsonGenerator g,
                                      @Nonnull final FieldFilter filter,
                                      @Nonnull final ChannelDescriptor descriptor )
  {
    emitChannelID( g, filter, descriptor.getChannelID() );
    emitSubChannelID( g, filter, descriptor.getSubChannelID() );
  }

  private void emitChannelDescriptors( @Nonnull final JsonGenerator g,
                                       @Nonnull final String key,
                                       @Nonnull final Set<ChannelDescriptor> descriptors,
                                       @Nonnull final FieldFilter filter )
  {
    if ( !descriptors.isEmpty() && filter.allow( key ) )
    {
      final FieldFilter subFilter = filter.subFilter( key );
      g.writeStartArray( key );
      emitChannelDescriptors( g, descriptors, subFilter );
      g.writeEnd();
    }
  }
}
