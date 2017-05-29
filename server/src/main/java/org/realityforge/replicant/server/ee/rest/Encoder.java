package org.realityforge.replicant.server.ee.rest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.core.UriInfo;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import org.realityforge.replicant.server.ChannelDescriptor;
import org.realityforge.replicant.server.transport.ChannelMetaData;
import org.realityforge.replicant.server.transport.Packet;
import org.realityforge.replicant.server.transport.PacketQueue;
import org.realityforge.replicant.server.transport.ReplicantSession;
import org.realityforge.replicant.server.transport.SessionInfo;
import org.realityforge.replicant.server.transport.SubscriptionEntry;
import org.realityforge.replicant.server.transport.SystemMetaData;
import org.realityforge.replicant.shared.ee.JsonUtil;
import org.realityforge.replicant.shared.transport.ReplicantContext;

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

  static void emitSession( @Nonnull final SystemMetaData systemMetaData,
                           @Nonnull final ReplicantSession session,
                           @Nonnull final JsonGenerator g,
                           @Nonnull final UriInfo uri,
                           final boolean emitNetworkData )
  {
    final PacketQueue queue = session.getQueue();
    g.writeStartObject();
    g.write( "id", session.getSessionID() );
    final String userID = session.getUserID();
    if ( null == userID )
    {
      g.writeNull( "userID" );
    }
    else
    {
      g.write( "userID", userID );
    }
    g.write( "url", getSessionURL( session, uri ) );
    g.write( "createdAt", asDateTimeString( session.getCreatedAt() ) );
    g.write( "lastAccessedAt", asDateTimeString( session.getLastAccessedAt() ) );
    g.write( "synchronized", queue.getLastSequenceAcked() == queue.size() );
    g.writeStartObject( "attributes" );

    for ( final String attributeKey : session.getAttributeKeys() )
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
    g.writeEnd();

    if ( emitNetworkData )
    {
      g.writeStartObject( "net" );
      g.write( "queueSize", queue.size() );
      g.write( "lastSequenceAcked", queue.getLastSequenceAcked() );
      final Packet nextPacket = queue.nextPacketToProcess();
      if ( null != nextPacket )
      {
        g.write( "nextPacketSequence", nextPacket.getSequence() );
      }
      else
      {
        g.writeNull( "nextPacketSequence" );
      }
      g.writeEnd();

      g.writeStartObject( "channels" );
      emitChannels( systemMetaData, session, g, uri );
      g.writeEnd();
    }

    g.writeEnd();
  }

  static void emitChannelsList( @Nonnull final SystemMetaData systemMetaData,
                                @Nonnull final ReplicantSession session,
                                @Nonnull final JsonGenerator g,
                                @Nonnull final UriInfo uri )
  {
    g.writeStartObject();
    emitChannels( systemMetaData, session, g, uri );
    g.writeEnd();
  }

  private static void emitChannels( @Nonnull final SystemMetaData systemMetaData,
                                    @Nonnull final ReplicantSession session,
                                    @Nonnull final JsonGenerator g,
                                    @Nonnull final UriInfo uri )
  {
    g.write( "url", getSubscriptionsURL( session, uri ) );
    emitSubscriptions( systemMetaData, session, g, session.getSubscriptions().values(), uri );
  }

  static void emitInstanceChannelList( @Nonnull final SystemMetaData systemMetaData,
                                       final int channeID,
                                       @Nonnull final ReplicantSession session,
                                       @Nonnull final JsonGenerator g,
                                       @Nonnull final UriInfo uri )
  {
    g.writeStartObject();
    emitInstanceChannels( systemMetaData, channeID, session, g, uri );
    g.writeEnd();
  }

  private static void emitInstanceChannels( @Nonnull final SystemMetaData systemMetaData,
                                            final int channeID,
                                            @Nonnull final ReplicantSession session,
                                            @Nonnull final JsonGenerator g,
                                            @Nonnull final UriInfo uri )
  {
    g.write( "url", getInstanceChannelURL( session, channeID, uri ) );
    final Collection<SubscriptionEntry> entries =
      session.getSubscriptions().values().stream().filter( s -> s.getDescriptor().getChannelID() == channeID ).
        collect( Collectors.toList() );
    emitSubscriptions( systemMetaData, session, g, entries, uri );
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
  private static String getInstanceChannelURL( @Nonnull final SessionInfo session,
                                               final int channelID,
                                               @Nonnull final UriInfo uri )
  {
    return getSubscriptionsURL( session, uri ) + '/' + channelID;
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

  private static void emitSubscriptions( @Nonnull final SystemMetaData systemMetaData,
                                         @Nonnull final ReplicantSession session,
                                         @Nonnull final JsonGenerator g,
                                         @Nonnull final Collection<SubscriptionEntry> subscriptionEntries,
                                         @Nonnull final UriInfo uri )
  {
    g.writeStartArray( "subscriptions" );

    final ArrayList<SubscriptionEntry> subscriptions = new ArrayList<>( subscriptionEntries );
    Collections.sort( subscriptions );

    for ( final SubscriptionEntry subscription : subscriptions )
    {
      emitChannel( systemMetaData, session, g, subscription, uri );
    }
    g.writeEnd();
  }

  private static void emitChannelID( @Nonnull final JsonGenerator g,
                                     final int channelID )
  {
    g.write( "channelID", channelID );
  }

  private static void emitSubChannelID( @Nonnull final JsonGenerator g, @Nullable final Serializable subChannelID )
  {
    if ( null != subChannelID )
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
                                              @Nonnull final Set<ChannelDescriptor> descriptors )
  {
    for ( final ChannelDescriptor descriptor : descriptors )
    {
      g.writeStartObject();
      emitChannelDescriptor( g, descriptor );
      g.writeEnd();
    }
  }

  static void emitChannel( @Nonnull final SystemMetaData systemMetaData,
                           @Nonnull final ReplicantSession session,
                           @Nonnull final JsonGenerator g,
                           @Nonnull final SubscriptionEntry entry,
                           @Nonnull final UriInfo uri )
  {
    g.writeStartObject();
    final ChannelMetaData channelMetaData = systemMetaData.getChannelMetaData( entry.getDescriptor() );
    g.write( "name", channelMetaData.getName() );
    g.write( "url", getChannelURL( session, entry.getDescriptor(), uri ) );
    emitChannelDescriptor( g, entry.getDescriptor() );
    g.write( "explicitlySubscribed", entry.isExplicitlySubscribed() );
    if ( channelMetaData.getFilterType() != ChannelMetaData.FilterType.NONE )
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

    emitChannelDescriptors( g, "inwardSubscriptions", entry.getInwardSubscriptions() );
    emitChannelDescriptors( g, "outwardSubscriptions", entry.getOutwardSubscriptions() );
    g.writeEnd();
  }

  private static void emitChannelDescriptor( @Nonnull final JsonGenerator g,
                                             @Nonnull final ChannelDescriptor descriptor )
  {
    emitChannelID( g, descriptor.getChannelID() );
    emitSubChannelID( g, descriptor.getSubChannelID() );
  }

  private static void emitChannelDescriptors( @Nonnull final JsonGenerator g,
                                              @Nonnull final String key,
                                              @Nonnull final Set<ChannelDescriptor> descriptors )
  {
    if ( !descriptors.isEmpty() )
    {
      g.writeStartArray( key );
      emitChannelDescriptors( g, descriptors );
      g.writeEnd();
    }
  }
}
