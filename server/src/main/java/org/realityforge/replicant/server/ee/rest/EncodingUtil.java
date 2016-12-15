package org.realityforge.replicant.server.ee.rest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.json.stream.JsonGenerator;
import org.realityforge.replicant.server.ChannelDescriptor;
import org.realityforge.replicant.server.ee.JsonUtil;
import org.realityforge.replicant.server.transport.SubscriptionEntry;
import org.realityforge.rest.field_filter.FieldFilter;

/**
 * Utilities for encoding session details as json.
 */
final class EncodingUtil
{

  private EncodingUtil()
  {
  }

  static void emitSubscriptions( @Nonnull final JsonGenerator g,
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

  private static void emitSubscriptionEntry( @Nonnull final JsonGenerator g,
                                             @Nonnull final SubscriptionEntry entry,
                                             @Nonnull final FieldFilter filter )
  {
    if ( filter.allow( "subscription" ) )
    {
      final FieldFilter subFilter = filter.subFilter( "subscription" );
      g.writeStartObject();
      if ( subFilter.allow( "explicitlySubscribed" ) )
      {
        g.write( "explicitlySubscribed", entry.isExplicitlySubscribed() );
      }
      emitChannelDescriptor( g, subFilter, entry.getDescriptor() );
      if ( null != entry.getFilter() && subFilter.allow( "filter" ) )
      {
        g.write( "filter", JsonUtil.toJsonObject( entry.getFilter() ) );
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
