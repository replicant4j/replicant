package org.realityforge.replicant.server.ee.rest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.core.UriInfo;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import org.realityforge.replicant.server.ChannelDescriptor;
import org.realityforge.replicant.server.transport.ChannelMetaData;
import org.realityforge.replicant.server.transport.Packet;
import org.realityforge.replicant.server.transport.PacketQueue;
import org.realityforge.replicant.server.transport.ReplicantSession;
import org.realityforge.replicant.server.transport.ReplicantSessionManager;
import org.realityforge.replicant.server.transport.SubscriptionEntry;
import org.realityforge.replicant.shared.ee.JsonUtil;
import org.realityforge.replicant.shared.transport.ReplicantContext;
import org.realityforge.rest.field_filter.FieldFilter;
import org.realityforge.ssf.SessionInfo;

final class Encoder
{
  private static final DatatypeFactory c_datatypeFactory;

  static
  {
    try
    {
      c_datatypeFactory = DatatypeFactory.newInstance();
    }
    catch ( final DatatypeConfigurationException dtce )
    {
      throw new IllegalStateException( "Unable to initialize DatatypeFactory", dtce );
    }
  }

  private Encoder()
  {
  }

  static void emitSession( @Nonnull final ReplicantSessionManager sessionManager,
                           @Nonnull final ReplicantSession session,
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

    if ( filter.allow( "channels" ) )
    {
      final FieldFilter subFilter = filter.subFilter( "channels" );
      g.writeStartObject( "channels" );
      emitChannels( sessionManager, session, g, uri, subFilter );
      g.writeEnd();
    }
    g.writeEnd();
  }

  static void emitChannelsList( @Nonnull final ReplicantSessionManager sessionManager,
                                @Nonnull final ReplicantSession session,
                                @Nonnull final JsonGenerator g,
                                @Nonnull final FieldFilter filter,
                                @Nonnull final UriInfo uri )
  {
    g.writeStartObject();
    emitChannels( sessionManager, session, g, uri, filter );
    g.writeEnd();
  }

  private static void emitChannels( @Nonnull final ReplicantSessionManager sessionManager,
                                    @Nonnull final ReplicantSession session,
                                    @Nonnull final JsonGenerator g,
                                    @Nonnull final UriInfo uri,
                                    @Nonnull final FieldFilter subFilter )
  {
    if ( subFilter.allow( "url" ) )
    {
      g.write( "url", getSubscriptionsURL( session, uri ) );
    }
    emitSubscriptions( sessionManager, session, g, session.getSubscriptions().values(), subFilter, uri );
  }

  @Nonnull
  private static String getSessionURL( @Nonnull final SessionInfo session, @Nonnull final UriInfo uri )
  {
    return uri.getBaseUri() + ReplicantContext.SESSION_URL_FRAGMENT.substring( 1 ) + "/" + session.getSessionID();
  }

  @Nonnull
  private static String getSubscriptionsURL( @Nonnull final SessionInfo session, @Nonnull final UriInfo uri )
  {
    return getSessionURL( session, uri ) + ReplicantContext.CHANNEL_URL_FRAGMENT;
  }

  @Nonnull
  private static String getChannelURL( @Nonnull final SessionInfo session,
                                       @Nonnull final ChannelDescriptor descriptor,
                                       @Nonnull final UriInfo uri )
  {
    final String baseURL = getSubscriptionsURL( session, uri ) + '/' + descriptor.getChannelID();
    if ( null != descriptor.getSubChannelID() )
    {
      return baseURL + '.' + descriptor.getSubChannelID();
    }
    else
    {
      return baseURL;
    }
  }

  @Nonnull
  private static String asDateTimeString( final long timeInMillis )
  {
    final GregorianCalendar calendar = new GregorianCalendar();
    calendar.setTimeInMillis( timeInMillis );
    return c_datatypeFactory.newXMLGregorianCalendar( calendar ).toXMLFormat();
  }

  private static void emitSubscriptions( @Nonnull final ReplicantSessionManager sessionManager,
                                         @Nonnull final ReplicantSession session,
                                         @Nonnull final JsonGenerator g,
                                         @Nonnull final Collection<SubscriptionEntry> subscriptionEntries,
                                         @Nonnull final FieldFilter filter,
                                         @Nonnull final UriInfo uri )
  {
    if ( filter.allow( "subscriptions" ) )
    {
      g.writeStartArray( "subscriptions" );
      final FieldFilter subFilter = filter.subFilter( "subscriptions" );

      final ArrayList<SubscriptionEntry> subscriptions = new ArrayList<>( subscriptionEntries );
      Collections.sort( subscriptions );

      for ( final SubscriptionEntry subscription : subscriptions )
      {
        if ( subFilter.allow( "channel" ) )
        {
          final FieldFilter subFilter1 = subFilter.subFilter( "channel" );
          emitChannel( sessionManager, session, g, subscription, subFilter1, uri );
        }
      }
      g.writeEnd();
    }
  }

  private static void emitChannelID( @Nonnull final JsonGenerator g,
                                     @Nonnull final FieldFilter filter,
                                     final int channelID )
  {
    if ( filter.allow( "channelID" ) )
    {
      g.write( "channelID", channelID );
    }
  }

  private static void emitSubChannelID( @Nonnull final JsonGenerator g,
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

  private static void emitChannelDescriptors( @Nonnull final JsonGenerator g,
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

  static void emitChannel( @Nonnull final ReplicantSessionManager sessionManager,
                           @Nonnull final ReplicantSession session,
                           @Nonnull final JsonGenerator g,
                           @Nonnull final SubscriptionEntry entry,
                           @Nonnull final FieldFilter filter,
                           @Nonnull final UriInfo uri )
  {
    g.writeStartObject();
    final ChannelMetaData channelMetaData = sessionManager.getChannelMetaData( entry.getDescriptor() );
    if ( filter.allow( "name" ) )
    {
      g.write( "name", channelMetaData.getName() );
    }
    if ( filter.allow( "url" ) )
    {
      g.write( "url", getChannelURL( session, entry.getDescriptor(), uri ) );
    }
    emitChannelDescriptor( g, filter, entry.getDescriptor() );
    if ( filter.allow( "explicitlySubscribed" ) )
    {
      g.write( "explicitlySubscribed", entry.isExplicitlySubscribed() );
    }
    if ( channelMetaData.getFilterType() != ChannelMetaData.FilterType.NONE && filter.allow( "filter" ) )
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
                            filter );
    emitChannelDescriptors( g,
                            "outwardSubscriptions",
                            entry.getOutwardSubscriptions(),
                            filter );
    g.writeEnd();
  }

  private static void emitChannelDescriptor( @Nonnull final JsonGenerator g,
                                             @Nonnull final FieldFilter filter,
                                             @Nonnull final ChannelDescriptor descriptor )
  {
    emitChannelID( g, filter, descriptor.getChannelID() );
    emitSubChannelID( g, filter, descriptor.getSubChannelID() );
  }

  private static void emitChannelDescriptors( @Nonnull final JsonGenerator g,
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
