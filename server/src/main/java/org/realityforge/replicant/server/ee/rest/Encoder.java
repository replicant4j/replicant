package org.realityforge.replicant.server.ee.rest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.core.UriInfo;
import org.realityforge.replicant.server.ChannelAddress;
import org.realityforge.replicant.server.ee.JsonUtil;
import org.realityforge.replicant.server.transport.ChannelMetaData;
import org.realityforge.replicant.server.transport.ReplicantSession;
import org.realityforge.replicant.server.transport.SubscriptionEntry;
import org.realityforge.replicant.server.transport.SystemMetaData;
import org.realityforge.replicant.shared.SharedConstants;

final class Encoder
{
  private Encoder()
  {
  }

  static void emitSession( @Nonnull final SystemMetaData systemMetaData,
                           @Nonnull final ReplicantSession session,
                           @Nonnull final JsonGenerator g,
                           @Nonnull final UriInfo uri,
                           final boolean emitNetworkData )
  {
    g.writeStartObject();
    g.write( "id", session.getId() );
    final String userID = session.getUserId();
    if ( null == userID )
    {
      g.writeNull( "userID" );
    }
    else
    {
      g.write( "userID", userID );
    }
    g.write( "url", getSessionURL( session, uri ) );

    if ( emitNetworkData )
    {
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
      session.getSubscriptions().values().stream().filter( s -> s.getDescriptor().getChannelId() == channeID ).
        collect( Collectors.toList() );
    emitSubscriptions( systemMetaData, session, g, entries, uri );
  }

  @Nonnull
  private static String getSessionURL( @Nonnull final ReplicantSession session, @Nonnull final UriInfo uri )
  {
    return uri.getBaseUri() + SharedConstants.CONNECTION_URL_FRAGMENT.substring( 1 ) + "/" + session.getId();
  }

  @Nonnull
  private static String getSubscriptionsURL( @Nonnull final ReplicantSession session, @Nonnull final UriInfo uri )
  {
    return getSessionURL( session, uri ) + SharedConstants.CHANNEL_URL_FRAGMENT;
  }

  @Nonnull
  private static String getInstanceChannelURL( @Nonnull final ReplicantSession session,
                                               final int channelId,
                                               @Nonnull final UriInfo uri )
  {
    return getSubscriptionsURL( session, uri ) + '/' + channelId;
  }

  @Nonnull
  private static String getChannelURL( @Nonnull final ReplicantSession session,
                                       @Nonnull final ChannelAddress address,
                                       @Nonnull final UriInfo uri )
  {
    final String baseURL = getSubscriptionsURL( session, uri ) + '/' + address.getChannelId();
    if ( null != address.getSubChannelId() )
    {
      return baseURL + '.' + address.getSubChannelId();
    }
    else
    {
      return baseURL;
    }
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

  private static void emitChannelId( @Nonnull final JsonGenerator g,
                                     final int channelId )
  {
    g.write( "channelId", channelId );
  }

  private static void emitSubChannelId( @Nonnull final JsonGenerator g, @Nullable final Serializable subChannelId )
  {
    if ( null != subChannelId )
    {
      if ( subChannelId instanceof Integer )
      {
        g.write( "instanceID", (Integer) subChannelId );
      }
      else
      {
        g.write( "instanceID", String.valueOf( subChannelId ) );
      }
    }
  }

  private static void emitChannelDescriptors( @Nonnull final SystemMetaData systemMetaData,
                                              @Nonnull final JsonGenerator g,
                                              @Nonnull final Set<ChannelAddress> addresses )
  {
    for ( final ChannelAddress address : addresses )
    {
      g.writeStartObject();
      emitChannelDescriptor( systemMetaData, g, address );
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
    g.write( "url", getChannelURL( session, entry.getDescriptor(), uri ) );
    emitChannelDescriptor( systemMetaData, g, entry.getDescriptor() );
    g.write( "explicitlySubscribed", entry.isExplicitlySubscribed() );
    if ( channelMetaData.hasFilterParameter() )
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

    emitChannelDescriptors( systemMetaData, g, "inwardSubscriptions", entry.getInwardSubscriptions() );
    emitChannelDescriptors( systemMetaData, g, "outwardSubscriptions", entry.getOutwardSubscriptions() );
    g.writeEnd();
  }

  private static void emitChannelDescriptor( @Nonnull final SystemMetaData systemMetaData,
                                             @Nonnull final JsonGenerator g,
                                             @Nonnull final ChannelAddress address )
  {
    g.write( "name", systemMetaData.getChannelMetaData( address.getChannelId() ).getName() );
    emitChannelId( g, address.getChannelId() );
    emitSubChannelId( g, address.getSubChannelId() );
  }

  private static void emitChannelDescriptors( @Nonnull final SystemMetaData systemMetaData,
                                              @Nonnull final JsonGenerator g,
                                              @Nonnull final String key,
                                              @Nonnull final Set<ChannelAddress> addresses )
  {
    if ( !addresses.isEmpty() )
    {
      g.writeStartArray( key );
      emitChannelDescriptors( systemMetaData, g, addresses );
      g.writeEnd();
    }
  }
}
